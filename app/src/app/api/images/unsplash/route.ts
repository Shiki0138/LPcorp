import { NextRequest, NextResponse } from 'next/server'

const UNSPLASH_ACCESS_KEY = process.env.UNSPLASH_ACCESS_KEY || 'demo-key'
const UNSPLASH_API_URL = 'https://api.unsplash.com'

interface UnsplashSearchRequest {
  query: string
  count?: number
  category?: string
  orientation?: 'landscape' | 'portrait' | 'squarish'
}

export async function POST(request: NextRequest) {
  try {
    const { query, count = 8, category, orientation = 'landscape' }: UnsplashSearchRequest = await request.json()

    if (!query) {
      return NextResponse.json(
        { error: '検索クエリが必要です' },
        { status: 400 }
      )
    }

    // Unsplash API呼び出し
    const searchUrl = new URL(`${UNSPLASH_API_URL}/search/photos`)
    searchUrl.searchParams.set('query', query)
    searchUrl.searchParams.set('per_page', Math.min(count, 30).toString())
    searchUrl.searchParams.set('order_by', 'popularity')
    if (orientation) {
      searchUrl.searchParams.set('orientation', orientation)
    }

    const response = await fetch(searchUrl.toString(), {
      headers: {
        'Authorization': `Client-ID ${UNSPLASH_ACCESS_KEY}`,
        'Accept': 'application/json',
      },
    })

    if (!response.ok) {
      console.error('Unsplash API エラー:', response.status, response.statusText)
      
      // フォールバック: Picsum Photos
      return NextResponse.json({
        success: true,
        images: generatePicsumFallback(count, query),
        source: 'picsum',
        message: 'Unsplash APIが利用できないため、代替画像を提供しています'
      })
    }

    const data = await response.json()
    const images = data.results || []

    // 画像データを整形
    const formattedImages = images.map((img: any, index: number) => ({
      id: img.id,
      urls: {
        raw: img.urls.raw,
        full: img.urls.full,
        regular: img.urls.regular,
        small: img.urls.small,
        thumb: img.urls.thumb,
      },
      alt_description: img.alt_description || img.description || `${query} image ${index + 1}`,
      description: img.description,
      user: {
        name: img.user.name,
        username: img.user.username,
        profile_url: `https://unsplash.com/@${img.user.username}`,
      },
      download_url: img.links?.download,
      likes: img.likes,
      downloads: img.downloads,
      category: category || 'general',
      ai_score: calculateAIScore(img, query, category),
    }))

    // AI スコア順でソート
    const sortedImages = formattedImages.sort((a, b) => b.ai_score - a.ai_score)

    console.log(`✅ Unsplash画像取得成功: ${sortedImages.length}枚 (${query})`)

    return NextResponse.json({
      success: true,
      images: sortedImages,
      total: data.total,
      total_pages: data.total_pages,
      source: 'unsplash',
      query,
      recommendations: generateImageRecommendations(sortedImages, query, category)
    })

  } catch (error) {
    console.error('Unsplash API統合エラー:', error)
    
    // 完全フォールバック
    return NextResponse.json({
      success: true,
      images: generatePicsumFallback(count || 8, query),
      source: 'picsum',
      message: 'エラーが発生したため、代替画像を提供しています',
      error: error instanceof Error ? error.message : 'Unknown error'
    })
  }
}

// AI画像評価スコア計算
function calculateAIScore(image: any, query: string, category?: string): number {
  let score = 0
  
  // 人気度による加点
  score += Math.min(image.likes / 100, 5) // 最大5点
  score += Math.min((image.downloads || 0) / 1000, 3) // 最大3点
  
  // 説明文による関連度判定
  const description = (image.description || image.alt_description || '').toLowerCase()
  const queryLower = query.toLowerCase()
  
  // キーワードマッチング
  if (description.includes(queryLower)) score += 5
  if (description.includes('professional')) score += 3
  if (description.includes('business')) score += 3
  if (description.includes('modern')) score += 2
  if (description.includes('clean')) score += 2
  if (description.includes('high quality')) score += 2
  
  // カテゴリ特化加点
  if (category) {
    const categoryKeywords = getCategoryKeywords(category)
    categoryKeywords.forEach(keyword => {
      if (description.includes(keyword)) score += 2
    })
  }
  
  return score
}

// カテゴリ別キーワード
function getCategoryKeywords(category: string): string[] {
  const keywordMap: Record<string, string[]> = {
    'hero': ['main', 'banner', 'header', 'primary'],
    'testimonial': ['person', 'people', 'portrait', 'customer', 'user'],
    'feature': ['product', 'interface', 'dashboard', 'screen', 'app'],
    'team': ['team', 'group', 'meeting', 'collaboration', 'office'],
    'background': ['abstract', 'pattern', 'texture', 'backdrop']
  }
  
  return keywordMap[category] || []
}

// 画像推奨システム
function generateImageRecommendations(images: any[], query: string, category?: string) {
  const recommendations = {
    best_for_hero: images.filter(img => img.ai_score > 8).slice(0, 3),
    high_engagement: images.filter(img => img.likes > 100).slice(0, 3),
    professional_quality: images.filter(img => 
      (img.description || '').toLowerCase().includes('professional')
    ).slice(0, 3),
    usage_tips: [
      'ヒーロー画像は高解像度（1920×1080以上）を推奨',
      'お客様の声には人物画像が効果的',
      '機能説明にはスクリーンショット・UI画像を活用',
      'ブランドカラーと調和する画像を選択'
    ]
  }
  
  return recommendations
}

// Picsum Photos フォールバック
function generatePicsumFallback(count: number, query: string) {
  return Array.from({ length: count }, (_, i) => {
    const seed = hashString(query + i.toString())
    return {
      id: `picsum-${seed}-${i}`,
      urls: {
        raw: `https://picsum.photos/seed/${seed}/1920/1080`,
        full: `https://picsum.photos/seed/${seed}/1920/1080`,
        regular: `https://picsum.photos/seed/${seed}/800/600`,
        small: `https://picsum.photos/seed/${seed}/400/300`,
        thumb: `https://picsum.photos/seed/${seed}/200/150`,
      },
      alt_description: `${query} professional image ${i + 1}`,
      description: `High quality ${query} stock photo`,
      user: {
        name: 'Picsum Photos',
        username: 'picsum',
        profile_url: 'https://picsum.photos',
      },
      likes: Math.floor(Math.random() * 500) + 100,
      downloads: Math.floor(Math.random() * 5000) + 1000,
      category: 'general',
      ai_score: 6 + Math.random() * 2, // 6-8点のランダムスコア
    }
  })
}

// 簡単なハッシュ関数
function hashString(str: string): string {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i)
    hash = ((hash << 5) - hash) + char
    hash = hash & hash // 32bit整数に変換
  }
  return Math.abs(hash).toString(36)
}