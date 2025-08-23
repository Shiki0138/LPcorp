/**
 * Jest設定 - 史上最高システム品質検証用
 * 統合・セキュリティ・パフォーマンス・負荷テスト統合設定
 */

module.exports = {
  // テスト環境設定
  testEnvironment: 'node',
  
  // TypeScript対応
  preset: 'ts-jest',
  
  // テストファイルパターン
  testMatch: [
    '<rootDir>/tests/**/*.test.{ts,tsx,js,jsx}',
    '<rootDir>/src/**/__tests__/**/*.{ts,tsx,js,jsx}',
    '<rootDir>/src/**/*.{test,spec}.{ts,tsx,js,jsx}'
  ],
  
  // セットアップファイル
  setupFilesAfterEnv: [
    '<rootDir>/tests/setup.ts'
  ],
  
  // カバレッジ設定
  collectCoverage: true,
  collectCoverageFrom: [
    'src/**/*.{ts,tsx}',
    '!src/**/*.d.ts',
    '!src/**/index.ts',
    '!src/**/*.stories.{ts,tsx}',
    '!src/**/__tests__/**/*',
    '!src/**/node_modules/**'
  ],
  
  // カバレッジ閾値（史上最高品質基準）
  coverageThreshold: {
    global: {
      branches: 80,
      functions: 85,
      lines: 85,
      statements: 85
    },
    // 重要ファイルは更に高い基準
    './src/lib/ai/': {
      branches: 90,
      functions: 95,
      lines: 95,
      statements: 95
    },
    './src/hooks/': {
      branches: 85,
      functions: 90,
      lines: 90,
      statements: 90
    }
  },
  
  // カバレッジレポート形式
  coverageReporters: [
    'text',
    'text-summary',
    'html',
    'lcov',
    'json',
    'clover'
  ],
  
  // カバレッジ出力ディレクトリ
  coverageDirectory: '<rootDir>/coverage',
  
  // モジュール解決
  moduleNameMapping: {
    '^@/(.*)$': '<rootDir>/src/$1',
    '^@/tests/(.*)$': '<rootDir>/tests/$1'
  },
  
  // 変換対象外
  transformIgnorePatterns: [
    'node_modules/(?!(.*\\.mjs$))'
  ],
  
  // グローバル設定
  globals: {
    'ts-jest': {
      useESM: true,
      tsconfig: {
        jsx: 'react-jsx',
        esModuleInterop: true,
        allowSyntheticDefaultImports: true
      }
    }
  },
  
  // テストタイムアウト（負荷テスト対応）
  testTimeout: 30000, // 30秒
  
  // 並行実行設定
  maxWorkers: '50%', // CPU使用率50%
  
  // キャッシュ設定
  cache: true,
  cacheDirectory: '<rootDir>/.jest-cache',
  
  // 詳細レポート
  verbose: true,
  
  // カラー出力
  colors: true,
  
  // エラー検出設定
  errorOnDeprecated: true,
  
  // 監視モード設定
  watchman: true,
  
  // テストシーケンサー（並行実行制御）
  testSequencer: '<rootDir>/tests/sequencer.js',
  
  // レポーター設定
  reporters: [
    'default',
    [
      'jest-html-reporters',
      {
        publicPath: '<rootDir>/test-reports',
        filename: 'test-report.html',
        expand: true,
        hideIcon: false,
        pageTitle: 'Supreme QA Test Report',
        logoImgPath: undefined,
        includeFailureMsg: true,
        includeSuiteFailure: true
      }
    ],
    [
      'jest-junit',
      {
        outputDirectory: '<rootDir>/test-reports',
        outputName: 'junit.xml',
        ancestorSeparator: ' › ',
        uniqueOutputName: 'false',
        suiteNameTemplate: '{filepath}',
        classNameTemplate: '{classname}',
        titleTemplate: '{title}'
      }
    ]
  ],
  
  // モック設定
  clearMocks: true,
  restoreMocks: true,
  
  // プロジェクト設定（マルチプロジェクト対応）
  projects: [
    {
      displayName: 'unit',
      testMatch: ['<rootDir>/src/**/*.test.{ts,tsx}'],
      testEnvironment: 'jsdom',
      setupFilesAfterEnv: ['<rootDir>/tests/unit-setup.ts']
    },
    {
      displayName: 'integration',
      testMatch: ['<rootDir>/tests/integration/**/*.test.{ts,tsx}'],
      testEnvironment: 'node',
      testTimeout: 120000, // 2分
      setupFilesAfterEnv: ['<rootDir>/tests/integration-setup.ts']
    },
    {
      displayName: 'security',
      testMatch: ['<rootDir>/tests/security/**/*.test.{ts,tsx}'],
      testEnvironment: 'node',
      testTimeout: 60000, // 1分
      setupFilesAfterEnv: ['<rootDir>/tests/security-setup.ts']
    },
    {
      displayName: 'performance',
      testMatch: ['<rootDir>/tests/performance/**/*.test.{ts,tsx}'],
      testEnvironment: 'node',
      testTimeout: 900000, // 15分（負荷テスト対応）
      setupFilesAfterEnv: ['<rootDir>/tests/performance-setup.ts'],
      maxWorkers: 1 // パフォーマンステストは単一プロセス
    }
  ]
};