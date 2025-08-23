'use client'

import React, { useState, useEffect } from 'react'

interface ProjectItem {
  clientId: string
  companyName: string
  contactName: string
  email: string
  industry: string
  submittedAt: string
  status: 'submitted' | 'analyzing' | 'completed'
  priority: 'high' | 'medium' | 'low'
  estimatedValue: number
}

interface ClientData {
  companyName: string
  contactName: string
  email: string
  phone: string
  industry: string
  companySize: string
  productName: string
  productDescription: string
  productPrice: string
  targetAge: string
  targetGender: string
  targetOccupation: string
  monthlyGoalCV: string
  currentCVR: string
  budget: string
  deadline: string
  competitors: string
  specialRequests: string
  submittedAt: string
  clientId: string
  status: string
}

export default function AdminProjectsPage() {
  const [projects, setProjects] = useState<ProjectItem[]>([])
  const [selectedProject, setSelectedProject] = useState<ClientData | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchProjects()
  }, [])

  const fetchProjects = async () => {
    try {
      // プロジェクト一覧を模擬データで表示（実際はAPIから取得）
      const mockProjects: ProjectItem[] = [
        {
          clientId: 'client_1692345678901_abc123',
          companyName: '株式会社テック',
          contactName: '田中太郎',
          email: 'tanaka@tech.com',
          industry: 'IT・SaaS',
          submittedAt: '2024-01-15T10:30:00.000Z',
          status: 'submitted',
          priority: 'high',
          estimatedValue: 200000
        },
        {
          clientId: 'client_1692345678902_def456',
          companyName: 'ヘルスケア合同会社',
          contactName: '佐藤花子',
          email: 'sato@health.com',
          industry: 'ヘルスケア',
          submittedAt: '2024-01-14T15:45:00.000Z',
          status: 'analyzing',
          priority: 'medium',
          estimatedValue: 150000
        }
      ]
      
      setProjects(mockProjects)
      setLoading(false)
    } catch (error) {
      console.error('プロジェクト取得エラー:', error)
      setLoading(false)
    }
  }

  const viewProjectDetails = async (clientId: string) => {
    try {
      // 詳細データを模擬表示（実際はAPIから取得）
      const mockClientData: ClientData = {
        companyName: '株式会社テック',
        contactName: '田中太郎',
        email: 'tanaka@tech.com',
        phone: '03-1234-5678',
        industry: 'IT・SaaS',
        companySize: '51-200名',
        productName: 'AI マーケティングツール',
        productDescription: 'AIを活用したマーケティング自動化ツール。CVR向上・コスト削減を実現。',
        productPrice: '10-50万円',
        targetAge: '30-40代',
        targetGender: '問わず',
        targetOccupation: '経営者・マーケティング担当者',
        monthlyGoalCV: '100',
        currentCVR: '2.5',
        budget: '50-100万円',
        deadline: '2週間以内',
        competitors: 'A社：https://a-company.com（月額3万円・基本機能）\nB社：https://b-company.com（月額10万円・高機能）',
        specialRequests: 'ブランドカラー（青系）を活用したい。信頼感のあるデザインを希望。',
        submittedAt: '2024-01-15T10:30:00.000Z',
        clientId,
        status: 'submitted'
      }
      
      setSelectedProject(mockClientData)
    } catch (error) {
      console.error('詳細取得エラー:', error)
    }
  }

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'high': return '#dc2626'
      case 'medium': return '#d97706'
      case 'low': return '#65a30d'
      default: return '#6b7280'
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'submitted': return '#3b82f6'
      case 'analyzing': return '#f59e0b'
      case 'completed': return '#10b981'
      default: return '#6b7280'
    }
  }

  if (selectedProject) {
    return (
      <div style={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #111827 0%, #000000 50%, #581c87 100%)',
        color: '#ffffff',
        padding: '2rem'
      }}>
        {/* ヘッダー */}
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '2rem'
        }}>
          <div>
            <h1 style={{ fontSize: '2rem', fontWeight: '900', marginBottom: '0.5rem' }}>
              📋 プロジェクト詳細
            </h1>
            <p style={{ color: '#9ca3af' }}>クライアント: {selectedProject.companyName}</p>
          </div>
          
          <button
            onClick={() => setSelectedProject(null)}
            style={{
              padding: '0.75rem 1.5rem',
              background: 'rgba(255, 255, 255, 0.1)',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              borderRadius: '0.5rem',
              color: '#ffffff',
              cursor: 'pointer'
            }}
          >
            ← 一覧に戻る
          </button>
        </div>

        {/* 詳細データ表示 */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: '2rem'
        }}>
          {/* 基本情報 */}
          <div style={{
            background: 'rgba(0, 0, 0, 0.5)',
            backdropFilter: 'blur(24px)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: '1rem',
            padding: '2rem'
          }}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: '700', marginBottom: '1rem' }}>
              🏢 基本情報
            </h3>
            <div style={{ display: 'grid', gap: '0.75rem', fontSize: '0.875rem' }}>
              <div><strong>会社名:</strong> {selectedProject.companyName}</div>
              <div><strong>担当者:</strong> {selectedProject.contactName}</div>
              <div><strong>メール:</strong> {selectedProject.email}</div>
              <div><strong>電話:</strong> {selectedProject.phone}</div>
              <div><strong>業界:</strong> {selectedProject.industry}</div>
              <div><strong>従業員数:</strong> {selectedProject.companySize}</div>
            </div>
          </div>

          {/* 商品情報 */}
          <div style={{
            background: 'rgba(0, 0, 0, 0.5)',
            backdropFilter: 'blur(24px)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: '1rem',
            padding: '2rem'
          }}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: '700', marginBottom: '1rem' }}>
              📦 商品・サービス
            </h3>
            <div style={{ display: 'grid', gap: '0.75rem', fontSize: '0.875rem' }}>
              <div><strong>商品名:</strong> {selectedProject.productName}</div>
              <div><strong>価格帯:</strong> {selectedProject.productPrice}</div>
              <div style={{ gridColumn: 'span 2' }}>
                <strong>特徴・強み:</strong><br/>
                <div style={{ marginTop: '0.5rem', color: '#d1d5db', lineHeight: '1.5' }}>
                  {selectedProject.productDescription}
                </div>
              </div>
            </div>
          </div>

          {/* ターゲット情報 */}
          <div style={{
            background: 'rgba(0, 0, 0, 0.5)',
            backdropFilter: 'blur(24px)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: '1rem',
            padding: '2rem'
          }}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: '700', marginBottom: '1rem' }}>
              🎯 ターゲット・競合
            </h3>
            <div style={{ display: 'grid', gap: '0.75rem', fontSize: '0.875rem' }}>
              <div><strong>年齢層:</strong> {selectedProject.targetAge}</div>
              <div><strong>性別:</strong> {selectedProject.targetGender}</div>
              <div><strong>職業:</strong> {selectedProject.targetOccupation}</div>
              <div style={{ gridColumn: 'span 2' }}>
                <strong>競合他社:</strong><br/>
                <div style={{ marginTop: '0.5rem', color: '#d1d5db', lineHeight: '1.5', whiteSpace: 'pre-line' }}>
                  {selectedProject.competitors}
                </div>
              </div>
            </div>
          </div>

          {/* 目標・予算 */}
          <div style={{
            background: 'rgba(0, 0, 0, 0.5)',
            backdropFilter: 'blur(24px)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: '1rem',
            padding: '2rem'
          }}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: '700', marginBottom: '1rem' }}>
              💰 目標・予算
            </h3>
            <div style={{ display: 'grid', gap: '0.75rem', fontSize: '0.875rem' }}>
              <div><strong>月間目標CV:</strong> {selectedProject.monthlyGoalCV}件</div>
              <div><strong>現在CVR:</strong> {selectedProject.currentCVR}%</div>
              <div><strong>予算:</strong> {selectedProject.budget}</div>
              <div><strong>納期:</strong> {selectedProject.deadline}</div>
              {selectedProject.specialRequests && (
                <div style={{ gridColumn: 'span 2' }}>
                  <strong>特別要望:</strong><br/>
                  <div style={{ marginTop: '0.5rem', color: '#d1d5db', lineHeight: '1.5' }}>
                    {selectedProject.specialRequests}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* アクションボタン */}
        <div style={{
          marginTop: '3rem',
          display: 'flex',
          gap: '1rem',
          justifyContent: 'center'
        }}>
          <button
            onClick={() => window.open(`/lp-wizard/${selectedProject.clientId}`, '_blank')}
            style={{
              padding: '1rem 2rem',
              background: 'linear-gradient(135deg, #facc15, #f97316)',
              border: 'none',
              borderRadius: '0.5rem',
              color: '#000000',
              fontWeight: '900',
              fontSize: '1.125rem',
              cursor: 'pointer',
              transition: 'all 0.3s'
            }}
          >
            🚀 LP制作ウィザード開始
          </button>
          
          <button
            onClick={() => window.open(`/image-manager/${selectedProject.clientId}`, '_blank')}
            style={{
              padding: '1rem 2rem',
              background: 'rgba(59, 130, 246, 0.8)',
              border: 'none',
              borderRadius: '0.5rem',
              color: '#ffffff',
              fontWeight: '700',
              cursor: 'pointer',
              transition: 'all 0.3s'
            }}
          >
            🖼️ 画像管理システム
          </button>
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
            📊 プロジェクト管理システム
          </span>
        </h1>
        <p style={{ textAlign: 'center', color: '#9ca3af', fontSize: '1.125rem' }}>
          依頼者データ・LP制作進捗を一括管理
        </p>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '4rem' }}>
          <div style={{
            width: '3rem',
            height: '3rem',
            border: '4px solid rgba(255, 255, 255, 0.1)',
            borderTop: '4px solid #facc15',
            borderRadius: '50%',
            animation: 'spin 1s linear infinite',
            margin: '0 auto 1rem'
          }}></div>
          <p>プロジェクトを読み込み中...</p>
        </div>
      ) : (
        <div style={{
          display: 'grid',
          gap: '1.5rem',
          maxWidth: '1200px',
          margin: '0 auto'
        }}>
          {projects.map((project) => (
            <div
              key={project.clientId}
              style={{
                background: 'rgba(0, 0, 0, 0.5)',
                backdropFilter: 'blur(24px)',
                border: '1px solid rgba(255, 255, 255, 0.1)',
                borderRadius: '1rem',
                padding: '2rem',
                cursor: 'pointer',
                transition: 'all 0.3s ease'
              }}
              onClick={() => viewProjectDetails(project.clientId)}
            >
              <div style={{
                display: 'grid',
                gridTemplateColumns: '1fr auto auto auto',
                gap: '1rem',
                alignItems: 'center'
              }}>
                {/* 基本情報 */}
                <div>
                  <h3 style={{ fontSize: '1.25rem', fontWeight: '700', marginBottom: '0.5rem' }}>
                    {project.companyName}
                  </h3>
                  <p style={{ color: '#9ca3af', fontSize: '0.875rem' }}>
                    {project.contactName} | {project.industry}
                  </p>
                  <p style={{ color: '#9ca3af', fontSize: '0.75rem' }}>
                    {new Date(project.submittedAt).toLocaleDateString('ja-JP')} 提出
                  </p>
                </div>

                {/* 優先度 */}
                <div style={{
                  padding: '0.5rem 1rem',
                  borderRadius: '9999px',
                  fontSize: '0.75rem',
                  fontWeight: '700',
                  textAlign: 'center',
                  background: getPriorityColor(project.priority),
                  color: '#ffffff'
                }}>
                  {project.priority === 'high' ? '🔥 高' : 
                   project.priority === 'medium' ? '⚡ 中' : '📝 低'}
                </div>

                {/* ステータス */}
                <div style={{
                  padding: '0.5rem 1rem',
                  borderRadius: '9999px',
                  fontSize: '0.75rem',
                  fontWeight: '700',
                  textAlign: 'center',
                  background: getStatusColor(project.status),
                  color: '#ffffff'
                }}>
                  {project.status === 'submitted' ? '📥 受付済' :
                   project.status === 'analyzing' ? '🤖 AI分析中' : '✅ 完成'}
                </div>

                {/* 推定価値 */}
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '1.125rem', fontWeight: '700', color: '#facc15' }}>
                    ¥{project.estimatedValue.toLocaleString()}
                  </div>
                  <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>
                    推定価値
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* 新規プロジェクト作成ボタン */}
      <div style={{ textAlign: 'center', marginTop: '3rem' }}>
        <a
          href="/client-request"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.5rem',
            padding: '1rem 2rem',
            background: 'linear-gradient(135deg, #facc15, #f97316)',
            border: 'none',
            borderRadius: '0.5rem',
            color: '#000000',
            fontWeight: '700',
            textDecoration: 'none',
            transition: 'all 0.3s'
          }}
        >
          ➕ 新規プロジェクト作成
        </a>
      </div>

      <style dangerouslySetInnerHTML={{
        __html: `
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
          
          @media (max-width: 768px) {
            .grid-4-cols {
              grid-template-columns: 1fr !important;
            }
            .grid-2-cols {
              grid-template-columns: 1fr !important;
            }
          }
        `
      }} />
    </div>
  )
}