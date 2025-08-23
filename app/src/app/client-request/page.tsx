'use client'

import React, { useState, useEffect } from 'react'

interface FormData {
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
}

export default function ClientRequestPage() {
  const [currentStep, setCurrentStep] = useState(1)
  const [formData, setFormData] = useState<FormData>({
    companyName: '',
    contactName: '',
    email: '',
    phone: '',
    industry: '',
    companySize: '',
    productName: '',
    productDescription: '',
    productPrice: '',
    targetAge: '',
    targetGender: '',
    targetOccupation: '',
    monthlyGoalCV: '',
    currentCVR: '',
    budget: '',
    deadline: '',
    competitors: '',
    specialRequests: ''
  })
  const [isLoading, setIsLoading] = useState(false)
  const [submitSuccess, setSubmitSuccess] = useState(false)

  // 自動保存機能
  useEffect(() => {
    const savedData = localStorage.getItem('growth-ai-form-data')
    if (savedData) {
      setFormData(JSON.parse(savedData))
    }
  }, [])

  useEffect(() => {
    localStorage.setItem('growth-ai-form-data', JSON.stringify(formData))
  }, [formData])

  const totalSteps = 4
  const progress = (currentStep / totalSteps) * 100

  const handleSubmit = async () => {
    setIsLoading(true)
    
    // AI分析・提案生成のシミュレーション
    setTimeout(() => {
      setIsLoading(false)
      setSubmitSuccess(true)
      
      // 自動メール送信シミュレーション
      console.log('📧 依頼者にメール送信完了')
      console.log('🤖 AI分析開始')
    }, 2000)
  }

  const handleInputChange = (field: keyof FormData, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }))
  }

  if (submitSuccess) {
    return (
      <div style={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #111827 0%, #000000 50%, #581c87 100%)',
        color: '#ffffff',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '2rem'
      }}>
        <div style={{
          background: 'rgba(0, 0, 0, 0.5)',
          backdropFilter: 'blur(24px)',
          border: '1px solid rgba(255, 255, 255, 0.1)',
          borderRadius: '2rem',
          padding: '3rem',
          textAlign: 'center',
          maxWidth: '600px'
        }}>
          <div style={{ fontSize: '4rem', marginBottom: '1rem' }}>✅</div>
          <h1 style={{ fontSize: '2rem', fontWeight: '900', marginBottom: '1rem' }}>
            ご依頼ありがとうございます！
          </h1>
          <p style={{ color: '#9ca3af', marginBottom: '2rem', lineHeight: '1.6' }}>
            AI分析を開始しました。30分以内に詳細な提案をメールでお送りします。<br/>
            史上最高のLPを制作いたします！
          </p>
          <div style={{
            background: 'linear-gradient(135deg, #facc15, #f97316)',
            color: '#000000',
            padding: '1rem 2rem',
            borderRadius: '1rem',
            fontWeight: '700'
          }}>
            📊 AI分析進行中... 99.2%完了
          </div>
        </div>
      </div>
    )
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
        <div style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '0.75rem',
          marginBottom: '1rem'
        }}>
          <div style={{
            width: '3rem',
            height: '3rem',
            background: 'linear-gradient(135deg, #facc15, #f97316)',
            borderRadius: '1rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <span style={{ color: '#000000', fontWeight: '900', fontSize: '1.5rem' }}>G</span>
          </div>
          <div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: '900', margin: 0 }}>GROWTH AI</h1>
            <p style={{ fontSize: '0.875rem', color: '#9ca3af', margin: 0 }}>Premium LP制作依頼フォーム</p>
          </div>
        </div>
        
        <h2 style={{ fontSize: '2.5rem', fontWeight: '900', marginBottom: '1rem' }}>
          <span style={{
            background: 'linear-gradient(to right, #facc15, #f97316)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            color: 'transparent'
          }}>
            史上最高のLP制作開始
          </span>
        </h2>
        
        <p style={{ color: '#9ca3af', marginBottom: '2rem' }}>
          Google級AI技術で、ROI 40,000%のプロ級LPを30秒で生成
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
            background: 'linear-gradient(to right, #facc15, #f97316)',
            height: '100%',
            width: `${progress}%`,
            transition: 'width 0.3s ease'
          }}></div>
        </div>
        <p style={{ fontSize: '0.875rem', color: '#9ca3af' }}>
          Step {currentStep} / {totalSteps} - {Math.round(progress)}% 完了
        </p>
      </div>

      {/* フォームコンテンツ */}
      <div style={{
        maxWidth: '800px',
        margin: '0 auto',
        background: 'rgba(0, 0, 0, 0.5)',
        backdropFilter: 'blur(24px)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        borderRadius: '2rem',
        padding: '3rem'
      }}>
        {/* Step 1: 基本情報 */}
        {currentStep === 1 && (
          <div>
            <h3 style={{ fontSize: '1.75rem', fontWeight: '700', marginBottom: '2rem', textAlign: 'center' }}>
              📋 基本情報
            </h3>
            
            <div style={{ display: 'grid', gap: '1.5rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem' }}>
                  会社名 *
                </label>
                <input
                  type="text"
                  value={formData.companyName}
                  onChange={(e) => handleInputChange('companyName', e.target.value)}
                  placeholder="株式会社○○○"
                  style={{
                    width: '100%',
                    padding: '0.75rem 1rem',
                    border: '1px solid rgba(255, 255, 255, 0.2)',
                    borderRadius: '0.5rem',
                    background: 'rgba(255, 255, 255, 0.1)',
                    color: '#ffffff',
                    fontSize: '1rem'
                  }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem' }}>
                    担当者名 *
                  </label>
                  <input
                    type="text"
                    value={formData.contactName}
                    onChange={(e) => handleInputChange('contactName', e.target.value)}
                    placeholder="山田太郎"
                    style={{
                      width: '100%',
                      padding: '0.75rem 1rem',
                      border: '1px solid rgba(255, 255, 255, 0.2)',
                      borderRadius: '0.5rem',
                      background: 'rgba(255, 255, 255, 0.1)',
                      color: '#ffffff',
                      fontSize: '1rem'
                    }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem' }}>
                    メールアドレス *
                  </label>
                  <input
                    type="email"
                    value={formData.email}
                    onChange={(e) => handleInputChange('email', e.target.value)}
                    placeholder="info@company.com"
                    style={{
                      width: '100%',
                      padding: '0.75rem 1rem',
                      border: '1px solid rgba(255, 255, 255, 0.2)',
                      borderRadius: '0.5rem',
                      background: 'rgba(255, 255, 255, 0.1)',
                      color: '#ffffff',
                      fontSize: '1rem'
                    }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem' }}>
                    業界
                  </label>
                  <select
                    value={formData.industry}
                    onChange={(e) => handleInputChange('industry', e.target.value)}
                    style={{
                      width: '100%',
                      padding: '0.75rem 1rem',
                      border: '1px solid rgba(255, 255, 255, 0.2)',
                      borderRadius: '0.5rem',
                      background: 'rgba(255, 255, 255, 0.1)',
                      color: '#ffffff',
                      fontSize: '1rem'
                    }}
                  >
                    <option value="">選択してください</option>
                    <option value="IT・SaaS">IT・SaaS</option>
                    <option value="ヘルスケア">ヘルスケア・医療</option>
                    <option value="教育">教育・研修</option>
                    <option value="製造業">製造業・B2B</option>
                    <option value="金融">金融・保険</option>
                    <option value="EC・小売">EC・小売</option>
                    <option value="不動産">不動産・建設</option>
                    <option value="その他">その他</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem' }}>
                    従業員規模
                  </label>
                  <select
                    value={formData.companySize}
                    onChange={(e) => handleInputChange('companySize', e.target.value)}
                    style={{
                      width: '100%',
                      padding: '0.75rem 1rem',
                      border: '1px solid rgba(255, 255, 255, 0.2)',
                      borderRadius: '0.5rem',
                      background: 'rgba(255, 255, 255, 0.1)',
                      color: '#ffffff',
                      fontSize: '1rem'
                    }}
                  >
                    <option value="">選択してください</option>
                    <option value="1-10名">1-10名（スタートアップ）</option>
                    <option value="11-50名">11-50名（小企業）</option>
                    <option value="51-200名">51-200名（中企業）</option>
                    <option value="201-1000名">201-1000名（大企業）</option>
                    <option value="1000名以上">1000名以上（大手企業）</option>
                  </select>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Step 2: 商品・サービス情報 */}
        {currentStep === 2 && (
          <div>
            <h3 style={{ fontSize: '1.75rem', fontWeight: '700', marginBottom: '2rem', textAlign: 'center' }}>
              📦 商品・サービス情報
            </h3>
            
            <div style={{ display: 'grid', gap: '1.5rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem' }}>
                  商品・サービス名 *
                </label>
                <input
                  type="text"
                  value={formData.productName}
                  onChange={(e) => handleInputChange('productName', e.target.value)}
                  placeholder="AI マーケティングツール"
                  style={{
                    width: '100%',
                    padding: '0.75rem 1rem',
                    border: '1px solid rgba(255, 255, 255, 0.2)',
                    borderRadius: '0.5rem',
                    background: 'rgba(255, 255, 255, 0.1)',
                    color: '#ffffff',
                    fontSize: '1rem'
                  }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem' }}>
                  商品・サービスの特徴・強み *
                </label>
                <textarea
                  value={formData.productDescription}
                  onChange={(e) => handleInputChange('productDescription', e.target.value)}
                  placeholder="AIを活用して○○を自動化し、○○%のコスト削減を実現..."
                  rows={4}
                  style={{
                    width: '100%',
                    padding: '0.75rem 1rem',
                    border: '1px solid rgba(255, 255, 255, 0.2)',
                    borderRadius: '0.5rem',
                    background: 'rgba(255, 255, 255, 0.1)',
                    color: '#ffffff',
                    fontSize: '1rem',
                    resize: 'vertical'
                  }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem' }}>
                  価格帯
                </label>
                <select
                  value={formData.productPrice}
                  onChange={(e) => handleInputChange('productPrice', e.target.value)}
                  style={{
                    width: '100%',
                    padding: '0.75rem 1rem',
                    border: '1px solid rgba(255, 255, 255, 0.2)',
                    borderRadius: '0.5rem',
                    background: 'rgba(255, 255, 255, 0.1)',
                    color: '#ffffff',
                    fontSize: '1rem'
                  }}
                >
                  <option value="">選択してください</option>
                  <option value="〜1万円">〜1万円</option>
                  <option value="1-5万円">1-5万円</option>
                  <option value="5-10万円">5-10万円</option>
                  <option value="10-50万円">10-50万円</option>
                  <option value="50万円以上">50万円以上</option>
                </select>
              </div>
            </div>
          </div>
        )}

        {/* Step 3: ターゲット情報 */}
        {currentStep === 3 && (
          <div>
            <h3 style={{ fontSize: '1.75rem', fontWeight: '700', marginBottom: '2rem', textAlign: 'center' }}>
              🎯 ターゲット情報
            </h3>
            
            <div style={{ display: 'grid', gap: '1.5rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem' }}>
                    ターゲット年齢層
                  </label>
                  <select
                    value={formData.targetAge}
                    onChange={(e) => handleInputChange('targetAge', e.target.value)}
                    style={{
                      width: '100%',
                      padding: '0.75rem 1rem',
                      border: '1px solid rgba(255, 255, 255, 0.2)',
                      borderRadius: '0.5rem',
                      background: 'rgba(255, 255, 255, 0.1)',
                      color: '#ffffff',
                      fontSize: '1rem'
                    }}
                  >
                    <option value="">選択してください</option>
                    <option value="20-30代">20-30代</option>
                    <option value="30-40代">30-40代</option>
                    <option value="40-50代">40-50代</option>
                    <option value="50代以上">50代以上</option>
                    <option value="全年代">全年代</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem' }}>
                    ターゲット性別
                  </label>
                  <select
                    value={formData.targetGender}
                    onChange={(e) => handleInputChange('targetGender', e.target.value)}
                    style={{
                      width: '100%',
                      padding: '0.75rem 1rem',
                      border: '1px solid rgba(255, 255, 255, 0.2)',
                      borderRadius: '0.5rem',
                      background: 'rgba(255, 255, 255, 0.1)',
                      color: '#ffffff',
                      fontSize: '1rem'
                    }}
                  >
                    <option value="">選択してください</option>
                    <option value="男性">男性</option>
                    <option value="女性">女性</option>
                    <option value="問わず">問わず</option>
                  </select>
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem' }}>
                  ターゲット職業・属性 *
                </label>
                <input
                  type="text"
                  value={formData.targetOccupation}
                  onChange={(e) => handleInputChange('targetOccupation', e.target.value)}
                  placeholder="経営者・マーケティング担当者・営業責任者など"
                  style={{
                    width: '100%',
                    padding: '0.75rem 1rem',
                    border: '1px solid rgba(255, 255, 255, 0.2)',
                    borderRadius: '0.5rem',
                    background: 'rgba(255, 255, 255, 0.1)',
                    color: '#ffffff',
                    fontSize: '1rem'
                  }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem' }}>
                  主要競合他社（3社まで）
                </label>
                <textarea
                  value={formData.competitors}
                  onChange={(e) => handleInputChange('competitors', e.target.value)}
                  placeholder="A社：https://a-company.com（価格・特徴）&#10;B社：https://b-company.com（価格・特徴）&#10;C社：https://c-company.com（価格・特徴）"
                  rows={3}
                  style={{
                    width: '100%',
                    padding: '0.75rem 1rem',
                    border: '1px solid rgba(255, 255, 255, 0.2)',
                    borderRadius: '0.5rem',
                    background: 'rgba(255, 255, 255, 0.1)',
                    color: '#ffffff',
                    fontSize: '1rem',
                    resize: 'vertical'
                  }}
                />
              </div>
            </div>
          </div>
        )}

        {/* Step 4: 目標・予算 */}
        {currentStep === 4 && (
          <div>
            <h3 style={{ fontSize: '1.75rem', fontWeight: '700', marginBottom: '2rem', textAlign: 'center' }}>
              🎯 目標・予算設定
            </h3>
            
            <div style={{ display: 'grid', gap: '1.5rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem' }}>
                    月間目標CV数 *
                  </label>
                  <input
                    type="number"
                    value={formData.monthlyGoalCV}
                    onChange={(e) => handleInputChange('monthlyGoalCV', e.target.value)}
                    placeholder="100"
                    style={{
                      width: '100%',
                      padding: '0.75rem 1rem',
                      border: '1px solid rgba(255, 255, 255, 0.2)',
                      borderRadius: '0.5rem',
                      background: 'rgba(255, 255, 255, 0.1)',
                      color: '#ffffff',
                      fontSize: '1rem'
                    }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem' }}>
                    現在のCVR（%）
                  </label>
                  <input
                    type="number"
                    value={formData.currentCVR}
                    onChange={(e) => handleInputChange('currentCVR', e.target.value)}
                    placeholder="2.5"
                    step="0.1"
                    style={{
                      width: '100%',
                      padding: '0.75rem 1rem',
                      border: '1px solid rgba(255, 255, 255, 0.2)',
                      borderRadius: '0.5rem',
                      background: 'rgba(255, 255, 255, 0.1)',
                      color: '#ffffff',
                      fontSize: '1rem'
                    }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem' }}>
                    予算感
                  </label>
                  <select
                    value={formData.budget}
                    onChange={(e) => handleInputChange('budget', e.target.value)}
                    style={{
                      width: '100%',
                      padding: '0.75rem 1rem',
                      border: '1px solid rgba(255, 255, 255, 0.2)',
                      borderRadius: '0.5rem',
                      background: 'rgba(255, 255, 255, 0.1)',
                      color: '#ffffff',
                      fontSize: '1rem'
                    }}
                  >
                    <option value="">選択してください</option>
                    <option value="〜10万円">〜10万円</option>
                    <option value="10-30万円">10-30万円</option>
                    <option value="30-50万円">30-50万円</option>
                    <option value="50-100万円">50-100万円</option>
                    <option value="100万円以上">100万円以上</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem' }}>
                    希望納期
                  </label>
                  <select
                    value={formData.deadline}
                    onChange={(e) => handleInputChange('deadline', e.target.value)}
                    style={{
                      width: '100%',
                      padding: '0.75rem 1rem',
                      border: '1px solid rgba(255, 255, 255, 0.2)',
                      borderRadius: '0.5rem',
                      background: 'rgba(255, 255, 255, 0.1)',
                      color: '#ffffff',
                      fontSize: '1rem'
                    }}
                  >
                    <option value="">選択してください</option>
                    <option value="即日">即日（30秒生成）</option>
                    <option value="1週間以内">1週間以内</option>
                    <option value="2週間以内">2週間以内</option>
                    <option value="1ヶ月以内">1ヶ月以内</option>
                    <option value="相談">相談</option>
                  </select>
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem' }}>
                  特別なご要望・こだわり
                </label>
                <textarea
                  value={formData.specialRequests}
                  onChange={(e) => handleInputChange('specialRequests', e.target.value)}
                  placeholder="ブランドカラーの統一、特定のデザインイメージ、避けたい表現など..."
                  rows={3}
                  style={{
                    width: '100%',
                    padding: '0.75rem 1rem',
                    border: '1px solid rgba(255, 255, 255, 0.2)',
                    borderRadius: '0.5rem',
                    background: 'rgba(255, 255, 255, 0.1)',
                    color: '#ffffff',
                    fontSize: '1rem',
                    resize: 'vertical'
                  }}
                />
              </div>
            </div>
          </div>
        )}

        {/* ナビゲーションボタン */}
        <div style={{
          marginTop: '3rem',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          {currentStep > 1 ? (
            <button
              onClick={() => setCurrentStep(currentStep - 1)}
              style={{
                padding: '0.75rem 1.5rem',
                background: 'rgba(255, 255, 255, 0.1)',
                border: '1px solid rgba(255, 255, 255, 0.2)',
                borderRadius: '0.5rem',
                color: '#ffffff',
                fontWeight: '600',
                cursor: 'pointer',
                transition: 'all 0.3s'
              }}
            >
              ← 前へ
            </button>
          ) : <div></div>}

          {currentStep < totalSteps ? (
            <button
              onClick={() => setCurrentStep(currentStep + 1)}
              style={{
                padding: '0.75rem 2rem',
                background: 'linear-gradient(135deg, #facc15, #f97316)',
                border: 'none',
                borderRadius: '0.5rem',
                color: '#000000',
                fontWeight: '700',
                cursor: 'pointer',
                transition: 'all 0.3s'
              }}
            >
              次へ →
            </button>
          ) : (
            <button
              onClick={handleSubmit}
              disabled={isLoading}
              style={{
                padding: '1rem 3rem',
                background: isLoading 
                  ? 'rgba(255, 255, 255, 0.2)' 
                  : 'linear-gradient(135deg, #facc15, #f97316)',
                border: 'none',
                borderRadius: '0.5rem',
                color: isLoading ? '#9ca3af' : '#000000',
                fontWeight: '900',
                fontSize: '1.125rem',
                cursor: isLoading ? 'not-allowed' : 'pointer',
                transition: 'all 0.3s'
              }}
            >
              {isLoading ? (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <div style={{
                    width: '1rem',
                    height: '1rem',
                    border: '2px solid #9ca3af',
                    borderTop: '2px solid #facc15',
                    borderRadius: '50%',
                    animation: 'spin 1s linear infinite'
                  }}></div>
                  AI分析中...
                </div>
              ) : (
                '🚀 史上最高LP制作開始！'
              )}
            </button>
          )}
        </div>

        {/* 自動保存表示 */}
        <div style={{
          marginTop: '2rem',
          textAlign: 'center',
          fontSize: '0.75rem',
          color: '#9ca3af'
        }}>
          💾 入力内容は自動保存されています
        </div>
      </div>

      <style dangerouslySetInnerHTML={{
        __html: `
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
          
          input::placeholder, textarea::placeholder {
            color: #9ca3af;
          }
          
          select option {
            background: #1f2937;
            color: #ffffff;
          }
          
          @media (max-width: 768px) {
            .grid-2-cols {
              grid-template-columns: 1fr !important;
            }
          }
        `
      }} />
    </div>
  )
}