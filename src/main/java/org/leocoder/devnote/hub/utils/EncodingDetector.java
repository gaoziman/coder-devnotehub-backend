package org.leocoder.devnote.hub.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-05-14
 * @description : 文件编码检测工具类
 */
@Slf4j
public class EncodingDetector {

    // 常见编码的BOM标记
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] UTF16LE_BOM = {(byte) 0xFF, (byte) 0xFE};
    private static final byte[] UTF16BE_BOM = {(byte) 0xFE, (byte) 0xFF};
    private static final byte[] UTF32LE_BOM = {(byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x00};
    private static final byte[] UTF32BE_BOM = {(byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0xFF};

    /**
     * 自动检测文本内容的编码
     *
     * @param inputStream 输入流
     * @return 检测到的字符集
     */
    public static Charset detectEncoding(InputStream inputStream) throws IOException {
        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream);
        }
        
        // 标记当前位置，以便回退
        inputStream.mark(4);
        
        // 读取前4个字节用于检测BOM
        byte[] bom = new byte[4];
        int read = inputStream.read(bom);
        
        // 重置到开始位置
        inputStream.reset();
        
        // 根据BOM判断编码
        if (read >= 3 && Arrays.equals(Arrays.copyOf(bom, 3), UTF8_BOM)) {
            log.info("检测到UTF-8编码(带BOM)");
            return StandardCharsets.UTF_8;
        } else if (read >= 2 && Arrays.equals(Arrays.copyOf(bom, 2), UTF16LE_BOM)) {
            log.info("检测到UTF-16LE编码");
            return StandardCharsets.UTF_16LE;
        } else if (read >= 2 && Arrays.equals(Arrays.copyOf(bom, 2), UTF16BE_BOM)) {
            log.info("检测到UTF-16BE编码");
            return StandardCharsets.UTF_16BE;
        } else if (read >= 4 && Arrays.equals(bom, UTF32LE_BOM)) {
            log.info("检测到UTF-32LE编码");
            return Charset.forName("UTF-32LE");
        } else if (read >= 4 && Arrays.equals(bom, UTF32BE_BOM)) {
            log.info("检测到UTF-32BE编码");
            return Charset.forName("UTF-32BE");
        }
        
        // 如果没有BOM，尝试通过内容分析判断编码
        return detectEncodingByContent(inputStream);
    }
    
    /**
     * 通过内容分析判断编码
     * 
     * @param inputStream 输入流
     * @return 检测到的字符集
     */
    private static Charset detectEncodingByContent(InputStream inputStream) throws IOException {
        // 标记当前位置，以便回退
        inputStream.mark(4096);
        
        byte[] buffer = new byte[4096];
        int read = inputStream.read(buffer);
        
        // 重置到开始位置
        inputStream.reset();
        
        if (read <= 0) {
            log.info("文件为空，默认使用UTF-8编码");
            return StandardCharsets.UTF_8;
        }
        
        // 统计字节特征，用于判断可能的编码
        int asciiCount = 0;
        int gbkCount = 0;
        int invalidUtf8Count = 0;
        
        for (int i = 0; i < read; i++) {
            int value = buffer[i] & 0xFF;
            
            // 统计ASCII范围内的字符
            if (value < 128) {
                asciiCount++;
            } 
            // 检测可能的GBK编码特征（双字节）
            else if (i + 1 < read) {
                int nextValue = buffer[i + 1] & 0xFF;
                // GBK编码范围
                if ((value >= 0x81 && value <= 0xFE) && 
                    ((nextValue >= 0x40 && nextValue <= 0x7E) || (nextValue >= 0x80 && nextValue <= 0xFE))) {
                    gbkCount++;
                    i++; // 跳过下一个字节，因为已经判断为双字节
                }
            }
            
            // 检测无效的UTF-8序列
            if (value >= 0x80) {
                // 检查是否符合UTF-8编码规则
                if (value >= 0xC0 && value <= 0xDF) {
                    // 2字节UTF-8序列
                    if (i + 1 >= read || (buffer[i + 1] & 0xC0) != 0x80) {
                        invalidUtf8Count++;
                    }
                } else if (value >= 0xE0 && value <= 0xEF) {
                    // 3字节UTF-8序列
                    if (i + 2 >= read || (buffer[i + 1] & 0xC0) != 0x80 || (buffer[i + 2] & 0xC0) != 0x80) {
                        invalidUtf8Count++;
                    }
                } else if (value >= 0xF0 && value <= 0xF7) {
                    // 4字节UTF-8序列
                    if (i + 3 >= read || (buffer[i + 1] & 0xC0) != 0x80 || 
                        (buffer[i + 2] & 0xC0) != 0x80 || (buffer[i + 3] & 0xC0) != 0x80) {
                        invalidUtf8Count++;
                    }
                } else if ((value & 0xC0) == 0x80) {
                    // 单独出现的UTF-8后续字节
                    invalidUtf8Count++;
                }
            }
        }
        
        // 根据各编码特征的统计结果，判断最可能的编码
        if (invalidUtf8Count > 0 && gbkCount > 0) {
            log.info("检测到GBK编码特征，使用GBK编码");
            return Charset.forName("GBK");
        } else if (invalidUtf8Count > 0) {
            // 尝试常见的中文编码
            log.info("检测到中文编码特征，使用GB18030编码");
            return Charset.forName("GB18030");
        } else {
            log.info("默认使用UTF-8编码");
            return StandardCharsets.UTF_8;
        }
    }
    
    /**
     * 读取输入流内容并转换为字符串，自动检测编码
     *
     * @param inputStream 输入流
     * @return 转换后的字符串
     */
    public static String readInputStream(InputStream inputStream) throws IOException {
        // 确保支持mark/reset
        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream);
        }
        
        // 检测编码
        Charset charset = detectEncoding(inputStream);
        
        // 读取全部内容
        byte[] bytes = inputStream.readAllBytes();
        
        // 如果是UTF-8带BOM，需要跳过BOM标记
        if (charset == StandardCharsets.UTF_8 && bytes.length >= 3 && 
            bytes[0] == (byte)0xEF && bytes[1] == (byte)0xBB && bytes[2] == (byte)0xBF) {
            return new String(bytes, 3, bytes.length - 3, charset);
        }
        
        // 转换为字符串
        return new String(bytes, charset);
    }
}