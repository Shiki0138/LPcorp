/**
 * 分析システム統合フック
 * 全分析機能への統一アクセスポイント
 */

import { useState, useEffect, useCallback } from 'react';
import { analyticsIntegration } from '@/lib/analytics';
import { reportGenerator, type ReportConfig } from '@/lib/reports/generator';

interface AnalyticsState {
  isInitialized: boolean;
  isLoading: boolean;
  error: string | null;
  systemHealth: 'healthy' | 'degraded' | 'critical' | 'unknown';
}

interface ComprehensiveAnalysisOptions {
  includeHeatmap?: boolean;
  includeSegmentation?: boolean;
  includeABTests?: boolean;
  includePredictive?: boolean;
}

export const useAnalyticsIntegration = () => {
  const [state, setState] = useState<AnalyticsState>({
    isInitialized: false,
    isLoading: true,
    error: null,
    systemHealth: 'unknown'
  });

  const [realtimeData, setRealtimeData] = useState<any>({});
  const [alerts, setAlerts] = useState<any[]>([]);

  /**
   * システム初期化
   */
  useEffect(() => {
    const initializeSystem = async () => {
      try {
        setState(prev => ({ ...prev, isLoading: true, error: null }));

        const initialized = await analyticsIntegration.initialize({
          enableHeatmap: true,
          enableRealtime: true,
          gaConfig: {
            measurementId: process.env.NEXT_PUBLIC_GA4_MEASUREMENT_ID,
            apiSecret: process.env.GA4_API_SECRET
          }
        });

        setState(prev => ({
          ...prev,
          isInitialized: initialized,
          isLoading: false,
          systemHealth: initialized ? 'healthy' : 'critical',
          error: initialized ? null : 'システム初期化に失敗しました'
        }));

        // リアルタイム監視開始
        if (initialized) {
          startRealtimeMonitoring();
        }

      } catch (error) {
        setState(prev => ({
          ...prev,
          isInitialized: false,
          isLoading: false,
          systemHealth: 'critical',
          error: `初期化エラー: ${error}`
        }));
      }
    };

    initializeSystem();
  }, []);

  /**
   * リアルタイム監視開始
   */
  const startRealtimeMonitoring = useCallback(() => {
    analyticsIntegration.startRealTimeMonitoring({
      onMetricsUpdate: (metrics) => {
        setRealtimeData(metrics);
      },
      onAlert: (alert) => {
        setAlerts(prev => [alert, ...prev.slice(0, 9)]); // 最新10件保持
      },
      onConversion: (event) => {
        console.log('コンバージョンイベント:', event);
      }
    });
  }, []);

  /**
   * 包括的分析実行
   */
  const performComprehensiveAnalysis = useCallback(async (
    startDate: Date,
    endDate: Date,
    options: ComprehensiveAnalysisOptions = {}
  ) => {
    if (!state.isInitialized) {
      throw new Error('分析システムが初期化されていません');
    }

    try {
      return await analyticsIntegration.performComprehensiveAnalysis(
        startDate,
        endDate,
        {
          includeHeatmap: options.includeHeatmap ?? true,
          includeSegmentation: options.includeSegmentation ?? true,
          includeABTests: options.includeABTests ?? true,
          includePredictive: options.includePredictive ?? true
        }
      );
    } catch (error) {
      console.error('包括的分析エラー:', error);
      throw error;
    }
  }, [state.isInitialized]);

  /**
   * 自動レポート生成
   */
  const generateAutomatedReport = useCallback(async (
    type: 'performance' | 'analytics' | 'executive' = 'performance',
    format: 'pdf' | 'excel' | 'html' = 'pdf',
    dateRange?: { start: Date; end: Date }
  ) => {
    if (!state.isInitialized) {
      throw new Error('分析システムが初期化されていません');
    }

    const reportConfig: ReportConfig = {
      type,
      format,
      dateRange: dateRange || {
        start: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000), // 7日前
        end: new Date()
      },
      includeCharts: true,
      includeRecommendations: true,
      language: 'ja'
    };

    try {
      return await reportGenerator.generateReport(reportConfig);
    } catch (error) {
      console.error('レポート生成エラー:', error);
      throw error;
    }
  }, [state.isInitialized]);

  /**
   * A/Bテスト結果分析
   */
  const analyzeABTestResults = useCallback(async (
    testData: {
      testId: string;
      variantA: { conversions: number; visitors: number };
      variantB: { conversions: number; visitors: number };
      confidenceLevel?: number;
    }
  ) => {
    if (!state.isInitialized) {
      throw new Error('分析システムが初期化されていません');
    }

    try {
      const { analyticsEngine } = await import('@/lib/analytics/engine');
      return await analyticsEngine.performABTest(
        testData.testId,
        testData.variantA,
        testData.variantB,
        testData.confidenceLevel || 0.95
      );
    } catch (error) {
      console.error('A/Bテスト分析エラー:', error);
      throw error;
    }
  }, [state.isInitialized]);

  /**
   * ROI分析
   */
  const analyzeROI = useCallback(async (
    campaign: string,
    startDate: Date,
    endDate: Date
  ) => {
    if (!state.isInitialized) {
      throw new Error('分析システムが初期化されていません');
    }

    try {
      const { analyticsEngine } = await import('@/lib/analytics/engine');
      return await analyticsEngine.calculateROI(campaign, startDate, endDate);
    } catch (error) {
      console.error('ROI分析エラー:', error);
      throw error;
    }
  }, [state.isInitialized]);

  /**
   * ユーザーセグメント分析
   */
  const analyzeUserSegments = useCallback(async () => {
    if (!state.isInitialized) {
      throw new Error('分析システムが初期化されていません');
    }

    try {
      const { analyticsEngine } = await import('@/lib/analytics/engine');
      return await analyticsEngine.analyzeUserSegments();
    } catch (error) {
      console.error('セグメント分析エラー:', error);
      throw error;
    }
  }, [state.isInitialized]);

  /**
   * 予測分析実行
   */
  const runPredictiveAnalysis = useCallback(async (
    type: 'churn' | 'ltv' | 'conversion' | 'revenue',
    userId?: string
  ) => {
    if (!state.isInitialized) {
      throw new Error('分析システムが初期化されていません');
    }

    try {
      const { analyticsEngine } = await import('@/lib/analytics/engine');
      return await analyticsEngine.runPredictiveAnalysis(type, userId);
    } catch (error) {
      console.error('予測分析エラー:', error);
      throw error;
    }
  }, [state.isInitialized]);

  /**
   * ヒートマップデータ取得
   */
  const getHeatmapAnalysis = useCallback(async (
    startDate: Date,
    endDate: Date,
    pageUrl?: string
  ) => {
    if (!state.isInitialized) {
      throw new Error('分析システムが初期化されていません');
    }

    try {
      const { heatmapSystem } = await import('@/lib/analytics/heatmap');
      const data = await heatmapSystem.getAnalyticsData(startDate, endDate, pageUrl);
      return data ? heatmapSystem.generateAnalysisReport(data) : null;
    } catch (error) {
      console.error('ヒートマップ分析エラー:', error);
      throw error;
    }
  }, [state.isInitialized]);

  /**
   * パフォーマンス最適化実行
   */
  const optimizeSystemPerformance = useCallback(async () => {
    if (!state.isInitialized) {
      throw new Error('分析システムが初期化されていません');
    }

    try {
      return await analyticsIntegration.optimizePerformance();
    } catch (error) {
      console.error('パフォーマンス最適化エラー:', error);
      throw error;
    }
  }, [state.isInitialized]);

  /**
   * カスタムイベント追跡
   */
  const trackCustomEvent = useCallback(async (event: {
    userId: string;
    sessionId: string;
    eventType: string;
    value: number;
    metadata?: Record<string, any>;
  }) => {
    if (!state.isInitialized) {
      console.warn('分析システムが初期化されていません');
      return;
    }

    try {
      const { analyticsEngine } = await import('@/lib/analytics/engine');
      await analyticsEngine.trackCustomEvent({
        ...event,
        timestamp: Date.now()
      });
    } catch (error) {
      console.error('イベント追跡エラー:', error);
    }
  }, [state.isInitialized]);

  /**
   * システム健全性チェック
   */
  const checkSystemHealth = useCallback(async () => {
    try {
      const health = await analyticsIntegration.getPerformanceMetrics();
      const healthStatus = health.errors?.length > 0 ? 'degraded' : 'healthy';
      
      setState(prev => ({
        ...prev,
        systemHealth: healthStatus
      }));
      
      return health;
    } catch (error) {
      setState(prev => ({
        ...prev,
        systemHealth: 'critical'
      }));
      throw error;
    }
  }, []);

  /**
   * エラーリセット
   */
  const clearError = useCallback(() => {
    setState(prev => ({ ...prev, error: null }));
  }, []);

  /**
   * アラートクリア
   */
  const clearAlerts = useCallback(() => {
    setAlerts([]);
  }, []);

  return {
    // 状態
    ...state,
    realtimeData,
    alerts,
    
    // 分析機能
    performComprehensiveAnalysis,
    analyzeABTestResults,
    analyzeROI,
    analyzeUserSegments,
    runPredictiveAnalysis,
    getHeatmapAnalysis,
    
    // レポート機能
    generateAutomatedReport,
    
    // システム管理
    optimizeSystemPerformance,
    checkSystemHealth,
    trackCustomEvent,
    clearError,
    clearAlerts
  };
};

export default useAnalyticsIntegration;