/**
 * Jest セットアップファイル
 * 史上最高システム品質検証環境構築
 */

import { jest } from '@jest/globals';

// グローバル設定
global.console = {
  ...console,
  // テスト中のログ制御
  log: jest.fn(),
  debug: jest.fn(),
  info: jest.fn(),
  warn: jest.fn(),
  error: jest.fn(),
};

// 環境変数設定
process.env.NODE_ENV = 'test';
process.env.LOG_LEVEL = 'error';
process.env.OPENAI_API_KEY = 'test-key-mock';
process.env.REDIS_URL = 'redis://localhost:6379';

// テストタイムアウトのグローバル設定
jest.setTimeout(30000);

// 未ハンドルPromise拒否の処理
process.on('unhandledRejection', (reason, promise) => {
  console.error('Unhandled Rejection at:', promise, 'reason:', reason);
  // テスト環境では終了させる
  process.exit(1);
});

// 未キャッチ例外の処理
process.on('uncaughtException', (error) => {
  console.error('Uncaught Exception:', error);
  process.exit(1);
});

// パフォーマンス測定用グローバル
global.performance = performance;

// メモリ使用量監視
const originalMemoryUsage = process.memoryUsage;
global.trackMemory = () => {
  const usage = originalMemoryUsage.call(process);
  return {
    heapUsed: usage.heapUsed / 1024 / 1024, // MB
    heapTotal: usage.heapTotal / 1024 / 1024,
    external: usage.external / 1024 / 1024,
    rss: usage.rss / 1024 / 1024
  };
};

// テストケース共通ユーティリティ
global.testUtils = {
  // 非同期待機ヘルパー
  wait: (ms: number) => new Promise(resolve => setTimeout(resolve, ms)),
  
  // レスポンス時間測定
  measureTime: async (fn: () => Promise<any>) => {
    const start = performance.now();
    const result = await fn();
    const end = performance.now();
    return { result, time: end - start };
  },
  
  // ランダムデータ生成
  generateTestData: (type: 'business' | 'user' | 'product') => {
    switch (type) {
      case 'business':
        return {
          businessType: 'SaaS',
          targetAudience: {
            age: '25-40',
            gender: 'balanced',
            income: 'middle-high',
            interests: ['技術', '効率化'],
            painPoints: ['時間不足', '業務効率']
          }
        };
      case 'user':
        return {
          id: Math.random().toString(36).substr(2, 9),
          email: `test${Date.now()}@example.com`,
          name: 'テストユーザー'
        };
      case 'product':
        return {
          name: 'テスト商品',
          description: 'テスト用商品説明',
          price: 9800,
          category: 'software'
        };
      default:
        return {};
    }
  },
  
  // エラーシミュレーション
  simulateError: (type: 'network' | 'api' | 'timeout' | 'server') => {
    switch (type) {
      case 'network':
        throw new Error('Network Error: Unable to connect');
      case 'api':
        throw new Error('API Error: Invalid response');
      case 'timeout':
        throw new Error('Timeout Error: Request timed out');
      case 'server':
        throw new Error('Server Error: Internal server error');
      default:
        throw new Error('Unknown error');
    }
  }
};

// モック設定
global.mockFetch = (responseData: any, delay: number = 100) => {
  global.fetch = jest.fn().mockImplementation(() =>
    new Promise(resolve => 
      setTimeout(() => resolve({
        ok: true,
        status: 200,
        json: () => Promise.resolve(responseData),
        text: () => Promise.resolve(JSON.stringify(responseData))
      }), delay)
    )
  );
};

// テスト開始時の初期化
beforeAll(() => {
  console.log('🚀 Supreme QA Testing Environment Initialized');
  console.log(`📊 Memory: ${JSON.stringify(global.trackMemory(), null, 2)}`);
});

// 各テスト後のクリーンアップ
afterEach(() => {
  // モッククリア
  jest.clearAllMocks();
  
  // メモリリーク検出
  if (global.gc) {
    global.gc();
  }
});

// 全テスト終了時
afterAll(() => {
  console.log('✅ All tests completed');
  console.log(`📊 Final Memory: ${JSON.stringify(global.trackMemory(), null, 2)}`);
});