package org.leocoder.devnote.hub.service.impl;

import org.leocoder.devnote.hub.domain.vo.file.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07
 * @description :
 */

public interface FileService {

    /**
     * 上传文件
     *
     * @param file         要上传的文件
     * @param fileCategory 文件分类/文件夹
     * @return 文件上传响应
     */
    FileUploadVO uploadFile(MultipartFile file, String fileCategory);

    /**
     * 通过文件键获取文件
     *
     * @param fileKey 文件标识符
     * @return 文件输入流
     */
    InputStream getFile(String fileKey);

    /**
     * 通过文件键删除文件
     *
     * @param fileKey 文件标识符
     */
    void deleteFile(String fileKey);

    /**
     * 生成文件访问URL
     *
     * @param fileKey 文件标识符
     * @return 可访问URL字符串
     */
    String getPresignedUrl(String fileKey);

    /**
     * 初始化存储桶
     */
    void initBucket();


    /**
     * 获取 MinIO 服务端点
     *
     * @return MinIO 服务端点 URL
     */
    String getMinioEndpoint();
}