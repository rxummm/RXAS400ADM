# RXAS400ADM 推送到 GitHub 指南

## 前置条件

1. 已安装 Git（`git --version`）
2. 已有 GitHub 账号并创建了仓库
3. 项目代码已在本地（当前目录）

## 步骤

### 1. 初始化 Git 仓库（如尚未初始化）

```bash
cd /path/to/RXAS400ADM
git init
git branch -M main
```

### 2. 检查 .gitignore

确保以下目录/文件已忽略：

```
# Node
node_modules/
frontend/dist/
docs/.vitepress/dist/
docs/.vitepress/cache/

# Java / Maven
backend/**/target/
*.class

# IDE
.idea/
.vscode/
*.iml

# OS
.DS_Store
Thumbs.db

# Logs / temp
*.log
*.err

# Local dev tools
.freebuff/
.opencode/
.workbuddy/
.tmp-gate-tests-*/

# Local secrets
.env
.env.local

# Runtime logs and temp files
logs/
*.tmp
```

### 3. 添加文件并提交

```bash
# 检查状态
git status

# 添加所有文件（.gitignore 会自动排除）
git add .

# 或者逐个添加关键文件
git add AGENTS.md README.md
git add backend/ frontend/ docs/ scripts/ docker-compose.yml

# 提交
git commit -m "Initial commit: RXAS400ADM - AS400 management platform"
```

### 4. 关联远程仓库

```bash
# 替换为你的 GitHub 仓库地址
git remote add origin https://github.com/your-username/RXAS400ADM.git

# 或使用 SSH
git remote add origin git@github.com:your-username/RXAS400ADM.git
```

### 5. 推送到 GitHub

```bash
git push -u origin main
```

### 6. 首次推送后验证

```bash
# 检查远程仓库
git remote -v

# 查看推送状态
git status
```

## 注意事项

### 敏感信息

- **永远不要**提交 `.env` 文件（含数据库密码、JWT 密钥等）
- 确保 `application.yml` 中的密码使用环境变量 `${...}` 占位
- Docker Compose 的数据库密码通过 `.env` 注入

### 大文件

- GitHub 仓库限制单文件 100MB
- 如需提交大文件，使用 [Git LFS](https://git-lfs.github.com/)
- 本项目无大文件，无需 LFS

### 分支策略

```bash
# 开发分支
git checkout -b feature/your-feature
# 开发完成后合并到 main
git checkout main
git merge feature/your-feature
git push origin main
```

### 私有仓库

如项目包含敏感配置，建议创建 **私有仓库**：

1. GitHub → New repository → Private
2. 或将已有仓库设为 Private：Settings → Danger Zone → Change repository visibility

## 常见问题

### Q: push 被拒绝？

```bash
# 如果远程有内容，先拉取
git pull origin main --allow-unrelated-histories
# 解决冲突后推送
git push -u origin main
```

### Q: 如何更新远程仓库？

```bash
# 修改代码后
git add .
git commit -m "描述修改内容"
git push origin main
```

### Q: 如何克隆已有仓库？

```bash
git clone https://github.com/your-username/RXAS400ADM.git
cd RXAS400ADM
```
