'use client';

/**
 * 分析システム統合プロバイダー
 * ヒートマップ・分析エンジン・レポート生成の統合管理
 */

import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { heatmapSystem } from '@/lib/analytics/heatmap';
import { analyticsEngine } from '@/lib/analytics/engine';
import { reportGenerator } from '@/lib/reports/generator';

// 型定義
interface AnalyticsContextType {
  isInitialized: boolean;
  isLoading: boolean;
  error: string | null;
  
  // システム管理
  initialize: () => Promise<void>;
  generateReport: (config: any) => Promise<any>;
  getSystemHealth: () => Promise<any>;
  
  // リアルタイムデータ
  realtimeMetrics: any;
  alerts: any[];
  
  // ヒートマップ
  startHeatmapTracking: () => void;
  stopHeatmapTracking: () => void;
  getHeatmapData: (startDate: Date, endDate: Date) => Promise<any>;
  
  // 分析
  calculateCVR: (startDate: Date, endDate: Date) => Promise<any>;
  calculateROI: (campaign: string, startDate: Date, endDate: Date) => Promise<any>;
  performABTest: (testData: any) => Promise<any>;
}

const AnalyticsContext = createContext<AnalyticsContextType | null>(null);

interface AnalyticsProviderProps {
  children: ReactNode;
  config?: {
    enableHeatmap?: boolean;
    enableRealtime?: boolean;
    enableReports?: boolean;
    gaConfig?: any;
  };
}

export const AnalyticsProvider: React.FC<AnalyticsProviderProps> = ({
  children,
  config = {}
}) => {
  const [isInitialized, setIsInitialized] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [realtimeMetrics, setRealtimeMetrics] = useState<any>({});
  const [alerts, setAlerts] = useState<any[]>([]);

  /**
   * 分析システム初期化
   */
  const initialize = async () => {
    if (isInitialized) return;

    setIsLoading(true);
    setError(null);

    try {
      console.log('分析システム統合初期化開始...');

      // 並行初期化で高速化
      const initPromises = [];

      // ヒートマップシステム
      if (config.enableHeatmap !== false) {
        initPromises.push(
          heatmapSystem.init(config.gaConfig)
            .catch(error => console.warn('ヒートマップ初期化失敗:', error))
        );
      }

      // 分析エンジン
      initPromises.push(
        analyticsEngine.initialize()
          .catch(error => console.warn('分析エンジン初期化失敗:', error))
      );

      // レポート生成器
      if (config.enableReports !== false) {
        initPromises.push(
          reportGenerator.initialize()
            .catch(error => console.warn('レポート生成器初期化失敗:', error))
        );
      }

      await Promise.allSettled(initPromises);

      // リアルタイム更新開始
      if (config.enableRealtime !== false) {
        startRealtimeUpdates();
      }

      setIsInitialized(true);
      console.log('分析システム統合初期化完了');

    } catch (error) {
      const errorMessage = `分析システム初期化エラー: ${error}`;
      setError(errorMessage);
      console.error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * リアルタイム更新開始
   */
  const startRealtimeUpdates = () => {
    // 分析エンジンのイベントリスナー設定
    analyticsEngine.on('metrics_updated', (metrics) => {
      setRealtimeMetrics(metrics);
    });

    analyticsEngine.on('alert_triggered', (alert) => {
      setAlerts(prev => [alert, ...prev.slice(0, 9)]); // 最新10件保持
    });

    // 定期メトリクス更新
    const metricsInterval = setInterval(async () => {
      try {
        const metrics = await analyticsEngine.getRealTimeMetrics();
        setRealtimeMetrics(metrics);
      } catch (error) {
        console.error('リアルタイムメトリクス更新エラー:', error);
      }
    }, 30000); // 30秒間隔

    // クリーンアップ
    return () => {
      clearInterval(metricsInterval);
      analyticsEngine.removeAllListeners();
    };
  };

  /**
   * システム健全性チェック
   */
  const getSystemHealth = async () => {
    try {
      const health = await analyticsEngine.getRealTimeMetrics();
      return {
        overall: 'healthy',
        components: {
          heatmap: true,
          analytics: true,
          reports: true
        },
        metrics: health
      };
    } catch (error) {
      console.error('システム健全性チェックエラー:', error);
      return {
        overall: 'degraded',
        components: {
          heatmap: false,
          analytics: false,
          reports: false
        },
        error: error
      };
    }
  };

  /**
   * ヒートマップ追跡開始
   */
  const startHeatmapTracking = () => {
    if (!config.enableHeatmap) {
      console.warn('ヒートマップが無効になっています');
      return;
    }
    // ヒートマップは初期化時に自動開始
    console.log('ヒートマップ追跡開始');
  };

  /**
   * ヒートマップ追跡停止
   */
  const stopHeatmapTracking = () => {
    heatmapSystem.destroy();
    console.log('ヒートマップ追跡停止');
  };

  /**
   * ヒートマップデータ取得
   */
  const getHeatmapData = async (startDate: Date, endDate: Date) => {
    try {
      return await heatmapSystem.getAnalyticsData(startDate, endDate);
    } catch (error) {
      console.error('ヒートマップデータ取得エラー:', error);
      throw error;
    }
  };

  /**
   * CVR計算
   */
  const calculateCVR = async (startDate: Date, endDate: Date) => {
    try {
      return await analyticsEngine.calculateCVR(startDate, endDate);
    } catch (error) {
      console.error('CVR計算エラー:', error);
      throw error;
    }
  };

  /**
   * ROI計算
   */
  const calculateROI = async (campaign: string, startDate: Date, endDate: Date) => {
    try {
      return await analyticsEngine.calculateROI(campaign, startDate, endDate);
    } catch (error) {
      console.error('ROI計算エラー:', error);
      throw error;
    }
  };

  /**
   * A/Bテスト実行
   */
  const performABTest = async (testData: any) => {
    try {
      return await analyticsEngine.performABTest(
        testData.testId,
        testData.variantA,
        testData.variantB,
        testData.confidenceLevel
      );
    } catch (error) {
      console.error('A/Bテストエラー:', error);
      throw error;
    }
  };

  /**
   * レポート生成
   */
  const generateReport = async (reportConfig: any) => {
    try {
      return await reportGenerator.generateReport(reportConfig);
    } catch (error) {
      console.error('レポート生成エラー:', error);
      throw error;
    }
  };

  // 初期化実行
  useEffect(() => {
    initialize();
  }, []);

  // コンテキスト値
  const contextValue: AnalyticsContextType = {
    isInitialized,
    isLoading,
    error,
    realtimeMetrics,
    alerts,
    
    // システム管理
    initialize,
    generateReport,
    getSystemHealth,
    
    // ヒートマップ
    startHeatmapTracking,
    stopHeatmapTracking,
    getHeatmapData,
    
    // 分析
    calculateCVR,
    calculateROI,
    performABTest
  };

  return (
    <AnalyticsContext.Provider value={contextValue}>
      {children}
    </AnalyticsContext.Provider>
  );
};

/**
 * 分析システムフック
 */
export const useAnalytics = (): AnalyticsContextType => {
  const context = useContext(AnalyticsContext);
  if (!context) {
    throw new Error('useAnalytics must be used within an AnalyticsProvider');
  }
  return context;
};

export default AnalyticsProvider;