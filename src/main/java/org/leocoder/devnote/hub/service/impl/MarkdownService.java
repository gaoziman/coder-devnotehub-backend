package org.leocoder.devnote.hub.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.devnote.hub.domain.vo.file.FileUploadVO;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-05-01 04:14
 * @description :
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarkdownService {

    private final FileService fileService;

    // Markdown 图片链接正则表达式（支持两种常见格式）
    private static final Pattern MD_IMAGE_PATTERN = Pattern.compile("!\\[(.*?)\\]\\((.*?)\\)");
    private static final Pattern HTML_IMAGE_PATTERN = Pattern.compile("<img\\s+[^>]*src=[\"']([^\"']+)[\"'][^>]*>");

    /**
     * 处理 Markdown 文件中的图片：提取、下载、上传到 MinIO 并替换 URL
     *
     * @param markdownContent 原始 Markdown 内容
     * @return 处理后的 Markdown 内容（图片 URL 已更新）
     */
    public String processMarkdownImages(String markdownContent) {
        // 提取所有图片 URL
        List<String> imageUrls = extractImageUrls(markdownContent);

        if (imageUrls.isEmpty()) {
            log.info("Markdown 中未找到图片");
            return markdownContent;
        }

        log.info("在 Markdown 中找到 {} 个图片", imageUrls.size());

        // 处理每个图片
        for (String imageUrl : imageUrls) {
            try {
                // 过滤掉已经是系统内部 URL 的图片
                if (imageUrl.contains(fileService.getMinioEndpoint())) {
                    log.info("图片已在 MinIO 中，跳过处理: {}", imageUrl);
                    continue;
                }

                // 下载并上传图片
                FileUploadVO uploadedImage = downloadAndUploadImage(imageUrl);

                // 在 Markdown 中替换旧 URL 为新 URL
                markdownContent = replaceImageUrl(markdownContent, imageUrl, uploadedImage.getUrl());

                log.info("已替换图片 URL: {} -> {}", imageUrl, uploadedImage.getUrl());
            } catch (Exception e) {
                log.error("处理图片失败: {}, 错误: {}", imageUrl, e.getMessage());
                // 即使某个图片处理失败，也继续处理其他图片
            }
        }

        return markdownContent;
    }

    /**
     * 从 Markdown 内容中提取所有图片 URL
     */
    private List<String> extractImageUrls(String markdownContent) {
        List<String> urls = new ArrayList<>();

        // 匹配 Markdown 风格图片链接: ![alt](url)
        Matcher mdMatcher = MD_IMAGE_PATTERN.matcher(markdownContent);
        while (mdMatcher.find()) {
            urls.add(mdMatcher.group(2));
        }

        // 匹配 HTML 风格图片标签: <img src="url" />
        Matcher htmlMatcher = HTML_IMAGE_PATTERN.matcher(markdownContent);
        while (htmlMatcher.find()) {
            urls.add(htmlMatcher.group(1));
        }

        return urls;
    }

    /**
     * 从 URL 下载图片并上传到 MinIO
     */
    private FileUploadVO downloadAndUploadImage(String imageUrl) throws IOException {
        // 下载图片
        URL url = new URL(imageUrl);
        URLConnection connection = url.openConnection();
        // 设置请求头，避免某些站点的访问限制
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        // 确定文件类型和名称
        String contentType = connection.getContentType();
        String filename = getFilenameFromUrl(imageUrl, contentType);

        try (InputStream inputStream = connection.getInputStream()) {
            // 读取图片数据
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            // 创建 MultipartFile 对象
            MultipartFile multipartFile = new MockMultipartFile(
                    filename,
                    filename,
                    contentType,
                    outputStream.toByteArray()
            );

            // 上传到 MinIO
            return fileService.uploadFile(multipartFile, "images");
        }
    }

    /**
     * 从图片 URL 中提取文件名
     */
    private String getFilenameFromUrl(String imageUrl, String contentType) {
        // 尝试从 URL 提取文件名
        String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);

        // 移除查询参数
        if (filename.contains("?")) {
            filename = filename.substring(0, filename.indexOf('?'));
        }

        // 如果文件名为空或没有扩展名，生成一个新的
        if (filename.isEmpty() || !filename.contains(".")) {
            String extension = ".jpg"; // 默认扩展名

            // 根据内容类型确定扩展名
            if (contentType != null) {
                if (contentType.contains("png")) {
                    extension = ".png";
                } else if (contentType.contains("gif")) {
                    extension = ".gif";
                } else if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                    extension = ".jpg";
                } else if (contentType.contains("webp")) {
                    extension = ".webp";
                }
            }

            // 生成随机文件名
            filename = UUID.randomUUID().toString().replace("-", "") + extension;
        }

        return filename;
    }

    /**
     * 在 Markdown 内容中替换图片 URL
     */
    private String replaceImageUrl(String markdownContent, String oldUrl, String newUrl) {
        // 转义 URL 中的特殊字符，避免正则表达式问题
        String escapedOldUrl = oldUrl.replaceAll("([\\\\*+\\[\\](){}\\|^$.?])", "\\\\$1");

        // 替换 Markdown 格式中的 URL
        String updated = markdownContent.replaceAll(
                "!\\[(.*?)\\]\\(" + escapedOldUrl + "\\)",
                "![$1](" + newUrl + ")"
        );

        // 替换 HTML 格式中的 URL
        return updated.replaceAll(
                "<img\\s+([^>]*)src=[\"']" + escapedOldUrl + "[\"']([^>]*)>",
                "<img $1src=\"" + newUrl + "\"$2>"
        );
    }
}
