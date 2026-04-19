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


## 启动

在 `yss-filesys` 根目录执行：

```bash
mvn -pl yss-filesys-boot -am spring-boot:run
```
