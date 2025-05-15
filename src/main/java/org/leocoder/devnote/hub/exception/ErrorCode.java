package org.leocoder.devnote.hub.exception;

import lombok.Getter;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 13:36
 * @description : 系统错误码
 */
@Getter
public enum ErrorCode {

    SUCCESS(200, "success"),

    PARAMS_ERROR(40000, "请求参数错误"),
    PARAMETER_ERROR(40001, "参数校验失败"),

    NOT_LOGIN_ERROR(40100, "未登录"),

    NO_AUTH_ERROR(40101, "无权限"),

    OPERATION_ERROR(50001, "操作失败"),

    BUSINESS_ERROR(50002, "业务异常"),

    SYSTEM_ERROR(50003, "系统内部异常"),

    NOT_FOUND_ERROR(404,  "资源不存在"),

    UPLOAD_FAILURE(50010, "文件上传失败"),

    DELETE_FAILURE(50011, "文件删除失败"),


    ACCOUNT_EXIST(40201, "账号已存在"),


    ACCOUNT_NOT_FOUND(40202, "账号不存在"),

    PASSWORD_ERROR(40203, "用户名或密码错误"),


    ACCOUNT_BANNED(40204, "账号已被禁用"),


    NOT_LOGIN(40206, "未登录"),

    NO_AUTH(40207, "无权限"),

    // 数据相关错误码
    DATA_EXIST(40301, "数据已存在"),

    DATA_NOT_FOUND(40302, "数据不存在"),

    DATA_USED(40303, "数据已被使用"),


    ;

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}