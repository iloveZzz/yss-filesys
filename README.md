# yss-filesys

`yss-filesys` 是对 `free-fs` 的分层重构版本，参考 `subject-match-java` 的模块化架构与技术选型。

## 架构分层

- `yss-filesys-core`：领域模型、网关接口、用例端口（不依赖具体存储实现）
- `yss-filesys-infra`：MyBatis-Plus 持久化实现、PO/Mapper、GatewayImpl
- `yss-filesys-boot`：启动入口、应用服务、REST Controller、Quartz、Liquibase、OpenAPI
- `yss-filesys-e2e`：端到端测试模块（占位）

## 对齐的技术栈

- Spring Boot 3
- MyBatis-Plus
- Liquibase
- Quartz
- springdoc-openapi
- Actuator

## 与 free-fs 的迁移映射

- `fs-modules/fs-file` -> `core.domain.model.FileRecord` + `boot.controller.FileController`
- `fs-modules/fs-storage` -> `core.domain.model.StoragePlatform/StorageSetting` + `boot.controller.StorageController`
- `fs-modules/fs-file`（分享/传输）-> `core.domain.model.FileShareRecord/FileTransferTask` + `boot.controller.FileShareController/FileTransferController`
- `fs-modules/* mapper` -> `infra.repository.mapper/*`
- `service impl 直接持久化` -> `boot.appService -> core.gateway -> infra.gatewayImpl`

## 启动

在 `yss-filesys` 根目录执行：

```bash
mvn -pl yss-filesys-boot -am spring-boot:run
```

## 当前阶段说明

当前已迁移：

- 文件主数据查询、建目录、回收站标记
- 文件重命名、移动、目录列表、目录层级路径
- 文件直链获取与直接下载
- 存储平台与用户配置管理
- 存储插件体系（插件元数据注册、实例缓存、门面调用、Local 内置插件）
- 套餐管理（`subscription_plan`、`user_subscription`、`user_quota_usage`）
- 文件分享创建/查询/取消
- 分享公开页信息、验证提取码、文件列表、下载
- 分享页面入口（`/share/{shareId}`）
- 传输任务初始化、上传前校验、查询、取消
- 分片上传、自动/手动合并、下载任务初始化、下载分片
- 传输任务 SSE 订阅与进度推送
- 传输任务定时清理
- 预览链路（预览 token、预览信息、流式访问，支持 Range）
- 预览页面入口（`/preview/page/{fileId}`、`/preview/page/archive/{archiveFileId}`）
- 压缩包内文件预览（archive token + innerPath + 流式读取）
- 类型化预览模板（image/video/audio/pdf/text/code/markdown/office/unsupported）
- Office/PDF 仅保留 OnlyOffice 扩展口，当前不做本地渲染
- 回收站恢复、永久删除、清空，以及过期回收项定时清理
- 文件收藏、取消收藏、收藏数量统计，以及收藏筛选
- 分享访问记录写入与查询
- 登录成功日志写入与分页查询
- 文件首页统计（文件数、目录数、回收站、收藏、容量、按天容量趋势）
- 匿名用户上下文（接口默认使用匿名账号，不再暴露登录/鉴权接口）

尚未迁移（后续阶段）：

- 细粒度权限模型（角色/资源级别）
- 标签、批注、历史版本等边角能力
