from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import os
from dotenv import load_dotenv

# 環境変数を読み込み
load_dotenv('../../.env')

app = FastAPI(title="GROWTH AI - Simple AI Service", version="0.1.0")

# CORS設定
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:4000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class LPGenerationRequest(BaseModel):
    company_name: str
    industry: str
    purpose: str

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "ai-service"}

@app.post("/generate-lp")
async def generate_lp(request: LPGenerationRequest):
    """簡易LP生成エンドポイント"""
    return {
        "lp_id": "demo-lp-123",
        "generation_time": 2.5,
        "preview_url": f"/lp/demo-lp-123/preview",
        "status": "completed",
        "demo": True,
        "company": request.company_name
    }

@app.post("/check-compliance")
async def check_compliance(content: dict):
    """コンプライアンスチェック"""
    return {
        "is_compliant": True,
        "violations": [],
        "suggestions": ["コンプライアンステストOK"],
        "auto_fixed": False
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)