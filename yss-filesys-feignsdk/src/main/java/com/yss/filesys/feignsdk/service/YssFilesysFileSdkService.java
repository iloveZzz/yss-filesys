package com.yss.filesys.feignsdk.service;

import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.filesys.feignsdk.client.YssFilesysFileFeignClient;
import com.yss.filesys.feignsdk.dto.YssFilesysCreateDirectoryRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysDirectoryTreeDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysFileRecordDTO;
import com.yss.filesys.feignsdk.exception.YssFilesysFeignSdkException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * yss-filesys 文件与目录门面。
 * <p>
 * 封装目录创建和目录列表查询等通用文件能力。
 * </p>
 */
@Service
public class YssFilesysFileSdkService {

    private final YssFilesysFileFeignClient feignClient;

    public YssFilesysFileSdkService(YssFilesysFileFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    /**
     * 创建目录。
     *
     * @param request 创建目录请求
     * @return 创建后的目录记录
     */
    public YssFilesysFileRecordDTO createDirectory(YssFilesysCreateDirectoryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("创建目录请求不能为空");
        }
        if (request.getFolderName() == null || request.getFolderName().isBlank()) {
            throw new IllegalArgumentException("目录名称不能为空");
        }
        return requireData(feignClient.createDirectory(request), "创建目录失败");
    }

    /**
     * 查询指定父目录下的目录列表。
     *
     * @param parentId 父目录ID
     * @return 目录树列表
     */
    public List<YssFilesysDirectoryTreeDTO> listDirs(String parentId) {
        return requireData(feignClient.listDirs(parentId), "查询目录失败");
    }

    private static <T> T requireData(SingleResult<T> response, String message) {
        requireSuccess(response, message);
        return response.getData();
    }

    private static <T> List<T> requireData(MultiResult<T> response, String message) {
        requireSuccess(response, message);
        return response.getData();
    }

    private static void requireSuccess(com.yss.cloud.dto.response.Result response, String message) {
        if (response == null) {
            throw new YssFilesysFeignSdkException(message + "，返回结果为空");
        }
        if (!response.isSuccess()) {
            throw new YssFilesysFeignSdkException(message + "，code=" + response.getCode() + "，message=" + response.getMessage());
        }
    }
}
