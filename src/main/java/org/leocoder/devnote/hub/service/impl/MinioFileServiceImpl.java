package org.leocoder.devnote.hub.service.impl;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.devnote.hub.config.MinioConfig;
import org.leocoder.devnote.hub.domain.vo.file.FileUploadVO;
import org.leocoder.devnote.hub.enums.FileTypeEnum;
import org.leocoder.devnote.hub.exception.BusinessException;
import org.leocoder.devnote.hub.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    // 日期格式化器，用于生成文件存储路径
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");


    /**
     * 上传文件
     *
     * @param file     上传的文件
     * @param mimeType 文件MIME类型
     * @return 文件上传响应对象
     */
    @Override
    public FileUploadVO uploadFile(MultipartFile file, String mimeType) {
        // 获取原始文件名和扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "文件名不能为空");
        }

        // 获取文件扩展名
        String extension = getFileExtension(originalFilename);
        if (extension.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "不支持的文件类型");
        }

        // 验证文件扩展名是否在允许列表中
        if (!minioConfig.getAllAllowedExtensions().contains(extension.toLowerCase())) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "不支持的文件类型: " + extension);
        }

        // 验证文件大小
        if (file.getSize() > minioConfig.getMaxSize()) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR,
                    "文件大小超出限制，最大允许: " + (minioConfig.getMaxSize() / 1024 / 1024) + "MB");
        }

        try {
            // 生成存储对象名
            String objectName = generateObjectName(extension);
            // 设置文件元数据
            Map<String, String> userMetadata = new HashMap<>();
            userMetadata.put("originalFilename", originalFilename);

            // 上传文件到MinIO
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .contentType(mimeType)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .userMetadata(userMetadata)
                    .build());

            // 获取文件访问URL
            String url = getFileUrl(objectName, -1);

            // 构建并返回上传响应对象
            return FileUploadVO.builder()
                    .originalFilename(originalFilename)
                    .size(file.getSize())
                    .contentType(mimeType)
                    .objectName(objectName)
                    .url(url)
                    .extension(extension)
                    .build();
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.UPLOAD_FAILURE, "文件上传失败: " + e.getMessage());
        }
    }


    /**
     * 上传图片
     *
     * @param file 图片文件
     * @return 文件上传响应对象
     */
    @Override
    public FileUploadVO uploadImage(MultipartFile file) {
        // 获取文件扩展名
        String extension = getFileExtension(file.getOriginalFilename());

        // 验证是否为允许的图片类型
        if (!minioConfig.getAllowedImageExtensionList().contains(extension.toLowerCase())) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "不支持的图片类型: " + extension);
        }

        // 获取图片的MIME类型
        String mimeType = FileTypeEnum.getMimeTypeByExtension(extension);

        // 调用通用上传方法
        return uploadFile(file, mimeType);
    }


    /**
     * 上传文档
     *
     * @param file 文档文件
     * @return 文件上传响应对象
     */
    @Override
    public FileUploadVO uploadDocument(MultipartFile file) {
        // 获取文件扩展名
        String extension = getFileExtension(file.getOriginalFilename());

        // 验证是否为允许的文档类型
        if (!minioConfig.getAllowedDocumentExtensionList().contains(extension.toLowerCase())) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "不支持的文档类型: " + extension);
        }

        // 获取文档的MIME类型
        String mimeType = FileTypeEnum.getMimeTypeByExtension(extension);

        // 调用通用上传方法
        return uploadFile(file, mimeType);
    }



    /**
     * 通过InputStream上传文件
     *
     * @param inputStream 输入流
     * @param fileName    文件名
     * @param contentType 内容类型
     * @param size        文件大小
     * @return 文件上传响应对象
     */
    @Override
    public FileUploadVO uploadFile(InputStream inputStream, String fileName, String contentType, long size) {
        // 获取文件扩展名
        String extension = getFileExtension(fileName);
        if (extension.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "不支持的文件类型");
        }

        // 验证文件扩展名是否在允许列表中
        if (!minioConfig.getAllAllowedExtensions().contains(extension.toLowerCase())) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "不支持的文件类型: " + extension);
        }

        // 验证文件大小
        if (size > minioConfig.getMaxSize()) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR,
                    "文件大小超出限制，最大允许: " + (minioConfig.getMaxSize() / 1024 / 1024) + "MB");
        }

        try {
            // 生成存储对象名
            String objectName = generateObjectName(extension);
            // 设置文件元数据
            Map<String, String> userMetadata = new HashMap<>();
            userMetadata.put("originalFilename", fileName);

            // 上传文件到MinIO
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .contentType(contentType)
                    .stream(inputStream, size, -1)
                    .userMetadata(userMetadata)
                    .build());

            // 获取文件访问URL
            String url = getFileUrl(objectName, -1);

            // 构建并返回上传响应对象
            return FileUploadVO.builder()
                    .originalFilename(fileName)
                    .size(size)
                    .contentType(contentType)
                    .objectName(objectName)
                    .url(url)
                    .extension(extension)
                    .build();
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.UPLOAD_FAILURE, "文件上传失败: " + e.getMessage());
        }
    }


    /**
     * 删除文件
     *
     * @param objectName 对象名称
     * @return 是否删除成功
     */
    @Override
    public boolean deleteFile(String objectName) {
        try {
            // 检查文件是否存在
            if (!isFileExist(objectName)) {
                return false;
            }

            // 删除MinIO中的文件
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .build());

            return true;
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.DELETE_FAILURE, "文件删除失败: " + e.getMessage());
        }
    }


    /**
     * 获取文件访问URL
     *
     * @param objectName 对象名称
     * @param expiry     过期时间(秒)，-1表示永不过期
     * @return 文件访问URL
     */
    @Override
    public String getFileUrl(String objectName, int expiry) {
        try {
            String endpoint = minioConfig.getEndpoint();
            String bucketName = minioConfig.getBucketName();

            // 去除endpoint末尾的斜杠（如果有）
            if (endpoint.endsWith("/")) {
                endpoint = endpoint.substring(0, endpoint.length() - 1);
            }

            // 构建简单直接访问URL
            return String.format("%s/%s/%s", endpoint, bucketName, objectName);
        } catch (Exception e) {
            log.error("获取文件URL失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取文件URL失败: " + e.getMessage());
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param objectName 对象名称
     * @return 是否存在
     */
    @Override
    public boolean isFileExist(String objectName) {
        try {
            // 尝试获取文件统计信息，如果存在则返回true
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .build());
            return true;
        } catch (Exception e) {
            // 如果文件不存在会抛出异常，返回false
            return false;
        }
    }

    /**
     * 生成文件存储路径
     * 格式：yyyy/MM/dd/文件类型/uuid.扩展名
     * 例如：2025/05/11/images/550e8400-e29b-41d4-a716-446655440000.jpg
     *
     * @param extension 文件扩展名
     * @return 生成的对象名
     */
    private String generateObjectName(String extension) {
        // 获取当前日期
        String dateDir = LocalDateTime.now().format(DATE_FORMATTER);

        // 确定文件类型目录
        String typeDir;
        if (FileTypeEnum.isImageExtension(extension)) {
            typeDir = "images";
        } else if (FileTypeEnum.isVideoExtension(extension)) {
            typeDir = "videos";
        } else if (FileTypeEnum.isDocumentExtension(extension)) {
            typeDir = "documents";
        } else {
            typeDir = "others";
        }

        // 生成UUID作为文件名
        String uuid = UUID.randomUUID().toString();

        // 组合生成最终的对象名
        return String.format("%s/%s/%s.%s", dateDir, typeDir, uuid, extension.toLowerCase());
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名（不包含点）
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty() || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * 创建MinIO存储桶（如果不存在）
     * 此方法可在应用启动时调用，确保存储桶存在
     */
    public void createBucketIfNotExist() {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .build());

            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .build());
                log.info("成功创建存储桶: {}", minioConfig.getBucketName());

                // 设置桶策略，使其对象可公开访问
                setBucketPolicy();
            }
        } catch (Exception e) {
            log.error("创建存储桶失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建存储桶失败: " + e.getMessage());
        }
    }

    /**
     * 设置存储桶策略，允许公开读取
     */
    private void setBucketPolicy() {
        try {
            // 允许公开读取桶中所有对象的策略
            String policy = "{\n" +
                    "    \"Version\": \"2012-10-17\",\n" +
                    "    \"Statement\": [\n" +
                    "        {\n" +
                    "            \"Effect\": \"Allow\",\n" +
                    "            \"Principal\": {\"AWS\": [\"*\"]},\n" +
                    "            \"Action\": [\"s3:GetObject\"],\n" +
                    "            \"Resource\": [\"arn:aws:s3:::" + minioConfig.getBucketName() + "/*\"]\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}";

            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .config(policy)
                    .build());

            log.info("成功设置桶策略: {}", minioConfig.getBucketName());
        } catch (Exception e) {
            log.error("设置桶策略失败: {}", e.getMessage(), e);
        }
    }


    /**
     * 获取MinIO服务端点URL
     *
     * @return MinIO端点URL
     */
    @Override
    public String getMinioEndpoint() {
        return minioConfig.getEndpoint();
    }
}