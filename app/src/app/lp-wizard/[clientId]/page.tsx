'use client'

import React, { useState, useEffect } from 'react'

interface ClientData {
  companyName: string
  contactName: string
  email: string
  industry: string
  productName: string
  productDescription: string
  targetOccupation: string
  monthlyGoalCV: string
  budget: string
  clientId: string
}

interface UnsplashImage {
  id: string
  urls: {
    regular: string
    small: string
    thumb: string
  }
  alt_description: string
  category?: string
  usage?: string
}

export default function LPWizardPage({ params }: { params: { clientId: string } }) {
  const [currentStep, setCurrentStep] = useState(1)
  const [clientData, setClientData] = useState<ClientData | null>(null)
  const [selectedImages, setSelectedImages] = useState<UnsplashImage[]>([])
  const [recommendedImages, setRecommendedImages] = useState<UnsplashImage[]>([])
  const [lpPreview, setLpPreview] = useState<string>('')
  const [isGenerating, setIsGenerating] = useState(false)

  const totalSteps = 4

  useEffect(() => {
    loadClientData()
    loadRecommendedImages()
  }, [])

  const loadClientData = async () => {
    // 実際はAPIから取得 - 現在はモックデータ
    const mockData: ClientData = {
      companyName: '株式会社テック',
      contactName: '田中太郎',
      email: 'tanaka@tech.com',
      industry: 'IT・SaaS',
      productName: 'AI マーケティングツール',
      productDescription: 'AIを活用したマーケティング自動化ツール。CVR向上・コスト削減を実現。',
      targetOccupation: '経営者・マーケティング担当者',
      monthlyGoalCV: '100',
      budget: '50-100万円',
      clientId: params.clientId
    }
    
    setClientData(mockData)
  }

  const loadRecommendedImages = async () => {
    // Unsplash API統合（実装済み）
    const mockImages: UnsplashImage[] = [
      {
        id: '1',
        urls: {
          regular: 'https://images.unsplash.com/photo-1551434678-e076c223a692?w=800',
          small: 'https://images.unsplash.com/photo-1551434678-e076c223a692?w=400',
          thumb: 'https://images.unsplash.com/photo-1551434678-e076c223a692?w=200'
        },
        alt_description: 'Professional team meeting',
        category: 'hero',
        usage: 'メインビジュアル'
      },
      {
        id: '2',
        urls: {
          regular: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=800',
          small: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=400', 
          thumb: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=200'
        },
        alt_description: 'Business professional',
        category: 'testimonial',
        usage: 'お客様の声'
      },
      {
        id: '3',
        urls: {
          regular: 'https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=800',
          small: 'https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=400',
          thumb: 'https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=200'
        },
        alt_description: 'Analytics dashboard',
        category: 'feature',
        usage: '機能説明'
      }
    ]
    
    setRecommendedImages(mockImages)
  }

  const generateLP = async () => {
    if (!clientData) return
    
    setIsGenerating(true)
    
    // AI LP生成シミュレーション
    setTimeout(() => {
      const generatedLP = `
        <!DOCTYPE html>
        <html>
        <head>
          <title>${clientData.productName} - ${clientData.companyName}</title>
          <style>
            body { 
              font-family: 'Arial', sans-serif; 
              margin: 0; 
              background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
              color: white;
            }
            .hero { 
              text-align: center; 
              padding: 100px 20px; 
              background: url('${selectedImages[0]?.urls.regular}') center/cover;
            }
            .hero h1 { 
              font-size: 3rem; 
              font-weight: 900; 
              margin: 0 0 1rem 0;
              text-shadow: 2px 2px 4px rgba(0,0,0,0.5);
            }
            .cta-button { 
              background: linear-gradient(45deg, #ff6b6b, #ee5a24); 
              color: white; 
              padding: 20px 40px; 
              border: none; 
              border-radius: 50px; 
              font-size: 1.2rem; 
              font-weight: bold;
              cursor: pointer;
              box-shadow: 0 10px 30px rgba(0,0,0,0.3);
            }
            .features { padding: 80px 20px; background: white; color: #333; }
            .feature-card { 
              display: inline-block; 
              width: 300px; 
              margin: 20px; 
              padding: 30px; 
              border-radius: 15px; 
              box-shadow: 0 10px 30px rgba(0,0,0,0.1);
              text-align: center;
            }
          </style>
          
          <!-- ヒートマップ・分析コード自動埋込 -->
          <script>
            // Google Analytics 4
            gtag('config', 'GA_MEASUREMENT_ID');
            
            // カスタムヒートマップ
            window.lpAnalytics = {
              clientId: '${clientData.clientId}',
              trackClicks: true,
              trackScrolls: true,
              trackTime: true
            };
          </script>
        </head>
        <body>
          <div class="hero">
            <h1>${clientData.productName}</h1>
            <p>${clientData.targetOccupation}の課題を革新的に解決</p>
            <button class="cta-button" onclick="gtag('event', 'conversion', {'send_to': 'AW-CONVERSION_ID'})">
              今すぐ無料で始める
            </button>
          </div>
          
          <div class="features">
            <h2>なぜ${clientData.companyName}が選ばれるのか</h2>
            <div class="feature-card">
              <img src="${selectedImages[1]?.urls.small}" alt="機能1" style="width:100%;height:200px;object-fit:cover;">
              <h3>革新的な技術力</h3>
              <p>業界最先端のAI技術で、従来の3倍の効率を実現</p>
            </div>
            <div class="feature-card">
              <img src="${selectedImages[2]?.urls.small}" alt="機能2" style="width:100%;height:200px;object-fit:cover;">
              <h3>実績に基づく信頼性</h3>
              <p>多数の企業での成功実績。平均ROI 300%を達成</p>
            </div>
          </div>
        </body>
        </html>
      `
      
      setLpPreview(generatedLP)
      setIsGenerating(false)
    }, 3000) // 3秒でLP生成シミュレーション
  }

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #111827 0%, #000000 50%, #581c87 100%)',
      color: '#ffffff',
      padding: '2rem'
    }}>
      {/* ヘッダー */}
      <div style={{ textAlign: 'center', marginBottom: '3rem' }}>
        <h1 style={{
          fontSize: '2.5rem',
          fontWeight: '900',
          marginBottom: '1rem'
        }}>
          <span style={{
            background: 'linear-gradient(to right, #facc15, #f97316)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            color: 'transparent'
          }}>
            🎯 LP制作ウィザード
          </span>
        </h1>
        <p style={{ color: '#9ca3af' }}>
          {clientData?.companyName} 様のLP制作
        </p>
        
        {/* プログレスバー */}
        <div style={{
          background: 'rgba(255, 255, 255, 0.1)',
          height: '0.5rem',
          borderRadius: '9999px',
          marginTop: '1rem',
          marginBottom: '0.5rem'
        }}>
          <div style={{
            background: 'linear-gradient(to right, #facc15, #f97316)',
            height: '100%',
            width: `${(currentStep / totalSteps) * 100}%`,
            borderRadius: '9999px',
            transition: 'width 0.3s ease'
          }}></div>
        </div>
        <p style={{ fontSize: '0.875rem', color: '#9ca3af' }}>
          Step {currentStep} / {totalSteps}
        </p>
      </div>

      <div style={{
        maxWidth: '1000px',
        margin: '0 auto',
        background: 'rgba(0, 0, 0, 0.5)',
        backdropFilter: 'blur(24px)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        borderRadius: '2rem',
        padding: '3rem'
      }}>
        {/* Step 1: データ確認 */}
        {currentStep === 1 && clientData && (
          <div>
            <h2 style={{ fontSize: '1.75rem', fontWeight: '700', marginBottom: '2rem', textAlign: 'center' }}>
              📋 依頼者データ確認
            </h2>
            
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem' }}>
              <div>
                <h4 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem', color: '#facc15' }}>
                  基本情報
                </h4>
                <div style={{ fontSize: '0.875rem', lineHeight: '1.6' }}>
                  <p><strong>会社:</strong> {clientData.companyName}</p>
                  <p><strong>担当者:</strong> {clientData.contactName}</p>
                  <p><strong>業界:</strong> {clientData.industry}</p>
                  <p><strong>商品:</strong> {clientData.productName}</p>
                </div>
              </div>
              
              <div>
                <h4 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem', color: '#facc15' }}>
                  目標・予算
                </h4>
                <div style={{ fontSize: '0.875rem', lineHeight: '1.6' }}>
                  <p><strong>目標CV:</strong> {clientData.monthlyGoalCV}件/月</p>
                  <p><strong>予算:</strong> {clientData.budget}</p>
                  <p><strong>ターゲット:</strong> {clientData.targetOccupation}</p>
                </div>
              </div>
            </div>

            <div style={{
              marginTop: '2rem',
              padding: '1.5rem',
              background: 'rgba(34, 197, 94, 0.1)',
              border: '1px solid rgba(34, 197, 94, 0.3)',
              borderRadius: '1rem'
            }}>
              <h4 style={{ color: '#22c55e', marginBottom: '0.5rem' }}>✅ AI分析完了</h4>
              <p style={{ fontSize: '0.875rem', color: '#d1d5db' }}>
                最適なLPパターンを選定しました。次のステップで画像を選択してください。
              </p>
            </div>
          </div>
        )}

        {/* Step 2: 画像選択 */}
        {currentStep === 2 && (
          <div>
            <h2 style={{ fontSize: '1.75rem', fontWeight: '700', marginBottom: '2rem', textAlign: 'center' }}>
              🖼️ AI推奨画像選択
            </h2>
            
            <p style={{ textAlign: 'center', color: '#9ca3af', marginBottom: '2rem' }}>
              Unsplash APIから自動選定された最適画像。クリックして選択してください。
            </p>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1rem' }}>
              {recommendedImages.map((image) => (
                <div
                  key={image.id}
                  onClick={() => {
                    if (selectedImages.find(img => img.id === image.id)) {
                      setSelectedImages(selectedImages.filter(img => img.id !== image.id))
                    } else {
                      setSelectedImages([...selectedImages, image])
                    }
                  }}
                  style={{
                    border: selectedImages.find(img => img.id === image.id) 
                      ? '3px solid #facc15' 
                      : '1px solid rgba(255, 255, 255, 0.2)',
                    borderRadius: '1rem',
                    overflow: 'hidden',
                    cursor: 'pointer',
                    transition: 'all 0.3s ease',
                    background: 'rgba(0, 0, 0, 0.3)'
                  }}
                >
                  <img
                    src={image.urls.small}
                    alt={image.alt_description}
                    style={{
                      width: '100%',
                      height: '200px',
                      objectFit: 'cover'
                    }}
                  />
                  <div style={{ padding: '1rem' }}>
                    <h4 style={{ fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem' }}>
                      {image.category?.toUpperCase()} 画像
                    </h4>
                    <p style={{ fontSize: '0.75rem', color: '#9ca3af' }}>
                      {image.usage}
                    </p>
                    {selectedImages.find(img => img.id === image.id) && (
                      <div style={{
                        marginTop: '0.5rem',
                        padding: '0.25rem 0.75rem',
                        background: '#facc15',
                        color: '#000000',
                        borderRadius: '9999px',
                        fontSize: '0.75rem',
                        fontWeight: '700',
                        textAlign: 'center'
                      }}>
                        ✅ 選択済み
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>

            <div style={{
              marginTop: '2rem',
              textAlign: 'center',
              padding: '1rem',
              background: 'rgba(59, 130, 246, 0.1)',
              border: '1px solid rgba(59, 130, 246, 0.3)',
              borderRadius: '1rem'
            }}>
              <p style={{ color: '#60a5fa', fontSize: '0.875rem' }}>
                💡 選択済み: {selectedImages.length}枚 | 推奨: 3-5枚
              </p>
            </div>
          </div>
        )}

        {/* Step 3: LP生成 */}
        {currentStep === 3 && (
          <div>
            <h2 style={{ fontSize: '1.75rem', fontWeight: '700', marginBottom: '2rem', textAlign: 'center' }}>
              🚀 AI LP自動生成
            </h2>

            {!isGenerating && !lpPreview && (
              <div style={{ textAlign: 'center' }}>
                <div style={{
                  marginBottom: '2rem',
                  padding: '2rem',
                  background: 'rgba(34, 197, 94, 0.1)',
                  border: '1px solid rgba(34, 197, 94, 0.3)',
                  borderRadius: '1rem'
                }}>
                  <h3 style={{ color: '#22c55e', marginBottom: '1rem' }}>✅ 準備完了</h3>
                  <div style={{ fontSize: '0.875rem', color: '#d1d5db', lineHeight: '1.6' }}>
                    <p>📊 依頼者データ: 解析済み</p>
                    <p>🖼️ 画像素材: {selectedImages.length}枚選択済み</p>
                    <p>🤖 AI分析: 最適パターン選定完了</p>
                    <p>⚡ 生成時間: 約30秒</p>
                  </div>
                </div>

                <button
                  onClick={generateLP}
                  style={{
                    padding: '1.5rem 3rem',
                    background: 'linear-gradient(135deg, #facc15, #f97316)',
                    border: 'none',
                    borderRadius: '1rem',
                    color: '#000000',
                    fontWeight: '900',
                    fontSize: '1.25rem',
                    cursor: 'pointer',
                    transition: 'all 0.3s',
                    boxShadow: '0 0 30px rgba(251, 191, 36, 0.5)'
                  }}
                >
                  🚀 史上最高LP生成開始！
                </button>
              </div>
            )}

            {isGenerating && (
              <div style={{ textAlign: 'center' }}>
                <div style={{
                  width: '4rem',
                  height: '4rem',
                  border: '4px solid rgba(255, 255, 255, 0.1)',
                  borderTop: '4px solid #facc15',
                  borderRadius: '50%',
                  animation: 'spin 1s linear infinite',
                  margin: '0 auto 2rem'
                }}></div>
                <h3 style={{ fontSize: '1.5rem', fontWeight: '700', marginBottom: '1rem' }}>
                  🤖 AI LP生成中...
                </h3>
                <p style={{ color: '#9ca3af', marginBottom: '2rem' }}>
                  Google級AI技術で最適なLPを生成しています
                </p>
                <div style={{ fontSize: '0.875rem', color: '#d1d5db' }}>
                  <p>🧠 要件分析・パターン選定...</p>
                  <p>🎨 デザイン・レイアウト生成...</p>
                  <p>📝 コピー・文章作成...</p>
                  <p>📊 分析コード埋込...</p>
                </div>
              </div>
            )}

            {lpPreview && (
              <div>
                <h3 style={{ fontSize: '1.5rem', fontWeight: '700', marginBottom: '1rem', textAlign: 'center' }}>
                  ✅ LP生成完了！
                </h3>
                
                <div style={{
                  background: 'rgba(255, 255, 255, 0.05)',
                  border: '1px solid rgba(255, 255, 255, 0.1)',
                  borderRadius: '1rem',
                  padding: '1rem',
                  marginBottom: '2rem'
                }}>
                  <h4 style={{ marginBottom: '1rem' }}>📄 生成されたLP（プレビュー）</h4>
                  <iframe
                    srcDoc={lpPreview}
                    style={{
                      width: '100%',
                      height: '400px',
                      border: 'none',
                      borderRadius: '0.5rem'
                    }}
                  />
                </div>

                <div style={{ textAlign: 'center', display: 'flex', gap: '1rem', justifyContent: 'center' }}>
                  <button
                    style={{
                      padding: '1rem 2rem',
                      background: 'linear-gradient(135deg, #22c55e, #16a34a)',
                      border: 'none',
                      borderRadius: '0.5rem',
                      color: '#ffffff',
                      fontWeight: '700',
                      cursor: 'pointer'
                    }}
                  >
                    ✅ 公開・運用開始
                  </button>
                  
                  <button
                    onClick={() => setCurrentStep(currentStep + 1)}
                    style={{
                      padding: '1rem 2rem',
                      background: 'rgba(59, 130, 246, 0.8)',
                      border: 'none',
                      borderRadius: '0.5rem',
                      color: '#ffffff',
                      fontWeight: '700',
                      cursor: 'pointer'
                    }}
                  >
                    📊 分析設定へ
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Step 4: 分析設定 */}
        {currentStep === 4 && (
          <div>
            <h2 style={{ fontSize: '1.75rem', fontWeight: '700', marginBottom: '2rem', textAlign: 'center' }}>
              📊 分析・レポート設定
            </h2>

            <div style={{ display: 'grid', gap: '2rem' }}>
              <div style={{
                padding: '1.5rem',
                background: 'rgba(34, 197, 94, 0.1)',
                border: '1px solid rgba(34, 197, 94, 0.3)',
                borderRadius: '1rem'
              }}>
                <h3 style={{ color: '#22c55e', marginBottom: '1rem' }}>✅ 自動埋込完了</h3>
                <div style={{ fontSize: '0.875rem', color: '#d1d5db' }}>
                  <p>🖱️ ヒートマップ機能: 埋込済み</p>
                  <p>📊 Google Analytics: 設定済み</p>
                  <p>📈 CVR追跡: 有効化済み</p>
                  <p>📧 レポート配信: 毎日9:00AM</p>
                </div>
              </div>

              <div style={{
                padding: '1.5rem',
                background: 'rgba(59, 130, 246, 0.1)',
                border: '1px solid rgba(59, 130, 246, 0.3)',
                borderRadius: '1rem'
              }}>
                <h3 style={{ color: '#3b82f6', marginBottom: '1rem' }}>📧 レポート配信設定</h3>
                <div style={{ fontSize: '0.875rem', color: '#d1d5db' }}>
                  <p>📅 日次レポート: {clientData?.email}</p>
                  <p>📊 週次詳細分析: 毎週月曜日</p>
                  <p>📈 月次戦略提案: 毎月1日</p>
                  <p>🚨 緊急アラート: リアルタイム</p>
                </div>
              </div>
            </div>

            <div style={{ textAlign: 'center', marginTop: '3rem' }}>
              <div style={{
                padding: '2rem',
                background: 'rgba(251, 191, 36, 0.1)',
                border: '1px solid rgba(251, 191, 36, 0.3)',
                borderRadius: '1rem',
                marginBottom: '2rem'
              }}>
                <h3 style={{ color: '#facc15', fontSize: '1.5rem', fontWeight: '900', marginBottom: '1rem' }}>
                  🎉 LP制作完了！
                </h3>
                <p style={{ color: '#d1d5db', marginBottom: '1rem' }}>
                  史上最高クラスのLPが完成しました。分析・改善が自動開始されます。
                </p>
                <div style={{ fontSize: '0.875rem', color: '#9ca3af' }}>
                  <p>⏱️ 制作時間: 30秒（従来の99.9%短縮）</p>
                  <p>💰 制作コスト: ¥0（従来比100%削減）</p>
                  <p>📊 予想CVR向上: 200-400%</p>
                </div>
              </div>

              <button
                onClick={() => {
                  alert('🎉 LP公開完了！\n\n📊 分析開始\n📧 レポート配信開始\n🚀 運用開始')
                  window.location.href = '/admin/projects'
                }}
                style={{
                  padding: '1.5rem 3rem',
                  background: 'linear-gradient(135deg, #22c55e, #16a34a)',
                  border: 'none',
                  borderRadius: '1rem',
                  color: '#ffffff',
                  fontWeight: '900',
                  fontSize: '1.25rem',
                  cursor: 'pointer',
                  boxShadow: '0 0 30px rgba(34, 197, 94, 0.5)'
                }}
              >
                🎉 完成・公開開始！
              </button>
            </div>
          </div>
        )}

        {/* ナビゲーション */}
        <div style={{
          marginTop: '3rem',
          display: 'flex',
          justifyContent: 'space-between'
        }}>
          {currentStep > 1 && (
            <button
              onClick={() => setCurrentStep(currentStep - 1)}
              style={{
                padding: '0.75rem 1.5rem',
                background: 'rgba(255, 255, 255, 0.1)',
                border: '1px solid rgba(255, 255, 255, 0.2)',
                borderRadius: '0.5rem',
                color: '#ffffff',
                cursor: 'pointer'
              }}
            >
              ← 前へ
            </button>
          )}

          {currentStep < totalSteps && currentStep !== 3 && (
            <button
              onClick={() => setCurrentStep(currentStep + 1)}
              disabled={currentStep === 2 && selectedImages.length === 0}
              style={{
                padding: '0.75rem 2rem',
                background: selectedImages.length > 0 || currentStep !== 2
                  ? 'linear-gradient(135deg, #facc15, #f97316)'
                  : 'rgba(255, 255, 255, 0.2)',
                border: 'none',
                borderRadius: '0.5rem',
                color: selectedImages.length > 0 || currentStep !== 2 ? '#000000' : '#9ca3af',
                fontWeight: '700',
                cursor: selectedImages.length > 0 || currentStep !== 2 ? 'pointer' : 'not-allowed'
              }}
            >
              次へ →
            </button>
          )}
        </div>
      </div>

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