from fastapi import FastAPI, HTTPException, Depends, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
import os
import asyncio
import json
from datetime import datetime
import redis.asyncio as redis
import asyncpg
from openai import AsyncOpenAI
import logging
from contextlib import asynccontextmanager

# ローカルモジュール
from services.lp_generator import LPGenerator
from services.compliance_checker import ComplianceChecker
from services.content_optimizer import ContentOptimizer
from services.image_generator import ImageGenerator
from services.heatmap_analyzer import HeatmapAnalyzer
from utils.prompts import PromptManager
from utils.cache import CacheManager

# ログ設定
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 環境変数
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
DATABASE_URL = os.getenv("DATABASE_URL")
REDIS_URL = os.getenv("REDIS_URL")

# グローバル変数
db_pool = None
redis_client = None
openai_client = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    """アプリケーションのライフサイクル管理"""
    global db_pool, redis_client, openai_client
    
    # 起動時
    logger.info("Starting AI service...")
    
    # データベース接続プール
    db_pool = await asyncpg.create_pool(DATABASE_URL)
    
    # Redis接続
    redis_client = await redis.from_url(REDIS_URL)
    
    # OpenAI クライアント
    openai_client = AsyncOpenAI(api_key=OPENAI_API_KEY)
    
    logger.info("AI service started successfully")
    
    yield
    
    # シャットダウン時
    logger.info("Shutting down AI service...")
    
    if db_pool:
        await db_pool.close()
    if redis_client:
        await redis_client.close()
    
    logger.info("AI service shut down")

# FastAPIアプリケーション
app = FastAPI(
    title="GROWTH AI - AI Service",
    version="0.1.0",
    lifespan=lifespan
)

# CORS設定
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:4000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# リクエスト/レスポンスモデル
class LPGenerationRequest(BaseModel):
    user_id: str
    mode: str = Field(default="quick", pattern="^(quick|normal)$")
    company_name: str
    website_url: Optional[str] = None
    industry: str
    purpose: str
    target_audience: Optional[str] = None
    unique_value: Optional[str] = None
    current_challenges: Optional[str] = None
    
class LPGenerationResponse(BaseModel):
    lp_id: str
    generation_time: float
    preview_url: str
    status: str
    
class ComplianceCheckRequest(BaseModel):
    content: str
    check_types: List[str] = ["medical", "misleading", "copyright"]
    
class ComplianceCheckResponse(BaseModel):
    is_compliant: bool
    violations: List[Dict[str, Any]]
    suggestions: List[str]
    auto_fixed: bool
    
class ContentOptimizationRequest(BaseModel):
    lp_id: str
    optimization_type: str = "conversion"
    target_metrics: Optional[Dict[str, float]] = None
    
class HeatmapAnalysisRequest(BaseModel):
    lp_id: str
    date_range: Optional[str] = "7d"
    
class HeatmapAnalysisResponse(BaseModel):
    hot_spots: List[Dict[str, Any]]
    cold_spots: List[Dict[str, Any]]
    scroll_depth: Dict[str, float]
    recommendations: List[str]

# エンドポイント
@app.get("/health")
async def health_check():
    """ヘルスチェック"""
    return {
        "status": "healthy",
        "timestamp": datetime.utcnow().isoformat(),
        "services": {
            "database": db_pool is not None,
            "redis": redis_client is not None,
            "openai": openai_client is not None
        }
    }

@app.post("/generate-lp", response_model=LPGenerationResponse)
async def generate_lp(
    request: LPGenerationRequest,
    background_tasks: BackgroundTasks
):
    """LP自動生成"""
    try:
        start_time = datetime.utcnow()
        
        # キャッシュチェック
        cache_key = f"lp:{request.company_name}:{request.industry}:{request.purpose}"
        cached = await redis_client.get(cache_key)
        
        if cached and request.mode == "quick":
            logger.info(f"Cache hit for {cache_key}")
            return json.loads(cached)
        
        # LP生成サービス
        generator = LPGenerator(openai_client, db_pool)
        
        # 生成実行
        lp_data = await generator.generate(
            mode=request.mode,
            company_info={
                "name": request.company_name,
                "url": request.website_url,
                "industry": request.industry,
                "purpose": request.purpose,
                "target_audience": request.target_audience,
                "unique_value": request.unique_value,
                "challenges": request.current_challenges
            }
        )
        
        # コンプライアンスチェック
        checker = ComplianceChecker()
        compliance_result = await checker.check(lp_data["content"])
        
        if not compliance_result["is_compliant"]:
            # 自動修正試行
            lp_data["content"] = await checker.auto_fix(
                lp_data["content"],
                compliance_result["violations"]
            )
        
        # データベース保存
        async with db_pool.acquire() as conn:
            lp_id = await conn.fetchval(
                """
                INSERT INTO landing_pages 
                (user_id, title, slug, industry, purpose, content, ai_model, generation_time, compliance_check)
                VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
                RETURNING id
                """,
                request.user_id,
                lp_data["title"],
                lp_data["slug"],
                request.industry,
                request.purpose,
                json.dumps(lp_data["content"]),
                "gpt-4-turbo" if request.mode == "normal" else "gpt-3.5-turbo",
                (datetime.utcnow() - start_time).total_seconds(),
                json.dumps(compliance_result)
            )
        
        # キャッシュ保存（バックグラウンド）
        if request.mode == "quick":
            background_tasks.add_task(
                redis_client.setex,
                cache_key,
                3600,  # 1時間
                json.dumps({
                    "lp_id": lp_id,
                    "generation_time": (datetime.utcnow() - start_time).total_seconds(),
                    "preview_url": f"/lp/{lp_id}/preview",
                    "status": "completed"
                })
            )
        
        # API使用量記録（バックグラウンド）
        background_tasks.add_task(
            record_api_usage,
            request.user_id,
            "gpt-4-turbo" if request.mode == "normal" else "gpt-3.5-turbo",
            lp_data.get("tokens_used", 0)
        )
        
        return LPGenerationResponse(
            lp_id=lp_id,
            generation_time=(datetime.utcnow() - start_time).total_seconds(),
            preview_url=f"/lp/{lp_id}/preview",
            status="completed"
        )
        
    except Exception as e:
        logger.error(f"LP generation error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/check-compliance", response_model=ComplianceCheckResponse)
async def check_compliance(request: ComplianceCheckRequest):
    """法令遵守チェック"""
    try:
        checker = ComplianceChecker()
        result = await checker.check(
            request.content,
            check_types=request.check_types
        )
        
        return ComplianceCheckResponse(
            is_compliant=result["is_compliant"],
            violations=result["violations"],
            suggestions=result["suggestions"],
            auto_fixed=result.get("auto_fixed", False)
        )
        
    except Exception as e:
        logger.error(f"Compliance check error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/optimize-content")
async def optimize_content(request: ContentOptimizationRequest):
    """コンテンツ最適化"""
    try:
        optimizer = ContentOptimizer(openai_client, db_pool)
        
        # 現在のLPデータ取得
        async with db_pool.acquire() as conn:
            lp_data = await conn.fetchrow(
                "SELECT * FROM landing_pages WHERE id = $1",
                request.lp_id
            )
        
        if not lp_data:
            raise HTTPException(status_code=404, detail="LP not found")
        
        # 最適化実行
        optimized_content = await optimizer.optimize(
            json.loads(lp_data["content"]),
            optimization_type=request.optimization_type,
            target_metrics=request.target_metrics
        )
        
        # 更新保存
        async with db_pool.acquire() as conn:
            await conn.execute(
                "UPDATE landing_pages SET content = $1, updated_at = $2 WHERE id = $3",
                json.dumps(optimized_content),
                datetime.utcnow(),
                request.lp_id
            )
        
        return {
            "status": "optimized",
            "improvements": optimized_content.get("improvements", []),
            "expected_impact": optimized_content.get("expected_impact", {})
        }
        
    except Exception as e:
        logger.error(f"Content optimization error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/analyze-heatmap", response_model=HeatmapAnalysisResponse)
async def analyze_heatmap(request: HeatmapAnalysisRequest):
    """ヒートマップ分析"""
    try:
        analyzer = HeatmapAnalyzer(db_pool)
        
        # ヒートマップデータ取得と分析
        analysis = await analyzer.analyze(
            request.lp_id,
            date_range=request.date_range
        )
        
        # AI による改善提案生成
        recommendations = await generate_heatmap_recommendations(
            analysis,
            openai_client
        )
        
        return HeatmapAnalysisResponse(
            hot_spots=analysis["hot_spots"],
            cold_spots=analysis["cold_spots"],
            scroll_depth=analysis["scroll_depth"],
            recommendations=recommendations
        )
        
    except Exception as e:
        logger.error(f"Heatmap analysis error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/generate-images")
async def generate_images(lp_id: str, num_images: int = 3):
    """LP用画像生成"""
    try:
        generator = ImageGenerator(openai_client)
        
        # LPデータ取得
        async with db_pool.acquire() as conn:
            lp_data = await conn.fetchrow(
                "SELECT industry, purpose, content FROM landing_pages WHERE id = $1",
                lp_id
            )
        
        if not lp_data:
            raise HTTPException(status_code=404, detail="LP not found")
        
        # 画像生成
        images = await generator.generate_for_lp(
            industry=lp_data["industry"],
            purpose=lp_data["purpose"],
            content=json.loads(lp_data["content"]),
            num_images=num_images
        )
        
        return {
            "status": "generated",
            "images": images
        }
        
    except Exception as e:
        logger.error(f"Image generation error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

# ヘルパー関数
async def record_api_usage(user_id: str, model: str, tokens: int):
    """API使用量記録"""
    try:
        # コスト計算
        costs = {
            "gpt-3.5-turbo": 0.002,  # per 1K tokens
            "gpt-4-turbo": 0.01,      # per 1K tokens
            "dalle-3": 0.04           # per image
        }
        
        cost = (tokens / 1000) * costs.get(model, 0.002)
        
        async with db_pool.acquire() as conn:
            await conn.execute(
                """
                INSERT INTO api_usage (user_id, endpoint, model, tokens, cost)
                VALUES ($1, $2, $3, $4, $5)
                """,
                user_id,
                "/generate-lp",
                model,
                tokens,
                cost
            )
            
    except Exception as e:
        logger.error(f"Failed to record API usage: {str(e)}")

async def generate_heatmap_recommendations(
    analysis: Dict[str, Any],
    client: AsyncOpenAI
) -> List[str]:
    """ヒートマップ分析に基づく改善提案生成"""
    try:
        prompt = f"""
        以下のヒートマップ分析結果から、具体的な改善提案を3つ生成してください：
        
        ホットスポット: {json.dumps(analysis['hot_spots'], ensure_ascii=False)}
        コールドスポット: {json.dumps(analysis['cold_spots'], ensure_ascii=False)}
        スクロール深度: {json.dumps(analysis['scroll_depth'], ensure_ascii=False)}
        
        改善提案は具体的で実装可能なものにしてください。
        """
        
        response = await client.chat.completions.create(
            model="gpt-3.5-turbo",
            messages=[
                {"role": "system", "content": "あなたはLP最適化の専門家です。"},
                {"role": "user", "content": prompt}
            ],
            temperature=0.7,
            max_tokens=500
        )
        
        content = response.choices[0].message.content
        recommendations = [line.strip() for line in content.split('\n') if line.strip()]
        
        return recommendations[:3]
        
    except Exception as e:
        logger.error(f"Failed to generate recommendations: {str(e)}")
        return ["ヒートマップデータを基に改善点を検討してください"]

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)