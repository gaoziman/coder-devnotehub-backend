package org.leocoder.devnote.hub.domain.vo.file;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-05-01 02:10
 * @description : 文件上传响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("文件上传响应")
public class FileUploadVO {

    /**
     * 文件名
     */
    @ApiModelProperty("原始文件名")
    private String originalFilename;

    /**
     * 文件大小(字节)
     */
    @ApiModelProperty("文件大小(字节)")
    private Long size;

    /**
     * 文件类型
     */
    @ApiModelProperty("文件类型")
    private String contentType;

    /**
     * 存储路径
     */
    @ApiModelProperty("存储路径")
    private String objectName;

    /**
     * 访问URL
     */
    @ApiModelProperty("访问URL")
    private String url;

    /**
     * 文件扩展名
     */
    @ApiModelProperty("文件扩展名")
    private String extension;
}