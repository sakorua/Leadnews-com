package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.CustException;
import com.heima.common.exception.CustomException;
import com.heima.feigns.ArticleFeign;
import com.heima.feigns.WemediaFeign;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.constants.admin.AdminConstants;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author SaKoRua
 * @date 2022-02-27 8:51 PM
 * @Description //TODO
 */
@Service
@Slf4j
public class ApUserRealnameServiceImpl extends ServiceImpl<ApUserRealnameMapper, ApUserRealname> implements ApUserRealnameService {


    @Autowired
    ApUserMapper apUserMapper;
    @Autowired
    WemediaFeign wemediaFeign;
    @Autowired
    ArticleFeign articleFeign;

    @Override
    public ResponseResult loadListByStatus(AuthDTO dto) {

        if (dto == null) {
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        }

        dto.checkParam();

        Page<ApUserRealname> page = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<ApUserRealname> wrapper = Wrappers.<ApUserRealname>lambdaQuery();

        if (dto.getStatus() != null) {
            wrapper.eq(ApUserRealname::getStatus, dto.getStatus());
        }

        IPage<ApUserRealname> iPage = this.page(page, wrapper);


        return new PageResponseResult(dto.getPage(), dto.getSize(), iPage.getTotal(), iPage.getRecords());
    }

    @Override
    public ResponseResult updateStatusById(AuthDTO dto, Short status) {

        //1 ????????????
        if (dto.getId() == null) {
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2 ????????????????????????????????? APP??????????????????
        ApUserRealname apUserRealname = getOne(Wrappers.<ApUserRealname>lambdaQuery()
                .eq(ApUserRealname::getId, dto.getId())
        );
        if (apUserRealname == null) {
            log.error("????????? ???????????????????????????   userRealnameId:{}", dto.getId());
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        if (!AdminConstants.WAIT_AUTH.equals(apUserRealname.getStatus())) {
            log.error("????????????????????????????????????   userRealnameId:{}", dto.getId());
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_ALLOW);
        }

        ApUser apUser = apUserMapper.selectOne(Wrappers.<ApUser>lambdaQuery()
                .eq(ApUser::getId, apUserRealname.getUserId()));
        if (apUser == null) {
            log.error("?????????????????? ?????? app??????????????????    userRealnameId:{}, userId:{} ", dto.getId(), apUserRealname.getUserId());
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //3 ????????????????????????
        apUserRealname.setStatus(status);
        apUserRealname.setUpdatedTime(new Date());
        if (StringUtils.isNotBlank(dto.getMsg())) {
            apUserRealname.setReason(dto.getMsg());
        }
        updateById(apUserRealname);

        //4 ????????????????????? ??????
        if (AdminConstants.PASS_AUTH.equals(status)) {
            //4.1 ?????????????????????
            WmUser wmUser = createWmUser(dto, apUser);
            //4.2 ??????????????????
            createApAuthor(wmUser);
        }
        //5 ????????????
        return ResponseResult.okResult();
    }

    /**
     * 4.2 ??????????????????
     *
     * @param wmUser
     * @return
     */
    private void createApAuthor(WmUser wmUser) {
        //1 ????????????????????????
        ResponseResult<ApAuthor> apAuthorResult = articleFeign.findByUserId(wmUser.getApUserId());
        if (apAuthorResult.getCode().intValue() != 0) {
            CustException.cust(AppHttpCodeEnum.SERVER_ERROR, apAuthorResult.getErrorMessage());
        }
        //2. ????????????????????????????????????
        ApAuthor apAuthor = apAuthorResult.getData();
        if (apAuthor != null) {
            CustException.cust(AppHttpCodeEnum.DATA_EXIST, "?????????????????????");
        }
        //3. ??????????????????
        apAuthor = new ApAuthor();
        apAuthor.setCreatedTime(new Date());
        apAuthor.setName(wmUser.getName());
        apAuthor.setType(AdminConstants.AUTHOR_TYPE); // ??????????????????
        apAuthor.setUserId(wmUser.getApUserId()); // APP ??????ID
        apAuthor.setWmUserId(wmUser.getId()); // ???????????????ID
        ResponseResult result = articleFeign.save(apAuthor);
        //4. ???????????????????????????
        if (result.getCode() != 0) {
            CustException.cust(AppHttpCodeEnum.SERVER_ERROR, result.getErrorMessage());
        }
    }

    /**
     * 4.1 ?????????????????????
     *
     * @param dto
     * @param apUser APP?????????
     * @return
     */
    private WmUser createWmUser(AuthDTO dto, ApUser apUser) {
        //1 ????????????????????????????????????APP??????????????????????????????????????????
        ResponseResult<WmUser> wmUserResult = wemediaFeign.findByName(apUser.getName());
        if (wmUserResult.getCode().intValue() != 0) {
            CustException.cust(AppHttpCodeEnum.SERVER_ERROR, wmUserResult.getErrorMessage());
        }
        WmUser wmUser = wmUserResult.getData();
        if (wmUser != null) {
            CustException.cust(AppHttpCodeEnum.DATA_EXIST, "??????????????????????????????");
        }
        wmUser = new WmUser();
        wmUser.setName(apUser.getName());
        wmUser.setSalt(apUser.getSalt());  // ???
        wmUser.setPassword(apUser.getPassword()); // ??????
        wmUser.setPhone(apUser.getPhone());
        wmUser.setCreatedTime(new Date());
        wmUser.setType(0); // ??????
        wmUser.setApUserId(apUser.getId());  // app?????????id
        wmUser.setStatus(AdminConstants.PASS_AUTH.intValue());

        ResponseResult<WmUser> saveResult = wemediaFeign.save(wmUser);
        if (saveResult.getCode().intValue() != 0) {
            CustException.cust(AppHttpCodeEnum.SERVER_ERROR, saveResult.getErrorMessage());
        }
        return saveResult.getData();
    }
}
