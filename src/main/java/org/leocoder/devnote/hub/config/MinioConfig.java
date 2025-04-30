package org.leocoder.devnote.hub.config;

import io.minio.MinioClient;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.leocoder.devnote.hub.enums.FileTypeEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-05-01 01:54
 * @description : MinIO 配置类
 */

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {
    @ApiModelProperty(value = "MinIO 服务器地址")
    private String endpoint;

    @ApiModelProperty(value = "MinIO 访问密钥")
    private String accessKey;

    @ApiModelProperty(value = "MinIO 访问秘密密钥")
    private String secretKey;

    @ApiModelProperty(value = "MinIO 存储桶名称")
    private String bucketName;

    @ApiModelProperty(value = "MinIO 上传文件时，是否使用预签名URL")
    private int presignedExpiry;

    @ApiModelProperty(value = "MinIO 上传文件最大大小")
    private long maxSize;

    @ApiModelProperty(value = "允许上传的图片后缀")
    private String allowedImageExtensions;

    @ApiModelProperty(value = "允许上传的文档后缀")
    private String allowedDocumentExtensions;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * 获取允许的图片扩展名列表
     * @return 逗号分隔的扩展名列表
     */
    public String getDefaultAllowedImageExtensions() {
        return Arrays.stream(FileTypeEnum.values())
                .filter(fileType -> fileType.getMimeType().startsWith("image/"))
                .map(FileTypeEnum::getExtension)
                .filter(ext -> !ext.isEmpty())
                .collect(Collectors.joining(","));
    }

    /**
     * 获取允许的文档扩展名列表
     * @return 逗号分隔的扩展名列表
     */
    public String getDefaultAllowedDocumentExtensions() {
        return Arrays.stream(FileTypeEnum.values())
                .filter(fileType -> !fileType.getMimeType().startsWith("image/") && fileType != FileTypeEnum.UNKNOWN)
                .map(FileTypeEnum::getExtension)
                .filter(ext -> !ext.isEmpty())
                .collect(Collectors.joining(","));
    }
}