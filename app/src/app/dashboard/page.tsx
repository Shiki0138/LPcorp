export default function DashboardPage() {
  const stats = {
    totalProjects: 47,
    activeProjects: 12,
    completedProjects: 35,
    totalRevenue: 14650000,
    averageProjectValue: 52500,
    conversionRate: 8.7,
    clientSatisfaction: 4.9
  }

  const recentProjects = [
    {
      id: '001',
      companyName: '株式会社テックソリューション',
      status: 'completed',
      value: 98000,
      completedAt: '2時間前'
    },
    {
      id: '002',
      companyName: 'ヘルスケア合同会社',
      status: 'in_progress',
      value: 49800,
      lastUpdate: '30分前'
    },
    {
      id: '003',
      companyName: '教育システム株式会社',
      status: 'quote_sent',
      value: 98000,
      sentAt: '1時間前'
    }
  ]

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'completed': return '#22c55e'
      case 'in_progress': return '#f59e0b'
      case 'quote_sent': return '#3b82f6'
      default: return '#6b7280'
    }
  }

  const getStatusText = (status: string) => {
    switch (status) {
      case 'completed': return '✅ 完成'
      case 'in_progress': return '🔄 制作中'
      case 'quote_sent': return '📋 見積送付済み'
      default: return '📝 準備中'
    }
  }

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #0f0f23 0%, #1a1a2e 50%, #16213e 100%)',
      color: '#ffffff',
      padding: '2rem'
    }}>
      {/* ヘッダー */}
      <div style={{ marginBottom: '3rem' }}>
        <h1 style={{
          fontSize: '2.5rem',
          fontWeight: '900',
          marginBottom: '1rem',
          textAlign: 'center'
        }}>
          <span style={{
            background: 'linear-gradient(135deg, #4ecdc4, #45b7d1)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            color: 'transparent'
          }}>
            📊 事業ダッシュボード
          </span>
        </h1>
        <p style={{ textAlign: 'center', color: '#b8b8b8', fontSize: '1.1rem' }}>
          LP制作サービスの運営状況・売上実績
        </p>
      </div>

      {/* KPI統計 */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        gap: '1.5rem',
        marginBottom: '3rem',
        maxWidth: '1200px',
        margin: '0 auto 3rem'
      }}>
        <div style={{
          background: 'rgba(255, 255, 255, 0.05)',
          backdropFilter: 'blur(20px)',
          border: '1px solid rgba(255, 255, 255, 0.1)',
          borderRadius: '1.5rem',
          padding: '2rem',
          textAlign: 'center'
        }}>
          <div style={{
            fontSize: '2.5rem',
            fontWeight: '900',
            background: 'linear-gradient(135deg, #22c55e, #16a34a)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            color: 'transparent',
            marginBottom: '0.5rem'
          }}>
            {stats.totalProjects}
          </div>
          <div style={{ color: '#b8b8b8', fontSize: '0.9rem' }}>総LP制作数</div>
        </div>

        <div style={{
          background: 'rgba(255, 255, 255, 0.05)',
          backdropFilter: 'blur(20px)',
          border: '1px solid rgba(255, 255, 255, 0.1)',
          borderRadius: '1.5rem',
          padding: '2rem',
          textAlign: 'center'
        }}>
          <div style={{
            fontSize: '2.5rem',
            fontWeight: '900',
            background: 'linear-gradient(135deg, #3b82f6, #1d4ed8)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            color: 'transparent',
            marginBottom: '0.5rem'
          }}>
            {stats.activeProjects}
          </div>
          <div style={{ color: '#b8b8b8', fontSize: '0.9rem' }}>進行中案件</div>
        </div>

        <div style={{
          background: 'rgba(255, 255, 255, 0.05)',
          backdropFilter: 'blur(20px)',
          border: '1px solid rgba(255, 255, 255, 0.1)',
          borderRadius: '1.5rem',
          padding: '2rem',
          textAlign: 'center'
        }}>
          <div style={{
            fontSize: '2.5rem',
            fontWeight: '900',
            background: 'linear-gradient(135deg, #ff6b6b, #ee5a24)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            color: 'transparent',
            marginBottom: '0.5rem'
          }}>
            ¥{Math.round(stats.totalRevenue / 10000)}万
          </div>
          <div style={{ color: '#b8b8b8', fontSize: '0.9rem' }}>総売上</div>
        </div>

        <div style={{
          background: 'rgba(255, 255, 255, 0.05)',
          backdropFilter: 'blur(20px)',
          border: '1px solid rgba(255, 255, 255, 0.1)',
          borderRadius: '1.5rem',
          padding: '2rem',
          textAlign: 'center'
        }}>
          <div style={{
            fontSize: '2.5rem',
            fontWeight: '900',
            background: 'linear-gradient(135deg, #4ecdc4, #45b7d1)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            color: 'transparent',
            marginBottom: '0.5rem'
          }}>
            ¥{Math.round(stats.averageProjectValue / 1000)}万
          </div>
          <div style={{ color: '#b8b8b8', fontSize: '0.9rem' }}>平均案件単価</div>
        </div>
      </div>

      {/* 最近の案件 */}
      <div style={{
        background: 'rgba(255, 255, 255, 0.05)',
        backdropFilter: 'blur(20px)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        borderRadius: '1.5rem',
        padding: '2rem',
        maxWidth: '1000px',
        margin: '0 auto 3rem'
      }}>
        <h2 style={{
          fontSize: '1.5rem',
          fontWeight: '700',
          marginBottom: '2rem',
          color: '#4ecdc4'
        }}>
          📋 最近の案件
        </h2>

        <div style={{ display: 'grid', gap: '1rem' }}>
          {recentProjects.map((project) => (
            <div
              key={project.id}
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr auto auto auto',
                gap: '1rem',
                alignItems: 'center',
                padding: '1.5rem',
                background: 'rgba(255, 255, 255, 0.05)',
                borderRadius: '1rem',
                transition: 'all 0.3s ease'
              }}
            >
              <div>
                <h3 style={{ fontSize: '1.1rem', fontWeight: '600', marginBottom: '0.3rem' }}>
                  {project.companyName}
                </h3>
                <p style={{ color: '#b8b8b8', fontSize: '0.8rem' }}>
                  案件ID: {project.id}
                </p>
              </div>

              <div style={{
                padding: '0.5rem 1rem',
                borderRadius: '50px',
                fontSize: '0.75rem',
                fontWeight: '700',
                background: getStatusColor(project.status),
                color: '#ffffff'
              }}>
                {getStatusText(project.status)}
              </div>

              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '1rem', fontWeight: '700', color: '#4ecdc4' }}>
                  ¥{project.value.toLocaleString()}
                </div>
                <div style={{ fontSize: '0.7rem', color: '#b8b8b8' }}>
                  {project.status === 'completed' ? project.completedAt : 
                   project.status === 'in_progress' ? project.lastUpdate : 
                   project.sentAt}
                </div>
              </div>

              <div style={{ color: '#b8b8b8' }}>→</div>
            </div>
          ))}
        </div>
      </div>

      {/* クイックアクション */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
        gap: '1.5rem',
        maxWidth: '1000px',
        margin: '0 auto'
      }}>
        <a
          href="/client-request"
          style={{
            background: 'linear-gradient(135deg, #22c55e, #16a34a)',
            color: '#ffffff',
            padding: '2rem',
            borderRadius: '1rem',
            textDecoration: 'none',
            textAlign: 'center',
            transition: 'all 0.3s',
            fontWeight: '700',
            fontSize: '1.1rem'
          }}
        >
          ➕ 新規案件受付
        </a>

        <a
          href="/pricing"
          style={{
            background: 'linear-gradient(135deg, #3b82f6, #1d4ed8)',
            color: '#ffffff',
            padding: '2rem',
            borderRadius: '1rem',
            textDecoration: 'none',
            textAlign: 'center',
            transition: 'all 0.3s',
            fontWeight: '700',
            fontSize: '1.1rem'
          }}
        >
          💰 料金プラン管理
        </a>

        <a
          href="/quote"
          style={{
            background: 'linear-gradient(135deg, #4ecdc4, #45b7d1)',
            color: '#000000',
            padding: '2rem',
            borderRadius: '1rem',
            textDecoration: 'none',
            textAlign: 'center',
            transition: 'all 0.3s',
            fontWeight: '700',
            fontSize: '1.1rem'
          }}
        >
          🤖 AI見積もりシステム
        </a>

        <a
          href="/services/continuous"
          style={{
            background: 'linear-gradient(135deg, #f59e0b, #d97706)',
            color: '#ffffff',
            padding: '2rem',
            borderRadius: '1rem',
            textDecoration: 'none',
            textAlign: 'center',
            transition: 'all 0.3s',
            fontWeight: '700',
            fontSize: '1.1rem'
          }}
        >
          📈 継続サービス
        </a>
      </div>

      {/* システム稼働状況 */}
      <div style={{
        marginTop: '3rem',
        background: 'rgba(255, 255, 255, 0.05)',
        backdropFilter: 'blur(20px)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        borderRadius: '1.5rem',
        padding: '2rem',
        maxWidth: '800px',
        margin: '3rem auto 0'
      }}>
        <h2 style={{
          fontSize: '1.3rem',
          fontWeight: '700',
          marginBottom: '1.5rem',
          color: '#4ecdc4'
        }}>
          🚀 システム稼働状況
        </h2>

        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
          gap: '1rem'
        }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '1rem',
            background: 'rgba(34, 197, 94, 0.1)',
            borderRadius: '0.5rem'
          }}>
            <span style={{ fontSize: '0.9rem' }}>🖥️ Webサーバー</span>
            <span style={{ color: '#22c55e', fontWeight: '700', fontSize: '0.8rem' }}>✅ 稼働中</span>
          </div>

          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '1rem',
            background: 'rgba(34, 197, 94, 0.1)',
            borderRadius: '0.5rem'
          }}>
            <span style={{ fontSize: '0.9rem' }}>🤖 AI エンジン</span>
            <span style={{ color: '#22c55e', fontWeight: '700', fontSize: '0.8rem' }}>✅ 稼働中</span>
          </div>

          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '1rem',
            background: 'rgba(34, 197, 94, 0.1)',
            borderRadius: '0.5rem'
          }}>
            <span style={{ fontSize: '0.9rem' }}>📊 分析システム</span>
            <span style={{ color: '#22c55e', fontWeight: '700', fontSize: '0.8rem' }}>✅ 稼働中</span>
          </div>

          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '1rem',
            background: 'rgba(34, 197, 94, 0.1)',
            borderRadius: '0.5rem'
          }}>
            <span style={{ fontSize: '0.9rem' }}>📧 メール配信</span>
            <span style={{ color: '#22c55e', fontWeight: '700', fontSize: '0.8rem' }}>✅ 稼働中</span>
          </div>
        </div>

        <div style={{
          marginTop: '1.5rem',
          padding: '1rem',
          background: 'rgba(78, 205, 196, 0.1)',
          borderRadius: '0.5rem',
          textAlign: 'center'
        }}>
          <p style={{ color: '#4ecdc4', fontWeight: '600', fontSize: '0.9rem' }}>
            🎉 全システム正常稼働中 | 稼働率: 99.97% | 平均応答時間: 0.8秒
          </p>
        </div>
      </div>
    </div>
  )
}