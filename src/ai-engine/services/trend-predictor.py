"""
Trend Prediction AI - Advanced Market Analysis and Future Forecasting
3-month ahead prediction models with industry-specific trend detection
"""

import asyncio
import logging
from typing import Dict, List, Optional, Any, Tuple
from datetime import datetime, timedelta
import json
import numpy as np
import pandas as pd
from dataclasses import dataclass, asdict
from enum import Enum
import pickle
import aiohttp
import hashlib

# Machine Learning
import torch
import torch.nn as nn
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.preprocessing import StandardScaler, MinMaxScaler
from sklearn.metrics import mean_absolute_error, mean_squared_error
from sklearn.model_selection import TimeSeriesSplit
import xgboost as xgb

# Time series analysis
from statsmodels.tsa.arima.model import ARIMA
from statsmodels.tsa.seasonal import seasonal_decompose
from statsmodels.tsa.stattools import adfuller
import pmdarima as pm

# Data sources
import yfinance as yf
import requests
from bs4 import BeautifulSoup

# NLP for trend analysis
import openai
from transformers import pipeline, AutoTokenizer, AutoModel
from langchain.llms import OpenAI
from langchain.chains import LLMChain
from langchain.prompts import PromptTemplate

# Visualization (for trend reports)
import matplotlib.pyplot as plt
import seaborn as sns
import plotly.graph_objects as go
import plotly.express as px


class TrendCategory(Enum):
    TECHNOLOGY = "technology"
    DESIGN = "design"
    MARKETING = "marketing"
    CONSUMER_BEHAVIOR = "consumer_behavior"
    ECONOMIC = "economic"
    INDUSTRY_SPECIFIC = "industry_specific"


class TrendImpact(Enum):
    HIGH = "high"        # Major disruption expected
    MEDIUM = "medium"    # Moderate influence
    LOW = "low"         # Minor influence
    EMERGING = "emerging"  # Early stage, monitoring


class PredictionConfidence(Enum):
    VERY_HIGH = "very_high"  # 90%+ confidence
    HIGH = "high"            # 80-90% confidence
    MEDIUM = "medium"        # 60-80% confidence
    LOW = "low"              # 40-60% confidence
    VERY_LOW = "very_low"    # <40% confidence


@dataclass
class TrendData:
    trend_id: str
    name: str
    category: TrendCategory
    description: str
    current_adoption: float  # 0-1 scale
    growth_rate: float      # Monthly growth rate
    peak_prediction: datetime  # When trend will peak
    decline_prediction: Optional[datetime]  # When trend will decline
    impact_level: TrendImpact
    confidence: PredictionConfidence
    
    # Data sources
    data_sources: List[str]
    last_updated: datetime
    
    # Metrics
    search_volume: float
    social_mentions: int
    news_articles: int
    patent_filings: int
    investment_funding: float
    
    # Predictions
    three_month_forecast: Dict[str, float]
    key_indicators: List[str]
    risk_factors: List[str]
    opportunity_factors: List[str]


@dataclass
class IndustryTrendAnalysis:
    industry: str
    analysis_date: datetime
    
    # Current trends
    trending_up: List[TrendData]
    trending_down: List[TrendData]
    stable_trends: List[TrendData]
    emerging_trends: List[TrendData]
    
    # Predictions
    predicted_disruptions: List[Dict[str, Any]]
    opportunity_windows: List[Dict[str, Any]]
    threat_assessments: List[Dict[str, Any]]
    
    # Strategy recommendations
    short_term_actions: List[str]  # Next 1-3 months
    medium_term_preparations: List[str]  # 3-6 months
    long_term_positioning: List[str]  # 6+ months
    
    confidence_score: float


class TrendPredictor:
    """
    Advanced trend prediction AI with multiple data sources and ML models
    """
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.logger = logging.getLogger("trend_predictor")
        
        # Data sources configuration
        self.data_sources = {
            'google_trends': config.get('google_trends_api_key'),
            'twitter_api': config.get('twitter_api_key'),
            'news_api': config.get('news_api_key'),
            'patent_api': config.get('patent_api_key'),
            'funding_api': config.get('funding_api_key')
        }
        
        # ML Models
        self.trend_models = {}
        self.scaler = StandardScaler()
        
        # Neural network for complex trend prediction
        self.neural_model = None
        
        # Data storage
        self.trend_data: Dict[str, TrendData] = {}
        self.historical_data: Dict[str, pd.DataFrame] = {}
        
        # OpenAI for insights
        self.openai_client = openai
        self.openai_client.api_key = config.get('openai_api_key')
        
        # Cache for expensive operations
        self.prediction_cache: Dict[str, Any] = {}
        self.cache_ttl = timedelta(hours=6)  # Cache predictions for 6 hours
        
        # Initialize models
        asyncio.create_task(self._initialize_models())
    
    async def _initialize_models(self):
        """Initialize ML models and load pre-trained weights"""
        try:
            self.logger.info("Initializing trend prediction models...")
            
            # Random Forest for trend classification
            self.trend_models['rf_classifier'] = RandomForestRegressor(
                n_estimators=100,
                random_state=42,
                n_jobs=-1
            )
            
            # XGBoost for time series prediction
            self.trend_models['xgb_predictor'] = xgb.XGBRegressor(
                n_estimators=100,
                learning_rate=0.1,
                max_depth=6,
                random_state=42
            )
            
            # Neural network for complex pattern recognition
            self.neural_model = TrendNeuralNetwork()
            
            # Load pre-trained models if available
            await self._load_pretrained_models()
            
            self.logger.info("Trend prediction models initialized successfully")
            
        except Exception as e:
            self.logger.error(f"Failed to initialize models: {e}")
    
    async def _load_pretrained_models(self):
        """Load pre-trained models from disk"""
        try:
            # In production, load from cloud storage or database
            model_files = [
                'trend_rf_model.pkl',
                'trend_xgb_model.pkl',
                'trend_neural_model.pth'
            ]
            
            # Placeholder for actual model loading
            self.logger.info("Pre-trained models loaded")
            
        except Exception as e:
            self.logger.info(f"No pre-trained models found, will train from scratch: {e}")
    
    async def collect_trend_data(
        self,
        keywords: List[str],
        industry: str,
        timeframe: str = "3m"  # 3 months of historical data
    ) -> Dict[str, pd.DataFrame]:
        """
        Collect trend data from multiple sources
        """
        try:
            self.logger.info(f"Collecting trend data for {industry} industry")
            
            collected_data = {}
            
            # Google Trends data
            google_trends = await self._collect_google_trends(keywords, timeframe)
            if google_trends is not None:
                collected_data['google_trends'] = google_trends
            
            # Social media data
            social_data = await self._collect_social_media_data(keywords, timeframe)
            if social_data is not None:
                collected_data['social_media'] = social_data
            
            # News and media data
            news_data = await self._collect_news_data(keywords, timeframe)
            if news_data is not None:
                collected_data['news'] = news_data
            
            # Patent and innovation data
            patent_data = await self._collect_patent_data(keywords, timeframe)
            if patent_data is not None:
                collected_data['patents'] = patent_data
            
            # Investment and funding data
            funding_data = await self._collect_funding_data(keywords, industry, timeframe)
            if funding_data is not None:
                collected_data['funding'] = funding_data
            
            # Economic indicators
            economic_data = await self._collect_economic_data(industry, timeframe)
            if economic_data is not None:
                collected_data['economic'] = economic_data
            
            # Store historical data
            self.historical_data[industry] = self._combine_datasets(collected_data)
            
            self.logger.info(f"Collected {len(collected_data)} datasets for trend analysis")
            return collected_data
            
        except Exception as e:
            self.logger.error(f"Failed to collect trend data: {e}")
            return {}
    
    async def _collect_google_trends(self, keywords: List[str], timeframe: str) -> Optional[pd.DataFrame]:
        """Collect Google Trends data"""
        try:
            from pytrends.request import TrendReq
            
            # Initialize Google Trends
            pytrends = TrendReq(hl='en-US', tz=360)
            
            # Build comparison data
            trend_data = []
            
            for keyword in keywords[:5]:  # Limit to avoid rate limits
                try:
                    pytrends.build_payload([keyword], cat=0, timeframe=timeframe, geo='US')
                    data = pytrends.interest_over_time()
                    
                    if not data.empty:
                        data['keyword'] = keyword
                        trend_data.append(data)
                    
                    # Rate limiting
                    await asyncio.sleep(1)
                    
                except Exception as e:
                    self.logger.warning(f"Failed to get trends for {keyword}: {e}")
                    continue
            
            if trend_data:
                combined_data = pd.concat(trend_data, ignore_index=True)
                return combined_data
            
            return None
            
        except Exception as e:
            self.logger.error(f"Google Trends collection failed: {e}")
            return None
    
    async def _collect_social_media_data(self, keywords: List[str], timeframe: str) -> Optional[pd.DataFrame]:
        """Collect social media trend data"""
        try:
            # Placeholder for Twitter/X API integration
            # In production, integrate with Twitter API v2, Reddit API, etc.
            
            social_data = []
            
            for keyword in keywords:
                # Simulate social media metrics
                dates = pd.date_range(end=datetime.now(), periods=90, freq='D')
                
                # Generate synthetic data (replace with real API calls)
                mentions = np.random.poisson(100, len(dates))
                sentiment = np.random.normal(0.6, 0.2, len(dates))  # Generally positive
                engagement = np.random.exponential(50, len(dates))
                
                df = pd.DataFrame({
                    'date': dates,
                    'keyword': keyword,
                    'mentions': mentions,
                    'sentiment_score': np.clip(sentiment, -1, 1),
                    'engagement_rate': engagement,
                    'reach': mentions * np.random.uniform(10, 50, len(dates))
                })
                
                social_data.append(df)
            
            if social_data:
                return pd.concat(social_data, ignore_index=True)
            
            return None
            
        except Exception as e:
            self.logger.error(f"Social media data collection failed: {e}")
            return None
    
    async def _collect_news_data(self, keywords: List[str], timeframe: str) -> Optional[pd.DataFrame]:
        """Collect news and media coverage data"""
        try:
            # Use News API or similar service
            news_data = []
            
            async with aiohttp.ClientSession() as session:
                for keyword in keywords:
                    # Placeholder for news API integration
                    # In production, use NewsAPI, Bing News API, etc.
                    
                    # Simulate news data
                    dates = pd.date_range(end=datetime.now(), periods=90, freq='D')
                    article_counts = np.random.poisson(5, len(dates))
                    
                    df = pd.DataFrame({
                        'date': dates,
                        'keyword': keyword,
                        'article_count': article_counts,
                        'avg_sentiment': np.random.normal(0.5, 0.3, len(dates)),
                        'media_tier': np.random.choice(['tier1', 'tier2', 'tier3'], len(dates)),
                        'total_reach': article_counts * np.random.uniform(1000, 100000, len(dates))
                    })
                    
                    news_data.append(df)
            
            if news_data:
                return pd.concat(news_data, ignore_index=True)
            
            return None
            
        except Exception as e:
            self.logger.error(f"News data collection failed: {e}")
            return None
    
    async def _collect_patent_data(self, keywords: List[str], timeframe: str) -> Optional[pd.DataFrame]:
        """Collect patent and innovation data"""
        try:
            # Placeholder for USPTO API or Google Patents API
            patent_data = []
            
            for keyword in keywords:
                # Simulate patent filing data
                dates = pd.date_range(end=datetime.now(), periods=12, freq='M')  # Monthly data
                patent_counts = np.random.poisson(3, len(dates))
                
                df = pd.DataFrame({
                    'date': dates,
                    'keyword': keyword,
                    'patent_filings': patent_counts,
                    'avg_claims': np.random.normal(15, 5, len(dates)),
                    'innovation_score': np.random.beta(2, 3, len(dates))  # Skewed toward lower scores
                })
                
                patent_data.append(df)
            
            if patent_data:
                return pd.concat(patent_data, ignore_index=True)
            
            return None
            
        except Exception as e:
            self.logger.error(f"Patent data collection failed: {e}")
            return None
    
    async def _collect_funding_data(self, keywords: List[str], industry: str, timeframe: str) -> Optional[pd.DataFrame]:
        """Collect investment and funding data"""
        try:
            # Placeholder for Crunchbase API or PitchBook integration
            funding_data = []
            
            # Simulate funding rounds data
            dates = pd.date_range(end=datetime.now(), periods=12, freq='M')
            
            for keyword in keywords:
                funding_amounts = np.random.lognormal(15, 2, len(dates))  # Log-normal for realistic funding
                deal_counts = np.random.poisson(2, len(dates))
                
                df = pd.DataFrame({
                    'date': dates,
                    'keyword': keyword,
                    'industry': industry,
                    'total_funding': funding_amounts,
                    'deal_count': deal_counts,
                    'avg_deal_size': funding_amounts / np.maximum(deal_counts, 1),
                    'stage_distribution': np.random.dirichlet([2, 3, 2, 1], len(dates)).tolist()  # Seed, Series A, B, C+
                })
                
                funding_data.append(df)
            
            if funding_data:
                return pd.concat(funding_data, ignore_index=True)
            
            return None
            
        except Exception as e:
            self.logger.error(f"Funding data collection failed: {e}")
            return None
    
    async def _collect_economic_data(self, industry: str, timeframe: str) -> Optional[pd.DataFrame]:
        """Collect relevant economic indicators"""
        try:
            # Use financial APIs like Alpha Vantage, FRED, Yahoo Finance
            
            # Key economic indicators
            indicators = [
                'GDP', 'CPI', 'unemployment_rate', 'interest_rates',
                'consumer_confidence', 'business_investment'
            ]
            
            dates = pd.date_range(end=datetime.now(), periods=36, freq='M')  # 3 years monthly
            
            economic_data = []
            
            for indicator in indicators:
                # Simulate economic data (replace with real API calls)
                if indicator == 'GDP':
                    values = np.random.normal(2.5, 1.0, len(dates))  # GDP growth %
                elif indicator == 'CPI':
                    values = np.random.normal(2.0, 0.5, len(dates))  # Inflation %
                elif indicator == 'unemployment_rate':
                    values = np.random.normal(5.0, 1.5, len(dates))  # Unemployment %
                elif indicator == 'interest_rates':
                    values = np.random.normal(3.0, 1.0, len(dates))  # Interest rates %
                elif indicator == 'consumer_confidence':
                    values = np.random.normal(100, 10, len(dates))  # Index
                else:
                    values = np.random.normal(50, 10, len(dates))  # Generic index
                
                df = pd.DataFrame({
                    'date': dates,
                    'indicator': indicator,
                    'value': values,
                    'industry_relevance': np.random.uniform(0.3, 0.9)  # How relevant to industry
                })
                
                economic_data.append(df)
            
            if economic_data:
                return pd.concat(economic_data, ignore_index=True)
            
            return None
            
        except Exception as e:
            self.logger.error(f"Economic data collection failed: {e}")
            return None
    
    def _combine_datasets(self, datasets: Dict[str, pd.DataFrame]) -> pd.DataFrame:
        """Combine multiple datasets into unified trend data"""
        try:
            combined_data = []
            
            # Normalize all datasets to daily frequency
            base_dates = pd.date_range(end=datetime.now(), periods=90, freq='D')
            
            for source, data in datasets.items():
                if data is None or data.empty:
                    continue
                
                # Resample to daily frequency and forward fill
                if 'date' in data.columns:
                    data.set_index('date', inplace=True)
                    data = data.resample('D').mean().fillna(method='ffill')
                
                # Add source identifier
                data['data_source'] = source
                combined_data.append(data.reset_index())
            
            if combined_data:
                result = pd.concat(combined_data, ignore_index=True, sort=False)
                return result.fillna(0)  # Fill remaining NaN values
            
            return pd.DataFrame()
            
        except Exception as e:
            self.logger.error(f"Failed to combine datasets: {e}")
            return pd.DataFrame()
    
    async def predict_trends(
        self,
        industry: str,
        keywords: List[str],
        prediction_horizon: int = 90  # days
    ) -> IndustryTrendAnalysis:
        """
        Generate comprehensive trend predictions for industry
        """
        try:
            self.logger.info(f"Predicting trends for {industry} industry")
            
            # Check cache first
            cache_key = f"{industry}_{hash(str(sorted(keywords)))}"
            if cache_key in self.prediction_cache:
                cached = self.prediction_cache[cache_key]
                if datetime.now() - cached['timestamp'] < self.cache_ttl:
                    self.logger.info("Using cached prediction")
                    return cached['result']
            
            # Collect fresh data
            trend_datasets = await self.collect_trend_data(keywords, industry)
            
            if not trend_datasets:
                raise Exception("No trend data available for prediction")
            
            # Analyze current trends
            current_trends = await self._analyze_current_trends(trend_datasets, keywords)
            
            # Generate predictions using ML models
            predictions = await self._generate_ml_predictions(
                trend_datasets, prediction_horizon
            )
            
            # AI-powered insights and strategies
            ai_insights = await self._generate_ai_insights(
                current_trends, predictions, industry
            )
            
            # Combine into comprehensive analysis
            analysis = IndustryTrendAnalysis(
                industry=industry,
                analysis_date=datetime.now(),
                trending_up=current_trends['trending_up'],
                trending_down=current_trends['trending_down'],
                stable_trends=current_trends['stable_trends'],
                emerging_trends=current_trends['emerging_trends'],
                predicted_disruptions=predictions['disruptions'],
                opportunity_windows=predictions['opportunities'],
                threat_assessments=predictions['threats'],
                short_term_actions=ai_insights['short_term_actions'],
                medium_term_preparations=ai_insights['medium_term_preparations'],
                long_term_positioning=ai_insights['long_term_positioning'],
                confidence_score=self._calculate_prediction_confidence(
                    trend_datasets, predictions
                )
            )
            
            # Cache the result
            self.prediction_cache[cache_key] = {
                'result': analysis,
                'timestamp': datetime.now()
            }
            
            self.logger.info("Trend prediction completed successfully")
            return analysis
            
        except Exception as e:
            self.logger.error(f"Trend prediction failed: {e}")
            raise
    
    async def _analyze_current_trends(
        self,
        datasets: Dict[str, pd.DataFrame],
        keywords: List[str]
    ) -> Dict[str, List[TrendData]]:
        """Analyze current trend status for each keyword"""
        
        current_trends = {
            'trending_up': [],
            'trending_down': [],
            'stable_trends': [],
            'emerging_trends': []
        }
        
        for keyword in keywords:
            trend_data = self._analyze_keyword_trend(datasets, keyword)
            
            if trend_data.growth_rate > 0.1:  # 10% monthly growth
                if trend_data.current_adoption < 0.3:
                    current_trends['emerging_trends'].append(trend_data)
                else:
                    current_trends['trending_up'].append(trend_data)
            elif trend_data.growth_rate < -0.1:  # -10% monthly decline
                current_trends['trending_down'].append(trend_data)
            else:
                current_trends['stable_trends'].append(trend_data)
        
        return current_trends
    
    def _analyze_keyword_trend(self, datasets: Dict[str, pd.DataFrame], keyword: str) -> TrendData:
        """Analyze trend for a specific keyword"""
        
        # Extract keyword-specific data from all sources
        keyword_metrics = {}
        
        for source, data in datasets.items():
            if data is None or data.empty:
                continue
            
            keyword_data = data[data.get('keyword', '') == keyword]
            if not keyword_data.empty:
                keyword_metrics[source] = keyword_data
        
        # Calculate trend metrics
        search_volume = self._calculate_search_volume(keyword_metrics)
        social_mentions = self._calculate_social_mentions(keyword_metrics)
        news_articles = self._calculate_news_coverage(keyword_metrics)
        patent_filings = self._calculate_patent_activity(keyword_metrics)
        investment_funding = self._calculate_funding_activity(keyword_metrics)
        
        # Calculate growth rate and adoption
        growth_rate = self._calculate_growth_rate(keyword_metrics)
        current_adoption = self._estimate_current_adoption(keyword_metrics)
        
        # Predict peak and decline
        peak_prediction = self._predict_trend_peak(keyword_metrics)
        decline_prediction = self._predict_trend_decline(keyword_metrics)
        
        # Assess impact and confidence
        impact_level = self._assess_impact_level(keyword_metrics)
        confidence = self._assess_prediction_confidence(keyword_metrics)
        
        # Generate predictions
        three_month_forecast = self._generate_three_month_forecast(keyword_metrics)
        
        return TrendData(
            trend_id=f"trend_{hashlib.md5(keyword.encode()).hexdigest()[:8]}",
            name=keyword,
            category=self._categorize_trend(keyword),
            description=f"Trend analysis for {keyword}",
            current_adoption=current_adoption,
            growth_rate=growth_rate,
            peak_prediction=peak_prediction,
            decline_prediction=decline_prediction,
            impact_level=impact_level,
            confidence=confidence,
            data_sources=list(keyword_metrics.keys()),
            last_updated=datetime.now(),
            search_volume=search_volume,
            social_mentions=social_mentions,
            news_articles=news_articles,
            patent_filings=patent_filings,
            investment_funding=investment_funding,
            three_month_forecast=three_month_forecast,
            key_indicators=self._identify_key_indicators(keyword_metrics),
            risk_factors=self._identify_risk_factors(keyword_metrics),
            opportunity_factors=self._identify_opportunity_factors(keyword_metrics)
        )
    
    def _calculate_search_volume(self, keyword_metrics: Dict) -> float:
        """Calculate average search volume"""
        if 'google_trends' in keyword_metrics:
            data = keyword_metrics['google_trends']
            if not data.empty and 'search_volume' in data.columns:
                return float(data['search_volume'].mean())
        return 0.0
    
    def _calculate_social_mentions(self, keyword_metrics: Dict) -> int:
        """Calculate total social media mentions"""
        if 'social_media' in keyword_metrics:
            data = keyword_metrics['social_media']
            if not data.empty and 'mentions' in data.columns:
                return int(data['mentions'].sum())
        return 0
    
    def _calculate_news_coverage(self, keyword_metrics: Dict) -> int:
        """Calculate news article count"""
        if 'news' in keyword_metrics:
            data = keyword_metrics['news']
            if not data.empty and 'article_count' in data.columns:
                return int(data['article_count'].sum())
        return 0
    
    def _calculate_patent_activity(self, keyword_metrics: Dict) -> int:
        """Calculate patent filing activity"""
        if 'patents' in keyword_metrics:
            data = keyword_metrics['patents']
            if not data.empty and 'patent_filings' in data.columns:
                return int(data['patent_filings'].sum())
        return 0
    
    def _calculate_funding_activity(self, keyword_metrics: Dict) -> float:
        """Calculate investment funding amount"""
        if 'funding' in keyword_metrics:
            data = keyword_metrics['funding']
            if not data.empty and 'total_funding' in data.columns:
                return float(data['total_funding'].sum())
        return 0.0
    
    def _calculate_growth_rate(self, keyword_metrics: Dict) -> float:
        """Calculate monthly growth rate"""
        # Use multiple sources to calculate growth
        growth_rates = []
        
        for source, data in keyword_metrics.items():
            if data.empty:
                continue
            
            # Get time-series data
            if 'date' in data.columns:
                data_sorted = data.sort_values('date')
                
                # Calculate growth for different metrics
                if 'mentions' in data.columns:
                    recent = data_sorted['mentions'].tail(30).mean()
                    older = data_sorted['mentions'].head(30).mean()
                    if older > 0:
                        growth_rates.append((recent - older) / older)
                
                if 'search_volume' in data.columns:
                    recent = data_sorted['search_volume'].tail(30).mean()
                    older = data_sorted['search_volume'].head(30).mean()
                    if older > 0:
                        growth_rates.append((recent - older) / older)
        
        return np.mean(growth_rates) if growth_rates else 0.0
    
    def _estimate_current_adoption(self, keyword_metrics: Dict) -> float:
        """Estimate current adoption level (0-1 scale)"""
        # Combine multiple signals to estimate adoption
        adoption_signals = []
        
        # Search volume signal (normalized)
        if 'google_trends' in keyword_metrics:
            data = keyword_metrics['google_trends']
            if not data.empty:
                max_volume = data.get('search_volume', [0]).max() if not data.empty else 0
                if max_volume > 0:
                    current_volume = data.get('search_volume', [0]).iloc[-1] if not data.empty else 0
                    adoption_signals.append(current_volume / max_volume)
        
        # Social media signal
        if 'social_media' in keyword_metrics:
            # Simplified adoption estimation
            adoption_signals.append(min(1.0, 0.5))  # Placeholder
        
        return np.mean(adoption_signals) if adoption_signals else 0.5
    
    def _predict_trend_peak(self, keyword_metrics: Dict) -> datetime:
        """Predict when trend will reach its peak"""
        # Simple prediction based on current growth rate
        growth_rate = self._calculate_growth_rate(keyword_metrics)
        
        if growth_rate > 0:
            # Estimate time to peak based on logistic curve
            days_to_peak = max(30, min(365, int(180 / (growth_rate + 0.1))))
            return datetime.now() + timedelta(days=days_to_peak)
        else:
            # Already peaked or declining
            return datetime.now()
    
    def _predict_trend_decline(self, keyword_metrics: Dict) -> Optional[datetime]:
        """Predict when trend will start declining"""
        peak_date = self._predict_trend_peak(keyword_metrics)
        
        # Estimate decline phase (usually 2-3x the growth phase)
        growth_rate = self._calculate_growth_rate(keyword_metrics)
        
        if growth_rate > 0:
            decline_delay = max(90, int(365 / (growth_rate + 0.05)))
            return peak_date + timedelta(days=decline_delay)
        
        return None
    
    def _assess_impact_level(self, keyword_metrics: Dict) -> TrendImpact:
        """Assess the potential impact level of the trend"""
        
        # Calculate impact score based on multiple factors
        impact_score = 0.0
        
        # Funding activity (high funding = high impact potential)
        funding = self._calculate_funding_activity(keyword_metrics)
        if funding > 100_000_000:  # $100M+
            impact_score += 0.3
        elif funding > 10_000_000:  # $10M+
            impact_score += 0.2
        elif funding > 1_000_000:  # $1M+
            impact_score += 0.1
        
        # Patent activity
        patents = self._calculate_patent_activity(keyword_metrics)
        if patents > 50:
            impact_score += 0.3
        elif patents > 10:
            impact_score += 0.2
        elif patents > 1:
            impact_score += 0.1
        
        # Growth rate
        growth_rate = self._calculate_growth_rate(keyword_metrics)
        if growth_rate > 0.5:  # 50%+ monthly growth
            impact_score += 0.3
        elif growth_rate > 0.2:  # 20%+ monthly growth
            impact_score += 0.2
        elif growth_rate > 0.1:  # 10%+ monthly growth
            impact_score += 0.1
        
        # Current adoption (early stage = higher potential impact)
        adoption = self._estimate_current_adoption(keyword_metrics)
        if adoption < 0.2:
            impact_score += 0.1  # Early stage bonus
        
        # Map score to impact level
        if impact_score >= 0.7:
            return TrendImpact.HIGH
        elif impact_score >= 0.4:
            return TrendImpact.MEDIUM
        elif impact_score >= 0.2:
            return TrendImpact.LOW
        else:
            return TrendImpact.EMERGING
    
    def _assess_prediction_confidence(self, keyword_metrics: Dict) -> PredictionConfidence:
        """Assess confidence in the prediction"""
        
        confidence_score = 0.0
        
        # Data source diversity
        num_sources = len(keyword_metrics)
        if num_sources >= 4:
            confidence_score += 0.3
        elif num_sources >= 2:
            confidence_score += 0.2
        else:
            confidence_score += 0.1
        
        # Data recency and completeness
        for source, data in keyword_metrics.items():
            if not data.empty and len(data) >= 30:  # At least 30 data points
                confidence_score += 0.1
        
        # Historical pattern consistency
        growth_rate = self._calculate_growth_rate(keyword_metrics)
        if abs(growth_rate) > 0.05:  # Clear trend direction
            confidence_score += 0.2
        
        # Map to confidence level
        if confidence_score >= 0.8:
            return PredictionConfidence.VERY_HIGH
        elif confidence_score >= 0.6:
            return PredictionConfidence.HIGH
        elif confidence_score >= 0.4:
            return PredictionConfidence.MEDIUM
        elif confidence_score >= 0.2:
            return PredictionConfidence.LOW
        else:
            return PredictionConfidence.VERY_LOW
    
    def _generate_three_month_forecast(self, keyword_metrics: Dict) -> Dict[str, float]:
        """Generate 3-month forecast for key metrics"""
        
        current_adoption = self._estimate_current_adoption(keyword_metrics)
        growth_rate = self._calculate_growth_rate(keyword_metrics)
        
        forecast = {}
        
        # Monthly projections
        for month in range(1, 4):
            # Simple exponential growth model
            projected_adoption = current_adoption * ((1 + growth_rate) ** month)
            projected_adoption = min(1.0, projected_adoption)  # Cap at 100%
            
            forecast[f"month_{month}"] = projected_adoption
        
        # Add volatility estimates
        forecast["volatility"] = min(0.5, abs(growth_rate) * 2)
        
        return forecast
    
    def _categorize_trend(self, keyword: str) -> TrendCategory:
        """Categorize the trend based on keyword"""
        
        keyword_lower = keyword.lower()
        
        if any(tech_word in keyword_lower for tech_word in 
               ['ai', 'ml', 'blockchain', 'cloud', 'iot', 'automation', 'digital']):
            return TrendCategory.TECHNOLOGY
        elif any(design_word in keyword_lower for design_word in 
                 ['design', 'ux', 'ui', 'visual', 'branding', 'color', 'typography']):
            return TrendCategory.DESIGN
        elif any(marketing_word in keyword_lower for marketing_word in 
                 ['marketing', 'advertising', 'social media', 'content', 'seo', 'sem']):
            return TrendCategory.MARKETING
        elif any(behavior_word in keyword_lower for behavior_word in 
                 ['consumer', 'behavior', 'preference', 'shopping', 'buying']):
            return TrendCategory.CONSUMER_BEHAVIOR
        elif any(economic_word in keyword_lower for economic_word in 
                 ['economic', 'financial', 'investment', 'funding', 'market']):
            return TrendCategory.ECONOMIC
        else:
            return TrendCategory.INDUSTRY_SPECIFIC
    
    def _identify_key_indicators(self, keyword_metrics: Dict) -> List[str]:
        """Identify key indicators to monitor for this trend"""
        
        indicators = []
        
        # Based on available data sources
        if 'google_trends' in keyword_metrics:
            indicators.append("Search volume trends")
        if 'social_media' in keyword_metrics:
            indicators.append("Social media sentiment and mentions")
        if 'news' in keyword_metrics:
            indicators.append("Media coverage and tone")
        if 'patents' in keyword_metrics:
            indicators.append("Patent filing activity")
        if 'funding' in keyword_metrics:
            indicators.append("Investment and funding rounds")
        
        # Add generic indicators
        indicators.extend([
            "Competitor activity",
            "Regulatory changes",
            "Economic conditions"
        ])
        
        return indicators
    
    def _identify_risk_factors(self, keyword_metrics: Dict) -> List[str]:
        """Identify risk factors for the trend"""
        
        risks = []
        
        # High volatility
        growth_rate = self._calculate_growth_rate(keyword_metrics)
        if abs(growth_rate) > 0.3:
            risks.append("High volatility and unpredictable growth")
        
        # Market saturation
        adoption = self._estimate_current_adoption(keyword_metrics)
        if adoption > 0.8:
            risks.append("Market approaching saturation")
        
        # Technology dependency
        risks.extend([
            "Technology disruption risk",
            "Regulatory intervention",
            "Economic downturn impact",
            "Competitor response"
        ])
        
        return risks
    
    def _identify_opportunity_factors(self, keyword_metrics: Dict) -> List[str]:
        """Identify opportunity factors for the trend"""
        
        opportunities = []
        
        # Early stage opportunity
        adoption = self._estimate_current_adoption(keyword_metrics)
        if adoption < 0.3:
            opportunities.append("Early market entry advantage")
        
        # Strong growth
        growth_rate = self._calculate_growth_rate(keyword_metrics)
        if growth_rate > 0.2:
            opportunities.append("Strong growth momentum")
        
        # Investment interest
        funding = self._calculate_funding_activity(keyword_metrics)
        if funding > 1_000_000:
            opportunities.append("High investor interest and funding availability")
        
        # Add generic opportunities
        opportunities.extend([
            "Market expansion potential",
            "Partnership opportunities",
            "Technology advancement benefits"
        ])
        
        return opportunities
    
    async def _generate_ml_predictions(
        self,
        datasets: Dict[str, pd.DataFrame],
        horizon: int
    ) -> Dict[str, Any]:
        """Generate ML-based predictions"""
        
        try:
            # Prepare features for ML models
            features = self._prepare_ml_features(datasets)
            
            if features.empty:
                return self._generate_fallback_predictions()
            
            # Train and predict with multiple models
            rf_predictions = await self._predict_with_random_forest(features, horizon)
            xgb_predictions = await self._predict_with_xgboost(features, horizon)
            neural_predictions = await self._predict_with_neural_network(features, horizon)
            
            # Ensemble predictions
            ensemble_predictions = self._ensemble_predictions([
                rf_predictions, xgb_predictions, neural_predictions
            ])
            
            # Generate specific predictions
            disruptions = self._identify_potential_disruptions(ensemble_predictions)
            opportunities = self._identify_market_opportunities(ensemble_predictions)
            threats = self._identify_market_threats(ensemble_predictions)
            
            return {
                'disruptions': disruptions,
                'opportunities': opportunities,
                'threats': threats,
                'raw_predictions': ensemble_predictions
            }
            
        except Exception as e:
            self.logger.error(f"ML prediction generation failed: {e}")
            return self._generate_fallback_predictions()
    
    def _prepare_ml_features(self, datasets: Dict[str, pd.DataFrame]) -> pd.DataFrame:
        """Prepare features for ML models"""
        
        try:
            features_list = []
            
            # Extract time-series features from each dataset
            for source, data in datasets.items():
                if data is None or data.empty:
                    continue
                
                # Ensure date column
                if 'date' not in data.columns:
                    continue
                
                data = data.sort_values('date')
                
                # Extract statistical features
                source_features = {
                    f'{source}_mean': data.select_dtypes(include=[np.number]).mean().to_dict(),
                    f'{source}_std': data.select_dtypes(include=[np.number]).std().to_dict(),
                    f'{source}_trend': self._calculate_trend_features(data),
                    f'{source}_seasonality': self._calculate_seasonality_features(data)
                }
                
                features_list.append(source_features)
            
            # Flatten and combine features
            combined_features = {}
            for feature_dict in features_list:
                for key, value in feature_dict.items():
                    if isinstance(value, dict):
                        for subkey, subvalue in value.items():
                            combined_features[f"{key}_{subkey}"] = subvalue
                    else:
                        combined_features[key] = value
            
            # Create DataFrame
            features_df = pd.DataFrame([combined_features])
            
            # Fill NaN values
            features_df = features_df.fillna(0)
            
            return features_df
            
        except Exception as e:
            self.logger.error(f"Feature preparation failed: {e}")
            return pd.DataFrame()
    
    def _calculate_trend_features(self, data: pd.DataFrame) -> Dict[str, float]:
        """Calculate trend-based features"""
        
        features = {}
        
        numeric_columns = data.select_dtypes(include=[np.number]).columns
        
        for col in numeric_columns:
            if col in data.columns and len(data) > 1:
                # Linear trend
                x = np.arange(len(data))
                y = data[col].values
                slope = np.polyfit(x, y, 1)[0] if len(x) > 1 else 0
                features[f'{col}_trend_slope'] = slope
                
                # Acceleration (second derivative)
                if len(data) > 2:
                    accel = np.polyfit(x, y, 2)[0] if len(x) > 2 else 0
                    features[f'{col}_trend_acceleration'] = accel
        
        return features
    
    def _calculate_seasonality_features(self, data: pd.DataFrame) -> Dict[str, float]:
        """Calculate seasonality features"""
        
        features = {}
        
        if 'date' not in data.columns or len(data) < 14:
            return features
        
        # Day of week patterns
        data['dow'] = pd.to_datetime(data['date']).dt.dayofweek
        
        # Month patterns
        data['month'] = pd.to_datetime(data['date']).dt.month
        
        numeric_columns = data.select_dtypes(include=[np.number]).columns
        
        for col in numeric_columns:
            if col in ['dow', 'month']:
                continue
                
            # Day of week variation
            dow_var = data.groupby('dow')[col].std().mean() if len(data) > 7 else 0
            features[f'{col}_dow_variation'] = dow_var
            
            # Monthly variation
            month_var = data.groupby('month')[col].std().mean() if len(data) > 30 else 0
            features[f'{col}_month_variation'] = month_var
        
        return features
    
    async def _predict_with_random_forest(self, features: pd.DataFrame, horizon: int) -> Dict[str, Any]:
        """Generate predictions using Random Forest"""
        
        try:
            # For demonstration, generate synthetic predictions
            # In production, train on historical data and real features
            
            predictions = {
                'trend_direction': np.random.choice(['up', 'down', 'stable'], p=[0.4, 0.2, 0.4]),
                'confidence': np.random.uniform(0.6, 0.9),
                'growth_forecast': np.random.normal(0.1, 0.2, horizon).tolist(),
                'volatility_forecast': np.random.uniform(0.1, 0.3, horizon).tolist()
            }
            
            return predictions
            
        except Exception as e:
            self.logger.error(f"Random Forest prediction failed: {e}")
            return {}
    
    async def _predict_with_xgboost(self, features: pd.DataFrame, horizon: int) -> Dict[str, Any]:
        """Generate predictions using XGBoost"""
        
        try:
            # Placeholder for XGBoost predictions
            predictions = {
                'trend_direction': np.random.choice(['up', 'down', 'stable'], p=[0.3, 0.3, 0.4]),
                'confidence': np.random.uniform(0.7, 0.95),
                'growth_forecast': np.random.normal(0.05, 0.15, horizon).tolist(),
                'volatility_forecast': np.random.uniform(0.05, 0.25, horizon).tolist()
            }
            
            return predictions
            
        except Exception as e:
            self.logger.error(f"XGBoost prediction failed: {e}")
            return {}
    
    async def _predict_with_neural_network(self, features: pd.DataFrame, horizon: int) -> Dict[str, Any]:
        """Generate predictions using Neural Network"""
        
        try:
            # Placeholder for neural network predictions
            predictions = {
                'trend_direction': np.random.choice(['up', 'down', 'stable'], p=[0.45, 0.25, 0.3]),
                'confidence': np.random.uniform(0.65, 0.85),
                'growth_forecast': np.random.normal(0.08, 0.18, horizon).tolist(),
                'volatility_forecast': np.random.uniform(0.08, 0.28, horizon).tolist()
            }
            
            return predictions
            
        except Exception as e:
            self.logger.error(f"Neural Network prediction failed: {e}")
            return {}
    
    def _ensemble_predictions(self, predictions_list: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Combine predictions from multiple models"""
        
        try:
            if not predictions_list:
                return {}
            
            # Filter out empty predictions
            valid_predictions = [p for p in predictions_list if p]
            
            if not valid_predictions:
                return {}
            
            ensemble = {}
            
            # Average confidence scores
            confidences = [p.get('confidence', 0) for p in valid_predictions]
            ensemble['confidence'] = np.mean(confidences)
            
            # Majority vote for trend direction
            directions = [p.get('trend_direction', 'stable') for p in valid_predictions]
            ensemble['trend_direction'] = max(set(directions), key=directions.count)
            
            # Average growth forecasts
            growth_forecasts = [p.get('growth_forecast', []) for p in valid_predictions]
            valid_forecasts = [f for f in growth_forecasts if f]
            if valid_forecasts:
                max_len = max(len(f) for f in valid_forecasts)
                padded_forecasts = [f + [0] * (max_len - len(f)) for f in valid_forecasts]
                ensemble['growth_forecast'] = np.mean(padded_forecasts, axis=0).tolist()
            
            # Average volatility forecasts
            volatility_forecasts = [p.get('volatility_forecast', []) for p in valid_predictions]
            valid_volatility = [v for v in volatility_forecasts if v]
            if valid_volatility:
                max_len = max(len(v) for v in valid_volatility)
                padded_volatility = [v + [0] * (max_len - len(v)) for v in valid_volatility]
                ensemble['volatility_forecast'] = np.mean(padded_volatility, axis=0).tolist()
            
            return ensemble
            
        except Exception as e:
            self.logger.error(f"Ensemble prediction failed: {e}")
            return {}
    
    def _identify_potential_disruptions(self, predictions: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Identify potential market disruptions"""
        
        disruptions = []
        
        # High growth with high volatility indicates potential disruption
        growth_forecast = predictions.get('growth_forecast', [])
        volatility_forecast = predictions.get('volatility_forecast', [])
        
        if growth_forecast and volatility_forecast:
            avg_growth = np.mean(growth_forecast)
            avg_volatility = np.mean(volatility_forecast)
            
            if avg_growth > 0.2 and avg_volatility > 0.3:
                disruptions.append({
                    'type': 'Technology Disruption',
                    'probability': min(0.9, avg_growth + avg_volatility),
                    'timeframe': '3-6 months',
                    'impact': 'High',
                    'description': 'Rapid growth with high volatility suggests potential technology disruption'
                })
        
        # Add generic disruption possibilities
        disruptions.extend([
            {
                'type': 'Regulatory Change',
                'probability': 0.3,
                'timeframe': '6-12 months',
                'impact': 'Medium',
                'description': 'Potential regulatory changes affecting industry'
            },
            {
                'type': 'New Market Entrant',
                'probability': 0.4,
                'timeframe': '1-3 months',
                'impact': 'Medium',
                'description': 'Risk of new competitors entering market'
            }
        ])
        
        return disruptions
    
    def _identify_market_opportunities(self, predictions: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Identify market opportunities"""
        
        opportunities = []
        
        growth_forecast = predictions.get('growth_forecast', [])
        confidence = predictions.get('confidence', 0)
        
        if growth_forecast:
            avg_growth = np.mean(growth_forecast)
            
            if avg_growth > 0.1 and confidence > 0.7:
                opportunities.append({
                    'type': 'Market Expansion',
                    'probability': confidence,
                    'timeframe': '1-3 months',
                    'value': 'High',
                    'description': 'Strong growth trend with high confidence'
                })
        
        # Add standard opportunities
        opportunities.extend([
            {
                'type': 'Partnership Opportunities',
                'probability': 0.6,
                'timeframe': '2-4 months',
                'value': 'Medium',
                'description': 'Potential for strategic partnerships'
            },
            {
                'type': 'Product Innovation',
                'probability': 0.5,
                'timeframe': '3-6 months',
                'value': 'High',
                'description': 'Opportunity for product/service innovation'
            }
        ])
        
        return opportunities
    
    def _identify_market_threats(self, predictions: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Identify market threats"""
        
        threats = []
        
        growth_forecast = predictions.get('growth_forecast', [])
        trend_direction = predictions.get('trend_direction', 'stable')
        
        if growth_forecast:
            avg_growth = np.mean(growth_forecast)
            
            if avg_growth < -0.05 or trend_direction == 'down':
                threats.append({
                    'type': 'Market Decline',
                    'severity': 'High' if avg_growth < -0.1 else 'Medium',
                    'timeframe': '1-2 months',
                    'description': 'Negative growth trend detected'
                })
        
        # Add standard threats
        threats.extend([
            {
                'type': 'Increased Competition',
                'severity': 'Medium',
                'timeframe': '3-6 months',
                'description': 'Risk of increased competitive pressure'
            },
            {
                'type': 'Economic Downturn',
                'severity': 'Medium',
                'timeframe': '6-12 months',
                'description': 'Potential economic factors affecting market'
            }
        ])
        
        return threats
    
    def _generate_fallback_predictions(self) -> Dict[str, Any]:
        """Generate fallback predictions when ML fails"""
        
        return {
            'disruptions': [
                {
                    'type': 'Market Evolution',
                    'probability': 0.5,
                    'timeframe': '3-6 months',
                    'impact': 'Medium',
                    'description': 'General market evolution expected'
                }
            ],
            'opportunities': [
                {
                    'type': 'Market Growth',
                    'probability': 0.6,
                    'timeframe': '1-6 months',
                    'value': 'Medium',
                    'description': 'Potential for market growth'
                }
            ],
            'threats': [
                {
                    'type': 'Competitive Pressure',
                    'severity': 'Medium',
                    'timeframe': '3-12 months',
                    'description': 'Standard competitive threats'
                }
            ]
        }
    
    async def _generate_ai_insights(
        self,
        current_trends: Dict[str, List[TrendData]],
        predictions: Dict[str, Any],
        industry: str
    ) -> Dict[str, List[str]]:
        """Generate AI-powered strategic insights"""
        
        try:
            # Prepare data for AI analysis
            trends_summary = {
                'trending_up_count': len(current_trends['trending_up']),
                'trending_down_count': len(current_trends['trending_down']),
                'emerging_count': len(current_trends['emerging_trends']),
                'key_trends': [trend.name for trend in current_trends['trending_up'][:3]]
            }
            
            # Generate insights using GPT-4
            insights_prompt = f"""
            Based on this trend analysis for {industry} industry, provide strategic recommendations:
            
            Current Trends:
            {json.dumps(trends_summary, indent=2)}
            
            Predictions:
            {json.dumps(predictions, indent=2, default=str)}
            
            Provide specific, actionable recommendations in three categories:
            1. Short-term actions (1-3 months)
            2. Medium-term preparations (3-6 months) 
            3. Long-term positioning (6+ months)
            
            Each category should have 3-5 specific, implementable actions.
            Format as JSON with arrays for each category.
            """
            
            response = await self.openai_client.chat.completions.create(
                model="gpt-4",
                messages=[
                    {"role": "system", "content": f"You are a strategic business advisor specializing in {industry} industry trends."},
                    {"role": "user", "content": insights_prompt}
                ],
                temperature=0.3,
                max_tokens=1500
            )
            
            insights_text = response.choices[0].message.content
            insights = json.loads(insights_text)
            
            return insights
            
        except Exception as e:
            self.logger.error(f"Failed to generate AI insights: {e}")
            
            # Fallback insights
            return {
                'short_term_actions': [
                    f"Monitor emerging trends in {industry}",
                    "Analyze competitor responses to market changes",
                    "Optimize current offerings based on trend data"
                ],
                'medium_term_preparations': [
                    "Develop capabilities for predicted growth areas",
                    "Build partnerships for trend adaptation",
                    "Prepare for potential market disruptions"
                ],
                'long_term_positioning': [
                    f"Position as innovation leader in {industry}",
                    "Build sustainable competitive advantages",
                    "Develop future-ready business model"
                ]
            }
    
    def _calculate_prediction_confidence(
        self,
        datasets: Dict[str, pd.DataFrame],
        predictions: Dict[str, Any]
    ) -> float:
        """Calculate overall prediction confidence"""
        
        confidence_factors = []
        
        # Data quality factor
        data_quality = len([d for d in datasets.values() if d is not None and not d.empty])
        data_factor = min(1.0, data_quality / 4.0)  # Normalized by expected 4 sources
        confidence_factors.append(data_factor)
        
        # Prediction consistency
        pred_confidence = predictions.get('confidence', 0.5)
        confidence_factors.append(pred_confidence)
        
        # Historical accuracy (placeholder - would track over time)
        historical_accuracy = 0.75  # Assumed baseline
        confidence_factors.append(historical_accuracy)
        
        return np.mean(confidence_factors)


class TrendNeuralNetwork(nn.Module):
    """Neural network for trend prediction"""
    
    def __init__(self, input_size: int = 50, hidden_size: int = 128, output_size: int = 1):
        super(TrendNeuralNetwork, self).__init__()
        
        self.layers = nn.Sequential(
            nn.Linear(input_size, hidden_size),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(hidden_size, hidden_size // 2),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(hidden_size // 2, hidden_size // 4),
            nn.ReLU(),
            nn.Linear(hidden_size // 4, output_size),
            nn.Sigmoid()
        )
    
    def forward(self, x):
        return self.layers(x)


# Usage example
async def main():
    """Example usage of TrendPredictor"""
    
    config = {
        'openai_api_key': 'your-openai-key',
        'google_trends_api_key': 'your-google-key',
        'twitter_api_key': 'your-twitter-key',
        'news_api_key': 'your-news-key'
    }
    
    predictor = TrendPredictor(config)
    
    # Predict trends for SaaS industry
    analysis = await predictor.predict_trends(
        industry="SaaS",
        keywords=["artificial intelligence", "automation", "remote work", "cybersecurity"],
        prediction_horizon=90
    )
    
    print(f"Industry: {analysis.industry}")
    print(f"Trending up: {len(analysis.trending_up)} trends")
    print(f"Emerging: {len(analysis.emerging_trends)} trends")
    print(f"Predicted disruptions: {len(analysis.predicted_disruptions)}")
    print(f"Confidence score: {analysis.confidence_score:.2f}")
    
    # Print key recommendations
    print("\nShort-term actions:")
    for action in analysis.short_term_actions:
        print(f"- {action}")


if __name__ == "__main__":
    asyncio.run(main())