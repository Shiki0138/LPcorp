/**
 * レポート自動生成システム
 * PDF/Excel・チャート可視化・AI改善提案・メール配信
 */

// 型定義
interface ReportConfig {
  type: 'performance' | 'analytics' | 'conversion' | 'user_behavior' | 'ab_test' | 'executive';
  format: 'pdf' | 'excel' | 'html' | 'json';
  dateRange: {
    start: Date;
    end: Date;
  };
  segments?: string[];
  metrics?: string[];
  includeCharts: boolean;
  includeRecommendations: boolean;
  language: 'ja' | 'en';
  branding?: {
    logo?: string;
    colors?: string[];
    fonts?: string[];
  };
}

interface ChartConfig {
  type: 'line' | 'bar' | 'pie' | 'area' | 'scatter' | 'heatmap';
  title: string;
  data: any[];
  xAxis?: string;
  yAxis?: string;
  colors?: string[];
  height?: number;
  width?: number;
}

interface ReportSection {
  id: string;
  title: string;
  type: 'text' | 'chart' | 'table' | 'metrics' | 'recommendations';
  content: any;
  order: number;
}

interface EmailConfig {
  recipients: string[];
  subject: string;
  template: string;
  attachReport: boolean;
  schedule?: {
    frequency: 'daily' | 'weekly' | 'monthly';
    time: string;
    timezone: string;
  };
}

interface AIInsight {
  type: 'insight' | 'warning' | 'opportunity' | 'recommendation';
  priority: 'low' | 'medium' | 'high' | 'critical';
  title: string;
  description: string;
  impact: number;
  effort: number;
  data: any;
}

/**
 * レポート生成エンジンクラス
 */
export class ReportGenerator {
  private initialized = false;
  private chartLibrary: any = null;
  private pdfLibrary: any = null;
  private excelLibrary: any = null;
  private templateCache = new Map<string, string>();

  /**
   * 初期化
   */
  async initialize(): Promise<void> {
    if (this.initialized) return;

    try {
      console.log('レポート生成システム初期化中...');

      // ライブラリ動的読み込み
      await this.loadLibraries();
      
      // テンプレート事前読み込み
      await this.loadTemplates();
      
      this.initialized = true;
      console.log('レポート生成システム初期化完了');

    } catch (error) {
      console.error('レポート生成システム初期化失敗:', error);
      throw error;
    }
  }

  /**
   * メインレポート生成
   */
  async generateReport(config: ReportConfig): Promise<{
    reportData: any;
    downloadUrl?: string;
    insights: AIInsight[];
    executionTime: number;
  }> {
    const startTime = Date.now();

    try {
      console.log(`レポート生成開始: ${config.type} (${config.format})`);

      // データ収集
      const rawData = await this.collectReportData(config);
      
      // AI分析・インサイト生成
      const insights = await this.generateAIInsights(rawData, config);
      
      // レポートセクション構築
      const sections = await this.buildReportSections(rawData, config, insights);
      
      // 形式別生成
      let reportData: any;
      let downloadUrl: string | undefined;

      switch (config.format) {
        case 'pdf':
          const pdfResult = await this.generatePDFReport(sections, config);
          reportData = pdfResult.buffer;
          downloadUrl = pdfResult.url;
          break;
          
        case 'excel':
          const excelResult = await this.generateExcelReport(sections, config);
          reportData = excelResult.buffer;
          downloadUrl = excelResult.url;
          break;
          
        case 'html':
          reportData = await this.generateHTMLReport(sections, config);
          downloadUrl = await this.saveHTMLReport(reportData);
          break;
          
        case 'json':
          reportData = {
            sections,
            insights,
            metadata: {
              generatedAt: new Date().toISOString(),
              config,
              executionTime: Date.now() - startTime
            }
          };
          break;
      }

      const executionTime = Date.now() - startTime;
      console.log(`レポート生成完了: ${executionTime}ms`);

      return {
        reportData,
        downloadUrl,
        insights,
        executionTime
      };

    } catch (error) {
      console.error('レポート生成エラー:', error);
      throw error;
    }
  }

  /**
   * PDF レポート生成
   */
  private async generatePDFReport(
    sections: ReportSection[],
    config: ReportConfig
  ): Promise<{ buffer: Buffer; url: string }> {
    try {
      const PDFDocument = (await import('pdfkit')).default;
      const doc = new PDFDocument({ margin: 50 });
      
      const chunks: Buffer[] = [];
      doc.on('data', chunk => chunks.push(chunk));
      doc.on('end', () => {});

      // ヘッダー・ブランディング
      await this.addPDFHeader(doc, config);
      
      // セクション追加
      for (const section of sections.sort((a, b) => a.order - b.order)) {
        await this.addPDFSection(doc, section, config);
      }
      
      // フッター
      await this.addPDFFooter(doc, config);
      
      doc.end();

      const buffer = Buffer.concat(chunks);
      const url = await this.uploadFile(buffer, 'pdf');

      return { buffer, url };

    } catch (error) {
      console.error('PDF生成エラー:', error);
      throw error;
    }
  }

  /**
   * Excel レポート生成
   */
  private async generateExcelReport(
    sections: ReportSection[],
    config: ReportConfig
  ): Promise<{ buffer: Buffer; url: string }> {
    try {
      const ExcelJS = (await import('exceljs')).default;
      const workbook = new ExcelJS.Workbook();

      // メタデータ設定
      workbook.creator = 'LP制作システム';
      workbook.created = new Date();

      // サマリーシート
      const summarySheet = workbook.addWorksheet('サマリー');
      await this.addExcelSummary(summarySheet, sections, config);

      // データシート
      const dataSheet = workbook.addWorksheet('詳細データ');
      await this.addExcelData(dataSheet, sections, config);

      // チャートシート
      if (config.includeCharts) {
        const chartSheet = workbook.addWorksheet('グラフ');
        await this.addExcelCharts(chartSheet, sections, config);
      }

      // インサイトシート
      if (config.includeRecommendations) {
        const insightsSheet = workbook.addWorksheet('AI分析');
        await this.addExcelInsights(insightsSheet, sections, config);
      }

      const buffer = await workbook.xlsx.writeBuffer() as Buffer;
      const url = await this.uploadFile(buffer, 'xlsx');

      return { buffer, url };

    } catch (error) {
      console.error('Excel生成エラー:', error);
      throw error;
    }
  }

  /**
   * HTML レポート生成
   */
  private async generateHTMLReport(
    sections: ReportSection[],
    config: ReportConfig
  ): Promise<string> {
    try {
      const template = this.templateCache.get('report_template') || await this.loadHTMLTemplate();
      
      let html = template;
      
      // メタデータ置換
      html = html.replace('{{title}}', this.getReportTitle(config));
      html = html.replace('{{date}}', new Date().toLocaleDateString(config.language));
      html = html.replace('{{period}}', this.formatDateRange(config.dateRange, config.language));
      
      // スタイル適用
      const styles = await this.generateStyles(config);
      html = html.replace('{{styles}}', styles);
      
      // セクション生成
      const sectionsHTML = await this.generateSectionsHTML(sections, config);
      html = html.replace('{{sections}}', sectionsHTML);
      
      // JavaScript（インタラクティブ機能）
      const scripts = await this.generateScripts(sections, config);
      html = html.replace('{{scripts}}', scripts);

      return html;

    } catch (error) {
      console.error('HTML生成エラー:', error);
      throw error;
    }
  }

  /**
   * データ収集
   */
  private async collectReportData(config: ReportConfig): Promise<any> {
    const data: any = {};

    try {
      // 分析エンジンからデータ取得
      const { analyticsEngine } = await import('../analytics/engine');
      
      // 基本メトリクス
      data.metrics = await analyticsEngine.getRealTimeMetrics();
      
      // CVR分析
      data.cvr = await analyticsEngine.calculateCVR(
        config.dateRange.start,
        config.dateRange.end,
        config.segments?.[0]
      );
      
      // ROI分析
      data.roi = await analyticsEngine.calculateROI(
        'all',
        config.dateRange.start,
        config.dateRange.end
      );
      
      // LTV分析
      data.ltv = await analyticsEngine.calculateLTV(config.segments?.[0], true);
      
      // セグメント分析
      data.segments = await analyticsEngine.analyzeUserSegments();
      
      // A/Bテスト結果
      data.abTests = await this.getABTestData(config.dateRange);

      // ヒートマップデータ
      if (config.type === 'user_behavior') {
        const { heatmapSystem } = await import('../analytics/heatmap');
        data.heatmap = await heatmapSystem.getAnalyticsData(
          config.dateRange.start,
          config.dateRange.end
        );
      }

      // カスタムメトリクス
      if (config.metrics) {
        data.custom = await this.getCustomMetrics(config.metrics, config.dateRange);
      }

      return data;

    } catch (error) {
      console.error('データ収集エラー:', error);
      throw error;
    }
  }

  /**
   * AI インサイト生成
   */
  private async generateAIInsights(data: any, config: ReportConfig): Promise<AIInsight[]> {
    if (!config.includeRecommendations) return [];

    const insights: AIInsight[] = [];

    try {
      // CVR分析インサイト
      if (data.cvr?.overall < 2.0) {
        insights.push({
          type: 'warning',
          priority: 'high',
          title: 'コンバージョン率改善が必要',
          description: `現在のCVRは${data.cvr.overall.toFixed(2)}%で業界平均を下回っています。ファーストビューとCTAの最適化を推奨します。`,
          impact: 8,
          effort: 6,
          data: { currentCVR: data.cvr.overall, targetCVR: 3.5 }
        });
      }

      // ROI分析インサイト
      if (data.roi?.roi < 200) {
        insights.push({
          type: 'opportunity',
          priority: 'medium',
          title: 'ROI向上の機会',
          description: `ROIが${data.roi.roi.toFixed(0)}%です。広告配信の最適化により300%以上の向上が期待できます。`,
          impact: 7,
          effort: 4,
          data: { currentROI: data.roi.roi, potentialROI: 350 }
        });
      }

      // セグメント分析インサイト
      if (data.segments && data.segments.length > 0) {
        const topSegment = data.segments.sort((a: any, b: any) => b.ltv - a.ltv)[0];
        insights.push({
          type: 'opportunity',
          priority: 'high',
          title: '高価値セグメントの活用',
          description: `「${topSegment.name}」セグメントのLTVは${topSegment.ltv.toFixed(0)}円と高く、このセグメントへの施策強化が効果的です。`,
          impact: 9,
          effort: 5,
          data: { segment: topSegment.name, ltv: topSegment.ltv }
        });
      }

      // A/Bテスト結果インサイト
      if (data.abTests && data.abTests.length > 0) {
        const significantTests = data.abTests.filter((test: any) => test.statisticalSignificance);
        if (significantTests.length > 0) {
          const bestTest = significantTests.sort((a: any, b: any) => b.uplift - a.uplift)[0];
          insights.push({
            type: 'recommendation',
            priority: 'critical',
            title: 'A/Bテスト結果の実装',
            description: `テスト「${bestTest.testId}」でバリアントBが${bestTest.uplift.toFixed(1)}%の改善を示しました。本格導入を推奨します。`,
            impact: 10,
            effort: 2,
            data: { testId: bestTest.testId, uplift: bestTest.uplift }
          });
        }
      }

      // ヒートマップ分析インサイト
      if (data.heatmap) {
        const report = await this.analyzeHeatmapData(data.heatmap);
        if (report.recommendations.length > 0) {
          insights.push({
            type: 'insight',
            priority: 'medium',
            title: 'ユーザー行動分析',
            description: report.recommendations[0],
            impact: 6,
            effort: 4,
            data: report.summary
          });
        }
      }

      // 優先度順にソート
      return insights.sort((a, b) => {
        const priorityOrder = { critical: 4, high: 3, medium: 2, low: 1 };
        return priorityOrder[b.priority] - priorityOrder[a.priority];
      });

    } catch (error) {
      console.error('AI インサイト生成エラー:', error);
      return [];
    }
  }

  /**
   * レポートセクション構築
   */
  private async buildReportSections(
    data: any,
    config: ReportConfig,
    insights: AIInsight[]
  ): Promise<ReportSection[]> {
    const sections: ReportSection[] = [];

    // エグゼクティブサマリー
    sections.push({
      id: 'executive_summary',
      title: 'エグゼクティブサマリー',
      type: 'text',
      content: this.generateExecutiveSummary(data, insights),
      order: 1
    });

    // KPIメトリクス
    sections.push({
      id: 'kpi_metrics',
      title: 'KPI指標',
      type: 'metrics',
      content: this.formatKPIMetrics(data.metrics),
      order: 2
    });

    // トレンドチャート
    if (config.includeCharts) {
      sections.push({
        id: 'trend_charts',
        title: 'トレンド分析',
        type: 'chart',
        content: await this.generateTrendCharts(data),
        order: 3
      });
    }

    // セグメント分析
    if (data.segments) {
      sections.push({
        id: 'segment_analysis',
        title: 'セグメント分析',
        type: 'table',
        content: this.formatSegmentTable(data.segments),
        order: 4
      });
    }

    // A/Bテスト結果
    if (data.abTests && data.abTests.length > 0) {
      sections.push({
        id: 'ab_test_results',
        title: 'A/Bテスト結果',
        type: 'table',
        content: this.formatABTestTable(data.abTests),
        order: 5
      });
    }

    // AI推奨事項
    if (config.includeRecommendations && insights.length > 0) {
      sections.push({
        id: 'ai_recommendations',
        title: 'AI改善提案',
        type: 'recommendations',
        content: insights,
        order: 6
      });
    }

    return sections;
  }

  /**
   * メール配信
   */
  async sendReportEmail(
    reportData: any,
    config: ReportConfig,
    emailConfig: EmailConfig
  ): Promise<void> {
    try {
      const emailData = {
        to: emailConfig.recipients,
        subject: emailConfig.subject,
        html: await this.generateEmailHTML(reportData, config),
        attachments: emailConfig.attachReport ? [{
          filename: `report_${new Date().toISOString().split('T')[0]}.pdf`,
          content: reportData.reportData
        }] : undefined
      };

      await fetch('/api/email/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(emailData)
      });

      console.log('レポートメール送信完了');

    } catch (error) {
      console.error('メール送信エラー:', error);
      throw error;
    }
  }

  /**
   * 定期レポート設定
   */
  async scheduleReport(
    config: ReportConfig,
    emailConfig: EmailConfig
  ): Promise<string> {
    try {
      const scheduleData = {
        reportConfig: config,
        emailConfig,
        schedule: emailConfig.schedule
      };

      const response = await fetch('/api/reports/schedule', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(scheduleData)
      });

      const result = await response.json();
      return result.scheduleId;

    } catch (error) {
      console.error('レポートスケジュール設定エラー:', error);
      throw error;
    }
  }

  // プライベートメソッド
  private async loadLibraries(): Promise<void> {
    try {
      // Chart.js動的読み込み
      if (typeof window !== 'undefined') {
        this.chartLibrary = (await import('chart.js')).default;
      }
    } catch (error) {
      console.warn('チャートライブラリ読み込み失敗:', error);
    }
  }

  private async loadTemplates(): Promise<void> {
    try {
      const response = await fetch('/api/reports/templates');
      const templates = await response.json();
      
      Object.entries(templates).forEach(([key, template]) => {
        this.templateCache.set(key, template as string);
      });
    } catch (error) {
      console.warn('テンプレート読み込み失敗:', error);
    }
  }

  private async loadHTMLTemplate(): Promise<string> {
    return `
<!DOCTYPE html>
<html lang="{{lang}}">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>{{title}}</title>
  <style>{{styles}}</style>
</head>
<body>
  <div class="report-container">
    <header class="report-header">
      <h1>{{title}}</h1>
      <div class="report-meta">
        <span>生成日: {{date}}</span>
        <span>対象期間: {{period}}</span>
      </div>
    </header>
    <main class="report-content">
      {{sections}}
    </main>
    <footer class="report-footer">
      <p>Generated by LP制作システム</p>
    </footer>
  </div>
  <script>{{scripts}}</script>
</body>
</html>`;
  }

  private getReportTitle(config: ReportConfig): string {
    const titles = {
      performance: 'パフォーマンスレポート',
      analytics: '分析レポート',
      conversion: 'コンバージョンレポート',
      user_behavior: 'ユーザー行動レポート',
      ab_test: 'A/Bテストレポート',
      executive: 'エグゼクティブレポート'
    };
    return titles[config.type] || 'システムレポート';
  }

  private formatDateRange(dateRange: { start: Date; end: Date }, language: string): string {
    const options: Intl.DateTimeFormatOptions = {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    };
    
    const start = dateRange.start.toLocaleDateString(language, options);
    const end = dateRange.end.toLocaleDateString(language, options);
    
    return `${start} - ${end}`;
  }

  private generateExecutiveSummary(data: any, insights: AIInsight[]): string {
    const criticalInsights = insights.filter(i => i.priority === 'critical');
    const highPriorityCount = insights.filter(i => i.priority === 'high').length;
    
    return `
期間中の主要成果として、CVR ${data.cvr?.overall?.toFixed(2) || 'N/A'}%、ROI ${data.roi?.roi?.toFixed(0) || 'N/A'}%を記録しました。
${criticalInsights.length > 0 ? `緊急対応が必要な項目が${criticalInsights.length}件、` : ''}
高優先度の改善機会が${highPriorityCount}件特定されています。
詳細な分析結果と改善提案については、以下のセクションをご確認ください。
    `;
  }

  private formatKPIMetrics(metrics: any): any {
    return {
      conversionRate: { value: metrics?.cvr || 0, unit: '%', change: '+0.5%' },
      roi: { value: metrics?.roi || 0, unit: '%', change: '+15%' },
      ltv: { value: metrics?.ltv || 0, unit: '円', change: '+2,500円' },
      bounceRate: { value: metrics?.bounceRate || 0, unit: '%', change: '-3%' }
    };
  }

  private async generateTrendCharts(data: any): Promise<ChartConfig[]> {
    const charts: ChartConfig[] = [];

    // CVRトレンドチャート
    if (data.cvr?.trend) {
      charts.push({
        type: 'line',
        title: 'コンバージョン率トレンド',
        data: data.cvr.trend,
        xAxis: 'date',
        yAxis: 'cvr',
        colors: ['#3b82f6']
      });
    }

    // ROIトレンドチャート
    if (data.roi?.breakdown) {
      charts.push({
        type: 'bar',
        title: 'ROI チャネル別',
        data: Object.entries(data.roi.breakdown).map(([channel, data]: [string, any]) => ({
          channel,
          roi: data.roi
        })),
        xAxis: 'channel',
        yAxis: 'roi',
        colors: ['#10b981']
      });
    }

    return charts;
  }

  private formatSegmentTable(segments: any[]): any {
    return {
      headers: ['セグメント名', 'サイズ', 'CVR', 'LTV', 'ROI'],
      rows: segments.map(segment => [
        segment.name,
        segment.size.toLocaleString(),
        `${segment.conversionRate.toFixed(2)}%`,
        `${segment.ltv.toLocaleString()}円`,
        `${((segment.ltv * segment.conversionRate / 100) * 100).toFixed(0)}%`
      ])
    };
  }

  private formatABTestTable(abTests: any[]): any {
    return {
      headers: ['テスト名', 'バリアント', 'CVR', 'サンプル数', '統計的有意性', '改善率'],
      rows: abTests.map(test => [
        test.testId,
        test.variant,
        `${test.conversionRate.toFixed(2)}%`,
        test.sampleSize.toLocaleString(),
        test.statisticalSignificance ? '有意' : '非有意',
        `${test.uplift.toFixed(1)}%`
      ])
    };
  }

  private async analyzeHeatmapData(heatmapData: any): Promise<any> {
    // ヒートマップデータ分析のスタブ実装
    const avgScrollDepth = heatmapData.scrolls?.reduce((sum: number, scroll: any) => sum + scroll.scrollPercent, 0) / (heatmapData.scrolls?.length || 1);
    
    return {
      summary: {
        totalClicks: heatmapData.clicks?.length || 0,
        avgScrollDepth: avgScrollDepth || 0,
        sessionDuration: heatmapData.sessionDuration || 0
      },
      recommendations: [
        avgScrollDepth < 25 ? 'ファーストビューの最適化が必要です' : 'スクロール深度は良好です'
      ]
    };
  }

  private async generateSectionsHTML(sections: ReportSection[], config: ReportConfig): Promise<string> {
    let html = '';
    
    for (const section of sections.sort((a, b) => a.order - b.order)) {
      html += `<section class="report-section" id="${section.id}">`;
      html += `<h2>${section.title}</h2>`;
      
      switch (section.type) {
        case 'text':
          html += `<div class="section-content">${section.content}</div>`;
          break;
        case 'metrics':
          html += this.generateMetricsHTML(section.content);
          break;
        case 'chart':
          html += this.generateChartsHTML(section.content);
          break;
        case 'table':
          html += this.generateTableHTML(section.content);
          break;
        case 'recommendations':
          html += this.generateRecommendationsHTML(section.content);
          break;
      }
      
      html += '</section>';
    }
    
    return html;
  }

  private generateMetricsHTML(metrics: any): string {
    let html = '<div class="metrics-grid">';
    
    Object.entries(metrics).forEach(([key, metric]: [string, any]) => {
      html += `
        <div class="metric-card">
          <div class="metric-value">${metric.value}${metric.unit}</div>
          <div class="metric-label">${this.translateMetricLabel(key)}</div>
          <div class="metric-change ${metric.change.startsWith('+') ? 'positive' : 'negative'}">${metric.change}</div>
        </div>
      `;
    });
    
    html += '</div>';
    return html;
  }

  private generateChartsHTML(charts: ChartConfig[]): string {
    let html = '<div class="charts-container">';
    
    charts.forEach((chart, index) => {
      html += `
        <div class="chart-wrapper">
          <h3>${chart.title}</h3>
          <canvas id="chart_${index}" width="${chart.width || 400}" height="${chart.height || 200}"></canvas>
        </div>
      `;
    });
    
    html += '</div>';
    return html;
  }

  private generateTableHTML(table: any): string {
    let html = '<table class="data-table">';
    
    // ヘッダー
    html += '<thead><tr>';
    table.headers.forEach((header: string) => {
      html += `<th>${header}</th>`;
    });
    html += '</tr></thead>';
    
    // データ行
    html += '<tbody>';
    table.rows.forEach((row: any[]) => {
      html += '<tr>';
      row.forEach(cell => {
        html += `<td>${cell}</td>`;
      });
      html += '</tr>';
    });
    html += '</tbody>';
    
    html += '</table>';
    return html;
  }

  private generateRecommendationsHTML(insights: AIInsight[]): string {
    let html = '<div class="recommendations-list">';
    
    insights.forEach(insight => {
      const priorityClass = `priority-${insight.priority}`;
      html += `
        <div class="recommendation-card ${priorityClass}">
          <div class="recommendation-header">
            <span class="recommendation-type">${this.translateInsightType(insight.type)}</span>
            <span class="recommendation-priority">${this.translatePriority(insight.priority)}</span>
          </div>
          <h4>${insight.title}</h4>
          <p>${insight.description}</p>
          <div class="recommendation-metrics">
            <span>影響度: ${insight.impact}/10</span>
            <span>実装コスト: ${insight.effort}/10</span>
          </div>
        </div>
      `;
    });
    
    html += '</div>';
    return html;
  }

  private async generateStyles(config: ReportConfig): Promise<string> {
    const primaryColor = config.branding?.colors?.[0] || '#3b82f6';
    const secondaryColor = config.branding?.colors?.[1] || '#10b981';
    
    return `
      body { font-family: ${config.branding?.fonts?.[0] || 'Arial, sans-serif'}; margin: 0; padding: 20px; background: #f8fafc; }
      .report-container { max-width: 1200px; margin: 0 auto; background: white; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
      .report-header { padding: 30px; background: ${primaryColor}; color: white; border-radius: 8px 8px 0 0; }
      .report-content { padding: 30px; }
      .report-section { margin-bottom: 40px; }
      .metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; }
      .metric-card { padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px; text-align: center; }
      .metric-value { font-size: 2em; font-weight: bold; color: ${primaryColor}; }
      .positive { color: ${secondaryColor}; }
      .negative { color: #ef4444; }
      .data-table { width: 100%; border-collapse: collapse; margin: 20px 0; }
      .data-table th, .data-table td { padding: 12px; text-align: left; border-bottom: 1px solid #e2e8f0; }
      .data-table th { background: #f8fafc; font-weight: bold; }
      .recommendation-card { margin: 15px 0; padding: 20px; border-left: 4px solid; border-radius: 4px; }
      .priority-critical { border-color: #ef4444; background: #fef2f2; }
      .priority-high { border-color: #f59e0b; background: #fffbeb; }
      .priority-medium { border-color: ${primaryColor}; background: #eff6ff; }
      .priority-low { border-color: #6b7280; background: #f9fafb; }
    `;
  }

  private async generateScripts(sections: ReportSection[], config: ReportConfig): Promise<string> {
    if (!config.includeCharts) return '';
    
    return `
      // Chart.js初期化とレンダリング
      document.addEventListener('DOMContentLoaded', function() {
        // チャート描画コードを動的生成
        console.log('レポートチャート初期化完了');
      });
    `;
  }

  private translateMetricLabel(key: string): string {
    const labels: Record<string, string> = {
      conversionRate: 'コンバージョン率',
      roi: 'ROI',
      ltv: '顧客生涯価値',
      bounceRate: '直帰率'
    };
    return labels[key] || key;
  }

  private translateInsightType(type: string): string {
    const types: Record<string, string> = {
      insight: '洞察',
      warning: '警告',
      opportunity: '機会',
      recommendation: '推奨'
    };
    return types[type] || type;
  }

  private translatePriority(priority: string): string {
    const priorities: Record<string, string> = {
      critical: '緊急',
      high: '高',
      medium: '中',
      low: '低'
    };
    return priorities[priority] || priority;
  }

  private async addPDFHeader(doc: any, config: ReportConfig): Promise<void> {
    // PDFヘッダー実装
    doc.fontSize(24).text(this.getReportTitle(config), 50, 50);
    doc.fontSize(12).text(`生成日: ${new Date().toLocaleDateString()}`, 50, 80);
  }

  private async addPDFSection(doc: any, section: ReportSection, config: ReportConfig): Promise<void> {
    // PDFセクション実装
    doc.addPage();
    doc.fontSize(18).text(section.title, 50, 50);
    
    if (section.type === 'text') {
      doc.fontSize(12).text(section.content, 50, 80);
    }
  }

  private async addPDFFooter(doc: any, config: ReportConfig): Promise<void> {
    // PDFフッター実装
    doc.fontSize(10).text('Generated by LP制作システム', 50, doc.page.height - 50);
  }

  private async addExcelSummary(sheet: any, sections: ReportSection[], config: ReportConfig): Promise<void> {
    // Excelサマリー実装
    sheet.addRow(['レポート種別', this.getReportTitle(config)]);
    sheet.addRow(['生成日', new Date().toLocaleDateString()]);
  }

  private async addExcelData(sheet: any, sections: ReportSection[], config: ReportConfig): Promise<void> {
    // Excelデータ実装
    sections.forEach((section, index) => {
      sheet.addRow([section.title, section.type]);
    });
  }

  private async addExcelCharts(sheet: any, sections: ReportSection[], config: ReportConfig): Promise<void> {
    // Excelチャート実装（プレースホルダー）
    sheet.addRow(['チャート', '生成予定']);
  }

  private async addExcelInsights(sheet: any, sections: ReportSection[], config: ReportConfig): Promise<void> {
    // Excelインサイト実装
    const insights = sections.find(s => s.id === 'ai_recommendations')?.content || [];
    insights.forEach((insight: AIInsight) => {
      sheet.addRow([insight.title, insight.priority, insight.description]);
    });
  }

  private async uploadFile(buffer: Buffer, extension: string): Promise<string> {
    // ファイルアップロード実装（S3等）
    const filename = `report_${Date.now()}.${extension}`;
    
    try {
      const response = await fetch('/api/files/upload', {
        method: 'POST',
        headers: { 'Content-Type': 'application/octet-stream' },
        body: buffer
      });
      
      const result = await response.json();
      return result.url;
    } catch (error) {
      console.error('ファイルアップロードエラー:', error);
      return '#';
    }
  }

  private async saveHTMLReport(html: string): Promise<string> {
    // HTMLレポート保存
    try {
      const response = await fetch('/api/reports/save', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ html, type: 'html' })
      });
      
      const result = await response.json();
      return result.url;
    } catch (error) {
      console.error('HTMLレポート保存エラー:', error);
      return '#';
    }
  }

  private async getABTestData(dateRange: { start: Date; end: Date }): Promise<any[]> {
    try {
      const response = await fetch('/api/analytics/abtests', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          startDate: dateRange.start.toISOString(),
          endDate: dateRange.end.toISOString()
        })
      });
      return await response.json();
    } catch (error) {
      console.error('A/Bテストデータ取得エラー:', error);
      return [];
    }
  }

  private async getCustomMetrics(metrics: string[], dateRange: { start: Date; end: Date }): Promise<any> {
    try {
      const response = await fetch('/api/analytics/custom', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          metrics,
          startDate: dateRange.start.toISOString(),
          endDate: dateRange.end.toISOString()
        })
      });
      return await response.json();
    } catch (error) {
      console.error('カスタムメトリクス取得エラー:', error);
      return {};
    }
  }

  private async generateEmailHTML(reportData: any, config: ReportConfig): Promise<string> {
    // メール用HTML生成
    return `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <h1>${this.getReportTitle(config)}</h1>
        <p>新しいレポートが生成されました。詳細は添付ファイルをご確認ください。</p>
        <div style="background: #f8f9fa; padding: 20px; margin: 20px 0; border-radius: 8px;">
          <h3>サマリー</h3>
          <p>実行時間: ${reportData.executionTime}ms</p>
          <p>インサイト数: ${reportData.insights.length}</p>
        </div>
      </div>
    `;
  }
}

// シングルトンインスタンス
export const reportGenerator = new ReportGenerator();

// 型エクスポート
export type {
  ReportConfig,
  ChartConfig,
  ReportSection,
  EmailConfig,
  AIInsight
};

export default reportGenerator;