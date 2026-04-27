# YSS FS Frontend Workspace

基于 `yss-datamiddle-approval-frontend` 的工作区结构重建的文件存储微应用。

## 目录结构

```text
yss-fs-frontend/
├── packages/              # 实际运行的子应用源码
│   ├── src/
│   ├── vite.config.ts
│   ├── vite.config.jsp.ts
│   └── package.json
├── micro-config.json      # qiankun 注册信息
└── package.json           # workspace 脚本入口
```

## 启动

```bash
pnpm install
pnpm dev
```

`dev` 实际会进入 `packages/` 执行，开发端口为 `8082`。

## 构建

```bash
pnpm build
pnpm build:jsp
pnpm build:standalone
```

## 说明

- `packages/` 是唯一的运行代码目录。
- 支持 qiankun 子应用模式。
- 支持独立运行和 JSP 打包模式。
