package com.heima.wemedia.service.impl;
import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.wemedia.WemediaConstants;
import com.heima.common.exception.CustException;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.WmThreadLocalUtils;
import com.heima.model.wemedia.dtos.WmMaterialDTO;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author mrchen
 * @date 2022/3/1 15:26
 */
@Service
@Slf4j
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {

    @Autowired
    FileStorageService fileStorageService;

    @Value("${file.oss.prefix}")
    String prefix;
    @Value("${file.oss.web-site}")
    String webSite;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult uploadPicture(MultipartFile multipartFile) {
        // 1. 校验参数
        //    文件不为空
        if (multipartFile==null || multipartFile.getSize() <=0 ) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"文件参数错误");
        }
        //    文件后缀     .jpg  .jpeg  .png .gif
        // 原始的文件名称
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        if (!checkSuffix(suffix)) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"文件类型不支持");
        }
        //    判断用户是否登陆
        WmUser user = WmThreadLocalUtils.getUser();
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN,"登陆后才能上传素材");
        }
        // 2. 将文件上传到OSS
        String newUrl = UUID.randomUUID().toString().replaceAll("-","") + suffix;
        try {
            newUrl = fileStorageService.store(prefix, newUrl, multipartFile.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            log.error("上传文件出现异常  {}",e.getMessage());
            CustException.cust(AppHttpCodeEnum.SERVER_ERROR,"上传文件出现异常 "+e.getMessage());
        }
        // 3. 创建wmMaterial素材对象，存入数据库中
        WmMaterial wmMaterial = new WmMaterial();
        wmMaterial.setUserId(user.getId());
        wmMaterial.setUrl(newUrl);
        wmMaterial.setType((short)0);
        wmMaterial.setIsCollection((short)0);
        wmMaterial.setCreatedTime(new Date());
        this.save(wmMaterial);
        // 4. 响应结果   wmMaterial = webSite + url
        wmMaterial.setUrl(webSite + wmMaterial.getUrl());
        return ResponseResult.okResult(wmMaterial);
    }

    @Override
    public ResponseResult findList(WmMaterialDTO dto) {
        // 1. 校验参数  分页  是否登陆
        dto.checkParam();
        WmUser user = WmThreadLocalUtils.getUser();
        if (user==null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        // 2. 封装条件
        LambdaQueryWrapper<WmMaterial> wrapper = Wrappers.lambdaQuery();
        // 2.1  分页条件
        Page<WmMaterial> pageReq = new Page<>(dto.getPage(), dto.getSize());
        // 2.2  查询条件
        //      isCollection
        if(WemediaConstants.COLLECT_MATERIAL.equals(dto.getIsCollection())){
            wrapper.eq(WmMaterial::getIsCollection,WemediaConstants.COLLECT_MATERIAL);
        }
        //      用户上传的素材
        wrapper.eq(WmMaterial::getUserId,user.getId());
        //      按照时间排序
        wrapper.orderByDesc(WmMaterial::getCreatedTime);
        // 3.  执行查询
        IPage<WmMaterial> pageResult = this.page(pageReq, wrapper);
        List<WmMaterial> records = pageResult.getRecords();
        // 4.   添加webSite前缀
        for (WmMaterial record : records) {
            record.setUrl(webSite + record.getUrl());
        }
        return new PageResponseResult(dto.getPage(),dto.getSize(),pageResult.getTotal(),records);
    }

    private boolean checkSuffix(String suffix) {
        if(StringUtils.isBlank(suffix)){
            return false;
        }
        List<String> list = Arrays.asList(".jpg", ".jpeg", ".png", ".gif");
        for (String allowSuffix : list) {
            if(allowSuffix.equals(suffix)){
                return true;
            }
        }
        return false;
    }
    @Autowired
    WmNewsMaterialMapper wmNewsMaterialMapper;
    /**
     * 删除素材
     * @param id
     * @return
     */
    @Override
    public ResponseResult delPicture(Integer id) {
        // 1 参数校验
        if (id == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        // 2 业务处理
        // 判断当前id是否被引用，如果被使用则禁止删除
        WmMaterial wmMaterial = getById(id);
        if(wmMaterial == null){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        LambdaQueryWrapper<WmNewsMaterial> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmNewsMaterial::getMaterialId, id);
        Integer count = wmNewsMaterialMapper.selectCount(wrapper);
        if(count > 0){
            CustException.cust( AppHttpCodeEnum.DATA_NOT_ALLOW);
        }
        // 删除素材库
        removeById(id);
        // 删除图片OSS
        fileStorageService.delete(wmMaterial.getUrl());
        // 3 封装结果
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult updateStatus(Integer id, Short type) {
        //1.检查参数
        if(id == null){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.更新状态
        WmMaterial material = getById(id);
        if (material == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"素材信息不存在");
        }
        //获取当前用户信息
        Integer uid = WmThreadLocalUtils.getUser().getId();
        if(!material.getUserId().equals(uid)){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW,"只允许收藏自己上传的素材");
        }
        material.setIsCollection(type);
        updateById(material);
//        update(Wrappers.<WmMaterial>lambdaUpdate()  // 如果只想修改指定字段 可以使用此方法
//                .set(WmMaterial::getIsCollection,type)
//                .eq(WmMaterial::getId,id)
//                .eq(WmMaterial::getUserId,uid));
        return ResponseResult.okResult();
    }

}
