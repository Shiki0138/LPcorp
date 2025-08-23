"""
Learning Optimizer - A/B Testing and Continuous Learning System
Real-time optimization with advanced ML-based performance analysis
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
import hashlib
import statistics

# Machine Learning
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score
from sklearn.preprocessing import StandardScaler, LabelEncoder
import xgboost as xgb

# Statistical analysis
from scipy import stats
from scipy.stats import chi2_contingency, mannwhitneyu, ttest_ind
import statsmodels.api as sm
from statsmodels.stats.power import ttest_power

# A/B Testing
from scipy.stats import beta

# Deep Learning
import torch
import torch.nn as nn
import torch.optim as optim


class TestType(Enum):
    AB_TEST = "ab_test"
    MULTIVARIATE = "multivariate"
    SEQUENTIAL = "sequential"
    ADAPTIVE = "adaptive"


class TestStatus(Enum):
    PLANNING = "planning"
    RUNNING = "running"
    COMPLETED = "completed"
    PAUSED = "paused"
    CANCELLED = "cancelled"


class SignificanceLevel(Enum):
    HIGH = 0.01      # 99% confidence
    MEDIUM = 0.05    # 95% confidence
    LOW = 0.1        # 90% confidence


@dataclass
class TestVariant:
    variant_id: str
    name: str
    description: str
    traffic_allocation: float  # 0.0 to 1.0
    
    # LP configuration
    content: Dict[str, Any]
    design: Dict[str, Any]
    
    # Performance metrics
    visitors: int = 0
    conversions: int = 0
    conversion_rate: float = 0.0
    
    # Advanced metrics
    bounce_rate: float = 0.0
    time_on_page: float = 0.0
    scroll_depth: float = 0.0
    engagement_score: float = 0.0
    
    # Revenue metrics
    revenue: float = 0.0
    revenue_per_visitor: float = 0.0
    
    created_at: datetime = None
    last_updated: datetime = None


@dataclass
class ABTest:
    test_id: str
    name: str
    description: str
    test_type: TestType
    status: TestStatus
    
    # Test configuration
    primary_metric: str
    secondary_metrics: List[str]
    significance_level: SignificanceLevel
    minimum_sample_size: int
    minimum_duration_days: int
    maximum_duration_days: int
    
    # Variants
    control_variant: TestVariant
    test_variants: List[TestVariant]
    
    # Results
    winner_variant_id: Optional[str] = None
    statistical_significance: bool = False
    confidence_interval: Tuple[float, float] = None
    p_value: float = None
    effect_size: float = None
    
    # Metadata
    created_by: str = None
    created_at: datetime = None
    started_at: Optional[datetime] = None
    ended_at: Optional[datetime] = None
    
    # Learning data
    insights: List[str] = None
    recommendations: List[str] = None


@dataclass
class LearningUpdate:
    update_id: str
    source_test_id: str
    pattern_type: str
    learned_insights: List[str]
    performance_improvement: float
    confidence_score: float
    applicable_contexts: List[str]
    created_at: datetime


class ABTestManager:
    """Advanced A/B testing and optimization system"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.logger = logging.getLogger("ab_test_manager")
        
        # Test storage
        self.active_tests: Dict[str, ABTest] = {}
        self.completed_tests: Dict[str, ABTest] = {}
        
        # ML models for optimization
        self.conversion_predictor = None
        self.engagement_predictor = None
        self.scaler = StandardScaler()
        
        # Statistical analysis
        self.significance_threshold = 0.05
        self.minimum_detectable_effect = 0.05  # 5% improvement
        
        # Learning database
        self.learned_patterns: List[LearningUpdate] = []
        self.pattern_performance: Dict[str, Dict] = {}
        
        # Performance tracking
        self.test_results_history: List[Dict] = []
        
        # Initialize ML models
        asyncio.create_task(self._initialize_ml_models())
    
    async def _initialize_ml_models(self):
        """Initialize machine learning models for optimization"""
        try:
            # Conversion rate predictor
            self.conversion_predictor = RandomForestClassifier(
                n_estimators=100,
                random_state=42,
                class_weight='balanced'
            )
            
            # Engagement predictor
            self.engagement_predictor = xgb.XGBRegressor(
                n_estimators=100,
                learning_rate=0.1,
                max_depth=6,
                random_state=42
            )
            
            # Load pre-trained models if available
            await self._load_pretrained_models()
            
            self.logger.info("ML models initialized successfully")
        except Exception as e:
            self.logger.error(f"Failed to initialize ML models: {e}")
    
    async def _load_pretrained_models(self):
        """Load pre-trained models from storage"""
        try:
            # Placeholder for model loading
            # In production, load from cloud storage or database
            pass
        except Exception as e:
            self.logger.info(f"No pre-trained models found: {e}")
    
    async def create_ab_test(
        self,
        name: str,
        description: str,
        control_variant: TestVariant,
        test_variants: List[TestVariant],
        primary_metric: str = "conversion_rate",
        significance_level: SignificanceLevel = SignificanceLevel.MEDIUM
    ) -> str:
        """Create a new A/B test"""
        try:
            # Generate test ID
            test_id = f"test_{hashlib.md5(f'{name}_{datetime.now()}'.encode()).hexdigest()[:8]}"
            
            # Calculate required sample size
            sample_size = self._calculate_sample_size(
                significance_level.value,
                0.8,  # 80% power
                self.minimum_detectable_effect
            )
            
            # Create test object
            ab_test = ABTest(
                test_id=test_id,
                name=name,
                description=description,
                test_type=TestType.AB_TEST,
                status=TestStatus.PLANNING,
                primary_metric=primary_metric,
                secondary_metrics=["bounce_rate", "time_on_page", "engagement_score"],
                significance_level=significance_level,
                minimum_sample_size=sample_size,
                minimum_duration_days=7,
                maximum_duration_days=30,
                control_variant=control_variant,
                test_variants=test_variants,
                created_at=datetime.now(),
                insights=[],
                recommendations=[]
            )
            
            # Store test
            self.active_tests[test_id] = ab_test
            
            self.logger.info(f"Created A/B test: {test_id} - {name}")
            return test_id
            
        except Exception as e:
            self.logger.error(f"Failed to create A/B test: {e}")
            raise
    
    def _calculate_sample_size(
        self,
        alpha: float,
        power: float,
        effect_size: float,
        baseline_rate: float = 0.1
    ) -> int:
        """Calculate required sample size for statistical significance"""
        try:
            # Use power analysis for sample size calculation
            sample_size = ttest_power(
                effect_size=effect_size,
                power=power,
                alpha=alpha,
                alternative='two-sided'
            )
            
            # Convert to actual visitor count based on expected conversion rate
            visitors_per_group = int(sample_size / baseline_rate)
            
            # Minimum sample size
            return max(visitors_per_group, 1000)
        except:
            # Fallback calculation
            return max(2000, int(16 * (1 / effect_size) ** 2))
    
    async def start_test(self, test_id: str) -> bool:
        """Start running an A/B test"""
        try:
            if test_id not in self.active_tests:
                raise ValueError(f"Test {test_id} not found")
            
            test = self.active_tests[test_id]
            
            if test.status != TestStatus.PLANNING:
                raise ValueError(f"Test {test_id} is not in planning state")
            
            # Validate test configuration
            if not self._validate_test_configuration(test):
                raise ValueError("Test configuration is invalid")
            
            # Start the test
            test.status = TestStatus.RUNNING
            test.started_at = datetime.now()
            
            self.logger.info(f"Started A/B test: {test_id}")
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to start test {test_id}: {e}")
            return False
    
    def _validate_test_configuration(self, test: ABTest) -> bool:
        """Validate A/B test configuration"""
        try:
            # Check traffic allocation
            total_allocation = test.control_variant.traffic_allocation
            for variant in test.test_variants:
                total_allocation += variant.traffic_allocation
            
            if abs(total_allocation - 1.0) > 0.01:  # Allow small floating point errors
                self.logger.error(f"Traffic allocation sum is {total_allocation}, should be 1.0")
                return False
            
            # Check minimum sample size
            if test.minimum_sample_size < 100:
                self.logger.error("Minimum sample size is too small")
                return False
            
            # Check test duration
            if test.minimum_duration_days < 1:
                self.logger.error("Minimum duration is too short")
                return False
            
            return True
        except Exception as e:
            self.logger.error(f"Test validation failed: {e}")
            return False
    
    async def record_visitor_interaction(
        self,
        test_id: str,
        variant_id: str,
        visitor_data: Dict[str, Any]
    ) -> bool:
        """Record a visitor interaction for A/B test"""
        try:
            if test_id not in self.active_tests:
                return False
            
            test = self.active_tests[test_id]
            
            if test.status != TestStatus.RUNNING:
                return False
            
            # Find variant
            variant = None
            if test.control_variant.variant_id == variant_id:
                variant = test.control_variant
            else:
                for v in test.test_variants:
                    if v.variant_id == variant_id:
                        variant = v
                        break
            
            if not variant:
                return False
            
            # Update visitor metrics
            variant.visitors += 1
            
            # Record conversion if applicable
            if visitor_data.get('converted', False):
                variant.conversions += 1
                variant.conversion_rate = variant.conversions / variant.visitors
            
            # Update engagement metrics
            if 'time_on_page' in visitor_data:
                variant.time_on_page = self._update_running_average(
                    variant.time_on_page,
                    visitor_data['time_on_page'],
                    variant.visitors
                )
            
            if 'bounce_rate' in visitor_data:
                variant.bounce_rate = self._update_running_average(
                    variant.bounce_rate,
                    1.0 if visitor_data['bounce_rate'] else 0.0,
                    variant.visitors
                )
            
            if 'scroll_depth' in visitor_data:
                variant.scroll_depth = self._update_running_average(
                    variant.scroll_depth,
                    visitor_data['scroll_depth'],
                    variant.visitors
                )
            
            # Calculate engagement score
            variant.engagement_score = self._calculate_engagement_score(variant)
            
            # Update revenue metrics
            if 'revenue' in visitor_data:
                variant.revenue += visitor_data['revenue']
                variant.revenue_per_visitor = variant.revenue / variant.visitors
            
            variant.last_updated = datetime.now()
            
            # Check if test should be stopped
            await self._check_early_stopping(test)
            
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to record visitor interaction: {e}")
            return False
    
    def _update_running_average(self, current_avg: float, new_value: float, count: int) -> float:
        """Update running average with new value"""
        if count <= 1:
            return new_value
        return ((current_avg * (count - 1)) + new_value) / count
    
    def _calculate_engagement_score(self, variant: TestVariant) -> float:
        """Calculate composite engagement score"""
        try:
            # Normalize metrics (0-1 scale)
            time_score = min(variant.time_on_page / 300.0, 1.0)  # Max 5 minutes
            bounce_score = 1.0 - variant.bounce_rate  # Invert bounce rate
            scroll_score = variant.scroll_depth
            
            # Weighted combination
            engagement_score = (
                time_score * 0.4 +
                bounce_score * 0.3 +
                scroll_score * 0.3
            )
            
            return engagement_score
        except:
            return 0.0
    
    async def _check_early_stopping(self, test: ABTest):
        """Check if test should be stopped early due to significance"""
        try:
            # Only check if minimum sample size is reached
            total_visitors = test.control_variant.visitors
            for variant in test.test_variants:
                total_visitors += variant.visitors
            
            if total_visitors < test.minimum_sample_size:
                return
            
            # Check if minimum duration has passed
            if test.started_at:
                days_running = (datetime.now() - test.started_at).days
                if days_running < test.minimum_duration_days:
                    return
            
            # Perform statistical analysis
            significance_result = await self._analyze_statistical_significance(test)
            
            if significance_result['significant']:
                # Stop test early due to significance
                await self._stop_test(test.test_id, "Statistical significance reached")
                
        except Exception as e:
            self.logger.error(f"Early stopping check failed: {e}")
    
    async def _analyze_statistical_significance(self, test: ABTest) -> Dict[str, Any]:
        """Analyze statistical significance of test results"""
        try:
            if len(test.test_variants) != 1:
                # For now, only handle simple A/B tests
                return {'significant': False, 'p_value': 1.0}
            
            # Get control and test data
            control = test.control_variant
            test_variant = test.test_variants[0]
            
            # Perform statistical test based on primary metric
            if test.primary_metric == "conversion_rate":
                result = self._chi_square_test(control, test_variant)
            else:
                result = self._t_test(control, test_variant, test.primary_metric)
            
            # Update test with results
            test.p_value = result['p_value']
            test.statistical_significance = result['significant']
            test.confidence_interval = result.get('confidence_interval')
            test.effect_size = result.get('effect_size', 0.0)
            
            # Determine winner
            if result['significant']:
                if test.primary_metric == "conversion_rate":
                    if test_variant.conversion_rate > control.conversion_rate:
                        test.winner_variant_id = test_variant.variant_id
                    else:
                        test.winner_variant_id = control.variant_id
                else:
                    # For other metrics, compare means
                    control_mean = getattr(control, test.primary_metric, 0)
                    test_mean = getattr(test_variant, test.primary_metric, 0)
                    
                    if test_mean > control_mean:
                        test.winner_variant_id = test_variant.variant_id
                    else:
                        test.winner_variant_id = control.variant_id
            
            return result
            
        except Exception as e:
            self.logger.error(f"Statistical analysis failed: {e}")
            return {'significant': False, 'p_value': 1.0}
    
    def _chi_square_test(self, control: TestVariant, test_variant: TestVariant) -> Dict[str, Any]:
        """Perform chi-square test for conversion rates"""
        try:
            # Create contingency table
            control_conversions = control.conversions
            control_non_conversions = control.visitors - control.conversions
            test_conversions = test_variant.conversions
            test_non_conversions = test_variant.visitors - test_variant.conversions
            
            contingency_table = [
                [control_conversions, control_non_conversions],
                [test_conversions, test_non_conversions]
            ]
            
            # Perform chi-square test
            chi2, p_value, dof, expected = chi2_contingency(contingency_table)
            
            # Calculate effect size (Cramer's V)
            n = control.visitors + test_variant.visitors
            effect_size = np.sqrt(chi2 / (n * (min(2, 2) - 1)))
            
            # Calculate confidence interval for difference in proportions
            p1 = control.conversion_rate
            p2 = test_variant.conversion_rate
            n1, n2 = control.visitors, test_variant.visitors
            
            diff = p2 - p1
            se = np.sqrt((p1 * (1 - p1) / n1) + (p2 * (1 - p2) / n2))
            
            # 95% confidence interval
            ci_lower = diff - 1.96 * se
            ci_upper = diff + 1.96 * se
            
            return {
                'significant': p_value < self.significance_threshold,
                'p_value': p_value,
                'effect_size': effect_size,
                'confidence_interval': (ci_lower, ci_upper),
                'test_statistic': chi2
            }
            
        except Exception as e:
            self.logger.error(f"Chi-square test failed: {e}")
            return {'significant': False, 'p_value': 1.0}
    
    def _t_test(self, control: TestVariant, test_variant: TestVariant, metric: str) -> Dict[str, Any]:
        """Perform t-test for continuous metrics"""
        try:
            # For simplicity, assume normal distribution
            # In practice, you'd collect individual data points
            
            control_mean = getattr(control, metric, 0)
            test_mean = getattr(test_variant, metric, 0)
            
            # Estimate standard deviation (placeholder)
            control_std = control_mean * 0.2  # Assume 20% CV
            test_std = test_mean * 0.2
            
            # Generate sample data (for demonstration)
            control_data = np.random.normal(control_mean, control_std, control.visitors)
            test_data = np.random.normal(test_mean, test_std, test_variant.visitors)
            
            # Perform t-test
            t_statistic, p_value = ttest_ind(control_data, test_data)
            
            # Calculate effect size (Cohen's d)
            pooled_std = np.sqrt(((len(control_data) - 1) * control_std**2 + 
                                 (len(test_data) - 1) * test_std**2) / 
                                (len(control_data) + len(test_data) - 2))
            
            effect_size = (test_mean - control_mean) / pooled_std
            
            # Confidence interval for difference in means
            se = pooled_std * np.sqrt(1/len(control_data) + 1/len(test_data))
            diff = test_mean - control_mean
            ci_lower = diff - 1.96 * se
            ci_upper = diff + 1.96 * se
            
            return {
                'significant': p_value < self.significance_threshold,
                'p_value': p_value,
                'effect_size': abs(effect_size),
                'confidence_interval': (ci_lower, ci_upper),
                'test_statistic': t_statistic
            }
            
        except Exception as e:
            self.logger.error(f"T-test failed: {e}")
            return {'significant': False, 'p_value': 1.0}
    
    async def _stop_test(self, test_id: str, reason: str = "Manual stop"):
        """Stop a running A/B test"""
        try:
            if test_id not in self.active_tests:
                return False
            
            test = self.active_tests[test_id]
            test.status = TestStatus.COMPLETED
            test.ended_at = datetime.now()
            
            # Generate insights and recommendations
            await self._generate_test_insights(test)
            
            # Learn from test results
            await self._learn_from_test(test)
            
            # Move to completed tests
            self.completed_tests[test_id] = test
            del self.active_tests[test_id]
            
            self.logger.info(f"Stopped test {test_id}: {reason}")
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to stop test {test_id}: {e}")
            return False
    
    async def _generate_test_insights(self, test: ABTest):
        """Generate insights from test results"""
        try:
            insights = []
            recommendations = []
            
            # Performance comparison
            control = test.control_variant
            
            for variant in test.test_variants:
                if test.primary_metric == "conversion_rate":
                    improvement = ((variant.conversion_rate - control.conversion_rate) / 
                                 control.conversion_rate) * 100
                    
                    if improvement > 0:
                        insights.append(
                            f"Variant {variant.name} improved conversion rate by {improvement:.1f}%"
                        )
                    else:
                        insights.append(
                            f"Variant {variant.name} decreased conversion rate by {abs(improvement):.1f}%"
                        )
                
                # Engagement insights
                if variant.engagement_score > control.engagement_score:
                    eng_improvement = ((variant.engagement_score - control.engagement_score) / 
                                     control.engagement_score) * 100
                    insights.append(
                        f"Variant {variant.name} improved engagement by {eng_improvement:.1f}%"
                    )
            
            # Statistical significance
            if test.statistical_significance:
                insights.append(
                    f"Results are statistically significant (p-value: {test.p_value:.4f})"
                )
            else:
                insights.append(
                    f"Results are not statistically significant (p-value: {test.p_value:.4f})"
                )
            
            # Generate recommendations
            if test.winner_variant_id:
                winner = None
                if test.control_variant.variant_id == test.winner_variant_id:
                    winner = test.control_variant
                else:
                    for v in test.test_variants:
                        if v.variant_id == test.winner_variant_id:
                            winner = v
                            break
                
                if winner:
                    recommendations.append(
                        f"Implement variant '{winner.name}' for improved performance"
                    )
                    
                    # Specific recommendations based on winner characteristics
                    await self._generate_specific_recommendations(winner, recommendations)
            
            test.insights = insights
            test.recommendations = recommendations
            
        except Exception as e:
            self.logger.error(f"Failed to generate insights: {e}")
    
    async def _generate_specific_recommendations(self, winner: TestVariant, recommendations: List[str]):
        """Generate specific recommendations based on winning variant"""
        try:
            # Analyze winning variant characteristics
            content = winner.content
            design = winner.design
            
            # Content recommendations
            if 'headline' in content:
                recommendations.append(
                    f"Use headline pattern: '{content['headline'][:50]}...' for similar campaigns"
                )
            
            if 'cta_text' in content:
                recommendations.append(
                    f"Apply CTA text: '{content['cta_text']}' to other landing pages"
                )
            
            # Design recommendations
            if 'color_scheme' in design:
                recommendations.append(
                    f"Consider color scheme '{design['color_scheme']}' for brand consistency"
                )
            
            if 'layout' in design:
                recommendations.append(
                    f"Implement '{design['layout']}' layout for similar pages"
                )
            
        except Exception as e:
            self.logger.error(f"Failed to generate specific recommendations: {e}")
    
    async def _learn_from_test(self, test: ABTest):
        """Extract learning patterns from completed test"""
        try:
            if not test.statistical_significance:
                return
            
            # Identify key differences between winner and control
            winner_variant = None
            if test.winner_variant_id:
                if test.control_variant.variant_id == test.winner_variant_id:
                    winner_variant = test.control_variant
                else:
                    for v in test.test_variants:
                        if v.variant_id == test.winner_variant_id:
                            winner_variant = v
                            break
            
            if not winner_variant:
                return
            
            # Calculate performance improvement
            control = test.control_variant
            if test.primary_metric == "conversion_rate":
                improvement = ((winner_variant.conversion_rate - control.conversion_rate) / 
                             control.conversion_rate)
            else:
                control_value = getattr(control, test.primary_metric, 0)
                winner_value = getattr(winner_variant, test.primary_metric, 0)
                if control_value > 0:
                    improvement = ((winner_value - control_value) / control_value)
                else:
                    improvement = 0.0
            
            # Extract learned insights
            learned_insights = []
            
            # Content patterns
            if winner_variant.content != control.content:
                content_diff = self._analyze_content_differences(
                    control.content, winner_variant.content
                )
                learned_insights.extend(content_diff)
            
            # Design patterns
            if winner_variant.design != control.design:
                design_diff = self._analyze_design_differences(
                    control.design, winner_variant.design
                )
                learned_insights.extend(design_diff)
            
            # Create learning update
            learning_update = LearningUpdate(
                update_id=f"learn_{test.test_id}",
                source_test_id=test.test_id,
                pattern_type="test_result",
                learned_insights=learned_insights,
                performance_improvement=improvement,
                confidence_score=1.0 - test.p_value,  # Higher confidence = lower p-value
                applicable_contexts=[],  # Would be determined based on test context
                created_at=datetime.now()
            )
            
            self.learned_patterns.append(learning_update)
            
            # Update ML models with new data
            await self._update_ml_models_with_test_data(test)
            
        except Exception as e:
            self.logger.error(f"Failed to learn from test: {e}")
    
    def _analyze_content_differences(self, control_content: Dict, winner_content: Dict) -> List[str]:
        """Analyze differences in content between control and winner"""
        differences = []
        
        for key in winner_content:
            if key not in control_content:
                differences.append(f"Added content element: {key}")
            elif winner_content[key] != control_content[key]:
                differences.append(f"Modified {key}: {control_content[key]} -> {winner_content[key]}")
        
        return differences
    
    def _analyze_design_differences(self, control_design: Dict, winner_design: Dict) -> List[str]:
        """Analyze differences in design between control and winner"""
        differences = []
        
        for key in winner_design:
            if key not in control_design:
                differences.append(f"Added design element: {key}")
            elif winner_design[key] != control_design[key]:
                differences.append(f"Modified {key}: {control_design[key]} -> {winner_design[key]}")
        
        return differences
    
    async def _update_ml_models_with_test_data(self, test: ABTest):
        """Update ML models with data from completed test"""
        try:
            # Prepare training data
            training_data = []
            
            # Add control variant data
            control_features = self._extract_features_from_variant(test.control_variant)
            control_features['conversion_rate'] = test.control_variant.conversion_rate
            training_data.append(control_features)
            
            # Add test variant data
            for variant in test.test_variants:
                variant_features = self._extract_features_from_variant(variant)
                variant_features['conversion_rate'] = variant.conversion_rate
                training_data.append(variant_features)
            
            # Convert to DataFrame for easier processing
            df = pd.DataFrame(training_data)
            
            # Update conversion predictor
            if len(df) >= 2:  # Minimum data for training
                await self._retrain_conversion_model(df)
            
        except Exception as e:
            self.logger.error(f"Failed to update ML models: {e}")
    
    def _extract_features_from_variant(self, variant: TestVariant) -> Dict[str, Any]:
        """Extract numerical features from variant for ML training"""
        features = {}
        
        # Content features (simplified)
        content = variant.content
        features['headline_length'] = len(content.get('headline', ''))
        features['cta_count'] = len(content.get('cta_elements', []))
        features['has_urgency'] = 1 if any(word in str(content).lower() 
                                         for word in ['urgent', 'limited', 'now', 'today']) else 0
        
        # Design features
        design = variant.design
        features['color_count'] = len(design.get('colors', []))
        features['is_mobile_optimized'] = 1 if design.get('mobile_responsive', False) else 0
        
        # Performance features
        features['visitors'] = variant.visitors
        features['bounce_rate'] = variant.bounce_rate
        features['time_on_page'] = variant.time_on_page
        features['scroll_depth'] = variant.scroll_depth
        features['engagement_score'] = variant.engagement_score
        
        return features
    
    async def _retrain_conversion_model(self, df: pd.DataFrame):
        """Retrain conversion prediction model"""
        try:
            # Prepare features and target
            feature_columns = [col for col in df.columns if col != 'conversion_rate']
            X = df[feature_columns]
            y = df['conversion_rate']
            
            # Scale features
            X_scaled = self.scaler.fit_transform(X)
            
            # Convert continuous target to binary for classification
            y_binary = (y > y.median()).astype(int)
            
            # Retrain model
            self.conversion_predictor.fit(X_scaled, y_binary)
            
            self.logger.info("Conversion prediction model retrained successfully")
            
        except Exception as e:
            self.logger.error(f"Model retraining failed: {e}")
    
    async def predict_variant_performance(self, variant: TestVariant) -> Dict[str, float]:
        """Predict performance metrics for a variant using ML models"""
        try:
            if self.conversion_predictor is None:
                return {'predicted_conversion_rate': 0.0, 'confidence': 0.0}
            
            # Extract features
            features = self._extract_features_from_variant(variant)
            feature_values = list(features.values())
            
            # Predict conversion probability
            X_scaled = self.scaler.transform([feature_values])
            conversion_prob = self.conversion_predictor.predict_proba(X_scaled)[0][1]
            
            # Get prediction confidence (simplified)
            confidence = self.conversion_predictor.predict_proba(X_scaled).max()
            
            return {
                'predicted_conversion_rate': conversion_prob,
                'confidence': confidence
            }
            
        except Exception as e:
            self.logger.error(f"Performance prediction failed: {e}")
            return {'predicted_conversion_rate': 0.0, 'confidence': 0.0}
    
    async def get_optimization_recommendations(
        self,
        current_variant: TestVariant,
        target_metric: str = "conversion_rate"
    ) -> List[str]:
        """Get ML-powered optimization recommendations"""
        try:
            recommendations = []
            
            # Analyze historical learning patterns
            relevant_patterns = [
                pattern for pattern in self.learned_patterns
                if pattern.performance_improvement > 0.05  # 5% improvement threshold
            ]
            
            # Extract common success patterns
            success_insights = []
            for pattern in relevant_patterns[-10:]:  # Last 10 successful tests
                success_insights.extend(pattern.learned_insights)
            
            # Count frequency of successful patterns
            pattern_frequency = {}
            for insight in success_insights:
                pattern_frequency[insight] = pattern_frequency.get(insight, 0) + 1
            
            # Generate recommendations based on frequent successful patterns
            top_patterns = sorted(pattern_frequency.items(), key=lambda x: x[1], reverse=True)
            
            for pattern, frequency in top_patterns[:5]:
                if frequency >= 2:  # Seen in at least 2 successful tests
                    recommendations.append(f"Consider: {pattern} (successful in {frequency} tests)")
            
            # Add general optimization recommendations
            if current_variant.bounce_rate > 0.7:
                recommendations.append("High bounce rate detected - consider improving page loading speed")
            
            if current_variant.scroll_depth < 0.3:
                recommendations.append("Low scroll depth - consider moving key content above the fold")
            
            if len(recommendations) == 0:
                recommendations.append("Continue testing with incremental changes to identify improvements")
            
            return recommendations
            
        except Exception as e:
            self.logger.error(f"Failed to generate recommendations: {e}")
            return ["Unable to generate recommendations at this time"]
    
    async def get_test_status(self, test_id: str) -> Optional[Dict[str, Any]]:
        """Get comprehensive status of an A/B test"""
        try:
            test = None
            if test_id in self.active_tests:
                test = self.active_tests[test_id]
            elif test_id in self.completed_tests:
                test = self.completed_tests[test_id]
            
            if not test:
                return None
            
            # Calculate progress
            total_visitors = test.control_variant.visitors
            for variant in test.test_variants:
                total_visitors += variant.visitors
            
            progress = min(total_visitors / test.minimum_sample_size, 1.0)
            
            # Time progress
            time_progress = 0.0
            if test.started_at:
                days_running = (datetime.now() - test.started_at).days
                time_progress = min(days_running / test.minimum_duration_days, 1.0)
            
            return {
                'test_id': test.test_id,
                'name': test.name,
                'status': test.status.value,
                'progress': {
                    'visitors': progress,
                    'time': time_progress,
                    'overall': max(progress, time_progress)
                },
                'results': {
                    'statistical_significance': test.statistical_significance,
                    'p_value': test.p_value,
                    'winner_variant_id': test.winner_variant_id,
                    'effect_size': test.effect_size
                },
                'variants': {
                    'control': {
                        'visitors': test.control_variant.visitors,
                        'conversions': test.control_variant.conversions,
                        'conversion_rate': test.control_variant.conversion_rate,
                        'engagement_score': test.control_variant.engagement_score
                    },
                    'test': [
                        {
                            'variant_id': v.variant_id,
                            'visitors': v.visitors,
                            'conversions': v.conversions,
                            'conversion_rate': v.conversion_rate,
                            'engagement_score': v.engagement_score
                        }
                        for v in test.test_variants
                    ]
                },
                'insights': test.insights or [],
                'recommendations': test.recommendations or []
            }
            
        except Exception as e:
            self.logger.error(f"Failed to get test status: {e}")
            return None
    
    async def get_learning_summary(self) -> Dict[str, Any]:
        """Get summary of all learning from A/B tests"""
        try:
            total_tests = len(self.completed_tests)
            successful_tests = len([
                test for test in self.completed_tests.values()
                if test.statistical_significance
            ])
            
            # Calculate average improvement
            improvements = []
            for pattern in self.learned_patterns:
                if pattern.performance_improvement > 0:
                    improvements.append(pattern.performance_improvement)
            
            avg_improvement = np.mean(improvements) if improvements else 0.0
            
            # Top insights
            all_insights = []
            for pattern in self.learned_patterns:
                all_insights.extend(pattern.learned_insights)
            
            insight_frequency = {}
            for insight in all_insights:
                insight_frequency[insight] = insight_frequency.get(insight, 0) + 1
            
            top_insights = sorted(insight_frequency.items(), key=lambda x: x[1], reverse=True)[:10]
            
            return {
                'total_tests_completed': total_tests,
                'successful_tests': successful_tests,
                'success_rate': successful_tests / total_tests if total_tests > 0 else 0.0,
                'average_improvement': avg_improvement,
                'total_patterns_learned': len(self.learned_patterns),
                'top_insights': [
                    {'insight': insight, 'frequency': freq}
                    for insight, freq in top_insights
                ],
                'model_performance': {
                    'conversion_predictor_trained': self.conversion_predictor is not None,
                    'total_training_data_points': len(self.test_results_history)
                }
            }
            
        except Exception as e:
            self.logger.error(f"Failed to generate learning summary: {e}")
            return {}


# Usage example
async def main():
    """Example usage of ABTestManager"""
    
    config = {
        'significance_threshold': 0.05,
        'minimum_detectable_effect': 0.05
    }
    
    # Initialize A/B test manager
    ab_manager = ABTestManager(config)
    
    # Create test variants
    control_variant = TestVariant(
        variant_id="control",
        name="Original",
        description="Current landing page",
        traffic_allocation=0.5,
        content={'headline': 'Original Headline', 'cta': 'Sign Up'},
        design={'color': 'blue', 'layout': 'standard'}
    )
    
    test_variant = TestVariant(
        variant_id="test_v1",
        name="New Design",
        description="New landing page with improved CTA",
        traffic_allocation=0.5,
        content={'headline': 'Improved Headline', 'cta': 'Get Started Now'},
        design={'color': 'green', 'layout': 'modern'}
    )
    
    # Create A/B test
    test_id = await ab_manager.create_ab_test(
        name="Homepage CTA Test",
        description="Testing new CTA button design",
        control_variant=control_variant,
        test_variants=[test_variant]
    )
    
    # Start test
    await ab_manager.start_test(test_id)
    
    # Simulate visitor interactions
    for i in range(100):
        variant_id = "control" if i % 2 == 0 else "test_v1"
        converted = np.random.random() < (0.12 if variant_id == "test_v1" else 0.10)
        
        await ab_manager.record_visitor_interaction(
            test_id=test_id,
            variant_id=variant_id,
            visitor_data={
                'converted': converted,
                'time_on_page': np.random.normal(120, 30),
                'bounce_rate': np.random.random() < 0.3,
                'scroll_depth': np.random.uniform(0.3, 1.0)
            }
        )
    
    # Get test status
    status = await ab_manager.get_test_status(test_id)
    print(f"Test Status: {status}")
    
    # Get optimization recommendations
    recommendations = await ab_manager.get_optimization_recommendations(control_variant)
    print(f"Recommendations: {recommendations}")


if __name__ == "__main__":
    asyncio.run(main())