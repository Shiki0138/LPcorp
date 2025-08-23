export default function HomePage() {
  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #111827 0%, #000000 50%, #581c87 100%)',
      color: '#ffffff',
      fontFamily: 'ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
    }}>
      {/* 固定ナビゲーション */}
      <nav style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        zIndex: 50,
        background: 'rgba(0, 0, 0, 0.8)',
        backdropFilter: 'blur(24px)',
        borderBottom: '1px solid rgba(75, 85, 99, 1)'
      }}>
        <div style={{
          maxWidth: '80rem',
          margin: '0 auto',
          padding: '1rem 1.5rem',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div style={{
              width: '2.5rem',
              height: '2.5rem',
              background: 'linear-gradient(135deg, #facc15, #f97316)',
              borderRadius: '0.75rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <span style={{ color: '#000000', fontWeight: '900' }}>G</span>
            </div>
            <div>
              <h1 style={{ fontSize: '1.25rem', fontWeight: '900', color: '#ffffff', margin: 0 }}>GROWTH AI</h1>
              <p style={{ fontSize: '0.75rem', color: '#9ca3af', margin: 0 }}>Premium Edition</p>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '2rem' }}>
            <a href="#features" style={{ 
              color: '#ffffff', 
              textDecoration: 'none',
              fontWeight: '500',
              transition: 'color 0.3s'
            }}>プレミアム機能</a>
            <a href="#pricing" style={{ 
              color: '#ffffff', 
              textDecoration: 'none',
              fontWeight: '500',
              transition: 'color 0.3s'
            }}>料金プラン</a>
            <a 
              href="#contact"
              style={{
                background: 'linear-gradient(135deg, #facc15, #f97316)',
                color: '#000000',
                fontWeight: '700',
                padding: '0.5rem 1.5rem',
                borderRadius: '9999px',
                textDecoration: 'none',
                transition: 'all 0.3s'
              }}
            >
              👑 今すぐ開始
            </a>
          </div>
        </div>
      </nav>

      {/* ヒーローセクション */}
      <section style={{
        position: 'relative',
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '1.5rem',
        paddingTop: '6rem',
        textAlign: 'center'
      }}>
        <div style={{ maxWidth: '80rem', margin: '0 auto' }}>
          {/* バッジ */}
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.5rem',
            padding: '0.75rem 1.5rem',
            borderRadius: '9999px',
            background: 'linear-gradient(to right, rgba(251, 191, 36, 0.2), rgba(249, 115, 22, 0.2))',
            border: '1px solid rgba(251, 191, 36, 0.3)',
            marginBottom: '2rem'
          }}>
            <span style={{ color: '#facc15' }}>⚡</span>
            <span style={{
              fontSize: '0.875rem',
              fontWeight: '700',
              background: 'linear-gradient(to right, #facc15, #f97316)',
              backgroundClip: 'text',
              WebkitBackgroundClip: 'text',
              color: 'transparent'
            }}>
              世界初・AI戦略パートナーシップ
            </span>
          </div>

          {/* メインタイトル */}
          <h1 style={{
            fontSize: 'clamp(3rem, 8vw, 8rem)',
            fontWeight: '900',
            lineHeight: '0.9',
            marginBottom: '2rem'
          }}>
            <span style={{
              display: 'block',
              marginBottom: '1rem',
              background: 'linear-gradient(135deg, #facc15 0%, #f97316 50%, #dc2626 100%)',
              backgroundClip: 'text',
              WebkitBackgroundClip: 'text',
              color: 'transparent'
            }}>
              Google級AI
            </span>
            <span style={{
              display: 'block',
              marginBottom: '1rem',
              background: 'linear-gradient(135deg, #c084fc 0%, #60a5fa 50%, #22d3ee 100%)',
              backgroundClip: 'text',
              WebkitBackgroundClip: 'text',
              color: 'transparent'
            }}>
              専属マーケター
            </span>
            <span style={{ display: 'block', color: '#ffffff' }}>
              月額5万円
            </span>
          </h1>

          {/* サブタイトル */}
          <p style={{
            fontSize: 'clamp(1.25rem, 3vw, 2rem)',
            color: '#9ca3af',
            marginBottom: '3rem',
            maxWidth: '800px',
            marginLeft: 'auto',
            marginRight: 'auto',
            lineHeight: '1.6'
          }}>
            年間<span style={{ color: '#facc15', fontWeight: '700' }}>数億円企業</span>が使うAI技術を月額5万円で。<br/>
            <span style={{ color: '#ffffff', fontWeight: '600' }}>96%コストダウン</span>で
            <span style={{ color: '#4ade80', fontWeight: '700' }}>ROI 40,000%</span>を実現。
          </p>

          {/* CTAボタン */}
          <div style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '1.5rem',
            alignItems: 'center',
            marginBottom: '3rem'
          }}>
            <a 
              href="#contact"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.75rem',
                padding: '2rem 4rem',
                fontSize: '1.5rem',
                fontWeight: '900',
                color: '#000000',
                background: 'linear-gradient(135deg, #facc15 0%, #f97316 100%)',
                border: 'none',
                borderRadius: '1rem',
                cursor: 'pointer',
                transition: 'all 0.3s ease',
                boxShadow: '0 0 40px rgba(251, 191, 36, 0.5)',
                textDecoration: 'none'
              }}
            >
              👑 限定価格で今すぐ開始 →
            </a>
          </div>

          {/* 信頼要素 */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: '1rem',
            maxWidth: '600px',
            margin: '0 auto',
            fontSize: '0.875rem',
            color: '#9ca3af'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span style={{ color: '#4ade80' }}>🛡️</span>
              <span>30日返金保証</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span style={{ color: '#facc15' }}>👑</span>
              <span>専属サポート</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span style={{ color: '#c084fc' }}>👥</span>
              <span>導入企業100社突破</span>
            </div>
          </div>
        </div>
      </section>

      {/* 機能セクション */}
      <section id="features" style={{ padding: '8rem 1.5rem' }}>
        <div style={{ maxWidth: '80rem', margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: '5rem' }}>
            <h2 style={{
              fontSize: 'clamp(2.5rem, 5vw, 4rem)',
              fontWeight: '700',
              marginBottom: '1rem'
            }}>
              <span style={{
                background: 'linear-gradient(to right, #c084fc, #f472b6)',
                backgroundClip: 'text',
                WebkitBackgroundClip: 'text',
                color: 'transparent'
              }}>
                革新的な機能
              </span>
            </h2>
            <p style={{ fontSize: '1.25rem', color: '#9ca3af' }}>年間5,000万円相当の価値を月額5万円で</p>
          </div>

          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
            gap: '2rem'
          }}>
            {/* 機能カード1 */}
            <div style={{
              background: 'rgba(0, 0, 0, 0.5)',
              backdropFilter: 'blur(24px)',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              borderRadius: '1.5rem',
              padding: '2rem',
              transition: 'all 0.3s ease'
            }}>
              <div style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: '4rem',
                height: '4rem',
                borderRadius: '1rem',
                marginBottom: '1.5rem',
                fontSize: '1.5rem',
                background: 'linear-gradient(135deg, #8b5cf6, #6366f1)'
              }}>
                🧠
              </div>
              <h3 style={{ fontSize: '1.5rem', fontWeight: '700', marginBottom: '1rem' }}>専任AIコンサルタント</h3>
              <p style={{ color: '#9ca3af', marginBottom: '1rem', lineHeight: '1.6' }}>
                Google級AIがあなた専属のマーケティングディレクターとして24時間稼働
              </p>
              <div style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.5rem',
                padding: '0.5rem 1rem',
                background: '#fef3c7',
                color: '#92400e',
                borderRadius: '9999px',
                fontSize: '0.875rem',
                fontWeight: '600'
              }}>
                💰 年間1,200万円相当
              </div>
            </div>

            {/* 機能カード2 */}
            <div style={{
              background: 'rgba(0, 0, 0, 0.5)',
              backdropFilter: 'blur(24px)',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              borderRadius: '1.5rem',
              padding: '2rem',
              transition: 'all 0.3s ease'
            }}>
              <div style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: '4rem',
                height: '4rem',
                borderRadius: '1rem',
                marginBottom: '1.5rem',
                fontSize: '1.5rem',
                background: 'linear-gradient(135deg, #06b6d4, #3b82f6)'
              }}>
                🎯
              </div>
              <h3 style={{ fontSize: '1.5rem', fontWeight: '700', marginBottom: '1rem' }}>競合AI分析システム</h3>
              <p style={{ color: '#9ca3af', marginBottom: '1rem', lineHeight: '1.6' }}>
                同業他社のLP・広告戦略を自動分析し、勝つための施策を毎日提案
              </p>
              <div style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.5rem',
                padding: '0.5rem 1rem',
                background: '#fef3c7',
                color: '#92400e',
                borderRadius: '9999px',
                fontSize: '0.875rem',
                fontWeight: '600'
              }}>
                💰 年間600万円相当
              </div>
            </div>

            {/* 機能カード3 */}
            <div style={{
              background: 'rgba(0, 0, 0, 0.5)',
              backdropFilter: 'blur(24px)',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              borderRadius: '1.5rem',
              padding: '2rem',
              transition: 'all 0.3s ease'
            }}>
              <div style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: '4rem',
                height: '4rem',
                borderRadius: '1rem',
                marginBottom: '1.5rem',
                fontSize: '1.5rem',
                background: 'linear-gradient(135deg, #10b981, #059669)'
              }}>
                📈
              </div>
              <h3 style={{ fontSize: '1.5rem', fontWeight: '700', marginBottom: '1rem' }}>未来予測AI</h3>
              <p style={{ color: '#9ca3af', marginBottom: '1rem', lineHeight: '1.6' }}>
                3ヶ月先の市場トレンドを予測し、先手必勝の戦略を立案
              </p>
              <div style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.5rem',
                padding: '0.5rem 1rem',
                background: '#fef3c7',
                color: '#92400e',
                borderRadius: '9999px',
                fontSize: '0.875rem',
                fontWeight: '600'
              }}>
                💰 年間800万円相当
              </div>
            </div>

            {/* 機能カード4 */}
            <div style={{
              background: 'rgba(0, 0, 0, 0.5)',
              backdropFilter: 'blur(24px)',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              borderRadius: '1.5rem',
              padding: '2rem',
              transition: 'all 0.3s ease'
            }}>
              <div style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: '4rem',
                height: '4rem',
                borderRadius: '1rem',
                marginBottom: '1.5rem',
                fontSize: '1.5rem',
                background: 'linear-gradient(135deg, #f59e0b, #f97316)'
              }}>
                👑
              </div>
              <h3 style={{ fontSize: '1.5rem', fontWeight: '700', marginBottom: '1rem' }}>エクスクルーシブ・サポート</h3>
              <p style={{ color: '#9ca3af', marginBottom: '1rem', lineHeight: '1.6' }}>
                元Google・Apple出身の専門家による専属コンサルティング
              </p>
              <div style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.5rem',
                padding: '0.5rem 1rem',
                background: '#fef3c7',
                color: '#92400e',
                borderRadius: '9999px',
                fontSize: '0.875rem',
                fontWeight: '600'
              }}>
                💰 年間2,400万円相当
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 価格セクション */}
      <section id="pricing" style={{ padding: '8rem 1.5rem', textAlign: 'center' }}>
        <div style={{ maxWidth: '56rem', margin: '0 auto' }}>
          <div style={{
            position: 'relative',
            background: 'rgba(0, 0, 0, 0.5)',
            backdropFilter: 'blur(24px)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: '2rem',
            padding: '3rem'
          }}>
            <h2 style={{
              fontSize: 'clamp(2.5rem, 5vw, 4rem)',
              fontWeight: '700',
              marginBottom: '1.5rem'
            }}>
              <span style={{
                background: 'linear-gradient(to right, #c084fc, #f472b6, #22d3ee)',
                backgroundClip: 'text',
                WebkitBackgroundClip: 'text',
                color: 'transparent'
              }}>
                今すぐ始めよう
              </span>
            </h2>
            
            <div style={{ marginBottom: '2rem' }}>
              <p style={{ fontSize: '1.5rem', color: '#9ca3af', marginBottom: '0.5rem' }}>
                通常価格 <span style={{ textDecoration: 'line-through' }}>¥49,800/月</span>
              </p>
              <p style={{ fontSize: '3rem', fontWeight: '900', color: '#facc15', marginBottom: '1rem' }}>
                創業記念価格 ¥19,800/月
              </p>
              <div style={{
                display: 'inline-flex',
                alignItems: 'center',
                padding: '0.75rem 1.5rem',
                background: '#dc2626',
                color: 'white',
                borderRadius: '9999px',
                fontWeight: '700'
              }}>
                🔥 先着100社限定・残り72時間
              </div>
            </div>

            <a 
              href="mailto:contact@growth-ai.com?subject=GROWTH AI 創業記念価格申込み"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.75rem',
                padding: '2rem 4rem',
                fontSize: '1.5rem',
                fontWeight: '900',
                color: '#000000',
                background: 'linear-gradient(135deg, #facc15 0%, #f97316 100%)',
                border: 'none',
                borderRadius: '1rem',
                cursor: 'pointer',
                transition: 'all 0.3s ease',
                boxShadow: '0 0 40px rgba(251, 191, 36, 0.5)',
                textDecoration: 'none'
              }}
            >
              👑 限定価格で今すぐ開始 →
            </a>

            <div style={{
              marginTop: '2rem',
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '1rem',
              fontSize: '1.125rem',
              fontWeight: '700'
            }}>
              <div style={{ color: '#4ade80' }}>⚡ 投資回収：平均30日</div>
              <div style={{ color: '#facc15' }}>📊 平均ROI：40,000%</div>
              <div style={{ color: '#c084fc' }}>🚀 年間価値：5,000万円相当</div>
            </div>
          </div>
        </div>
      </section>

      {/* 最終CTA */}
      <div id="contact" style={{
        background: 'linear-gradient(135deg, #1f2937, #000000)',
        padding: '4rem 1.5rem',
        textAlign: 'center'
      }}>
        <h3 style={{
          fontSize: '2.5rem',
          fontWeight: '900',
          marginBottom: '2rem',
          background: 'linear-gradient(45deg, #facc15, #f97316)',
          backgroundClip: 'text',
          WebkitBackgroundClip: 'text',
          color: 'transparent'
        }}>
          競合が追いつく前に、圧倒的優位性を確立
        </h3>
        <p style={{
          fontSize: '1.3rem',
          color: '#9ca3af',
          marginBottom: '2rem'
        }}>
          このページを閉じると、二度とこの価格では利用できません...
        </p>
        <a 
          href="mailto:contact@growth-ai.com?subject=GROWTH AI 緊急申込み"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.75rem',
            padding: '2rem 4rem',
            fontSize: '1.5rem',
            fontWeight: '900',
            color: '#000000',
            background: 'linear-gradient(135deg, #facc15 0%, #f97316 100%)',
            border: 'none',
            borderRadius: '1rem',
            cursor: 'pointer',
            transition: 'all 0.3s ease',
            boxShadow: '0 0 40px rgba(251, 191, 36, 0.5)',
            textDecoration: 'none'
          }}
        >
          🔥 最後のチャンス - 今すぐ申込む
        </a>
      </div>
    </div>
  )
}