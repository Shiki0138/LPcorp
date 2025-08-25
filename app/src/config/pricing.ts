// GROWTH AI LP制作サービス - 最適化価格設定

export interface PricingTier {
  id: string
  name: string
  price: number
  originalPrice?: number
  description: string
  features: string[]
  testPatterns: number
  analyticsLevel: 'basic' | 'standard' | 'premium'
  support: 'email' | 'email+chat' | 'dedicated'
  deliveryTime: string
  revisions: number
  target: string[]
}

export interface AdditionalService {
  id: string
  name: string
  price: number
  description: string
  unit: 'per_month' | 'per_project' | 'per_result'
}

// メインLP制作プラン
export const LP_PRICING_TIERS: PricingTier[] = [
  {
    id: 'basic',
    name: 'ベーシックLP制作',
    price: 19800,
    originalPrice: 49800,
    description: 'AI自動生成による高品質LP制作。個人事業主・小企業向け。',
    features: [
      'AI自動LP生成（30秒完成）',
      '基本デザインテンプレート適用',
      'レスポンシブ対応',
      '基本SEO最適化',
      '基本分析機能搭載',
      'Google Analytics設定',
      '1回まで修正対応'
    ],
    testPatterns: 1,
    analyticsLevel: 'basic',
    support: 'email',
    deliveryTime: '30秒〜1時間',
    revisions: 1,
    target: ['個人事業主', '小企業', 'スタートアップ']
  },
  {
    id: 'standard',
    name: 'スタンダードLP制作',
    price: 49800,
    originalPrice: 98000,
    description: 'A/Bテスト対応＋詳細分析。中小企業の本格運用向け。',
    features: [
      'AI自動LP生成＋最適化',
      'A/Bテスト2パターン作成',
      'ヒートマップ分析機能',
      'CVR・ROI詳細分析',
      'モバイル完全最適化',
      'SEO・アクセシビリティ対応',
      '3回まで修正対応',
      '1ヶ月間改善サポート'
    ],
    testPatterns: 2,
    analyticsLevel: 'standard',
    support: 'email+chat',
    deliveryTime: '1-3時間',
    revisions: 3,
    target: ['中小企業', '成長企業', 'EC事業者']
  },
  {
    id: 'premium',
    name: 'プレミアムLP制作',
    price: 98000,
    originalPrice: 198000,
    description: 'A/B/Cテスト＋プロ監修＋専属サポート。大企業・重要案件向け。',
    features: [
      'AI生成＋プロデザイナー監修',
      'A/B/Cテスト3パターン作成',
      '高度ヒートマップ・行動分析',
      '競合分析・差別化提案',
      'カスタムデザイン・ブランディング',
      'コンバージョン最適化',
      '無制限修正対応',
      '3ヶ月間専属サポート',
      '成果保証制度'
    ],
    testPatterns: 3,
    analyticsLevel: 'premium',
    support: 'dedicated',
    deliveryTime: '2-6時間',
    revisions: -1, // 無制限
    target: ['中堅企業', '大企業', '重要プロジェクト']
  }
]

// 継続サービス・アップセル
export const ADDITIONAL_SERVICES: AdditionalService[] = [
  {
    id: 'monthly_optimization',
    name: '月次最適化サービス',
    price: 29800,
    description: 'AI分析による継続的な改善・最適化。月次レポート・改善実装込み。',
    unit: 'per_month'
  },
  {
    id: 'analytics_pro',
    name: 'プロ分析レポート',
    price: 19800,
    description: '詳細なユーザー行動分析・改善提案レポート。月次配信。',
    unit: 'per_month'
  },
  {
    id: 'strategy_consulting',
    name: '戦略コンサルティング',
    price: 98000,
    description: 'マーケティング戦略立案・実行支援。月2回のミーティング込み。',
    unit: 'per_month'
  },
  {
    id: 'emergency_support',
    name: '緊急対応サポート',
    price: 50000,
    description: '24時間以内の緊急修正・対応。重要イベント・キャンペーン向け。',
    unit: 'per_project'
  },
  {
    id: 'performance_bonus',
    name: '成果連動報酬',
    price: 0, // 基本料金なし
    description: 'CVR改善分の20%を12ヶ月間。確実な成果にコミット。',
    unit: 'per_result'
  }
]

// パッケージ割引
export const PACKAGE_DEALS = [
  {
    id: 'startup_pack',
    name: 'スタートアップパック',
    items: ['LP制作3枚', '6ヶ月分析サポート'],
    regularPrice: 149400 + 178800,
    packagePrice: 248000,
    discount: 80200,
    description: '事業立ち上げに必要なLP一式＋継続改善'
  },
  {
    id: 'enterprise_pack', 
    name: 'エンタープライズパック',
    items: ['プレミアムLP5枚', '12ヶ月戦略コンサル', '専属サポート'],
    regularPrice: 490000 + 1176000,
    packagePrice: 1280000,
    discount: 386000,
    description: '大企業向け包括的マーケティングソリューション'
  }
]

// 特別キャンペーン
export const CAMPAIGNS = [
  {
    id: 'launch_campaign',
    name: '正式ローンチ記念',
    discount: 0.5, // 50%OFF
    condition: '先着30社限定',
    validUntil: '2025-12-31',
    applicable: ['basic', 'standard']
  },
  {
    id: 'referral_bonus',
    name: '紹介キャンペーン',
    discount: 10000, // ¥10,000割引
    condition: 'ご紹介1件につき',
    description: '紹介者・被紹介者両方に割引適用'
  }
]

// 価格計算ユーティリティ
export function calculatePrice(
  tierId: string, 
  additionalServices: string[] = [],
  campaign?: string
): {
  basePrice: number
  additionalPrice: number
  discount: number
  finalPrice: number
  breakdown: any[]
} {
  const tier = LP_PRICING_TIERS.find(t => t.id === tierId)
  if (!tier) throw new Error('Invalid tier ID')

  let basePrice = tier.price
  let additionalPrice = 0
  let discount = 0
  const breakdown = []

  // 基本料金
  breakdown.push({
    item: tier.name,
    price: basePrice,
    type: 'base'
  })

  // 追加サービス
  additionalServices.forEach(serviceId => {
    const service = ADDITIONAL_SERVICES.find(s => s.id === serviceId)
    if (service) {
      additionalPrice += service.price
      breakdown.push({
        item: service.name,
        price: service.price,
        type: 'additional'
      })
    }
  })

  // キャンペーン割引
  if (campaign) {
    const campaignInfo = CAMPAIGNS.find(c => c.id === campaign)
    if (campaignInfo) {
      if (typeof campaignInfo.discount === 'number' && campaignInfo.discount < 1) {
        // パーセンテージ割引
        discount = Math.floor(basePrice * campaignInfo.discount)
      } else {
        // 固定額割引
        discount = campaignInfo.discount as number
      }
      breakdown.push({
        item: campaignInfo.name,
        price: -discount,
        type: 'discount'
      })
    }
  }

  const finalPrice = basePrice + additionalPrice - discount

  return {
    basePrice,
    additionalPrice,
    discount,
    finalPrice,
    breakdown
  }
}

// 見積もり自動計算
export function generateQuote(requirements: {
  companySize: 'small' | 'medium' | 'large'
  industry: string
  budget: string
  urgency: 'normal' | 'urgent' | 'emergency'
  features: string[]
}) {
  // 企業規模に基づく推奨プラン
  let recommendedTier: string
  
  switch (requirements.companySize) {
    case 'small':
      recommendedTier = 'basic'
      break
    case 'medium':
      recommendedTier = 'standard'  
      break
    case 'large':
      recommendedTier = 'premium'
      break
    default:
      recommendedTier = 'standard'
  }

  // 追加サービス推奨
  const recommendedServices = []
  
  if (requirements.urgency === 'urgent') {
    recommendedServices.push('emergency_support')
  }
  
  if (requirements.features.includes('継続改善')) {
    recommendedServices.push('monthly_optimization')
  }
  
  if (requirements.features.includes('詳細分析')) {
    recommendedServices.push('analytics_pro')
  }

  // キャンペーン適用判定
  let appliedCampaign = undefined
  if (['basic', 'standard'].includes(recommendedTier)) {
    appliedCampaign = 'launch_campaign'
  }

  const quote = calculatePrice(recommendedTier, recommendedServices, appliedCampaign)
  
  return {
    ...quote,
    recommendedTier,
    recommendedServices,
    appliedCampaign,
    tier: LP_PRICING_TIERS.find(t => t.id === recommendedTier),
    estimatedDelivery: calculateDeliveryTime(recommendedTier, requirements.urgency),
    expectedResults: generateExpectedResults(recommendedTier, requirements.industry)
  }
}

function calculateDeliveryTime(tierId: string, urgency: string): string {
  const tier = LP_PRICING_TIERS.find(t => t.id === tierId)
  if (!tier) return '1-3時間'
  
  if (urgency === 'emergency') return '30分以内'
  if (urgency === 'urgent') return '1時間以内'
  
  return tier.deliveryTime
}

function generateExpectedResults(tierId: string, industry: string) {
  const baseResults = {
    basic: { cvrImprovement: '15-25%', timeToLive: '即座', roiExpected: '200-400%' },
    standard: { cvrImprovement: '25-40%', timeToLive: '2-4週間', roiExpected: '400-800%' },
    premium: { cvrImprovement: '35-60%', timeToLive: '1-3ヶ月', roiExpected: '800-1500%' }
  }
  
  return baseResults[tierId as keyof typeof baseResults] || baseResults.standard
}