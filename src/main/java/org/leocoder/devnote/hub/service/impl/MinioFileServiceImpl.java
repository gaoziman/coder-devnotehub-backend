package org.leocoder.devnote.hub.service.impl;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.devnote.hub.config.MinioConfig;
import org.leocoder.devnote.hub.domain.vo.file.FileUploadVO;
import org.leocoder.devnote.hub.exception.FileOperationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07
 * @description : 文件上传实现类
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileServiceImpl implements FileService {

    private final MinioClient minioClient;

    private final MinioConfig minioConfig;

    /**
     * 初始化MinIO桶（如果不存在）
     */
    @Override
    public void initBucket() {
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .build()
            );

            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .build()
                );
                log.info("已创建MinIO存储桶: {}", minioConfig.getBucketName());
            }
        } catch (Exception e) {
            log.error("初始化MinIO存储桶失败", e);
            throw new FileOperationException("初始化存储桶失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传文件到MinIO
     * @param file         要上传的文件
     * @param fileCategory 文件分类/文件夹
     * @return 上传的返回vo
     */
    @Override
    public FileUploadVO uploadFile(MultipartFile file, String fileCategory) {
        try {
            initBucket();

            // 生成带日期前缀的唯一文件名，便于管理
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String uniqueId = UUID.randomUUID().toString().replaceAll("-", "");

            String fileKey = String.format("%s/%s/%s%s", fileCategory, datePrefix, uniqueId, extension);

            // 获取正确的内容类型
            String contentType = file.getContentType();

            // 对于 Markdown 文件，确保设置正确的内容类型
            if (extension.equalsIgnoreCase(".md")) {
                contentType = "text/markdown; charset=UTF-8";
            }

            // 创建额外的头信息和元数据
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", contentType);

            // 对于文本类型文件，添加字符编码
            if (contentType.startsWith("text/")) {
                headers.put("Content-Disposition", "inline");
            }

            // 上传文件到MinIO，同时设置元数据
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileKey)
                            .contentType(contentType)
                            .headers(headers)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .build()
            );

            log.info("文件上传成功: {}", fileKey);

            // 创建预签名URL用于访问文件
            String url = getPresignedUrl(fileKey);

            return FileUploadVO.builder()
                    .filename(originalFilename)
                    .fileKey(fileKey)
                    .fileSize(file.getSize())
                    .fileType(contentType)
                    .url(url)
                    .uploadTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .build();

        } catch (Exception e) {
            log.error("上传文件到MinIO失败", e);
            throw new FileOperationException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从MinIO获取文件
     * @param fileKey 文件标识符
     * @return 获取的文件
     */
    @Override
    public InputStream getFile(String fileKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("从MinIO获取文件失败: {}", fileKey, e);
            throw new FileOperationException("获取文件失败: " + fileKey, e);
        }
    }

    /**
     *  从MinIO删除文件
     * @param fileKey 文件标识符
     */
    @Override
    public void deleteFile(String fileKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileKey)
                            .build()
            );
            log.info("从MinIO删除文件: {}", fileKey);
        } catch (Exception e) {
            log.error("从MinIO删除文件失败: {}", fileKey, e);
            throw new FileOperationException("删除文件失败: " + fileKey, e);
        }
    }

    /**
     * 生成临时访问文件的预签名URL
     * @param fileKey 文件标识符
     * @return 预签名URL
     */
    @Override
    public String getPresignedUrl(String fileKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileKey)
                            .method(Method.GET)
                            .expiry(minioConfig.getPresignedExpiry(), TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("为文件生成预签名URL失败: {}", fileKey, e);
            throw new FileOperationException("为文件生成访问URL失败: " + fileKey, e);
        }
    }

    @Override
    public String getMinioEndpoint() {
        return minioConfig.getEndpoint();
    }
}