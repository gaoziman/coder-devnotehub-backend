package org.leocoder.devnote.hub.exception;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-05-01 02:06
 * @description : 文件操作专用异常类
 */
public class FileOperationException extends BusinessException {

    public FileOperationException(String message) {
        super(ErrorCode.OPERATION_ERROR, message);
    }

    public FileOperationException(String message, Throwable cause) {
        super(ErrorCode.OPERATION_ERROR, message);
    }
}