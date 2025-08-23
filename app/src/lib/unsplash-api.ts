// Unsplash API統合 - 自動画像取得システム

const UNSPLASH_ACCESS_KEY = process.env.UNSPLASH_ACCESS_KEY || 'demo-key'
const UNSPLASH_API_URL = 'https://api.unsplash.com'

interface UnsplashImage {
  id: string
  urls: {
    raw: string
    full: string
    regular: string
    small: string
    thumb: string
  }
  alt_description: string
  description: string
  user: {
    name: string
    username: string
  }
  downloads: number
  likes: number
}

interface ImageRecommendation {
  category: 'hero' | 'feature' | 'testimonial' | 'team' | 'background'
  query: string
  count: number
  priority: 'high' | 'medium' | 'low'
  usage: string
}

export class UnsplashService {
  
  // 業界・商品に基づく画像自動取得
  static async getRecommendedImages(
    industry: string, 
    productName: string, 
    targetAudience: string
  ): Promise<UnsplashImage[]> {
    try {
      const recommendations = this.generateImageRecommendations(industry, productName, targetAudience)
      const allImages: UnsplashImage[] = []
      
      for (const rec of recommendations) {
        const images = await this.searchImages(rec.query, rec.count)
        allImages.push(...images.map(img => ({
          ...img,
          category: rec.category,
          priority: rec.priority,
          usage: rec.usage
        } as any)))
      }
      
      return allImages
    } catch (error) {
      console.error('Unsplash画像取得エラー:', error)
      return []
    }
  }

  // 画像検索API
  static async searchImages(query: string, count: number = 5): Promise<UnsplashImage[]> {
    try {
      const response = await fetch(
        `${UNSPLASH_API_URL}/search/photos?query=${encodeURIComponent(query)}&per_page=${count}&order_by=popularity`,
        {
          headers: {
            'Authorization': `Client-ID ${UNSPLASH_ACCESS_KEY}`,
            'Content-Type': 'application/json'
          }
        }
      )

      if (!response.ok) {
        throw new Error(`Unsplash API Error: ${response.status}`)
      }

      const data = await response.json()
      return data.results || []
    } catch (error) {
      console.error('Unsplash検索エラー:', error)
      
      // フォールバック: Picsum Photos
      return this.getPicsumFallback(count)
    }
  }

  // 業界別画像推奨リスト生成
  static generateImageRecommendations(
    industry: string, 
    productName: string, 
    targetAudience: string
  ): ImageRecommendation[] {
    const baseRecommendations: ImageRecommendation[] = [
      {
        category: 'hero',
        query: `${industry} professional technology`,
        count: 3,
        priority: 'high',
        usage: 'メインビジュアル・ヒーローセクション'
      },
      {
        category: 'testimonial',
        query: `business professional ${targetAudience}`,
        count: 5,
        priority: 'high',
        usage: 'お客様の声・信頼性構築'
      },
      {
        category: 'feature',
        query: `${productName} dashboard interface`,
        count: 4,
        priority: 'medium',
        usage: '機能説明・デモ画像'
      }
    ]

    // 業界特化の追加推奨
    const industrySpecific = this.getIndustrySpecificImages(industry)
    
    return [...baseRecommendations, ...industrySpecific]
  }

  // 業界特化画像推奨
  static getIndustrySpecificImages(industry: string): ImageRecommendation[] {
    const industryMap: Record<string, ImageRecommendation[]> = {
      'IT・SaaS': [
        {
          category: 'team',
          query: 'software engineers coding',
          count: 3,
          priority: 'medium',
          usage: 'チーム紹介・技術力アピール'
        },
        {
          category: 'background',
          query: 'modern tech office',
          count: 2,
          priority: 'low',
          usage: '背景・セクション装飾'
        }
      ],
      'ヘルスケア': [
        {
          category: 'team',
          query: 'medical professionals healthcare',
          count: 3,
          priority: 'medium',
          usage: '医療従事者・専門性アピール'
        },
        {
          category: 'background',
          query: 'clean medical facility',
          count: 2,
          priority: 'low',
          usage: 'クリーンなイメージ・信頼性'
        }
      ],
      '教育': [
        {
          category: 'team',
          query: 'teachers students learning',
          count: 3,
          priority: 'medium',
          usage: '教育現場・学習効果'
        }
      ]
    }
    
    return industryMap[industry] || []
  }

  // フォールバック用（Picsum Photos）
  static getPicsumFallback(count: number): UnsplashImage[] {
    return Array.from({ length: count }, (_, i) => ({
      id: `picsum-${i}`,
      urls: {
        raw: `https://picsum.photos/1920/1080?random=${i}`,
        full: `https://picsum.photos/1920/1080?random=${i}`,
        regular: `https://picsum.photos/800/600?random=${i}`,
        small: `https://picsum.photos/400/300?random=${i}`,
        thumb: `https://picsum.photos/200/150?random=${i}`
      },
      alt_description: 'Professional image',
      description: 'High quality stock photo',
      user: {
        name: 'Picsum Photos',
        username: 'picsum'
      },
      downloads: 1000,
      likes: 100
    }))
  }

  // 画像ダウンロード・保存
  static async downloadAndSaveImages(
    images: UnsplashImage[],
    clientId: string
  ): Promise<string[]> {
    try {
      const projectDir = path.join(process.cwd(), 'projects', clientId, 'images')
      const savedPaths: string[] = []
      
      for (let i = 0; i < images.length; i++) {
        const image = images[i]
        const filename = `${image.category || 'general'}_${i + 1}_${image.id}.jpg`
        const filepath = path.join(projectDir, filename)
        
        try {
          // 画像ダウンロード
          const response = await fetch(image.urls.regular)
          if (response.ok) {
            const buffer = await response.arrayBuffer()
            await fs.writeFile(filepath, Buffer.from(buffer))
            savedPaths.push(filepath)
            
            console.log(`✅ 画像保存完了: ${filename}`)
          }
        } catch (error) {
          console.error(`画像保存エラー ${filename}:`, error)
        }
      }
      
      return savedPaths
    } catch (error) {
      console.error('画像一括保存エラー:', error)
      return []
    }
  }

  // AI画像選定（最適画像を自動選択）
  static selectOptimalImages(
    images: UnsplashImage[],
    industry: string,
    targetAudience: string
  ): UnsplashImage[] {
    // AI評価スコア計算
    const scoredImages = images.map(image => ({
      ...image,
      aiScore: this.calculateImageScore(image, industry, targetAudience)
    }))

    // スコア順でソート
    return scoredImages
      .sort((a, b) => b.aiScore - a.aiScore)
      .slice(0, 8) // 上位8枚を選定
  }

  // AI画像評価スコア計算
  static calculateImageScore(
    image: UnsplashImage,
    industry: string,
    targetAudience: string
  ): number {
    let score = 0
    
    // 人気度による加点
    score += Math.min(image.likes / 100, 5) // 最大5点
    score += Math.min(image.downloads / 1000, 3) // 最大3点
    
    // 説明文による関連度判定
    const description = (image.description || image.alt_description || '').toLowerCase()
    
    if (description.includes('professional')) score += 2
    if (description.includes('business')) score += 2
    if (description.includes(industry.toLowerCase())) score += 3
    if (description.includes('modern')) score += 1
    if (description.includes('team')) score += 1
    
    return score
  }
}

// path import for server environment
import path from 'path'
import fs from 'fs/promises'