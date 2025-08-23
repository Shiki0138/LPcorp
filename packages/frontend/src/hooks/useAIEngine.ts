/**
 * AI統合エンジン Reactフック
 * UI連携・Redis統合・エラーハンドリング実装
 */

import { useState, useEffect, useCallback } from 'react';
import {
  aiIntegrationManager,
  lpGenerationEngine,
  competitiveAnalysisEngine,
  trendPredictionEngine,
  learningPatternSystem,
  type GenerationRequest,
  type GenerationResult,
  type CompetitiveAnalysisResult,
  type TrendPredictionResult
} from '@/lib/ai';

interface AIEngineState {
  isInitialized: boolean;
  isLoading: boolean;
  error: string | null;
  systemHealth: 'healthy' | 'degraded' | 'critical' | 'unknown';
}

interface GenerationState {
  isGenerating: boolean;
  progress: number;
  currentStep: string;
  result: GenerationResult | null;
  error: string | null;
}

interface AnalysisState {
  isAnalyzing: boolean;
  competitiveResult: CompetitiveAnalysisResult | null;
  trendResult: TrendPredictionResult | null;
  error: string | null;
}

export const useAIEngine = () => {
  const [aiState, setAIState] = useState<AIEngineState>({
    isInitialized: false,
    isLoading: true,
    error: null,
    systemHealth: 'unknown'
  });

  const [generationState, setGenerationState] = useState<GenerationState>({
    isGenerating: false,
    progress: 0,
    currentStep: '',
    result: null,
    error: null
  });

  const [analysisState, setAnalysisState] = useState<AnalysisState>({
    isAnalyzing: false,
    competitiveResult: null,
    trendResult: null,
    error: null
  });

  /**
   * AI システム初期化
   */
  useEffect(() => {
    const initializeAI = async () => {
      try {
        setAIState(prev => ({ ...prev, isLoading: true }));
        
        const initialized = await aiIntegrationManager.initialize();
        const health = await aiIntegrationManager.getSystemHealth();
        
        setAIState({
          isInitialized: initialized,
          isLoading: false,
          error: initialized ? null : 'AI初期化に失敗しました',
          systemHealth: health.overall
        });

      } catch (error) {
        setAIState({
          isInitialized: false,
          isLoading: false,
          error: `AI初期化エラー: ${error}`,
          systemHealth: 'critical'
        });
      }
    };

    initializeAI();
  }, []);

  /**
   * LP自動生成実行
   */
  const generateLP = useCallback(async (request: GenerationRequest): Promise<GenerationResult | null> => {
    if (!aiState.isInitialized) {
      throw new Error('AI システムが初期化されていません');
    }

    try {
      setGenerationState(prev => ({
        ...prev,
        isGenerating: true,
        progress: 0,
        currentStep: 'パターン分析中...',
        error: null
      }));

      // プログレス更新シミュレーション
      const updateProgress = (step: string, progress: number) => {
        setGenerationState(prev => ({
          ...prev,
          currentStep: step,
          progress
        }));
      };

      updateProgress('最適パターン選択中...', 20);
      await new Promise(resolve => setTimeout(resolve, 500));

      updateProgress('HTMLコンテンツ生成中...', 40);
      await new Promise(resolve => setTimeout(resolve, 800));

      updateProgress('CSSスタイル生成中...', 60);
      await new Promise(resolve => setTimeout(resolve, 600));

      updateProgress('JavaScript機能実装中...', 80);
      await new Promise(resolve => setTimeout(resolve, 400));

      updateProgress('最終最適化処理中...', 95);

      const result = await lpGenerationEngine.generateLP(request);

      setGenerationState(prev => ({
        ...prev,
        isGenerating: false,
        progress: 100,
        currentStep: '生成完了!',
        result,
        error: null
      }));

      return result;

    } catch (error) {
      const errorMessage = `LP生成エラー: ${error}`;
      setGenerationState(prev => ({
        ...prev,
        isGenerating: false,
        error: errorMessage,
        result: null
      }));
      
      await aiIntegrationManager.handleSystemError(error as Error, 'generator');
      throw new Error(errorMessage);
    }
  }, [aiState.isInitialized]);

  /**
   * 競合分析実行
   */
  const analyzeCompetitors = useCallback(async (
    industry: string,
    product: string,
    targetMarket: string
  ): Promise<CompetitiveAnalysisResult | null> => {
    if (!aiState.isInitialized) {
      throw new Error('AI システムが初期化されていません');
    }

    try {
      setAnalysisState(prev => ({
        ...prev,
        isAnalyzing: true,
        error: null
      }));

      const result = await competitiveAnalysisEngine.analyzeCompetitors(
        industry,
        product,
        targetMarket
      );

      setAnalysisState(prev => ({
        ...prev,
        isAnalyzing: false,
        competitiveResult: result,
        error: null
      }));

      return result;

    } catch (error) {
      const errorMessage = `競合分析エラー: ${error}`;
      setAnalysisState(prev => ({
        ...prev,
        isAnalyzing: false,
        error: errorMessage
      }));
      
      await aiIntegrationManager.handleSystemError(error as Error, 'competitive');
      throw new Error(errorMessage);
    }
  }, [aiState.isInitialized]);

  /**
   * トレンド予測実行
   */
  const predictTrends = useCallback(async (
    industry: string,
    businessType: string,
    targetMarket: string
  ): Promise<TrendPredictionResult | null> => {
    if (!aiState.isInitialized) {
      throw new Error('AI システムが初期化されていません');
    }

    try {
      setAnalysisState(prev => ({
        ...prev,
        isAnalyzing: true,
        error: null
      }));

      const result = await trendPredictionEngine.predictMarketTrends(
        industry,
        businessType,
        targetMarket
      );

      setAnalysisState(prev => ({
        ...prev,
        isAnalyzing: false,
        trendResult: result,
        error: null
      }));

      return result;

    } catch (error) {
      const errorMessage = `トレンド予測エラー: ${error}`;
      setAnalysisState(prev => ({
        ...prev,
        isAnalyzing: false,
        error: errorMessage
      }));
      
      await aiIntegrationManager.handleSystemError(error as Error, 'trends');
      throw new Error(errorMessage);
    }
  }, [aiState.isInitialized]);

  /**
   * 統合AI分析実行 - 競合・トレンド同時実行
   */
  const performComprehensiveAnalysis = useCallback(async (
    industry: string,
    businessType: string,
    product: string,
    targetMarket: string
  ) => {
    if (!aiState.isInitialized) {
      throw new Error('AI システムが初期化されていません');
    }

    try {
      setAnalysisState(prev => ({
        ...prev,
        isAnalyzing: true,
        error: null
      }));

      // 並行実行で高速化
      const [competitiveResult, trendResult] = await Promise.all([
        competitiveAnalysisEngine.analyzeCompetitors(industry, product, targetMarket),
        trendPredictionEngine.predictMarketTrends(industry, businessType, targetMarket)
      ]);

      setAnalysisState(prev => ({
        ...prev,
        isAnalyzing: false,
        competitiveResult,
        trendResult,
        error: null
      }));

      return { competitiveResult, trendResult };

    } catch (error) {
      const errorMessage = `統合分析エラー: ${error}`;
      setAnalysisState(prev => ({
        ...prev,
        isAnalyzing: false,
        error: errorMessage
      }));
      
      throw new Error(errorMessage);
    }
  }, [aiState.isInitialized]);

  /**
   * システム健全性チェック
   */
  const checkSystemHealth = useCallback(async () => {
    try {
      const health = await aiIntegrationManager.getSystemHealth();
      setAIState(prev => ({
        ...prev,
        systemHealth: health.overall
      }));
      return health;
    } catch (error) {
      console.error('Health check failed:', error);
      return null;
    }
  }, []);

  /**
   * パフォーマンス統計取得
   */
  const getUsageStats = useCallback(async (timeframe: '1h' | '24h' | '7d' | '30d' = '24h') => {
    try {
      return await aiIntegrationManager.getUsageAnalytics(timeframe);
    } catch (error) {
      console.error('Failed to get usage stats:', error);
      return null;
    }
  }, []);

  /**
   * エラーリセット
   */
  const clearErrors = useCallback(() => {
    setGenerationState(prev => ({ ...prev, error: null }));
    setAnalysisState(prev => ({ ...prev, error: null }));
    setAIState(prev => ({ ...prev, error: null }));
  }, []);

  /**
   * 生成結果リセット
   */
  const clearResults = useCallback(() => {
    setGenerationState(prev => ({
      ...prev,
      result: null,
      progress: 0,
      currentStep: ''
    }));
    setAnalysisState(prev => ({
      ...prev,
      competitiveResult: null,
      trendResult: null
    }));
  }, []);

  return {
    // 状態
    aiState,
    generationState,
    analysisState,
    
    // AI生成機能
    generateLP,
    
    // 分析機能
    analyzeCompetitors,
    predictTrends,
    performComprehensiveAnalysis,
    
    // システム管理
    checkSystemHealth,
    getUsageStats,
    clearErrors,
    clearResults
  };
};

export default useAIEngine;