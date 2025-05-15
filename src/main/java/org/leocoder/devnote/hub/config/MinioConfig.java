package org.leocoder.devnote.hub.config;

import io.minio.MinioClient;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    /**
     * MinIO服务地址
     */
    @ApiModelProperty("MinIO服务地址")
    private String endpoint;

    /**
     * 访问密钥
     */
    @ApiModelProperty("访问密钥")
    private String accessKey;

    /**
     * 秘密密钥
     */
    @ApiModelProperty("秘密密钥")
    private String secretKey;

    /**
     * 存储桶名称
     */
    @ApiModelProperty("存储桶名称")
    private String bucketName;

    /**
     * 最大文件大小（字节）
     */
    @ApiModelProperty("最大文件大小（字节）")
    private Long maxSize;

    /**
     * 允许上传的图片扩展名
     */
    @ApiModelProperty("允许上传的图片扩展名")
    private String allowedImageExtensions;

    /**
     * 允许上传的文档扩展名
     */
    @ApiModelProperty("允许上传的文档扩展名")
    private String allowedDocumentExtensions;

    /**
     * 允许上传的视频扩展名
     */
    @ApiModelProperty("允许上传的视频扩展名")
    private String allowedVideoExtensions = "mp4,avi,mov,wmv,flv,mkv";

    /**
     * 获取允许的图片扩展名列表
     */
    public List<String> getAllowedImageExtensionList() {
        return Arrays.stream(allowedImageExtensions.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * 获取允许的文档扩展名列表
     */
    public List<String> getAllowedDocumentExtensionList() {
        return Arrays.stream(allowedDocumentExtensions.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * 获取允许的视频扩展名列表
     */
    public List<String> getAllowedVideoExtensionList() {
        return Arrays.stream(allowedVideoExtensions.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有允许的文件扩展名列表
     */
    public List<String> getAllAllowedExtensions() {
        return Stream.concat(
                Stream.concat(
                        getAllowedImageExtensionList().stream(),
                        getAllowedDocumentExtensionList().stream()
                ),
                getAllowedVideoExtensionList().stream()
        ).collect(Collectors.toList());
    }

    /**
     * 创建MinioClient Bean
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}