/**
 * LP生成速度検証POC
 * 目標: 10秒以内でLP生成完了
 */

import { performance } from 'perf_hooks'
import OpenAI from 'openai'
import Redis from 'ioredis'
import { PrismaClient } from '@prisma/client'

// テスト設定
const TEST_ITERATIONS = 10
const TARGET_TIME_MS = 10000 // 10秒
const CACHE_HIT_TARGET = 0.6 // 60%のキャッシュヒット率

// クライアント初期化
const openai = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY,
})
const redis = new Redis({
  host: 'localhost',
  port: 6379,
})
const prisma = new PrismaClient()

interface TestResult {
  iteration: number
  totalTime: number
  breakdownTimes: {
    cacheCheck: number
    templateSelection: number
    contentGeneration: number
    imagePreparation: number
    integration: number
    optimization: number
    complianceCheck: number
    databaseSave: number
  }
  cacheHit: boolean
  success: boolean
  error?: string
}

class LPGenerationPOC {
  private results: TestResult[] = []
  
  async runTests(): Promise<void> {
    console.log('🚀 LP生成速度検証POC開始')
    console.log(`目標: ${TARGET_TIME_MS}ms以内`)
    console.log(`テスト回数: ${TEST_ITERATIONS}回\n`)
    
    for (let i = 1; i <= TEST_ITERATIONS; i++) {
      console.log(`\n--- テスト ${i}/${TEST_ITERATIONS} ---`)
      const result = await this.runSingleTest(i)
      this.results.push(result)
      
      if (result.success) {
        console.log(`✅ 成功: ${result.totalTime.toFixed(2)}ms`)
      } else {
        console.log(`❌ 失敗: ${result.error}`)
      }
      
      // クールダウン
      await this.sleep(1000)
    }
    
    this.printSummary()
  }
  
  private async runSingleTest(iteration: number): Promise<TestResult> {
    const result: TestResult = {
      iteration,
      totalTime: 0,
      breakdownTimes: {
        cacheCheck: 0,
        templateSelection: 0,
        contentGeneration: 0,
        imagePreparation: 0,
        integration: 0,
        optimization: 0,
        complianceCheck: 0,
        databaseSave: 0,
      },
      cacheHit: false,
      success: false,
    }
    
    const testData = this.generateTestData(iteration)
    const startTime = performance.now()
    
    try {
      // 1. キャッシュチェック
      const cacheStart = performance.now()
      const cacheKey = `poc:lp:${testData.companyName}:${testData.industry}`
      const cached = await redis.get(cacheKey)
      result.breakdownTimes.cacheCheck = performance.now() - cacheStart
      
      if (cached) {
        result.cacheHit = true
        result.totalTime = performance.now() - startTime
        result.success = true
        console.log('  📦 キャッシュヒット')
        return result
      }
      
      // 2. 並列処理実行
      const [template, content, images] = await Promise.all([
        this.selectTemplate(testData),
        this.generateContent(testData),
        this.prepareImages(testData),
      ])
      
      result.breakdownTimes.templateSelection = template.time
      result.breakdownTimes.contentGeneration = content.time
      result.breakdownTimes.imagePreparation = images.time
      
      // 3. 統合処理
      const integrationStart = performance.now()
      const integrated = await this.integrateComponents(template, content, images)
      result.breakdownTimes.integration = performance.now() - integrationStart
      
      // 4. 最適化
      const optimizationStart = performance.now()
      const optimized = await this.optimizeLP(integrated)
      result.breakdownTimes.optimization = performance.now() - optimizationStart
      
      // 5. コンプライアンスチェック
      const complianceStart = performance.now()
      const compliant = await this.checkCompliance(optimized)
      result.breakdownTimes.complianceCheck = performance.now() - complianceStart
      
      // 6. データベース保存
      const dbStart = performance.now()
      const lpId = await this.saveToDatabase(optimized)
      result.breakdownTimes.databaseSave = performance.now() - dbStart
      
      // 7. キャッシュ保存（非同期）
      redis.setex(cacheKey, 3600, JSON.stringify(optimized))
      
      result.totalTime = performance.now() - startTime
      result.success = result.totalTime <= TARGET_TIME_MS
      
    } catch (error) {
      result.error = error instanceof Error ? error.message : String(error)
      result.totalTime = performance.now() - startTime
    }
    
    return result
  }
  
  private generateTestData(iteration: number) {
    const industries = ['飲食', '小売', '美容', 'IT', '不動産']
    const purposes = ['lead', 'sales', 'recruit', 'inquiry']
    
    return {
      companyName: `テスト会社${iteration}`,
      industry: industries[iteration % industries.length],
      purpose: purposes[iteration % purposes.length],
      websiteUrl: `https://example${iteration}.com`,
    }
  }
  
  private async selectTemplate(data: any) {
    const start = performance.now()
    
    // テンプレート選択ロジック（モック）
    await this.sleep(Math.random() * 200 + 300) // 300-500ms
    
    return {
      templateId: `template_${data.industry}`,
      time: performance.now() - start,
    }
  }
  
  private async generateContent(data: any) {
    const start = performance.now()
    
    try {
      // GPT-3.5 Turbo使用（高速）
      const response = await openai.chat.completions.create({
        model: 'gpt-3.5-turbo',
        messages: [
          {
            role: 'system',
            content: 'あなたはLP制作の専門家です。簡潔で効果的なコンテンツを生成してください。',
          },
          {
            role: 'user',
            content: `
              会社名: ${data.companyName}
              業種: ${data.industry}
              目的: ${data.purpose}
              
              この情報から、効果的なLPのヘッドライン、サブヘッドライン、CTA文言を生成してください。
              JSON形式で返してください。
            `,
          },
        ],
        temperature: 0.7,
        max_tokens: 500,
        response_format: { type: 'json_object' },
      })
      
      return {
        content: JSON.parse(response.choices[0].message.content || '{}'),
        time: performance.now() - start,
        tokens: response.usage?.total_tokens || 0,
      }
    } catch (error) {
      // フォールバック: テンプレートコンテンツ
      return {
        content: {
          headline: `${data.companyName}の革新的なサービス`,
          subheadline: '業界最高水準の品質をお約束',
          cta: '今すぐ無料相談',
        },
        time: performance.now() - start,
        tokens: 0,
      }
    }
  }
  
  private async prepareImages(data: any) {
    const start = performance.now()
    
    // 画像準備（モック: 実際はDALL-E 3またはStable Diffusion）
    // 事前生成済み画像バンクから選択
    await this.sleep(Math.random() * 1000 + 1000) // 1-2秒
    
    return {
      images: [
        `/images/stock/${data.industry}_hero.jpg`,
        `/images/stock/${data.industry}_feature1.jpg`,
        `/images/stock/${data.industry}_feature2.jpg`,
      ],
      time: performance.now() - start,
    }
  }
  
  private async integrateComponents(template: any, content: any, images: any) {
    // コンポーネント統合
    await this.sleep(Math.random() * 500 + 500) // 0.5-1秒
    
    return {
      template: template.templateId,
      content: content.content,
      images: images.images,
      metadata: {
        generatedAt: new Date().toISOString(),
      },
    }
  }
  
  private async optimizeLP(lp: any) {
    // 最適化処理（圧縮、CDN準備など）
    await this.sleep(Math.random() * 300 + 200) // 0.2-0.5秒
    
    return {
      ...lp,
      optimized: true,
      performance: {
        loadTime: Math.random() * 1000 + 1000, // 1-2秒
        size: Math.random() * 500 + 500, // 500-1000KB
      },
    }
  }
  
  private async checkCompliance(lp: any) {
    // 法令チェック（簡易版）
    await this.sleep(Math.random() * 500 + 500) // 0.5-1秒
    
    return {
      ...lp,
      compliance: {
        checked: true,
        violations: [],
        score: Math.random() * 20 + 80, // 80-100
      },
    }
  }
  
  private async saveToDatabase(lp: any) {
    // データベース保存（モック）
    await this.sleep(Math.random() * 200 + 100) // 0.1-0.3秒
    
    return `lp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  }
  
  private async sleep(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms))
  }
  
  private printSummary() {
    console.log('\n\n=== 📊 テスト結果サマリー ===\n')
    
    const successCount = this.results.filter(r => r.success).length
    const successRate = (successCount / TEST_ITERATIONS) * 100
    const cacheHitCount = this.results.filter(r => r.cacheHit).length
    const cacheHitRate = (cacheHitCount / TEST_ITERATIONS) * 100
    
    const times = this.results.map(r => r.totalTime)
    const avgTime = times.reduce((a, b) => a + b, 0) / times.length
    const minTime = Math.min(...times)
    const maxTime = Math.max(...times)
    const p95Time = this.calculatePercentile(times, 95)
    
    console.log(`成功率: ${successRate.toFixed(1)}% (${successCount}/${TEST_ITERATIONS})`)
    console.log(`キャッシュヒット率: ${cacheHitRate.toFixed(1)}% (目標: ${CACHE_HIT_TARGET * 100}%)`)
    console.log('\n⏱️  処理時間統計:')
    console.log(`  平均: ${avgTime.toFixed(2)}ms`)
    console.log(`  最小: ${minTime.toFixed(2)}ms`)
    console.log(`  最大: ${maxTime.toFixed(2)}ms`)
    console.log(`  P95: ${p95Time.toFixed(2)}ms`)
    
    // 処理別の平均時間
    console.log('\n📈 処理別平均時間:')
    const breakdown = this.calculateAverageBreakdown()
    Object.entries(breakdown).forEach(([key, value]) => {
      console.log(`  ${this.formatKey(key)}: ${value.toFixed(2)}ms`)
    })
    
    // 判定
    console.log('\n🎯 総合判定:')
    if (successRate >= 90 && avgTime <= TARGET_TIME_MS) {
      console.log('✅ POC成功: 目標を達成しました！')
    } else if (successRate >= 70 && p95Time <= TARGET_TIME_MS * 1.5) {
      console.log('⚠️  POC条件付き成功: 最適化により目標達成可能')
    } else {
      console.log('❌ POC失敗: 大幅な改善が必要です')
    }
    
    // 改善提案
    console.log('\n💡 改善提案:')
    this.generateImprovementSuggestions(breakdown)
  }
  
  private calculatePercentile(values: number[], percentile: number): number {
    const sorted = [...values].sort((a, b) => a - b)
    const index = Math.ceil((percentile / 100) * sorted.length) - 1
    return sorted[index]
  }
  
  private calculateAverageBreakdown() {
    const breakdown: any = {}
    const nonCacheHitResults = this.results.filter(r => !r.cacheHit)
    
    if (nonCacheHitResults.length === 0) return breakdown
    
    Object.keys(nonCacheHitResults[0].breakdownTimes).forEach(key => {
      const values = nonCacheHitResults.map(r => (r.breakdownTimes as any)[key])
      breakdown[key] = values.reduce((a, b) => a + b, 0) / values.length
    })
    
    return breakdown
  }
  
  private formatKey(key: string): string {
    const formatted: { [key: string]: string } = {
      cacheCheck: 'キャッシュチェック',
      templateSelection: 'テンプレート選択',
      contentGeneration: 'コンテンツ生成',
      imagePreparation: '画像準備',
      integration: '統合処理',
      optimization: '最適化',
      complianceCheck: 'コンプライアンス',
      databaseSave: 'DB保存',
    }
    return formatted[key] || key
  }
  
  private generateImprovementSuggestions(breakdown: any) {
    const suggestions = []
    
    if (breakdown.contentGeneration > 3000) {
      suggestions.push('• GPT-3.5 Turboへの完全移行でコンテンツ生成を高速化')
    }
    
    if (breakdown.imagePreparation > 2000) {
      suggestions.push('• 事前生成済み画像バンクの拡充')
      suggestions.push('• Stable Diffusionの並列処理実装')
    }
    
    if (breakdown.complianceCheck > 1000) {
      suggestions.push('• コンプライアンスチェックの非同期化')
      suggestions.push('• ルールベースチェックの高速化')
    }
    
    if (breakdown.databaseSave > 300) {
      suggestions.push('• データベース接続プーリングの最適化')
      suggestions.push('• バッチ保存の実装')
    }
    
    const cacheHitRate = this.results.filter(r => r.cacheHit).length / TEST_ITERATIONS
    if (cacheHitRate < CACHE_HIT_TARGET) {
      suggestions.push('• キャッシュ戦略の改善（TTL延長、キー設計見直し）')
    }
    
    if (suggestions.length === 0) {
      suggestions.push('• 現在の実装で目標を達成しています')
    }
    
    suggestions.forEach(s => console.log(s))
  }
}

// 実行
async function main() {
  const poc = new LPGenerationPOC()
  
  try {
    await poc.runTests()
  } catch (error) {
    console.error('POC実行エラー:', error)
  } finally {
    await prisma.$disconnect()
    redis.disconnect()
  }
}

// テスト実行
if (require.main === module) {
  main()
}

export { LPGenerationPOC, TestResult }