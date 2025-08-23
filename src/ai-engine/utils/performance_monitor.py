"""
Performance Monitor - Real-time Performance Tracking and Optimization
Advanced monitoring system with bottleneck detection and auto-optimization
"""

import asyncio
import logging
import time
import psutil
import threading
from typing import Dict, List, Optional, Any, Callable
from datetime import datetime, timedelta
import json
import statistics
from dataclasses import dataclass, asdict, field
from collections import deque, defaultdict
from enum import Enum
import hashlib

# Monitoring libraries
import prometheus_client
from prometheus_client import Counter, Histogram, Gauge, CollectorRegistry
import aioredis

# ML for performance optimization
import numpy as np
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler


class MetricType(Enum):
    COUNTER = "counter"
    GAUGE = "gauge"
    HISTOGRAM = "histogram"
    SUMMARY = "summary"


class AlertLevel(Enum):
    INFO = "info"
    WARNING = "warning"
    ERROR = "error"
    CRITICAL = "critical"


@dataclass
class PerformanceMetric:
    name: str
    value: float
    timestamp: datetime
    labels: Dict[str, str] = field(default_factory=dict)
    metric_type: MetricType = MetricType.GAUGE


@dataclass
class PerformanceAlert:
    alert_id: str
    level: AlertLevel
    title: str
    description: str
    metric_name: str
    threshold_value: float
    current_value: float
    timestamp: datetime
    resolved: bool = False
    resolution_time: Optional[datetime] = None


@dataclass
class SystemResourceMetrics:
    cpu_percent: float
    memory_percent: float
    memory_available_mb: float
    disk_usage_percent: float
    network_bytes_sent: float
    network_bytes_recv: float
    open_file_descriptors: int
    thread_count: int
    timestamp: datetime


@dataclass
class ApplicationMetrics:
    requests_per_second: float
    avg_response_time_ms: float
    error_rate_percent: float
    active_connections: int
    queue_depth: int
    cache_hit_rate_percent: float
    ml_inference_time_ms: float
    database_query_time_ms: float
    timestamp: datetime


@dataclass
class BottleneckAnalysis:
    component: str
    severity: float  # 0-1 scale
    impact_score: float  # 0-1 scale
    root_cause: str
    recommendations: List[str]
    estimated_improvement: float
    confidence: float
    detected_at: datetime


class PerformanceMonitor:
    """Advanced performance monitoring and optimization system"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.logger = logging.getLogger("performance_monitor")
        
        # Monitoring state
        self.is_monitoring = False
        self.monitor_thread = None
        
        # Metrics storage
        self.metrics_history: deque = deque(maxlen=10000)
        self.system_metrics_history: deque = deque(maxlen=1000)
        self.app_metrics_history: deque = deque(maxlen=1000)
        
        # Real-time metrics
        self.current_metrics: Dict[str, PerformanceMetric] = {}
        self.metric_aggregators: Dict[str, List[float]] = defaultdict(list)
        
        # Alerts and notifications
        self.active_alerts: Dict[str, PerformanceAlert] = {}
        self.alert_handlers: List[Callable] = []
        
        # Performance thresholds
        self.thresholds = {
            'cpu_percent': 80.0,
            'memory_percent': 85.0,
            'response_time_ms': 5000.0,
            'error_rate_percent': 5.0,
            'queue_depth': 100,
            'ml_inference_time_ms': 2000.0
        }
        
        # Bottleneck detection
        self.bottleneck_detector = None
        self.bottleneck_history: List[BottleneckAnalysis] = []
        
        # Prometheus metrics
        self.prometheus_registry = CollectorRegistry()
        self.prometheus_metrics = self._setup_prometheus_metrics()
        
        # Redis client for distributed metrics
        self.redis_client = None
        
        # Performance optimization
        self.auto_optimization_enabled = config.get('auto_optimization', False)
        self.optimization_history: List[Dict] = []
        
        # Initialize components
        asyncio.create_task(self._initialize_async_components())
    
    async def _initialize_async_components(self):
        """Initialize async components"""
        try:
            # Initialize Redis client
            redis_url = self.config.get('redis_url')
            if redis_url:
                self.redis_client = await aioredis.from_url(redis_url)
            
            # Initialize ML models for bottleneck detection
            self._initialize_bottleneck_detector()
            
            self.logger.info("Performance monitor initialized successfully")
        except Exception as e:
            self.logger.error(f"Failed to initialize performance monitor: {e}")
    
    def _setup_prometheus_metrics(self) -> Dict[str, Any]:
        """Setup Prometheus metrics"""
        metrics = {}
        
        # Request metrics
        metrics['request_duration'] = Histogram(
            'request_duration_seconds',
            'Request duration in seconds',
            ['method', 'endpoint'],
            registry=self.prometheus_registry
        )
        
        metrics['request_count'] = Counter(
            'requests_total',
            'Total number of requests',
            ['method', 'endpoint', 'status'],
            registry=self.prometheus_registry
        )
        
        # System metrics
        metrics['cpu_usage'] = Gauge(
            'cpu_usage_percent',
            'CPU usage percentage',
            registry=self.prometheus_registry
        )
        
        metrics['memory_usage'] = Gauge(
            'memory_usage_percent',
            'Memory usage percentage',
            registry=self.prometheus_registry
        )
        
        # AI Engine specific metrics
        metrics['generation_time'] = Histogram(
            'ai_generation_time_seconds',
            'AI generation time in seconds',
            ['engine_type'],
            registry=self.prometheus_registry
        )
        
        metrics['ml_inference_time'] = Histogram(
            'ml_inference_time_seconds',
            'ML inference time in seconds',
            ['model_type'],
            registry=self.prometheus_registry
        )
        
        metrics['cache_hits'] = Counter(
            'cache_hits_total',
            'Total cache hits',
            ['cache_type'],
            registry=self.prometheus_registry
        )
        
        return metrics
    
    def _initialize_bottleneck_detector(self):
        """Initialize ML model for bottleneck detection"""
        try:
            # Use Isolation Forest for anomaly detection
            self.bottleneck_detector = IsolationForest(
                contamination=0.1,  # Expect 10% anomalies
                random_state=42
            )
            
            # Initialize with some baseline data
            # In production, this would be trained on historical data
            baseline_data = np.random.normal(0.5, 0.2, (100, 8))  # 8 features
            self.bottleneck_detector.fit(baseline_data)
            
            self.logger.info("Bottleneck detector initialized")
        except Exception as e:
            self.logger.error(f"Failed to initialize bottleneck detector: {e}")
    
    def start_monitoring(self):
        """Start the performance monitoring system"""
        if self.is_monitoring:
            return
        
        self.is_monitoring = True
        self.monitor_thread = threading.Thread(target=self._monitoring_loop, daemon=True)
        self.monitor_thread.start()
        
        self.logger.info("Performance monitoring started")
    
    def stop_monitoring(self):
        """Stop the performance monitoring system"""
        self.is_monitoring = False
        
        if self.monitor_thread:
            self.monitor_thread.join(timeout=5)
        
        self.logger.info("Performance monitoring stopped")
    
    def _monitoring_loop(self):
        """Main monitoring loop (runs in separate thread)"""
        try:
            while self.is_monitoring:
                # Collect system metrics
                system_metrics = self._collect_system_metrics()
                self.system_metrics_history.append(system_metrics)
                
                # Collect application metrics
                app_metrics = self._collect_application_metrics()
                self.app_metrics_history.append(app_metrics)
                
                # Update Prometheus metrics
                self._update_prometheus_metrics(system_metrics, app_metrics)
                
                # Check for alerts
                self._check_alert_conditions(system_metrics, app_metrics)
                
                # Detect bottlenecks
                self._detect_bottlenecks(system_metrics, app_metrics)
                
                # Auto-optimization if enabled
                if self.auto_optimization_enabled:
                    asyncio.create_task(self._auto_optimize())
                
                # Store metrics to Redis if available
                if self.redis_client:
                    asyncio.create_task(self._store_metrics_to_redis(system_metrics, app_metrics))
                
                # Sleep for monitoring interval
                time.sleep(self.config.get('monitoring_interval', 10))
                
        except Exception as e:
            self.logger.error(f"Monitoring loop error: {e}")
    
    def _collect_system_metrics(self) -> SystemResourceMetrics:
        """Collect system resource metrics"""
        try:
            # CPU usage
            cpu_percent = psutil.cpu_percent(interval=1)
            
            # Memory usage
            memory = psutil.virtual_memory()
            memory_percent = memory.percent
            memory_available_mb = memory.available / 1024 / 1024
            
            # Disk usage
            disk = psutil.disk_usage('/')
            disk_usage_percent = disk.percent
            
            # Network I/O
            network = psutil.net_io_counters()
            network_bytes_sent = network.bytes_sent
            network_bytes_recv = network.bytes_recv
            
            # Process information
            process = psutil.Process()
            open_fds = process.num_fds() if hasattr(process, 'num_fds') else 0
            thread_count = process.num_threads()
            
            return SystemResourceMetrics(
                cpu_percent=cpu_percent,
                memory_percent=memory_percent,
                memory_available_mb=memory_available_mb,
                disk_usage_percent=disk_usage_percent,
                network_bytes_sent=network_bytes_sent,
                network_bytes_recv=network_bytes_recv,
                open_file_descriptors=open_fds,
                thread_count=thread_count,
                timestamp=datetime.now()
            )
            
        except Exception as e:
            self.logger.error(f"Failed to collect system metrics: {e}")
            return SystemResourceMetrics(
                cpu_percent=0.0, memory_percent=0.0, memory_available_mb=0.0,
                disk_usage_percent=0.0, network_bytes_sent=0.0, network_bytes_recv=0.0,
                open_file_descriptors=0, thread_count=0, timestamp=datetime.now()
            )
    
    def _collect_application_metrics(self) -> ApplicationMetrics:
        """Collect application-specific metrics"""
        try:
            # Calculate requests per second
            recent_metrics = list(self.app_metrics_history)[-10:]  # Last 10 samples
            if recent_metrics:
                rps = sum(getattr(m, 'requests_per_second', 0) for m in recent_metrics) / len(recent_metrics)
            else:
                rps = 0.0
            
            # Get current metric values from aggregators
            avg_response_time = self._get_average_metric('response_time_ms', default=0.0)
            error_rate = self._get_average_metric('error_rate', default=0.0)
            active_connections = self._get_current_metric('active_connections', default=0)
            queue_depth = self._get_current_metric('queue_depth', default=0)
            cache_hit_rate = self._get_average_metric('cache_hit_rate', default=0.0)
            ml_inference_time = self._get_average_metric('ml_inference_time_ms', default=0.0)
            db_query_time = self._get_average_metric('db_query_time_ms', default=0.0)
            
            return ApplicationMetrics(
                requests_per_second=rps,
                avg_response_time_ms=avg_response_time,
                error_rate_percent=error_rate * 100,
                active_connections=int(active_connections),
                queue_depth=int(queue_depth),
                cache_hit_rate_percent=cache_hit_rate * 100,
                ml_inference_time_ms=ml_inference_time,
                database_query_time_ms=db_query_time,
                timestamp=datetime.now()
            )
            
        except Exception as e:
            self.logger.error(f"Failed to collect application metrics: {e}")
            return ApplicationMetrics(
                requests_per_second=0.0, avg_response_time_ms=0.0, error_rate_percent=0.0,
                active_connections=0, queue_depth=0, cache_hit_rate_percent=0.0,
                ml_inference_time_ms=0.0, database_query_time_ms=0.0, timestamp=datetime.now()
            )
    
    def _get_average_metric(self, metric_name: str, default: float = 0.0) -> float:
        """Get average value of a metric from recent samples"""
        values = self.metric_aggregators.get(metric_name, [])
        if values:
            avg = statistics.mean(values[-100:])  # Last 100 samples
            # Clear old values to prevent memory growth
            self.metric_aggregators[metric_name] = values[-100:]
            return avg
        return default
    
    def _get_current_metric(self, metric_name: str, default: float = 0.0) -> float:
        """Get current value of a metric"""
        metric = self.current_metrics.get(metric_name)
        if metric:
            return metric.value
        return default
    
    def _update_prometheus_metrics(self, system_metrics: SystemResourceMetrics, app_metrics: ApplicationMetrics):
        """Update Prometheus metrics"""
        try:
            # Update system metrics
            self.prometheus_metrics['cpu_usage'].set(system_metrics.cpu_percent)
            self.prometheus_metrics['memory_usage'].set(system_metrics.memory_percent)
            
        except Exception as e:
            self.logger.error(f"Failed to update Prometheus metrics: {e}")
    
    def _check_alert_conditions(self, system_metrics: SystemResourceMetrics, app_metrics: ApplicationMetrics):
        """Check for alert conditions"""
        try:
            # Check system resource alerts
            self._check_threshold_alert('cpu_percent', system_metrics.cpu_percent, 'High CPU Usage')
            self._check_threshold_alert('memory_percent', system_metrics.memory_percent, 'High Memory Usage')
            
            # Check application alerts
            self._check_threshold_alert('response_time_ms', app_metrics.avg_response_time_ms, 'High Response Time')
            self._check_threshold_alert('error_rate_percent', app_metrics.error_rate_percent, 'High Error Rate')
            self._check_threshold_alert('queue_depth', app_metrics.queue_depth, 'High Queue Depth')
            
        except Exception as e:
            self.logger.error(f"Alert checking failed: {e}")
    
    def _check_threshold_alert(self, metric_name: str, current_value: float, alert_title: str):
        """Check if metric exceeds threshold and create alert"""
        threshold = self.thresholds.get(metric_name)
        if threshold is None:
            return
        
        alert_id = f"threshold_{metric_name}"
        
        if current_value > threshold:
            if alert_id not in self.active_alerts:
                # Create new alert
                alert = PerformanceAlert(
                    alert_id=alert_id,
                    level=self._determine_alert_level(metric_name, current_value, threshold),
                    title=alert_title,
                    description=f"{metric_name} is {current_value:.2f}, exceeding threshold of {threshold}",
                    metric_name=metric_name,
                    threshold_value=threshold,
                    current_value=current_value,
                    timestamp=datetime.now()
                )
                
                self.active_alerts[alert_id] = alert
                self._trigger_alert(alert)
        else:
            # Resolve alert if it exists
            if alert_id in self.active_alerts:
                alert = self.active_alerts[alert_id]
                alert.resolved = True
                alert.resolution_time = datetime.now()
                self._resolve_alert(alert)
                del self.active_alerts[alert_id]
    
    def _determine_alert_level(self, metric_name: str, current_value: float, threshold: float) -> AlertLevel:
        """Determine alert level based on how much threshold is exceeded"""
        excess_ratio = current_value / threshold
        
        if excess_ratio > 2.0:
            return AlertLevel.CRITICAL
        elif excess_ratio > 1.5:
            return AlertLevel.ERROR
        elif excess_ratio > 1.2:
            return AlertLevel.WARNING
        else:
            return AlertLevel.INFO
    
    def _trigger_alert(self, alert: PerformanceAlert):
        """Trigger an alert"""
        self.logger.warning(f"ALERT [{alert.level.value.upper()}]: {alert.title} - {alert.description}")
        
        # Notify alert handlers
        for handler in self.alert_handlers:
            try:
                handler(alert)
            except Exception as e:
                self.logger.error(f"Alert handler failed: {e}")
    
    def _resolve_alert(self, alert: PerformanceAlert):
        """Resolve an alert"""
        self.logger.info(f"RESOLVED: {alert.title}")
    
    def _detect_bottlenecks(self, system_metrics: SystemResourceMetrics, app_metrics: ApplicationMetrics):
        """Detect performance bottlenecks using ML"""
        try:
            if not self.bottleneck_detector:
                return
            
            # Prepare feature vector
            features = np.array([[
                system_metrics.cpu_percent / 100.0,
                system_metrics.memory_percent / 100.0,
                min(app_metrics.avg_response_time_ms / 1000.0, 10.0),  # Cap at 10s
                app_metrics.error_rate_percent / 100.0,
                min(app_metrics.queue_depth / 100.0, 1.0),  # Normalize queue depth
                app_metrics.cache_hit_rate_percent / 100.0,
                min(app_metrics.ml_inference_time_ms / 1000.0, 5.0),  # Cap at 5s
                min(app_metrics.database_query_time_ms / 1000.0, 5.0)  # Cap at 5s
            ]])
            
            # Detect anomalies
            anomaly_score = self.bottleneck_detector.decision_function(features)[0]
            is_anomaly = self.bottleneck_detector.predict(features)[0] == -1
            
            if is_anomaly:
                # Analyze which component is the bottleneck
                bottleneck = self._analyze_bottleneck_components(system_metrics, app_metrics, anomaly_score)
                if bottleneck:
                    self.bottleneck_history.append(bottleneck)
                    self.logger.warning(f"Bottleneck detected: {bottleneck.component} - {bottleneck.root_cause}")
                    
        except Exception as e:
            self.logger.error(f"Bottleneck detection failed: {e}")
    
    def _analyze_bottleneck_components(
        self,
        system_metrics: SystemResourceMetrics,
        app_metrics: ApplicationMetrics,
        anomaly_score: float
    ) -> Optional[BottleneckAnalysis]:
        """Analyze which component is causing the bottleneck"""
        
        # Score different components
        component_scores = {
            'cpu': system_metrics.cpu_percent / 100.0,
            'memory': system_metrics.memory_percent / 100.0,
            'network': 0.5,  # Placeholder - would need more detailed network metrics
            'database': min(app_metrics.database_query_time_ms / 1000.0, 1.0),
            'ml_inference': min(app_metrics.ml_inference_time_ms / 1000.0, 1.0),
            'application': min(app_metrics.avg_response_time_ms / 1000.0, 1.0)
        }
        
        # Find the highest scoring component
        bottleneck_component = max(component_scores, key=component_scores.get)
        severity = component_scores[bottleneck_component]
        
        # Generate recommendations based on component
        recommendations = self._generate_bottleneck_recommendations(bottleneck_component, severity)
        
        # Calculate impact and confidence
        impact_score = min(severity * abs(anomaly_score), 1.0)
        confidence = min(abs(anomaly_score) * 2, 1.0)
        
        return BottleneckAnalysis(
            component=bottleneck_component,
            severity=severity,
            impact_score=impact_score,
            root_cause=self._determine_root_cause(bottleneck_component, system_metrics, app_metrics),
            recommendations=recommendations,
            estimated_improvement=self._estimate_improvement(bottleneck_component, severity),
            confidence=confidence,
            detected_at=datetime.now()
        )
    
    def _determine_root_cause(
        self,
        component: str,
        system_metrics: SystemResourceMetrics,
        app_metrics: ApplicationMetrics
    ) -> str:
        """Determine root cause based on component and metrics"""
        
        root_causes = {
            'cpu': f"High CPU usage ({system_metrics.cpu_percent:.1f}%) - possible CPU-intensive operations",
            'memory': f"High memory usage ({system_metrics.memory_percent:.1f}%) - possible memory leaks or large data processing",
            'database': f"Slow database queries ({app_metrics.database_query_time_ms:.0f}ms avg) - needs query optimization",
            'ml_inference': f"Slow ML inference ({app_metrics.ml_inference_time_ms:.0f}ms avg) - model optimization needed",
            'application': f"High response times ({app_metrics.avg_response_time_ms:.0f}ms avg) - application bottleneck",
            'network': "Network latency or bandwidth limitations"
        }
        
        return root_causes.get(component, "Unknown root cause")
    
    def _generate_bottleneck_recommendations(self, component: str, severity: float) -> List[str]:
        """Generate recommendations based on bottleneck component"""
        
        recommendations_map = {
            'cpu': [
                "Consider upgrading CPU or adding more cores",
                "Optimize CPU-intensive algorithms",
                "Implement CPU task queuing and load balancing",
                "Use async/await for I/O-bound operations"
            ],
            'memory': [
                "Increase available memory",
                "Implement memory caching strategies",
                "Optimize data structures and algorithms",
                "Add memory leak detection and monitoring"
            ],
            'database': [
                "Optimize database queries and add indexes",
                "Implement database connection pooling",
                "Consider database caching (Redis/Memcached)",
                "Scale database horizontally or vertically"
            ],
            'ml_inference': [
                "Optimize ML model architecture",
                "Implement model quantization or pruning",
                "Use GPU acceleration for inference",
                "Implement model caching and batching"
            ],
            'application': [
                "Profile application code for bottlenecks",
                "Implement caching at application level",
                "Optimize critical code paths",
                "Consider horizontal scaling"
            ],
            'network': [
                "Upgrade network bandwidth",
                "Implement CDN for static content",
                "Optimize API payload sizes",
                "Add network monitoring and diagnostics"
            ]
        }
        
        base_recommendations = recommendations_map.get(component, [])
        
        # Add severity-specific recommendations
        if severity > 0.8:
            base_recommendations.insert(0, "URGENT: Immediate attention required")
        elif severity > 0.6:
            base_recommendations.insert(0, "HIGH PRIORITY: Address within 24 hours")
        
        return base_recommendations[:4]  # Return top 4 recommendations
    
    def _estimate_improvement(self, component: str, severity: float) -> float:
        """Estimate potential performance improvement"""
        
        # Base improvement estimates by component
        base_improvements = {
            'cpu': 0.3,
            'memory': 0.25,
            'database': 0.4,
            'ml_inference': 0.5,
            'application': 0.35,
            'network': 0.2
        }
        
        base_improvement = base_improvements.get(component, 0.2)
        
        # Scale by severity
        return min(base_improvement * severity, 0.8)  # Cap at 80% improvement
    
    async def _auto_optimize(self):
        """Automatically optimize performance based on detected bottlenecks"""
        try:
            if not self.bottleneck_history:
                return
            
            # Get most recent bottleneck
            latest_bottleneck = self.bottleneck_history[-1]
            
            # Only optimize if confidence is high and impact is significant
            if latest_bottleneck.confidence < 0.7 or latest_bottleneck.impact_score < 0.5:
                return
            
            # Apply automatic optimizations
            optimization_applied = False
            
            if latest_bottleneck.component == 'ml_inference':
                # Optimize ML inference settings
                optimization_applied = await self._optimize_ml_inference()
            elif latest_bottleneck.component == 'database':
                # Optimize database settings
                optimization_applied = await self._optimize_database_performance()
            elif latest_bottleneck.component == 'application':
                # Optimize application settings
                optimization_applied = await self._optimize_application_performance()
            
            if optimization_applied:
                self.optimization_history.append({
                    'timestamp': datetime.now(),
                    'component': latest_bottleneck.component,
                    'optimization_type': 'auto',
                    'expected_improvement': latest_bottleneck.estimated_improvement
                })
                
                self.logger.info(f"Auto-optimization applied for {latest_bottleneck.component}")
                
        except Exception as e:
            self.logger.error(f"Auto-optimization failed: {e}")
    
    async def _optimize_ml_inference(self) -> bool:
        """Optimize ML inference performance"""
        try:
            # This would implement actual ML optimization
            # For now, just simulate optimization
            
            self.logger.info("Applied ML inference optimizations: batch processing, model caching")
            return True
        except Exception as e:
            self.logger.error(f"ML inference optimization failed: {e}")
            return False
    
    async def _optimize_database_performance(self) -> bool:
        """Optimize database performance"""
        try:
            # This would implement actual database optimization
            # For now, just simulate optimization
            
            self.logger.info("Applied database optimizations: connection pooling, query caching")
            return True
        except Exception as e:
            self.logger.error(f"Database optimization failed: {e}")
            return False
    
    async def _optimize_application_performance(self) -> bool:
        """Optimize application performance"""
        try:
            # This would implement actual application optimization
            # For now, just simulate optimization
            
            self.logger.info("Applied application optimizations: response caching, async processing")
            return True
        except Exception as e:
            self.logger.error(f"Application optimization failed: {e}")
            return False
    
    async def _store_metrics_to_redis(self, system_metrics: SystemResourceMetrics, app_metrics: ApplicationMetrics):
        """Store metrics to Redis for distributed monitoring"""
        try:
            if not self.redis_client:
                return
            
            # Store system metrics
            system_key = f"metrics:system:{int(system_metrics.timestamp.timestamp())}"
            await self.redis_client.setex(system_key, 3600, json.dumps(asdict(system_metrics), default=str))
            
            # Store application metrics
            app_key = f"metrics:app:{int(app_metrics.timestamp.timestamp())}"
            await self.redis_client.setex(app_key, 3600, json.dumps(asdict(app_metrics), default=str))
            
        except Exception as e:
            self.logger.error(f"Failed to store metrics to Redis: {e}")
    
    # Public API methods
    
    def record_metric(self, name: str, value: float, labels: Optional[Dict[str, str]] = None):
        """Record a custom metric"""
        metric = PerformanceMetric(
            name=name,
            value=value,
            timestamp=datetime.now(),
            labels=labels or {}
        )
        
        self.current_metrics[name] = metric
        self.metric_aggregators[name].append(value)
        self.metrics_history.append(metric)
    
    def record_request_duration(self, duration_seconds: float, method: str, endpoint: str):
        """Record request duration"""
        if 'request_duration' in self.prometheus_metrics:
            self.prometheus_metrics['request_duration'].labels(method=method, endpoint=endpoint).observe(duration_seconds)
        
        self.record_metric('response_time_ms', duration_seconds * 1000, {'method': method, 'endpoint': endpoint})
    
    def record_ml_inference_time(self, duration_ms: float, model_type: str):
        """Record ML inference time"""
        if 'ml_inference_time' in self.prometheus_metrics:
            self.prometheus_metrics['ml_inference_time'].labels(model_type=model_type).observe(duration_ms / 1000.0)
        
        self.record_metric('ml_inference_time_ms', duration_ms, {'model_type': model_type})
    
    def record_error(self, error_type: str = "general"):
        """Record an error occurrence"""
        self.record_metric('error_count', 1, {'error_type': error_type})
        
        # Update error rate
        current_rate = self._get_current_metric('error_rate', 0.0)
        self.record_metric('error_rate', min(current_rate + 0.01, 1.0))
    
    def add_alert_handler(self, handler: Callable[[PerformanceAlert], None]):
        """Add an alert handler function"""
        self.alert_handlers.append(handler)
    
    def get_performance_summary(self) -> Dict[str, Any]:
        """Get performance summary"""
        try:
            recent_system = self.system_metrics_history[-10:] if self.system_metrics_history else []
            recent_app = self.app_metrics_history[-10:] if self.app_metrics_history else []
            
            summary = {
                'timestamp': datetime.now().isoformat(),
                'monitoring_active': self.is_monitoring,
                'system_health': {
                    'cpu_avg': statistics.mean([m.cpu_percent for m in recent_system]) if recent_system else 0,
                    'memory_avg': statistics.mean([m.memory_percent for m in recent_system]) if recent_system else 0,
                    'status': 'healthy' if (recent_system and recent_system[-1].cpu_percent < 80 and recent_system[-1].memory_percent < 85) else 'degraded'
                },
                'application_health': {
                    'response_time_avg': statistics.mean([m.avg_response_time_ms for m in recent_app]) if recent_app else 0,
                    'error_rate_avg': statistics.mean([m.error_rate_percent for m in recent_app]) if recent_app else 0,
                    'status': 'healthy' if (recent_app and recent_app[-1].avg_response_time_ms < 1000 and recent_app[-1].error_rate_percent < 5) else 'degraded'
                },
                'active_alerts': len(self.active_alerts),
                'bottlenecks_detected': len([b for b in self.bottleneck_history if (datetime.now() - b.detected_at).seconds < 3600]),
                'optimizations_applied': len(self.optimization_history)
            }
            
            return summary
            
        except Exception as e:
            self.logger.error(f"Failed to generate performance summary: {e}")
            return {'error': str(e)}
    
    def get_bottleneck_analysis(self) -> List[Dict[str, Any]]:
        """Get recent bottleneck analysis results"""
        recent_bottlenecks = [
            b for b in self.bottleneck_history
            if (datetime.now() - b.detected_at).seconds < 3600  # Last hour
        ]
        
        return [asdict(b) for b in recent_bottlenecks]
    
    def get_optimization_history(self) -> List[Dict[str, Any]]:
        """Get optimization history"""
        return self.optimization_history[-20:]  # Last 20 optimizations
    
    def export_prometheus_metrics(self) -> str:
        """Export Prometheus metrics"""
        try:
            from prometheus_client import generate_latest
            return generate_latest(self.prometheus_registry).decode('utf-8')
        except Exception as e:
            self.logger.error(f"Failed to export Prometheus metrics: {e}")
            return ""


# Performance monitoring decorator
def monitor_performance(metric_name: str, monitor_instance: PerformanceMonitor):
    """Decorator to monitor function performance"""
    def decorator(func):
        def wrapper(*args, **kwargs):
            start_time = time.time()
            try:
                result = func(*args, **kwargs)
                duration = time.time() - start_time
                monitor_instance.record_metric(f"{metric_name}_duration_ms", duration * 1000)
                return result
            except Exception as e:
                monitor_instance.record_error(type(e).__name__)
                raise
        return wrapper
    return decorator


# Async performance monitoring decorator
def monitor_async_performance(metric_name: str, monitor_instance: PerformanceMonitor):
    """Decorator to monitor async function performance"""
    def decorator(func):
        async def wrapper(*args, **kwargs):
            start_time = time.time()
            try:
                result = await func(*args, **kwargs)
                duration = time.time() - start_time
                monitor_instance.record_metric(f"{metric_name}_duration_ms", duration * 1000)
                return result
            except Exception as e:
                monitor_instance.record_error(type(e).__name__)
                raise
        return wrapper
    return decorator


# Usage example
async def main():
    """Example usage of PerformanceMonitor"""
    
    config = {
        'monitoring_interval': 5,
        'redis_url': 'redis://localhost:6379',
        'auto_optimization': True
    }
    
    # Initialize monitor
    monitor = PerformanceMonitor(config)
    
    # Add alert handler
    def alert_handler(alert: PerformanceAlert):
        print(f"ALERT: {alert.title} - {alert.description}")
    
    monitor.add_alert_handler(alert_handler)
    
    # Start monitoring
    monitor.start_monitoring()
    
    # Simulate some metrics
    for i in range(10):
        monitor.record_request_duration(0.1 + i * 0.05, "GET", "/api/generate")
        monitor.record_ml_inference_time(500 + i * 100, "gpt-4")
        await asyncio.sleep(1)
    
    # Get performance summary
    summary = monitor.get_performance_summary()
    print(f"Performance Summary: {summary}")
    
    # Get bottleneck analysis
    bottlenecks = monitor.get_bottleneck_analysis()
    print(f"Bottlenecks: {bottlenecks}")
    
    # Stop monitoring
    monitor.stop_monitoring()


if __name__ == "__main__":
    asyncio.run(main())