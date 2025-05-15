package org.leocoder.devnote.hub.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.devnote.hub.common.Result;
import org.leocoder.devnote.hub.common.ResultUtils;
import org.leocoder.devnote.hub.domain.vo.file.FileUploadVO;
import org.leocoder.devnote.hub.exception.BusinessException;
import org.leocoder.devnote.hub.exception.ErrorCode;
import org.leocoder.devnote.hub.service.impl.FileService;
import org.leocoder.devnote.hub.service.impl.MarkdownService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-05-01 02:04
 * @description : 文件管理
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Api(tags = "文件管理")
public class FileController {

    private final FileService fileService;
    private final MarkdownService markdownService;

    @ApiOperation("上传图片")
    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileUploadVO> uploadImage(@RequestParam("file") MultipartFile file) {
        log.info("上传图片: {}, 大小: {}", file.getOriginalFilename(), file.getSize());
        FileUploadVO result = fileService.uploadImage(file);
        return ResultUtils.success(result);
    }

    @ApiOperation("上传文档")
    @PostMapping(value = "/upload/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileUploadVO> uploadDocument(@RequestParam("file") MultipartFile file) {
        log.info("上传文档: {}, 大小: {}", file.getOriginalFilename(), file.getSize());
        FileUploadVO result = fileService.uploadDocument(file);
        return ResultUtils.success(result);
    }

    @ApiOperation("上传视频")
    @PostMapping(value = "/upload/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileUploadVO> uploadVideo(@RequestParam("file") MultipartFile file) {
        log.info("上传视频: {}, 大小: {}", file.getOriginalFilename(), file.getSize());
        FileUploadVO result = fileService.uploadVideo(file);
        return ResultUtils.success(result);
    }

    @ApiOperation("上传Markdown文件")
    @PostMapping(value = "/upload/markdown", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileUploadVO> uploadMarkdown(@RequestParam("file") MultipartFile file) {
        log.info("上传Markdown文件: {}, 大小: {}", file.getOriginalFilename(), file.getSize());

        // 验证文件扩展名
        String extension = getFileExtension(file.getOriginalFilename());
        if (!extension.equalsIgnoreCase("md") && !extension.equalsIgnoreCase("markdown")) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "不支持的文件类型，请上传Markdown文件");
        }

        // 处理并上传Markdown文件
        FileUploadVO result = markdownService.processAndUploadMarkdown(file);
        return ResultUtils.success(result);
    }

    @ApiOperation("删除文件")
    @DeleteMapping("")
    public Result<Boolean> deleteFile(@RequestParam("objectName") String objectName) {
        log.info("删除文件: {}", objectName);
        boolean result = fileService.deleteFile(objectName);
        return ResultUtils.success(result);
    }

    @ApiOperation("检查文件是否存在")
    @GetMapping("/exists")
    public Result<Boolean> isFileExist(@RequestParam("objectName") String objectName) {
        log.info("检查文件是否存在: {}", objectName);
        boolean exists = fileService.isFileExist(objectName);
        return ResultUtils.success(exists);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}