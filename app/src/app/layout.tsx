import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
  title: 'GROWTH AI - 史上最高クラスLP制作システム',
  description: 'Google級AI技術でプロ級LPを30秒で生成。ROI 40,000%、96%コストダウンを実現。年間数億円企業の手法を月額5万円で。',
  keywords: ['LP制作', 'AI', '自動生成', 'マーケティング', 'Google級AI', 'ROI'],
  authors: [{ name: 'GROWTH AI Team' }],
  robots: 'index, follow',
  openGraph: {
    title: 'GROWTH AI - 史上最高クラスLP制作システム',
    description: 'Google級AI技術でプロ級LPを30秒で生成。ROI 40,000%を実現。',
    type: 'website',
    locale: 'ja_JP',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'GROWTH AI - 史上最高クラスLP制作システム',
    description: 'Google級AI技術でプロ級LPを30秒で生成。ROI 40,000%を実現。',
  },
};

export function generateViewport() {
  return {
    width: 'device-width',
    initialScale: 1,
  }
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ja" suppressHydrationWarning>
      <head>
        <meta charSet="utf-8" />
        <link rel="icon" href="/favicon.ico" />
      </head>
      <body className={inter.className} suppressHydrationWarning>
        <div className="min-h-screen bg-gradient-to-br from-gray-900 via-black to-purple-900 text-white">
          {children}
        </div>
      </body>
    </html>
  );
}