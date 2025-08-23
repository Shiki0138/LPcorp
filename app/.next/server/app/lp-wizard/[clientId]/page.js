(()=>{var e={};e.id=615,e.ids=[615],e.modules={2934:e=>{"use strict";e.exports=require("next/dist/client/components/action-async-storage.external.js")},4580:e=>{"use strict";e.exports=require("next/dist/client/components/request-async-storage.external.js")},5869:e=>{"use strict";e.exports=require("next/dist/client/components/static-generation-async-storage.external.js")},399:e=>{"use strict";e.exports=require("next/dist/compiled/next-server/app-page.runtime.prod.js")},489:(e,r,t)=>{"use strict";t.r(r),t.d(r,{GlobalError:()=>o.a,__next_app__:()=>g,originalPathname:()=>m,pages:()=>c,routeModule:()=>h,tree:()=>a}),t(9933),t(2029),t(5866);var i=t(3191),n=t(8716),s=t(7922),o=t.n(s),d=t(5231),l={};for(let e in d)0>["default","tree","pages","GlobalError","originalPathname","__next_app__","routeModule"].indexOf(e)&&(l[e]=()=>d[e]);t.d(r,l);let a=["",{children:["lp-wizard",{children:["[clientId]",{children:["__PAGE__",{},{page:[()=>Promise.resolve().then(t.bind(t,9933)),"/Users/MBP/Desktop/system/034_LP制作/app/src/app/lp-wizard/[clientId]/page.tsx"]}]},{}]},{}]},{layout:[()=>Promise.resolve().then(t.bind(t,2029)),"/Users/MBP/Desktop/system/034_LP制作/app/src/app/layout.tsx"],"not-found":[()=>Promise.resolve().then(t.t.bind(t,5866,23)),"next/dist/client/components/not-found-error"]}],c=["/Users/MBP/Desktop/system/034_LP制作/app/src/app/lp-wizard/[clientId]/page.tsx"],m="/lp-wizard/[clientId]/page",g={require:t,loadChunk:()=>Promise.resolve()},h=new i.AppPageRouteModule({definition:{kind:n.x.APP_PAGE,page:"/lp-wizard/[clientId]/page",pathname:"/lp-wizard/[clientId]",bundlePath:"",filename:"",appPaths:[]},userland:{loaderTree:a}})},8541:(e,r,t)=>{Promise.resolve().then(t.t.bind(t,2994,23)),Promise.resolve().then(t.t.bind(t,6114,23)),Promise.resolve().then(t.t.bind(t,9727,23)),Promise.resolve().then(t.t.bind(t,9671,23)),Promise.resolve().then(t.t.bind(t,1868,23)),Promise.resolve().then(t.t.bind(t,4759,23))},3798:()=>{},7340:(e,r,t)=>{Promise.resolve().then(t.bind(t,5269))},5269:(e,r,t)=>{"use strict";t.r(r),t.d(r,{default:()=>s});var i=t(326),n=t(7577);function s({params:e}){let[r,t]=(0,n.useState)(1),[s,o]=(0,n.useState)(null),[d,l]=(0,n.useState)([]),[a,c]=(0,n.useState)([]),[m,g]=(0,n.useState)(""),[h,p]=(0,n.useState)(!1),u=async()=>{s&&(p(!0),setTimeout(()=>{g(`
        <!DOCTYPE html>
        <html>
        <head>
          <title>${s.productName} - ${s.companyName}</title>
          <style>
            body { 
              font-family: 'Arial', sans-serif; 
              margin: 0; 
              background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
              color: white;
            }
            .hero { 
              text-align: center; 
              padding: 100px 20px; 
              background: url('${d[0]?.urls.regular}') center/cover;
            }
            .hero h1 { 
              font-size: 3rem; 
              font-weight: 900; 
              margin: 0 0 1rem 0;
              text-shadow: 2px 2px 4px rgba(0,0,0,0.5);
            }
            .cta-button { 
              background: linear-gradient(45deg, #ff6b6b, #ee5a24); 
              color: white; 
              padding: 20px 40px; 
              border: none; 
              border-radius: 50px; 
              font-size: 1.2rem; 
              font-weight: bold;
              cursor: pointer;
              box-shadow: 0 10px 30px rgba(0,0,0,0.3);
            }
            .features { padding: 80px 20px; background: white; color: #333; }
            .feature-card { 
              display: inline-block; 
              width: 300px; 
              margin: 20px; 
              padding: 30px; 
              border-radius: 15px; 
              box-shadow: 0 10px 30px rgba(0,0,0,0.1);
              text-align: center;
            }
          </style>
          
          <!-- ヒートマップ・分析コード自動埋込 -->
          <script>
            // Google Analytics 4
            gtag('config', 'GA_MEASUREMENT_ID');
            
            // カスタムヒートマップ
            window.lpAnalytics = {
              clientId: '${s.clientId}',
              trackClicks: true,
              trackScrolls: true,
              trackTime: true
            };
          </script>
        </head>
        <body>
          <div class="hero">
            <h1>${s.productName}</h1>
            <p>${s.targetOccupation}の課題を革新的に解決</p>
            <button class="cta-button" onclick="gtag('event', 'conversion', {'send_to': 'AW-CONVERSION_ID'})">
              今すぐ無料で始める
            </button>
          </div>
          
          <div class="features">
            <h2>なぜ${s.companyName}が選ばれるのか</h2>
            <div class="feature-card">
              <img src="${d[1]?.urls.small}" alt="機能1" style="width:100%;height:200px;object-fit:cover;">
              <h3>革新的な技術力</h3>
              <p>業界最先端のAI技術で、従来の3倍の効率を実現</p>
            </div>
            <div class="feature-card">
              <img src="${d[2]?.urls.small}" alt="機能2" style="width:100%;height:200px;object-fit:cover;">
              <h3>実績に基づく信頼性</h3>
              <p>多数の企業での成功実績。平均ROI 300%を達成</p>
            </div>
          </div>
        </body>
        </html>
      `),p(!1)},3e3))};return(0,i.jsxs)("div",{style:{minHeight:"100vh",background:"linear-gradient(135deg, #111827 0%, #000000 50%, #581c87 100%)",color:"#ffffff",padding:"2rem"},children:[(0,i.jsxs)("div",{style:{textAlign:"center",marginBottom:"3rem"},children:[i.jsx("h1",{style:{fontSize:"2.5rem",fontWeight:"900",marginBottom:"1rem"},children:i.jsx("span",{style:{background:"linear-gradient(to right, #facc15, #f97316)",backgroundClip:"text",WebkitBackgroundClip:"text",color:"transparent"},children:"\uD83C\uDFAF LP制作ウィザード"})}),(0,i.jsxs)("p",{style:{color:"#9ca3af"},children:[s?.companyName," 様のLP制作"]}),i.jsx("div",{style:{background:"rgba(255, 255, 255, 0.1)",height:"0.5rem",borderRadius:"9999px",marginTop:"1rem",marginBottom:"0.5rem"},children:i.jsx("div",{style:{background:"linear-gradient(to right, #facc15, #f97316)",height:"100%",width:`${r/4*100}%`,borderRadius:"9999px",transition:"width 0.3s ease"}})}),(0,i.jsxs)("p",{style:{fontSize:"0.875rem",color:"#9ca3af"},children:["Step ",r," / ",4]})]}),(0,i.jsxs)("div",{style:{maxWidth:"1000px",margin:"0 auto",background:"rgba(0, 0, 0, 0.5)",backdropFilter:"blur(24px)",border:"1px solid rgba(255, 255, 255, 0.1)",borderRadius:"2rem",padding:"3rem"},children:[1===r&&s&&(0,i.jsxs)("div",{children:[i.jsx("h2",{style:{fontSize:"1.75rem",fontWeight:"700",marginBottom:"2rem",textAlign:"center"},children:"\uD83D\uDCCB 依頼者データ確認"}),(0,i.jsxs)("div",{style:{display:"grid",gridTemplateColumns:"1fr 1fr",gap:"2rem"},children:[(0,i.jsxs)("div",{children:[i.jsx("h4",{style:{fontSize:"1.125rem",fontWeight:"600",marginBottom:"1rem",color:"#facc15"},children:"基本情報"}),(0,i.jsxs)("div",{style:{fontSize:"0.875rem",lineHeight:"1.6"},children:[(0,i.jsxs)("p",{children:[i.jsx("strong",{children:"会社:"})," ",s.companyName]}),(0,i.jsxs)("p",{children:[i.jsx("strong",{children:"担当者:"})," ",s.contactName]}),(0,i.jsxs)("p",{children:[i.jsx("strong",{children:"業界:"})," ",s.industry]}),(0,i.jsxs)("p",{children:[i.jsx("strong",{children:"商品:"})," ",s.productName]})]})]}),(0,i.jsxs)("div",{children:[i.jsx("h4",{style:{fontSize:"1.125rem",fontWeight:"600",marginBottom:"1rem",color:"#facc15"},children:"目標・予算"}),(0,i.jsxs)("div",{style:{fontSize:"0.875rem",lineHeight:"1.6"},children:[(0,i.jsxs)("p",{children:[i.jsx("strong",{children:"目標CV:"})," ",s.monthlyGoalCV,"件/月"]}),(0,i.jsxs)("p",{children:[i.jsx("strong",{children:"予算:"})," ",s.budget]}),(0,i.jsxs)("p",{children:[i.jsx("strong",{children:"ターゲット:"})," ",s.targetOccupation]})]})]})]}),(0,i.jsxs)("div",{style:{marginTop:"2rem",padding:"1.5rem",background:"rgba(34, 197, 94, 0.1)",border:"1px solid rgba(34, 197, 94, 0.3)",borderRadius:"1rem"},children:[i.jsx("h4",{style:{color:"#22c55e",marginBottom:"0.5rem"},children:"✅ AI分析完了"}),i.jsx("p",{style:{fontSize:"0.875rem",color:"#d1d5db"},children:"最適なLPパターンを選定しました。次のステップで画像を選択してください。"})]})]}),2===r&&(0,i.jsxs)("div",{children:[i.jsx("h2",{style:{fontSize:"1.75rem",fontWeight:"700",marginBottom:"2rem",textAlign:"center"},children:"\uD83D\uDDBC️ AI推奨画像選択"}),i.jsx("p",{style:{textAlign:"center",color:"#9ca3af",marginBottom:"2rem"},children:"Unsplash APIから自動選定された最適画像。クリックして選択してください。"}),i.jsx("div",{style:{display:"grid",gridTemplateColumns:"repeat(auto-fit, minmax(250px, 1fr))",gap:"1rem"},children:a.map(e=>(0,i.jsxs)("div",{onClick:()=>{d.find(r=>r.id===e.id)?l(d.filter(r=>r.id!==e.id)):l([...d,e])},style:{border:d.find(r=>r.id===e.id)?"3px solid #facc15":"1px solid rgba(255, 255, 255, 0.2)",borderRadius:"1rem",overflow:"hidden",cursor:"pointer",transition:"all 0.3s ease",background:"rgba(0, 0, 0, 0.3)"},children:[i.jsx("img",{src:e.urls.small,alt:e.alt_description,style:{width:"100%",height:"200px",objectFit:"cover"}}),(0,i.jsxs)("div",{style:{padding:"1rem"},children:[(0,i.jsxs)("h4",{style:{fontSize:"0.875rem",fontWeight:"600",marginBottom:"0.5rem"},children:[e.category?.toUpperCase()," 画像"]}),i.jsx("p",{style:{fontSize:"0.75rem",color:"#9ca3af"},children:e.usage}),d.find(r=>r.id===e.id)&&i.jsx("div",{style:{marginTop:"0.5rem",padding:"0.25rem 0.75rem",background:"#facc15",color:"#000000",borderRadius:"9999px",fontSize:"0.75rem",fontWeight:"700",textAlign:"center"},children:"✅ 選択済み"})]})]},e.id))}),i.jsx("div",{style:{marginTop:"2rem",textAlign:"center",padding:"1rem",background:"rgba(59, 130, 246, 0.1)",border:"1px solid rgba(59, 130, 246, 0.3)",borderRadius:"1rem"},children:(0,i.jsxs)("p",{style:{color:"#60a5fa",fontSize:"0.875rem"},children:["\uD83D\uDCA1 選択済み: ",d.length,"枚 | 推奨: 3-5枚"]})})]}),3===r&&(0,i.jsxs)("div",{children:[i.jsx("h2",{style:{fontSize:"1.75rem",fontWeight:"700",marginBottom:"2rem",textAlign:"center"},children:"\uD83D\uDE80 AI LP自動生成"}),!h&&!m&&(0,i.jsxs)("div",{style:{textAlign:"center"},children:[(0,i.jsxs)("div",{style:{marginBottom:"2rem",padding:"2rem",background:"rgba(34, 197, 94, 0.1)",border:"1px solid rgba(34, 197, 94, 0.3)",borderRadius:"1rem"},children:[i.jsx("h3",{style:{color:"#22c55e",marginBottom:"1rem"},children:"✅ 準備完了"}),(0,i.jsxs)("div",{style:{fontSize:"0.875rem",color:"#d1d5db",lineHeight:"1.6"},children:[i.jsx("p",{children:"\uD83D\uDCCA 依頼者データ: 解析済み"}),(0,i.jsxs)("p",{children:["\uD83D\uDDBC️ 画像素材: ",d.length,"枚選択済み"]}),i.jsx("p",{children:"\uD83E\uDD16 AI分析: 最適パターン選定完了"}),i.jsx("p",{children:"⚡ 生成時間: 約30秒"})]})]}),i.jsx("button",{onClick:u,style:{padding:"1.5rem 3rem",background:"linear-gradient(135deg, #facc15, #f97316)",border:"none",borderRadius:"1rem",color:"#000000",fontWeight:"900",fontSize:"1.25rem",cursor:"pointer",transition:"all 0.3s",boxShadow:"0 0 30px rgba(251, 191, 36, 0.5)"},children:"\uD83D\uDE80 史上最高LP生成開始！"})]}),h&&(0,i.jsxs)("div",{style:{textAlign:"center"},children:[i.jsx("div",{style:{width:"4rem",height:"4rem",border:"4px solid rgba(255, 255, 255, 0.1)",borderTop:"4px solid #facc15",borderRadius:"50%",animation:"spin 1s linear infinite",margin:"0 auto 2rem"}}),i.jsx("h3",{style:{fontSize:"1.5rem",fontWeight:"700",marginBottom:"1rem"},children:"\uD83E\uDD16 AI LP生成中..."}),i.jsx("p",{style:{color:"#9ca3af",marginBottom:"2rem"},children:"Google級AI技術で最適なLPを生成しています"}),(0,i.jsxs)("div",{style:{fontSize:"0.875rem",color:"#d1d5db"},children:[i.jsx("p",{children:"\uD83E\uDDE0 要件分析・パターン選定..."}),i.jsx("p",{children:"\uD83C\uDFA8 デザイン・レイアウト生成..."}),i.jsx("p",{children:"\uD83D\uDCDD コピー・文章作成..."}),i.jsx("p",{children:"\uD83D\uDCCA 分析コード埋込..."})]})]}),m&&(0,i.jsxs)("div",{children:[i.jsx("h3",{style:{fontSize:"1.5rem",fontWeight:"700",marginBottom:"1rem",textAlign:"center"},children:"✅ LP生成完了！"}),(0,i.jsxs)("div",{style:{background:"rgba(255, 255, 255, 0.05)",border:"1px solid rgba(255, 255, 255, 0.1)",borderRadius:"1rem",padding:"1rem",marginBottom:"2rem"},children:[i.jsx("h4",{style:{marginBottom:"1rem"},children:"\uD83D\uDCC4 生成されたLP（プレビュー）"}),i.jsx("iframe",{srcDoc:m,style:{width:"100%",height:"400px",border:"none",borderRadius:"0.5rem"}})]}),(0,i.jsxs)("div",{style:{textAlign:"center",display:"flex",gap:"1rem",justifyContent:"center"},children:[i.jsx("button",{style:{padding:"1rem 2rem",background:"linear-gradient(135deg, #22c55e, #16a34a)",border:"none",borderRadius:"0.5rem",color:"#ffffff",fontWeight:"700",cursor:"pointer"},children:"✅ 公開・運用開始"}),i.jsx("button",{onClick:()=>t(r+1),style:{padding:"1rem 2rem",background:"rgba(59, 130, 246, 0.8)",border:"none",borderRadius:"0.5rem",color:"#ffffff",fontWeight:"700",cursor:"pointer"},children:"\uD83D\uDCCA 分析設定へ"})]})]})]}),4===r&&(0,i.jsxs)("div",{children:[i.jsx("h2",{style:{fontSize:"1.75rem",fontWeight:"700",marginBottom:"2rem",textAlign:"center"},children:"\uD83D\uDCCA 分析・レポート設定"}),(0,i.jsxs)("div",{style:{display:"grid",gap:"2rem"},children:[(0,i.jsxs)("div",{style:{padding:"1.5rem",background:"rgba(34, 197, 94, 0.1)",border:"1px solid rgba(34, 197, 94, 0.3)",borderRadius:"1rem"},children:[i.jsx("h3",{style:{color:"#22c55e",marginBottom:"1rem"},children:"✅ 自動埋込完了"}),(0,i.jsxs)("div",{style:{fontSize:"0.875rem",color:"#d1d5db"},children:[i.jsx("p",{children:"\uD83D\uDDB1️ ヒートマップ機能: 埋込済み"}),i.jsx("p",{children:"\uD83D\uDCCA Google Analytics: 設定済み"}),i.jsx("p",{children:"\uD83D\uDCC8 CVR追跡: 有効化済み"}),i.jsx("p",{children:"\uD83D\uDCE7 レポート配信: 毎日9:00AM"})]})]}),(0,i.jsxs)("div",{style:{padding:"1.5rem",background:"rgba(59, 130, 246, 0.1)",border:"1px solid rgba(59, 130, 246, 0.3)",borderRadius:"1rem"},children:[i.jsx("h3",{style:{color:"#3b82f6",marginBottom:"1rem"},children:"\uD83D\uDCE7 レポート配信設定"}),(0,i.jsxs)("div",{style:{fontSize:"0.875rem",color:"#d1d5db"},children:[(0,i.jsxs)("p",{children:["\uD83D\uDCC5 日次レポート: ",s?.email]}),i.jsx("p",{children:"\uD83D\uDCCA 週次詳細分析: 毎週月曜日"}),i.jsx("p",{children:"\uD83D\uDCC8 月次戦略提案: 毎月1日"}),i.jsx("p",{children:"\uD83D\uDEA8 緊急アラート: リアルタイム"})]})]})]}),(0,i.jsxs)("div",{style:{textAlign:"center",marginTop:"3rem"},children:[(0,i.jsxs)("div",{style:{padding:"2rem",background:"rgba(251, 191, 36, 0.1)",border:"1px solid rgba(251, 191, 36, 0.3)",borderRadius:"1rem",marginBottom:"2rem"},children:[i.jsx("h3",{style:{color:"#facc15",fontSize:"1.5rem",fontWeight:"900",marginBottom:"1rem"},children:"\uD83C\uDF89 LP制作完了！"}),i.jsx("p",{style:{color:"#d1d5db",marginBottom:"1rem"},children:"史上最高クラスのLPが完成しました。分析・改善が自動開始されます。"}),(0,i.jsxs)("div",{style:{fontSize:"0.875rem",color:"#9ca3af"},children:[i.jsx("p",{children:"⏱️ 制作時間: 30秒（従来の99.9%短縮）"}),i.jsx("p",{children:"\uD83D\uDCB0 制作コスト: \xa50（従来比100%削減）"}),i.jsx("p",{children:"\uD83D\uDCCA 予想CVR向上: 200-400%"})]})]}),i.jsx("button",{onClick:()=>{alert("\uD83C\uDF89 LP公開完了！\n\n\uD83D\uDCCA 分析開始\n\uD83D\uDCE7 レポート配信開始\n\uD83D\uDE80 運用開始"),window.location.href="/admin/projects"},style:{padding:"1.5rem 3rem",background:"linear-gradient(135deg, #22c55e, #16a34a)",border:"none",borderRadius:"1rem",color:"#ffffff",fontWeight:"900",fontSize:"1.25rem",cursor:"pointer",boxShadow:"0 0 30px rgba(34, 197, 94, 0.5)"},children:"\uD83C\uDF89 完成・公開開始！"})]})]}),(0,i.jsxs)("div",{style:{marginTop:"3rem",display:"flex",justifyContent:"space-between"},children:[r>1&&i.jsx("button",{onClick:()=>t(r-1),style:{padding:"0.75rem 1.5rem",background:"rgba(255, 255, 255, 0.1)",border:"1px solid rgba(255, 255, 255, 0.2)",borderRadius:"0.5rem",color:"#ffffff",cursor:"pointer"},children:"← 前へ"}),r<4&&3!==r&&i.jsx("button",{onClick:()=>t(r+1),disabled:2===r&&0===d.length,style:{padding:"0.75rem 2rem",background:d.length>0||2!==r?"linear-gradient(135deg, #facc15, #f97316)":"rgba(255, 255, 255, 0.2)",border:"none",borderRadius:"0.5rem",color:d.length>0||2!==r?"#000000":"#9ca3af",fontWeight:"700",cursor:d.length>0||2!==r?"pointer":"not-allowed"},children:"次へ →"})]})]}),i.jsx("style",{dangerouslySetInnerHTML:{__html:`
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
        `}})]})}},2029:(e,r,t)=>{"use strict";t.r(r),t.d(r,{default:()=>l,generateViewport:()=>d,metadata:()=>o});var i=t(9510),n=t(5317),s=t.n(n);t(5023);let o={title:"GROWTH AI - 史上最高クラスLP制作システム",description:"Google級AI技術でプロ級LPを30秒で生成。ROI 40,000%、96%コストダウンを実現。年間数億円企業の手法を月額5万円で。",keywords:["LP制作","AI","自動生成","マーケティング","Google級AI","ROI"],authors:[{name:"GROWTH AI Team"}],robots:"index, follow",openGraph:{title:"GROWTH AI - 史上最高クラスLP制作システム",description:"Google級AI技術でプロ級LPを30秒で生成。ROI 40,000%を実現。",type:"website",locale:"ja_JP"},twitter:{card:"summary_large_image",title:"GROWTH AI - 史上最高クラスLP制作システム",description:"Google級AI技術でプロ級LPを30秒で生成。ROI 40,000%を実現。"}};function d(){return{width:"device-width",initialScale:1}}function l({children:e}){return(0,i.jsxs)("html",{lang:"ja",suppressHydrationWarning:!0,children:[(0,i.jsxs)("head",{children:[i.jsx("meta",{charSet:"utf-8"}),i.jsx("link",{rel:"icon",href:"/favicon.ico"})]}),i.jsx("body",{className:s().className,suppressHydrationWarning:!0,children:i.jsx("div",{className:"min-h-screen bg-gradient-to-br from-gray-900 via-black to-purple-900 text-white",children:e})})]})}},9933:(e,r,t)=>{"use strict";t.r(r),t.d(r,{default:()=>i});let i=(0,t(8570).createProxy)(String.raw`/Users/MBP/Desktop/system/034_LP制作/app/src/app/lp-wizard/[clientId]/page.tsx#default`)},5023:()=>{}};var r=require("../../../webpack-runtime.js");r.C(e);var t=e=>r(r.s=e),i=r.X(0,[276,347],()=>t(489));module.exports=i})();