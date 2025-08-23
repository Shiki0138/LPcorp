/**
 * ヒートマップ自動埋込システム
 * Google Analytics 4統合・リアルタイムデータ収集・可視化エンジン
 */

import { EventEmitter } from 'events';

// Google Analytics 4 統合型定義
interface GAConfig {
  measurementId: string;
  apiSecret: string;
  sessionId: string;
  userId?: string;
}

interface ClickEvent {
  x: number;
  y: number;
  element: string;
  timestamp: number;
  sessionId: string;
  pageUrl: string;
  userId?: string;
}

interface ScrollEvent {
  scrollY: number;
  scrollPercent: number;
  maxScroll: number;
  timestamp: number;
  sessionId: string;
  pageUrl: string;
}

interface HeatmapData {
  clicks: ClickEvent[];
  scrolls: ScrollEvent[];
  pageViews: number;
  sessionDuration: number;
  bounceRate: number;
  conversionEvents: number;
}

interface HeatmapConfig {
  enabled: boolean;
  gaConfig: GAConfig;
  sampleRate: number;
  excludeElements: string[];
  trackScrollDepth: boolean;
  trackClicks: boolean;
  trackHovers: boolean;
  realTimeSync: boolean;
  batchSize: number;
  flushInterval: number;
}

class HeatmapTracker extends EventEmitter {
  private config: HeatmapConfig;
  private isInitialized = false;
  private eventQueue: Array<ClickEvent | ScrollEvent> = [];
  private sessionStartTime: number;
  private maxScrollDepth = 0;
  private totalClicks = 0;
  private websocket: WebSocket | null = null;
  private flushTimer: NodeJS.Timeout | null = null;

  constructor(config: HeatmapConfig) {
    super();
    this.config = config;
    this.sessionStartTime = Date.now();
    
    if (typeof window !== 'undefined') {
      this.initialize();
    }
  }

  /**
   * システム初期化・Google Analytics 4統合
   */
  async initialize(): Promise<void> {
    if (this.isInitialized) return;

    try {
      // Google Analytics 4 初期化
      await this.initializeGA4();
      
      // イベントリスナー設定
      this.setupEventListeners();
      
      // WebSocket接続（リアルタイム同期）
      if (this.config.realTimeSync) {
        await this.setupWebSocket();
      }
      
      // 定期フラッシュ設定
      this.setupFlushTimer();
      
      this.isInitialized = true;
      this.emit('initialized');
      
      console.log('ヒートマップトラッカー初期化完了');
      
    } catch (error) {
      console.error('ヒートマップ初期化失敗:', error);
      throw error;
    }
  }

  /**
   * Google Analytics 4 初期化
   */
  private async initializeGA4(): Promise<void> {
    if (typeof window === 'undefined') return;

    // GA4 スクリプト動的ロード
    const script = document.createElement('script');
    script.src = `https://www.googletagmanager.com/gtag/js?id=${this.config.gaConfig.measurementId}`;
    script.async = true;
    document.head.appendChild(script);

    // gtag初期化
    (window as any).dataLayer = (window as any).dataLayer || [];
    function gtag(...args: any[]) {
      (window as any).dataLayer.push(args);
    }
    (window as any).gtag = gtag;

    gtag('js', new Date());
    gtag('config', this.config.gaConfig.measurementId, {
      session_id: this.config.gaConfig.sessionId,
      user_id: this.config.gaConfig.userId,
      custom_map: {
        custom_parameter_1: 'heatmap_data'
      }
    });

    // Enhanced Ecommerce設定
    gtag('config', this.config.gaConfig.measurementId, {
      enhanced_ecommerce: true,
      allow_enhanced_conversions: true
    });
  }

  /**
   * イベントリスナー設定
   */
  private setupEventListeners(): void {
    if (typeof window === 'undefined') return;

    // クリックイベント追跡
    if (this.config.trackClicks) {
      document.addEventListener('click', this.handleClick.bind(this), { passive: true });
    }

    // スクロールイベント追跡
    if (this.config.trackScrollDepth) {
      document.addEventListener('scroll', this.handleScroll.bind(this), { passive: true });
      window.addEventListener('beforeunload', this.handlePageUnload.bind(this));
    }

    // ホバーイベント追跡
    if (this.config.trackHovers) {
      document.addEventListener('mouseover', this.handleHover.bind(this), { passive: true });
    }

    // ページ可視性変更
    document.addEventListener('visibilitychange', this.handleVisibilityChange.bind(this));
  }

  /**
   * クリックイベントハンドラ
   */
  private handleClick(event: MouseEvent): void {
    if (!this.shouldTrack()) return;

    const target = event.target as HTMLElement;
    
    // 除外要素チェック
    if (this.config.excludeElements.some(selector => target.matches(selector))) {
      return;
    }

    const clickEvent: ClickEvent = {
      x: event.clientX,
      y: event.clientY,
      element: this.getElementSelector(target),
      timestamp: Date.now(),
      sessionId: this.config.gaConfig.sessionId,
      pageUrl: window.location.href,
      userId: this.config.gaConfig.userId
    };

    this.trackEvent(clickEvent);
    this.totalClicks++;

    // Google Analytics イベント送信
    this.sendToGA4('click', {
      element_selector: clickEvent.element,
      click_x: clickEvent.x,
      click_y: clickEvent.y,
      page_location: clickEvent.pageUrl
    });
  }

  /**
   * スクロールイベントハンドラ
   */
  private handleScroll(): void {
    if (!this.shouldTrack()) return;

    const scrollY = window.scrollY;
    const documentHeight = document.documentElement.scrollHeight - window.innerHeight;
    const scrollPercent = Math.round((scrollY / documentHeight) * 100);

    // 最大スクロール深度更新
    this.maxScrollDepth = Math.max(this.maxScrollDepth, scrollPercent);

    const scrollEvent: ScrollEvent = {
      scrollY,
      scrollPercent,
      maxScroll: this.maxScrollDepth,
      timestamp: Date.now(),
      sessionId: this.config.gaConfig.sessionId,
      pageUrl: window.location.href
    };

    this.trackEvent(scrollEvent);

    // スクロール深度マイルストーン
    if (scrollPercent === 25 || scrollPercent === 50 || scrollPercent === 75 || scrollPercent === 100) {
      this.sendToGA4('scroll', {
        scroll_depth: scrollPercent,
        max_scroll_depth: this.maxScrollDepth
      });
    }
  }

  /**
   * ホバーイベントハンドラ
   */
  private handleHover(event: MouseEvent): void {
    if (!this.shouldTrack()) return;

    const target = event.target as HTMLElement;
    
    // 重要な要素のみ追跡（CTAボタン、リンク等）
    if (target.matches('button, a, [data-track-hover]')) {
      this.sendToGA4('hover', {
        element_selector: this.getElementSelector(target),
        hover_x: event.clientX,
        hover_y: event.clientY
      });
    }
  }

  /**
   * ページ離脱時処理
   */
  private handlePageUnload(): void {
    const sessionDuration = Date.now() - this.sessionStartTime;
    
    // セッション統計をGA4に送信
    this.sendToGA4('session_end', {
      session_duration: sessionDuration,
      total_clicks: this.totalClicks,
      max_scroll_depth: this.maxScrollDepth,
      page_views: 1
    });

    // 残りのイベントをフラッシュ
    this.flushEvents();
  }

  /**
   * ページ可視性変更ハンドラ
   */
  private handleVisibilityChange(): void {
    if (document.visibilityState === 'hidden') {
      this.flushEvents();
    }
  }

  /**
   * WebSocket接続設定（リアルタイム同期）
   */
  private async setupWebSocket(): Promise<void> {
    try {
      const wsUrl = process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:3001/heatmap';
      this.websocket = new WebSocket(wsUrl);

      this.websocket.onopen = () => {
        console.log('リアルタイムヒートマップ接続確立');
        this.emit('websocket_connected');
      };

      this.websocket.onmessage = (event) => {
        const data = JSON.parse(event.data);
        this.emit('realtime_data', data);
      };

      this.websocket.onerror = (error) => {
        console.error('WebSocket接続エラー:', error);
        this.emit('websocket_error', error);
      };

    } catch (error) {
      console.warn('WebSocket接続失敗 - 非リアルタイムモードで続行:', error);
    }
  }

  /**
   * 定期フラッシュタイマー設定
   */
  private setupFlushTimer(): void {
    this.flushTimer = setInterval(() => {
      this.flushEvents();
    }, this.config.flushInterval);
  }

  /**
   * イベント追跡
   */
  private trackEvent(event: ClickEvent | ScrollEvent): void {
    this.eventQueue.push(event);

    // リアルタイム送信
    if (this.config.realTimeSync && this.websocket?.readyState === WebSocket.OPEN) {
      this.websocket.send(JSON.stringify(event));
    }

    // バッチサイズに達したらフラッシュ
    if (this.eventQueue.length >= this.config.batchSize) {
      this.flushEvents();
    }

    this.emit('event_tracked', event);
  }

  /**
   * イベントバッチ送信
   */
  private async flushEvents(): Promise<void> {
    if (this.eventQueue.length === 0) return;

    const events = [...this.eventQueue];
    this.eventQueue = [];

    try {
      // バックエンドAPI送信
      await fetch('/api/analytics/heatmap', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          events,
          sessionId: this.config.gaConfig.sessionId,
          timestamp: Date.now()
        })
      });

      this.emit('events_flushed', events.length);

    } catch (error) {
      console.error('イベント送信失敗:', error);
      // 失敗した場合は再度キューに戻す
      this.eventQueue.unshift(...events);
      this.emit('flush_error', error);
    }
  }

  /**
   * Google Analytics 4 イベント送信
   */
  private sendToGA4(eventName: string, parameters: Record<string, any>): void {
    if (typeof window === 'undefined' || !(window as any).gtag) return;

    (window as any).gtag('event', eventName, {
      ...parameters,
      session_id: this.config.gaConfig.sessionId,
      timestamp: Date.now()
    });
  }

  /**
   * 要素セレクター生成
   */
  private getElementSelector(element: HTMLElement): string {
    if (element.id) return `#${element.id}`;
    if (element.className) return `.${element.className.split(' ')[0]}`;
    return element.tagName.toLowerCase();
  }

  /**
   * 追跡判定（サンプリング）
   */
  private shouldTrack(): boolean {
    return Math.random() < this.config.sampleRate;
  }

  /**
   * ヒートマップデータ取得
   */
  async getHeatmapData(
    startDate: Date,
    endDate: Date,
    pageUrl?: string
  ): Promise<HeatmapData> {
    try {
      const response = await fetch('/api/analytics/heatmap/data', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          startDate: startDate.toISOString(),
          endDate: endDate.toISOString(),
          pageUrl,
          sessionId: this.config.gaConfig.sessionId
        })
      });

      const data = await response.json();
      return data;

    } catch (error) {
      console.error('ヒートマップデータ取得失敗:', error);
      throw error;
    }
  }

  /**
   * リアルタイムヒートマップ可視化
   */
  renderHeatmap(container: HTMLElement, data: HeatmapData): void {
    // Canvas要素作成
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d')!;
    
    canvas.width = window.innerWidth;
    canvas.height = document.documentElement.scrollHeight;
    canvas.style.position = 'fixed';
    canvas.style.top = '0';
    canvas.style.left = '0';
    canvas.style.pointerEvents = 'none';
    canvas.style.zIndex = '9999';
    
    container.appendChild(canvas);

    // クリックヒートマップ描画
    data.clicks.forEach(click => {
      ctx.beginPath();
      ctx.arc(click.x, click.y, 20, 0, 2 * Math.PI);
      ctx.fillStyle = 'rgba(255, 0, 0, 0.3)';
      ctx.fill();
    });

    // スクロールヒートマップ描画
    const scrollGradient = ctx.createLinearGradient(0, 0, 0, canvas.height);
    scrollGradient.addColorStop(0, 'rgba(0, 255, 0, 0.1)');
    scrollGradient.addColorStop(1, 'rgba(255, 255, 0, 0.3)');
    
    ctx.fillStyle = scrollGradient;
    ctx.fillRect(0, 0, canvas.width, canvas.height);
  }

  /**
   * 統計レポート生成
   */
  generateReport(data: HeatmapData): {
    summary: {
      totalClicks: number;
      avgScrollDepth: number;
      sessionDuration: number;
      bounceRate: number;
    };
    insights: string[];
    recommendations: string[];
  } {
    const avgScrollDepth = data.scrolls.reduce((sum, scroll) => sum + scroll.scrollPercent, 0) / data.scrolls.length;
    
    const insights = [];
    const recommendations = [];

    // インサイト生成
    if (avgScrollDepth < 25) {
      insights.push('ユーザーのスクロール深度が低い');
      recommendations.push('ファーストビューのコンテンツを最適化');
    }

    if (data.clicks.length < 3) {
      insights.push('クリック数が少ない');
      recommendations.push('CTAボタンの配置と文言を見直し');
    }

    if (data.bounceRate > 0.7) {
      insights.push('直帰率が高い');
      recommendations.push('ページ読み込み速度とコンテンツ関連性を改善');
    }

    return {
      summary: {
        totalClicks: data.clicks.length,
        avgScrollDepth,
        sessionDuration: data.sessionDuration,
        bounceRate: data.bounceRate
      },
      insights,
      recommendations
    };
  }

  /**
   * トラッカー停止
   */
  destroy(): void {
    if (this.flushTimer) {
      clearInterval(this.flushTimer);
    }

    if (this.websocket) {
      this.websocket.close();
    }

    // 残りイベントをフラッシュ
    this.flushEvents();

    this.removeAllListeners();
    this.isInitialized = false;
  }
}

/**
 * ヒートマップシステム管理クラス
 */
export class HeatmapSystem {
  private tracker: HeatmapTracker | null = null;
  private defaultConfig: HeatmapConfig = {
    enabled: true,
    gaConfig: {
      measurementId: process.env.NEXT_PUBLIC_GA4_MEASUREMENT_ID || '',
      apiSecret: process.env.GA4_API_SECRET || '',
      sessionId: this.generateSessionId(),
      userId: undefined
    },
    sampleRate: 0.1, // 10%サンプリング
    excludeElements: ['[data-no-track]', '.no-track'],
    trackScrollDepth: true,
    trackClicks: true,
    trackHovers: true,
    realTimeSync: true,
    batchSize: 50,
    flushInterval: 30000 // 30秒
  };

  /**
   * システム初期化
   */
  async init(customConfig?: Partial<HeatmapConfig>): Promise<void> {
    if (this.tracker) {
      console.warn('ヒートマップシステムは既に初期化済み');
      return;
    }

    const config = { ...this.defaultConfig, ...customConfig };
    this.tracker = new HeatmapTracker(config);
    
    await this.tracker.initialize();
  }

  /**
   * ヒートマップデータ取得
   */
  async getAnalyticsData(
    startDate: Date,
    endDate: Date,
    pageUrl?: string
  ): Promise<HeatmapData | null> {
    if (!this.tracker) {
      console.error('ヒートマップシステムが初期化されていません');
      return null;
    }

    return await this.tracker.getHeatmapData(startDate, endDate, pageUrl);
  }

  /**
   * ヒートマップ可視化
   */
  visualizeHeatmap(container: HTMLElement, data: HeatmapData): void {
    if (!this.tracker) {
      console.error('ヒートマップシステムが初期化されていません');
      return;
    }

    this.tracker.renderHeatmap(container, data);
  }

  /**
   * レポート生成
   */
  generateAnalysisReport(data: HeatmapData) {
    if (!this.tracker) {
      console.error('ヒートマップシステムが初期化されていません');
      return null;
    }

    return this.tracker.generateReport(data);
  }

  /**
   * セッションID生成
   */
  private generateSessionId(): string {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * システム停止
   */
  destroy(): void {
    if (this.tracker) {
      this.tracker.destroy();
      this.tracker = null;
    }
  }
}

// シングルトンインスタンス
export const heatmapSystem = new HeatmapSystem();

// 自動初期化（プロダクション環境でのみ）
if (typeof window !== 'undefined' && process.env.NODE_ENV === 'production') {
  heatmapSystem.init().catch(console.error);
}

export default heatmapSystem;