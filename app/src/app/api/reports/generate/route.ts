import { NextRequest, NextResponse } from 'next/server'
import fs from 'fs/promises'
import path from 'path'

interface ReportRequest {
  clientId: string
  reportType: 'daily' | 'weekly' | 'monthly'
  format: 'json' | 'html' | 'pdf'
  includeImages?: boolean
}

interface AnalyticsMetrics {
  pageViews: number
  uniqueVisitors: number
  conversionRate: number
  averageSessionDuration: number
  bounceRate: number
  topPages: Array<{ page: string; views: number }>
  deviceBreakdown: { desktop: number; mobile: number; tablet: number }
  trafficSources: Array<{ source: string; visits: number }>
}

export async function POST(request: NextRequest) {
  try {
    const { clientId, reportType = 'daily', format = 'html', includeImages = true }: ReportRequest = await request.json()

    if (!clientId) {
      return NextResponse.json(
        { error: 'クライアントIDが必要です' },
        { status: 400 }
      )
    }

    console.log(`📊 レポート生成開始: ${clientId} (${reportType})`)

    // クライアントデータ読み込み
    const projectDir = path.join(process.cwd(), 'projects', clientId)
    const clientDataPath = path.join(projectDir, 'client-data.json')
    
    let clientData: any
    try {
      const rawData = await fs.readFile(clientDataPath, 'utf8')
      clientData = JSON.parse(rawData)
    } catch (error) {
      return NextResponse.json(
        { error: 'クライアントデータが見つかりません' },
        { status: 404 }
      )
    }

    // 分析データ収集
    const analyticsData = await collectAnalyticsData(clientId, reportType)
    
    // レポート生成
    const report = await generateReport(clientData, analyticsData, reportType, format)

    // レポート保存
    const reportsDir = path.join(projectDir, 'reports')
    await fs.mkdir(reportsDir, { recursive: true })
    
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-')
    const filename = `${reportType}-report-${timestamp}.${format}`
    const reportPath = path.join(reportsDir, filename)
    
    await fs.writeFile(reportPath, report.content, 'utf8')

    // メール送信準備（実装時はnodemailer等を使用）
    const emailData = {
      to: clientData.email,
      subject: `【${clientData.companyName}】${reportType}レポート - GROWTH AI分析結果`,
      html: format === 'html' ? report.content : `
        <h2>${clientData.companyName} 様</h2>
        <p>${reportType}分析レポートを作成いたしました。</p>
        <p>詳細は添付ファイルをご確認ください。</p>
        <hr>
        <h3>📊 サマリー</h3>
        ${report.summary}
      `,
      attachments: format !== 'html' ? [
        {
          filename,
          path: reportPath
        }
      ] : undefined
    }

    console.log(`✅ レポート生成完了: ${filename}`)

    return NextResponse.json({
      success: true,
      clientId,
      reportType,
      format,
      filename,
      reportPath: `/projects/${clientId}/reports/${filename}`,
      emailData,
      metrics: report.metrics,
      summary: report.summary,
      recommendations: report.recommendations
    })

  } catch (error) {
    console.error('レポート生成エラー:', error)
    return NextResponse.json(
      { 
        error: 'レポート生成中にエラーが発生しました',
        details: error instanceof Error ? error.message : 'Unknown error'
      },
      { status: 500 }
    )
  }
}

// 分析データ収集
async function collectAnalyticsData(clientId: string, reportType: string): Promise<AnalyticsMetrics> {
  try {
    const analyticsDir = path.join(process.cwd(), 'projects', clientId, 'analytics')
    
    // 期間設定
    const endDate = new Date()
    const startDate = new Date()
    
    switch (reportType) {
      case 'daily':
        startDate.setDate(endDate.getDate() - 1)
        break
      case 'weekly':
        startDate.setDate(endDate.getDate() - 7)
        break
      case 'monthly':
        startDate.setDate(endDate.getDate() - 30)
        break
    }

    // 模擬データ（実際は実データを使用）
    const mockMetrics: AnalyticsMetrics = {
      pageViews: Math.floor(Math.random() * 1000) + 500,
      uniqueVisitors: Math.floor(Math.random() * 300) + 150,
      conversionRate: parseFloat((Math.random() * 5 + 2).toFixed(2)),
      averageSessionDuration: Math.floor(Math.random() * 300) + 120, // 秒
      bounceRate: parseFloat((Math.random() * 30 + 20).toFixed(1)),
      topPages: [
        { page: '/', views: Math.floor(Math.random() * 400) + 200 },
        { page: '/features', views: Math.floor(Math.random() * 200) + 100 },
        { page: '/contact', views: Math.floor(Math.random() * 150) + 75 }
      ],
      deviceBreakdown: {
        desktop: Math.floor(Math.random() * 200) + 100,
        mobile: Math.floor(Math.random() * 300) + 200,
        tablet: Math.floor(Math.random() * 50) + 25
      },
      trafficSources: [
        { source: 'Organic Search', visits: Math.floor(Math.random() * 200) + 100 },
        { source: 'Direct', visits: Math.floor(Math.random() * 150) + 75 },
        { source: 'Social Media', visits: Math.floor(Math.random() * 100) + 50 }
      ]
    }

    return mockMetrics
  } catch (error) {
    console.error('分析データ収集エラー:', error)
    throw error
  }
}

// レポート生成
async function generateReport(
  clientData: any, 
  metrics: AnalyticsMetrics, 
  reportType: string,
  format: string
) {
  const reportDate = new Date().toLocaleDateString('ja-JP')
  const periodText = reportType === 'daily' ? '日次' : reportType === 'weekly' ? '週次' : '月次'

  // HTML形式レポート
  const htmlContent = `
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="utf-8">
    <title>${clientData.companyName} ${periodText}分析レポート</title>
    <style>
        body { font-family: 'Helvetica Neue', Arial, sans-serif; margin: 0; padding: 2rem; background: #f8f9fa; }
        .report { max-width: 800px; margin: 0 auto; background: white; border-radius: 15px; padding: 3rem; box-shadow: 0 10px 30px rgba(0,0,0,0.1); }
        .header { text-align: center; margin-bottom: 3rem; border-bottom: 2px solid #007bff; padding-bottom: 2rem; }
        .metric-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1.5rem; margin: 2rem 0; }
        .metric-card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 1.5rem; border-radius: 10px; text-align: center; }
        .metric-value { font-size: 2rem; font-weight: 900; margin-bottom: 0.5rem; }
        .metric-label { font-size: 0.9rem; opacity: 0.9; }
        .chart-section { margin: 3rem 0; }
        .recommendations { background: #e3f2fd; padding: 2rem; border-radius: 10px; border-left: 4px solid #2196f3; }
        .recommendation-item { margin: 1rem 0; padding: 1rem; background: white; border-radius: 8px; }
    </style>
</head>
<body>
    <div class="report">
        <div class="header">
            <h1>${clientData.companyName}</h1>
            <h2>${periodText}分析レポート</h2>
            <p style="color: #666; margin-top: 1rem;">
                ${reportDate} | Generated by GROWTH AI
            </p>
        </div>

        <section>
            <h3>📊 主要指標</h3>
            <div class="metric-grid">
                <div class="metric-card">
                    <div class="metric-value">${metrics.pageViews.toLocaleString()}</div>
                    <div class="metric-label">ページビュー</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">${metrics.uniqueVisitors.toLocaleString()}</div>
                    <div class="metric-label">ユニークビジター</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">${metrics.conversionRate}%</div>
                    <div class="metric-label">コンバージョン率</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">${Math.floor(metrics.averageSessionDuration / 60)}m${metrics.averageSessionDuration % 60}s</div>
                    <div class="metric-label">平均滞在時間</div>
                </div>
            </div>
        </section>

        <section>
            <h3>📱 デバイス別分析</h3>
            <div style="background: #f8f9fa; padding: 1.5rem; border-radius: 10px;">
                <div style="display: flex; justify-content: space-between; margin: 1rem 0;">
                    <span>📱 モバイル:</span>
                    <strong>${metrics.deviceBreakdown.mobile} (${Math.round(metrics.deviceBreakdown.mobile / (metrics.deviceBreakdown.desktop + metrics.deviceBreakdown.mobile + metrics.deviceBreakdown.tablet) * 100)}%)</strong>
                </div>
                <div style="display: flex; justify-content: space-between; margin: 1rem 0;">
                    <span>💻 デスクトップ:</span>
                    <strong>${metrics.deviceBreakdown.desktop} (${Math.round(metrics.deviceBreakdown.desktop / (metrics.deviceBreakdown.desktop + metrics.deviceBreakdown.mobile + metrics.deviceBreakdown.tablet) * 100)}%)</strong>
                </div>
                <div style="display: flex; justify-content: space-between; margin: 1rem 0;">
                    <span>📟 タブレット:</span>
                    <strong>${metrics.deviceBreakdown.tablet} (${Math.round(metrics.deviceBreakdown.tablet / (metrics.deviceBreakdown.desktop + metrics.deviceBreakdown.mobile + metrics.deviceBreakdown.tablet) * 100)}%)</strong>
                </div>
            </div>
        </section>

        <section>
            <h3>🎯 人気ページ</h3>
            <div style="background: #f8f9fa; padding: 1.5rem; border-radius: 10px;">
                ${metrics.topPages.map(page => `
                    <div style="display: flex; justify-content: space-between; margin: 1rem 0; padding: 0.5rem 0; border-bottom: 1px solid #ddd;">
                        <span>${page.page}</span>
                        <strong>${page.views.toLocaleString()} views</strong>
                    </div>
                `).join('')}
            </div>
        </section>

        <section>
            <h3>🔄 トラフィック源</h3>
            <div style="background: #f8f9fa; padding: 1.5rem; border-radius: 10px;">
                ${metrics.trafficSources.map(source => `
                    <div style="display: flex; justify-content: space-between; margin: 1rem 0; padding: 0.5rem 0; border-bottom: 1px solid #ddd;">
                        <span>${source.source}</span>
                        <strong>${source.visits.toLocaleString()} visits</strong>
                    </div>
                `).join('')}
            </div>
        </section>

        <section class="recommendations">
            <h3>💡 AI改善提案</h3>
            <div class="recommendation-item">
                <h4>🎯 CVR向上施策</h4>
                <p>現在のCVR ${metrics.conversionRate}%から、以下の改善で<strong>+2-3%向上</strong>が期待できます：</p>
                <ul>
                    <li>CTAボタンの色・文言最適化</li>
                    <li>ファーストビューの改善</li>
                    <li>モバイル体験の最適化</li>
                </ul>
            </div>
            
            <div class="recommendation-item">
                <h4>📈 トラフィック増加施策</h4>
                <p>月間${metrics.pageViews.toLocaleString()}PVから<strong>+50%増加</strong>を目指す施策：</p>
                <ul>
                    <li>SEO最適化・コンテンツ拡充</li>
                    <li>SNS活用・シェア機能強化</li>
                    <li>リファラル・口コミ促進</li>
                </ul>
            </div>
            
            <div class="recommendation-item">
                <h4>🔮 次月予測</h4>
                <p>現在のトレンドが継続した場合の予測：</p>
                <ul>
                    <li>予想PV: <strong>${Math.round(metrics.pageViews * 1.2).toLocaleString()}</strong> (+20%)</li>
                    <li>予想CV: <strong>${Math.round(metrics.pageViews * (metrics.conversionRate / 100) * 1.5)}</strong>件 (+50%)</li>
                    <li>予想売上: <strong>¥${Math.round(metrics.pageViews * (metrics.conversionRate / 100) * 50000).toLocaleString()}</strong></li>
                </ul>
            </div>
        </section>

        <section style="margin-top: 3rem; text-align: center; color: #666;">
            <p>このレポートは GROWTH AI により自動生成されました</p>
            <p>詳細分析・カスタマイズ: support@growth-ai.com</p>
        </section>
    </div>
</body>
</html>`

  const aiInsights = generateAIInsights(metrics, clientData)

  return {
    content: format === 'html' ? htmlContent : JSON.stringify({
      clientData,
      metrics,
      insights: aiInsights,
      generatedAt: new Date().toISOString()
    }, null, 2),
    summary: generateSummary(metrics),
    metrics,
    recommendations: aiInsights,
    filename: `${reportType}-report-${new Date().toISOString().split('T')[0]}.${format}`
  }
}

// AI洞察生成
function generateAIInsights(metrics: AnalyticsMetrics, clientData: any): string[] {
  const insights: string[] = []

  // CVR分析
  if (metrics.conversionRate > 5) {
    insights.push('🎉 優秀なCVRです。この水準を維持し、さらなる最適化で業界トップレベルを目指しましょう。')
  } else if (metrics.conversionRate < 2) {
    insights.push('⚠️ CVRが低めです。ファーストビュー・CTAの改善を最優先で実施してください。')
  }

  // 滞在時間分析
  if (metrics.averageSessionDuration > 180) {
    insights.push('✅ 高いエンゲージメントです。コンテンツが効果的に機能しています。')
  } else if (metrics.averageSessionDuration < 60) {
    insights.push('📈 滞在時間の向上余地があります。コンテンツの魅力度向上を推奨します。')
  }

  // デバイス分析
  const totalDevices = metrics.deviceBreakdown.desktop + metrics.deviceBreakdown.mobile + metrics.deviceBreakdown.tablet
  const mobileRatio = metrics.deviceBreakdown.mobile / totalDevices
  
  if (mobileRatio > 0.7) {
    insights.push('📱 モバイルユーザーが多数。モバイル最適化に重点投資してください。')
  } else if (mobileRatio < 0.3) {
    insights.push('💻 デスクトップユーザー中心。詳細な情報提供で差別化してください。')
  }

  // トラフィック源分析
  const organicTraffic = metrics.trafficSources.find(s => s.source.includes('Organic'))
  if (organicTraffic && organicTraffic.visits > totalDevices * 0.4) {
    insights.push('🔍 SEOが効果的です。さらなるコンテンツ拡充で優位性を拡大してください。')
  }

  return insights
}

// サマリー生成
function generateSummary(metrics: AnalyticsMetrics): string {
  return `
    📊 PV: ${metrics.pageViews.toLocaleString()}
    👥 UV: ${metrics.uniqueVisitors.toLocaleString()}
    🎯 CVR: ${metrics.conversionRate}%
    ⏱️ 滞在: ${Math.floor(metrics.averageSessionDuration / 60)}分${metrics.averageSessionDuration % 60}秒
    📱 モバイル率: ${Math.round(metrics.deviceBreakdown.mobile / (metrics.deviceBreakdown.desktop + metrics.deviceBreakdown.mobile + metrics.deviceBreakdown.tablet) * 100)}%
  `
}