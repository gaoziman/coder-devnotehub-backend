package org.leocoder.devnote.hub.domain.vo.file;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-05-14
 * @description : Markdown处理响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Markdown处理响应")
public class MarkdownProcessVO {

    /**
     * 原始Markdown内容
     */
    @ApiModelProperty("原始Markdown内容")
    private String originalContent;

    /**
     * 处理后的Markdown内容
     */
    @ApiModelProperty("处理后的Markdown内容（图片URL已替换）")
    private String processedContent;

    /**
     * 文件信息
     */
    @ApiModelProperty("Markdown文件信息")
    private FileUploadVO fileInfo;

    /**
     * 处理的图片数量
     */
    @ApiModelProperty("处理的图片数量")
    private int imageCount;

    /**
     * 图片URL列表
     */
    @ApiModelProperty("处理后的图片URL列表")
    private List<String> imageUrls;
}