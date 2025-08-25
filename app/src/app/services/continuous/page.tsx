'use client'

import React, { useState } from 'react'

interface ContinuousService {
  id: string
  name: string
  price: number
  description: string
  features: string[]
  targetCustomers: string[]
  expectedResults: string[]
  minimumContract: string
}

export default function ContinuousServicesPage() {
  const [selectedService, setSelectedService] = useState<string>('')

  const services: ContinuousService[] = [
    {
      id: 'basic_optimization',
      name: '基本最適化サービス',
      price: 19800,
      description: 'AI分析による月次改善・最適化。継続的なパフォーマンス向上を実現。',
      features: [
        '月次ヒートマップ分析',
        'CVR・ROI改善提案（AI生成）',
        'A/Bテスト実行・結果分析',
        '基本レポート配信',
        'メールサポート'
      ],
      targetCustomers: ['小企業', '個人事業主', 'スタートアップ'],
      expectedResults: [
        'CVR +10-20%向上/月',
        'ROI +15-30%改善',
        '運用効率50%向上'
      ],
      minimumContract: '3ヶ月'
    },
    {
      id: 'pro_optimization',
      name: 'プロ最適化サービス',
      price: 49800,
      description: '詳細分析＋改善実装。プロマーケターレベルの継続サポート。',
      features: [
        '詳細ユーザー行動分析',
        '競合ベンチマーク分析',
        '改善施策の実装サポート',
        'カスタムA/Bテスト設計',
        '週次進捗ミーティング',
        'チャット・電話サポート',
        '緊急対応（24時間以内）'
      ],
      targetCustomers: ['中小企業', '成長企業', 'EC事業者'],
      expectedResults: [
        'CVR +20-40%向上/月',
        'ROI +50-100%改善',
        '新規獲得コスト30%削減'
      ],
      minimumContract: '6ヶ月'
    },
    {
      id: 'enterprise_optimization',
      name: 'エンタープライズ最適化',
      price: 98000,
      description: '専属チーム＋戦略コンサル。企業成長を包括的にサポート。',
      features: [
        '専属マーケティングチーム',
        'マーケティング戦略立案',
        '多変量テスト・高度分析',
        'カスタムダッシュボード構築',
        'チーム教育・研修',
        '他システム連携サポート',
        '成果保証制度',
        '専用サポート窓口'
      ],
      targetCustomers: ['中堅企業', '大企業', '重要プロジェクト'],
      expectedResults: [
        'CVR +30-60%向上/月',
        'ROI +100-300%改善',
        'マーケティング効率3倍向上'
      ],
      minimumContract: '12ヶ月'
    }
  ]

  const selectedServiceData = services.find(s => s.id === selectedService)

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
            background: 'linear-gradient(135deg, #4ecdc4, #45b7d1)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            color: 'transparent'
          }}>
            📈 継続最適化サービス
          </span>
        </h1>
        <p style={{ fontSize: '1.2rem', color: '#b8b8b8', marginBottom: '2rem' }}>
          LP制作後の継続的な改善で、長期的な成果を最大化
        </p>
        
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
          <span>🎯 LTV 300%向上</span>
          <span>📊 継続改善率95%</span>
          <span>⚡ 解約率5%以下</span>
        </div>
      </div>

      {/* サービス一覧 */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))',
        gap: '2rem',
        maxWidth: '1200px',
        margin: '0 auto 4rem'
      }}>
        {services.map((service, index) => (
          <div
            key={service.id}
            onClick={() => setSelectedService(service.id)}
            style={{
              background: selectedService === service.id
                ? 'linear-gradient(135deg, rgba(78, 205, 196, 0.2), rgba(69, 183, 209, 0.2))'
                : 'rgba(255, 255, 255, 0.05)',
              backdropFilter: 'blur(20px)',
              border: selectedService === service.id
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
                background: 'linear-gradient(135deg, #4ecdc4, #45b7d1)',
                color: '#000000',
                padding: '0.5rem 1.5rem',
                borderRadius: '50px',
                fontSize: '0.8rem',
                fontWeight: '700'
              }}>
                ⭐ おすすめ
              </div>
            )}

            <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
              <h3 style={{
                fontSize: '1.5rem',
                fontWeight: '700',
                marginBottom: '1rem',
                color: '#ffffff'
              }}>
                {service.name}
              </h3>
              
              <div style={{
                fontSize: '2.5rem',
                fontWeight: '900',
                background: 'linear-gradient(135deg, #4ecdc4, #45b7d1)',
                backgroundClip: 'text',
                WebkitBackgroundClip: 'text',
                color: 'transparent',
                marginBottom: '0.5rem'
              }}>
                ¥{service.price.toLocaleString()}
              </div>
              
              <div style={{ color: '#b8b8b8', fontSize: '0.9rem', marginBottom: '1rem' }}>
                /月（最低契約: {service.minimumContract}）
              </div>

              <p style={{ color: '#d1d1d1', fontSize: '0.95rem' }}>
                {service.description}
              </p>
            </div>

            {/* 機能一覧 */}
            <div style={{ marginBottom: '2rem' }}>
              <h4 style={{ fontSize: '1rem', fontWeight: '600', marginBottom: '1rem', color: '#4ecdc4' }}>
                📋 含まれるサービス
              </h4>
              <ul style={{ listStyle: 'none', padding: 0 }}>
                {service.features.map((feature, i) => (
                  <li key={i} style={{
                    padding: '0.3rem 0',
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

            {/* 期待される結果 */}
            <div style={{
              background: 'rgba(0, 0, 0, 0.2)',
              padding: '1rem',
              borderRadius: '1rem',
              marginBottom: '1rem'
            }}>
              <h4 style={{ fontSize: '0.9rem', fontWeight: '600', marginBottom: '0.5rem', color: '#45b7d1' }}>
                📊 期待される結果
              </h4>
              {service.expectedResults.map((result, i) => (
                <div key={i} style={{
                  fontSize: '0.8rem',
                  color: '#d1d1d1',
                  marginBottom: '0.2rem'
                }}>
                  • {result}
                </div>
              ))}
            </div>

            {/* ターゲット */}
            <div style={{ fontSize: '0.8rem', color: '#b8b8b8' }}>
              💼 推奨: {service.targetCustomers.join('・')}
            </div>
          </div>
        ))}
      </div>

      {/* 選択されたサービスの詳細 */}
      {selectedServiceData && (
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
            marginBottom: '1rem',
            color: '#4ecdc4'
          }}>
            🎯 選択中: {selectedServiceData.name}
          </h3>
          
          <div style={{
            fontSize: '2.5rem',
            fontWeight: '900',
            color: '#4ecdc4',
            marginBottom: '1rem'
          }}>
            ¥{selectedServiceData.price.toLocaleString()}/月
          </div>

          <div style={{
            background: 'rgba(0, 0, 0, 0.3)',
            padding: '2rem',
            borderRadius: '1rem',
            marginBottom: '2rem',
            textAlign: 'left'
          }}>
            <h4 style={{ marginBottom: '1rem', color: '#ffffff' }}>💡 このサービスの価値</h4>
            <div style={{ fontSize: '0.9rem', color: '#d1d1d1', lineHeight: '1.6' }}>
              <p>月額{selectedServiceData.price.toLocaleString()}円の投資で、</p>
              <ul style={{ margin: '1rem 0', paddingLeft: '1rem' }}>
                {selectedServiceData.expectedResults.map((result, i) => (
                  <li key={i} style={{ marginBottom: '0.5rem' }}>
                    📈 {result}
                  </li>
                ))}
              </ul>
              <p>
                <strong style={{ color: '#4ecdc4' }}>
                  投資回収期間: 1-3ヶ月 | 年間ROI: 300-800%期待
                </strong>
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
            <a
              href={`mailto:lp-pro@example.com?subject=${selectedServiceData.name}のお申し込み&body=${selectedServiceData.name}（¥${selectedServiceData.price.toLocaleString()}/月）のお申し込みを希望します。%0D%0A%0D%0A【会社名】%0D%0A【ご担当者名】%0D%0A【現在のLP URL】%0D%0A【改善したいポイント】%0D%0A%0D%0A詳細打ち合わせのご連絡をお待ちしております。`}
              style={{
                padding: '1rem 2rem',
                background: 'linear-gradient(135deg, #ff6b6b, #ee5a24)',
                color: '#ffffff',
                textDecoration: 'none',
                borderRadius: '0.5rem',
                fontWeight: '700',
                transition: 'all 0.3s'
              }}
            >
              📧 このプランで申し込む
            </a>
            
            <button
              onClick={() => setSelectedService('')}
              style={{
                padding: '1rem 2rem',
                background: 'rgba(255, 255, 255, 0.1)',
                border: '1px solid rgba(255, 255, 255, 0.2)',
                borderRadius: '0.5rem',
                color: '#ffffff',
                cursor: 'pointer'
              }}
            >
              他のプランを見る
            </button>
          </div>
        </div>
      )}

      {/* 成果事例 */}
      <div style={{
        background: 'rgba(255, 255, 255, 0.02)',
        padding: '4rem 2rem',
        textAlign: 'center'
      }}>
        <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
          <h3 style={{
            fontSize: '2rem',
            fontWeight: '700',
            marginBottom: '3rem',
            background: 'linear-gradient(135deg, #4ecdc4, #45b7d1)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            color: 'transparent'
          }}>
            📊 継続サービス成果事例
          </h3>

          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
            gap: '2rem'
          }}>
            <div style={{
              background: 'rgba(255, 255, 255, 0.05)',
              padding: '2rem',
              borderRadius: '1rem'
            }}>
              <h4 style={{ fontSize: '1.2rem', marginBottom: '1rem', color: '#ffffff' }}>
                A社（IT企業）
              </h4>
              <div style={{ fontSize: '0.9rem', color: '#d1d1d1', lineHeight: '1.6' }}>
                <p><strong>期間:</strong> 6ヶ月継続</p>
                <p><strong>結果:</strong> CVR 2.1% → 3.8%（+81%向上）</p>
                <p><strong>売上:</strong> 月額売上150%向上</p>
                <p><strong>ROI:</strong> 投資の8.5倍回収</p>
              </div>
            </div>

            <div style={{
              background: 'rgba(255, 255, 255, 0.05)',
              padding: '2rem',
              borderRadius: '1rem'
            }}>
              <h4 style={{ fontSize: '1.2rem', marginBottom: '1rem', color: '#ffffff' }}>
                B社（EC事業）
              </h4>
              <div style={{ fontSize: '0.9rem', color: '#d1d1d1', lineHeight: '1.6' }}>
                <p><strong>期間:</strong> 12ヶ月継続</p>
                <p><strong>結果:</strong> CVR 1.8% → 4.2%（+133%向上）</p>
                <p><strong>売上:</strong> 年商300%増加</p>
                <p><strong>ROI:</strong> 投資の12倍回収</p>
              </div>
            </div>

            <div style={{
              background: 'rgba(255, 255, 255, 0.05)',
              padding: '2rem',
              borderRadius: '1rem'
            }}>
              <h4 style={{ fontSize: '1.2rem', marginBottom: '1rem', color: '#ffffff' }}>
                C社（教育）
              </h4>
              <div style={{ fontSize: '0.9rem', color: '#d1d1d1', lineHeight: '1.6' }}>
                <p><strong>期間:</strong> 9ヶ月継続</p>
                <p><strong>結果:</strong> CVR 3.2% → 6.1%（+91%向上）</p>
                <p><strong>売上:</strong> 受講申込200%増加</p>
                <p><strong>ROI:</strong> 投資の15倍回収</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 継続サービスの価値 */}
      <div style={{
        background: 'linear-gradient(135deg, #1a1a2e, #16213e)',
        padding: '4rem 2rem',
        textAlign: 'center'
      }}>
        <div style={{ maxWidth: '800px', margin: '0 auto' }}>
          <h3 style={{
            fontSize: '2.5rem',
            fontWeight: '900',
            marginBottom: '2rem',
            background: 'linear-gradient(135deg, #ff6b6b, #4ecdc4)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            color: 'transparent'
          }}>
            なぜ継続サービスが重要なのか？
          </h3>

          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
            gap: '2rem',
            marginBottom: '3rem'
          }}>
            <div style={{
              background: 'rgba(255, 107, 107, 0.1)',
              padding: '2rem',
              borderRadius: '1rem'
            }}>
              <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>📈</div>
              <h4 style={{ marginBottom: '1rem', color: '#ff6b6b' }}>継続的成長</h4>
              <p style={{ color: '#d1d1d1', fontSize: '0.9rem' }}>
                1回の制作では限界があります。継続的な分析・改善で、真の成果を実現。
              </p>
            </div>

            <div style={{
              background: 'rgba(78, 205, 196, 0.1)',
              padding: '2rem',
              borderRadius: '1rem'
            }}>
              <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>🎯</div>
              <h4 style={{ marginBottom: '1rem', color: '#4ecdc4' }}>データドリブン</h4>
              <p style={{ color: '#d1d1d1', fontSize: '0.9rem' }}>
                実際のユーザー行動データに基づく改善で、確実な成果向上を実現。
              </p>
            </div>

            <div style={{
              background: 'rgba(69, 183, 209, 0.1)',
              padding: '2rem',
              borderRadius: '1rem'
            }}>
              <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>🚀</div>
              <h4 style={{ marginBottom: '1rem', color: '#45b7d1' }}>競合優位性</h4>
              <p style={{ color: '#d1d1d1', fontSize: '0.9rem' }}>
                市場・競合の変化に対応し、常に最適化された状態を維持。
              </p>
            </div>
          </div>

          <a
            href="mailto:lp-pro@example.com?subject=継続サービスのご相談&body=継続最適化サービスについてご相談があります。%0D%0A%0D%0A【会社名】%0D%0A【現在のLP】%0D%0A【改善したい指標】%0D%0A【月間予算】%0D%0A%0D%0A最適なプランをご提案させていただきます。"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '1rem',
              padding: '2rem 4rem',
              background: 'linear-gradient(135deg, #4ecdc4, #45b7d1)',
              color: '#000000',
              textDecoration: 'none',
              borderRadius: '1rem',
              fontWeight: '900',
              fontSize: '1.3rem',
              boxShadow: '0 10px 40px rgba(78, 205, 196, 0.4)',
              transition: 'all 0.3s'
            }}
          >
            💬 継続サービスを相談する
          </a>
        </div>
      </div>
    </div>
  )
}