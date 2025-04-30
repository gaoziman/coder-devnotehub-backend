package org.leocoder.devnote.hub.controller;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.leocoder.devnote.hub.common.Result;
import org.leocoder.devnote.hub.common.ResultUtils;
import org.leocoder.devnote.hub.config.MinioConfig;
import org.leocoder.devnote.hub.domain.vo.file.FileUploadVO;
import org.leocoder.devnote.hub.enums.FileTypeEnum;
import org.leocoder.devnote.hub.exception.BusinessException;
import org.leocoder.devnote.hub.exception.ErrorCode;
import org.leocoder.devnote.hub.exception.ThrowUtils;
import org.leocoder.devnote.hub.service.impl.FileService;
import org.leocoder.devnote.hub.service.impl.MarkdownService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-05-01 02:04
 * @description :
 */
@Slf4j
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
@Api(tags = "文件管理")
public class FileController {

    private final FileService fileService;

    private final MinioConfig minioConfig;

    private final MarkdownService markdownService;

    private final MinioClient minioClient;

    @PostMapping("/upload/image")
    @ApiOperation("上传图片文件")
    public Result<FileUploadVO> uploadImage(
            @ApiParam(value = "图片文件", required = true)
            @RequestParam("file") MultipartFile file) {

        // 校验文件是否为空
        ThrowUtils.throwIf(file.isEmpty(), ErrorCode.PARAMS_ERROR, "上传的文件不能为空");

        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        ThrowUtils.throwIf(originalFilename == null, ErrorCode.PARAMS_ERROR, "文件名不能为空");

        // 验证文件类型
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        ThrowUtils.throwIf(!FileTypeEnum.isImageExtension(extension),
                ErrorCode.PARAMS_ERROR, "不支持的图片格式，仅支持: " + minioConfig.getAllowedImageExtensions());

        // 验证文件大小
        ThrowUtils.throwIf(file.getSize() > minioConfig.getMaxSize(),
                ErrorCode.PARAMS_ERROR, "图片大小不能超过" + (minioConfig.getMaxSize() / 1024 / 1024) + "MB");

        // 上传图片
        FileUploadVO result = fileService.uploadFile(file, "images");
        return ResultUtils.success(result);
    }

    @PostMapping("/upload/document")
    @ApiOperation("上传文档文件(PDF、Word、TXT等)")
    public Result<FileUploadVO> uploadDocument(
            @ApiParam(value = "文档文件", required = true)
            @RequestParam("file") MultipartFile file) {

        // 校验文件是否为空
        ThrowUtils.throwIf(file.isEmpty(), ErrorCode.PARAMS_ERROR, "上传的文件不能为空");

        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        ThrowUtils.throwIf(originalFilename == null, ErrorCode.PARAMS_ERROR, "文件名不能为空");

        // 验证文件类型
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        ThrowUtils.throwIf(!FileTypeEnum.isDocumentExtension(extension),
                ErrorCode.PARAMS_ERROR, "不支持的文档格式，仅支持: " + minioConfig.getAllowedDocumentExtensions());

        // 验证文件大小
        ThrowUtils.throwIf(file.getSize() > minioConfig.getMaxSize(),
                ErrorCode.PARAMS_ERROR, "文件大小不能超过" + (minioConfig.getMaxSize() / 1024 / 1024) + "MB");

        // 上传文档
        FileUploadVO result = fileService.uploadFile(file, "documents");
        return ResultUtils.success(result);
    }

    @GetMapping("/download/{fileKey}")
    @ApiOperation("通过文件键下载文件")
    public void downloadFile(
            @PathVariable String fileKey,
            HttpServletResponse response) {
        try {
            InputStream fileStream = fileService.getFile(fileKey);

            // 设置响应头
            String filename = fileKey.substring(fileKey.lastIndexOf('/') + 1);
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8.toString()));

            // 将文件数据复制到响应输出流
            IOUtils.copy(fileStream, response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            log.error("下载文件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载文件失败");
        }
    }

    @GetMapping("/preview")
    @ApiOperation("使用查询参数预览文件")
    public void previewFileByQuery(
            @RequestParam String fileKey,
            HttpServletResponse response) {
        try {
            log.info("使用查询参数预览文件: {}", fileKey);
            InputStream fileStream = fileService.getFile(fileKey);

            // 获取文件类型
            String contentType = FileTypeEnum.getMimeTypeByFilename(fileKey);
            response.setContentType(contentType);

            // 扩展条件：图片、PDF、文本类型、Markdown都使用内联显示
            if (contentType.startsWith("image/") ||
                    contentType.equals("application/pdf") ||
                    contentType.startsWith("text/")) {
                // 内联显示
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline");
            } else {
                // 其他类型强制下载
                String filename = fileKey.substring(fileKey.lastIndexOf('/') + 1);
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
            }

            IOUtils.copy(fileStream, response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            log.error("预览文件失败: {}", fileKey, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "预览文件失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{fileKey}")
    @ApiOperation("删除文件")
    public Result<Void> deleteFile(@PathVariable String fileKey) {
        fileService.deleteFile(fileKey);
        return ResultUtils.success(null);
    }

    @PostMapping("/process-markdown")
    @ApiOperation("处理 Markdown 文件中的图片并替换 URL")
    public Result<FileUploadVO> processMarkdownFile(
            @ApiParam(value = "Markdown 文件", required = true)
            @RequestParam("file") MultipartFile file) {

        // 校验文件是否为空
        ThrowUtils.throwIf(file.isEmpty(), ErrorCode.PARAMS_ERROR, "上传的文件不能为空");

        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        ThrowUtils.throwIf(originalFilename == null, ErrorCode.PARAMS_ERROR, "文件名不能为空");

        // 验证文件是否为 Markdown
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        ThrowUtils.throwIf(!extension.equals("md"), ErrorCode.PARAMS_ERROR,
                "不支持的文件格式，仅支持 Markdown (.md) 文件");

        try {
            // 读取 Markdown 内容
            String markdownContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 处理 Markdown 中的图片
            String processedContent = markdownService.processMarkdownImages(markdownContent);

            // 创建处理后的文件
            MultipartFile processedFile = new MockMultipartFile(
                    originalFilename,
                    originalFilename,
                    "text/markdown",
                    processedContent.getBytes(StandardCharsets.UTF_8)
            );

            // 上传处理后的文件
            FileUploadVO result = fileService.uploadFile(processedFile, "documents");
            return ResultUtils.success(result);
        } catch (Exception e) {
            log.error("处理 Markdown 文件图片失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "处理 Markdown 文件失败: " + e.getMessage());
        }
    }

    @PostMapping("/process-existing-markdown/{fileKey:.+}")
    @ApiOperation("处理已上传的 Markdown 文件中的图片")
    public Result<FileUploadVO> processExistingMarkdown(@PathVariable String fileKey) {
        try {
            log.info("开始处理已上传的 Markdown 文件: {}", fileKey);

            // 获取现有 Markdown 文件
            InputStream fileStream = fileService.getFile(fileKey);
            String markdownContent = new String(fileStream.readAllBytes(), StandardCharsets.UTF_8);

            // 处理图片
            String processedContent = markdownService.processMarkdownImages(markdownContent);

            // 如果内容未变化，直接返回
            if (markdownContent.equals(processedContent)) {
                log.info("Markdown 内容未变化，无需更新");

                // 构建响应
                String filename = fileKey.substring(fileKey.lastIndexOf('/') + 1);
                FileUploadVO result = FileUploadVO.builder()
                        .filename(filename)
                        .fileKey(fileKey)
                        .fileSize(markdownContent.getBytes(StandardCharsets.UTF_8).length)
                        .fileType("text/markdown")
                        .url(fileService.getPresignedUrl(fileKey))
                        .uploadTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .build();

                return ResultUtils.success(result);
            }

            // 上传处理后的内容，覆盖原文件
            ByteArrayInputStream inputStream = new ByteArrayInputStream(
                    processedContent.getBytes(StandardCharsets.UTF_8));

            // 删除旧文件
            fileService.deleteFile(fileKey);

            // 上传新文件（使用相同的 fileKey）
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileKey)
                            .contentType("text/markdown")
                            .stream(inputStream, processedContent.getBytes(StandardCharsets.UTF_8).length, -1)
                            .build()
            );

            log.info("已完成 Markdown 文件处理并重新上传");

            // 构建响应
            String filename = fileKey.substring(fileKey.lastIndexOf('/') + 1);
            FileUploadVO result = FileUploadVO.builder()
                    .filename(filename)
                    .fileKey(fileKey)
                    .fileSize(processedContent.getBytes(StandardCharsets.UTF_8).length)
                    .fileType("text/markdown")
                    .url(fileService.getPresignedUrl(fileKey))
                    .uploadTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .build();

            return ResultUtils.success(result);
        } catch (Exception e) {
            log.error("处理已上传的 Markdown 文件失败: {}", fileKey, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "处理 Markdown 文件失败: " + e.getMessage());
        }
    }
}
