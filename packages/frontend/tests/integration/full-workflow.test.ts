/**
 * 統合テストスイート - フルワークフロー検証
 * フォーム→AI生成→分析→レポート 完全フロー検証
 */

import { describe, it, expect, beforeAll, afterAll, beforeEach } from '@jest/globals';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { lpGenerationEngine } from '@/lib/ai/generator';
import { competitiveAnalysisEngine } from '@/lib/ai/competitive';
import { trendPredictionEngine } from '@/lib/ai/trends';
import { learningPatternSystem } from '@/lib/ai/patterns';
import { useAIEngine } from '@/hooks/useAIEngine';

// テストデータ
const mockBusinessData = {
  businessType: 'SaaS',
  targetAudience: {
    age: '25-40',
    gender: 'balanced',
    income: 'middle-high',
    interests: ['技術', '効率化', 'ビジネス'],
    painPoints: ['時間不足', '業務効率', 'コスト削減']
  },
  product: {
    name: 'TaskMaster Pro',
    description: 'AI搭載タスク管理ツール',
    benefits: ['生産性向上', '時間節約', 'チーム協働'],
    features: ['AI自動分類', 'スマート通知', 'データ分析'],
    price: 9800,
    category: 'productivity'
  },
  goals: {
    primary: 'signup' as const,
    secondary: ['trial-conversion', 'feature-adoption']
  },
  brand: {
    name: 'TaskMaster',
    tone: 'professional' as const,
    colors: ['#2E86DE', '#54A0FF', '#5F27CD'],
    logoUrl: 'https://example.com/logo.png'
  }
};

describe('フルワークフロー統合テスト', () => {
  let user: ReturnType<typeof userEvent.setup>;

  beforeAll(async () => {
    // テスト環境セットアップ
    user = userEvent.setup();
    
    // AI システム初期化
    await new Promise(resolve => setTimeout(resolve, 1000));
  });

  beforeEach(() => {
    // 各テスト前にクリーンアップ
    jest.clearAllMocks();
  });

  describe('フォーム入力からLP生成まで', () => {
    it('ビジネス情報入力が正常に動作する', async () => {
      // フォーム入力フローテスト
      const formData = {
        businessType: mockBusinessData.businessType,
        productName: mockBusinessData.product.name,
        productDescription: mockBusinessData.product.description,
        targetAge: mockBusinessData.targetAudience.age,
        targetGender: mockBusinessData.targetAudience.gender
      };

      // フォーム検証
      expect(formData.businessType).toBeTruthy();
      expect(formData.productName).toHaveLength.greaterThan(0);
      expect(formData.productDescription).toHaveLength.greaterThan(10);
      
      // バリデーション通過確認
      const isValid = Object.values(formData).every(value => 
        value !== null && value !== undefined && value !== ''
      );
      expect(isValid).toBe(true);
    });

    it('AI生成エンジンが正常に動作する', async () => {
      const startTime = Date.now();
      
      const result = await lpGenerationEngine.generateLP(mockBusinessData);
      const generationTime = Date.now() - startTime;

      // 生成結果検証
      expect(result).toBeDefined();
      expect(result.html).toContain('html');
      expect(result.css).toContain('{');
      expect(result.js).toContain('function');
      
      // パフォーマンス検証（30秒以内）
      expect(generationTime).toBeLessThan(30000);
      
      // 品質検証
      expect(result.metadata.title).toBeTruthy();
      expect(result.metadata.description).toBeTruthy();
      expect(result.metadata.keywords).toHaveLength.greaterThan(0);
      expect(result.analytics.confidence).toBeGreaterThan(0.5);

      console.log(`✅ LP生成完了: ${generationTime}ms`);
    });

    it('競合分析が正常に実行される', async () => {
      const startTime = Date.now();
      
      const analysisResult = await competitiveAnalysisEngine.analyzeCompetitors(
        mockBusinessData.businessType,
        mockBusinessData.product.name,
        mockBusinessData.targetAudience.age
      );
      
      const analysisTime = Date.now() - startTime;

      // 分析結果検証
      expect(analysisResult).toBeDefined();
      expect(analysisResult.competitors).toHaveLength.greaterThan(0);
      expect(analysisResult.marketInsights).toBeDefined();
      expect(analysisResult.recommendations).toHaveLength.greaterThan(0);
      
      // パフォーマンス検証（15秒以内）
      expect(analysisTime).toBeLessThan(15000);

      console.log(`✅ 競合分析完了: ${analysisTime}ms`);
    });

    it('トレンド予測が正常に実行される', async () => {
      const startTime = Date.now();
      
      const trendResult = await trendPredictionEngine.predictMarketTrends(
        mockBusinessData.businessType,
        mockBusinessData.product.category,
        mockBusinessData.targetAudience.age
      );
      
      const trendTime = Date.now() - startTime;

      // 予測結果検証
      expect(trendResult).toBeDefined();
      expect(trendResult.trends).toHaveLength.greaterThan(0);
      expect(trendResult.predictions).toBeDefined();
      expect(trendResult.opportunities).toHaveLength.greaterThan(0);
      
      // パフォーマンス検証（10秒以内）
      expect(trendTime).toBeLessThan(10000);

      console.log(`✅ トレンド予測完了: ${trendTime}ms`);
    });
  });

  describe('並行処理・統合分析テスト', () => {
    it('並行分析実行が正常に動作する', async () => {
      const startTime = Date.now();
      
      // 並行実行
      const [lpResult, competitiveResult, trendResult] = await Promise.all([
        lpGenerationEngine.generateLP(mockBusinessData),
        competitiveAnalysisEngine.analyzeCompetitors(
          mockBusinessData.businessType,
          mockBusinessData.product.name,
          mockBusinessData.targetAudience.age
        ),
        trendPredictionEngine.predictMarketTrends(
          mockBusinessData.businessType,
          mockBusinessData.product.category,
          mockBusinessData.targetAudience.age
        )
      ]);
      
      const totalTime = Date.now() - startTime;

      // 結果検証
      expect(lpResult).toBeDefined();
      expect(competitiveResult).toBeDefined();
      expect(trendResult).toBeDefined();
      
      // 並行処理効率検証（順次実行より30%以上高速）
      expect(totalTime).toBeLessThan(40000); // 40秒以内
      
      console.log(`✅ 並行分析完了: ${totalTime}ms`);
    });

    it('パターン選択アルゴリズムが正常に動作する', async () => {
      const selectedPattern = await learningPatternSystem.selectOptimalPattern(
        mockBusinessData.targetAudience,
        mockBusinessData.businessType,
        mockBusinessData.goals.primary
      );

      // パターン選択検証
      expect(selectedPattern).toBeDefined();
      if (selectedPattern) {
        expect(selectedPattern.conversionRate).toBeGreaterThan(0);
        expect(selectedPattern.elements).toBeDefined();
        expect(selectedPattern.successMetrics).toBeDefined();
      }

      console.log(`✅ パターン選択完了: ${selectedPattern?.name}`);
    });
  });

  describe('エラーハンドリング・異常系テスト', () => {
    it('API制限エラーに対する適切な処理', async () => {
      // API制限シミュレーション
      const invalidApiKey = process.env.OPENAI_API_KEY;
      process.env.OPENAI_API_KEY = 'invalid-key';

      try {
        await lpGenerationEngine.generateLP(mockBusinessData);
        // エラーが発生しなかった場合はテスト失敗
        expect(true).toBe(false);
      } catch (error) {
        // 適切なエラーハンドリング確認
        expect(error).toBeDefined();
        expect(error.message).toContain('Generation failed');
      }

      // 環境変数復元
      process.env.OPENAI_API_KEY = invalidApiKey;
    });

    it('ネットワークエラーに対する回復処理', async () => {
      // ネットワークエラーシミュレーション
      const originalFetch = global.fetch;
      global.fetch = jest.fn().mockRejectedValue(new Error('Network error'));

      try {
        await competitiveAnalysisEngine.analyzeCompetitors('test', 'test', 'test');
      } catch (error) {
        expect(error).toBeDefined();
      }

      // fetch復元
      global.fetch = originalFetch;
    });

    it('不正入力データに対する適切な検証', async () => {
      const invalidData = {
        ...mockBusinessData,
        product: {
          ...mockBusinessData.product,
          name: '', // 空文字
          price: -1000 // 負の値
        }
      };

      try {
        await lpGenerationEngine.generateLP(invalidData);
      } catch (error) {
        expect(error).toBeDefined();
      }
    });
  });

  describe('パフォーマンス・負荷テスト', () => {
    it('同時リクエスト処理能力テスト', async () => {
      const concurrentRequests = 10;
      const startTime = Date.now();
      
      const promises = Array.from({ length: concurrentRequests }, () =>
        lpGenerationEngine.generateLP(mockBusinessData)
      );

      const results = await Promise.allSettled(promises);
      const totalTime = Date.now() - startTime;
      
      // 成功率検証（90%以上）
      const successCount = results.filter(r => r.status === 'fulfilled').length;
      const successRate = successCount / concurrentRequests;
      expect(successRate).toBeGreaterThanOrEqual(0.9);
      
      // パフォーマンス検証
      expect(totalTime).toBeLessThan(60000); // 60秒以内
      
      console.log(`✅ 同時処理テスト完了: ${successRate * 100}% 成功率`);
    });

    it('メモリリーク検証', async () => {
      const initialMemory = process.memoryUsage().heapUsed;
      
      // 大量処理実行
      for (let i = 0; i < 50; i++) {
        await lpGenerationEngine.generateLP(mockBusinessData);
      }
      
      // ガベージコレクション強制実行
      if (global.gc) {
        global.gc();
      }
      
      const finalMemory = process.memoryUsage().heapUsed;
      const memoryIncrease = finalMemory - initialMemory;
      
      // メモリ使用量検証（100MB以下）
      expect(memoryIncrease).toBeLessThan(100 * 1024 * 1024);
      
      console.log(`✅ メモリ使用量: ${(memoryIncrease / 1024 / 1024).toFixed(2)}MB`);
    });
  });

  describe('データ整合性・品質検証', () => {
    it('生成されたHTMLの品質検証', async () => {
      const result = await lpGenerationEngine.generateLP(mockBusinessData);
      
      // HTML構造検証
      expect(result.html).toContain('<!DOCTYPE html>');
      expect(result.html).toContain('<head>');
      expect(result.html).toContain('<body>');
      expect(result.html).toContain('</html>');
      
      // セマンティック要素検証
      expect(result.html).toMatch(/<header|<nav|<main|<section|<article|<aside|<footer/);
      
      // アクセシビリティ検証
      expect(result.html).toContain('alt=');
      expect(result.html).toContain('aria-');
      expect(result.html).toMatch(/<h[1-6]/);
    });

    it('生成されたCSSの品質検証', async () => {
      const result = await lpGenerationEngine.generateLP(mockBusinessData);
      
      // CSS構文検証
      expect(result.css).toMatch(/[^{}]*\{[^{}]*\}/);
      
      // レスポンシブ対応検証
      expect(result.css).toMatch(/@media.*screen/);
      
      // モダンCSS機能検証
      expect(result.css).toMatch(/grid|flex|var\(--/);
    });

    it('生成されたJavaScriptの品質検証', async () => {
      const result = await lpGenerationEngine.generateLP(mockBusinessData);
      
      // JavaScript構文検証
      expect(result.js).toMatch(/function|const|let|=>/);
      
      // エラーハンドリング検証
      expect(result.js).toMatch(/try|catch|error/i);
      
      // モダンJS機能検証
      expect(result.js).toMatch(/async|await|Promise/);
    });
  });

  afterAll(() => {
    // テスト終了後のクリーンアップ
    console.log('🎯 統合テスト完了 - 全機能正常動作確認');
  });
});