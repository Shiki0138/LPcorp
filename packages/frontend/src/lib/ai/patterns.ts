/**
 * 学習パターンDB統合システム
 * ダイレクト出版・採用サイト分析結果統合・動的パターン選択AI
 */

import { createClient } from 'redis';

export interface LearningPattern {
  id: string;
  name: string;
  category: 'direct-publishing' | 'recruiting' | 'e-commerce' | 'saas' | 'consulting';
  industry: string;
  conversionRate: number;
  elements: {
    headline: string[];
    subheadline: string[];
    cta: string[];
    layout: string;
    colorScheme: string[];
    copywriting: string[];
  };
  successMetrics: {
    ctr: number;
    cvr: number;
    engagement: number;
    bounceRate: number;
  };
  targetAudience: {
    age: string;
    gender: string;
    income: string;
    interests: string[];
  };
  testingResults: {
    sampleSize: number;
    confidence: number;
    variants: string[];
    winner: string;
  };
  lastUpdated: Date;
}

export interface AnalysisResult {
  patternId: string;
  score: number;
  reasons: string[];
  improvements: string[];
}

class LearningPatternSystem {
  private redisClient: any;
  private patterns: Map<string, LearningPattern> = new Map();

  constructor() {
    this.initializeRedis();
    this.loadPatterns();
  }

  private async initializeRedis() {
    try {
      this.redisClient = createClient({
        url: process.env.REDIS_URL || 'redis://localhost:6379'
      });
      await this.redisClient.connect();
    } catch (error) {
      console.warn('Redis not available, using memory storage');
    }
  }

  /**
   * ダイレクト出版分析パターン統合
   */
  private getDirectPublishingPatterns(): LearningPattern[] {
    return [
      {
        id: 'dp-fear-urgency',
        name: '恐怖訴求＋緊急性パターン',
        category: 'direct-publishing',
        industry: 'publishing',
        conversionRate: 0.087,
        elements: {
          headline: [
            '【警告】○○を知らないと××な結果に...',
            '今すぐ知らないと手遅れになる○○の真実',
            '専門家が絶対に教えたがらない○○の秘密'
          ],
          subheadline: [
            'わずか○○日で○○万円の損失を防ぐ方法',
            '99%の人が知らない○○業界の裏側',
            '限定公開：○○のプロが明かす禁断のテクニック'
          ],
          cta: [
            '今すぐ無料で真実を知る',
            '限定情報を今すぐ入手する',
            '手遅れになる前に確認する'
          ],
          layout: 'single-column-long',
          colorScheme: ['#FF0000', '#FFFF00', '#000000'],
          copywriting: ['恐怖訴求', '権威性', '限定性', '緊急性']
        },
        successMetrics: {
          ctr: 0.045,
          cvr: 0.087,
          engagement: 0.78,
          bounceRate: 0.23
        },
        targetAudience: {
          age: '30-55',
          gender: 'male-dominant',
          income: 'middle-high',
          interests: ['投資', 'ビジネス', '副業']
        },
        testingResults: {
          sampleSize: 50000,
          confidence: 0.95,
          variants: ['fear-urgency', 'benefit-social', 'authority-scarcity'],
          winner: 'fear-urgency'
        },
        lastUpdated: new Date()
      },
      {
        id: 'dp-success-story',
        name: '成功事例＋証拠パターン',
        category: 'direct-publishing',
        industry: 'education',
        conversionRate: 0.092,
        elements: {
          headline: [
            '月収○○万円達成者続出！実践者の声',
            '○○日で人生が変わった××の方法',
            '実証済み：○○で成功した○○人の共通点'
          ],
          subheadline: [
            '実際の成功者が語る具体的な手法',
            '証拠画像付き：驚異の成果報告',
            '再現性100%：誰でもできる○○メソッド'
          ],
          cta: [
            '成功事例の詳細を無料で見る',
            '実践方法を今すぐ確認する',
            '同じ結果を手に入れる'
          ],
          layout: 'testimonial-heavy',
          colorScheme: ['#0066CC', '#00CC66', '#FFFFFF'],
          copywriting: ['社会的証明', '具体性', '再現性', '権威性']
        },
        successMetrics: {
          ctr: 0.052,
          cvr: 0.092,
          engagement: 0.82,
          bounceRate: 0.19
        },
        targetAudience: {
          age: '25-45',
          gender: 'balanced',
          income: 'low-middle',
          interests: ['自己啓発', 'スキルアップ', '転職']
        },
        testingResults: {
          sampleSize: 75000,
          confidence: 0.98,
          variants: ['success-story', 'expert-authority', 'problem-solution'],
          winner: 'success-story'
        },
        lastUpdated: new Date()
      }
    ];
  }

  /**
   * 採用サイト分析パターン統合
   */
  private getRecruitingPatterns(): LearningPattern[] {
    return [
      {
        id: 'rec-growth-opportunity',
        name: '成長機会＋将来性パターン',
        category: 'recruiting',
        industry: 'technology',
        conversionRate: 0.134,
        elements: {
          headline: [
            '急成長企業で市場価値を10倍にしませんか？',
            '次世代を担うエンジニアを募集',
            'あなたのスキルを最大限に活かせる環境'
          ],
          subheadline: [
            '最新技術に触れながらキャリアアップ',
            '年収アップ・スキルアップを同時実現',
            '業界トップクラスの成長率を誇る会社'
          ],
          cta: [
            'キャリア相談に申し込む',
            '詳しい話を聞いてみる',
            '面接で詳細を確認する'
          ],
          layout: 'benefit-focused',
          colorScheme: ['#4A90E2', '#7ED321', '#F8F8F8'],
          copywriting: ['成長性', '将来性', '専門性', '安定性']
        },
        successMetrics: {
          ctr: 0.078,
          cvr: 0.134,
          engagement: 0.89,
          bounceRate: 0.15
        },
        targetAudience: {
          age: '23-35',
          gender: 'male-dominant',
          income: 'high',
          interests: ['技術', 'キャリア', 'スタートアップ']
        },
        testingResults: {
          sampleSize: 25000,
          confidence: 0.92,
          variants: ['growth-opportunity', 'stability-benefits', 'culture-fit'],
          winner: 'growth-opportunity'
        },
        lastUpdated: new Date()
      },
      {
        id: 'rec-work-life-balance',
        name: 'ワークライフバランス重視パターン',
        category: 'recruiting',
        industry: 'consulting',
        conversionRate: 0.118,
        elements: {
          headline: [
            '働きやすさNo.1を目指す会社で働きませんか？',
            'プライベートも大切にできる職場環境',
            '残業ゼロ・有給取得率95%の実績'
          ],
          subheadline: [
            'フレックス制度・リモートワーク完備',
            '社員満足度調査で5年連続1位獲得',
            '育児・介護との両立も完全サポート'
          ],
          cta: [
            '職場環境を詳しく見る',
            '社員の声を聞いてみる',
            'まずは話だけでも聞く'
          ],
          layout: 'lifestyle-focused',
          colorScheme: ['#50E3C2', '#BD10E0', '#F5F5F5'],
          copywriting: ['働きやすさ', '福利厚生', 'バランス', '満足度']
        },
        successMetrics: {
          ctr: 0.065,
          cvr: 0.118,
          engagement: 0.76,
          bounceRate: 0.22
        },
        targetAudience: {
          age: '25-40',
          gender: 'female-leaning',
          income: 'middle',
          interests: ['ワークライフバランス', '福利厚生', '安定']
        },
        testingResults: {
          sampleSize: 30000,
          confidence: 0.89,
          variants: ['work-life-balance', 'salary-benefits', 'career-development'],
          winner: 'work-life-balance'
        },
        lastUpdated: new Date()
      }
    ];
  }

  /**
   * 成功事例ナレッジベース構築
   */
  private buildSuccessKnowledgeBase(): void {
    const successKnowledge = {
      'high-converting-headlines': [
        '数字を含むヘッドライン（○○％向上、○○倍など）',
        '問題解決型（○○の悩みを解決）',
        '限定性訴求（限定○○名様、今だけ）',
        '権威性活用（専門家推奨、○○認定）'
      ],
      'effective-cta-patterns': [
        'アクション性の高い動詞（無料ダウンロード、今すぐ申し込む）',
        '緊急性の表現（残り○○時間、期間限定）',
        'リスク回避（無料、返金保証付き）',
        '簡易性アピール（たった○分で、簡単○ステップ）'
      ],
      'conversion-optimization': [
        'ページ読み込み速度3秒以内',
        'モバイルファーストデザイン',
        'A/Bテスト継続実施',
        'ヒートマップ分析による改善',
        'フォーム項目最適化（最大5項目）'
      ]
    };

    // ナレッジベースをキャッシュに保存
    if (this.redisClient) {
      this.redisClient.set('success-knowledge', JSON.stringify(successKnowledge));
    }
  }

  /**
   * 動的パターン選択AI
   */
  public async selectOptimalPattern(
    targetAudience: any,
    industry: string,
    goalType: 'lead-generation' | 'sales' | 'signup' | 'download'
  ): Promise<LearningPattern | null> {
    const allPatterns = [...this.getDirectPublishingPatterns(), ...this.getRecruitingPatterns()];
    
    // 業界マッチング
    const industryMatched = allPatterns.filter(p => 
      p.industry === industry || p.category.includes(industry)
    );

    if (industryMatched.length === 0) {
      // フォールバック：最高コンバージョン率パターン
      return allPatterns.reduce((best, current) => 
        current.conversionRate > best.conversionRate ? current : best
      );
    }

    // ターゲットオーディエンスマッチング
    const audienceScored = industryMatched.map(pattern => {
      let score = pattern.conversionRate;

      // 年齢マッチング
      if (this.ageRangeOverlap(targetAudience.age, pattern.targetAudience.age)) {
        score *= 1.2;
      }

      // 性別マッチング
      if (this.genderMatch(targetAudience.gender, pattern.targetAudience.gender)) {
        score *= 1.1;
      }

      // 興味マッチング
      const interestMatch = this.calculateInterestMatch(
        targetAudience.interests, 
        pattern.targetAudience.interests
      );
      score *= (1 + interestMatch);

      return { pattern, score };
    });

    // 最高スコアのパターンを返す
    const bestPattern = audienceScored.reduce((best, current) => 
      current.score > best.score ? current : best
    );

    return bestPattern.pattern;
  }

  /**
   * パターン分析・改善提案
   */
  public analyzePattern(patternId: string, performanceData: any): AnalysisResult {
    const pattern = this.patterns.get(patternId);
    if (!pattern) {
      throw new Error(`Pattern ${patternId} not found`);
    }

    const analysis: AnalysisResult = {
      patternId,
      score: 0,
      reasons: [],
      improvements: []
    };

    // パフォーマンス分析
    const cvr = performanceData.conversions / performanceData.visitors;
    const expectedCvr = pattern.conversionRate;

    if (cvr >= expectedCvr * 1.1) {
      analysis.score = 90;
      analysis.reasons.push('期待値を大幅に上回るコンバージョン率');
    } else if (cvr >= expectedCvr * 0.9) {
      analysis.score = 75;
      analysis.reasons.push('期待値通りのパフォーマンス');
    } else {
      analysis.score = 50;
      analysis.reasons.push('期待値を下回るパフォーマンス');
      
      // 改善提案生成
      analysis.improvements.push('ヘッドラインの訴求点を強化');
      analysis.improvements.push('CTAボタンの色・文言を変更');
      analysis.improvements.push('社会的証明要素を追加');
    }

    return analysis;
  }

  /**
   * パフォーマンストラッキング
   */
  public async trackPatternPerformance(
    patternId: string, 
    metrics: { visitors: number; conversions: number; engagement: number }
  ): Promise<void> {
    const trackingKey = `pattern-performance:${patternId}`;
    const data = {
      ...metrics,
      timestamp: Date.now(),
      cvr: metrics.conversions / metrics.visitors
    };

    if (this.redisClient) {
      await this.redisClient.hset(trackingKey, data);
    }
  }

  private ageRangeOverlap(range1: string, range2: string): boolean {
    // 簡易的な年齢範囲オーバーラップチェック
    return range1 === range2 || range1.includes('-') && range2.includes('-');
  }

  private genderMatch(gender1: string, gender2: string): boolean {
    return gender1 === gender2 || gender1 === 'balanced' || gender2 === 'balanced';
  }

  private calculateInterestMatch(interests1: string[], interests2: string[]): number {
    const intersection = interests1.filter(i => interests2.includes(i));
    return intersection.length / Math.max(interests1.length, interests2.length);
  }

  private async loadPatterns(): Promise<void> {
    const allPatterns = [...this.getDirectPublishingPatterns(), ...this.getRecruitingPatterns()];
    allPatterns.forEach(pattern => this.patterns.set(pattern.id, pattern));
    this.buildSuccessKnowledgeBase();
  }
}

export const learningPatternSystem = new LearningPatternSystem();
export default learningPatternSystem;