package com.yss.filesys.controller;

import com.yss.filesys.application.dto.FileRecordDTO;
import com.yss.filesys.application.dto.FileShareThinDTO;
import com.yss.filesys.application.port.FileShareQueryUseCase;
import com.yss.filesys.domain.model.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
public class SharePageController {

    private final FileShareQueryUseCase fileShareQueryUseCase;

    public SharePageController(FileShareQueryUseCase fileShareQueryUseCase) {
        this.fileShareQueryUseCase = fileShareQueryUseCase;
    }

    @GetMapping("/share/{shareId}")
    public String sharePage(@PathVariable String shareId,
                            @RequestParam(required = false) String shareCode,
                            Model model) {
        try {
            FileShareThinDTO shareInfo = fileShareQueryUseCase.getShareInfo(shareId);
            model.addAttribute("shareInfo", shareInfo);
            model.addAttribute("shareCode", shareCode);
            if (!Boolean.TRUE.equals(shareInfo.getHasCheckCode()) || (shareCode != null && !shareCode.isBlank())) {
                List<FileRecordDTO> files = fileShareQueryUseCase.listShareFiles(shareId, shareCode);
                model.addAttribute("files", files);
            }
            return "share/view";
        } catch (BizException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("shareId", shareId);
            return "share/error";
        } catch (Exception e) {
            log.warn("Share page failed: shareId={}", shareId, e);
            model.addAttribute("errorMessage", "分享页加载失败");
            model.addAttribute("shareId", shareId);
            return "share/error";
        }
    }
}
