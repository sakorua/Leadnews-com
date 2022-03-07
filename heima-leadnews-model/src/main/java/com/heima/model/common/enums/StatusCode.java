package com.heima.model.common.enums;

/**
 * @author TaoHongQiang
 * @date Created  2022/2/27 19:40
 * @Description
 */
public enum StatusCode {

    //用户账号状态
    USER_STATUS_TEMP_NO(0, "暂时不可用"),
    USER_STATUS_NO(1, "永久不可用"),
    USER_STATUS_YES(9, "正常可用");

    final int code;
    final String message;

    public int getCode() {
        return code;
    }


    public String getMessage() {
        return message;
    }


    StatusCode(int code, String message) {
        this.code = code;
        this.message = message;
    }


}
