# AI Resume Optimizer

轻量可部署的 AI 简历优化智能体（上传简历 + JD 匹配 + 优化建议 + AI咨询）。

## 功能
- PDF/DOCX/TXT 简历解析
- ATS关键词匹配与评分
- 简历改写建议与面试问题生成
- 继续追问（AI咨询）
- SQLite 持久化分析记录

## 本地运行
```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
export OPENAI_API_KEY=your_key
uvicorn app.main:app --host 0.0.0.0 --port 8090
```

访问：`http://127.0.0.1:8090`

## 部署
推荐 `systemd + uvicorn + nginx`，将 nginx 反代到 `127.0.0.1:8090`。

## 说明
- 未配置 OPENAI_API_KEY 时，会自动降级为规则引擎模式（可用但效果较弱）。
