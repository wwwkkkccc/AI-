import io
import json
import os
import re
import sqlite3
from urllib.parse import urlparse
from collections import Counter
from datetime import datetime
from pathlib import Path
from typing import Any

from docx import Document
from fastapi import FastAPI, File, Form, HTTPException, Request, UploadFile
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from pydantic import BaseModel, Field
from pypdf import PdfReader

try:
    import jieba
except Exception:  # pragma: no cover
    jieba = None

try:
    from openai import OpenAI
except Exception:  # pragma: no cover
    OpenAI = None

APP_NAME = "AI Resume Optimizer"
BASE_DIR = Path(__file__).resolve().parent
DB_PATH = BASE_DIR / "resume_ai.db"
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "").strip()
OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-4.1-mini").strip()
OPENAI_BASE_URL = os.getenv("OPENAI_BASE_URL", "").strip()
MAX_FILE_SIZE = 5 * 1024 * 1024

app = FastAPI(title=APP_NAME, version="1.0.0")
app.mount("/static", StaticFiles(directory=str(BASE_DIR / "static")), name="static")
templates = Jinja2Templates(directory=str(BASE_DIR / "templates"))

def normalize_base_url(base_url: str) -> str:
    url = (base_url or "").strip()
    if not url:
        return ""
    parsed = urlparse(url)
    path = (parsed.path or "").strip()
    if path in {"", "/"}:
        return url.rstrip("/") + "/v1"
    return url.rstrip("/")


def create_openai_client(api_key: str, base_url: str = ""):
    if not api_key or not OpenAI:
        return None
    kwargs: dict[str, Any] = {"api_key": api_key}
    normalized = normalize_base_url(base_url)
    if normalized:
        kwargs["base_url"] = normalized
    try:
        return OpenAI(**kwargs)
    except Exception:
        return None


OPENAI_CLIENT = create_openai_client(OPENAI_API_KEY, OPENAI_BASE_URL)


def get_openai_client(runtime_api_key: str = "", runtime_base_url: str = ""):
    key = (runtime_api_key or "").strip()
    base = (runtime_base_url or "").strip()
    if key or base:
        use_key = key or OPENAI_API_KEY
        use_base = base or OPENAI_BASE_URL
        return create_openai_client(use_key, use_base)
    return OPENAI_CLIENT

EN_STOPWORDS = {
    "the", "and", "for", "with", "from", "that", "this", "your", "you", "our", "are", "was",
    "were", "have", "has", "had", "will", "can", "able", "using", "used", "within", "across", "into",
    "job", "role", "team", "work", "years", "year", "experience", "requirements", "responsibilities",
}

ZH_STOPWORDS = {
    "我们", "你们", "负责", "能够", "进行", "相关", "以及", "以上", "具有", "优先", "工作", "经验",
    "能力", "熟悉", "参与", "岗位", "要求", "职位", "公司", "团队", "完成", "进行过", "并且",
}

ACTION_HINTS = {
    "designed", "built", "delivered", "improved", "optimized", "implemented", "led", "scaled", "automated",
    "reduced", "increased", "achieved", "created", "launched", "debugged", "analyzed", "规划", "设计", "开发",
    "优化", "提升", "落地", "推动", "实现", "搭建", "改进", "重构", "治理", "交付",
}


def init_db() -> None:
    conn = sqlite3.connect(DB_PATH)
    try:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS analyses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                created_at TEXT NOT NULL,
                filename TEXT,
                target_role TEXT,
                score REAL,
                coverage REAL,
                resume_text TEXT,
                jd_text TEXT,
                result_json TEXT
            )
            """
        )
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS chats (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                analysis_id INTEGER NOT NULL,
                created_at TEXT NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL
            )
            """
        )
        conn.commit()
    finally:
        conn.close()


def db_execute(query: str, params: tuple[Any, ...] = ()) -> sqlite3.Cursor:
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    try:
        cur = conn.execute(query, params)
        conn.commit()
        return cur
    finally:
        conn.close()


def db_query_one(query: str, params: tuple[Any, ...] = ()) -> dict[str, Any] | None:
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    try:
        row = conn.execute(query, params).fetchone()
        return dict(row) if row else None
    finally:
        conn.close()


def normalize_text(text: str) -> str:
    text = text.replace("\r", "\n")
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def extract_pdf_text(data: bytes) -> str:
    reader = PdfReader(io.BytesIO(data))
    chunks: list[str] = []
    for page in reader.pages:
        chunks.append(page.extract_text() or "")
    return "\n".join(chunks)


def extract_docx_text(data: bytes) -> str:
    doc = Document(io.BytesIO(data))
    return "\n".join([p.text for p in doc.paragraphs if p.text.strip()])


def tokenize(text: str) -> list[str]:
    text_lower = text.lower()
    en_tokens = re.findall(r"[a-zA-Z][a-zA-Z0-9_+.#-]{1,30}", text_lower)

    zh_tokens: list[str] = []
    if jieba:
        for token in jieba.lcut(text):
            token = token.strip()
            if not token:
                continue
            if re.fullmatch(r"[\u4e00-\u9fff]{2,12}", token):
                zh_tokens.append(token)
    else:
        zh_tokens = re.findall(r"[\u4e00-\u9fff]{2,8}", text)

    merged: list[str] = []
    for tk in en_tokens:
        if tk in EN_STOPWORDS or tk.isdigit() or len(tk) < 2:
            continue
        merged.append(tk)

    for tk in zh_tokens:
        if tk in ZH_STOPWORDS:
            continue
        merged.append(tk)

    return merged


def top_keywords(text: str, limit: int = 35) -> list[str]:
    freq = Counter(tokenize(text))
    items = [word for word, _ in freq.most_common(limit)]
    return items


def section_completeness(resume_text: str) -> float:
    text = resume_text.lower()
    checks = {
        "contact": bool(re.search(r"@|1\d{10}", resume_text)),
        "education": any(k in text for k in ["education", "学历", "教育", "school", "大学"]),
        "experience": any(k in text for k in ["experience", "经历", "工作", "employment"]),
        "project": any(k in text for k in ["project", "项目", "case"]),
        "skills": any(k in text for k in ["skill", "技能", "tech", "stack"]),
    }
    return sum(1 for ok in checks.values() if ok) / len(checks)


def action_strength(resume_text: str) -> float:
    tokens = set(tokenize(resume_text))
    hit = len([x for x in ACTION_HINTS if x in tokens])
    return min(1.0, hit / 8)


def build_ats_report(resume_text: str, jd_text: str) -> dict[str, Any]:
    jd_keywords = top_keywords(jd_text, limit=40)
    resume_keywords = set(top_keywords(resume_text, limit=80))

    matched = [k for k in jd_keywords if k in resume_keywords]
    missing = [k for k in jd_keywords if k not in resume_keywords]

    coverage = round((len(matched) / len(jd_keywords) if jd_keywords else 0) * 100, 2)
    section = section_completeness(resume_text)
    action = action_strength(resume_text)
    score = min(100.0, round(coverage * 0.7 + section * 20 + action * 10, 2))

    advice = [
        f"补充缺失关键词：{', '.join(missing[:8])}" if missing else "关键词覆盖较完整",
        "项目经历建议按 STAR（情境-任务-行动-结果）重写，每条包含可量化结果",
        "技能区建议分为：核心技能 / 熟练工具 / 证书与语言",
    ]

    return {
        "score": score,
        "coverage": coverage,
        "jd_keywords": jd_keywords,
        "matched_keywords": matched,
        "missing_keywords": missing,
        "advice": advice,
    }


def fallback_optimized_content(resume_text: str, jd_text: str, ats: dict[str, Any], target_role: str) -> dict[str, Any]:
    lines = [
        re.sub(r"^[\-•\d\.)\s]+", "", line.strip())
        for line in re.split(r"\n+", resume_text)
        if len(line.strip()) >= 10
    ]

    rewritten = []
    for line in lines[:6]:
        rewritten.append(f"{line}；补充量化结果（如效率提升20%）并明确技术栈与个人贡献。")

    if not rewritten:
        rewritten = [
            "负责核心模块设计与开发，主导关键需求落地，保障按期交付。",
            "通过性能优化和自动化流程建设，显著降低系统故障率与人工成本。",
        ]

    top_missing = ats["missing_keywords"][:8]
    interview = [f"请结合一个项目说明你如何应用 {kw} 解决实际问题。" for kw in top_missing]
    interview += [
        "你在项目中遇到的最大技术挑战是什么？你是如何拆解并解决的？",
        "如果让你重新做一次该项目，你会优先改进哪三个点？",
    ]

    summary = (
        f"面向{target_role or '目标岗位'}，建议强化与JD高度匹配的关键词与成果表达，"
        "重点突出业务价值、技术难点和可量化结果。"
    )

    skills = [
        "把技能拆成：编程语言 / 框架中间件 / 数据库 / DevOps 工具",
        "优先将 JD 中高频关键词前置，避免堆砌无关技能",
    ]

    return {
        "summary": summary,
        "rewritten_experience": rewritten,
        "skills_recommendations": skills,
        "interview_questions": interview[:10],
        "llm_used": False,
    }


def llm_optimized_content(
    resume_text: str,
    jd_text: str,
    ats: dict[str, Any],
    target_role: str,
    runtime_api_key: str = "",
    runtime_model: str = "",
    runtime_base_url: str = "",
) -> dict[str, Any] | None:
    client = get_openai_client(runtime_api_key, runtime_base_url)
    if not client:
        return None

    prompt = {
        "target_role": target_role,
        "jd_keywords": ats["jd_keywords"][:30],
        "matched_keywords": ats["matched_keywords"][:20],
        "missing_keywords": ats["missing_keywords"][:20],
        "resume_text": resume_text[:12000],
        "jd_text": jd_text[:8000],
    }

    system = (
        "你是资深简历优化顾问。请输出严格JSON，字段必须为："
        "summary(string), rewritten_experience(array of string, 6-10 items), "
        "skills_recommendations(array of string, 4-8 items), "
        "interview_questions(array of string, 8-12 items)。"
        "要求：不编造经历；缺失信息用'【需补充】'标记；语言简洁专业。"
    )

    try:
        model = (runtime_model or OPENAI_MODEL).strip() or OPENAI_MODEL
        resp = client.chat.completions.create(
            model=model,
            temperature=0.2,
            response_format={"type": "json_object"},
            messages=[
                {"role": "system", "content": system},
                {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)},
            ],
        )
        text = resp.choices[0].message.content or "{}"
        data = json.loads(text)
        data["llm_used"] = True
        return data
    except Exception:
        return None


def build_markdown(target_role: str, ats: dict[str, Any], optimized: dict[str, Any]) -> str:
    matched = ", ".join(ats["matched_keywords"][:20])
    missing = ", ".join(ats["missing_keywords"][:20])

    exp_lines = "\n".join([f"- {line}" for line in optimized["rewritten_experience"][:10]])
    skill_lines = "\n".join([f"- {line}" for line in optimized["skills_recommendations"][:10]])
    interview_lines = "\n".join([f"- {line}" for line in optimized["interview_questions"][:12]])

    return f"""# AI优化简历建议

## 目标岗位
{target_role or '未填写'}

## ATS评分
- 综合评分：**{ats['score']}** / 100
- 关键词覆盖率：**{ats['coverage']}%**
- 已匹配关键词：{matched or '无'}
- 缺失关键词：{missing or '无'}

## 优化摘要
{optimized['summary']}

## 建议改写的项目/经历
{exp_lines}

## 技能区优化建议
{skill_lines}

## 面试问题清单
{interview_lines}
"""


async def extract_resume_text(file: UploadFile) -> str:
    if not file.filename:
        raise HTTPException(status_code=400, detail="请上传简历文件")

    ext = Path(file.filename).suffix.lower()
    if ext not in {".pdf", ".docx", ".txt", ".md"}:
        raise HTTPException(status_code=400, detail="仅支持 PDF / DOCX / TXT / MD")

    data = await file.read()
    if len(data) > MAX_FILE_SIZE:
        raise HTTPException(status_code=413, detail="文件过大，请控制在 5MB 以内")

    if ext == ".pdf":
        text = extract_pdf_text(data)
    elif ext == ".docx":
        text = extract_docx_text(data)
    else:
        text = data.decode("utf-8", errors="ignore")

    text = normalize_text(text)
    if len(text) < 80:
        raise HTTPException(status_code=400, detail="简历内容太短，无法分析")

    return text


class ChatRequest(BaseModel):
    analysis_id: int = Field(..., ge=1)
    question: str = Field(..., min_length=2, max_length=500)
    api_key: str = Field(default="", max_length=256)
    model: str = Field(default="", max_length=80)
    base_url: str = Field(default="", max_length=300)


@app.on_event("startup")
def on_startup() -> None:
    init_db()


@app.get("/api/health")
def health() -> dict[str, Any]:
    return {
        "ok": True,
        "app": APP_NAME,
        "time": datetime.utcnow().isoformat() + "Z",
        "llm_enabled": bool(OPENAI_CLIENT),
    }


@app.get("/", response_class=HTMLResponse)
def index(request: Request) -> HTMLResponse:
    return templates.TemplateResponse(
        "index.html",
        {
            "request": request,
            "app_name": APP_NAME,
            "llm_enabled": bool(OPENAI_CLIENT),
        },
    )


@app.post("/api/analyze")
async def analyze_resume(
    file: UploadFile = File(...),
    jd_text: str = Form(..., min_length=1, max_length=20000),
    target_role: str = Form("", max_length=100),
    api_key: str = Form("", max_length=256),
    model: str = Form("", max_length=80),
    base_url: str = Form("", max_length=300),
) -> dict[str, Any]:
    resume_text = await extract_resume_text(file)
    jd_text = normalize_text(jd_text)
    target_role = normalize_text(target_role)

    if len(jd_text) < 20:
        role_hint = target_role if len(target_role) >= 2 else jd_text
        if len(role_hint) < 2:
            raise HTTPException(status_code=400, detail="请填写更完整的岗位JD（至少20字），或先填写目标岗位")
        if not target_role:
            target_role = role_hint
        jd_text = (
            f"目标岗位：{role_hint}。"
            f"岗位职责：负责{role_hint}相关系统设计、开发与优化，参与需求分析、接口设计、问题排查与上线交付。"
            "任职要求：具备良好沟通协作能力，能够独立推进项目并持续改进性能与稳定性。"
        )

    ats = build_ats_report(resume_text, jd_text)
    optimized = llm_optimized_content(
        resume_text, jd_text, ats, target_role, api_key, model, base_url
    ) or fallback_optimized_content(
        resume_text, jd_text, ats, target_role
    )
    markdown = build_markdown(target_role, ats, optimized)

    payload = {
        "target_role": target_role,
        "ats": ats,
        "optimized": optimized,
        "markdown": markdown,
        "generated_at": datetime.utcnow().isoformat() + "Z",
    }

    conn = sqlite3.connect(DB_PATH)
    try:
        cur = conn.execute(
            """
            INSERT INTO analyses (created_at, filename, target_role, score, coverage, resume_text, jd_text, result_json)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                datetime.utcnow().isoformat() + "Z",
                file.filename,
                target_role,
                ats["score"],
                ats["coverage"],
                resume_text,
                jd_text,
                json.dumps(payload, ensure_ascii=False),
            ),
        )
        analysis_id = int(cur.lastrowid)
        conn.commit()
    finally:
        conn.close()

    return {
        "analysis_id": analysis_id,
        "score": ats["score"],
        "coverage": ats["coverage"],
        "matched_keywords": ats["matched_keywords"],
        "missing_keywords": ats["missing_keywords"],
        "advice": ats["advice"],
        "optimized": {
            "summary": optimized["summary"],
            "rewritten_experience": optimized["rewritten_experience"],
            "skills_recommendations": optimized["skills_recommendations"],
            "interview_questions": optimized["interview_questions"],
        },
        "optimized_markdown": markdown,
        "llm_used": optimized.get("llm_used", False),
    }


@app.post("/api/chat")
def chat(req: ChatRequest) -> dict[str, Any]:
    row = db_query_one("SELECT * FROM analyses WHERE id=?", (req.analysis_id,))
    if not row:
        raise HTTPException(status_code=404, detail="analysis_id 不存在")

    result = json.loads(row["result_json"])
    missing = result["ats"].get("missing_keywords", [])

    answer: str
    llm_used = False
    runtime_client = get_openai_client(req.api_key, req.base_url)
    if runtime_client:
        try:
            system = (
                "你是简历求职顾问。回答需短、可执行、不要编造候选人经历。"
                "如果信息不足，请明确提示用户补充。"
            )
            user_input = {
                "question": req.question,
                "target_role": row.get("target_role", ""),
                "missing_keywords": missing[:12],
                "resume_text": (row.get("resume_text") or "")[:9000],
                "jd_text": (row.get("jd_text") or "")[:6000],
            }
            use_model = (req.model or OPENAI_MODEL).strip() or OPENAI_MODEL
            resp = runtime_client.chat.completions.create(
                model=use_model,
                temperature=0.3,
                messages=[
                    {"role": "system", "content": system},
                    {"role": "user", "content": json.dumps(user_input, ensure_ascii=False)},
                ],
            )
            answer = (resp.choices[0].message.content or "").strip()
            llm_used = True
        except Exception:
            answer = "我现在无法调用AI模型。你可以先根据缺失关键词补充项目细节和可量化结果。"
    else:
        hint = "、".join(missing[:5]) if missing else "项目成果量化"
        answer = (
            f"建议先补齐这些关键词：{hint}。\n"
            "每条经历用‘做了什么-怎么做-结果如何’三段式表达，并补上数字结果（如提升xx%、节省xx小时）。"
        )

    conn = sqlite3.connect(DB_PATH)
    try:
        now = datetime.utcnow().isoformat() + "Z"
        conn.execute(
            "INSERT INTO chats (analysis_id, created_at, role, content) VALUES (?, ?, ?, ?)",
            (req.analysis_id, now, "user", req.question),
        )
        conn.execute(
            "INSERT INTO chats (analysis_id, created_at, role, content) VALUES (?, ?, ?, ?)",
            (req.analysis_id, now, "assistant", answer),
        )
        conn.commit()
    finally:
        conn.close()

    return {"answer": answer, "llm_used": llm_used}







