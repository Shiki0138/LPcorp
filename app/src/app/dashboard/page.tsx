'use client'

import React, { useState, useEffect } from 'react'

interface DashboardStats {
  totalProjects: number
  activeProjects: number
  completedProjects: number
  totalRevenue: number
  averageProjectValue: number
  conversionRate: number
  clientSatisfaction: number
}

interface RecentProject {
  clientId: string
  companyName: string
  status: string
  value: number
  lastActivity: string
}

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [recentProjects, setRecentProjects] = useState<RecentProject[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadDashboardData()
  }, [])

  const loadDashboardData = async () => {
    try {
      // 模擬ダッシュボードデータ
      const mockStats: DashboardStats = {
        totalProjects: 47,
        activeProjects: 12,
        completedProjects: 35,
        totalRevenue: 12450000,
        averageProjectValue: 265000,
        conversionRate: 8.7,
        clientSatisfaction: 4.8
      }

      const mockProjects: RecentProject[] = [
        {
          clientId: 'client_001',
          companyName: '株式会社テックソリューション',
          status: 'completed',
          value: 380000,
          lastActivity: '2時間前'
        },
        {
          clientId: 'client_002', 
          companyName: 'ヘルスケア合同会社',
          status: 'in_progress',
          value: 520000,
          lastActivity: '30分前'
        },
        {
          clientId: 'client_003',
          companyName: '教育システム株式会社',
          status: 'analyzing',
          value: 180000,
          lastActivity: '1時間前'
        }
      ]

      setStats(mockStats)
      setRecentProjects(mockProjects)
      setLoading(false)
    } catch (error) {
      console.error('ダッシュボードデータ読み込みエラー:', error)
      setLoading(false)
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'completed': return '#22c55e'
      case 'in_progress': return '#f59e0b'
      case 'analyzing': return '#3b82f6'
      default: return '#6b7280'
    }
  }

  const getStatusText = (status: string) => {
    switch (status) {
      case 'completed': return '✅ 完成'
      case 'in_progress': return '🔄 制作中'
      case 'analyzing': return '🤖 AI分析中'
      default: return '📋 待機中'
    }
  }

  if (loading) {
    return (
      <div style={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #111827 0%, #000000 50%, #581c87 100%)',
        color: '#ffffff',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{
            width: '4rem',
            height: '4rem',
            border: '4px solid rgba(255, 255, 255, 0.1)',
            borderTop: '4px solid #facc15',
            borderRadius: '50%',
            animation: 'spin 1s linear infinite',
            margin: '0 auto 1rem'
          }}></div>
          <p>ダッシュボード読み込み中...</p>
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
      <div style={{ marginBottom: '3rem' }}>
        <h1 style={{
          fontSize: '2.5rem',
          fontWeight: '900',
          marginBottom: '1rem',
          textAlign: 'center'
        }}>
          <span style={{
            background: 'linear-gradient(to right, #facc15, #f97316)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            color: 'transparent'
          }}>
            📊 GROWTH AI ダッシュボード
          </span>
        </h1>
        <p style={{ textAlign: 'center', color: '#9ca3af', fontSize: '1.125rem' }}>
          史上最高クラスLP制作システムの運営状況
        </p>
      </div>

      {/* 主要指標 */}
      {stats && (
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
          gap: '1.5rem',
          marginBottom: '3rem'
        }}>
          <div style={{
            background: 'rgba(0, 0, 0, 0.5)',
            backdropFilter: 'blur(24px)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: '1rem',
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
            <div style={{ color: '#9ca3af', fontSize: '0.875rem' }}>総プロジェクト数</div>
          </div>

          <div style={{
            background: 'rgba(0, 0, 0, 0.5)',
            backdropFilter: 'blur(24px)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: '1rem',
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
            <div style={{ color: '#9ca3af', fontSize: '0.875rem' }}>進行中プロジェクト</div>
          </div>

          <div style={{
            background: 'rgba(0, 0, 0, 0.5)',
            backdropFilter: 'blur(24px)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: '1rem',
            padding: '2rem',
            textAlign: 'center'
          }}>
            <div style={{
              fontSize: '2.5rem',
              fontWeight: '900',
              background: 'linear-gradient(135deg, #facc15, #f97316)',
              backgroundClip: 'text',
              WebkitBackgroundClip: 'text',
              color: 'transparent',
              marginBottom: '0.5rem'
            }}>
              ¥{Math.round(stats.totalRevenue / 10000)}万
            </div>
            <div style={{ color: '#9ca3af', fontSize: '0.875rem' }}>総売上</div>
          </div>

          <div style={{
            background: 'rgba(0, 0, 0, 0.5)',
            backdropFilter: 'blur(24px)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: '1rem',
            padding: '2rem',
            textAlign: 'center'
          }}>
            <div style={{
              fontSize: '2.5rem',
              fontWeight: '900',
              background: 'linear-gradient(135deg, #c084fc, #a855f7)',
              backgroundClip: 'text',
              WebkitBackgroundClip: 'text',
              color: 'transparent',
              marginBottom: '0.5rem'
            }}>
              {stats.conversionRate}%
            </div>
            <div style={{ color: '#9ca3af', fontSize: '0.875rem' }}>平均CVR</div>
          </div>
        </div>
      )}

      {/* 最近のプロジェクト */}
      <div style={{
        background: 'rgba(0, 0, 0, 0.5)',
        backdropFilter: 'blur(24px)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        borderRadius: '1rem',
        padding: '2rem',
        marginBottom: '3rem'
      }}>
        <h2 style={{
          fontSize: '1.5rem',
          fontWeight: '700',
          marginBottom: '2rem'
        }}>
          📋 最近のプロジェクト
        </h2>

        <div style={{ display: 'grid', gap: '1rem' }}>
          {recentProjects.map((project) => (
            <div
              key={project.clientId}
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr auto auto auto',
                gap: '1rem',
                alignItems: 'center',
                padding: '1.5rem',
                background: 'rgba(255, 255, 255, 0.05)',
                borderRadius: '0.5rem',
                transition: 'all 0.3s ease',
                cursor: 'pointer'
              }}
              onClick={() => window.open(`/admin/projects/${project.clientId}`, '_blank')}
            >
              <div>
                <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '0.25rem' }}>
                  {project.companyName}
                </h3>
                <p style={{ color: '#9ca3af', fontSize: '0.875rem' }}>
                  最終更新: {project.lastActivity}
                </p>
              </div>

              <div style={{
                padding: '0.5rem 1rem',
                borderRadius: '9999px',
                fontSize: '0.75rem',
                fontWeight: '700',
                background: getStatusColor(project.status),
                color: '#ffffff'
              }}>
                {getStatusText(project.status)}
              </div>

              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '1rem', fontWeight: '700', color: '#facc15' }}>
                  ¥{project.value.toLocaleString()}
                </div>
                <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>
                  案件価値
                </div>
              </div>

              <div style={{ color: '#9ca3af' }}>→</div>
            </div>
          ))}
        </div>
      </div>

      {/* クイックアクション */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
        gap: '1.5rem'
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
            fontSize: '1.125rem'
          }}
        >
          ➕ 新規プロジェクト作成
        </a>

        <a
          href="/admin/projects"
          style={{
            background: 'linear-gradient(135deg, #3b82f6, #1d4ed8)',
            color: '#ffffff',
            padding: '2rem',
            borderRadius: '1rem',
            textDecoration: 'none',
            textAlign: 'center',
            transition: 'all 0.3s',
            fontWeight: '700',
            fontSize: '1.125rem'
          }}
        >
          📊 プロジェクト管理
        </a>

        <button
          onClick={() => {
            alert('🤖 AI分析レポートを生成しています...\n\n📊 システム全体の分析\n📈 改善提案\n📧 メール配信\n\n完了までお待ちください。')
          }}
          style={{
            background: 'linear-gradient(135deg, #f59e0b, #d97706)',
            color: '#ffffff',
            padding: '2rem',
            borderRadius: '1rem',
            border: 'none',
            textAlign: 'center',
            transition: 'all 0.3s',
            fontWeight: '700',
            fontSize: '1.125rem',
            cursor: 'pointer'
          }}
        >
          📧 レポート生成・送信
        </button>
      </div>

      {/* システム稼働状況 */}
      <div style={{
        marginTop: '3rem',
        background: 'rgba(0, 0, 0, 0.5)',
        backdropFilter: 'blur(24px)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        borderRadius: '1rem',
        padding: '2rem'
      }}>
        <h2 style={{
          fontSize: '1.5rem',
          fontWeight: '700',
          marginBottom: '2rem'
        }}>
          🚀 システム稼働状況
        </h2>

        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
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
            <span>🖥️ Webサーバー</span>
            <span style={{ color: '#22c55e', fontWeight: '700' }}>✅ 稼働中</span>
          </div>

          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '1rem',
            background: 'rgba(34, 197, 94, 0.1)',
            borderRadius: '0.5rem'
          }}>
            <span>🤖 AI エンジン</span>
            <span style={{ color: '#22c55e', fontWeight: '700' }}>✅ 稼働中</span>
          </div>

          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '1rem',
            background: 'rgba(34, 197, 94, 0.1)',
            borderRadius: '0.5rem'
          }}>
            <span>📊 分析システム</span>
            <span style={{ color: '#22c55e', fontWeight: '700' }}>✅ 稼働中</span>
          </div>

          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '1rem',
            background: 'rgba(34, 197, 94, 0.1)',
            borderRadius: '0.5rem'
          }}>
            <span>📧 メール配信</span>
            <span style={{ color: '#22c55e', fontWeight: '700' }}>✅ 稼働中</span>
          </div>
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