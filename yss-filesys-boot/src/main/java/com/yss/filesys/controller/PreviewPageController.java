package com.yss.filesys.controller;

import com.yss.filesys.application.dto.FilePreviewDTO;
import com.yss.filesys.application.port.FilePreviewUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 预览页面控制器
 * <p>
 * 提供文件预览页面渲染接口，返回 Thymeleaf 模板页面
 * </p>
 */
@Slf4j
@Controller
public class PreviewPageController {

    /**
     * 文件预览用例
     */
    private final FilePreviewUseCase filePreviewUseCase;

    public PreviewPageController(FilePreviewUseCase filePreviewUseCase) {
        this.filePreviewUseCase = filePreviewUseCase;
    }

    /**
     * 文件预览页面
     *
     * @param fileId       文件ID
     * @param previewToken 预览令牌
     * @param model        模型
     * @return 模板名称
     */
    @GetMapping("/preview/{fileId}")
    public String preview(@PathVariable String fileId, @RequestParam String previewToken, Model model) {
        try {
            FilePreviewDTO dto = filePreviewUseCase.preview(fileId, previewToken);
            fillModel(model, dto);
            return resolveTemplate(dto.getPreviewType());
        } catch (Exception e) {
            log.warn("Preview page failed: fileId={}", fileId, e);
            model.addAttribute("errorMessage", e.getMessage());
            return "preview/error";
        }
    }

    /**
     * 压缩包内文件预览页面
     *
     * @param archiveFileId 压缩包文件ID
     * @param innerPath     压缩包内文件路径
     * @param previewToken  预览令牌
     * @param model         模型
     * @return 模板名称
     */
    @GetMapping("/archive/preview/{archiveFileId}")
    public String previewArchive(@PathVariable String archiveFileId,
                                 @RequestParam String innerPath,
                                 @RequestParam String previewToken,
                                 Model model) {
        try {
            FilePreviewDTO dto = filePreviewUseCase.previewArchive(archiveFileId, innerPath, previewToken);
            fillModel(model, dto);
            return resolveTemplate(dto.getPreviewType());
        } catch (Exception e) {
            log.warn("Archive preview page failed: archiveFileId={}, innerPath={}", archiveFileId, innerPath, e);
            model.addAttribute("errorMessage", e.getMessage());
            return "preview/error";
        }
    }

    /**
     * 填充模型数据
     *
     * @param model 模型
     * @param dto   预览信息
     */
    private void fillModel(Model model, FilePreviewDTO dto) {
        model.addAttribute("fileName", dto.getFileName());
        model.addAttribute("previewType", dto.getPreviewType());
        model.addAttribute("streamUrl", dto.getStreamUrl());
        model.addAttribute("mimeType", dto.getMimeType());
    }

    /**
     * 根据预览类型解析模板名称
     *
     * @param previewType 预览类型
     * @return 模板名称
     */
    private String resolveTemplate(String previewType) {
        return switch (previewType) {
            case "image" -> "preview/image";
            case "video" -> "preview/video";
            case "audio" -> "preview/audio";
            case "document" -> "preview/document";
            case "markdown" -> "preview/markdown";
            case "code" -> "preview/code";
            case "text" -> "preview/text";
            default -> "preview/unsupported";
        };
    }
}
