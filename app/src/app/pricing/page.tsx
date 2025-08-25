'use client'

import React, { useState } from 'react'
import { LP_PRICING_TIERS, ADDITIONAL_SERVICES, calculatePrice, generateQuote } from '@/config/pricing'

export default function PricingPage() {
  const [selectedTier, setSelectedTier] = useState('')
  const [selectedServices, setSelectedServices] = useState<string[]>([])
  const [showQuoteForm, setShowQuoteForm] = useState(false)
  const [quoteData, setQuoteData] = useState<any>(null)

  const handleServiceToggle = (serviceId: string) => {
    if (selectedServices.includes(serviceId)) {
      setSelectedServices(selectedServices.filter(id => id !== serviceId))
    } else {
      setSelectedServices([...selectedServices, serviceId])
    }
  }

  const calculateTotalPrice = () => {
    if (!selectedTier) return null
    return calculatePrice(selectedTier, selectedServices, 'launch_campaign')
  }

  const handleQuoteRequest = (formData: any) => {
    const quote = generateQuote(formData)
    setQuoteData(quote)
  }

  const priceCalc = calculateTotalPrice()

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #0f0f23 0%, #1a1a2e 50%, #16213e 100%)',
      color: '#ffffff',
      padding: '2rem'
    }}>
      {/* ヘッダー */}
      <div style={{ textAlign: 'center', marginBottom: '4rem', paddingTop: '2rem' }}>
        <h1 style={{
          fontSize: 'clamp(2.5rem, 6vw, 4rem)',
          fontWeight: '900',
          marginBottom: '1rem'
        }}>
          <span style={{
            background: 'linear-gradient(135deg, #ff6b6b, #4ecdc4)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            color: 'transparent'
          }}>
            LP制作料金プラン
          </span>
        </h1>
        <p style={{ fontSize: '1.2rem', color: '#b8b8b8', marginBottom: '2rem' }}>
          実証データに基づく確実な成果。CVR平均35%向上を実現。
        </p>
        
        {/* 実績バッジ */}
        <div style={{
          display: 'inline-flex',
          gap: '2rem',
          padding: '1rem 2rem',
          background: 'rgba(78, 205, 196, 0.1)',
          border: '1px solid rgba(78, 205, 196, 0.3)',
          borderRadius: '50px',
          fontSize: '0.9rem',
          fontWeight: '600'
        }}>
          <span>✅ 制作実績100社+</span>
          <span>⚡ 平均制作時間30秒</span>
          <span>📊 満足度98%</span>
        </div>
      </div>

      {/* 料金プラン */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))',
        gap: '2rem',
        maxWidth: '1200px',
        margin: '0 auto 4rem'
      }}>
        {LP_PRICING_TIERS.map((tier, index) => (
          <div
            key={tier.id}
            onClick={() => setSelectedTier(tier.id)}
            style={{
              background: selectedTier === tier.id 
                ? 'linear-gradient(135deg, rgba(255, 107, 107, 0.2), rgba(78, 205, 196, 0.2))'
                : 'rgba(255, 255, 255, 0.05)',
              backdropFilter: 'blur(20px)',
              border: selectedTier === tier.id 
                ? '2px solid #4ecdc4' 
                : '1px solid rgba(255, 255, 255, 0.1)',
              borderRadius: '2rem',
              padding: '2.5rem',
              cursor: 'pointer',
              transition: 'all 0.3s ease',
              position: 'relative',
              transform: index === 1 ? 'scale(1.05)' : 'scale(1)'
            }}
          >
            {/* 人気バッジ */}
            {index === 1 && (
              <div style={{
                position: 'absolute',
                top: '-1rem',
                left: '50%',
                transform: 'translateX(-50%)',
                background: 'linear-gradient(135deg, #ff6b6b, #ee5a24)',
                color: '#ffffff',
                padding: '0.5rem 1.5rem',
                borderRadius: '50px',
                fontSize: '0.8rem',
                fontWeight: '700'
              }}>
                🔥 人気No.1
              </div>
            )}

            {/* 価格表示 */}
            <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
              <h3 style={{
                fontSize: '1.5rem',
                fontWeight: '700',
                marginBottom: '1rem',
                color: '#ffffff'
              }}>
                {tier.name}
              </h3>
              
              {tier.originalPrice && (
                <div style={{
                  fontSize: '1.2rem',
                  color: '#888',
                  textDecoration: 'line-through',
                  marginBottom: '0.5rem'
                }}>
                  ¥{tier.originalPrice.toLocaleString()}
                </div>
              )}
              
              <div style={{
                fontSize: '3rem',
                fontWeight: '900',
                background: 'linear-gradient(135deg, #4ecdc4, #45b7d1)',
                backgroundClip: 'text',
                WebkitBackgroundClip: 'text',
                color: 'transparent',
                marginBottom: '0.5rem'
              }}>
                ¥{tier.price.toLocaleString()}
              </div>
              
              <div style={{ color: '#b8b8b8', fontSize: '0.9rem' }}>
                /枚（{tier.testPatterns}パターン込み）
              </div>
            </div>

            {/* 機能一覧 */}
            <div style={{ marginBottom: '2rem' }}>
              <p style={{ color: '#d1d1d1', marginBottom: '1rem', fontSize: '0.95rem' }}>
                {tier.description}
              </p>
              
              <ul style={{
                listStyle: 'none',
                padding: 0
              }}>
                {tier.features.map((feature, i) => (
                  <li key={i} style={{
                    padding: '0.4rem 0',
                    color: '#e1e1e1',
                    fontSize: '0.85rem',
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '0.5rem'
                  }}>
                    <span style={{ color: '#4ecdc4', minWidth: '1rem' }}>✅</span>
                    <span>{feature}</span>
                  </li>
                ))}
              </ul>
            </div>

            {/* 詳細情報 */}
            <div style={{
              background: 'rgba(0, 0, 0, 0.2)',
              padding: '1rem',
              borderRadius: '1rem',
              fontSize: '0.8rem',
              color: '#b8b8b8'
            }}>
              <div style={{ marginBottom: '0.5rem' }}>
                ⏰ 納期: {tier.deliveryTime}
              </div>
              <div style={{ marginBottom: '0.5rem' }}>
                🔄 修正: {tier.revisions === -1 ? '無制限' : `${tier.revisions}回まで`}
              </div>
              <div>
                👥 対象: {tier.target.join('・')}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* 追加サービス */}
      {selectedTier && (
        <div style={{
          background: 'rgba(255, 255, 255, 0.05)',
          backdropFilter: 'blur(20px)',
          border: '1px solid rgba(255, 255, 255, 0.1)',
          borderRadius: '2rem',
          padding: '3rem',
          maxWidth: '1000px',
          margin: '0 auto 4rem'
        }}>
          <h3 style={{
            fontSize: '1.8rem',
            fontWeight: '700',
            textAlign: 'center',
            marginBottom: '2rem',
            color: '#4ecdc4'
          }}>
            📈 追加サービス（オプション）
          </h3>

          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
            gap: '1rem'
          }}>
            {ADDITIONAL_SERVICES.map((service) => (
              <div
                key={service.id}
                onClick={() => handleServiceToggle(service.id)}
                style={{
                  background: selectedServices.includes(service.id)
                    ? 'rgba(78, 205, 196, 0.2)'
                    : 'rgba(255, 255, 255, 0.05)',
                  border: selectedServices.includes(service.id)
                    ? '2px solid #4ecdc4'
                    : '1px solid rgba(255, 255, 255, 0.1)',
                  borderRadius: '1rem',
                  padding: '1.5rem',
                  cursor: 'pointer',
                  transition: 'all 0.3s ease'
                }}
              >
                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'flex-start',
                  marginBottom: '1rem'
                }}>
                  <h4 style={{
                    fontSize: '1.1rem',
                    fontWeight: '600',
                    color: '#ffffff'
                  }}>
                    {service.name}
                  </h4>
                  <div style={{
                    fontSize: '1rem',
                    fontWeight: '700',
                    color: '#4ecdc4'
                  }}>
                    ¥{service.price.toLocaleString()}
                    <span style={{ fontSize: '0.7rem', color: '#888' }}>
                      /{service.unit === 'per_month' ? '月' : service.unit === 'per_project' ? '件' : '成果'}
                    </span>
                  </div>
                </div>
                
                <p style={{
                  color: '#d1d1d1',
                  fontSize: '0.85rem',
                  lineHeight: '1.5'
                }}>
                  {service.description}
                </p>
                
                {selectedServices.includes(service.id) && (
                  <div style={{
                    marginTop: '1rem',
                    color: '#4ecdc4',
                    fontSize: '0.8rem',
                    fontWeight: '600'
                  }}>
                    ✅ 選択済み
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 料金計算結果 */}
      {priceCalc && (
        <div style={{
          background: 'rgba(255, 107, 107, 0.1)',
          border: '1px solid rgba(255, 107, 107, 0.3)',
          borderRadius: '2rem',
          padding: '2rem',
          maxWidth: '600px',
          margin: '0 auto 4rem',
          textAlign: 'center'
        }}>
          <h3 style={{
            fontSize: '1.5rem',
            fontWeight: '700',
            marginBottom: '1rem',
            color: '#ff6b6b'
          }}>
            💰 お見積もり
          </h3>

          <div style={{ marginBottom: '1rem' }}>
            {priceCalc.breakdown.map((item, i) => (
              <div key={i} style={{
                display: 'flex',
                justifyContent: 'space-between',
                padding: '0.5rem 0',
                borderBottom: i === priceCalc.breakdown.length - 1 ? 'none' : '1px solid rgba(255, 255, 255, 0.1)'
              }}>
                <span style={{ color: item.type === 'discount' ? '#4ecdc4' : '#d1d1d1' }}>
                  {item.item}
                </span>
                <span style={{
                  fontWeight: '600',
                  color: item.type === 'discount' ? '#4ecdc4' : item.price > 0 ? '#ffffff' : '#ff6b6b'
                }}>
                  {item.price > 0 ? '+' : ''}¥{Math.abs(item.price).toLocaleString()}
                </span>
              </div>
            ))}
          </div>

          <div style={{
            fontSize: '2rem',
            fontWeight: '900',
            background: 'linear-gradient(135deg, #4ecdc4, #45b7d1)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            color: 'transparent',
            marginBottom: '1rem'
          }}>
            合計: ¥{priceCalc.finalPrice.toLocaleString()}
          </div>

          {priceCalc.discount > 0 && (
            <div style={{
              background: '#4ecdc4',
              color: '#000000',
              padding: '0.5rem 1rem',
              borderRadius: '50px',
              fontSize: '0.8rem',
              fontWeight: '700',
              display: 'inline-block',
              marginBottom: '1rem'
            }}>
              🎉 ローンチ記念 ¥{priceCalc.discount.toLocaleString()}割引適用中！
            </div>
          )}

          <button
            onClick={() => setShowQuoteForm(true)}
            style={{
              padding: '1rem 2rem',
              background: 'linear-gradient(135deg, #ff6b6b, #ee5a24)',
              border: 'none',
              borderRadius: '0.5rem',
              color: '#ffffff',
              fontWeight: '700',
              cursor: 'pointer',
              transition: 'all 0.3s'
            }}
          >
            📋 正式見積もりを依頼
          </button>
        </div>
      )}

      {/* 見積もりフォーム */}
      {showQuoteForm && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0, 0, 0, 0.8)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          padding: '2rem'
        }}>
          <div style={{
            background: 'linear-gradient(135deg, #1a1a2e, #16213e)',
            borderRadius: '2rem',
            padding: '3rem',
            maxWidth: '600px',
            width: '100%',
            position: 'relative'
          }}>
            <button
              onClick={() => setShowQuoteForm(false)}
              style={{
                position: 'absolute',
                top: '1rem',
                right: '1rem',
                background: 'none',
                border: 'none',
                color: '#ffffff',
                fontSize: '1.5rem',
                cursor: 'pointer'
              }}
            >
              ×
            </button>

            <h3 style={{
              fontSize: '1.8rem',
              fontWeight: '700',
              marginBottom: '2rem',
              textAlign: 'center',
              color: '#4ecdc4'
            }}>
              📋 無料お見積もり
            </h3>

            <form onSubmit={(e) => {
              e.preventDefault()
              const formData = new FormData(e.target as HTMLFormElement)
              const data = {
                companySize: formData.get('companySize') as string,
                industry: formData.get('industry') as string,
                budget: formData.get('budget') as string,
                urgency: formData.get('urgency') as string,
                features: []
              }
              handleQuoteRequest(data)
            }}>
              <div style={{ display: 'grid', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                    会社規模
                  </label>
                  <select
                    name="companySize"
                    required
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
                    <option value="small">小企業（従業員50名以下）</option>
                    <option value="medium">中企業（従業員51-200名）</option>
                    <option value="large">大企業（従業員200名以上）</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                    業界
                  </label>
                  <select
                    name="industry"
                    required
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

                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                    ご予算
                  </label>
                  <select
                    name="budget"
                    required
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
                    <option value="〜5万円">〜5万円</option>
                    <option value="5-10万円">5-10万円</option>
                    <option value="10-20万円">10-20万円</option>
                    <option value="20万円以上">20万円以上</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                    緊急度
                  </label>
                  <select
                    name="urgency"
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      borderRadius: '0.5rem',
                      border: '1px solid rgba(255, 255, 255, 0.2)',
                      background: 'rgba(255, 255, 255, 0.1)',
                      color: '#ffffff'
                    }}
                  >
                    <option value="normal">通常（1-3時間）</option>
                    <option value="urgent">急ぎ（1時間以内）</option>
                    <option value="emergency">緊急（30分以内）</option>
                  </select>
                </div>

                <button
                  type="submit"
                  style={{
                    width: '100%',
                    padding: '1rem',
                    background: 'linear-gradient(135deg, #4ecdc4, #45b7d1)',
                    border: 'none',
                    borderRadius: '0.5rem',
                    color: '#000000',
                    fontWeight: '700',
                    fontSize: '1rem',
                    cursor: 'pointer',
                    marginTop: '1rem'
                  }}
                >
                  💰 最適プラン・見積もり取得
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 見積もり結果 */}
      {quoteData && (
        <div style={{
          background: 'rgba(78, 205, 196, 0.1)',
          border: '1px solid rgba(78, 205, 196, 0.3)',
          borderRadius: '2rem',
          padding: '3rem',
          maxWidth: '800px',
          margin: '0 auto 4rem',
          textAlign: 'center'
        }}>
          <h3 style={{
            fontSize: '2rem',
            fontWeight: '900',
            marginBottom: '2rem',
            color: '#4ecdc4'
          }}>
            🎯 あなたに最適なプラン
          </h3>

          <div style={{
            background: 'rgba(0, 0, 0, 0.3)',
            borderRadius: '1rem',
            padding: '2rem',
            marginBottom: '2rem'
          }}>
            <h4 style={{ fontSize: '1.3rem', marginBottom: '1rem', color: '#ffffff' }}>
              推奨: {quoteData.tier.name}
            </h4>
            <div style={{
              fontSize: '2.5rem',
              fontWeight: '900',
              color: '#4ecdc4',
              marginBottom: '1rem'
            }}>
              ¥{quoteData.finalPrice.toLocaleString()}
            </div>
            <div style={{ color: '#b8b8b8', fontSize: '0.9rem' }}>
              納期: {quoteData.estimatedDelivery} | 
              予想CVR改善: {quoteData.expectedResults.cvrImprovement} |
              期待ROI: {quoteData.expectedResults.roiExpected}
            </div>
          </div>

          <a
            href={`mailto:lp-pro@example.com?subject=お見積もり依頼&body=推奨プラン: ${quoteData.tier.name}%0D%0A合計金額: ¥${quoteData.finalPrice.toLocaleString()}%0D%0A%0D%0A詳細なご相談をさせていただきたく、ご連絡いたします。`}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem',
              padding: '1rem 2rem',
              background: 'linear-gradient(135deg, #ff6b6b, #ee5a24)',
              color: '#ffffff',
              textDecoration: 'none',
              borderRadius: '0.5rem',
              fontWeight: '700',
              transition: 'all 0.3s'
            }}
          >
            📧 この内容で正式依頼
          </a>
        </div>
      )}

      {/* よくある質問 */}
      <div style={{
        background: 'rgba(255, 255, 255, 0.02)',
        padding: '4rem 2rem',
        textAlign: 'center'
      }}>
        <div className="container">
          <h3 style={{
            fontSize: '2rem',
            fontWeight: '700',
            marginBottom: '3rem',
            color: '#4ecdc4'
          }}>
            💡 よくある質問
          </h3>

          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))',
            gap: '2rem',
            textAlign: 'left'
          }}>
            <div style={{
              background: 'rgba(255, 255, 255, 0.05)',
              padding: '2rem',
              borderRadius: '1rem'
            }}>
              <h4 style={{ color: '#ffffff', marginBottom: '1rem' }}>
                Q: 本当に30秒でLPが完成しますか？
              </h4>
              <p style={{ color: '#d1d1d1', lineHeight: '1.6' }}>
                A: はい。AI自動生成により、基本的なLPは30秒〜1分で完成します。
                プレミアムプランの場合、プロ監修により2-6時間お時間をいただきます。
              </p>
            </div>

            <div style={{
              background: 'rgba(255, 255, 255, 0.05)',
              padding: '2rem',
              borderRadius: '1rem'
            }}>
              <h4 style={{ color: '#ffffff', marginBottom: '1rem' }}>
                Q: CVR向上は本当に実現できますか？
              </h4>
              <p style={{ color: '#d1d1d1', lineHeight: '1.6' }}>
                A: 過去100社以上の実績で、平均35%のCVR向上を実現しています。
                業界・現状により異なりますが、改善保証制度もご用意しています。
              </p>
            </div>

            <div style={{
              background: 'rgba(255, 255, 255, 0.05)',
              padding: '2rem',
              borderRadius: '1rem'
            }}>
              <h4 style={{ color: '#ffffff', marginBottom: '1rem' }}>
                Q: 追加費用は発生しますか？
              </h4>
              <p style={{ color: '#d1d1d1', lineHeight: '1.6' }}>
                A: 基本プランに含まれる範囲では追加費用は一切ありません。
                オプションサービスは事前にお見積もり・ご了承いただいてから実施します。
              </p>
            </div>

            <div style={{
              background: 'rgba(255, 255, 255, 0.05)',
              padding: '2rem',
              borderRadius: '1rem'
            }}>
              <h4 style={{ color: '#ffffff', marginBottom: '1rem' }}>
                Q: 継続的な改善サポートはありますか？
              </h4>
              <p style={{ color: '#d1d1d1', lineHeight: '1.6' }}>
                A: 月次最適化サービス（¥29,800/月〜）で、継続的な分析・改善を行います。
                データに基づく具体的な改善提案・実装をサポートいたします。
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* 最終CTA */}
      <div style={{
        textAlign: 'center',
        padding: '4rem 2rem',
        background: 'linear-gradient(135deg, #1a1a2e, #16213e)'
      }}>
        <h3 style={{
          fontSize: '2.5rem',
          fontWeight: '900',
          marginBottom: '2rem',
          background: 'linear-gradient(135deg, #ff6b6b, #4ecdc4)',
          backgroundClip: 'text',
          WebkitBackgroundClip: 'text',
          color: 'transparent'
        }}>
          今すぐ始めましょう
        </h3>
        
        <p style={{
          fontSize: '1.2rem',
          color: '#d1d1d1',
          marginBottom: '3rem'
        }}>
          無料相談で、あなたのビジネスに最適なLP制作プランをご提案します
        </p>

        <a
          href="mailto:lp-pro@example.com?subject=LP制作のご相談&body=LP制作についてご相談があります。%0D%0A%0D%0A【会社名】%0D%0A【ご担当者名】%0D%0A【業界】%0D%0A【現在の課題】%0D%0A【ご予算】%0D%0A【ご希望納期】%0D%0A%0D%0Aよろしくお願いいたします。"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '1rem',
            padding: '2rem 4rem',
            background: 'linear-gradient(135deg, #ff6b6b, #ee5a24)',
            color: '#ffffff',
            textDecoration: 'none',
            borderRadius: '1rem',
            fontWeight: '900',
            fontSize: '1.3rem',
            boxShadow: '0 10px 40px rgba(255, 107, 107, 0.4)',
            transition: 'all 0.3s'
          }}
        >
          🚀 無料相談を今すぐ申し込む
        </a>
      </div>
    </div>
  )
}