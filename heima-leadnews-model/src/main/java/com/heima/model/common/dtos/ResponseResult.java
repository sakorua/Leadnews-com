package com.heima.model.common.dtos;

import com.alibaba.fastjson.JSON;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.*;

/**
 * 通用的结果返回类
 * @param <T>
 */
@Setter
@Getter
public class ResponseResult<T> implements Serializable {
    private String host; // IP
    private Integer code = 200;  // 状态码
    private String errorMessage;  // 提示信息
    private T data;  // 数据 Object

    public ResponseResult() {
    }
    public ResponseResult(Integer code, T data) {
        this.code = code;
        this.data = data;
    }
    public ResponseResult(Integer code, String msg, T data) {
        this.code = code;
        this.errorMessage = msg;
        this.data = data;
    }
    public ResponseResult(Integer code, String msg) {
        this.code = code;
        this.errorMessage = msg;
    }
    public static ResponseResult errorResult(int code, String msg) {
        ResponseResult result = new ResponseResult();
        return result.error(code, msg);
    }

    public static ResponseResult okResult(int code, String msg) {
        ResponseResult result = new ResponseResult();
        return result.ok(code, null, msg);
    }
    public static ResponseResult okResult(Object data) {
        ResponseResult result = setAppHttpCodeEnum(AppHttpCodeEnum.SUCCESS, AppHttpCodeEnum.SUCCESS.getErrorMessage());
        if(data!=null) {
            result.setData(data);
        }
        return result;
    }
    public static ResponseResult okResult() {
        return okResult(null);
    }
    public static ResponseResult errorResult(AppHttpCodeEnum enums){
        return setAppHttpCodeEnum(enums,enums.getErrorMessage());
    }
    public static ResponseResult errorResult(AppHttpCodeEnum enums, String errorMessage){
        return setAppHttpCodeEnum(enums,errorMessage);
    }
    public static ResponseResult setAppHttpCodeEnum(AppHttpCodeEnum enums){
        return okResult(enums.getCode(),enums.getErrorMessage());
    }
    private static ResponseResult setAppHttpCodeEnum(AppHttpCodeEnum enums, String errorMessage){
        return okResult(enums.getCode(),errorMessage);
    }
    public ResponseResult<?> error(Integer code, String msg) {
        this.code = code;
        this.errorMessage = msg;
        return this;
    }
    public ResponseResult<?> ok(Integer code, T data) {
        this.code = code;
        this.data = data;
        return this;
    }
    public ResponseResult<?> ok(Integer code, T data, String msg) {
        this.code = code;
        this.data = data;
        this.errorMessage = msg;
        return this;
    }
    public ResponseResult<?> ok(T data) {
        this.data = data;
        return this;
    }

    public boolean checkCode() {
        if(this.getCode().intValue() != 0){
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        ResponseResult result = ResponseResult.okResult();
        System.out.println(JSON.toJSONString(result));

        ResponseResult result1 = ResponseResult.errorResult(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR, "您输入的密码错误");
        System.out.println(JSON.toJSONString(result1));
        Map map = new HashMap();
        map.put("username","itcast");
        map.put("phone","13888888888");
        ResponseResult result2 = ResponseResult.okResult(map);
        System.out.println(JSON.toJSONString(result2));

        PageResponseResult result3 = new PageResponseResult(1,5,600L);
        List<String> list = Arrays.asList("数据1","数据2","数据3","数据4");
        result3.setData(list);

        System.out.println(JSON.toJSONString(result3));

    }
}
