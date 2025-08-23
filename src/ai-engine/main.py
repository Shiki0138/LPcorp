"""
AI Engine Main Entry Point
Advanced AI-powered landing page optimization system
"""

import asyncio
import logging
import sys
import os
from pathlib import Path
from typing import Dict, Any

# Add the current directory to Python path
current_dir = Path(__file__).parent
sys.path.insert(0, str(current_dir))

# Core imports
from config.ai_config import get_config_manager, validate_api_keys, health_check_config
from utils.performance_monitor import PerformanceMonitor
from core.ai_engine import initialize_ai_engine, get_ai_engine
from services.competitive_analyzer import CompetitiveAnalyzer
from services.trend_predictor import TrendPredictor
from services.vector_database import VectorDatabaseManager
from services.learning_optimizer import ABTestManager


class AIEngineSystem:
    """Main AI Engine System orchestrator"""
    
    def __init__(self, config_file: str = None):
        self.logger = logging.getLogger("ai_engine_system")
        
        # Initialize configuration
        self.config_manager = get_config_manager(config_file)
        self.config = self.config_manager.config
        
        # Core components
        self.ai_engine = None
        self.competitive_analyzer = None
        self.trend_predictor = None
        self.vector_db_manager = None
        self.ab_test_manager = None
        self.performance_monitor = None
        
        # System state
        self.is_initialized = False
        self.is_running = False
    
    async def initialize(self) -> bool:
        """Initialize all AI Engine components"""
        try:
            self.logger.info("Initializing AI Engine System...")
            
            # Validate configuration
            validate_api_keys()
            health_status = health_check_config()
            self.logger.info(f"Configuration health: {health_status}")
            
            # Initialize performance monitor first
            await self._initialize_performance_monitor()
            
            # Initialize core AI engine
            await self._initialize_ai_engine()
            
            # Initialize vector database
            await self._initialize_vector_database()
            
            # Initialize competitive analyzer
            await self._initialize_competitive_analyzer()
            
            # Initialize trend predictor
            await self._initialize_trend_predictor()
            
            # Initialize A/B test manager
            await self._initialize_ab_test_manager()
            
            self.is_initialized = True
            self.logger.info("AI Engine System initialized successfully")
            
            return True
            
        except Exception as e:
            self.logger.error(f"AI Engine System initialization failed: {e}")
            raise
    
    async def _initialize_performance_monitor(self):
        """Initialize performance monitoring system"""
        try:
            if not self.config.performance.enable_metrics_collection:
                self.logger.info("Performance monitoring disabled by configuration")
                return
            
            monitor_config = {
                'monitoring_interval': self.config.performance.metrics_export_interval_seconds,
                'redis_url': self.config.performance.redis_url,
                'auto_optimization': self.config.enable_ml_optimization
            }
            
            self.performance_monitor = PerformanceMonitor(monitor_config)
            self.performance_monitor.start_monitoring()
            
            self.logger.info("Performance monitoring initialized")
            
        except Exception as e:
            self.logger.error(f"Performance monitor initialization failed: {e}")
            # Continue without performance monitoring
    
    async def _initialize_ai_engine(self):
        """Initialize core AI engine"""
        try:
            engine_config = {
                'openai_api_key': self.config.openai.api_key,
                'model_primary': self.config.openai.model_primary,
                'model_secondary': self.config.openai.model_secondary,
                'max_tokens': self.config.openai.max_tokens,
                'temperature': self.config.openai.temperature
            }
            
            self.ai_engine = await initialize_ai_engine(engine_config)
            self.logger.info("AI Engine core initialized")
            
        except Exception as e:
            self.logger.error(f"AI Engine initialization failed: {e}")
            raise
    
    async def _initialize_vector_database(self):
        """Initialize vector database for pattern storage"""
        try:
            if not self.config.enable_vector_storage:
                self.logger.info("Vector storage disabled by configuration")
                return
            
            vector_config = {
                'db_type': self.config.vector_db.provider,
                'dimension': self.config.vector_db.dimension,
                'pinecone': {
                    'api_key': self.config.vector_db.pinecone_api_key,
                    'environment': self.config.vector_db.pinecone_environment,
                    'index_name': self.config.vector_db.pinecone_index_name
                },
                'qdrant': {
                    'host': self.config.vector_db.qdrant_host,
                    'port': self.config.vector_db.qdrant_port,
                    'api_key': self.config.vector_db.qdrant_api_key
                },
                'faiss': {
                    'index_path': self.config.vector_db.faiss_index_path,
                    'metadata_path': self.config.vector_db.faiss_metadata_path
                },
                'embeddings': {
                    'openai_api_key': self.config.openai.api_key,
                    'model': self.config.openai.embedding_model
                }
            }
            
            self.vector_db_manager = VectorDatabaseManager(vector_config)
            await self.vector_db_manager.initialize()
            
            self.logger.info(f"Vector database initialized ({self.config.vector_db.provider})")
            
        except Exception as e:
            self.logger.error(f"Vector database initialization failed: {e}")
            # Continue without vector storage
    
    async def _initialize_competitive_analyzer(self):
        """Initialize competitive analysis system"""
        try:
            if not self.config.enable_competitive_analysis:
                self.logger.info("Competitive analysis disabled by configuration")
                return
            
            comp_config = {
                'openai_api_key': self.config.openai.api_key,
                'google_api_key': self.config.competitive_analysis.google_api_key,
                'google_cse_id': self.config.competitive_analysis.google_cse_id,
                'rate_limit_delay': self.config.competitive_analysis.rate_limit_delay,
                'max_competitors': self.config.competitive_analysis.max_competitors,
                'user_agent': self.config.competitive_analysis.user_agent,
                'timeout': self.config.competitive_analysis.timeout
            }
            
            self.competitive_analyzer = CompetitiveAnalyzer(comp_config)
            self.logger.info("Competitive analyzer initialized")
            
        except Exception as e:
            self.logger.error(f"Competitive analyzer initialization failed: {e}")
            # Continue without competitive analysis
    
    async def _initialize_trend_predictor(self):
        """Initialize trend prediction system"""
        try:
            if not self.config.enable_trend_prediction:
                self.logger.info("Trend prediction disabled by configuration")
                return
            
            trend_config = {
                'openai_api_key': self.config.openai.api_key,
                'google_api_key': self.config.competitive_analysis.google_api_key,
                'news_api_key': self.config.trend_prediction.news_api_key,
                'twitter_api_key': self.config.trend_prediction.twitter_api_key,
                'reddit_client_id': self.config.trend_prediction.reddit_client_id,
                'default_horizon_days': self.config.trend_prediction.default_horizon_days,
                'confidence_threshold': self.config.trend_prediction.confidence_threshold,
                'cache_ttl_hours': self.config.trend_prediction.cache_ttl_hours
            }
            
            self.trend_predictor = TrendPredictor(trend_config)
            self.logger.info("Trend predictor initialized")
            
        except Exception as e:
            self.logger.error(f"Trend predictor initialization failed: {e}")
            # Continue without trend prediction
    
    async def _initialize_ab_test_manager(self):
        """Initialize A/B testing and learning system"""
        try:
            if not self.config.enable_ab_testing:
                self.logger.info("A/B testing disabled by configuration")
                return
            
            ab_config = {
                'significance_threshold': self.config.ab_testing.default_significance_level,
                'minimum_detectable_effect': 0.05,  # 5% minimum improvement
                'default_power': self.config.ab_testing.default_power,
                'minimum_sample_size': self.config.ab_testing.minimum_sample_size,
                'minimum_duration_days': self.config.ab_testing.minimum_duration_days,
                'maximum_duration_days': self.config.ab_testing.maximum_duration_days,
                'enable_early_stopping': self.config.ab_testing.enable_early_stopping
            }
            
            self.ab_test_manager = ABTestManager(ab_config)
            self.logger.info("A/B test manager initialized")
            
        except Exception as e:
            self.logger.error(f"A/B test manager initialization failed: {e}")
            # Continue without A/B testing
    
    async def start(self):
        """Start the AI Engine System"""
        try:
            if not self.is_initialized:
                await self.initialize()
            
            self.is_running = True
            self.logger.info("AI Engine System started successfully")
            
            # Start background tasks
            await self._start_background_tasks()
            
        except Exception as e:
            self.logger.error(f"AI Engine System start failed: {e}")
            raise
    
    async def _start_background_tasks(self):
        """Start background tasks for maintenance and optimization"""
        try:
            # Background task for cleaning up old data
            asyncio.create_task(self._cleanup_task())
            
            # Background task for model training updates
            if self.config.enable_ml_optimization:
                asyncio.create_task(self._ml_optimization_task())
            
            self.logger.info("Background tasks started")
            
        except Exception as e:
            self.logger.error(f"Background tasks startup failed: {e}")
    
    async def _cleanup_task(self):
        """Background task for cleaning up old data"""
        try:
            while self.is_running:
                # Clean up old vector data
                if self.vector_db_manager:
                    cleaned_count = await self.vector_db_manager.cleanup_old_vectors(days=30)
                    if cleaned_count > 0:
                        self.logger.info(f"Cleaned up {cleaned_count} old vector records")
                
                # Sleep for 1 hour
                await asyncio.sleep(3600)
                
        except Exception as e:
            self.logger.error(f"Cleanup task error: {e}")
    
    async def _ml_optimization_task(self):
        """Background task for ML model optimization"""
        try:
            while self.is_running:
                # Update ML models with new data
                if self.ab_test_manager:
                    # This would trigger model retraining with new A/B test data
                    pass
                
                # Sleep for 6 hours
                await asyncio.sleep(21600)
                
        except Exception as e:
            self.logger.error(f"ML optimization task error: {e}")
    
    async def stop(self):
        """Stop the AI Engine System"""
        try:
            self.is_running = False
            
            # Stop performance monitoring
            if self.performance_monitor:
                self.performance_monitor.stop_monitoring()
            
            # Close async resources
            if hasattr(self.competitive_analyzer, '__aexit__'):
                await self.competitive_analyzer.__aexit__(None, None, None)
            
            self.logger.info("AI Engine System stopped")
            
        except Exception as e:
            self.logger.error(f"AI Engine System stop error: {e}")
    
    def get_system_status(self) -> Dict[str, Any]:
        """Get comprehensive system status"""
        try:
            status = {
                'system': {
                    'initialized': self.is_initialized,
                    'running': self.is_running,
                    'environment': self.config.environment,
                    'debug': self.config.debug
                },
                'components': {
                    'ai_engine': self.ai_engine is not None,
                    'competitive_analyzer': self.competitive_analyzer is not None,
                    'trend_predictor': self.trend_predictor is not None,
                    'vector_database': self.vector_db_manager is not None,
                    'ab_test_manager': self.ab_test_manager is not None,
                    'performance_monitor': self.performance_monitor is not None
                },
                'features': {
                    'competitive_analysis': self.config.enable_competitive_analysis,
                    'trend_prediction': self.config.enable_trend_prediction,
                    'ab_testing': self.config.enable_ab_testing,
                    'vector_storage': self.config.enable_vector_storage,
                    'ml_optimization': self.config.enable_ml_optimization
                },
                'configuration': {
                    'vector_db_provider': self.config.vector_db.provider,
                    'openai_model': self.config.openai.model_primary,
                    'monitoring_enabled': self.config.performance.enable_metrics_collection
                }
            }
            
            # Add performance summary if available
            if self.performance_monitor:
                status['performance'] = self.performance_monitor.get_performance_summary()
            
            return status
            
        except Exception as e:
            self.logger.error(f"Failed to get system status: {e}")
            return {'error': str(e)}
    
    async def health_check(self) -> Dict[str, Any]:
        """Perform comprehensive health check"""
        try:
            health = {
                'status': 'healthy',
                'timestamp': asyncio.get_event_loop().time(),
                'components': {}
            }
            
            # Check AI Engine
            if self.ai_engine:
                try:
                    engine_status = await self.ai_engine.get_engine_status()
                    health['components']['ai_engine'] = {
                        'status': 'healthy',
                        'details': engine_status
                    }
                except Exception as e:
                    health['components']['ai_engine'] = {
                        'status': 'unhealthy',
                        'error': str(e)
                    }
                    health['status'] = 'degraded'
            
            # Check Vector Database
            if self.vector_db_manager:
                try:
                    db_stats = await self.vector_db_manager.get_database_stats()
                    health['components']['vector_database'] = {
                        'status': 'healthy',
                        'details': db_stats
                    }
                except Exception as e:
                    health['components']['vector_database'] = {
                        'status': 'unhealthy',
                        'error': str(e)
                    }
                    health['status'] = 'degraded'
            
            # Check other components...
            # (Similar health checks for other components)
            
            return health
            
        except Exception as e:
            return {
                'status': 'unhealthy',
                'error': str(e),
                'timestamp': asyncio.get_event_loop().time()
            }


def setup_logging(log_level: str = "INFO", log_file: str = None):
    """Setup logging configuration"""
    logging.basicConfig(
        level=getattr(logging, log_level.upper()),
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.StreamHandler(sys.stdout),
            *([logging.FileHandler(log_file)] if log_file else [])
        ]
    )
    
    # Reduce noise from external libraries
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)
    logging.getLogger("asyncio").setLevel(logging.WARNING)


async def main():
    """Main entry point for standalone execution"""
    import argparse
    
    parser = argparse.ArgumentParser(description="AI Engine System")
    parser.add_argument("--config", help="Configuration file path")
    parser.add_argument("--log-level", default="INFO", help="Logging level")
    parser.add_argument("--log-file", help="Log file path")
    parser.add_argument("--test-mode", action="store_true", help="Run in test mode")
    
    args = parser.parse_args()
    
    # Setup logging
    setup_logging(args.log_level, args.log_file)
    logger = logging.getLogger("main")
    
    try:
        # Initialize AI Engine System
        ai_system = AIEngineSystem(args.config)
        
        # Start the system
        await ai_system.start()
        
        # Get system status
        status = ai_system.get_system_status()
        logger.info(f"System status: {status}")
        
        if args.test_mode:
            # Run tests and exit
            logger.info("Running in test mode...")
            
            # Test AI Engine
            if ai_system.ai_engine:
                from core.ai_engine import GenerationRequest
                
                test_request = GenerationRequest(
                    industry="SaaS",
                    target_audience="small businesses",
                    goal="lead_generation",
                    tone="professional",
                    length="medium",
                    special_requirements=[],
                    reference_competitors=[]
                )
                
                logger.info("Testing AI Engine...")
                result = await ai_system.ai_engine.generate_lp_content(test_request)
                logger.info(f"Generation test completed: quality_score={result.get('quality_score', 0)}")
            
            # Test Vector Database
            if ai_system.vector_db_manager:
                logger.info("Testing Vector Database...")
                test_pattern = {
                    'pattern_id': 'test_001',
                    'pattern_type': 'test',
                    'content': 'This is a test pattern',
                    'performance_metrics': {'conversion_rate': 0.15}
                }
                
                success = await ai_system.vector_db_manager.store_learning_pattern(test_pattern)
                logger.info(f"Vector DB test: {'PASS' if success else 'FAIL'}")
            
            logger.info("Test mode completed")
            
        else:
            # Run indefinitely
            logger.info("AI Engine System running. Press Ctrl+C to stop.")
            
            try:
                while True:
                    await asyncio.sleep(60)  # Sleep for 1 minute
                    
                    # Periodic health check
                    health = await ai_system.health_check()
                    if health['status'] != 'healthy':
                        logger.warning(f"System health: {health}")
                    
            except KeyboardInterrupt:
                logger.info("Shutdown requested...")
        
        # Stop the system
        await ai_system.stop()
        logger.info("AI Engine System shutdown complete")
        
    except Exception as e:
        logger.error(f"AI Engine System error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    asyncio.run(main())