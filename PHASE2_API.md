# Phase 2 API 文档

## 简历版本管理 API

### 1. 保存简历版本
```
POST /api/resume/versions
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Java后端工程师简历 v1",
  "content": "# 张三\n...",
  "targetRole": "Java后端工程师"
}

Response:
{
  "id": 1,
  "title": "Java后端工程师简历 v1",
  "content": "# 张三\n...",
  "targetRole": "Java后端工程师",
  "sourceType": "MANUAL",
  "sourceId": null,
  "createdAt": "2026-03-07T10:00:00Z",
  "updatedAt": "2026-03-07T10:00:00Z"
}
```

### 2. 获取简历版本列表
```
GET /api/resume/versions?page=0&size=20
Authorization: Bearer <token>

Response:
{
  "items": [
    {
      "id": 1,
      "title": "Java后端工程师简历 v1",
      "targetRole": "Java后端工程师",
      "sourceType": "MANUAL",
      "sourceId": null,
      "createdAt": "2026-03-07T10:00:00Z",
      "updatedAt": "2026-03-07T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

### 3. 获取简历版本详情
```
GET /api/resume/versions/{id}
Authorization: Bearer <token>

Response:
{
  "id": 1,
  "title": "Java后端工程师简历 v1",
  "content": "# 张三\n...",
  "targetRole": "Java后端工程师",
  "sourceType": "MANUAL",
  "sourceId": null,
  "createdAt": "2026-03-07T10:00:00Z",
  "updatedAt": "2026-03-07T10:00:00Z"
}
```

### 4. 删除简历版本
```
DELETE /api/resume/versions/{id}
Authorization: Bearer <token>

Response:
{
  "ok": true
}
```

### 5. 对比两个简历版本
```
GET /api/resume/versions/compare?id1=1&id2=2
Authorization: Bearer <token>

Response:
{
  "id1": 1,
  "title1": "Java后端工程师简历 v1",
  "id2": 2,
  "title2": "Java后端工程师简历 v2",
  "lines": [
    {
      "type": "SAME",
      "text": "# 张三"
    },
    {
      "type": "REMOVED",
      "text": "- 5年Java开发经验"
    },
    {
      "type": "ADDED",
      "text": "- 6年Java开发经验"
    }
  ]
}
```

## 简历模板库 API

### 1. 获取模板列表
```
GET /api/resume/templates?industry=互联网&page=0&size=20

Response:
{
  "items": [
    {
      "id": 1,
      "name": "Java后端工程师模板",
      "industry": "互联网",
      "targetRole": "Java后端工程师",
      "previewText": "5年Java后端经验，熟悉Spring Boot、微服务、高并发系统优化",
      "usageCount": 10,
      "createdAt": "2026-03-07T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "total": 6
}
```

### 2. 获取模板详情（会增加使用次数）
```
GET /api/resume/templates/{id}

Response:
{
  "id": 1,
  "name": "Java后端工程师模板",
  "industry": "互联网",
  "targetRole": "Java后端工程师",
  "content": "# 张三\n...",
  "previewText": "5年Java后端经验，熟悉Spring Boot、微服务、高并发系统优化",
  "usageCount": 11,
  "createdAt": "2026-03-07T10:00:00Z"
}
```

### 3. 创建模板（管理员）
```
POST /api/admin/resume/templates
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "name": "产品经理模板",
  "industry": "互联网",
  "targetRole": "产品经理",
  "content": "# 王五\n...",
  "previewText": "5年产品经验，擅长ToB SaaS、用户体验、数据驱动"
}

Response:
{
  "id": 7,
  "name": "产品经理模板",
  "industry": "互联网",
  "targetRole": "产品经理",
  "content": "# 王五\n...",
  "previewText": "5年产品经验，擅长ToB SaaS、用户体验、数据驱动",
  "usageCount": 0,
  "createdAt": "2026-03-07T10:00:00Z"
}
```

### 4. 更新模板（管理员）
```
PUT /api/admin/resume/templates/{id}
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "name": "产品经理模板（更新）",
  "industry": "互联网",
  "targetRole": "产品经理",
  "content": "# 王五\n...",
  "previewText": "5年产品经验，擅长ToB SaaS、用户体验、数据驱动"
}

Response:
{
  "id": 7,
  "name": "产品经理模板（更新）",
  ...
}
```

### 5. 删除模板（管理员）
```
DELETE /api/admin/resume/templates/{id}
Authorization: Bearer <admin-token>

Response:
{
  "ok": true
}
```

## 自动保存版本

当用户调用以下接口时，系统会自动保存简历版本：

1. `POST /api/resume/generate` - 生成简历后自动保存，sourceType=GENERATED
2. `POST /api/resume/rewrite` - 重写简历后自动保存，sourceType=REWRITTEN

版本标题格式：
- 生成简历：`生成简历 - {targetRole} - {timestamp}`
- 重写简历：`重写简历 - {targetRole} - {timestamp}`

## 内置模板

系统启动时会自动初始化 6 个内置模板：
1. Java后端工程师模板
2. 前端工程师模板
3. 产品经理模板
4. 数据分析师模板
5. 运维工程师模板
6. 测试工程师模板

所有模板都包含完整的简历结构和 STAR 风格的工作经历示例。
