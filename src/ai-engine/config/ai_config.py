"""
AI Engine Configuration Management
Centralized configuration for all AI services and components
"""

import os
import json
import logging
from typing import Dict, Any, Optional
from dataclasses import dataclass, asdict
from datetime import datetime, timedelta
from pathlib import Path

# Environment management
from dotenv import load_dotenv
load_dotenv()


@dataclass
class OpenAIConfig:
    """OpenAI API configuration"""
    api_key: str
    model_primary: str = "gpt-4"
    model_secondary: str = "gpt-3.5-turbo"
    embedding_model: str = "text-embedding-ada-002"
    max_tokens: int = 2000
    temperature: float = 0.7
    timeout: int = 30
    rate_limit_per_minute: int = 60
    

@dataclass  
class VectorDatabaseConfig:
    """Vector database configuration"""
    provider: str = "faiss"  # faiss, pinecone, qdrant, weaviate
    dimension: int = 1536
    
    # Pinecone specific
    pinecone_api_key: Optional[str] = None
    pinecone_environment: Optional[str] = None
    pinecone_index_name: str = "ai-patterns"
    
    # Qdrant specific
    qdrant_host: str = "localhost"
    qdrant_port: int = 6333
    qdrant_api_key: Optional[str] = None
    
    # FAISS specific (local)
    faiss_index_path: str = "./data/faiss_indexes"
    faiss_metadata_path: str = "./data/faiss_metadata"


@dataclass
class CompetitiveAnalysisConfig:
    """Competitive analysis configuration"""
    max_competitors: int = 10
    default_analysis_depth: str = "standard"  # quick, standard, deep
    rate_limit_delay: float = 2.0  # seconds between requests
    
    # Web scraping
    user_agent: str = "Mozilla/5.0 (compatible; AI-Engine/2.0)"
    timeout: int = 30
    max_retries: int = 3
    
    # Data sources
    google_api_key: Optional[str] = None
    google_cse_id: Optional[str] = None
    
    # Analysis thresholds
    min_content_length: int = 100
    max_pages_per_site: int = 5


@dataclass
class TrendPredictionConfig:
    """Trend prediction configuration"""
    default_horizon_days: int = 90
    min_data_points: int = 30
    confidence_threshold: float = 0.7
    
    # Data sources
    google_trends_timeout: int = 60
    news_api_key: Optional[str] = None
    twitter_api_key: Optional[str] = None
    reddit_client_id: Optional[str] = None
    
    # ML model parameters
    rf_n_estimators: int = 100
    xgb_n_estimators: int = 100
    neural_hidden_size: int = 128
    
    # Cache settings
    cache_ttl_hours: int = 6
    max_cache_size: int = 1000


@dataclass
class ABTestingConfig:
    """A/B testing configuration"""
    default_significance_level: float = 0.05
    default_power: float = 0.8
    minimum_sample_size: int = 1000
    minimum_duration_days: int = 7
    maximum_duration_days: int = 30
    
    # Early stopping
    enable_early_stopping: bool = True
    early_stop_check_interval_hours: int = 24
    
    # Statistical methods
    use_sequential_testing: bool = True
    bayesian_threshold: float = 0.95


@dataclass
class PerformanceConfig:
    """Performance and monitoring configuration"""
    max_generation_time_seconds: int = 30
    target_accuracy_threshold: float = 0.8
    
    # Caching
    enable_redis_cache: bool = True
    redis_url: str = "redis://localhost:6379"
    cache_ttl_seconds: int = 3600
    
    # Rate limiting
    api_rate_limit_per_minute: int = 100
    burst_rate_limit: int = 10
    
    # Monitoring
    enable_metrics_collection: bool = True
    metrics_export_interval_seconds: int = 60
    
    # Resource limits
    max_memory_mb: int = 2048
    max_cpu_percent: float = 80.0


@dataclass
class SecurityConfig:
    """Security configuration"""
    api_key_required: bool = True
    api_key_env_var: str = "AI_ENGINE_API_KEY"
    
    # Input validation
    max_input_length: int = 10000
    allowed_file_types: list = None
    sanitize_inputs: bool = True
    
    # Output filtering
    filter_sensitive_data: bool = True
    content_moderation: bool = True
    
    def __post_init__(self):
        if self.allowed_file_types is None:
            self.allowed_file_types = ['.txt', '.json', '.csv']


@dataclass
class AIEngineConfig:
    """Main AI Engine configuration"""
    # Service configurations
    openai: OpenAIConfig
    vector_db: VectorDatabaseConfig
    competitive_analysis: CompetitiveAnalysisConfig
    trend_prediction: TrendPredictionConfig
    ab_testing: ABTestingConfig
    performance: PerformanceConfig
    security: SecurityConfig
    
    # Environment
    environment: str = "development"  # development, staging, production
    debug: bool = False
    log_level: str = "INFO"
    
    # Data storage
    data_directory: str = "./data"
    model_directory: str = "./models"
    logs_directory: str = "./logs"
    
    # Feature flags
    enable_competitive_analysis: bool = True
    enable_trend_prediction: bool = True
    enable_ab_testing: bool = True
    enable_vector_storage: bool = True
    enable_ml_optimization: bool = True


class ConfigManager:
    """Configuration manager with validation and environment overrides"""
    
    def __init__(self, config_file: Optional[str] = None):
        self.config_file = config_file
        self.logger = logging.getLogger("config_manager")
        
        # Create default config
        self._config = self._create_default_config()
        
        # Override with file config if provided
        if config_file and Path(config_file).exists():
            self._load_config_file(config_file)
        
        # Override with environment variables
        self._load_environment_overrides()
        
        # Validate configuration
        self._validate_config()
        
        # Create necessary directories
        self._create_directories()
    
    def _create_default_config(self) -> AIEngineConfig:
        """Create default configuration"""
        return AIEngineConfig(
            openai=OpenAIConfig(
                api_key=os.getenv("OPENAI_API_KEY", ""),
            ),
            vector_db=VectorDatabaseConfig(
                pinecone_api_key=os.getenv("PINECONE_API_KEY"),
                pinecone_environment=os.getenv("PINECONE_ENVIRONMENT"),
                qdrant_api_key=os.getenv("QDRANT_API_KEY")
            ),
            competitive_analysis=CompetitiveAnalysisConfig(
                google_api_key=os.getenv("GOOGLE_API_KEY"),
                google_cse_id=os.getenv("GOOGLE_CSE_ID")
            ),
            trend_prediction=TrendPredictionConfig(
                news_api_key=os.getenv("NEWS_API_KEY"),
                twitter_api_key=os.getenv("TWITTER_API_KEY"),
                reddit_client_id=os.getenv("REDDIT_CLIENT_ID")
            ),
            ab_testing=ABTestingConfig(),
            performance=PerformanceConfig(
                redis_url=os.getenv("REDIS_URL", "redis://localhost:6379")
            ),
            security=SecurityConfig()
        )
    
    def _load_config_file(self, config_file: str):
        """Load configuration from JSON file"""
        try:
            with open(config_file, 'r') as f:
                config_data = json.load(f)
            
            # Update configuration with file data
            self._update_config_from_dict(config_data)
            
            self.logger.info(f"Loaded configuration from {config_file}")
        except Exception as e:
            self.logger.error(f"Failed to load config file {config_file}: {e}")
    
    def _load_environment_overrides(self):
        """Load configuration overrides from environment variables"""
        
        # Environment detection
        env = os.getenv("AI_ENGINE_ENV", "development")
        self._config.environment = env
        self._config.debug = env == "development"
        
        # OpenAI overrides
        if os.getenv("OPENAI_MODEL_PRIMARY"):
            self._config.openai.model_primary = os.getenv("OPENAI_MODEL_PRIMARY")
        
        if os.getenv("OPENAI_MAX_TOKENS"):
            self._config.openai.max_tokens = int(os.getenv("OPENAI_MAX_TOKENS"))
        
        # Vector DB overrides
        if os.getenv("VECTOR_DB_PROVIDER"):
            self._config.vector_db.provider = os.getenv("VECTOR_DB_PROVIDER")
        
        # Performance overrides
        if os.getenv("MAX_GENERATION_TIME"):
            self._config.performance.max_generation_time_seconds = int(os.getenv("MAX_GENERATION_TIME"))
        
        # Feature flag overrides
        feature_flags = {
            "ENABLE_COMPETITIVE_ANALYSIS": "enable_competitive_analysis",
            "ENABLE_TREND_PREDICTION": "enable_trend_prediction", 
            "ENABLE_AB_TESTING": "enable_ab_testing",
            "ENABLE_VECTOR_STORAGE": "enable_vector_storage",
            "ENABLE_ML_OPTIMIZATION": "enable_ml_optimization"
        }
        
        for env_var, attr_name in feature_flags.items():
            env_value = os.getenv(env_var)
            if env_value is not None:
                setattr(self._config, attr_name, env_value.lower() in ('true', '1', 'yes'))
    
    def _update_config_from_dict(self, config_dict: Dict[str, Any]):
        """Update configuration from dictionary"""
        # This would be implemented to recursively update nested dataclass fields
        # For brevity, showing basic structure
        
        if 'openai' in config_dict:
            openai_config = config_dict['openai']
            for key, value in openai_config.items():
                if hasattr(self._config.openai, key):
                    setattr(self._config.openai, key, value)
        
        # Similar updates for other config sections...
    
    def _validate_config(self):
        """Validate configuration values"""
        errors = []
        
        # Validate OpenAI configuration
        if not self._config.openai.api_key:
            errors.append("OpenAI API key is required")
        
        if self._config.openai.temperature < 0 or self._config.openai.temperature > 1:
            errors.append("OpenAI temperature must be between 0 and 1")
        
        # Validate vector database configuration
        if self._config.vector_db.provider == "pinecone":
            if not self._config.vector_db.pinecone_api_key:
                errors.append("Pinecone API key is required when using Pinecone")
        
        # Validate performance configuration
        if self._config.performance.max_generation_time_seconds <= 0:
            errors.append("Max generation time must be positive")
        
        # Validate A/B testing configuration
        if not 0 < self._config.ab_testing.default_significance_level < 1:
            errors.append("A/B test significance level must be between 0 and 1")
        
        if errors:
            error_msg = "Configuration validation errors:\n" + "\n".join(f"- {error}" for error in errors)
            raise ValueError(error_msg)
    
    def _create_directories(self):
        """Create necessary directories"""
        directories = [
            self._config.data_directory,
            self._config.model_directory,
            self._config.logs_directory,
            self._config.vector_db.faiss_index_path,
            self._config.vector_db.faiss_metadata_path
        ]
        
        for directory in directories:
            Path(directory).mkdir(parents=True, exist_ok=True)
    
    @property
    def config(self) -> AIEngineConfig:
        """Get the current configuration"""
        return self._config
    
    def get_openai_config(self) -> Dict[str, Any]:
        """Get OpenAI configuration as dictionary"""
        return asdict(self._config.openai)
    
    def get_vector_db_config(self) -> Dict[str, Any]:
        """Get Vector DB configuration as dictionary"""
        return asdict(self._config.vector_db)
    
    def get_competitive_analysis_config(self) -> Dict[str, Any]:
        """Get competitive analysis configuration as dictionary"""
        return asdict(self._config.competitive_analysis)
    
    def get_trend_prediction_config(self) -> Dict[str, Any]:
        """Get trend prediction configuration as dictionary"""
        return asdict(self._config.trend_prediction)
    
    def get_ab_testing_config(self) -> Dict[str, Any]:
        """Get A/B testing configuration as dictionary"""
        return asdict(self._config.ab_testing)
    
    def get_performance_config(self) -> Dict[str, Any]:
        """Get performance configuration as dictionary"""
        return asdict(self._config.performance)
    
    def save_config(self, file_path: str):
        """Save current configuration to file"""
        try:
            config_dict = asdict(self._config)
            
            with open(file_path, 'w') as f:
                json.dump(config_dict, f, indent=2, default=str)
            
            self.logger.info(f"Configuration saved to {file_path}")
        except Exception as e:
            self.logger.error(f"Failed to save config to {file_path}: {e}")
    
    def update_config(self, updates: Dict[str, Any]):
        """Update configuration with new values"""
        try:
            self._update_config_from_dict(updates)
            self._validate_config()
            self.logger.info("Configuration updated successfully")
        except Exception as e:
            self.logger.error(f"Failed to update configuration: {e}")
            raise
    
    def is_feature_enabled(self, feature: str) -> bool:
        """Check if a feature is enabled"""
        feature_mapping = {
            'competitive_analysis': self._config.enable_competitive_analysis,
            'trend_prediction': self._config.enable_trend_prediction,
            'ab_testing': self._config.enable_ab_testing,
            'vector_storage': self._config.enable_vector_storage,
            'ml_optimization': self._config.enable_ml_optimization
        }
        
        return feature_mapping.get(feature, False)
    
    def get_environment_info(self) -> Dict[str, Any]:
        """Get environment information"""
        return {
            'environment': self._config.environment,
            'debug': self._config.debug,
            'log_level': self._config.log_level,
            'data_directory': self._config.data_directory,
            'model_directory': self._config.model_directory,
            'logs_directory': self._config.logs_directory
        }


# Global configuration manager instance
_config_manager: Optional[ConfigManager] = None


def get_config_manager(config_file: Optional[str] = None) -> ConfigManager:
    """Get or create the global configuration manager"""
    global _config_manager
    
    if _config_manager is None:
        _config_manager = ConfigManager(config_file)
    
    return _config_manager


def get_config() -> AIEngineConfig:
    """Get the current AI Engine configuration"""
    return get_config_manager().config


# Convenience functions for getting specific configurations
def get_openai_config() -> Dict[str, Any]:
    """Get OpenAI configuration"""
    return get_config_manager().get_openai_config()


def get_vector_db_config() -> Dict[str, Any]:
    """Get Vector DB configuration"""
    return get_config_manager().get_vector_db_config()


def get_competitive_analysis_config() -> Dict[str, Any]:
    """Get competitive analysis configuration"""
    return get_config_manager().get_competitive_analysis_config()


def get_trend_prediction_config() -> Dict[str, Any]:
    """Get trend prediction configuration"""
    return get_config_manager().get_trend_prediction_config()


def get_ab_testing_config() -> Dict[str, Any]:
    """Get A/B testing configuration"""
    return get_config_manager().get_ab_testing_config()


def get_performance_config() -> Dict[str, Any]:
    """Get performance configuration"""
    return get_config_manager().get_performance_config()


def is_feature_enabled(feature: str) -> bool:
    """Check if a feature is enabled"""
    return get_config_manager().is_feature_enabled(feature)


# Environment-specific configuration loading
def load_development_config():
    """Load development-specific configuration"""
    config = get_config_manager()
    
    # Development overrides
    updates = {
        'debug': True,
        'log_level': 'DEBUG',
        'openai': {
            'max_tokens': 1000,  # Reduced for faster testing
            'timeout': 15
        },
        'performance': {
            'max_generation_time_seconds': 15,
            'enable_metrics_collection': False
        }
    }
    
    config.update_config(updates)


def load_production_config():
    """Load production-specific configuration"""
    config = get_config_manager()
    
    # Production overrides
    updates = {
        'debug': False,
        'log_level': 'INFO',
        'security': {
            'api_key_required': True,
            'content_moderation': True,
            'filter_sensitive_data': True
        },
        'performance': {
            'enable_metrics_collection': True,
            'api_rate_limit_per_minute': 1000
        }
    }
    
    config.update_config(updates)


# Configuration validation utilities
def validate_api_keys():
    """Validate that required API keys are present"""
    config = get_config()
    missing_keys = []
    
    if not config.openai.api_key:
        missing_keys.append("OPENAI_API_KEY")
    
    if config.vector_db.provider == "pinecone" and not config.vector_db.pinecone_api_key:
        missing_keys.append("PINECONE_API_KEY")
    
    if missing_keys:
        raise ValueError(f"Missing required API keys: {', '.join(missing_keys)}")


def health_check_config() -> Dict[str, Any]:
    """Perform configuration health check"""
    config = get_config()
    
    return {
        'config_valid': True,
        'environment': config.environment,
        'debug_mode': config.debug,
        'features_enabled': {
            'competitive_analysis': config.enable_competitive_analysis,
            'trend_prediction': config.enable_trend_prediction,
            'ab_testing': config.enable_ab_testing,
            'vector_storage': config.enable_vector_storage,
            'ml_optimization': config.enable_ml_optimization
        },
        'services_configured': {
            'openai': bool(config.openai.api_key),
            'vector_db': config.vector_db.provider,
            'redis': bool(config.performance.redis_url)
        }
    }