# yss-filesys-feignsdk

`yss-filesys-feignsdk` 是 `yss-filesys` 的 Spring Cloud OpenFeign SDK，面向外部服务提供统一的文件上传、目录创建、目录查询和传输任务操作能力。

## 功能说明

- 支持直接上传文件到指定父目录
- 支持按目录路径上传，目录不存在时自动创建
- 支持默认覆盖同目录下同名文件
- 支持秒传、分片上传和合并上传
- 支持目录查询与目录创建

## 依赖方式

在外部服务中引入模块依赖后，注入对应的 SDK Service 即可使用。

## 上传示例

### 1. 直接上传到父目录

```java
@Autowired
private YssFilesysUploadFlowService uploadFlowService;

public void uploadToParent(byte[] content) throws IOException {
    uploadFlowService.upload(
            content,
            "demo.txt",
            "text/plain",
            "parent-file-id",
            "storage-setting-id",
            5L * 1024 * 1024
    );
}
```

### 2. 按目录路径上传，目录不存在自动创建

```java
@Autowired
private YssFilesysUploadFlowService uploadFlowService;

public void uploadToDirectory(byte[] content) throws IOException {
    uploadFlowService.upload(
            content,
            "demo.txt",
            "text/plain",
            null,
            "项目资料/2026/合同",
            "storage-setting-id",
            5L * 1024 * 1024
    );
}
```

### 3. 使用目录上传请求对象

```java
YssFilesysUploadToDirectoryRequest request = new YssFilesysUploadToDirectoryRequest();
request.setDirectoryPath("项目资料/2026/合同");
request.setStorageSettingId("storage-setting-id");
request.setChunkSize(5L * 1024 * 1024);
request.setOverwriteExisting(Boolean.TRUE);

uploadFlowService.uploadToDirectory(file, request);
```

## 目录上传规则

- `directoryPath` 支持多级目录，例如 `项目资料/2026/合同`
- 路径中的每一级目录都会自动查找
- 找不到时会自动创建
- `overwriteExisting` 默认是 `true`
- 当设置为 `false` 时，遇到同目录同名文件会拒绝覆盖

## 主要入口

- `YssFilesysUploadFlowService`
- `YssFilesysTransferSdkService`
- `YssFilesysFileSdkService`

## 注意事项

- `directoryPath` 为空时，会回退为普通父目录上传
- `chunkSize` 为空或小于等于 0 时，使用 SDK 默认分片大小
- 外部服务应确保已正确配置 `yss.filesys.feign.name` 和对应的服务发现环境
