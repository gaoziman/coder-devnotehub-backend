package org.leocoder.devnote.hub.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.devnote.hub.domain.vo.file.FileUploadVO;
import org.leocoder.devnote.hub.exception.BusinessException;
import org.leocoder.devnote.hub.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-05-01 04:14
 * @description : Markdown处理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarkdownService {

    private final FileService fileService;
    private final RestTemplate restTemplate;

    // Base64编码图片的正则表达式模式
    private static final Pattern BASE64_IMAGE_PATTERN =
            Pattern.compile("!\\[(.*?)\\]\\(data:image/(.*?);base64,(.*?)\\)");

    // 外部URL图片的正则表达式模式 - 包含HTTP/HTTPS链接
    private static final Pattern URL_IMAGE_PATTERN =
            Pattern.compile("!\\[(.*?)\\]\\((https?://.*?\\.(png|jpg|jpeg|gif|webp|bmp))\\)");

    // 匹配形如 ![Image-20240202095614159](https://gaoziman.oss-cn-hangzhou.aliyuncs.com/LeoPic20240202095654.png) 的模式
    private static final Pattern SPECIAL_IMAGE_PATTERN =
            Pattern.compile("!\\[(Image-\\d+)\\]\\((https?://.*?\\.(png|jpg|jpeg|gif|webp|bmp))\\)");

    /**
     * 处理Markdown文件并上传
     *
     * @param file Markdown文件
     * @return 上传后的文件信息
     */
    public FileUploadVO processAndUploadMarkdown(MultipartFile file) {
        try {
            // 强制使用UTF-8读取文件内容，避免编码检测可能导致的问题
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            log.info("成功读取Markdown文件，大小: {}", content.length());

            // 处理Markdown中的图片
            String processedContent = processMarkdownImages(content);
            log.info("处理完成，处理后内容大小: {}", processedContent.length());

            // 将处理后的内容转为字节数组，使用UTF-8编码
            byte[] processedBytes = processedContent.getBytes(StandardCharsets.UTF_8);

            // 准备上传参数
            String fileName = file.getOriginalFilename();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(processedBytes);

            // 上传处理后的Markdown文件
            return fileService.uploadFile(
                    inputStream,
                    fileName,
                    "text/markdown; charset=utf-8", // 明确指定MIME类型和字符集
                    processedBytes.length
            );

        } catch (IOException e) {
            log.error("处理Markdown文件失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "处理Markdown文件失败: " + e.getMessage());
        }
    }

    /**
     * 处理Markdown中的图片
     *
     * @param content Markdown内容
     * @return 处理后的Markdown内容
     */
    private String processMarkdownImages(String content) {
        // 依次处理不同类型的图片
        String result = processBase64Images(content);
        result = processURLImages(result);
        result = processSpecialImages(result);

        return result;
    }

    /**
     * 处理Base64编码的图片
     */
    private String processBase64Images(String content) {
        Matcher matcher = BASE64_IMAGE_PATTERN.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String altText = matcher.group(1);
            String imageType = matcher.group(2);
            String base64Data = matcher.group(3);

            try {
                // 解码Base64数据
                byte[] imageData = Base64.getDecoder().decode(base64Data);

                // 生成临时文件名
                String fileName = UUID.randomUUID().toString() + "." + imageType;

                // 上传到MinIO
                ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
                FileUploadVO uploadResult = fileService.uploadFile(
                        inputStream,
                        fileName,
                        "image/" + imageType,
                        imageData.length
                );

                // 替换Markdown中的图片引用
                matcher.appendReplacement(sb, "![" + altText + "](" + uploadResult.getUrl() + ")");
                log.info("Base64图片已替换为: {}", uploadResult.getUrl());

            } catch (Exception e) {
                log.error("处理Base64图片失败: {}", e.getMessage(), e);
                // 如果处理失败，保留原始内容
                matcher.appendReplacement(sb, matcher.group(0));
            }
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 处理外部URL图片
     */
    private String processURLImages(String content) {
        Matcher matcher = URL_IMAGE_PATTERN.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String altText = matcher.group(1);
            String imageUrl = matcher.group(2);
            String extension = matcher.group(3).toLowerCase();

            try {
                // 下载图片
                log.info("开始下载图片: {}", imageUrl);
                byte[] imageData = downloadImage(imageUrl);

                if (imageData != null && imageData.length > 0) {
                    // 生成文件名
                    String fileName = UUID.randomUUID().toString() + "." + extension;

                    // 上传到MinIO
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
                    FileUploadVO uploadResult = fileService.uploadFile(
                            inputStream,
                            fileName,
                            "image/" + getContentTypeByExtension(extension),
                            imageData.length
                    );

                    // 替换Markdown中的图片引用
                    matcher.appendReplacement(sb, "![" + altText + "](" + uploadResult.getUrl() + ")");
                    log.info("外部URL图片已替换为: {}", uploadResult.getUrl());
                } else {
                    // 如果下载失败，保留原始内容
                    matcher.appendReplacement(sb, matcher.group(0));
                    log.warn("无法下载图片: {}", imageUrl);
                }
            } catch (Exception e) {
                log.error("处理URL图片失败: {}, 错误: {}", imageUrl, e.getMessage(), e);
                // 如果处理失败，保留原始内容
                matcher.appendReplacement(sb, matcher.group(0));
            }
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 处理特殊形式的图片（例如截图中的格式）
     */
    private String processSpecialImages(String content) {
        Matcher matcher = SPECIAL_IMAGE_PATTERN.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String altText = matcher.group(1);
            String imageUrl = matcher.group(2);
            String extension = matcher.group(3).toLowerCase();

            try {
                // 下载图片
                log.info("开始下载特殊格式图片: {}", imageUrl);
                byte[] imageData = downloadImage(imageUrl);

                if (imageData != null && imageData.length > 0) {
                    // 生成文件名，保留原始文件名的特殊格式
                    String fileName = altText + "." + extension;

                    // 上传到MinIO
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
                    FileUploadVO uploadResult = fileService.uploadFile(
                            inputStream,
                            fileName,
                            "image/" + getContentTypeByExtension(extension),
                            imageData.length
                    );

                    // 替换Markdown中的图片引用
                    matcher.appendReplacement(sb, "![" + altText + "](" + uploadResult.getUrl() + ")");
                    log.info("特殊格式图片已替换为: {}", uploadResult.getUrl());
                } else {
                    // 如果下载失败，保留原始内容
                    matcher.appendReplacement(sb, matcher.group(0));
                    log.warn("无法下载特殊格式图片: {}", imageUrl);
                }
            } catch (Exception e) {
                log.error("处理特殊格式图片失败: {}, 错误: {}", imageUrl, e.getMessage(), e);
                // 如果处理失败，保留原始内容
                matcher.appendReplacement(sb, matcher.group(0));
            }
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 下载图片
     *
     * @param imageUrl 图片URL
     * @return 图片字节数组
     */
    private byte[] downloadImage(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpStatus.OK.value()) {
                // 读取图片数据
                try (ByteArrayInputStream bis = new ByteArrayInputStream(connection.getInputStream().readAllBytes())) {
                    return bis.readAllBytes();
                }
            } else {
                log.error("下载图片失败, HTTP状态码: {}", responseCode);
                return null;
            }
        } catch (Exception e) {
            log.error("下载图片出错: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 根据扩展名获取内容类型
     */
    private String getContentTypeByExtension(String extension) {
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return "jpeg";
            case "png":
                return "png";
            case "gif":
                return "gif";
            case "webp":
                return "webp";
            case "bmp":
                return "bmp";
            default:
                return "jpeg"; // 默认类型
        }
    }
}