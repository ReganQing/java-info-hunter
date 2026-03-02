# 数据库安装脚本使用说明

## 问题说明

如果 Docker 无法拉取镜像（网络连接问题），请尝试以下方案：

---

## 方案 1：配置 Docker 镜像加速器（推荐）

### Windows Docker Desktop

1. 打开 Docker Desktop
2. 点击设置 (Settings)
3. 选择 Docker Engine
4. 添加以下配置：

```json
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}
```

5. 点击 "Apply & Restart"
6. 重新运行安装脚本：

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts\install-postgres-docker.ps1
```

---

## 方案 2：使用 Chocolatey 本地安装 PostgreSQL

### 优点
- 无需 Docker
- 网络要求低
- 性能更好

### 缺点
- pgvector 需要手动编译
- 需要管理员权限

### 步骤

1. **以管理员身份运行 PowerShell**

2. **安装 PostgreSQL**
   ```powershell
   choco install postgresql --params '/Password:admin123' -y
   ```

3. **运行数据库设置脚本**
   ```cmd
   scripts\setup-database.bat
   ```

4. **（可选）编译安装 pgvector**
   ```cmd
   scripts\install-pgvector.bat
   ```

---

## 方案 3：使用 Docker + 阿里云镜像

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts\install-postgres-docker-cn.ps1
```

此脚本使用官方 PostgreSQL 镜像，无需 pgvector 即可开始开发。

---

## 方案 4：等待网络恢复

如果您正在使用代理或防火墙：

1. 配置 Docker 代理设置
2. 或等待网络恢复正常
3. 重新运行安装脚本

---

## 验证安装

无论使用哪种方案，都可以通过以下命令验证：

```cmd
# Windows (PowerShell)
docker exec pg-javainfohunter psql -U postgres -d javainfohunter -c "SELECT version();"

# 或使用 psql（如果本地安装）
psql -U postgres -d javainfohunter -c "SELECT version();"
```

---

## 常见问题

### Q: Docker 无法拉取镜像
**A:** 配置镜像加速器（方案 1）或使用本地安装（方案 2）

### Q: 容器启动失败
**A:** 检查端口 5432 是否被占用：`netstat -ano | findstr 5432`

### Q: 环境变量不生效
**A:** 重启终端或重新登录 Windows

### Q: pgvector 扩展缺失
**A:** Flyway 迁移脚本会自动安装 pgvector 扩展

---

## 下一步

安装完成后：

1. **重启终端**（使环境变量生效）

2. **运行数据库迁移**
   ```bash
   mvnw.cmd flyway:migrate
   ```

3. **运行测试**
   ```bash
   mvnw.cmd test
   ```

---

如有问题，请查看：
- [数据库设计说明.md](../docs/数据库设计说明.md)
- [数据库使用指南.md](../docs/数据库使用指南.md)
