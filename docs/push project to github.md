# 推送项目到 GitHub

## 远程仓库

```
https://github.com/rxummm/RXAS400ADM
```

## 首次推送（已完成）

```bash
git init
git add .
git commit -m "Initial commit: RXAS400ADM - AS400 management platform"
git remote add origin https://github.com/rxummm/RXAS400ADM.git
git branch -M main
git push -u origin main
```

## 后续日常推送

```bash
git add .
git commit -m "描述你的改动"
git push
```

## 推送指定文件

```bash
git add path/to/file1 path/to/file2
git commit -m "描述你的改动"
git push
```

## 查看状态

```bash
git status
git log --oneline -5
git remote -v
```

## .gitignore 说明

项目已配置 `.gitignore`，自动排除以下内容：

| 排除项 | 说明 |
|--------|------|
| `node_modules/` | 前端依赖 |
| `frontend/dist/` | 前端构建产物 |
| `docs/.vitepress/dist/` | 文档构建产物 |
| `docs/.vitepress/cache/` | 文档缓存 |
| `backend/**/target/` | Java 编译产物 |
| `*.class` | Java 字节码 |
| `.idea/`, `.vscode/` | IDE 配置 |
| `*.log`, `*.err` | 日志文件 |
| `.freebuff/`, `.opencode/` | 本地开发工具 |
| `logs/`, `*.tmp` | 运行时文件 |