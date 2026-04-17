package com.yss.filesys.controller;

import com.yss.filesys.application.dto.FilePreviewDTO;
import com.yss.filesys.application.port.FilePreviewUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
public class PreviewPageController {

    private final FilePreviewUseCase filePreviewUseCase;

    public PreviewPageController(FilePreviewUseCase filePreviewUseCase) {
        this.filePreviewUseCase = filePreviewUseCase;
    }

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

    private void fillModel(Model model, FilePreviewDTO dto) {
        model.addAttribute("fileName", dto.getFileName());
        model.addAttribute("previewType", dto.getPreviewType());
        model.addAttribute("streamUrl", dto.getStreamUrl());
        model.addAttribute("mimeType", dto.getMimeType());
    }

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
