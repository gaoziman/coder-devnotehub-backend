package org.leocoder.devnote.hub.enums;


import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-05-01 02:15
 * @description : 文件类型枚举
 */
@Getter
public enum FileTypeEnum {

    // 图片类型
    JPG("jpg", "image/jpeg"),

    JPEG("jpeg", "image/jpeg"),

    PNG("png", "image/png"),

    GIF("gif", "image/gif"),

    WEBP("webp", "image/webp"),

    HEIC("heic", "image/heic"),

    // 文档类型
    PDF("pdf", "application/pdf"),

    DOC("doc", "application/msword"),

    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),

    TXT("txt", "text/plain"),

    MD("md", "text/markdown"),

    // 电子表格
    XLS("xls", "application/vnd.ms-excel"),

    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),

    // 演示文稿
    PPT("ppt", "application/vnd.ms-powerpoint"),

    PPTX("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),

    // 默认类型
    UNKNOWN("", "application/octet-stream");

    private final String extension;

    private final String mimeType;

    // 静态映射表，用于快速查找
    private static final Map<String, FileTypeEnum> EXTENSION_MAP;

    // 静态初始化块，构建映射表
    static {
        EXTENSION_MAP = Arrays.stream(FileTypeEnum.values())
                .collect(Collectors.toMap(
                        FileTypeEnum::getExtension,
                        fileType -> fileType,
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 构造函数
     * @param extension 文件扩展名（不含点）
     * @param mimeType MIME类型
     */
    FileTypeEnum(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    /**
     * 根据文件扩展名获取对应的FileType
     * @param extension 文件扩展名（不含点）
     * @return 对应的FileType，如果未找到则返回UNKNOWN
     */
    public static FileTypeEnum getByExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return UNKNOWN;
        }
        return EXTENSION_MAP.getOrDefault(extension.toLowerCase(), UNKNOWN);
    }

    /**
     * 根据文件扩展名获取对应的MIME类型
     * @param extension 文件扩展名（不含点）
     * @return 对应的MIME类型
     */
    public static String getMimeTypeByExtension(String extension) {
        return getByExtension(extension).getMimeType();
    }

    /**
     * 根据文件名获取对应的MIME类型
     * @param filename 完整文件名
     * @return 对应的MIME类型
     */
    public static String getMimeTypeByFilename(String filename) {
        if (filename == null || filename.isEmpty() || !filename.contains(".")) {
            return UNKNOWN.getMimeType();
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1);
        return getMimeTypeByExtension(extension);
    }

    /**
     * 判断文件扩展名是否为图片类型
     * @param extension 文件扩展名
     * @return 是否为图片类型
     */
    public static boolean isImageExtension(String extension) {
        FileTypeEnum fileType = getByExtension(extension);
        return fileType.getMimeType().startsWith("image/");
    }

    /**
     * 判断文件扩展名是否为文档类型
     * @param extension 文件扩展名
     * @return 是否为文档类型
     */
    public static boolean isDocumentExtension(String extension) {
        FileTypeEnum fileType = getByExtension(extension);
        return fileType != UNKNOWN && !fileType.getMimeType().startsWith("image/");
    }
}
