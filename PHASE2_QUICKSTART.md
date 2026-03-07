# Phase 2 快速启动指南

## 部署步骤

1. 确保所有新文件已提交到代码库
2. 重新构建并启动服务：
   ```bash
   cd /path/to/one
   docker compose up -d --build backend
   ```

3. 数据库表会自动创建（JPA auto-create）
4. 应用启动时会自动初始化 6 个内置模板

## 测试步骤

### 1. 测试模板列表
```bash
curl http://127.0.0.1:18081/api/resume/templates
```

应该返回 6 个内置模板。

### 2. 测试获取模板详情
```bash
curl http://127.0.0.1:18081/api/resume/templates/1
```

应该返回 Java后端工程师模板的完整内容，并且 usageCount 会增加。

### 3. 测试生成简历（会自动保存版本）
```bash
# 先登录获取 token
TOKEN=$(curl -X POST http://127.0.0.1:18081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}' \
  | jq -r '.token')

# 生成简历
curl -X POST http://127.0.0.1:18081/api/resume/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "targetRole": "Java后端工程师",
    "jdText": "要求5年Java开发经验，熟悉Spring Boot、微服务",
    "userBackground": "有5年Java开发经验"
  }'
```

### 4. 测试查看版本列表
```bash
curl http://127.0.0.1:18081/api/resume/versions \
  -H "Authorization: Bearer $TOKEN"
```

应该能看到刚才自动保存的版本。

### 5. 测试手动保存版本
```bash
curl -X POST http://127.0.0.1:18081/api/resume/versions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "我的简历 v1",
    "content": "# 张三\n## 工作经历\n...",
    "targetRole": "Java工程师"
  }'
```

### 6. 测试版本对比
```bash
# 假设有两个版本 id=1 和 id=2
curl "http://127.0.0.1:18081/api/resume/versions/compare?id1=1&id2=2" \
  -H "Authorization: Bearer $TOKEN"
```

### 7. 测试管理员创建模板
```bash
# 使用管理员账号登录
ADMIN_TOKEN=$(curl -X POST http://127.0.0.1:18081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.token')

# 创建新模板
curl -X POST http://127.0.0.1:18081/api/admin/resume/templates \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "全栈工程师模板",
    "industry": "互联网",
    "targetRole": "全栈工程师",
    "content": "# 全栈工程师简历模板\n...",
    "previewText": "精通前后端开发，熟悉全栈技术"
  }'
```

## 验证数据库

连接到 MySQL 数据库，检查表是否创建成功：

```sql
-- 查看简历版本表
SELECT * FROM resume_version;

-- 查看简历模板表
SELECT * FROM resume_template;

-- 查看模板使用统计
SELECT name, usage_count FROM resume_template ORDER BY usage_count DESC;
```

## 常见问题

### Q: 模板没有自动初始化？
A: 检查应用日志，确保 `ResumeTemplateService.initDefaultTemplates()` 被调用。如果表中已有数据，不会重复初始化。

### Q: 版本没有自动保存？
A: 检查 `ResumeGeneratorService` 中的 try-catch 块，确保没有异常被吞掉。查看应用日志。

### Q: 版本对比结果不准确？
A: 当前实现是简单的逐行对比，对于复杂的文本变更可能不够精确。可以考虑使用更高级的 diff 算法（如 Myers diff）。

## 下一步

- 前端集成：在 Vue 前端添加版本管理和模板选择界面
- 优化版本对比算法：使用更精确的 diff 算法
- 添加版本回滚功能：允许用户恢复到历史版本
- 模板分类优化：支持更细粒度的行业和岗位分类
