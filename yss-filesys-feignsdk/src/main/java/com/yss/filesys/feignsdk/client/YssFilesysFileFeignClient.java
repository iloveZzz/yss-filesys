package com.yss.filesys.feignsdk.client;

import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.filesys.feignsdk.dto.YssFilesysCreateDirectoryRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysDirectoryTreeDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysFileRecordDTO;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * yss-filesys 文件目录 Feign 客户端。
 * <p>
 * 面向其他 Spring Cloud 服务提供目录查询与目录创建能力。
 * </p>
 */
@FeignClient(
        name = "${yss.filesys.feign.name:yss-filesys}",
        path = "/files",
        contextId = "yssFilesysFileFeignClient"
)
public interface YssFilesysFileFeignClient {

    /**
     * 创建目录。
     */
    @PostMapping("/directory")
    SingleResult<YssFilesysFileRecordDTO> createDirectory(@Valid @RequestBody YssFilesysCreateDirectoryRequest request);

    /**
     * 查询目录列表。
     */
    @GetMapping("/dirs")
    MultiResult<YssFilesysDirectoryTreeDTO> listDirs(@RequestParam(required = false) String parentId);
}
