'use client'

import React, { useState, useEffect } from 'react'

interface UnsplashImage {
  id: string
  urls: {
    regular: string
    small: string
    thumb: string
  }
  alt_description: string
  user: {
    name: string
    username: string
  }
  likes: number
  ai_score: number
  category?: string
  usage?: string
}

interface ImageSelectorProps {
  industry: string
  productName: string
  targetAudience: string
  onImagesSelected: (images: UnsplashImage[]) => void
}

export default function ImageSelector({ 
  industry, 
  productName, 
  targetAudience, 
  onImagesSelected 
}: ImageSelectorProps) {
  const [availableImages, setAvailableImages] = useState<UnsplashImage[]>([])
  const [selectedImages, setSelectedImages] = useState<UnsplashImage[]>([])
  const [loading, setLoading] = useState(false)
  const [currentCategory, setCurrentCategory] = useState<string>('all')

  useEffect(() => {
    loadRecommendedImages()
  }, [industry, productName])

  const loadRecommendedImages = async () => {
    setLoading(true)
    
    try {
      // 業界・商品に基づく複数クエリで画像取得
      const queries = generateImageQueries(industry, productName, targetAudience)
      const allImages: UnsplashImage[] = []
      
      for (const queryConfig of queries) {
        try {
          const response = await fetch('/api/images/unsplash', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              query: queryConfig.query,
              count: queryConfig.count,
              category: queryConfig.category
            })
          })
          
          const result = await response.json()
          
          if (result.success && result.images) {
            // カテゴリ情報を追加
            const categorizedImages = result.images.map((img: UnsplashImage) => ({
              ...img,
              category: queryConfig.category,
              usage: queryConfig.usage,
              priority: queryConfig.priority
            }))
            
            allImages.push(...categorizedImages)
          }
        } catch (error) {
          console.error(`画像取得エラー (${queryConfig.query}):`, error)
        }
      }
      
      // 重複除去・スコア順ソート
      const uniqueImages = removeDuplicates(allImages)
      const sortedImages = uniqueImages.sort((a, b) => b.ai_score - a.ai_score)
      
      setAvailableImages(sortedImages)
      console.log(`✅ 画像取得完了: ${sortedImages.length}枚`)
      
    } catch (error) {
      console.error('画像読み込みエラー:', error)
    } finally {
      setLoading(false)
    }
  }

  const toggleImageSelection = (image: UnsplashImage) => {
    const isSelected = selectedImages.find(img => img.id === image.id)
    
    if (isSelected) {
      const newSelection = selectedImages.filter(img => img.id !== image.id)
      setSelectedImages(newSelection)
      onImagesSelected(newSelection)
    } else {
      if (selectedImages.length < 8) { // 最大8枚まで
        const newSelection = [...selectedImages, image]
        setSelectedImages(newSelection)
        onImagesSelected(newSelection)
      } else {
        alert('画像は最大8枚まで選択できます')
      }
    }
  }

  const filteredImages = currentCategory === 'all' 
    ? availableImages 
    : availableImages.filter(img => img.category === currentCategory)

  const categories = [
    { key: 'all', label: '全て', icon: '🖼️' },
    { key: 'hero', label: 'メイン', icon: '🎯' },
    { key: 'feature', label: '機能', icon: '⚙️' },
    { key: 'testimonial', label: '人物', icon: '👤' },
    { key: 'team', label: 'チーム', icon: '👥' },
    { key: 'background', label: '背景', icon: '🌅' }
  ]

  return (
    <div style={{
      background: 'rgba(0, 0, 0, 0.5)',
      backdropFilter: 'blur(24px)',
      border: '1px solid rgba(255, 255, 255, 0.1)',
      borderRadius: '1.5rem',
      padding: '2rem'
    }}>
      {/* ヘッダー */}
      <div style={{ marginBottom: '2rem' }}>
        <h3 style={{
          fontSize: '1.5rem',
          fontWeight: '700',
          marginBottom: '1rem',
          textAlign: 'center'
        }}>
          🖼️ AI推奨画像選択
        </h3>
        <p style={{
          color: '#9ca3af',
          textAlign: 'center',
          marginBottom: '1rem'
        }}>
          {industry}業界・{productName}に最適化された画像を自動選定
        </p>
        
        {/* 選択状況 */}
        <div style={{
          background: 'rgba(59, 130, 246, 0.1)',
          border: '1px solid rgba(59, 130, 246, 0.3)',
          borderRadius: '0.5rem',
          padding: '1rem',
          textAlign: 'center'
        }}>
          <span style={{ color: '#60a5fa', fontWeight: '600' }}>
            📊 選択済み: {selectedImages.length}/8枚 | 推奨: 5-8枚
          </span>
        </div>
      </div>

      {/* カテゴリフィルター */}
      <div style={{
        display: 'flex',
        gap: '0.5rem',
        marginBottom: '2rem',
        overflowX: 'auto',
        padding: '0.5rem 0'
      }}>
        {categories.map(cat => (
          <button
            key={cat.key}
            onClick={() => setCurrentCategory(cat.key)}
            style={{
              padding: '0.5rem 1rem',
              borderRadius: '9999px',
              border: 'none',
              background: currentCategory === cat.key 
                ? 'linear-gradient(135deg, #facc15, #f97316)'
                : 'rgba(255, 255, 255, 0.1)',
              color: currentCategory === cat.key ? '#000000' : '#ffffff',
              fontWeight: '600',
              fontSize: '0.875rem',
              cursor: 'pointer',
              transition: 'all 0.3s',
              whiteSpace: 'nowrap'
            }}
          >
            {cat.icon} {cat.label}
          </button>
        ))}
      </div>

      {/* ローディング */}
      {loading && (
        <div style={{ textAlign: 'center', padding: '3rem' }}>
          <div style={{
            width: '3rem',
            height: '3rem',
            border: '4px solid rgba(255, 255, 255, 0.1)',
            borderTop: '4px solid #facc15',
            borderRadius: '50%',
            animation: 'spin 1s linear infinite',
            margin: '0 auto 1rem'
          }}></div>
          <p>AI画像分析・選定中...</p>
        </div>
      )}

      {/* 画像グリッド */}
      {!loading && (
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))',
          gap: '1rem'
        }}>
          {filteredImages.map((image) => (
            <div
              key={image.id}
              onClick={() => toggleImageSelection(image)}
              style={{
                border: selectedImages.find(img => img.id === image.id)
                  ? '3px solid #facc15'
                  : '1px solid rgba(255, 255, 255, 0.2)',
                borderRadius: '1rem',
                overflow: 'hidden',
                cursor: 'pointer',
                transition: 'all 0.3s ease',
                background: 'rgba(0, 0, 0, 0.3)',
                position: 'relative'
              }}
            >
              {/* 画像 */}
              <img
                src={image.urls.small}
                alt={image.alt_description}
                style={{
                  width: '100%',
                  height: '180px',
                  objectFit: 'cover'
                }}
                loading="lazy"
              />
              
              {/* オーバーレイ情報 */}
              <div style={{
                position: 'absolute',
                top: '0.5rem',
                right: '0.5rem',
                background: 'rgba(0, 0, 0, 0.8)',
                color: '#ffffff',
                padding: '0.25rem 0.5rem',
                borderRadius: '0.25rem',
                fontSize: '0.75rem',
                fontWeight: '600'
              }}>
                AI: {Math.round(image.ai_score)}/10
              </div>
              
              {/* 選択マーク */}
              {selectedImages.find(img => img.id === image.id) && (
                <div style={{
                  position: 'absolute',
                  top: '0.5rem',
                  left: '0.5rem',
                  background: '#facc15',
                  color: '#000000',
                  padding: '0.25rem 0.5rem',
                  borderRadius: '0.25rem',
                  fontSize: '0.75rem',
                  fontWeight: '700'
                }}>
                  ✅ 選択中
                </div>
              )}

              {/* 画像情報 */}
              <div style={{ padding: '1rem' }}>
                <div style={{
                  fontSize: '0.75rem',
                  fontWeight: '600',
                  color: '#facc15',
                  marginBottom: '0.25rem'
                }}>
                  {image.category?.toUpperCase()} - {image.usage}
                </div>
                
                <div style={{
                  fontSize: '0.75rem',
                  color: '#9ca3af',
                  marginBottom: '0.5rem'
                }}>
                  👤 {image.user.name} | 👍 {image.likes}
                </div>
                
                <div style={{
                  fontSize: '0.7rem',
                  color: '#d1d5db',
                  lineHeight: '1.4'
                }}>
                  {image.alt_description}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* 選択済み画像プレビュー */}
      {selectedImages.length > 0 && (
        <div style={{
          marginTop: '2rem',
          padding: '1.5rem',
          background: 'rgba(34, 197, 94, 0.1)',
          border: '1px solid rgba(34, 197, 94, 0.3)',
          borderRadius: '1rem'
        }}>
          <h4 style={{
            color: '#22c55e',
            marginBottom: '1rem',
            fontSize: '1rem',
            fontWeight: '600'
          }}>
            ✅ 選択済み画像 ({selectedImages.length}枚)
          </h4>
          
          <div style={{
            display: 'flex',
            gap: '0.5rem',
            overflowX: 'auto',
            padding: '0.5rem 0'
          }}>
            {selectedImages.map((image) => (
              <div key={image.id} style={{ position: 'relative', flexShrink: 0 }}>
                <img
                  src={image.urls.thumb}
                  alt={image.alt_description}
                  style={{
                    width: '80px',
                    height: '60px',
                    objectFit: 'cover',
                    borderRadius: '0.5rem',
                    border: '2px solid #22c55e'
                  }}
                />
                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    toggleImageSelection(image)
                  }}
                  style={{
                    position: 'absolute',
                    top: '-0.5rem',
                    right: '-0.5rem',
                    width: '1.5rem',
                    height: '1.5rem',
                    borderRadius: '50%',
                    background: '#dc2626',
                    color: '#ffffff',
                    border: 'none',
                    fontSize: '0.75rem',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}
                >
                  ×
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      <style dangerouslySetInnerHTML={{
        __html: `
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
        `
      }} />
    </div>
  )
}

// 画像クエリ生成
function generateImageQueries(industry: string, productName: string, targetAudience: string) {
  return [
    {
      query: `${industry} professional technology`,
      count: 8,
      category: 'hero',
      usage: 'メインビジュアル',
      priority: 'high'
    },
    {
      query: `business professional ${targetAudience}`,
      count: 6,
      category: 'testimonial', 
      usage: 'お客様の声',
      priority: 'high'
    },
    {
      query: `${productName} dashboard interface`,
      count: 5,
      category: 'feature',
      usage: '機能説明',
      priority: 'medium'
    },
    {
      query: `team meeting ${industry}`,
      count: 4,
      category: 'team',
      usage: 'チーム紹介',
      priority: 'medium'
    },
    {
      query: `modern office workspace`,
      count: 3,
      category: 'background',
      usage: '背景・装飾',
      priority: 'low'
    }
  ]
}

// 重複画像除去
function removeDuplicates(images: UnsplashImage[]): UnsplashImage[] {
  const seen = new Set()
  return images.filter(img => {
    if (seen.has(img.id)) {
      return false
    }
    seen.add(img.id)
    return true
  })
}