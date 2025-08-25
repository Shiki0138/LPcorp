'use client'

import React, { useState } from 'react'
import { generateQuote } from '@/config/pricing'

interface QuoteForm {
  companyName: string
  contactName: string
  email: string
  phone: string
  companySize: 'small' | 'medium' | 'large'
  industry: string
  budget: string
  urgency: 'normal' | 'urgent' | 'emergency'
  currentLP: string
  goals: string
  features: string[]
  additionalRequests: string
}

export default function QuotePage() {
  const [step, setStep] = useState(1)
  const [formData, setFormData] = useState<Partial<QuoteForm>>({})
  const [quoteResult, setQuoteResult] = useState<any>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const totalSteps = 4

  const handleInputChange = (field: keyof QuoteForm, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }))
  }

  const handleFeatureToggle = (feature: string) => {
    const currentFeatures = formData.features || []
    if (currentFeatures.includes(feature)) {
      handleInputChange('features', currentFeatures.filter(f => f !== feature))
    } else {
      handleInputChange('features', [...currentFeatures, feature])
    }
  }

  const generateQuoteResult = () => {
    setIsSubmitting(true)
    
    // AI見積もり生成シミュレーション
    setTimeout(() => {
      const quote = generateQuote({
        companySize: formData.companySize || 'medium',
        industry: formData.industry || 'IT・SaaS',
        budget: formData.budget || '5-10万円',
        urgency: formData.urgency || 'normal',
        features: formData.features || []
      })
      
      setQuoteResult(quote)
      setIsSubmitting(false)
    }, 2000)
  }

  const progress = (step / totalSteps) * 100

  if (quoteResult) {
    return (
      <div style={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #0f0f23 0%, #1a1a2e 50%, #16213e 100%)',
        color: '#ffffff',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '2rem'
      }}>
        <div style={{
          background: 'rgba(255, 255, 255, 0.05)',
          backdropFilter: 'blur(30px)',
          border: '1px solid rgba(255, 255, 255, 0.1)',
          borderRadius: '3rem',
          padding: '4rem',
          maxWidth: '800px',
          textAlign: 'center'
        }}>
          <div style={{ fontSize: '4rem', marginBottom: '2rem' }}>🎉</div>
          
          <h1 style={{
            fontSize: '2.5rem',
            fontWeight: '900',
            marginBottom: '2rem',
            background: 'linear-gradient(135deg, #4ecdc4, #45b7d1)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            color: 'transparent'
          }}>
            お見積もり完了！
          </h1>

          <div style={{
            background: 'rgba(0, 0, 0, 0.3)',
            padding: '2rem',
            borderRadius: '2rem',
            marginBottom: '3rem'
          }}>
            <h3 style={{ fontSize: '1.5rem', marginBottom: '1rem', color: '#ffffff' }}>
              📋 {formData.companyName} 様専用プラン
            </h3>
            
            <div style={{
              fontSize: '3rem',
              fontWeight: '900',
              background: 'linear-gradient(135deg, #ff6b6b, #4ecdc4)',
              backgroundClip: 'text',
              WebkitBackgroundClip: 'text',
              color: 'transparent',
              marginBottom: '1rem'
            }}>
              ¥{quoteResult.finalPrice.toLocaleString()}
            </div>

            <div style={{ color: '#b8b8b8', marginBottom: '2rem' }}>
              推奨プラン: {quoteResult.tier.name} | 
              納期: {quoteResult.estimatedDelivery}
            </div>

            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '1rem',
              marginBottom: '2rem'
            }}>
              <div>
                <div style={{ color: '#4ecdc4', fontWeight: '700' }}>予想CVR改善</div>
                <div style={{ fontSize: '1.2rem', fontWeight: '900' }}>
                  {quoteResult.expectedResults.cvrImprovement}
                </div>
              </div>
              <div>
                <div style={{ color: '#ff6b6b', fontWeight: '700' }}>期待ROI</div>
                <div style={{ fontSize: '1.2rem', fontWeight: '900' }}>
                  {quoteResult.expectedResults.roiExpected}
                </div>
              </div>
              <div>
                <div style={{ color: '#45b7d1', fontWeight: '700' }}>効果発現</div>
                <div style={{ fontSize: '1.2rem', fontWeight: '900' }}>
                  {quoteResult.expectedResults.timeToLive}
                </div>
              </div>
            </div>

            {quoteResult.discount > 0 && (
              <div style={{
                background: '#4ecdc4',
                color: '#000000',
                padding: '1rem 2rem',
                borderRadius: '50px',
                fontSize: '1rem',
                fontWeight: '700',
                marginBottom: '2rem',
                display: 'inline-block'
              }}>
                🎉 ローンチ記念割引 ¥{quoteResult.discount.toLocaleString()}OFF適用！
              </div>
            )}
          </div>

          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
            <a
              href={`mailto:lp-pro@example.com?subject=正式お見積もり依頼 - ${formData.companyName}&body=【正式お見積もり依頼】%0D%0A%0D%0A会社名: ${formData.companyName}%0D%0A担当者: ${formData.contactName}%0D%0Aメール: ${formData.email}%0D%0A電話: ${formData.phone}%0D%0A%0D%0A推奨プラン: ${quoteResult.tier.name}%0D%0A合計金額: ¥${quoteResult.finalPrice.toLocaleString()}%0D%0A納期: ${quoteResult.estimatedDelivery}%0D%0A%0D%0A業界: ${formData.industry}%0D%0A会社規模: ${formData.companySize}%0D%0A予算: ${formData.budget}%0D%0A緊急度: ${formData.urgency}%0D%0A%0D%0A現在のLP: ${formData.currentLP}%0D%0A目標: ${formData.goals}%0D%0A%0D%0A【追加要望】%0D%0A${formData.additionalRequests}%0D%0A%0D%0Aこの内容で正式にLP制作をお願いいたします。%0D%0A詳細打ち合わせのご連絡をお待ちしております。`}
              style={{
                padding: '1.5rem 3rem',
                background: 'linear-gradient(135deg, #ff6b6b, #ee5a24)',
                color: '#ffffff',
                textDecoration: 'none',
                borderRadius: '1rem',
                fontWeight: '700',
                fontSize: '1.1rem',
                transition: 'all 0.3s'
              }}
            >
              📧 この内容で正式依頼
            </a>
            
            <button
              onClick={() => {
                setQuoteResult(null)
                setStep(1)
                setFormData({})
              }}
              style={{
                padding: '1.5rem 3rem',
                background: 'rgba(255, 255, 255, 0.1)',
                border: '1px solid rgba(255, 255, 255, 0.2)',
                borderRadius: '1rem',
                color: '#ffffff',
                fontWeight: '700',
                cursor: 'pointer'
              }}
            >
              🔄 新しい見積もり
            </button>
          </div>

          <div style={{
            marginTop: '2rem',
            padding: '1.5rem',
            background: 'rgba(78, 205, 196, 0.1)',
            borderRadius: '1rem',
            fontSize: '0.9rem',
            color: '#d1d1d1'
          }}>
            💡 <strong style={{ color: '#4ecdc4' }}>次のステップ:</strong><br/>
            1. メール送信後、24時間以内にご連絡<br/>
            2. 詳細ヒアリング・要件確認（30分）<br/>
            3. 契約締結・制作開始<br/>
            4. LP完成・納品（{quoteResult.estimatedDelivery}）
          </div>
        </div>
      </div>
    )
  }

  if (isSubmitting) {
    return (
      <div style={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #0f0f23 0%, #1a1a2e 50%, #16213e 100%)',
        color: '#ffffff',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '2rem'
      }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{
            width: '4rem',
            height: '4rem',
            border: '4px solid rgba(255, 255, 255, 0.1)',
            borderTop: '4px solid #4ecdc4',
            borderRadius: '50%',
            animation: 'spin 1s linear infinite',
            margin: '0 auto 2rem'
          }}></div>
          <h2 style={{ fontSize: '1.8rem', fontWeight: '700', marginBottom: '1rem' }}>
            🤖 AI見積もり生成中...
          </h2>
          <p style={{ color: '#b8b8b8' }}>
            最適なプラン・価格を分析しています
          </p>
        </div>
      </div>
    )
  }

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #0f0f23 0%, #1a1a2e 50%, #16213e 100%)',
      color: '#ffffff',
      padding: '2rem'
    }}>
      {/* ヘッダー */}
      <div style={{ textAlign: 'center', marginBottom: '3rem', paddingTop: '2rem' }}>
        <h1 style={{
          fontSize: 'clamp(2rem, 5vw, 3rem)',
          fontWeight: '900',
          marginBottom: '1rem'
        }}>
          <span style={{
            background: 'linear-gradient(135deg, #4ecdc4, #45b7d1)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            color: 'transparent'
          }}>
            💰 AI自動見積もり
          </span>
        </h1>
        <p style={{ fontSize: '1.1rem', color: '#b8b8b8', marginBottom: '2rem' }}>
          あなたのビジネスに最適なLP制作プラン・価格を自動算出
        </p>

        {/* プログレスバー */}
        <div style={{
          background: 'rgba(255, 255, 255, 0.1)',
          height: '0.5rem',
          borderRadius: '9999px',
          marginBottom: '1rem',
          overflow: 'hidden'
        }}>
          <div style={{
            background: 'linear-gradient(to right, #4ecdc4, #45b7d1)',
            height: '100%',
            width: `${progress}%`,
            transition: 'width 0.3s ease'
          }}></div>
        </div>
        <p style={{ fontSize: '0.9rem', color: '#b8b8b8' }}>
          Step {step} / {totalSteps} - {Math.round(progress)}% 完了
        </p>
      </div>

      {/* フォームステップ */}
      <div style={{
        maxWidth: '700px',
        margin: '0 auto',
        background: 'rgba(255, 255, 255, 0.05)',
        backdropFilter: 'blur(20px)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        borderRadius: '2rem',
        padding: '3rem'
      }}>
        {/* Step 1: 基本情報 */}
        {step === 1 && (
          <div>
            <h3 style={{ fontSize: '1.5rem', fontWeight: '700', marginBottom: '2rem', textAlign: 'center' }}>
              🏢 基本情報
            </h3>
            
            <div style={{ display: 'grid', gap: '1.5rem' }}>
              <div>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                  会社名 *
                </label>
                <input
                  type="text"
                  value={formData.companyName || ''}
                  onChange={(e) => handleInputChange('companyName', e.target.value)}
                  placeholder="株式会社○○○"
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    borderRadius: '0.5rem',
                    border: '1px solid rgba(255, 255, 255, 0.2)',
                    background: 'rgba(255, 255, 255, 0.1)',
                    color: '#ffffff'
                  }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                    ご担当者名 *
                  </label>
                  <input
                    type="text"
                    value={formData.contactName || ''}
                    onChange={(e) => handleInputChange('contactName', e.target.value)}
                    placeholder="山田太郎"
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      borderRadius: '0.5rem',
                      border: '1px solid rgba(255, 255, 255, 0.2)',
                      background: 'rgba(255, 255, 255, 0.1)',
                      color: '#ffffff'
                    }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                    メールアドレス *
                  </label>
                  <input
                    type="email"
                    value={formData.email || ''}
                    onChange={(e) => handleInputChange('email', e.target.value)}
                    placeholder="info@company.com"
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      borderRadius: '0.5rem',
                      border: '1px solid rgba(255, 255, 255, 0.2)',
                      background: 'rgba(255, 255, 255, 0.1)',
                      color: '#ffffff'
                    }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                    会社規模 *
                  </label>
                  <select
                    value={formData.companySize || ''}
                    onChange={(e) => handleInputChange('companySize', e.target.value)}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      borderRadius: '0.5rem',
                      border: '1px solid rgba(255, 255, 255, 0.2)',
                      background: 'rgba(255, 255, 255, 0.1)',
                      color: '#ffffff'
                    }}
                  >
                    <option value="">選択してください</option>
                    <option value="small">小企業（〜50名）</option>
                    <option value="medium">中企業（51-200名）</option>
                    <option value="large">大企業（200名〜）</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                    業界 *
                  </label>
                  <select
                    value={formData.industry || ''}
                    onChange={(e) => handleInputChange('industry', e.target.value)}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      borderRadius: '0.5rem',
                      border: '1px solid rgba(255, 255, 255, 0.2)',
                      background: 'rgba(255, 255, 255, 0.1)',
                      color: '#ffffff'
                    }}
                  >
                    <option value="">選択してください</option>
                    <option value="IT・SaaS">IT・SaaS</option>
                    <option value="EC・小売">EC・小売</option>
                    <option value="教育・研修">教育・研修</option>
                    <option value="ヘルスケア">ヘルスケア</option>
                    <option value="金融・保険">金融・保険</option>
                    <option value="不動産">不動産</option>
                    <option value="その他">その他</option>
                  </select>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Step 2: 要件・予算 */}
        {step === 2 && (
          <div>
            <h3 style={{ fontSize: '1.5rem', fontWeight: '700', marginBottom: '2rem', textAlign: 'center' }}>
              💰 要件・予算
            </h3>
            
            <div style={{ display: 'grid', gap: '1.5rem' }}>
              <div>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                  ご予算 *
                </label>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))', gap: '0.5rem' }}>
                  {['〜3万円', '3-5万円', '5-10万円', '10-20万円', '20万円以上'].map(budget => (
                    <button
                      key={budget}
                      onClick={() => handleInputChange('budget', budget)}
                      style={{
                        padding: '0.75rem',
                        borderRadius: '0.5rem',
                        border: 'none',
                        background: formData.budget === budget 
                          ? 'linear-gradient(135deg, #4ecdc4, #45b7d1)' 
                          : 'rgba(255, 255, 255, 0.1)',
                        color: formData.budget === budget ? '#000000' : '#ffffff',
                        fontWeight: '600',
                        cursor: 'pointer',
                        transition: 'all 0.3s'
                      }}
                    >
                      {budget}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                  緊急度
                </label>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '0.5rem' }}>
                  {[
                    { key: 'normal', label: '通常（1-3時間）' },
                    { key: 'urgent', label: '急ぎ（1時間以内）' },
                    { key: 'emergency', label: '緊急（30分以内）' }
                  ].map(urgency => (
                    <button
                      key={urgency.key}
                      onClick={() => handleInputChange('urgency', urgency.key)}
                      style={{
                        padding: '0.75rem',
                        borderRadius: '0.5rem',
                        border: 'none',
                        background: formData.urgency === urgency.key 
                          ? 'linear-gradient(135deg, #ff6b6b, #ee5a24)' 
                          : 'rgba(255, 255, 255, 0.1)',
                        color: '#ffffff',
                        fontWeight: '600',
                        cursor: 'pointer',
                        transition: 'all 0.3s',
                        fontSize: '0.8rem'
                      }}
                    >
                      {urgency.label}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                  現在のLP・サイト
                </label>
                <input
                  type="url"
                  value={formData.currentLP || ''}
                  onChange={(e) => handleInputChange('currentLP', e.target.value)}
                  placeholder="https://current-site.com（参考用・任意）"
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    borderRadius: '0.5rem',
                    border: '1px solid rgba(255, 255, 255, 0.2)',
                    background: 'rgba(255, 255, 255, 0.1)',
                    color: '#ffffff'
                  }}
                />
              </div>
            </div>
          </div>
        )}

        {/* Step 3: 目標・要望 */}
        {step === 3 && (
          <div>
            <h3 style={{ fontSize: '1.5rem', fontWeight: '700', marginBottom: '2rem', textAlign: 'center' }}>
              🎯 目標・ご要望
            </h3>
            
            <div style={{ display: 'grid', gap: '1.5rem' }}>
              <div>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                  目標・改善したいポイント *
                </label>
                <textarea
                  value={formData.goals || ''}
                  onChange={(e) => handleInputChange('goals', e.target.value)}
                  placeholder="例：CVRを2倍にしたい、ブランドイメージを向上させたい、競合に勝つLPを作りたい"
                  rows={4}
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    borderRadius: '0.5rem',
                    border: '1px solid rgba(255, 255, 255, 0.2)',
                    background: 'rgba(255, 255, 255, 0.1)',
                    color: '#ffffff',
                    resize: 'vertical'
                  }}
                />
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '1rem', fontWeight: '600' }}>
                  ご希望機能（複数選択可）
                </label>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '0.5rem' }}>
                  {[
                    'A/Bテスト',
                    'ヒートマップ分析',
                    '競合分析',
                    'SEO最適化',
                    'スマホ最適化',
                    '継続改善',
                    'レポート配信',
                    'プロ監修'
                  ].map(feature => (
                    <button
                      key={feature}
                      onClick={() => handleFeatureToggle(feature)}
                      style={{
                        padding: '0.75rem',
                        borderRadius: '0.5rem',
                        border: 'none',
                        background: (formData.features || []).includes(feature)
                          ? 'linear-gradient(135deg, #4ecdc4, #45b7d1)'
                          : 'rgba(255, 255, 255, 0.1)',
                        color: (formData.features || []).includes(feature) ? '#000000' : '#ffffff',
                        fontWeight: '600',
                        cursor: 'pointer',
                        transition: 'all 0.3s',
                        fontSize: '0.85rem'
                      }}
                    >
                      {feature}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                  その他ご要望・特記事項
                </label>
                <textarea
                  value={formData.additionalRequests || ''}
                  onChange={(e) => handleInputChange('additionalRequests', e.target.value)}
                  placeholder="デザインのご要望、避けたい表現、特別な機能など"
                  rows={3}
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    borderRadius: '0.5rem',
                    border: '1px solid rgba(255, 255, 255, 0.2)',
                    background: 'rgba(255, 255, 255, 0.1)',
                    color: '#ffffff',
                    resize: 'vertical'
                  }}
                />
              </div>
            </div>
          </div>
        )}

        {/* Step 4: 確認 */}
        {step === 4 && (
          <div>
            <h3 style={{ fontSize: '1.5rem', fontWeight: '700', marginBottom: '2rem', textAlign: 'center' }}>
              ✅ 入力内容確認
            </h3>
            
            <div style={{
              background: 'rgba(0, 0, 0, 0.2)',
              padding: '2rem',
              borderRadius: '1rem',
              marginBottom: '2rem'
            }}>
              <div style={{ display: 'grid', gap: '1rem', fontSize: '0.9rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#b8b8b8' }}>会社名:</span>
                  <span style={{ fontWeight: '600' }}>{formData.companyName}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#b8b8b8' }}>担当者:</span>
                  <span style={{ fontWeight: '600' }}>{formData.contactName}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#b8b8b8' }}>業界:</span>
                  <span style={{ fontWeight: '600' }}>{formData.industry}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#b8b8b8' }}>予算:</span>
                  <span style={{ fontWeight: '600' }}>{formData.budget}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#b8b8b8' }}>緊急度:</span>
                  <span style={{ fontWeight: '600' }}>{formData.urgency}</span>
                </div>
                {(formData.features || []).length > 0 && (
                  <div>
                    <span style={{ color: '#b8b8b8' }}>希望機能:</span>
                    <div style={{ marginTop: '0.5rem' }}>
                      {(formData.features || []).map(feature => (
                        <span
                          key={feature}
                          style={{
                            display: 'inline-block',
                            background: 'rgba(78, 205, 196, 0.2)',
                            color: '#4ecdc4',
                            padding: '0.2rem 0.5rem',
                            borderRadius: '0.3rem',
                            fontSize: '0.7rem',
                            marginRight: '0.5rem',
                            marginBottom: '0.5rem'
                          }}
                        >
                          {feature}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div style={{
              background: 'rgba(78, 205, 196, 0.1)',
              border: '1px solid rgba(78, 205, 196, 0.3)',
              padding: '1.5rem',
              borderRadius: '1rem',
              textAlign: 'center',
              marginBottom: '2rem'
            }}>
              <p style={{ color: '#4ecdc4', fontWeight: '600', marginBottom: '0.5rem' }}>
                🤖 AI分析準備完了
              </p>
              <p style={{ color: '#d1d1d1', fontSize: '0.85rem' }}>
                入力内容を基に、最適なプラン・価格を自動算出します
              </p>
            </div>

            <button
              onClick={generateQuoteResult}
              style={{
                width: '100%',
                padding: '1.5rem',
                background: 'linear-gradient(135deg, #4ecdc4, #45b7d1)',
                border: 'none',
                borderRadius: '1rem',
                color: '#000000',
                fontWeight: '900',
                fontSize: '1.2rem',
                cursor: 'pointer',
                transition: 'all 0.3s'
              }}
            >
              🚀 AI見積もり生成開始！
            </button>
          </div>
        )}

        {/* ナビゲーション */}
        <div style={{
          marginTop: '3rem',
          display: 'flex',
          justifyContent: 'space-between'
        }}>
          {step > 1 && (
            <button
              onClick={() => setStep(step - 1)}
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

          {step < totalSteps && (
            <button
              onClick={() => setStep(step + 1)}
              disabled={step === 1 && (!formData.companyName || !formData.contactName || !formData.email)}
              style={{
                padding: '0.75rem 2rem',
                background: 'linear-gradient(135deg, #4ecdc4, #45b7d1)',
                border: 'none',
                borderRadius: '0.5rem',
                color: '#000000',
                fontWeight: '700',
                cursor: 'pointer',
                opacity: (step === 1 && (!formData.companyName || !formData.contactName || !formData.email)) ? 0.5 : 1
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