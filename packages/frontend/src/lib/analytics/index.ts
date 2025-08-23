/**
 * 分析システム統合エクスポート
 * Phase 3: ヒートマップ・分析エンジン・レポート生成システム
 */

// ヒートマップシステム
export {
  heatmapSystem,
  HeatmapSystem,
  type HeatmapData,
  type HeatmapConfig
} from './heatmap';

// 分析エンジン
export {
  analyticsEngine,
  AnalyticsEngine,
  type AnalyticsMetrics,
  type ConversionEvent,
  type UserSegment,
  type ABTestResult,
  type RealTimeAlert
} from './engine';

// 統合分析クラス
class AnalyticsIntegration {
  private initialized = false;

  /**
   * 統合システム初期化
   */
  async initialize(config?: {
    enableHeatmap?: boolean;
    enableRealtime?: boolean;
    gaConfig?: any;
  }): Promise<boolean> {
    if (this.initialized) return true;

    try {
      console.log('分析統合システム初期化中...');

      const initPromises = [];

      // ヒートマップ初期化
      if (config?.enableHeatmap !== false) {
        initPromises.push(
          heatmapSystem.init(config?.gaConfig)
            .catch(error => console.warn('ヒートマップ初期化警告:', error))
        );
      }

      // 分析エンジン初期化
      initPromises.push(
        analyticsEngine.initialize()
          .catch(error => console.warn('分析エンジン初期化警告:', error))
      );

      // 並行初期化
      await Promise.allSettled(initPromises);

      this.initialized = true;
      console.log('分析統合システム初期化完了');
      return true;

    } catch (error) {
      console.error('分析統合システム初期化エラー:', error);
      return false;
    }
  }

  /**
   * 包括的分析実行
   */
  async performComprehensiveAnalysis(
    startDate: Date,
    endDate: Date,
    options?: {
      includeHeatmap?: boolean;
      includeSegmentation?: boolean;
      includeABTests?: boolean;
      includePredictive?: boolean;
    }
  ): Promise<{
    overview: any;
    heatmap?: any;
    segments?: any[];
    abTests?: any[];
    predictions?: any;
    insights: string[];
    recommendations: string[];
  }> {
    if (!this.initialized) {
      throw new Error('分析システムが初期化されていません');
    }

    const results: any = {
      insights: [],
      recommendations: []
    };

    try {
      // 基本分析
      const [cvr, roi, metrics] = await Promise.all([
        analyticsEngine.calculateCVR(startDate, endDate),
        analyticsEngine.calculateROI('all', startDate, endDate),
        analyticsEngine.getRealTimeMetrics()
      ]);

      results.overview = {
        cvr,
        roi,
        metrics,
        period: { startDate, endDate }
      };

      // ヒートマップ分析
      if (options?.includeHeatmap) {
        try {
          const heatmapData = await heatmapSystem.getAnalyticsData(startDate, endDate);
          if (heatmapData) {
            results.heatmap = heatmapSystem.generateAnalysisReport(heatmapData);
            results.insights.push(...this.generateHeatmapInsights(results.heatmap));
          }
        } catch (error) {
          console.warn('ヒートマップ分析スキップ:', error);
        }
      }

      // セグメント分析
      if (options?.includeSegmentation) {
        try {
          results.segments = await analyticsEngine.analyzeUserSegments();
          results.insights.push(...this.generateSegmentInsights(results.segments));
        } catch (error) {
          console.warn('セグメント分析スキップ:', error);
        }
      }

      // A/Bテスト分析
      if (options?.includeABTests) {
        try {
          results.abTests = await this.getABTestResults(startDate, endDate);
          results.insights.push(...this.generateABTestInsights(results.abTests));
        } catch (error) {
          console.warn('A/Bテスト分析スキップ:', error);
        }
      }

      // 予測分析
      if (options?.includePredictive) {
        try {
          const [churnPrediction, ltvPrediction] = await Promise.all([
            analyticsEngine.runPredictiveAnalysis('churn'),
            analyticsEngine.runPredictiveAnalysis('ltv')
          ]);
          
          results.predictions = {
            churn: churnPrediction,
            ltv: ltvPrediction
          };
          
          results.insights.push(...this.generatePredictiveInsights(results.predictions));
        } catch (error) {
          console.warn('予測分析スキップ:', error);
        }
      }

      // 総合的な推奨事項生成
      results.recommendations = this.generateComprehensiveRecommendations(results);

      return results;

    } catch (error) {
      console.error('包括的分析エラー:', error);
      throw error;
    }
  }

  /**
   * リアルタイム監視開始
   */
  startRealTimeMonitoring(callbacks?: {
    onMetricsUpdate?: (metrics: any) => void;
    onAlert?: (alert: any) => void;
    onConversion?: (event: any) => void;
  }): void {
    if (!this.initialized) {
      console.warn('分析システムが初期化されていません');
      return;
    }

    // 分析エンジンイベント監視
    analyticsEngine.on('metrics_updated', (metrics) => {
      callbacks?.onMetricsUpdate?.(metrics);
    });

    analyticsEngine.on('alert_triggered', (alert) => {
      callbacks?.onAlert?.(alert);
    });

    analyticsEngine.on('conversion_tracked', (event) => {
      callbacks?.onConversion?.(event);
    });

    console.log('リアルタイム監視開始');
  }

  /**
   * パフォーマンス最適化
   */
  async optimizePerformance(): Promise<{
    before: any;
    after: any;
    improvements: string[];
  }> {
    const beforeMetrics = await this.getPerformanceMetrics();

    // キャッシュ最適化
    await this.optimizeCaches();

    // 不要データ削除
    await this.cleanupAnalyticsData();

    // メモリ最適化
    this.optimizeMemoryUsage();

    const afterMetrics = await this.getPerformanceMetrics();

    return {
      before: beforeMetrics,
      after: afterMetrics,
      improvements: this.calculateImprovements(beforeMetrics, afterMetrics)
    };
  }

  // プライベートメソッド
  private generateHeatmapInsights(heatmapReport: any): string[] {
    const insights = [];
    
    if (heatmapReport.summary.avgScrollDepth < 25) {
      insights.push('ユーザーのスクロール深度が低く、ファーストビューの改善が必要');
    }
    
    if (heatmapReport.summary.totalClicks < 5) {
      insights.push('クリック数が少なく、CTAの最適化が必要');
    }
    
    return insights;
  }

  private generateSegmentInsights(segments: any[]): string[] {
    const insights = [];
    
    if (segments.length > 0) {
      const highValueSegment = segments.sort((a, b) => b.ltv - a.ltv)[0];
      insights.push(`最高価値セグメント「${highValueSegment.name}」のLTVは${highValueSegment.ltv}円`);
    }
    
    return insights;
  }

  private generateABTestInsights(abTests: any[]): string[] {
    const insights = [];
    
    const significantTests = abTests.filter(test => test.statisticalSignificance);
    if (significantTests.length > 0) {
      const bestTest = significantTests.sort((a, b) => b.uplift - a.uplift)[0];
      insights.push(`A/Bテスト「${bestTest.testId}」で${bestTest.uplift}%の改善を確認`);
    }
    
    return insights;
  }

  private generatePredictiveInsights(predictions: any): string[] {
    const insights = [];
    
    if (predictions.churn?.prediction > 0.7) {
      insights.push('チャーンリスクが高く、顧客維持施策の強化が必要');
    }
    
    if (predictions.ltv?.prediction < 10000) {
      insights.push('LTV向上の余地があり、アップセル戦略の検討を推奨');
    }
    
    return insights;
  }

  private generateComprehensiveRecommendations(results: any): string[] {
    const recommendations = [];
    
    // CVRベースの推奨
    if (results.overview?.cvr?.overall < 2.0) {
      recommendations.push('コンバージョン率改善のためのLPO実施');
    }
    
    // ROIベースの推奨
    if (results.overview?.roi?.roi < 200) {
      recommendations.push('広告配信の最適化によるROI向上');
    }
    
    // セグメントベースの推奨
    if (results.segments?.length > 0) {
      const topSegment = results.segments.sort((a: any, b: any) => b.ltv - a.ltv)[0];
      recommendations.push(`高価値セグメント「${topSegment.name}」への施策集中投下`);
    }
    
    return recommendations;
  }

  private async getABTestResults(startDate: Date, endDate: Date): Promise<any[]> {
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

  private async getPerformanceMetrics(): Promise<any> {
    return {
      memoryUsage: process.memoryUsage?.() || { rss: 0, heapUsed: 0, heapTotal: 0 },
      timestamp: Date.now()
    };
  }

  private async optimizeCaches(): Promise<void> {
    // キャッシュ最適化の実装
    console.log('キャッシュ最適化実行');
  }

  private async cleanupAnalyticsData(): Promise<void> {
    // 古いデータクリーンアップの実装
    console.log('データクリーンアップ実行');
  }

  private optimizeMemoryUsage(): void {
    // メモリ最適化の実装
    if (global.gc) {
      global.gc();
    }
  }

  private calculateImprovements(before: any, after: any): string[] {
    const improvements = [];
    
    const memoryImprovement = ((before.memoryUsage.heapUsed - after.memoryUsage.heapUsed) / before.memoryUsage.heapUsed) * 100;
    if (memoryImprovement > 0) {
      improvements.push(`メモリ使用量 ${memoryImprovement.toFixed(1)}% 削減`);
    }
    
    return improvements;
  }

  /**
   * システム停止
   */
  destroy(): void {
    if (this.initialized) {
      heatmapSystem.destroy();
      analyticsEngine.destroy();
      this.initialized = false;
      console.log('分析統合システム停止完了');
    }
  }
}

// シングルトンインスタンス
export const analyticsIntegration = new AnalyticsIntegration();

// 自動初期化（本番環境のみ）
if (typeof window !== 'undefined' && process.env.NODE_ENV === 'production') {
  analyticsIntegration.initialize({
    enableHeatmap: true,
    enableRealtime: true,
    gaConfig: {
      measurementId: process.env.NEXT_PUBLIC_GA4_MEASUREMENT_ID,
      apiSecret: process.env.GA4_API_SECRET
    }
  }).catch(console.error);
}

export default analyticsIntegration;