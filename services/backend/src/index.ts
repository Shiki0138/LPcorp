import express from 'express'
import cors from 'cors'
import helmet from 'helmet'
import morgan from 'morgan'
import { createServer } from 'http'
import { Server } from 'socket.io'
import dotenv from 'dotenv'
import { PrismaClient } from '@prisma/client'
import Redis from 'ioredis'

// ルートインポート
import authRoutes from './routes/auth.routes'
import lpRoutes from './routes/lp.routes'
import analyticsRoutes from './routes/analytics.routes'
import aiRoutes from './routes/ai.routes'
import paymentRoutes from './routes/payment.routes'

// ミドルウェアインポート
import { errorHandler } from './middleware/error.middleware'
import { rateLimiter } from './middleware/rateLimiter.middleware'
import { authMiddleware as authenticate } from './middleware/auth.middleware'

// ユーティリティ
import logger from './utils/logger'
import { setupWebSocket } from './services/websocket.service'

dotenv.config({ path: '../../.env' })

const app = express()
const httpServer = createServer(app)
const io = new Server(httpServer, {
  cors: {
    origin: process.env.FRONTEND_URL || 'http://localhost:3000',
    credentials: true,
  },
})

// データベース接続
export const prisma = new PrismaClient()
export const redis = new Redis({
  host: process.env.REDIS_HOST || 'localhost',
  port: parseInt(process.env.REDIS_PORT || '6379'),
  password: process.env.REDIS_PASSWORD,
})

// ミドルウェア設定
app.use(helmet())
app.use(cors({
  origin: process.env.FRONTEND_URL || 'http://localhost:3000',
  credentials: true,
}))
app.use(express.json({ limit: '10mb' }))
app.use(express.urlencoded({ extended: true, limit: '10mb' }))
app.use(morgan('combined', { stream: { write: (message) => logger.info(message.trim()) } }))

// レート制限
app.use('/api/', rateLimiter)

// ヘルスチェック
app.get('/health', (req, res) => {
  res.json({
    status: 'healthy',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
  })
})

// APIルート
app.use('/api/auth', authRoutes)
app.use('/api/lp', authenticate, lpRoutes)
app.use('/api/analytics', authenticate, analyticsRoutes)
app.use('/api/ai', authenticate, aiRoutes)
app.use('/api/payment', authenticate, paymentRoutes)

// WebSocket初期化
setupWebSocket(httpServer)

// エラーハンドリング
app.use(errorHandler)

// 404ハンドリング
app.use((req, res) => {
  res.status(404).json({
    error: 'Not Found',
    message: 'The requested resource was not found',
  })
})

// サーバー起動
const PORT = process.env.PORT || 4000

async function startServer() {
  try {
    // データベース接続確認
    await prisma.$connect()
    logger.info('Database connected successfully')

    // Redis接続確認
    await redis.ping()
    logger.info('Redis connected successfully')

    // HTTPサーバー起動
    httpServer.listen(PORT, () => {
      logger.info(`Server is running on port ${PORT}`)
      logger.info(`Environment: ${process.env.NODE_ENV}`)
    })
  } catch (error) {
    logger.error('Failed to start server:', error)
    process.exit(1)
  }
}

// グレースフルシャットダウン
process.on('SIGTERM', async () => {
  logger.info('SIGTERM signal received: closing HTTP server')
  httpServer.close(async () => {
    logger.info('HTTP server closed')
    await prisma.$disconnect()
    await redis.quit()
    process.exit(0)
  })
})

process.on('SIGINT', async () => {
  logger.info('SIGINT signal received: closing HTTP server')
  httpServer.close(async () => {
    logger.info('HTTP server closed')
    await prisma.$disconnect()
    await redis.quit()
    process.exit(0)
  })
})

// エラーハンドリング
process.on('uncaughtException', (error) => {
  logger.error('Uncaught Exception:', error)
  process.exit(1)
})

process.on('unhandledRejection', (reason, promise) => {
  logger.error('Unhandled Rejection at:', promise, 'reason:', reason)
  process.exit(1)
})

startServer()