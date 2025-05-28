package org.leocoder.devnote.hub.service.impl;

import org.leocoder.devnote.hub.domain.vo.file.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07
 * @description : 文件服务接口
 */

public interface FileService {

    /**
     * 上传文件
     *
     * @param file     上传的文件
     * @param mimeType 文件MIME类型
     * @return 文件上传响应对象
     */
    FileUploadVO uploadFile(MultipartFile file, String mimeType);

    /**
     * 上传图片
     *
     * @param file 图片文件
     * @return 文件上传响应对象
     */
    FileUploadVO uploadImage(MultipartFile file);

    /**
     * 上传文档
     *
     * @param file 文档文件
     * @return 文件上传响应对象
     */
    FileUploadVO uploadDocument(MultipartFile file);


    /**
     * 通过InputStream上传文件
     *
     * @param inputStream 输入流
     * @param fileName    文件名
     * @param contentType 内容类型
     * @param size        文件大小
     * @return 文件上传响应对象
     */
    FileUploadVO uploadFile(InputStream inputStream, String fileName, String contentType, long size);

    /**
     * 删除文件
     *
     * @param objectName 对象名称
     * @return 是否删除成功
     */
    boolean deleteFile(String objectName);

    /**
     * 获取文件访问URL
     *
     * @param objectName 对象名称
     * @param expiry     过期时间(秒)，-1表示永不过期
     * @return 文件访问URL
     */
    String getFileUrl(String objectName, int expiry);

    /**
     * 检查文件是否存在
     *
     * @param objectName 对象名称
     * @return 是否存在
     */
    boolean isFileExist(String objectName);


    /**
     * 获取MinIO服务端点URL
     *
     * @return MinIO端点URL
     */
    String getMinioEndpoint();
}