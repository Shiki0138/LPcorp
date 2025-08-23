"""
AI Engine API - FastAPI endpoints for all AI services
Advanced ML/AI integration with real-time optimization capabilities
"""

from fastapi import FastAPI, HTTPException, Depends, BackgroundTasks, Query, Path
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel, Field, validator
from typing import Optional, List, Dict, Any, Union
import asyncio
import logging
from datetime import datetime, timedelta
import json
import hashlib
from contextlib import asynccontextmanager

# AI Engine imports
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from core.ai_engine import AIEngine, initialize_ai_engine, GenerationRequest
from services.competitive_analyzer import CompetitiveAnalyzer, AnalysisDepth, GenerationRequest as CompGenRequest
from services.trend_predictor import TrendPredictor
from services.vector_database import VectorDatabaseManager
from services.learning_optimizer import ABTestManager, TestVariant

# Configuration
import os
from dotenv import load_dotenv
load_dotenv()


# Pydantic Models
class LPGenerationRequest(BaseModel):
    industry: str = Field(..., description="Target industry")
    target_audience: str = Field(..., description="Primary target audience")
    goal: str = Field(default="lead_generation", description="Primary goal: lead_generation, sales, branding")
    tone: str = Field(default="professional", description="Content tone: professional, casual, urgent")
    length: str = Field(default="medium", description="Content length: short, medium, long")
    company_name: str = Field(..., description="Company name")
    unique_value_proposition: Optional[str] = Field(None, description="Unique value proposition")
    special_requirements: List[str] = Field(default=[], description="Special requirements or constraints")
    reference_competitors: List[str] = Field(default=[], description="Competitor websites for analysis")
    
    @validator('goal')
    def validate_goal(cls, v):
        allowed_goals = ['lead_generation', 'sales', 'branding', 'education', 'engagement']
        if v not in allowed_goals:
            raise ValueError(f'Goal must be one of {allowed_goals}')
        return v
    
    @validator('tone')
    def validate_tone(cls, v):
        allowed_tones = ['professional', 'casual', 'urgent', 'friendly', 'authoritative', 'conversational']
        if v not in allowed_tones:
            raise ValueError(f'Tone must be one of {allowed_tones}')
        return v


class LPGenerationResponse(BaseModel):
    generation_id: str
    status: str
    generation_time: float
    content: Dict[str, Any]
    design_specs: Dict[str, Any]
    seo_optimization: Dict[str, Any]
    quality_score: float
    patterns_used: List[str]
    recommendations: List[str]


class CompetitiveAnalysisRequest(BaseModel):
    industry: str = Field(..., description="Industry to analyze")
    keywords: List[str] = Field(..., description="Keywords for competitor discovery")
    target_audience: str = Field(..., description="Target audience description")
    competitor_urls: Optional[List[str]] = Field(default=None, description="Specific competitor URLs")
    analysis_depth: str = Field(default="standard", description="Analysis depth: quick, standard, deep")
    max_competitors: int = Field(default=5, description="Maximum competitors to analyze")
    
    @validator('analysis_depth')
    def validate_depth(cls, v):
        allowed_depths = ['quick', 'standard', 'deep']
        if v not in allowed_depths:
            raise ValueError(f'Analysis depth must be one of {allowed_depths}')
        return v


class TrendPredictionRequest(BaseModel):
    industry: str = Field(..., description="Industry for trend analysis")
    keywords: List[str] = Field(..., description="Keywords to analyze trends for")
    prediction_horizon: int = Field(default=90, description="Prediction horizon in days")
    include_economic_factors: bool = Field(default=True, description="Include economic trend analysis")


class ABTestRequest(BaseModel):
    test_name: str = Field(..., description="Name of the A/B test")
    test_description: str = Field(..., description="Description of what is being tested")
    control_variant: Dict[str, Any] = Field(..., description="Control variant configuration")
    test_variants: List[Dict[str, Any]] = Field(..., description="Test variant configurations")
    primary_metric: str = Field(default="conversion_rate", description="Primary success metric")
    significance_level: float = Field(default=0.05, description="Statistical significance level")
    
    @validator('significance_level')
    def validate_significance(cls, v):
        if not 0.001 <= v <= 0.1:
            raise ValueError('Significance level must be between 0.001 and 0.1')
        return v


class VisitorInteractionData(BaseModel):
    test_id: str
    variant_id: str
    converted: bool = False
    time_on_page: Optional[float] = None
    bounce_rate: Optional[bool] = None
    scroll_depth: Optional[float] = None
    revenue: Optional[float] = None
    custom_metrics: Optional[Dict[str, Any]] = None


class OptimizationRequest(BaseModel):
    current_lp_data: Dict[str, Any] = Field(..., description="Current landing page data")
    target_metric: str = Field(default="conversion_rate", description="Metric to optimize")
    constraints: Optional[Dict[str, Any]] = Field(None, description="Optimization constraints")
    historical_data: Optional[List[Dict]] = Field(None, description="Historical performance data")


# Global variables
ai_engine = None
competitive_analyzer = None
trend_predictor = None
vector_db_manager = None
ab_test_manager = None

# Authentication
security = HTTPBearer()

async def verify_api_key(credentials: HTTPAuthorizationCredentials = Depends(security)):
    """Verify API key authentication"""
    expected_key = os.getenv("AI_ENGINE_API_KEY")
    if not expected_key:
        # In development, allow any key
        return credentials.credentials
    
    if credentials.credentials != expected_key:
        raise HTTPException(status_code=401, detail="Invalid API key")
    
    return credentials.credentials


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan management"""
    global ai_engine, competitive_analyzer, trend_predictor, vector_db_manager, ab_test_manager
    
    # Startup
    logging.info("Starting AI Engine API...")
    
    try:
        # Initialize configuration
        config = {
            'openai_api_key': os.getenv('OPENAI_API_KEY'),
            'google_api_key': os.getenv('GOOGLE_API_KEY'),
            'pinecone_api_key': os.getenv('PINECONE_API_KEY'),
            'pinecone_environment': os.getenv('PINECONE_ENVIRONMENT'),
            'vector_db_type': os.getenv('VECTOR_DB_TYPE', 'faiss'),
            'redis_url': os.getenv('REDIS_URL', 'redis://localhost:6379'),
            'database_url': os.getenv('DATABASE_URL')
        }
        
        # Initialize AI Engine
        ai_engine = await initialize_ai_engine(config)
        
        # Initialize Competitive Analyzer
        competitive_analyzer = CompetitiveAnalyzer(config)
        
        # Initialize Trend Predictor
        trend_predictor = TrendPredictor(config)
        
        # Initialize Vector Database
        vector_db_config = {
            'db_type': config.get('vector_db_type', 'faiss'),
            'pinecone': {
                'api_key': config.get('pinecone_api_key'),
                'environment': config.get('pinecone_environment')
            },
            'embeddings': {
                'openai_api_key': config.get('openai_api_key')
            }
        }
        vector_db_manager = VectorDatabaseManager(vector_db_config)
        await vector_db_manager.initialize()
        
        # Initialize A/B Test Manager
        ab_test_manager = ABTestManager({
            'significance_threshold': 0.05,
            'minimum_detectable_effect': 0.05
        })
        
        logging.info("AI Engine API startup completed")
        
    except Exception as e:
        logging.error(f"Failed to initialize AI Engine API: {e}")
        raise
    
    yield
    
    # Shutdown
    logging.info("Shutting down AI Engine API...")


# FastAPI app
app = FastAPI(
    title="AI Engine API",
    description="Advanced AI-powered landing page optimization and analytics",
    version="2.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure appropriately for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Health check endpoint
@app.get("/health", tags=["System"])
async def health_check():
    """System health check"""
    return {
        "status": "healthy",
        "timestamp": datetime.utcnow().isoformat(),
        "version": "2.0.0",
        "services": {
            "ai_engine": ai_engine is not None,
            "competitive_analyzer": competitive_analyzer is not None,
            "trend_predictor": trend_predictor is not None,
            "vector_database": vector_db_manager is not None,
            "ab_test_manager": ab_test_manager is not None
        }
    }


@app.get("/system/status", tags=["System"])
async def system_status(api_key: str = Depends(verify_api_key)):
    """Get detailed system status"""
    try:
        status = {}
        
        if ai_engine:
            status['ai_engine'] = await ai_engine.get_engine_status()
        
        if vector_db_manager:
            status['vector_database'] = await vector_db_manager.get_database_stats()
        
        if ab_test_manager:
            status['learning_system'] = await ab_test_manager.get_learning_summary()
        
        return status
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# LP Generation Endpoints
@app.post("/generate/lp", response_model=LPGenerationResponse, tags=["Generation"])
async def generate_landing_page(
    request: LPGenerationRequest,
    background_tasks: BackgroundTasks,
    api_key: str = Depends(verify_api_key)
):
    """Generate optimized landing page using AI"""
    try:
        if not ai_engine:
            raise HTTPException(status_code=503, detail="AI Engine not available")
        
        # Convert to internal request format
        generation_request = GenerationRequest(
            industry=request.industry,
            target_audience=request.target_audience,
            goal=request.goal,
            tone=request.tone,
            length=request.length,
            special_requirements=request.special_requirements,
            reference_competitors=request.reference_competitors
        )
        
        # Generate LP
        result = await ai_engine.generate_lp_content(generation_request)
        
        # Store successful patterns in background
        background_tasks.add_task(
            store_generation_pattern,
            request.dict(),
            result,
            result.get('quality_score', 0.0)
        )
        
        return LPGenerationResponse(
            generation_id=f"gen_{hashlib.md5(str(datetime.now()).encode()).hexdigest()[:8]}",
            status="completed",
            generation_time=result.get('generation_time', 0.0),
            content=result.get('content', {}),
            design_specs=result.get('design', {}),
            seo_optimization=result.get('seo', {}),
            quality_score=result.get('quality_score', 0.0),
            patterns_used=result.get('patterns_used', []),
            recommendations=result.get('requirements_analysis', {}).get('recommendations', [])
        )
        
    except Exception as e:
        logging.error(f"LP generation failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/generate/optimize", tags=["Generation"])
async def optimize_landing_page(
    request: OptimizationRequest,
    api_key: str = Depends(verify_api_key)
):
    """Optimize existing landing page using AI recommendations"""
    try:
        if not ai_engine or not vector_db_manager:
            raise HTTPException(status_code=503, detail="AI services not available")
        
        # Find similar successful patterns
        similar_patterns = await vector_db_manager.find_similar_patterns(
            query_text=json.dumps(request.current_lp_data),
            top_k=5,
            min_score=0.7
        )
        
        # Generate optimization recommendations
        recommendations = []
        
        for pattern in similar_patterns:
            if pattern['performance_metrics'].get('conversion_rate', 0) > 0.1:
                recommendations.append({
                    'type': 'pattern_based',
                    'description': f"Apply successful pattern from {pattern['pattern_type']}",
                    'expected_improvement': pattern['performance_metrics'].get('conversion_rate', 0),
                    'confidence': pattern['similarity_score']
                })
        
        # Add ML-based recommendations
        if ab_test_manager:
            # Create dummy variant for analysis
            from services.learning_optimizer import TestVariant
            current_variant = TestVariant(
                variant_id="current",
                name="Current",
                description="Current LP",
                traffic_allocation=1.0,
                content=request.current_lp_data.get('content', {}),
                design=request.current_lp_data.get('design', {})
            )
            
            ml_recommendations = await ab_test_manager.get_optimization_recommendations(
                current_variant, request.target_metric
            )
            
            for rec in ml_recommendations:
                recommendations.append({
                    'type': 'ml_based',
                    'description': rec,
                    'expected_improvement': 0.05,  # Default 5% improvement estimate
                    'confidence': 0.7
                })
        
        return {
            'status': 'success',
            'optimization_id': f"opt_{hashlib.md5(str(datetime.now()).encode()).hexdigest()[:8]}",
            'recommendations': recommendations,
            'similar_patterns_found': len(similar_patterns),
            'generated_at': datetime.utcnow().isoformat()
        }
        
    except Exception as e:
        logging.error(f"LP optimization failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# Competitive Analysis Endpoints
@app.post("/analysis/competitive", tags=["Analysis"])
async def analyze_competitors(
    request: CompetitiveAnalysisRequest,
    background_tasks: BackgroundTasks,
    api_key: str = Depends(verify_api_key)
):
    """Analyze competitive landscape and generate differentiation strategies"""
    try:
        if not competitive_analyzer:
            raise HTTPException(status_code=503, detail="Competitive Analyzer not available")
        
        analysis_id = f"comp_{hashlib.md5(f'{request.industry}_{datetime.now()}'.encode()).hexdigest()[:8]}"
        
        # Start analysis in background for long-running operations
        background_tasks.add_task(
            run_competitive_analysis,
            analysis_id,
            request
        )
        
        return {
            'analysis_id': analysis_id,
            'status': 'started',
            'message': 'Competitive analysis started. Use /analysis/competitive/{analysis_id}/status to check progress.',
            'estimated_completion': (datetime.now() + timedelta(minutes=10)).isoformat()
        }
        
    except Exception as e:
        logging.error(f"Competitive analysis failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/analysis/competitive/{analysis_id}/status", tags=["Analysis"])
async def get_competitive_analysis_status(
    analysis_id: str = Path(..., description="Analysis ID"),
    api_key: str = Depends(verify_api_key)
):
    """Get status of competitive analysis"""
    try:
        # In a real implementation, you'd track analysis status in database
        # For now, return mock status
        
        return {
            'analysis_id': analysis_id,
            'status': 'completed',  # Mock completed status
            'progress': 100,
            'competitors_analyzed': 5,
            'strategies_generated': 8,
            'results_available': True,
            'completed_at': datetime.utcnow().isoformat()
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/analysis/competitive/{analysis_id}/results", tags=["Analysis"])
async def get_competitive_analysis_results(
    analysis_id: str = Path(..., description="Analysis ID"),
    api_key: str = Depends(verify_api_key)
):
    """Get results of competitive analysis"""
    try:
        # Mock results - in production, retrieve from database
        mock_results = {
            'analysis_id': analysis_id,
            'competitors_analyzed': [
                {
                    'domain': 'competitor1.com',
                    'analysis_score': 0.85,
                    'key_strengths': ['Strong CTA design', 'Clear value proposition'],
                    'potential_weaknesses': ['Slow loading speed', 'Limited social proof']
                },
                {
                    'domain': 'competitor2.com',
                    'analysis_score': 0.78,
                    'key_strengths': ['Excellent mobile experience', 'Strong SEO'],
                    'potential_weaknesses': ['Weak headline', 'Cluttered design']
                }
            ],
            'differentiation_strategies': [
                {
                    'strategy_id': 'diff_001',
                    'title': 'Unique Value Messaging',
                    'description': 'Focus on emotional benefits that competitors ignore',
                    'impact_score': 0.8,
                    'implementation_difficulty': 'medium'
                },
                {
                    'strategy_id': 'diff_002',
                    'title': 'Advanced Interactive Features',
                    'description': 'Implement interactive calculators and tools',
                    'impact_score': 0.9,
                    'implementation_difficulty': 'hard'
                }
            ],
            'market_opportunities': [
                'Underserved mobile optimization',
                'Lack of personalization in competitor offerings',
                'Weak social proof across competitive landscape'
            ],
            'generated_at': datetime.utcnow().isoformat()
        }
        
        return mock_results
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# Trend Prediction Endpoints
@app.post("/trends/predict", tags=["Trends"])
async def predict_industry_trends(
    request: TrendPredictionRequest,
    background_tasks: BackgroundTasks,
    api_key: str = Depends(verify_api_key)
):
    """Predict industry trends and market opportunities"""
    try:
        if not trend_predictor:
            raise HTTPException(status_code=503, detail="Trend Predictor not available")
        
        # Start prediction in background
        prediction_id = f"trend_{hashlib.md5(f'{request.industry}_{datetime.now()}'.encode()).hexdigest()[:8]}"
        
        background_tasks.add_task(
            run_trend_prediction,
            prediction_id,
            request
        )
        
        return {
            'prediction_id': prediction_id,
            'status': 'started',
            'message': 'Trend prediction started. Use /trends/predict/{prediction_id}/status to check progress.',
            'estimated_completion': (datetime.now() + timedelta(minutes=15)).isoformat()
        }
        
    except Exception as e:
        logging.error(f"Trend prediction failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/trends/predict/{prediction_id}/results", tags=["Trends"])
async def get_trend_prediction_results(
    prediction_id: str = Path(..., description="Prediction ID"),
    api_key: str = Depends(verify_api_key)
):
    """Get trend prediction results"""
    try:
        # Mock results - in production, retrieve from database/cache
        mock_results = {
            'prediction_id': prediction_id,
            'industry': 'SaaS',
            'prediction_horizon_days': 90,
            'trending_up': [
                {
                    'trend': 'AI-powered automation',
                    'growth_rate': 0.25,
                    'confidence': 0.92,
                    'peak_prediction': '2024-06-15'
                },
                {
                    'trend': 'No-code solutions',
                    'growth_rate': 0.18,
                    'confidence': 0.85,
                    'peak_prediction': '2024-07-20'
                }
            ],
            'emerging_trends': [
                {
                    'trend': 'Voice-enabled interfaces',
                    'adoption_rate': 0.15,
                    'confidence': 0.73,
                    'opportunity_window': '2024-05-01 to 2024-08-31'
                }
            ],
            'predicted_disruptions': [
                {
                    'type': 'Technology Disruption',
                    'probability': 0.68,
                    'timeframe': '3-6 months',
                    'impact': 'Medium-High'
                }
            ],
            'strategic_recommendations': {
                'short_term_actions': [
                    'Invest in AI integration capabilities',
                    'Monitor no-code platform developments',
                    'Test voice interface prototypes'
                ],
                'medium_term_preparations': [
                    'Develop partnership with AI vendors',
                    'Build no-code integration features'
                ],
                'long_term_positioning': [
                    'Position as AI-first solution provider',
                    'Establish voice interface leadership'
                ]
            },
            'confidence_score': 0.84,
            'generated_at': datetime.utcnow().isoformat()
        }
        
        return mock_results
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# A/B Testing Endpoints
@app.post("/testing/create", tags=["A/B Testing"])
async def create_ab_test(
    request: ABTestRequest,
    api_key: str = Depends(verify_api_key)
):
    """Create a new A/B test"""
    try:
        if not ab_test_manager:
            raise HTTPException(status_code=503, detail="A/B Test Manager not available")
        
        # Convert request to TestVariant objects
        from services.learning_optimizer import TestVariant, SignificanceLevel
        
        control_variant = TestVariant(
            variant_id="control",
            name="Control",
            description="Original version",
            traffic_allocation=0.5,
            content=request.control_variant.get('content', {}),
            design=request.control_variant.get('design', {})
        )
        
        test_variants = []
        for i, variant_data in enumerate(request.test_variants):
            test_variant = TestVariant(
                variant_id=f"test_v{i+1}",
                name=variant_data.get('name', f'Test Variant {i+1}'),
                description=variant_data.get('description', ''),
                traffic_allocation=variant_data.get('traffic_allocation', 0.5 / len(request.test_variants)),
                content=variant_data.get('content', {}),
                design=variant_data.get('design', {})
            )
            test_variants.append(test_variant)
        
        # Determine significance level
        if request.significance_level <= 0.01:
            sig_level = SignificanceLevel.HIGH
        elif request.significance_level <= 0.05:
            sig_level = SignificanceLevel.MEDIUM
        else:
            sig_level = SignificanceLevel.LOW
        
        # Create test
        test_id = await ab_test_manager.create_ab_test(
            name=request.test_name,
            description=request.test_description,
            control_variant=control_variant,
            test_variants=test_variants,
            primary_metric=request.primary_metric,
            significance_level=sig_level
        )
        
        return {
            'test_id': test_id,
            'status': 'created',
            'message': 'A/B test created successfully. Use /testing/start/{test_id} to begin.',
            'variants': {
                'control': control_variant.variant_id,
                'test': [v.variant_id for v in test_variants]
            }
        }
        
    except Exception as e:
        logging.error(f"A/B test creation failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/testing/start/{test_id}", tags=["A/B Testing"])
async def start_ab_test(
    test_id: str = Path(..., description="Test ID"),
    api_key: str = Depends(verify_api_key)
):
    """Start running an A/B test"""
    try:
        if not ab_test_manager:
            raise HTTPException(status_code=503, detail="A/B Test Manager not available")
        
        success = await ab_test_manager.start_test(test_id)
        
        if not success:
            raise HTTPException(status_code=400, detail="Failed to start test")
        
        return {
            'test_id': test_id,
            'status': 'running',
            'started_at': datetime.utcnow().isoformat(),
            'message': 'A/B test started successfully'
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/testing/interaction", tags=["A/B Testing"])
async def record_visitor_interaction(
    interaction: VisitorInteractionData,
    api_key: str = Depends(verify_api_key)
):
    """Record visitor interaction for A/B test"""
    try:
        if not ab_test_manager:
            raise HTTPException(status_code=503, detail="A/B Test Manager not available")
        
        # Prepare visitor data
        visitor_data = {
            'converted': interaction.converted,
            'time_on_page': interaction.time_on_page,
            'bounce_rate': interaction.bounce_rate,
            'scroll_depth': interaction.scroll_depth,
            'revenue': interaction.revenue
        }
        
        # Add custom metrics if provided
        if interaction.custom_metrics:
            visitor_data.update(interaction.custom_metrics)
        
        success = await ab_test_manager.record_visitor_interaction(
            interaction.test_id,
            interaction.variant_id,
            visitor_data
        )
        
        if not success:
            raise HTTPException(status_code=400, detail="Failed to record interaction")
        
        return {
            'status': 'recorded',
            'test_id': interaction.test_id,
            'variant_id': interaction.variant_id,
            'timestamp': datetime.utcnow().isoformat()
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/testing/{test_id}/status", tags=["A/B Testing"])
async def get_ab_test_status(
    test_id: str = Path(..., description="Test ID"),
    api_key: str = Depends(verify_api_key)
):
    """Get A/B test status and results"""
    try:
        if not ab_test_manager:
            raise HTTPException(status_code=503, detail="A/B Test Manager not available")
        
        status = await ab_test_manager.get_test_status(test_id)
        
        if not status:
            raise HTTPException(status_code=404, detail="Test not found")
        
        return status
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/learning/summary", tags=["Learning"])
async def get_learning_summary(api_key: str = Depends(verify_api_key)):
    """Get summary of all learning from A/B tests and optimizations"""
    try:
        if not ab_test_manager:
            raise HTTPException(status_code=503, detail="Learning system not available")
        
        summary = await ab_test_manager.get_learning_summary()
        return summary
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# Vector Database Endpoints
@app.post("/patterns/store", tags=["Patterns"])
async def store_pattern(
    pattern_data: Dict[str, Any],
    api_key: str = Depends(verify_api_key)
):
    """Store a learning pattern in vector database"""
    try:
        if not vector_db_manager:
            raise HTTPException(status_code=503, detail="Vector database not available")
        
        success = await vector_db_manager.store_learning_pattern(pattern_data)
        
        if not success:
            raise HTTPException(status_code=400, detail="Failed to store pattern")
        
        return {
            'status': 'stored',
            'pattern_id': pattern_data.get('pattern_id', 'unknown'),
            'stored_at': datetime.utcnow().isoformat()
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/patterns/search", tags=["Patterns"])
async def search_similar_patterns(
    query: str = Field(..., description="Search query"),
    pattern_type: Optional[str] = Query(None, description="Filter by pattern type"),
    top_k: int = Query(5, description="Number of results to return"),
    min_score: float = Query(0.7, description="Minimum similarity score"),
    api_key: str = Depends(verify_api_key)
):
    """Search for similar patterns in vector database"""
    try:
        if not vector_db_manager:
            raise HTTPException(status_code=503, detail="Vector database not available")
        
        results = await vector_db_manager.find_similar_patterns(
            query_text=query,
            pattern_type=pattern_type,
            top_k=top_k,
            min_score=min_score
        )
        
        return {
            'query': query,
            'results_count': len(results),
            'patterns': results,
            'searched_at': datetime.utcnow().isoformat()
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# Background task functions
async def store_generation_pattern(request_data: Dict, result: Dict, quality_score: float):
    """Store successful generation pattern"""
    try:
        if quality_score > 0.8 and vector_db_manager:
            pattern_data = {
                'pattern_id': f"gen_{hashlib.md5(str(datetime.now()).encode()).hexdigest()[:8]}",
                'pattern_type': 'generation_success',
                'industry': request_data.get('industry'),
                'target_audience': request_data.get('target_audience'),
                'content_structure': result.get('content', {}),
                'performance_metrics': {
                    'quality_score': quality_score,
                    'generation_time': result.get('generation_time', 0.0)
                },
                'created_at': datetime.now().isoformat()
            }
            
            await vector_db_manager.store_learning_pattern(pattern_data)
            logging.info(f"Stored successful generation pattern with quality score {quality_score}")
    except Exception as e:
        logging.error(f"Failed to store generation pattern: {e}")


async def run_competitive_analysis(analysis_id: str, request: CompetitiveAnalysisRequest):
    """Run competitive analysis in background"""
    try:
        logging.info(f"Starting competitive analysis {analysis_id}")
        
        # In a real implementation, this would:
        # 1. Use CompetitiveAnalyzer to discover and analyze competitors
        # 2. Store results in database
        # 3. Update analysis status
        
        # For now, simulate processing time
        await asyncio.sleep(30)  # Simulate 30 seconds of processing
        
        logging.info(f"Completed competitive analysis {analysis_id}")
        
    except Exception as e:
        logging.error(f"Competitive analysis {analysis_id} failed: {e}")


async def run_trend_prediction(prediction_id: str, request: TrendPredictionRequest):
    """Run trend prediction in background"""
    try:
        logging.info(f"Starting trend prediction {prediction_id}")
        
        # In a real implementation, this would:
        # 1. Use TrendPredictor to collect data and generate predictions
        # 2. Store results in database/cache
        # 3. Update prediction status
        
        # For now, simulate processing time
        await asyncio.sleep(60)  # Simulate 1 minute of processing
        
        logging.info(f"Completed trend prediction {prediction_id}")
        
    except Exception as e:
        logging.error(f"Trend prediction {prediction_id} failed: {e}")


# Error handlers
@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    return {
        "error": exc.detail,
        "status_code": exc.status_code,
        "timestamp": datetime.utcnow().isoformat()
    }


if __name__ == "__main__":
    import uvicorn
    
    # Configure logging
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    # Run the API server
    uvicorn.run(
        "ai_engine_api:app",
        host="0.0.0.0",
        port=8001,
        reload=True,
        log_level="info"
    )