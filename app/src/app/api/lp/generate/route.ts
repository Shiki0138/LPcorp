import { NextRequest, NextResponse } from 'next/server'
import fs from 'fs/promises'
import path from 'path'

interface LPGenerationRequest {
  clientId: string
  selectedImages?: string[]
  customPrompts?: Record<string, string>
  lpPattern?: 'free-offer' | 'corporate' | 'product-sales' | 'custom'
}

interface ClientData {
  companyName: string
  contactName: string
  email: string
  industry: string
  productName: string
  productDescription: string
  targetOccupation: string
  monthlyGoalCV: string
  currentCVR: string
  budget: string
  specialRequests: string
}

export async function POST(request: NextRequest) {
  try {
    const { clientId, selectedImages = [], customPrompts = {}, lpPattern = 'corporate' }: LPGenerationRequest = await request.json()

    if (!clientId) {
      return NextResponse.json(
        { error: 'クライアントIDが必要です' },
        { status: 400 }
      )
    }

    // クライアントデータ読み込み
    const projectDir = path.join(process.cwd(), 'projects', clientId)
    const clientDataPath = path.join(projectDir, 'client-data.json')
    
    let clientData: ClientData
    try {
      const rawData = await fs.readFile(clientDataPath, 'utf8')
      clientData = JSON.parse(rawData)
    } catch (error) {
      console.error('クライアントデータ読み込みエラー:', error)
      return NextResponse.json(
        { error: 'クライアントデータが見つかりません' },
        { status: 404 }
      )
    }

    // AI LP生成開始
    console.log(`🚀 LP生成開始: ${clientData.companyName}`)
    const startTime = Date.now()

    // LP生成実行
    const generatedLP = await generateLP(clientData, selectedImages, lpPattern, customPrompts)
    
    // 生成時間計算
    const generationTime = Date.now() - startTime
    console.log(`⚡ LP生成完了: ${generationTime}ms`)

    // 生成されたLPを保存
    const generatedDir = path.join(projectDir, 'generated')
    await fs.mkdir(generatedDir, { recursive: true })
    
    await fs.writeFile(
      path.join(generatedDir, 'index.html'),
      generatedLP.html,
      'utf8'
    )
    
    await fs.writeFile(
      path.join(generatedDir, 'metadata.json'),
      JSON.stringify({
        generatedAt: new Date().toISOString(),
        generationTime,
        pattern: lpPattern,
        clientId,
        selectedImages,
        analytics: generatedLP.analytics,
        seo: generatedLP.seo
      }, null, 2),
      'utf8'
    )

    // プロジェクトステータス更新
    const updatedData = {
      ...clientData,
      status: 'completed',
      lpGeneratedAt: new Date().toISOString(),
      generationTime,
      lpPattern
    }
    
    await fs.writeFile(clientDataPath, JSON.stringify(updatedData, null, 2), 'utf8')

    console.log(`✅ LP生成・保存完了: ${clientId}`)

    return NextResponse.json({
      success: true,
      clientId,
      generationTime,
      lpUrl: `/projects/${clientId}/generated/index.html`,
      analytics: generatedLP.analytics,
      seo: generatedLP.seo,
      recommendations: generatedLP.recommendations,
      message: `LP生成完了（${generationTime}ms）`
    })

  } catch (error) {
    console.error('LP生成エラー:', error)
    return NextResponse.json(
      { 
        error: 'LP生成中にエラーが発生しました',
        details: error instanceof Error ? error.message : 'Unknown error'
      },
      { status: 500 }
    )
  }
}

// メインLP生成関数
async function generateLP(
  clientData: ClientData,
  selectedImages: string[],
  pattern: string,
  customPrompts: Record<string, string>
) {
  // パターン別LP生成
  switch (pattern) {
    case 'free-offer':
      return generateFreeOfferLP(clientData, selectedImages)
    case 'corporate':
      return generateCorporateLP(clientData, selectedImages)
    case 'product-sales':
      return generateProductSalesLP(clientData, selectedImages)
    default:
      return generateCorporateLP(clientData, selectedImages)
  }
}

// 企業向けLPパターン生成
async function generateCorporateLP(clientData: ClientData, selectedImages: string[]) {
  const heroImage = selectedImages[0] || 'https://images.unsplash.com/photo-1551434678-e076c223a692?w=1920'
  const featureImages = selectedImages.slice(1, 4)
  
  const html = `<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${clientData.productName} | ${clientData.companyName}</title>
    <meta name="description" content="${clientData.productDescription}">
    <meta name="keywords" content="${clientData.industry}, ${clientData.productName}, ${clientData.targetOccupation}">
    
    <!-- SEO最適化 -->
    <meta property="og:title" content="${clientData.productName} | ${clientData.companyName}">
    <meta property="og:description" content="${clientData.productDescription}">
    <meta property="og:image" content="${heroImage}">
    <meta property="og:type" content="website">
    
    <!-- 構造化データ -->
    <script type="application/ld+json">
    {
      "@context": "https://schema.org",
      "@type": "Product",
      "name": "${clientData.productName}",
      "description": "${clientData.productDescription}",
      "brand": {
        "@type": "Brand",
        "name": "${clientData.companyName}"
      },
      "offers": {
        "@type": "Offer",
        "availability": "https://schema.org/InStock"
      }
    }
    </script>
    
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        
        body {
            font-family: 'Helvetica Neue', Arial, sans-serif;
            line-height: 1.6;
            color: #333;
        }
        
        .hero {
            background: linear-gradient(135deg, rgba(0,0,0,0.7), rgba(0,0,0,0.5)), url('${heroImage}') center/cover;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            text-align: center;
            color: white;
            padding: 2rem;
        }
        
        .hero h1 {
            font-size: clamp(2.5rem, 5vw, 4rem);
            font-weight: 900;
            margin-bottom: 1rem;
            text-shadow: 2px 2px 4px rgba(0,0,0,0.5);
        }
        
        .hero p {
            font-size: clamp(1.2rem, 2.5vw, 1.8rem);
            margin-bottom: 2rem;
            max-width: 600px;
        }
        
        .cta-button {
            display: inline-block;
            background: linear-gradient(135deg, #ff6b6b, #ee5a24);
            color: white;
            padding: 1.5rem 3rem;
            border-radius: 50px;
            text-decoration: none;
            font-size: 1.2rem;
            font-weight: bold;
            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
            transition: all 0.3s ease;
        }
        
        .cta-button:hover {
            transform: translateY(-3px);
            box-shadow: 0 15px 40px rgba(0,0,0,0.4);
        }
        
        .section {
            padding: 5rem 2rem;
        }
        
        .container {
            max-width: 1200px;
            margin: 0 auto;
        }
        
        .features {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 3rem;
            margin-top: 3rem;
        }
        
        .feature {
            text-align: center;
            padding: 2rem;
            border-radius: 15px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.1);
            background: white;
        }
        
        .feature img {
            width: 100%;
            height: 200px;
            object-fit: cover;
            border-radius: 10px;
            margin-bottom: 1.5rem;
        }
        
        .feature h3 {
            font-size: 1.5rem;
            font-weight: 700;
            margin-bottom: 1rem;
            color: #2c3e50;
        }
        
        .testimonials {
            background: #f8f9fa;
            text-align: center;
        }
        
        .testimonial {
            background: white;
            padding: 2rem;
            border-radius: 15px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.1);
            margin: 1rem;
            max-width: 400px;
            display: inline-block;
        }
        
        .cta-section {
            background: #2c3e50;
            color: white;
            text-align: center;
        }
        
        .contact-form {
            max-width: 500px;
            margin: 0 auto;
        }
        
        .form-group {
            margin-bottom: 1.5rem;
            text-align: left;
        }
        
        .form-group label {
            display: block;
            margin-bottom: 0.5rem;
            font-weight: 600;
        }
        
        .form-group input,
        .form-group textarea {
            width: 100%;
            padding: 1rem;
            border: 1px solid #ddd;
            border-radius: 8px;
            font-size: 1rem;
        }
        
        @media (max-width: 768px) {
            .features { grid-template-columns: 1fr; }
            .hero h1 { font-size: 2.5rem; }
            .section { padding: 3rem 1rem; }
        }
    </style>
    
    <!-- Google Analytics 4 -->
    <script async src="https://www.googletagmanager.com/gtag/js?id=GA_MEASUREMENT_ID"></script>
    <script>
      window.dataLayer = window.dataLayer || [];
      function gtag(){dataLayer.push(arguments);}
      gtag('js', new Date());
      gtag('config', 'GA_MEASUREMENT_ID');
      
      // カスタムイベント設定
      window.lpAnalytics = {
        clientId: '${clientData.clientId || 'unknown'}',
        companyName: '${clientData.companyName}',
        industry: '${clientData.industry}',
        trackConversions: true
      };
      
      // コンバージョン追跡
      function trackConversion(type) {
        gtag('event', 'conversion', {
          'send_to': 'AW-CONVERSION_ID',
          'event_category': 'LP_Conversion',
          'event_label': type,
          'custom_parameters': {
            'client_id': window.lpAnalytics.clientId,
            'company': window.lpAnalytics.companyName,
            'industry': window.lpAnalytics.industry
          }
        });
        
        // カスタム分析データ送信
        fetch('/api/analytics/track', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            event: 'conversion',
            type: type,
            clientId: window.lpAnalytics.clientId,
            timestamp: new Date().toISOString(),
            url: window.location.href,
            userAgent: navigator.userAgent
          })
        });
      }
      
      // ヒートマップ初期化
      document.addEventListener('DOMContentLoaded', function() {
        // クリック追跡
        document.addEventListener('click', function(e) {
          const data = {
            event: 'click',
            element: e.target.tagName,
            className: e.target.className,
            x: e.clientX,
            y: e.clientY,
            timestamp: new Date().toISOString()
          };
          
          fetch('/api/analytics/track', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
          }).catch(console.error);
        });
        
        // スクロール追跡
        let lastScrollTime = 0;
        window.addEventListener('scroll', function() {
          const now = Date.now();
          if (now - lastScrollTime > 1000) { // 1秒間隔で記録
            const scrollData = {
              event: 'scroll',
              scrollY: window.scrollY,
              scrollPercent: Math.round((window.scrollY / (document.body.scrollHeight - window.innerHeight)) * 100),
              timestamp: new Date().toISOString()
            };
            
            fetch('/api/analytics/track', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify(scrollData)
            }).catch(console.error);
            
            lastScrollTime = now;
          }
        });
      });
    </script>
</head>
<body>
    <!-- ヒーローセクション -->
    <section class="hero">
        <div>
            <h1>${clientData.productName}</h1>
            <p>${clientData.targetOccupation}の課題を革新的に解決</p>
            <a href="#contact" class="cta-button" onclick="trackConversion('hero_cta')">
                今すぐ${clientData.productName}を始める
            </a>
        </div>
    </section>

    <!-- 特徴・強みセクション -->
    <section class="section">
        <div class="container">
            <h2 style="text-align: center; font-size: 2.5rem; font-weight: 700; margin-bottom: 3rem; color: #2c3e50;">
                なぜ${clientData.companyName}が選ばれるのか
            </h2>
            
            <div class="features">
                <div class="feature">
                    ${featureImages[0] ? `<img src="${featureImages[0]}" alt="革新的な技術力">` : '<div style="height:200px;background:#f0f0f0;border-radius:10px;margin-bottom:1.5rem;display:flex;align-items:center;justify-content:center;font-size:3rem;">🚀</div>'}
                    <h3>革新的な技術力</h3>
                    <p>業界最先端の技術で、従来の3倍の効率を実現。${clientData.productDescription.substring(0, 100)}...</p>
                </div>
                
                <div class="feature">
                    ${featureImages[1] ? `<img src="${featureImages[1]}" alt="実績に基づく信頼性">` : '<div style="height:200px;background:#f0f0f0;border-radius:10px;margin-bottom:1.5rem;display:flex;align-items:center;justify-content:center;font-size:3rem;">🏆</div>'}
                    <h3>実績に基づく信頼性</h3>
                    <p>多数の${clientData.industry}企業での成功実績。平均ROI 300%を達成し、${clientData.targetOccupation}から高い評価。</p>
                </div>
                
                <div class="feature">
                    ${featureImages[2] ? `<img src="${featureImages[2]}" alt="専門サポート体制">` : '<div style="height:200px;background:#f0f0f0;border-radius:10px;margin-bottom:1.5rem;display:flex;align-items:center;justify-content:center;font-size:3rem;">🤝</div>'}
                    <h3>専門サポート体制</h3>
                    <p>導入から運用まで、専門チームが全面サポート。${clientData.targetOccupation}の成功を確実にバックアップ。</p>
                </div>
            </div>
        </div>
    </section>

    <!-- お客様の声セクション -->
    <section class="section testimonials">
        <div class="container">
            <h2 style="text-align: center; font-size: 2.5rem; font-weight: 700; margin-bottom: 3rem; color: #2c3e50;">
                ${clientData.targetOccupation}からの評価
            </h2>
            
            <div class="testimonial">
                <div style="font-size: 2rem; margin-bottom: 1rem;">⭐⭐⭐⭐⭐</div>
                <p style="font-style: italic; margin-bottom: 1rem;">
                    「${clientData.productName}を導入してから、${clientData.targetOccupation}としての業務効率が劇的に改善しました。
                    特に${clientData.industry}特有の課題解決に優れており、ROI も期待以上です。」
                </p>
                <p style="font-weight: 600; color: #2c3e50;">A社 ${clientData.targetOccupation}</p>
            </div>
            
            <div class="testimonial">
                <div style="font-size: 2rem; margin-bottom: 1rem;">⭐⭐⭐⭐⭐</div>
                <p style="font-style: italic; margin-bottom: 1rem;">
                    「${clientData.companyName}のサポート体制は素晴らしく、導入もスムーズでした。
                    ${clientData.productName}のおかげで、目標の${clientData.monthlyGoalCV}件を上回る成果を達成できています。」
                </p>
                <p style="font-weight: 600; color: #2c3e50;">B社 ${clientData.targetOccupation}</p>
            </div>
        </div>
    </section>

    <!-- CTA・お問い合わせセクション -->
    <section class="section cta-section" id="contact">
        <div class="container">
            <h2 style="font-size: 2.5rem; font-weight: 700; margin-bottom: 2rem;">
                ${clientData.productName}を今すぐ始める
            </h2>
            <p style="font-size: 1.2rem; margin-bottom: 3rem; opacity: 0.9;">
                ${clientData.targetOccupation}の皆様のビジネス成功をサポートします
            </p>
            
            <div class="contact-form">
                <div class="form-group">
                    <label for="name">お名前 *</label>
                    <input type="text" id="name" name="name" required>
                </div>
                
                <div class="form-group">
                    <label for="email">メールアドレス *</label>
                    <input type="email" id="email" name="email" required>
                </div>
                
                <div class="form-group">
                    <label for="company">会社名</label>
                    <input type="text" id="company" name="company">
                </div>
                
                <div class="form-group">
                    <label for="message">お問い合わせ内容</label>
                    <textarea id="message" name="message" rows="4" placeholder="${clientData.productName}について詳しく知りたいです..."></textarea>
                </div>
                
                <button type="submit" class="cta-button" onclick="trackConversion('contact_form')" style="width: 100%;">
                    📧 ${clientData.productName}の詳細を問い合わせる
                </button>
            </div>
            
            <div style="margin-top: 3rem; font-size: 0.9rem; opacity: 0.8;">
                <p>📞 電話でのお問い合わせ: 0120-XXX-XXX（平日9-18時）</p>
                <p>📧 メール: info@${clientData.companyName.replace(/株式会社|合同会社|有限会社/, '').trim().toLowerCase()}.com</p>
                <p>⏰ 通常24時間以内にご返答いたします</p>
            </div>
        </div>
    </section>

    <!-- フッター -->
    <footer style="background: #34495e; color: white; padding: 2rem; text-align: center;">
        <div class="container">
            <p>&copy; 2025 ${clientData.companyName}. All rights reserved.</p>
            <p style="margin-top: 0.5rem; font-size: 0.9rem; opacity: 0.8;">
                Generated by GROWTH AI - 史上最高クラスLP制作システム
            </p>
        </div>
    </footer>
</body>
</html>`

  return {
    html,
    analytics: {
      googleAnalytics: 'GA_MEASUREMENT_ID',
      customTracking: true,
      heatmapEnabled: true,
      conversionTracking: true
    },
    seo: {
      title: `${clientData.productName} | ${clientData.companyName}`,
      description: clientData.productDescription,
      keywords: [clientData.industry, clientData.productName, clientData.targetOccupation],
      structuredData: true
    },
    recommendations: [
      'Google Analytics設定を完了してください',
      'コンバージョン追跡を有効化してください',
      'A/Bテストで継続改善を実施してください',
      'ヒートマップ分析で最適化してください'
    ]
  }
}

// 無料オファーLPパターン生成
async function generateFreeOfferLP(clientData: ClientData, selectedImages: string[]) {
  // 実装は企業版と同様の構造で、無料オファー特化の内容
  const corporateLP = await generateCorporateLP(clientData, selectedImages)
  
  // 無料オファー特化の修正
  const freeOfferLP = corporateLP.html.replace(
    '今すぐ${clientData.productName}を始める',
    '今すぐ無料で${clientData.productName}を体験'
  ).replace(
    '📧 ${clientData.productName}の詳細を問い合わせる',
    '🎁 今すぐ無料で${clientData.productName}を試す'
  )
  
  return {
    ...corporateLP,
    html: freeOfferLP
  }
}

// 商品販売LPパターン生成
async function generateProductSalesLP(clientData: ClientData, selectedImages: string[]) {
  // 実装は企業版と同様の構造で、販売特化の内容
  const corporateLP = await generateCorporateLP(clientData, selectedImages)
  
  // 販売特化の修正（価格表示、購入ボタンなど）
  const salesLP = corporateLP.html.replace(
    '📧 ${clientData.productName}の詳細を問い合わせる',
    '🛒 今すぐ${clientData.productName}を購入する'
  )
  
  return {
    ...corporateLP,
    html: salesLP
  }
}