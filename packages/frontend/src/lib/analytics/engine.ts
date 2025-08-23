/**
 * リアルタイム分析エンジン
 * CVR・ROI・LTV自動計算・A/Bテスト・ユーザーセグメント・予測分析
 */

import { EventEmitter } from 'events';

// 分析データ型定義
interface ConversionEvent {
  userId: string;
  sessionId: string;
  eventType: 'pageview' | 'signup' | 'purchase' | 'download' | 'contact';
  value: number;
  timestamp: number;
  metadata: Record<string, any>;
}

interface UserSegment {
  id: string;
  name: string;
  criteria: {
    demographic?: {
      age?: [number, number];
      gender?: string;
      location?: string[];
    };
    behavioral?: {
      pageViews?: number;
      sessionDuration?: number;
      purchaseHistory?: string[];
    };
    psychographic?: {
      interests?: string[];
      values?: string[];
    };
  };
  size: number;
  conversionRate: number;
  ltv: number;
}

interface ABTestResult {
  testId: string;
  variant: 'A' | 'B';
  conversionRate: number;
  sampleSize: number;
  confidence: number;
  pValue: number;
  statisticalSignificance: boolean;
  uplift: number;
}

interface PredictionModel {
  type: 'churn' | 'ltv' | 'conversion' | 'revenue';
  accuracy: number;
  features: string[];
  predictions: Record<string, number>;
  lastTrained: Date;
}

interface AnalyticsMetrics {
  cvr: number;
  roi: number;
  roas: number;
  ltv: number;
  cac: number;
  churnRate: number;
  avgOrderValue: number;
  sessionDuration: number;
  bounceRate: number;
  revenuePerUser: number;
}

interface RealTimeAlert {
  id: string;
  type: 'conversion_drop' | 'traffic_spike' | 'error_rate' | 'revenue_target';
  severity: 'low' | 'medium' | 'high' | 'critical';
  message: string;
  threshold: number;
  currentValue: number;
  timestamp: number;
  resolved: boolean;
}

/**
 * リアルタイム分析エンジンクラス
 */
export class AnalyticsEngine extends EventEmitter {
  private initialized = false;
  private metricsCache = new Map<string, any>();
  private websocket: WebSocket | null = null;
  private updateInterval: NodeJS.Timeout | null = null;
  private alertThresholds: Map<string, number> = new Map();
  private mlModels: Map<string, PredictionModel> = new Map();

  constructor() {
    super();
    this.setupDefaultAlertThresholds();
  }

  /**
   * エンジン初期化
   */
  async initialize(): Promise<void> {
    if (this.initialized) return;

    try {
      console.log('分析エンジン初期化中...');
      
      // WebSocket接続
      await this.setupWebSocket();
      
      // 機械学習モデル初期化
      await this.initializeMachineLearningModels();
      
      // リアルタイム更新開始
      this.startRealTimeUpdates();
      
      // アラートシステム開始
      this.startAlertMonitoring();
      
      this.initialized = true;
      this.emit('initialized');
      
      console.log('分析エンジン初期化完了');
      
    } catch (error) {
      console.error('分析エンジン初期化失敗:', error);
      throw error;
    }
  }

  /**
   * CVR計算（コンバージョン率）
   */
  async calculateCVR(
    startDate: Date,
    endDate: Date,
    segment?: string
  ): Promise<{
    overall: number;
    byChannel: Record<string, number>;
    bySegment: Record<string, number>;
    trend: Array<{ date: string; cvr: number }>;
  }> {
    try {
      const response = await fetch('/api/analytics/cvr', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          startDate: startDate.toISOString(),
          endDate: endDate.toISOString(),
          segment
        })
      });

      const data = await response.json();
      
      // キャッシュに保存
      this.metricsCache.set('cvr', data);
      
      return data;

    } catch (error) {
      console.error('CVR計算エラー:', error);
      throw error;
    }
  }

  /**
   * ROI/ROAS計算（投資収益率・広告費用対効果）
   */
  async calculateROI(
    campaign: string,
    startDate: Date,
    endDate: Date
  ): Promise<{
    roi: number;
    roas: number;
    totalRevenue: number;
    totalCost: number;
    profit: number;
    breakdown: Record<string, { revenue: number; cost: number; roi: number }>;
  }> {
    try {
      const response = await fetch('/api/analytics/roi', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          campaign,
          startDate: startDate.toISOString(),
          endDate: endDate.toISOString()
        })
      });

      const data = await response.json();
      
      // アラートチェック
      if (data.roi < this.alertThresholds.get('roi_minimum')!) {
        this.triggerAlert({
          type: 'conversion_drop',
          severity: 'high',
          message: `ROIが目標値を下回りました: ${data.roi.toFixed(2)}%`,
          threshold: this.alertThresholds.get('roi_minimum')!,
          currentValue: data.roi
        });
      }

      return data;

    } catch (error) {
      console.error('ROI計算エラー:', error);
      throw error;
    }
  }

  /**
   * LTV計算（顧客生涯価値）
   */
  async calculateLTV(
    segmentId?: string,
    predictive: boolean = false
  ): Promise<{
    currentLTV: number;
    predictedLTV?: number;
    ltv30Days: number;
    ltv90Days: number;
    ltv365Days: number;
    cohortAnalysis: Array<{
      cohort: string;
      retention: number[];
      revenue: number[];
      ltv: number;
    }>;
  }> {
    try {
      const response = await fetch('/api/analytics/ltv', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          segmentId,
          predictive
        })
      });

      const data = await response.json();

      // 予測LTVが有効な場合、MLモデル使用
      if (predictive && this.mlModels.has('ltv')) {
        const model = this.mlModels.get('ltv')!;
        data.predictedLTV = this.applyMLPrediction(model, data);
        data.confidence = model.accuracy;
      }

      return data;

    } catch (error) {
      console.error('LTV計算エラー:', error);
      throw error;
    }
  }

  /**
   * A/Bテスト統計的有意性検定
   */
  async performABTest(
    testId: string,
    variantA: { conversions: number; visitors: number },
    variantB: { conversions: number; visitors: number },
    confidenceLevel: number = 0.95
  ): Promise<ABTestResult> {
    try {
      // 変換率計算
      const cvrA = variantA.conversions / variantA.visitors;
      const cvrB = variantB.conversions / variantB.visitors;

      // 統計的有意性検定（Z検定）
      const pooledCVR = (variantA.conversions + variantB.conversions) / 
                       (variantA.visitors + variantB.visitors);
      
      const standardError = Math.sqrt(
        pooledCVR * (1 - pooledCVR) * (1/variantA.visitors + 1/variantB.visitors)
      );

      const zScore = Math.abs(cvrB - cvrA) / standardError;
      const pValue = 2 * (1 - this.normalCDF(Math.abs(zScore)));
      
      const alpha = 1 - confidenceLevel;
      const statisticalSignificance = pValue < alpha;
      
      const uplift = ((cvrB - cvrA) / cvrA) * 100;

      const result: ABTestResult = {
        testId,
        variant: cvrB > cvrA ? 'B' : 'A',
        conversionRate: Math.max(cvrA, cvrB),
        sampleSize: variantA.visitors + variantB.visitors,
        confidence: confidenceLevel,
        pValue,
        statisticalSignificance,
        uplift: Math.abs(uplift)
      };

      // 結果保存
      await this.saveABTestResult(result);

      return result;

    } catch (error) {
      console.error('A/Bテスト計算エラー:', error);
      throw error;
    }
  }

  /**
   * ユーザーセグメント分析
   */
  async analyzeUserSegments(): Promise<UserSegment[]> {
    try {
      const response = await fetch('/api/analytics/segments');
      const segments = await response.json();

      // 各セグメントのパフォーマンス計算
      for (const segment of segments) {
        const cvr = await this.calculateSegmentCVR(segment.id);
        const ltv = await this.calculateSegmentLTV(segment.id);
        
        segment.conversionRate = cvr;
        segment.ltv = ltv;
      }

      // MLによるセグメント最適化提案
      if (this.mlModels.has('conversion')) {
        const optimizedSegments = await this.optimizeSegments(segments);
        this.emit('segments_optimized', optimizedSegments);
      }

      return segments;

    } catch (error) {
      console.error('セグメント分析エラー:', error);
      throw error;
    }
  }

  /**
   * 予測分析・機械学習
   */
  async runPredictiveAnalysis(
    type: 'churn' | 'ltv' | 'conversion' | 'revenue',
    userId?: string
  ): Promise<{
    prediction: number;
    confidence: number;
    factors: Array<{ factor: string; importance: number }>;
    recommendations: string[];
  }> {
    try {
      const model = this.mlModels.get(type);
      if (!model) {
        throw new Error(`予測モデル '${type}' が見つかりません`);
      }

      const response = await fetch('/api/analytics/predict', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          type,
          userId,
          modelVersion: model.lastTrained.toISOString()
        })
      });

      const prediction = await response.json();

      // 推奨アクション生成
      const recommendations = this.generateRecommendations(type, prediction.prediction);

      return {
        prediction: prediction.value,
        confidence: model.accuracy,
        factors: prediction.factors,
        recommendations
      };

    } catch (error) {
      console.error('予測分析エラー:', error);
      throw error;
    }
  }

  /**
   * リアルタイムメトリクス取得
   */
  async getRealTimeMetrics(): Promise<AnalyticsMetrics> {
    try {
      const cached = this.metricsCache.get('realtime');
      if (cached && Date.now() - cached.timestamp < 30000) { // 30秒キャッシュ
        return cached.metrics;
      }

      const response = await fetch('/api/analytics/realtime');
      const metrics = await response.json();

      // キャッシュ更新
      this.metricsCache.set('realtime', {
        metrics,
        timestamp: Date.now()
      });

      // アラートチェック
      this.checkMetricAlerts(metrics);

      return metrics;

    } catch (error) {
      console.error('リアルタイムメトリクス取得エラー:', error);
      throw error;
    }
  }

  /**
   * カスタムイベント追跡
   */
  async trackCustomEvent(event: ConversionEvent): Promise<void> {
    try {
      // リアルタイム送信
      if (this.websocket?.readyState === WebSocket.OPEN) {
        this.websocket.send(JSON.stringify({
          type: 'custom_event',
          event
        }));
      }

      // バックエンド保存
      await fetch('/api/analytics/events', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(event)
      });

      this.emit('event_tracked', event);

    } catch (error) {
      console.error('カスタムイベント追跡エラー:', error);
      throw error;
    }
  }

  /**
   * パフォーマンスレポート生成
   */
  async generatePerformanceReport(
    startDate: Date,
    endDate: Date,
    format: 'summary' | 'detailed' | 'executive' = 'summary'
  ): Promise<{
    summary: AnalyticsMetrics;
    trends: Record<string, Array<{ date: string; value: number }>>;
    insights: string[];
    recommendations: string[];
    segments: UserSegment[];
    abTests: ABTestResult[];
  }> {
    try {
      const [metrics, segments, abTests] = await Promise.all([
        this.getRealTimeMetrics(),
        this.analyzeUserSegments(),
        this.getABTestResults(startDate, endDate)
      ]);

      // トレンド分析
      const trends = await this.getTrends(startDate, endDate);

      // AI生成インサイト
      const insights = await this.generateInsights(metrics, trends);
      
      // 改善提案
      const recommendations = await this.generateOptimizationRecommendations(
        metrics, 
        segments, 
        abTests
      );

      return {
        summary: metrics,
        trends,
        insights,
        recommendations,
        segments,
        abTests
      };

    } catch (error) {
      console.error('パフォーマンスレポート生成エラー:', error);
      throw error;
    }
  }

  // プライベートメソッド
  private setupDefaultAlertThresholds(): void {
    this.alertThresholds.set('cvr_minimum', 2.0);
    this.alertThresholds.set('roi_minimum', 100.0);
    this.alertThresholds.set('bounce_rate_maximum', 70.0);
    this.alertThresholds.set('error_rate_maximum', 5.0);
  }

  private async setupWebSocket(): Promise<void> {
    try {
      const wsUrl = process.env.NEXT_PUBLIC_ANALYTICS_WS_URL || 'ws://localhost:3001/analytics';
      this.websocket = new WebSocket(wsUrl);

      this.websocket.onopen = () => {
        console.log('分析エンジンWebSocket接続確立');
        this.emit('websocket_connected');
      };

      this.websocket.onmessage = (event) => {
        const data = JSON.parse(event.data);
        this.handleRealTimeData(data);
      };

      this.websocket.onerror = (error) => {
        console.error('WebSocket接続エラー:', error);
      };

    } catch (error) {
      console.warn('WebSocket接続失敗 - 非リアルタイムモードで続行');
    }
  }

  private async initializeMachineLearningModels(): Promise<void> {
    const modelTypes = ['churn', 'ltv', 'conversion', 'revenue'];
    
    for (const type of modelTypes) {
      try {
        const response = await fetch(`/api/ml/models/${type}`);
        const model = await response.json();
        
        this.mlModels.set(type, {
          type: type as any,
          accuracy: model.accuracy,
          features: model.features,
          predictions: {},
          lastTrained: new Date(model.lastTrained)
        });

      } catch (error) {
        console.warn(`MLモデル '${type}' の初期化失敗:`, error);
      }
    }
  }

  private startRealTimeUpdates(): void {
    this.updateInterval = setInterval(async () => {
      try {
        await this.getRealTimeMetrics();
        this.emit('metrics_updated');
      } catch (error) {
        console.error('リアルタイム更新エラー:', error);
      }
    }, 30000); // 30秒間隔
  }

  private startAlertMonitoring(): void {
    setInterval(async () => {
      try {
        const metrics = await this.getRealTimeMetrics();
        this.checkMetricAlerts(metrics);
      } catch (error) {
        console.error('アラート監視エラー:', error);
      }
    }, 60000); // 1分間隔
  }

  private checkMetricAlerts(metrics: AnalyticsMetrics): void {
    // CVRアラート
    if (metrics.cvr < this.alertThresholds.get('cvr_minimum')!) {
      this.triggerAlert({
        type: 'conversion_drop',
        severity: 'high',
        message: `コンバージョン率が目標値を下回りました: ${metrics.cvr.toFixed(2)}%`,
        threshold: this.alertThresholds.get('cvr_minimum')!,
        currentValue: metrics.cvr
      });
    }

    // 直帰率アラート
    if (metrics.bounceRate > this.alertThresholds.get('bounce_rate_maximum')!) {
      this.triggerAlert({
        type: 'conversion_drop',
        severity: 'medium',
        message: `直帰率が高くなっています: ${metrics.bounceRate.toFixed(2)}%`,
        threshold: this.alertThresholds.get('bounce_rate_maximum')!,
        currentValue: metrics.bounceRate
      });
    }
  }

  private triggerAlert(alertData: Omit<RealTimeAlert, 'id' | 'timestamp' | 'resolved'>): void {
    const alert: RealTimeAlert = {
      id: `alert_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      timestamp: Date.now(),
      resolved: false,
      ...alertData
    };

    this.emit('alert_triggered', alert);

    // WebSocket経由でリアルタイム通知
    if (this.websocket?.readyState === WebSocket.OPEN) {
      this.websocket.send(JSON.stringify({
        type: 'alert',
        alert
      }));
    }
  }

  private handleRealTimeData(data: any): void {
    switch (data.type) {
      case 'metrics_update':
        this.metricsCache.set('realtime', {
          metrics: data.metrics,
          timestamp: Date.now()
        });
        this.emit('metrics_updated', data.metrics);
        break;
        
      case 'conversion_event':
        this.emit('conversion_tracked', data.event);
        break;
        
      case 'alert_resolved':
        this.emit('alert_resolved', data.alertId);
        break;
    }
  }

  private normalCDF(x: number): number {
    // 標準正規分布の累積分布関数（近似）
    return 0.5 * (1 + this.erf(x / Math.sqrt(2)));
  }

  private erf(x: number): number {
    // エラー関数（近似）
    const a1 =  0.254829592;
    const a2 = -0.284496736;
    const a3 =  1.421413741;
    const a4 = -1.453152027;
    const a5 =  1.061405429;
    const p  =  0.3275911;

    const sign = x >= 0 ? 1 : -1;
    x = Math.abs(x);

    const t = 1.0 / (1.0 + p * x);
    const y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);

    return sign * y;
  }

  private applyMLPrediction(model: PredictionModel, data: any): number {
    // MLモデル予測のスタブ実装
    // 実際の実装では学習済みモデルを使用
    return data.currentLTV * (1 + model.accuracy);
  }

  private async calculateSegmentCVR(segmentId: string): Promise<number> {
    try {
      const response = await fetch(`/api/analytics/segments/${segmentId}/cvr`);
      const data = await response.json();
      return data.cvr;
    } catch (error) {
      console.error(`セグメント ${segmentId} のCVR計算エラー:`, error);
      return 0;
    }
  }

  private async calculateSegmentLTV(segmentId: string): Promise<number> {
    try {
      const response = await fetch(`/api/analytics/segments/${segmentId}/ltv`);
      const data = await response.json();
      return data.ltv;
    } catch (error) {
      console.error(`セグメント ${segmentId} のLTV計算エラー:`, error);
      return 0;
    }
  }

  private async optimizeSegments(segments: UserSegment[]): Promise<UserSegment[]> {
    // MLによるセグメント最適化のスタブ実装
    return segments.sort((a, b) => b.ltv - a.ltv);
  }

  private generateRecommendations(type: string, prediction: number): string[] {
    const recommendations = [];
    
    switch (type) {
      case 'churn':
        if (prediction > 0.7) {
          recommendations.push('顧客エンゲージメント向上施策を実施');
          recommendations.push('個別カスタマーサポートを提供');
        }
        break;
        
      case 'ltv':
        if (prediction < 100) {
          recommendations.push('アップセル・クロスセル戦略を強化');
          recommendations.push('ロイヤルティプログラムを導入');
        }
        break;
    }
    
    return recommendations;
  }

  private async saveABTestResult(result: ABTestResult): Promise<void> {
    try {
      await fetch('/api/analytics/abtests', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(result)
      });
    } catch (error) {
      console.error('A/Bテスト結果保存エラー:', error);
    }
  }

  private async getABTestResults(startDate: Date, endDate: Date): Promise<ABTestResult[]> {
    try {
      const response = await fetch('/api/analytics/abtests', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          startDate: startDate.toISOString(),
          endDate: endDate.toISOString()
        })
      });
      return await response.json();
    } catch (error) {
      console.error('A/Bテスト結果取得エラー:', error);
      return [];
    }
  }

  private async getTrends(startDate: Date, endDate: Date): Promise<Record<string, Array<{ date: string; value: number }>>> {
    try {
      const response = await fetch('/api/analytics/trends', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          startDate: startDate.toISOString(),
          endDate: endDate.toISOString()
        })
      });
      return await response.json();
    } catch (error) {
      console.error('トレンド取得エラー:', error);
      return {};
    }
  }

  private async generateInsights(metrics: AnalyticsMetrics, trends: any): Promise<string[]> {
    // AI生成インサイトのスタブ実装
    const insights = [];
    
    if (metrics.cvr < 2) {
      insights.push('コンバージョン率が業界平均を下回っています');
    }
    
    if (metrics.bounceRate > 70) {
      insights.push('直帰率が高く、ファーストビューの改善が必要です');
    }
    
    return insights;
  }

  private async generateOptimizationRecommendations(
    metrics: AnalyticsMetrics,
    segments: UserSegment[],
    abTests: ABTestResult[]
  ): Promise<string[]> {
    // 最適化提案生成のスタブ実装
    const recommendations = [];
    
    if (metrics.cvr < 3) {
      recommendations.push('CTAボタンの配置とデザインを最適化');
    }
    
    if (segments.length > 0) {
      const topSegment = segments.sort((a, b) => b.ltv - a.ltv)[0];
      recommendations.push(`高LTVセグメント「${topSegment.name}」をターゲットに施策展開`);
    }
    
    return recommendations;
  }

  /**
   * エンジン停止
   */
  destroy(): void {
    if (this.updateInterval) {
      clearInterval(this.updateInterval);
    }

    if (this.websocket) {
      this.websocket.close();
    }

    this.metricsCache.clear();
    this.removeAllListeners();
    this.initialized = false;
  }
}

// シングルトンインスタンス
export const analyticsEngine = new AnalyticsEngine();

export default analyticsEngine;