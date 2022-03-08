package com.heima.user.service.impl;
import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.admin.AdminConstants;
import com.heima.common.exception.CustException;
import com.heima.feigns.ArticleFeign;
import com.heima.feigns.WemediaFeign;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.AuthDTO;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserRealname;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.mapper.ApUserRealnameMapper;
import com.heima.user.service.ApUserRealnameService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author mrchen
 * @date 2022/2/27 11:14
 */
@Service
@Slf4j
public class ApUserRealnameServiceImpl extends ServiceImpl<ApUserRealnameMapper, ApUserRealname> implements ApUserRealnameService {
    @Override
    public ResponseResult loadListByStatus(AuthDTO dto) {
        // 1. 校验参数
        dto.checkParam();
        // 2. 封装条件
        LambdaQueryWrapper<ApUserRealname> wrapper = Wrappers.<ApUserRealname>lambdaQuery();
        // 2.1  查询条件
        wrapper.eq(dto.getStatus()!=null,ApUserRealname::getStatus,dto.getStatus());
        wrapper.orderByDesc(ApUserRealname::getCreatedTime);
        // 2.2  分页条件
        Page<ApUserRealname> pageReq = new Page<>(dto.getPage(), dto.getSize());
        IPage<ApUserRealname> pageResult = this.page(pageReq, wrapper);
        // 3. 封装返回结果
        return new PageResponseResult(dto.getPage(),dto.getSize(),pageResult.getTotal(),pageResult.getRecords());
    }
    @Autowired
    ApUserMapper apUserMapper;
    @GlobalTransactional(rollbackFor = Exception.class,timeoutMills = 100000)
    @Override
    public ResponseResult updateStatusById(AuthDTO dto, Short status) {
        // 1. 参数校验  id (realname表)
        if(dto.getId() == null){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"实名认证id错误");
        }
        //     状态  1 (待审核)     2 9
        ApUserRealname realname = getById(dto.getId());
        if (realname==null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"实名认证数据不存在");
        }
        if (!AdminConstants.WAIT_AUTH.equals(realname.getStatus())){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW,"当前实名认证状态不是待审核");
        }
        //     关联的appUser是否存在
        ApUser apUser = apUserMapper.selectById(realname.getUserId());
        if (apUser == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"关联的app用户不存在");
        }
        // 2. 修改审核状态
        realname.setStatus(status);
        if (StringUtils.isNotBlank(dto.getMsg())) {
            realname.setReason(dto.getMsg());
        }
        updateById(realname);
        // 3. 判断状态是否为 9
        if(!status.equals(AdminConstants.PASS_AUTH)){
            return ResponseResult.okResult();
        }
        // 3.1  开通自媒体账户
        WmUser wmUser = createWmUser(apUser);
        // 3.2  创建作者信息
        createAuthor(apUser,wmUser);
        if (dto.getId().intValue() == 5) {
            CustException.cust(AppHttpCodeEnum.SERVER_ERROR,"模拟异常");
        }
        return ResponseResult.okResult();
    }

    @Autowired
    ArticleFeign articleFeign;

    /**
     * 创建 关联作者信息
     * @param apUser
     * @param wmUser
     */
    private void createAuthor(ApUser apUser, WmUser wmUser) {
        // 1. 远程查询 作者信息
        ResponseResult<ApAuthor> authorResult = articleFeign.findByUserId(apUser.getId());
        if (!authorResult.checkCode()) {
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR);
        }
        ApAuthor author = authorResult.getData();
        // 2. 判断是否存在
        if (author!=null){
            CustException.cust(AppHttpCodeEnum.DATA_EXIST,"作者信息已存在");
        }
        // 3. 远程创建作者
        author = new ApAuthor();
        author.setName(apUser.getName());
        author.setType(AdminConstants.AUTHOR_TYPE);
        author.setUserId(apUser.getId());
        author.setCreatedTime(new Date());
        author.setWmUserId(wmUser.getId());
        ResponseResult saveResult = articleFeign.save(author);
        if (!saveResult.checkCode()) {
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR);
        }
    }

    @Autowired
    WemediaFeign wemediaFeign;

    /**
     * 开通自媒体用户信息
     * @param apUser
     * @return
     */
    private WmUser createWmUser(ApUser apUser) {
        // 远程查询  根据 name查询自媒体用户是否存在
        ResponseResult<WmUser> wmUserResult = wemediaFeign.findByName(apUser.getName());
        if (!wmUserResult.checkCode()) {
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR);
        }
        // 如果存在  抛出已存在异常
        WmUser wmUser = wmUserResult.getData();
        if(wmUser!=null){
            CustException.cust(AppHttpCodeEnum.DATA_EXIST,"自媒体用户已经存在了");
        }
        // 如果不存在  远程调用添加接口 保存自媒体用户信息
        wmUser = new WmUser();
        wmUser.setApUserId(apUser.getId());
        wmUser.setName(apUser.getName());
        wmUser.setPassword(apUser.getPassword());
        wmUser.setSalt(apUser.getSalt());
        wmUser.setImage(apUser.getImage());
        wmUser.setPhone(apUser.getPhone());
        wmUser.setStatus(AdminConstants.USER_STATUS_ALLOW);
        wmUser.setType(AdminConstants.AUTHOR_TYPE);
        wmUser.setCreatedTime(new Date());
        ResponseResult<WmUser> saveResult = wemediaFeign.save(wmUser);
        if (!saveResult.checkCode()) {
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR);
        }
        return saveResult.getData();
    }
}
