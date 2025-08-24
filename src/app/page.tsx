export default function HomePage() {
  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #111827 0%, #000000 50%, #581c87 100%)',
      color: '#ffffff',
      fontFamily: 'ui-sans-serif, system-ui, sans-serif',
      padding: '2rem'
    }}>
      <div style={{ textAlign: 'center', marginBottom: '4rem' }}>
        <h1 style={{ fontSize: '3rem', fontWeight: '900', marginBottom: '2rem' }}>
          <span style={{
            background: 'linear-gradient(135deg, #facc15, #f97316)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            color: 'transparent'
          }}>
            GROWTH AI
          </span>
        </h1>
        <p style={{ fontSize: '1.5rem', color: '#9ca3af' }}>
          史上最高クラスLP制作システム
        </p>
      </div>

      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
        gap: '2rem',
        maxWidth: '1000px',
        margin: '0 auto'
      }}>
        <a href="/client-request" style={{
          background: 'rgba(59, 130, 246, 0.2)',
          border: '1px solid rgba(59, 130, 246, 0.5)',
          borderRadius: '1rem',
          padding: '2rem',
          textAlign: 'center',
          textDecoration: 'none',
          color: '#60a5fa'
        }}>
          <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>📝</div>
          <h3 style={{ fontWeight: '700', marginBottom: '0.5rem' }}>依頼者フォーム</h3>
          <p style={{ fontSize: '0.875rem' }}>4ステップ式入力</p>
        </a>

        <a href="/admin/projects" style={{
          background: 'rgba(34, 197, 94, 0.2)',
          border: '1px solid rgba(34, 197, 94, 0.5)',
          borderRadius: '1rem',
          padding: '2rem',
          textAlign: 'center',
          textDecoration: 'none',
          color: '#22c55e'
        }}>
          <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>📊</div>
          <h3 style={{ fontWeight: '700', marginBottom: '0.5rem' }}>管理画面</h3>
          <p style={{ fontSize: '0.875rem' }}>プロジェクト管理</p>
        </a>

        <a href="/lp-wizard/demo" style={{
          background: 'rgba(168, 85, 247, 0.2)',
          border: '1px solid rgba(168, 85, 247, 0.5)',
          borderRadius: '1rem',
          padding: '2rem',
          textAlign: 'center',
          textDecoration: 'none',
          color: '#a855f7'
        }}>
          <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>🎯</div>
          <h3 style={{ fontWeight: '700', marginBottom: '0.5rem' }}>LP制作ウィザード</h3>
          <p style={{ fontSize: '0.875rem' }}>AI自動生成</p>
        </a>

        <a href="/dashboard" style={{
          background: 'rgba(251, 191, 36, 0.2)',
          border: '1px solid rgba(251, 191, 36, 0.5)',
          borderRadius: '1rem',
          padding: '2rem',
          textAlign: 'center',
          textDecoration: 'none',
          color: '#facc15'
        }}>
          <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>📈</div>
          <h3 style={{ fontWeight: '700', marginBottom: '0.5rem' }}>ダッシュボード</h3>
          <p style={{ fontSize: '0.875rem' }}>統計・監視</p>
        </a>
      </div>
    </div>
  )
}
