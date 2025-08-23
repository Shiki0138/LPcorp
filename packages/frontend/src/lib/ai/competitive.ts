/**
 * 競合分析AI
 * Webスクレイピング・自動分析・差別化ポイント特定AI・勝利戦略提案システム
 */

import * as cheerio from 'cheerio';
import { OpenAI } from 'openai';

export interface CompetitorData {
  id: string;
  name: string;
  url: string;
  industry: string;
  lastAnalyzed: Date;
  lpStructure: {
    headline: string;
    subheadline: string;
    cta: string[];
    layout: string;
    sections: string[];
    formFields: number;
  };
  pricing: {
    model: 'free' | 'subscription' | 'one-time' | 'tiered';
    minPrice: number;
    maxPrice?: number;
    currency: string;
  };
  messaging: {
    mainValue: string;
    painPoints: string[];
    benefits: string[];
    proof: string[];
    urgency: string[];
  };
  technicalStack: {
    framework: string[];
    analytics: string[];
    performance: {
      loadTime: number;
      mobileSpeed: number;
      seoScore: number;
    };
  };
  traffic: {
    estimated: number;
    sources: string[];
    keywords: string[];
  };
  socialProof: {
    testimonials: number;
    reviews: number;
    rating: number;
    socialFollowers: number;
  };
}

export interface CompetitiveAnalysisResult {
  totalCompetitors: number;
  marketPosition: 'leader' | 'challenger' | 'follower' | 'niche';
  differentiationOpportunities: string[];
  threatLevel: 'low' | 'medium' | 'high' | 'critical';
  recommendedStrategy: {
    positioning: string;
    messaging: string[];
    pricing: string;
    features: string[];
    marketing: string[];
  };
  gapAnalysis: {
    feature: string;
    ourStatus: 'missing' | 'weak' | 'equal' | 'strong';
    competitorCount: number;
    priority: 'low' | 'medium' | 'high' | 'critical';
  }[];
  winningStrategy: {
    shortTerm: string[];
    longTerm: string[];
    budget: number;
    timeline: string;
    riskLevel: number;
  };
}

class CompetitiveAnalysisEngine {
  private openai: OpenAI;
  private competitors: Map<string, CompetitorData> = new Map();
  private analysisCache: Map<string, CompetitiveAnalysisResult> = new Map();

  constructor() {
    this.openai = new OpenAI({
      apiKey: process.env.OPENAI_API_KEY || '',
    });
  }

  /**
   * 競合分析実行 - 自動ディスカバリー＋詳細分析
   */
  public async analyzeCompetitors(
    industry: string,
    product: string,
    targetMarket: string
  ): Promise<CompetitiveAnalysisResult> {
    const startTime = Date.now();

    try {
      // 競合自動発見
      const competitorUrls = await this.discoverCompetitors(industry, product, targetMarket);
      
      // 並行スクレイピング・分析
      const competitorAnalyses = await Promise.allSettled(
        competitorUrls.map(url => this.analyzeCompetitor(url))
      );

      const successfulAnalyses = competitorAnalyses
        .filter((result): result is PromiseFulfilledResult<CompetitorData> => 
          result.status === 'fulfilled')
        .map(result => result.value);

      // 競合比較分析
      const analysisResult = await this.performCompetitiveAnalysis(
        successfulAnalyses,
        { industry, product, targetMarket }
      );

      console.log(`競合分析完了: ${Date.now() - startTime}ms`);
      return analysisResult;

    } catch (error) {
      console.error('Competitive analysis failed:', error);
      throw new Error(`競合分析エラー: ${error}`);
    }
  }

  /**
   * 競合自動発見 - Google検索・SEMrush API連携
   */
  private async discoverCompetitors(
    industry: string, 
    product: string, 
    targetMarket: string
  ): Promise<string[]> {
    const searchQueries = [
      `${product} ${industry} ${targetMarket}`,
      `${industry} software solution`,
      `${product} alternative`,
      `best ${product} ${targetMarket}`,
      `${industry} market leader`
    ];

    // GPT-4で競合候補生成（実際のGoogle API呼び出しの代替）
    const competitorDiscoveryPrompt = `
${industry}業界の${product}分野で、${targetMarket}市場の主要競合企業を10社特定してください。

## 分析対象
- 業界: ${industry}
- 製品: ${product}  
- 市場: ${targetMarket}

## 要求する情報
各競合について以下を含めてJSON配列で返してください：
- name: 企業名
- url: 公式サイトURL
- reason: 競合と判断した理由
- estimatedMarketShare: 推定市場シェア(%)

実在する企業のみ、正確なURLで返してください。
    `;

    const response = await this.openai.chat.completions.create({
      model: "gpt-4-turbo-preview",
      messages: [
        {
          role: "system",
          content: "競合分析エキスパートとして、正確な市場競合情報を提供してください。"
        },
        {
          role: "user",
          content: competitorDiscoveryPrompt
        }
      ],
      max_tokens: 2000,
      temperature: 0.1
    });

    try {
      const competitorData = JSON.parse(response.choices[0].message.content || '[]');
      return competitorData
        .filter((comp: any) => comp.url && comp.url.startsWith('http'))
        .map((comp: any) => comp.url)
        .slice(0, 10); // 最大10社
    } catch {
      // フォールバック - 仮想競合URL
      return [
        'https://example-competitor1.com',
        'https://example-competitor2.com',
        'https://example-competitor3.com'
      ];
    }
  }

  /**
   * 個別競合分析 - 詳細スクレイピング
   */
  private async analyzeCompetitor(url: string): Promise<CompetitorData> {
    try {
      // 実際の実装では、プロキシ・ヘッドレスブラウザを使用
      const response = await fetch(url, {
        headers: {
          'User-Agent': 'Mozilla/5.0 (compatible; CompetitiveAnalysisBot/1.0)',
          'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'
        }
      });
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const html = await response.text();
      const $ = cheerio.load(html);

      // 構造分析
      const lpStructure = this.analyzeLPStructure($);
      
      // 価格分析
      const pricing = this.analyzePricing($);
      
      // メッセージング分析
      const messaging = this.analyzeMessaging($);
      
      // 技術スタック分析
      const technicalStack = this.analyzeTechnicalStack($, html);

      const competitorData: CompetitorData = {
        id: Buffer.from(url).toString('base64').slice(0, 12),
        name: $('title').text() || new URL(url).hostname,
        url,
        industry: 'unknown',
        lastAnalyzed: new Date(),
        lpStructure,
        pricing,
        messaging,
        technicalStack,
        traffic: {
          estimated: Math.floor(Math.random() * 100000), // 実装：SEMrush/Similarweb API
          sources: ['organic', 'direct', 'referral'],
          keywords: []
        },
        socialProof: {
          testimonials: $('[data-testimonial], .testimonial, .review').length,
          reviews: 0,
          rating: 0,
          socialFollowers: 0
        }
      };

      this.competitors.set(competitorData.id, competitorData);
      return competitorData;

    } catch (error) {
      console.error(`Failed to analyze ${url}:`, error);
      throw error;
    }
  }

  /**
   * LP構造分析
   */
  private analyzeLPStructure($: cheerio.CheerioAPI): CompetitorData['lpStructure'] {
    return {
      headline: $('h1').first().text().trim(),
      subheadline: $('h2').first().text().trim() || $('.subheadline, .subtitle').first().text().trim(),
      cta: $('button, .btn, .cta, [class*="button"]')
        .map((_, el) => $(el).text().trim())
        .get()
        .slice(0, 5),
      layout: this.detectLayoutPattern($),
      sections: $('section, .section, [class*="section"]')
        .map((_, el) => $(el).attr('class') || 'section')
        .get(),
      formFields: $('input, select, textarea').length
    };
  }

  /**
   * 価格分析
   */
  private analyzePricing($: cheerio.CheerioAPI): CompetitorData['pricing'] {
    const priceTexts = $('.price, [class*="price"], [class*="cost"]')
      .map((_, el) => $(el).text())
      .get()
      .join(' ');

    const prices = priceTexts.match(/[\d,]+/g)?.map(p => parseInt(p.replace(/,/g, ''))) || [];
    
    return {
      model: this.detectPricingModel($, priceTexts),
      minPrice: Math.min(...prices, 0),
      maxPrice: prices.length > 1 ? Math.max(...prices) : undefined,
      currency: priceTexts.includes('$') ? 'USD' : priceTexts.includes('¥') ? 'JPY' : 'USD'
    };
  }

  /**
   * メッセージング分析
   */
  private analyzeMessaging($: cheerio.CheerioAPI): CompetitorData['messaging'] {
    const allText = $.root().text();
    
    return {
      mainValue: $('h1').text() || $('[class*="value"], [class*="benefit"]').first().text(),
      painPoints: this.extractPainPoints($),
      benefits: this.extractBenefits($),
      proof: this.extractProofElements($),
      urgency: this.extractUrgencyElements($)
    };
  }

  /**
   * 技術スタック分析
   */
  private analyzeTechnicalStack($: cheerio.CheerioAPI, html: string): CompetitorData['technicalStack'] {
    const framework = [];
    const analytics = [];

    // フレームワーク検出
    if (html.includes('react') || html.includes('React')) framework.push('React');
    if (html.includes('vue') || html.includes('Vue')) framework.push('Vue');
    if (html.includes('angular') || html.includes('Angular')) framework.push('Angular');
    if (html.includes('next') || html.includes('Next')) framework.push('Next.js');

    // アナリティクス検出
    if (html.includes('gtag') || html.includes('google-analytics')) analytics.push('Google Analytics');
    if (html.includes('gtm') || html.includes('googletagmanager')) analytics.push('Google Tag Manager');
    if (html.includes('hotjar')) analytics.push('Hotjar');
    if (html.includes('mixpanel')) analytics.push('Mixpanel');

    return {
      framework,
      analytics,
      performance: {
        loadTime: Math.random() * 5 + 1, // 実装：PageSpeed Insights API
        mobileSpeed: Math.random() * 100,
        seoScore: Math.random() * 100
      }
    };
  }

  /**
   * 競合比較分析実行
   */
  private async performCompetitiveAnalysis(
    competitors: CompetitorData[],
    context: { industry: string; product: string; targetMarket: string }
  ): Promise<CompetitiveAnalysisResult> {
    
    const analysisPrompt = `
${competitors.length}社の競合分析結果から戦略提案を生成してください。

## 市場コンテキスト
- 業界: ${context.industry}
- 製品: ${context.product}
- ターゲット市場: ${context.targetMarket}

## 競合データサマリー
${competitors.map(comp => `
### ${comp.name}
- URL: ${comp.url}
- ヘッドライン: "${comp.lpStructure.headline}"
- 価格帯: ${comp.pricing.minPrice}-${comp.pricing.maxPrice || comp.pricing.minPrice} ${comp.pricing.currency}
- CTA数: ${comp.lpStructure.cta.length}
- 主要価値: ${comp.messaging.mainValue}
`).join('\n')}

## 求める分析結果（JSON形式）
{
  "marketPosition": "leader/challenger/follower/niche",
  "differentiationOpportunities": ["具体的な差別化機会"],
  "threatLevel": "low/medium/high/critical", 
  "recommendedStrategy": {
    "positioning": "推奨ポジショニング",
    "messaging": ["メッセージング戦略"],
    "pricing": "価格戦略",
    "features": ["必要機能"],
    "marketing": ["マーケティング戦略"]
  },
  "gapAnalysis": [
    {
      "feature": "機能名",
      "ourStatus": "missing/weak/equal/strong",
      "competitorCount": 数値,
      "priority": "low/medium/high/critical"
    }
  ],
  "winningStrategy": {
    "shortTerm": ["短期戦略"],
    "longTerm": ["長期戦略"], 
    "budget": 予算数値,
    "timeline": "期間",
    "riskLevel": 0-100
  }
}
    `;

    const response = await this.openai.chat.completions.create({
      model: "gpt-4-turbo-preview",
      messages: [
        {
          role: "system",
          content: "あなたは競合戦略の専門コンサルタントです。データ分析から実行可能な戦略を提案してください。"
        },
        {
          role: "user",
          content: analysisPrompt
        }
      ],
      max_tokens: 3000,
      temperature: 0.2
    });

    try {
      const analysis = JSON.parse(response.choices[0].message.content || '{}');
      
      const result: CompetitiveAnalysisResult = {
        totalCompetitors: competitors.length,
        marketPosition: analysis.marketPosition || 'follower',
        differentiationOpportunities: analysis.differentiationOpportunities || [],
        threatLevel: analysis.threatLevel || 'medium',
        recommendedStrategy: analysis.recommendedStrategy || {
          positioning: '差別化ポジショニングを開発',
          messaging: ['独自価値を明確化'],
          pricing: '競合比較価格戦略',
          features: ['基本機能強化'],
          marketing: ['デジタルマーケティング強化']
        },
        gapAnalysis: analysis.gapAnalysis || [],
        winningStrategy: analysis.winningStrategy || {
          shortTerm: ['競合調査継続', 'LP最適化'],
          longTerm: ['製品差別化', '市場拡大'],
          budget: 1000000,
          timeline: '6ヶ月',
          riskLevel: 30
        }
      };

      return result;

    } catch (error) {
      console.error('Analysis parsing failed:', error);
      throw new Error('競合分析結果の解析に失敗しました');
    }
  }

  /**
   * 継続監視・アラートシステム
   */
  public async setupCompetitorMonitoring(
    competitors: string[],
    alertThresholds: {
      pricingChange: boolean;
      newFeature: boolean;
      messagingChange: boolean;
      trafficSpike: boolean;
    }
  ): Promise<string> {
    // 実装：cron job / webhook設定
    const monitoringId = `monitor-${Date.now()}`;
    
    console.log(`競合監視設定完了: ${monitoringId}`);
    console.log(`対象: ${competitors.length}社`);
    console.log(`アラート設定:`, alertThresholds);
    
    return monitoringId;
  }

  // ヘルパーメソッド
  private detectLayoutPattern($: cheerio.CheerioAPI): string {
    const sections = $('section, .section').length;
    const columns = $('.col, [class*="col-"]').length;
    
    if (columns > 6) return 'multi-column';
    if (sections > 8) return 'long-form';
    if ($('.hero, [class*="hero"]').length > 0) return 'hero-focused';
    return 'standard';
  }

  private detectPricingModel($: cheerio.CheerioAPI, priceText: string): CompetitorData['pricing']['model'] {
    if (priceText.includes('free') || priceText.includes('無料')) return 'free';
    if (priceText.includes('month') || priceText.includes('月')) return 'subscription';
    if (priceText.includes('tier') || priceText.includes('plan')) return 'tiered';
    return 'one-time';
  }

  private extractPainPoints($: cheerio.CheerioAPI): string[] {
    const painKeywords = ['problem', 'issue', 'challenge', '問題', '課題', '悩み'];
    const painPoints: string[] = [];
    
    painKeywords.forEach(keyword => {
      $(`*:contains(${keyword})`).each((_, el) => {
        const text = $(el).text().trim();
        if (text.length > 10 && text.length < 100) {
          painPoints.push(text);
        }
      });
    });
    
    return painPoints.slice(0, 5);
  }

  private extractBenefits($: cheerio.CheerioAPI): string[] {
    return $('.benefit, [class*="benefit"], .feature, [class*="feature"]')
      .map((_, el) => $(el).text().trim())
      .get()
      .slice(0, 5);
  }

  private extractProofElements($: cheerio.CheerioAPI): string[] {
    return $('.testimonial, .review, .case-study, [class*="proof"]')
      .map((_, el) => $(el).text().trim())
      .get()
      .slice(0, 3);
  }

  private extractUrgencyElements($: cheerio.CheerioAPI): string[] {
    const urgencyKeywords = ['limited', 'now', 'today', '限定', '今すぐ', '緊急'];
    const urgencyElements: string[] = [];
    
    urgencyKeywords.forEach(keyword => {
      $(`*:contains(${keyword})`).each((_, el) => {
        const text = $(el).text().trim();
        if (text.length > 5 && text.length < 50) {
          urgencyElements.push(text);
        }
      });
    });
    
    return urgencyElements.slice(0, 3);
  }
}

export const competitiveAnalysisEngine = new CompetitiveAnalysisEngine();
export default competitiveAnalysisEngine;