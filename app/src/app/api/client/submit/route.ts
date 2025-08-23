import { NextRequest, NextResponse } from 'next/server'
import fs from 'fs/promises'
import path from 'path'

interface ClientSubmission {
  // 基本情報
  companyName: string
  contactName: string
  email: string
  phone: string
  industry: string
  companySize: string
  
  // 商品・サービス情報
  productName: string
  productDescription: string
  productPrice: string
  targetAge: string
  targetGender: string
  targetOccupation: string
  
  // 目標・要件
  monthlyGoalCV: string
  currentCVR: string
  budget: string
  deadline: string
  competitors: string
  specialRequests: string
  
  // システム情報
  submittedAt: string
  clientId: string
  status: 'submitted' | 'analyzing' | 'completed'
}

export async function POST(request: NextRequest) {
  try {
    const formData: Omit<ClientSubmission, 'submittedAt' | 'clientId' | 'status'> = await request.json()
    
    // クライアントID生成（タイムスタンプベース）
    const clientId = `client_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    
    // 完全なデータ構造
    const submission: ClientSubmission = {
      ...formData,
      submittedAt: new Date().toISOString(),
      clientId,
      status: 'submitted'
    }

    // プロジェクトディレクトリ作成
    const projectDir = path.join(process.cwd(), 'projects', clientId)
    await fs.mkdir(projectDir, { recursive: true })
    
    // サブディレクトリ作成
    await fs.mkdir(path.join(projectDir, 'images'), { recursive: true })
    await fs.mkdir(path.join(projectDir, 'generated'), { recursive: true })
    await fs.mkdir(path.join(projectDir, 'reports'), { recursive: true })

    // 依頼者データ保存
    await fs.writeFile(
      path.join(projectDir, 'client-data.json'),
      JSON.stringify(submission, null, 2),
      'utf8'
    )

    // AI分析結果の初期ファイル作成
    const aiAnalysisTemplate = {
      clientId,
      analyzedAt: new Date().toISOString(),
      recommendations: {
        lpPattern: 'analyzing...',
        targetingStrategy: 'analyzing...',
        competitiveAnalysis: 'analyzing...',
        contentStrategy: 'analyzing...',
        designRecommendations: 'analyzing...',
        expectedCVR: 'calculating...',
        requiredImages: []
      },
      status: 'in_progress'
    }

    await fs.writeFile(
      path.join(projectDir, 'ai-analysis.json'),
      JSON.stringify(aiAnalysisTemplate, null, 2),
      'utf8'
    )

    // 管理者用一覧に追加
    const projectListPath = path.join(process.cwd(), 'projects', 'project-list.json')
    let projectList = []
    
    try {
      const existingList = await fs.readFile(projectListPath, 'utf8')
      projectList = JSON.parse(existingList)
    } catch (error) {
      // ファイルが存在しない場合は新規作成
    }

    projectList.push({
      clientId,
      companyName: submission.companyName,
      contactName: submission.contactName,
      email: submission.email,
      industry: submission.industry,
      submittedAt: submission.submittedAt,
      status: 'submitted',
      priority: calculatePriority(submission),
      estimatedValue: estimateProjectValue(submission)
    })

    await fs.writeFile(projectListPath, JSON.stringify(projectList, null, 2), 'utf8')

    console.log(`✅ 新規プロジェクト作成: ${clientId}`)
    console.log(`📁 保存先: /projects/${clientId}/`)
    console.log(`📧 依頼者: ${submission.email}`)

    return NextResponse.json({
      success: true,
      clientId,
      message: 'プロジェクトを作成しました。AI分析を開始します。',
      projectPath: `/projects/${clientId}`,
      nextSteps: [
        'AI要件分析（2分）',
        '競合調査・市場分析（3分）',
        '改善提案・戦略策定（5分）',
        'メール送信・連絡（即座）'
      ]
    })

  } catch (error) {
    console.error('フォーム送信エラー:', error)
    return NextResponse.json(
      { 
        success: false, 
        error: 'データの保存に失敗しました',
        details: error instanceof Error ? error.message : 'Unknown error'
      },
      { status: 500 }
    )
  }
}

// プロジェクト優先度計算
function calculatePriority(submission: ClientSubmission): 'high' | 'medium' | 'low' {
  let score = 0
  
  // 予算による加点
  if (submission.budget.includes('100万円以上')) score += 3
  else if (submission.budget.includes('50-100万円')) score += 2
  else if (submission.budget.includes('30-50万円')) score += 1
  
  // 会社規模による加点
  if (submission.companySize.includes('1000名以上')) score += 3
  else if (submission.companySize.includes('201-1000名')) score += 2
  else if (submission.companySize.includes('51-200名')) score += 1
  
  // 納期による加点
  if (submission.deadline.includes('即日')) score += 2
  else if (submission.deadline.includes('1週間')) score += 1
  
  if (score >= 5) return 'high'
  if (score >= 2) return 'medium'
  return 'low'
}

// プロジェクト価値推定
function estimateProjectValue(submission: ClientSubmission): number {
  let baseValue = 50000 // 基本価格5万円
  
  // 会社規模による調整
  if (submission.companySize.includes('1000名以上')) baseValue *= 6
  else if (submission.companySize.includes('201-1000名')) baseValue *= 4
  else if (submission.companySize.includes('51-200名')) baseValue *= 2
  
  // 予算による調整
  if (submission.budget.includes('100万円以上')) baseValue = Math.max(baseValue, 300000)
  else if (submission.budget.includes('50-100万円')) baseValue = Math.max(baseValue, 150000)
  
  return baseValue
}