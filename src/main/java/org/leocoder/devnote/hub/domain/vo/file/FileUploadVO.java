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
 * @date 2025-05-01 01:57
 * @description :
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "文件上传VO")
public class FileUploadVO {

    @ApiModelProperty(value = "原始文件名")
    private String filename;

    @ApiModelProperty(value = "文件存储键/路径")
    private String fileKey;

    @ApiModelProperty(value = "文件大小(字节)")
    private long fileSize;

    @ApiModelProperty(value = "文件MIME类型")
    private String fileType;

    @ApiModelProperty(value = "预签名访问URL")
    private String url;

    @ApiModelProperty(value = "上传时间")
    private String uploadTime;
}
