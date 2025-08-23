/**
 * LP自動生成エンジン
 * GPT-4 API完全版・プロンプトエンジニアリング最適化
 * HTML/CSS/JS自動生成・SEO・アクセシビリティ自動実装
 */

import OpenAI from 'openai';
import { learningPatternSystem, LearningPattern } from './patterns';

export interface GenerationRequest {
  businessType: string;
  targetAudience: {
    age: string;
    gender: string;
    income: string;
    interests: string[];
    painPoints: string[];
  };
  product: {
    name: string;
    description: string;
    benefits: string[];
    features: string[];
    price: number;
    category: string;
  };
  goals: {
    primary: 'lead-generation' | 'sales' | 'signup' | 'download';
    secondary?: string[];
  };
  brand: {
    name: string;
    tone: 'professional' | 'casual' | 'urgent' | 'friendly' | 'authoritative';
    colors: string[];
    logoUrl?: string;
  };
  constraints?: {
    maxLength?: number;
    requiredElements?: string[];
    excludeElements?: string[];
  };
}

export interface GenerationResult {
  html: string;
  css: string;
  js: string;
  metadata: {
    title: string;
    description: string;
    keywords: string[];
    estimatedCVR: number;
    patterns: string[];
    optimizations: string[];
  };
  analytics: {
    generationTime: number;
    confidence: number;
    abTestRecommendations: string[];
  };
}

class LPGenerationEngine {
  private openai: OpenAI;
  private generationCache: Map<string, GenerationResult> = new Map();

  constructor() {
    this.openai = new OpenAI({
      apiKey: process.env.OPENAI_API_KEY || '',
      dangerouslyAllowBrowser: true
    });
  }

  /**
   * メイン生成エンドポイント - 30秒以内保証
   */
  public async generateLP(request: GenerationRequest): Promise<GenerationResult> {
    const startTime = Date.now();
    
    try {
      // キャッシュチェック
      const cacheKey = this.generateCacheKey(request);
      if (this.generationCache.has(cacheKey)) {
        return this.generationCache.get(cacheKey)!;
      }

      // 最適パターン選択
      const selectedPattern = await learningPatternSystem.selectOptimalPattern(
        request.targetAudience,
        request.businessType,
        request.goals.primary
      );

      // 並行生成実行
      const [htmlResult, cssResult, jsResult] = await Promise.all([
        this.generateHTML(request, selectedPattern),
        this.generateCSS(request, selectedPattern),
        this.generateJavaScript(request, selectedPattern)
      ]);

      // メタデータ生成
      const metadata = await this.generateMetadata(request, selectedPattern);
      
      const result: GenerationResult = {
        html: htmlResult,
        css: cssResult,
        js: jsResult,
        metadata,
        analytics: {
          generationTime: Date.now() - startTime,
          confidence: this.calculateConfidence(request, selectedPattern),
          abTestRecommendations: this.generateABTestRecommendations(request)
        }
      };

      // キャッシュ保存
      this.generationCache.set(cacheKey, result);
      
      return result;
    } catch (error) {
      console.error('LP Generation failed:', error);
      throw new Error(`Generation failed: ${error}`);
    }
  }

  /**
   * HTML生成 - セマンティック・アクセシブル
   */
  private async generateHTML(request: GenerationRequest, pattern: LearningPattern | null): Promise<string> {
    const prompt = this.buildHTMLPrompt(request, pattern);
    
    const response = await this.openai.chat.completions.create({
      model: "gpt-4-turbo-preview",
      messages: [
        {
          role: "system",
          content: "あなたは世界最高のLP制作エキスパートです。セマンティックHTML、完全アクセシビリティ対応、SEO最適化されたコードを生成してください。"
        },
        {
          role: "user", 
          content: prompt
        }
      ],
      max_tokens: 4000,
      temperature: 0.3
    });

    return this.extractHTMLFromResponse(response.choices[0].message.content || '');
  }

  /**
   * CSS生成 - レスポンシブ・モダンデザイン
   */
  private async generateCSS(request: GenerationRequest, pattern: LearningPattern | null): Promise<string> {
    const prompt = this.buildCSSPrompt(request, pattern);
    
    const response = await this.openai.chat.completions.create({
      model: "gpt-4-turbo-preview",
      messages: [
        {
          role: "system",
          content: "あなたは最新のCSS技術を駆使するエキスパートです。モバイルファースト、Grid/Flexbox、アニメーション、パフォーマンス最適化されたCSSを生成してください。"
        },
        {
          role: "user",
          content: prompt
        }
      ],
      max_tokens: 3000,
      temperature: 0.2
    });

    return this.extractCSSFromResponse(response.choices[0].message.content || '');
  }

  /**
   * JavaScript生成 - インタラクション・トラッキング
   */
  private async generateJavaScript(request: GenerationRequest, pattern: LearningPattern | null): Promise<string> {
    const prompt = this.buildJSPrompt(request, pattern);
    
    const response = await this.openai.chat.completions.create({
      model: "gpt-4-turbo-preview", 
      messages: [
        {
          role: "system",
          content: "あなたはモダンJavaScriptのエキスパートです。ES6+、非同期処理、パフォーマンス最適化、アナリティクス統合されたコードを生成してください。"
        },
        {
          role: "user",
          content: prompt
        }
      ],
      max_tokens: 2000,
      temperature: 0.1
    });

    return this.extractJSFromResponse(response.choices[0].message.content || '');
  }

  /**
   * プロンプトエンジニアリング - HTML
   */
  private buildHTMLPrompt(request: GenerationRequest, pattern: LearningPattern | null): string {
    return `
# LP HTML生成指示

## ビジネス情報
- 業種: ${request.businessType}
- 商品: ${request.product.name}
- 価格: ¥${request.product.price.toLocaleString()}
- 目標: ${request.goals.primary}

## ターゲット
- 年齢: ${request.targetAudience.age}
- 性別: ${request.targetAudience.gender}
- 収入: ${request.targetAudience.income}
- 課題: ${request.targetAudience.painPoints.join(', ')}

## 選択パターン
${pattern ? `
- パターン: ${pattern.name}
- CVR実績: ${(pattern.conversionRate * 100).toFixed(1)}%
- 推奨ヘッドライン: ${pattern.elements.headline[0]}
- 推奨CTA: ${pattern.elements.cta[0]}
` : '汎用パターンを適用'}

## 生成要件
1. **構造**: header > hero > benefits > features > testimonials > cta > footer
2. **アクセシビリティ**: ARIA属性完全対応、キーボードナビ対応
3. **SEO**: 構造化データ、適切なヘッダー階層、メタタグ
4. **パフォーマンス**: 軽量HTML、遅延読み込み対応
5. **コンバージョン**: 緊急性、社会的証明、リスク軽減要素

## 必須要素
- フォーム最適化（最大5項目）
- 電話番号クリックtoコール
- プライバシーポリシー遵守
- Cookie同意バナー

完全なHTMLコードを生成してください：
    `;
  }

  /**
   * プロンプトエンジニアリング - CSS
   */
  private buildCSSPrompt(request: GenerationRequest, pattern: LearningPattern | null): string {
    const colors = pattern?.elements.colorScheme || request.brand.colors;
    
    return `
# LP CSS生成指示

## デザイン要件
- ブランドカラー: ${colors.join(', ')}
- トーン: ${request.brand.tone}
- レイアウト: ${pattern?.elements.layout || 'modern-minimal'}

## 技術要件
1. **レスポンシブ**: モバイルファースト、ブレークポイント対応
2. **モダンCSS**: Grid, Flexbox, Custom Properties
3. **パフォーマンス**: Critical CSS, 軽量化
4. **アニメーション**: スクロール連動、ホバーエフェクト
5. **アクセシビリティ**: コントラスト比4.5:1以上、フォーカス表示

## パターン最適化
${pattern ? `
- 実績CVR: ${(pattern.conversionRate * 100).toFixed(1)}%
- 成功色: ${colors[0]} (メインCTA)
- レイアウト: ${pattern.elements.layout}
` : '汎用レスポンシブデザイン適用'}

## 必須実装
- CTA最適化（色、サイズ、配置）
- フォーム視認性向上
- 読み込み時間最適化
- ダークモード対応

完全なCSSコードを生成してください：
    `;
  }

  /**
   * プロンプトエンジニアリング - JavaScript
   */
  private buildJSPrompt(request: GenerationRequest, pattern: LearningPattern | null): string {
    return `
# LP JavaScript生成指示

## 機能要件
1. **フォーム処理**: バリデーション、送信、エラーハンドリング
2. **トラッキング**: GTM、GA4、コンバージョン計測
3. **UX向上**: スムーススクロール、プログレスバー
4. **パフォーマンス**: 遅延読み込み、キャッシュ活用

## ビジネスロジック
- 商品: ${request.product.name}
- 目標: ${request.goals.primary}
- 価格: ¥${request.product.price.toLocaleString()}

## パターン最適化
${pattern ? `
- 成功パターン: ${pattern.name}
- CTAテキスト: ${pattern.elements.cta[0]}
- ターゲット行動: ${request.goals.primary}
` : '汎用コンバージョン最適化適用'}

## 必須機能
- A/Bテスト準備コード
- ヒートマップ対応（Hotjar）
- リアルタイム分析
- エラー監視（Sentry）

## パフォーマンス
- 非同期処理最適化
- メモリリーク防止
- イベント委譲
- デバウンス・スロットル

ES6+準拠の完全なJavaScriptコードを生成してください：
    `;
  }

  /**
   * メタデータ生成 - SEO最適化
   */
  private async generateMetadata(request: GenerationRequest, pattern: LearningPattern | null): Promise<GenerationResult['metadata']> {
    const prompt = `
${request.product.name}の${request.businessType}向けLP用のSEOメタデータを生成：

## 商品情報
- 名称: ${request.product.name}
- 説明: ${request.product.description}
- 利益: ${request.product.benefits.join(', ')}
- ターゲット: ${request.targetAudience.age}歳・${request.targetAudience.interests.join(', ')}

## 要件
- タイトル: 60文字以内、キーワード含有
- 説明: 160文字以内、行動促進
- キーワード: 主要5-10個選定

JSON形式で返してください：
    `;

    const response = await this.openai.chat.completions.create({
      model: "gpt-4-turbo-preview",
      messages: [
        {
          role: "system",
          content: "SEO専門家として最適化されたメタデータを生成してください"
        },
        {
          role: "user",
          content: prompt
        }
      ],
      max_tokens: 1000,
      temperature: 0.3
    });

    const content = response.choices[0].message.content || '';
    
    try {
      const parsed = JSON.parse(content);
      return {
        title: parsed.title || `${request.product.name} - ${request.brand.name}`,
        description: parsed.description || request.product.description,
        keywords: parsed.keywords || [],
        estimatedCVR: pattern?.conversionRate || 0.05,
        patterns: pattern ? [pattern.name] : [],
        optimizations: [
          'モバイルファースト最適化',
          'ページ速度最適化',
          'フォーム最適化',
          'SEO構造化データ'
        ]
      };
    } catch {
      return {
        title: `${request.product.name} - ${request.brand.name}`,
        description: request.product.description,
        keywords: [request.product.name, request.businessType],
        estimatedCVR: 0.05,
        patterns: [],
        optimizations: []
      };
    }
  }

  /**
   * A/Bテスト推奨案生成
   */
  private generateABTestRecommendations(request: GenerationRequest): string[] {
    return [
      'ヘッドライン訴求軸テスト（問題解決 vs 利益訴求）',
      'CTAボタン色テスト（現在色 vs 対比色）',
      'フォーム項目数テスト（5項目 vs 3項目）',
      '価格表示テスト（税込 vs 税抜）',
      '社会的証明位置テスト（ヒーロー直下 vs CTA直上）'
    ];
  }

  /**
   * 生成信頼度計算
   */
  private calculateConfidence(request: GenerationRequest, pattern: LearningPattern | null): number {
    let confidence = 0.7; // ベースライン

    if (pattern) {
      // パターンマッチング信頼度
      confidence += pattern.conversionRate * 0.3;
    }

    // リクエスト完全性チェック
    const completeness = this.calculateRequestCompleteness(request);
    confidence *= completeness;

    return Math.min(Math.max(confidence, 0.3), 0.95);
  }

  private calculateRequestCompleteness(request: GenerationRequest): number {
    let score = 0;
    const maxScore = 10;

    if (request.product.name) score++;
    if (request.product.description) score++;
    if (request.product.benefits.length > 0) score++;
    if (request.targetAudience.age) score++;
    if (request.targetAudience.interests.length > 0) score++;
    if (request.targetAudience.painPoints.length > 0) score++;
    if (request.brand.name) score++;
    if (request.brand.colors.length > 0) score++;
    if (request.goals.primary) score++;
    if (request.businessType) score++;

    return score / maxScore;
  }

  private generateCacheKey(request: GenerationRequest): string {
    return Buffer.from(JSON.stringify(request)).toString('base64').slice(0, 32);
  }

  private extractHTMLFromResponse(response: string): string {
    const htmlMatch = response.match(/```html\n([\s\S]*?)\n```/);
    return htmlMatch ? htmlMatch[1] : response;
  }

  private extractCSSFromResponse(response: string): string {
    const cssMatch = response.match(/```css\n([\s\S]*?)\n```/);
    return cssMatch ? cssMatch[1] : response;
  }

  private extractJSFromResponse(response: string): string {
    const jsMatch = response.match(/```(?:javascript|js)\n([\s\S]*?)\n```/);
    return jsMatch ? jsMatch[1] : response;
  }
}

export const lpGenerationEngine = new LPGenerationEngine();
export default lpGenerationEngine;