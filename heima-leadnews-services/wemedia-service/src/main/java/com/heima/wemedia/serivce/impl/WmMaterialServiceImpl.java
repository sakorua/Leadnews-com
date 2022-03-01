package com.heima.wemedia.serivce.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.heima.wemedia.serivce.WmMaterialService;
import io.seata.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author SaKoRua
 * @date 2022-03-01 7:57 PM
 * @Description //TODO
 */
@Service
@Slf4j
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    WmNewsMaterialMapper wmNewsMaterialMapper;

    @Value("${file.oss.prefix}")
    String prefix;

    @Value("${file.oss.web-site}")
    String webSite;


    @Override
    public ResponseResult uploadPicture(MultipartFile multipartFile) {

        if (multipartFile.getSize() == 0 || multipartFile == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "请上传正确的文件");
        }

        WmUser user = WmThreadLocalUtils.getUser();

        if (user == null) {
            CustException.cust(AppHttpCodeEnum.NO_OPERATOR_AUTH, "请登录后操作");
        }

        String originalFilename = multipartFile.getOriginalFilename();

        if (!checkFileSuffix(originalFilename)) {
            //上传文件格式不正确
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "上传文件格式不正确");
        }

        String filename = UUID.randomUUID().toString().replace("-", "");
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        String fileId = null;
        try {
            fileId = fileStorageService.store(prefix, filename + suffix, multipartFile.getInputStream());
            log.info("阿里云OSS 文件 fileId: {}", fileId);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("阿里云文件上传失败 ==> uploadPicture error: {}", e);
            CustException.cust(AppHttpCodeEnum.SERVER_ERROR, "服务器繁忙请稍后重试");
        }

        WmMaterial wmMaterial = new WmMaterial();
        wmMaterial.setIsCollection((short) 0);
        wmMaterial.setType((short) 0);
        wmMaterial.setCreatedTime(new Date());

        wmMaterial.setUrl(fileId);
        wmMaterial.setUserId(user.getId());
        save(wmMaterial);

        //用于前端展示
        wmMaterial.setUrl(webSite + fileId);
        return ResponseResult.okResult(wmMaterial);
    }


    /**
     * 素材列表查询
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findList(WmMaterialDTO dto) {

        dto.checkParam();

        LambdaQueryWrapper<WmMaterial> wrapper = new LambdaQueryWrapper<>();

        if (dto.getIsCollection() != null && dto.getIsCollection() == 1) {
            wrapper.eq(WmMaterial::getIsCollection, dto.getIsCollection());
        }

        WmUser user = WmThreadLocalUtils.getUser();
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.NO_OPERATOR_AUTH);
        }

        wrapper.eq(WmMaterial::getUserId, user.getId());

        Page<WmMaterial> pageParam = new Page<>(dto.getPage(), dto.getSize());
        IPage<WmMaterial> page = page(pageParam, wrapper);

        List<WmMaterial> records = page.getRecords();
        for (WmMaterial record : records) {
            record.setUrl(webSite + record.getUrl());
        }

        PageResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), page.getTotal());
        responseResult.setData(records);
        return responseResult;
    }

    /**
     * 删除图片
     *
     * @param id
     * @return
     */
    @Override
    public ResponseResult delPicture(Integer id) {

        if (id == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmMaterial wmMaterial = this.getById(id);

        if (wmMaterial == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        LambdaQueryWrapper<WmNewsMaterial> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmNewsMaterial::getMaterialId, id);

        int count = wmNewsMaterialMapper.selectCount(wrapper);

        if (count > 0) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW);
        }

        this.removeById(id);

        fileStorageService.delete(wmMaterial.getUrl());
        return ResponseResult.okResult();
    }


    /**
     * 收藏与取消收藏
     *
     * @param id
     * @param type
     * @return
     */
    @Override
    public ResponseResult updateStatus(Integer id, Short type) {

        if (id == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmMaterial material = this.getById(id);
        if (material == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "素材信息不存在");
        }

        Integer userId = WmThreadLocalUtils.getUser().getId();
        if (!userId.equals(material.getUserId())) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW, "只允许收藏自己上传的素材");
        }


        material.setIsCollection(type);
        this.updateById(material);


        return ResponseResult.okResult();
    }


    private Boolean checkFileSuffix(String originalFilename) {

        if (StringUtils.isBlank(originalFilename)) {
            return false;
        }

        List<String> allowSuffix = Arrays.asList("jpg", "jpeg", "png", "gif");

        Boolean isAllow = false;

        for (String suffix : allowSuffix) {
            if (suffix.equals(originalFilename)) {
                isAllow = true;
                break;
            }
        }

        return isAllow;
    }
}