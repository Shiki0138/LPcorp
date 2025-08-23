import { NextRequest, NextResponse } from 'next/server'
import fs from 'fs/promises'
import path from 'path'

interface HeatmapData {
  clientId: string
  event: 'click' | 'scroll' | 'hover' | 'form_focus'
  x?: number
  y?: number
  element?: string
  className?: string
  scrollY?: number
  scrollPercent?: number
  timestamp: string
  sessionId: string
  userAgent: string
  referrer: string
  deviceType: 'desktop' | 'mobile' | 'tablet'
}

interface HeatmapAnalysis {
  totalClicks: number
  popularElements: Array<{
    element: string
    clicks: number
    percentage: number
  }>
  scrollAnalysis: {
    averageScrollDepth: number
    exitPoints: number[]
    engagementZones: Array<{
      startY: number
      endY: number
      engagement: number
    }>
  }
  deviceAnalysis: {
    desktop: number
    mobile: number
    tablet: number
  }
  recommendations: string[]
}

export async function POST(request: NextRequest) {
  try {
    const heatmapData: HeatmapData = await request.json()

    if (!heatmapData.clientId || !heatmapData.event) {
      return NextResponse.json(
        { error: 'clientIdとeventは必須です' },
        { status: 400 }
      )
    }

    // データ保存
    const projectDir = path.join(process.cwd(), 'projects', heatmapData.clientId)
    const analyticsDir = path.join(projectDir, 'analytics')
    await fs.mkdir(analyticsDir, { recursive: true })

    // 日付別ファイルに保存
    const today = new Date().toISOString().split('T')[0]
    const analyticsFile = path.join(analyticsDir, `heatmap-${today}.json`)

    let existingData: HeatmapData[] = []
    try {
      const rawData = await fs.readFile(analyticsFile, 'utf8')
      existingData = JSON.parse(rawData)
    } catch (error) {
      // ファイルが存在しない場合は新規作成
    }

    existingData.push({
      ...heatmapData,
      timestamp: new Date().toISOString()
    })

    await fs.writeFile(analyticsFile, JSON.stringify(existingData, null, 2), 'utf8')

    // リアルタイム分析実行
    const analysis = await generateHeatmapAnalysis(existingData)

    console.log(`📊 ヒートマップデータ記録: ${heatmapData.clientId} (${heatmapData.event})`)

    return NextResponse.json({
      success: true,
      message: 'ヒートマップデータを記録しました',
      analysis: analysis,
      dataPoints: existingData.length
    })

  } catch (error) {
    console.error('ヒートマップ記録エラー:', error)
    return NextResponse.json(
      { 
        error: 'データの記録に失敗しました',
        details: error instanceof Error ? error.message : 'Unknown error'
      },
      { status: 500 }
    )
  }
}

// ヒートマップ分析取得API
export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url)
    const clientId = searchParams.get('clientId')
    const date = searchParams.get('date') || new Date().toISOString().split('T')[0]

    if (!clientId) {
      return NextResponse.json(
        { error: 'clientIdが必要です' },
        { status: 400 }
      )
    }

    // データ読み込み
    const analyticsFile = path.join(process.cwd(), 'projects', clientId, 'analytics', `heatmap-${date}.json`)
    
    let heatmapData: HeatmapData[] = []
    try {
      const rawData = await fs.readFile(analyticsFile, 'utf8')
      heatmapData = JSON.parse(rawData)
    } catch (error) {
      // データがない場合は空配列
    }

    // 分析実行
    const analysis = await generateHeatmapAnalysis(heatmapData)

    return NextResponse.json({
      success: true,
      clientId,
      date,
      dataPoints: heatmapData.length,
      analysis,
      rawData: heatmapData
    })

  } catch (error) {
    console.error('ヒートマップ分析エラー:', error)
    return NextResponse.json(
      { error: '分析の取得に失敗しました' },
      { status: 500 }
    )
  }
}

// ヒートマップ分析生成
async function generateHeatmapAnalysis(data: HeatmapData[]): Promise<HeatmapAnalysis> {
  const clickData = data.filter(d => d.event === 'click')
  const scrollData = data.filter(d => d.event === 'scroll')

  // クリック分析
  const elementClicks = clickData.reduce((acc, click) => {
    const element = click.element || 'unknown'
    acc[element] = (acc[element] || 0) + 1
    return acc
  }, {} as Record<string, number>)

  const totalClicks = clickData.length
  const popularElements = Object.entries(elementClicks)
    .map(([element, clicks]) => ({
      element,
      clicks,
      percentage: Math.round((clicks / totalClicks) * 100)
    }))
    .sort((a, b) => b.clicks - a.clicks)
    .slice(0, 10)

  // スクロール分析
  const scrollDepths = scrollData.map(d => d.scrollPercent || 0).filter(d => d > 0)
  const averageScrollDepth = scrollDepths.length > 0 
    ? Math.round(scrollDepths.reduce((a, b) => a + b, 0) / scrollDepths.length)
    : 0

  // デバイス分析
  const deviceCounts = data.reduce((acc, item) => {
    acc[item.deviceType] = (acc[item.deviceType] || 0) + 1
    return acc
  }, {} as Record<string, number>)

  // 改善提案生成
  const recommendations = generateRecommendations(popularElements, averageScrollDepth, deviceCounts)

  return {
    totalClicks,
    popularElements,
    scrollAnalysis: {
      averageScrollDepth,
      exitPoints: [25, 50, 75], // パーセンテージ
      engagementZones: [
        { startY: 0, endY: 500, engagement: 90 },
        { startY: 500, endY: 1500, engagement: 65 },
        { startY: 1500, endY: 3000, engagement: 40 }
      ]
    },
    deviceAnalysis: {
      desktop: deviceCounts.desktop || 0,
      mobile: deviceCounts.mobile || 0,
      tablet: deviceCounts.tablet || 0
    },
    recommendations
  }
}

// AI改善提案生成
function generateRecommendations(
  popularElements: Array<{ element: string; clicks: number; percentage: number }>,
  scrollDepth: number,
  deviceCounts: Record<string, number>
): string[] {
  const recommendations: string[] = []

  // クリック分析に基づく提案
  if (popularElements.length > 0) {
    const topElement = popularElements[0]
    if (topElement.percentage > 30) {
      recommendations.push(`${topElement.element}要素が非常に注目されています。この要素の最適化でCVR向上が期待できます。`)
    }
  }

  // スクロール深度に基づく提案
  if (scrollDepth < 50) {
    recommendations.push('平均スクロール深度が低いです。ファーストビューの改善で離脱率を下げられます。')
  } else if (scrollDepth > 80) {
    recommendations.push('スクロール深度が高く、コンテンツへの関心が高いです。CTA配置の最適化を推奨します。')
  }

  // デバイス分析に基づく提案
  const totalDevices = Object.values(deviceCounts).reduce((a, b) => a + b, 0)
  if (totalDevices > 0) {
    const mobileRatio = (deviceCounts.mobile || 0) / totalDevices
    if (mobileRatio > 0.6) {
      recommendations.push('モバイルユーザーが多いです。モバイル最適化を重点的に実施してください。')
    }
  }

  // デフォルト提案
  if (recommendations.length === 0) {
    recommendations.push('データ収集を継続し、より詳細な分析を実施します。')
  }

  return recommendations
}