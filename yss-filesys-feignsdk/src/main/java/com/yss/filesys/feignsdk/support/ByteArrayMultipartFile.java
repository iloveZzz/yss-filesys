package com.yss.filesys.feignsdk.support;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 基于字节数组的 MultipartFile 实现。
 */
public class ByteArrayMultipartFile implements MultipartFile {

    private final String name;
    private final String originalFilename;
    private final byte[] content;

    public ByteArrayMultipartFile(String name, String originalFilename, byte[] content) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.content = content == null ? new byte[0] : content.clone();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return "application/octet-stream";
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() {
        return content.clone();
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException {
        java.nio.file.Files.write(dest.toPath(), content);
    }
}
