# yss-filesys

`yss-filesys` 是参考 `subject-match-java` 的模块化架构与技术选型。

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
