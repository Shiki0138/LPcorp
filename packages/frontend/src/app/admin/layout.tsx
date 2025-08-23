/**
 * 管理画面レイアウト
 * 分析システム統合・権限管理・ナビゲーション
 */

import React from 'react';
import AnalyticsProvider from '@/components/analytics/AnalyticsProvider';

interface AdminLayoutProps {
  children: React.ReactNode;
}

const AdminLayout: React.FC<AdminLayoutProps> = ({ children }) => {
  return (
    <AnalyticsProvider
      config={{
        enableHeatmap: true,
        enableRealtime: true,
        enableReports: true,
        gaConfig: {
          measurementId: process.env.NEXT_PUBLIC_GA4_MEASUREMENT_ID || '',
          apiSecret: process.env.GA4_API_SECRET || ''
        }
      }}
    >
      <div className="min-h-screen bg-gray-50">
        {/* 管理画面ヘッダー */}
        <header className="bg-white shadow-sm border-b">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between h-16">
              <div className="flex items-center">
                <h1 className="text-xl font-semibold text-gray-900">
                  LP制作システム 管理画面
                </h1>
              </div>
              
              <div className="flex items-center space-x-4">
                <span className="text-sm text-gray-600">
                  分析システム統合済み
                </span>
                <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse"></div>
              </div>
            </div>
          </div>
        </header>

        {/* メインコンテンツ */}
        <main>
          {children}
        </main>
      </div>
    </AnalyticsProvider>
  );
};

export default AdminLayout;