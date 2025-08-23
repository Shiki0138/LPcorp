'use client';

/**
 * 経営ダッシュボード
 * リアルタイムKPI・売上監視・プロジェクト俯瞰・アラート通知
 */

import React, { useState, useEffect, useCallback } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Progress } from '@/components/ui/progress';

// アイコン（実際のプロジェクトではreact-iconsやlucide-react等を使用）
const TrendingUpIcon = () => <span>📈</span>;
const TrendingDownIcon = () => <span>📉</span>;
const AlertTriangleIcon = () => <span>⚠️</span>;
const DollarSignIcon = () => <span>💰</span>;
const UsersIcon = () => <span>👥</span>;
const EyeIcon = () => <span>👁️</span>;
const ShoppingCartIcon = () => <span>🛒</span>;
const BarChartIcon = () => <span>📊</span>;
const CalendarIcon = () => <span>📅</span>;
const BellIcon = () => <span>🔔</span>;

// 型定義
interface KPIMetric {
  title: string;
  value: string | number;
  change: number;
  changeType: 'increase' | 'decrease';
  target?: number;
  unit?: string;
  icon: React.ComponentType;
}

interface RealtimeData {
  timestamp: number;
  activeUsers: number;
  revenue: number;
  conversions: number;
  pageViews: number;
}

interface Alert {
  id: string;
  type: 'critical' | 'warning' | 'info';
  title: string;
  message: string;
  timestamp: number;
  resolved: boolean;
}

interface ProjectOverview {
  id: string;
  name: string;
  status: 'active' | 'completed' | 'paused';
  progress: number;
  revenue: number;
  cvr: number;
  lastUpdated: Date;
}

const ExecutiveDashboard: React.FC = () => {
  // State管理
  const [realtimeData, setRealtimeData] = useState<RealtimeData[]>([]);
  const [kpiMetrics, setKpiMetrics] = useState<KPIMetric[]>([]);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [projects, setProjects] = useState<ProjectOverview[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedTimeRange, setSelectedTimeRange] = useState<'24h' | '7d' | '30d'>('24h');
  const [websocket, setWebsocket] = useState<WebSocket | null>(null);

  /**
   * WebSocket接続・リアルタイム更新
   */
  useEffect(() => {
    const connectWebSocket = () => {
      try {
        const ws = new WebSocket(process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:3001/dashboard');
        
        ws.onopen = () => {
          console.log('ダッシュボードWebSocket接続確立');
          setWebsocket(ws);
        };

        ws.onmessage = (event) => {
          const data = JSON.parse(event.data);
          handleRealtimeUpdate(data);
        };

        ws.onerror = (error) => {
          console.error('WebSocket接続エラー:', error);
        };

        ws.onclose = () => {
          console.log('WebSocket接続切断 - 再接続試行中...');
          // 5秒後に再接続
          setTimeout(connectWebSocket, 5000);
        };

      } catch (error) {
        console.error('WebSocket初期化エラー:', error);
      }
    };

    connectWebSocket();

    return () => {
      if (websocket) {
        websocket.close();
      }
    };
  }, []);

  /**
   * 初期データ読み込み
   */
  useEffect(() => {
    loadInitialData();
  }, [selectedTimeRange]);

  /**
   * 定期更新（WebSocket未接続時のフォールバック）
   */
  useEffect(() => {
    const interval = setInterval(() => {
      if (!websocket || websocket.readyState !== WebSocket.OPEN) {
        refreshData();
      }
    }, 30000); // 30秒間隔

    return () => clearInterval(interval);
  }, [websocket]);

  /**
   * 初期データ読み込み
   */
  const loadInitialData = async () => {
    setIsLoading(true);
    
    try {
      await Promise.all([
        loadKPIMetrics(),
        loadRealtimeData(),
        loadAlerts(),
        loadProjectOverview()
      ]);
    } catch (error) {
      console.error('初期データ読み込みエラー:', error);
      showErrorAlert('データの読み込みに失敗しました');
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * KPI指標読み込み
   */
  const loadKPIMetrics = async () => {
    try {
      const response = await fetch(`/api/dashboard/kpi?timeRange=${selectedTimeRange}`);
      const data = await response.json();

      const metrics: KPIMetric[] = [
        {
          title: '総売上',
          value: formatCurrency(data.totalRevenue || 0),
          change: data.revenueChange || 0,
          changeType: data.revenueChange >= 0 ? 'increase' : 'decrease',
          target: data.revenueTarget,
          icon: DollarSignIcon
        },
        {
          title: 'コンバージョン率',
          value: `${(data.conversionRate || 0).toFixed(2)}%`,
          change: data.conversionChange || 0,
          changeType: data.conversionChange >= 0 ? 'increase' : 'decrease',
          target: data.conversionTarget,
          icon: ShoppingCartIcon
        },
        {
          title: 'アクティブユーザー',
          value: formatNumber(data.activeUsers || 0),
          change: data.usersChange || 0,
          changeType: data.usersChange >= 0 ? 'increase' : 'decrease',
          icon: UsersIcon
        },
        {
          title: 'ページビュー',
          value: formatNumber(data.pageViews || 0),
          change: data.pageViewsChange || 0,
          changeType: data.pageViewsChange >= 0 ? 'increase' : 'decrease',
          icon: EyeIcon
        }
      ];

      setKpiMetrics(metrics);

    } catch (error) {
      console.error('KPI読み込みエラー:', error);
    }
  };

  /**
   * リアルタイムデータ読み込み
   */
  const loadRealtimeData = async () => {
    try {
      const response = await fetch(`/api/dashboard/realtime?timeRange=${selectedTimeRange}`);
      const data = await response.json();
      setRealtimeData(data.timeSeries || []);
    } catch (error) {
      console.error('リアルタイムデータ読み込みエラー:', error);
    }
  };

  /**
   * アラート読み込み
   */
  const loadAlerts = async () => {
    try {
      const response = await fetch('/api/dashboard/alerts');
      const data = await response.json();
      setAlerts(data.alerts || []);
    } catch (error) {
      console.error('アラート読み込みエラー:', error);
    }
  };

  /**
   * プロジェクト概要読み込み
   */
  const loadProjectOverview = async () => {
    try {
      const response = await fetch('/api/dashboard/projects');
      const data = await response.json();
      setProjects(data.projects || []);
    } catch (error) {
      console.error('プロジェクト概要読み込みエラー:', error);
    }
  };

  /**
   * リアルタイム更新処理
   */
  const handleRealtimeUpdate = (data: any) => {
    switch (data.type) {
      case 'kpi_update':
        updateKPIMetrics(data.metrics);
        break;
        
      case 'realtime_data':
        addRealtimeDataPoint(data.dataPoint);
        break;
        
      case 'new_alert':
        addAlert(data.alert);
        break;
        
      case 'project_update':
        updateProject(data.project);
        break;
        
      default:
        console.log('未知のリアルタイム更新:', data);
    }
  };

  /**
   * KPI指標更新
   */
  const updateKPIMetrics = (newMetrics: any) => {
    setKpiMetrics(prevMetrics => 
      prevMetrics.map(metric => {
        const update = newMetrics[metric.title];
        return update ? { ...metric, ...update } : metric;
      })
    );
  };

  /**
   * リアルタイムデータポイント追加
   */
  const addRealtimeDataPoint = (dataPoint: RealtimeData) => {
    setRealtimeData(prevData => {
      const newData = [...prevData, dataPoint];
      // 最新100ポイントのみ保持
      return newData.slice(-100);
    });
  };

  /**
   * アラート追加
   */
  const addAlert = (alert: Alert) => {
    setAlerts(prevAlerts => [alert, ...prevAlerts]);
    
    // ブラウザ通知
    if (Notification.permission === 'granted') {
      new Notification(alert.title, {
        body: alert.message,
        icon: '/favicon.ico'
      });
    }
  };

  /**
   * プロジェクト更新
   */
  const updateProject = (updatedProject: ProjectOverview) => {
    setProjects(prevProjects =>
      prevProjects.map(project =>
        project.id === updatedProject.id ? updatedProject : project
      )
    );
  };

  /**
   * データ手動更新
   */
  const refreshData = useCallback(async () => {
    await loadInitialData();
  }, [selectedTimeRange]);

  /**
   * アラート解決
   */
  const resolveAlert = async (alertId: string) => {
    try {
      await fetch(`/api/dashboard/alerts/${alertId}/resolve`, {
        method: 'POST'
      });

      setAlerts(prevAlerts =>
        prevAlerts.map(alert =>
          alert.id === alertId ? { ...alert, resolved: true } : alert
        )
      );
    } catch (error) {
      console.error('アラート解決エラー:', error);
    }
  };

  /**
   * エラーアラート表示
   */
  const showErrorAlert = (message: string) => {
    const errorAlert: Alert = {
      id: `error_${Date.now()}`,
      type: 'critical',
      title: 'システムエラー',
      message,
      timestamp: Date.now(),
      resolved: false
    };
    addAlert(errorAlert);
  };

  // ユーティリティ関数
  const formatCurrency = (value: number): string => {
    return new Intl.NumberFormat('ja-JP', {
      style: 'currency',
      currency: 'JPY'
    }).format(value);
  };

  const formatNumber = (value: number): string => {
    return new Intl.NumberFormat('ja-JP').format(value);
  };

  const getChangeColor = (changeType: 'increase' | 'decrease') => {
    return changeType === 'increase' ? 'text-green-600' : 'text-red-600';
  };

  const getAlertColor = (type: 'critical' | 'warning' | 'info') => {
    switch (type) {
      case 'critical': return 'destructive';
      case 'warning': return 'secondary';
      case 'info': return 'outline';
      default: return 'outline';
    }
  };

  const getProjectStatusColor = (status: string) => {
    switch (status) {
      case 'active': return 'bg-green-500';
      case 'completed': return 'bg-blue-500';
      case 'paused': return 'bg-yellow-500';
      default: return 'bg-gray-500';
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">ダッシュボード読み込み中...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto">
        {/* ヘッダー */}
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">経営ダッシュボード</h1>
            <p className="text-gray-600 mt-2">
              最終更新: {new Date().toLocaleString('ja-JP')}
              {websocket?.readyState === WebSocket.OPEN && (
                <span className="ml-2 inline-flex items-center">
                  <span className="w-2 h-2 bg-green-400 rounded-full mr-1 animate-pulse"></span>
                  リアルタイム更新中
                </span>
              )}
            </p>
          </div>
          
          <div className="flex gap-4">
            {/* 時間範囲選択 */}
            <select
              value={selectedTimeRange}
              onChange={(e) => setSelectedTimeRange(e.target.value as '24h' | '7d' | '30d')}
              className="px-4 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="24h">24時間</option>
              <option value="7d">7日間</option>
              <option value="30d">30日間</option>
            </select>
            
            <Button onClick={refreshData} variant="outline">
              <span className="mr-2">🔄</span>
              更新
            </Button>
          </div>
        </div>

        {/* アラート通知 */}
        {alerts.filter(alert => !alert.resolved).length > 0 && (
          <div className="mb-6">
            <Card className="border-red-200 bg-red-50">
              <CardHeader className="pb-3">
                <div className="flex items-center">
                  <BellIcon />
                  <CardTitle className="text-red-800 ml-2">アクティブアラート</CardTitle>
                </div>
              </CardHeader>
              <CardContent>
                <div className="space-y-2">
                  {alerts
                    .filter(alert => !alert.resolved)
                    .slice(0, 3)
                    .map(alert => (
                      <div key={alert.id} className="flex items-center justify-between p-3 bg-white rounded border">
                        <div className="flex items-center">
                          <Badge variant={getAlertColor(alert.type)} className="mr-3">
                            {alert.type}
                          </Badge>
                          <div>
                            <p className="font-medium text-gray-900">{alert.title}</p>
                            <p className="text-sm text-gray-600">{alert.message}</p>
                          </div>
                        </div>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => resolveAlert(alert.id)}
                        >
                          解決
                        </Button>
                      </div>
                    ))}
                </div>
              </CardContent>
            </Card>
          </div>
        )}

        {/* KPI指標カード */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          {kpiMetrics.map((metric, index) => (
            <Card key={index} className="hover:shadow-lg transition-shadow">
              <CardContent className="p-6">
                <div className="flex items-center justify-between mb-4">
                  <div className="p-2 bg-blue-100 rounded-lg">
                    <metric.icon />
                  </div>
                  <div className={`flex items-center ${getChangeColor(metric.changeType)}`}>
                    {metric.changeType === 'increase' ? <TrendingUpIcon /> : <TrendingDownIcon />}
                    <span className="ml-1 text-sm font-medium">
                      {Math.abs(metric.change).toFixed(1)}%
                    </span>
                  </div>
                </div>
                
                <div>
                  <p className="text-sm font-medium text-gray-600 mb-1">{metric.title}</p>
                  <p className="text-2xl font-bold text-gray-900 mb-2">{metric.value}</p>
                  
                  {metric.target && (
                    <div className="mt-3">
                      <div className="flex justify-between text-xs text-gray-600 mb-1">
                        <span>目標達成率</span>
                        <span>{((typeof metric.value === 'string' ? parseFloat(metric.value.replace(/[^0-9.-]/g, '')) : metric.value) / metric.target * 100).toFixed(0)}%</span>
                      </div>
                      <Progress
                        value={(typeof metric.value === 'string' ? parseFloat(metric.value.replace(/[^0-9.-]/g, '')) : metric.value) / metric.target * 100}
                        className="h-2"
                      />
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* メインコンテンツ */}
        <Tabs defaultValue="overview" className="space-y-6">
          <TabsList className="grid w-full grid-cols-4">
            <TabsTrigger value="overview">概要</TabsTrigger>
            <TabsTrigger value="analytics">分析</TabsTrigger>
            <TabsTrigger value="projects">プロジェクト</TabsTrigger>
            <TabsTrigger value="alerts">アラート</TabsTrigger>
          </TabsList>

          {/* 概要タブ */}
          <TabsContent value="overview">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* リアルタイムチャート */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center">
                    <BarChartIcon />
                    <span className="ml-2">リアルタイム活動</span>
                  </CardTitle>
                  <CardDescription>
                    過去{selectedTimeRange === '24h' ? '24時間' : selectedTimeRange === '7d' ? '7日間' : '30日間'}のトレンド
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="h-64 flex items-center justify-center">
                    <p className="text-gray-500">チャート実装予定エリア</p>
                    <span className="ml-2">📊</span>
                  </div>
                </CardContent>
              </Card>

              {/* 最新活動 */}
              <Card>
                <CardHeader>
                  <CardTitle>最新活動</CardTitle>
                  <CardDescription>リアルタイム更新</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    {realtimeData.slice(-5).map((data, index) => (
                      <div key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded">
                        <div>
                          <p className="font-medium">アクティブユーザー: {formatNumber(data.activeUsers)}</p>
                          <p className="text-sm text-gray-600">
                            {new Date(data.timestamp).toLocaleTimeString('ja-JP')}
                          </p>
                        </div>
                        <div className="text-right">
                          <p className="font-medium">{formatCurrency(data.revenue)}</p>
                          <p className="text-sm text-gray-600">{data.conversions}件のCV</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            </div>
          </TabsContent>

          {/* 分析タブ */}
          <TabsContent value="analytics">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <Card>
                <CardHeader>
                  <CardTitle>コンバージョン分析</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="h-64 flex items-center justify-center">
                    <p className="text-gray-500">詳細分析チャート実装予定</p>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle>ユーザー行動分析</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="h-64 flex items-center justify-center">
                    <p className="text-gray-500">ヒートマップ実装予定</p>
                  </div>
                </CardContent>
              </Card>
            </div>
          </TabsContent>

          {/* プロジェクトタブ */}
          <TabsContent value="projects">
            <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
              {projects.map(project => (
                <Card key={project.id} className="hover:shadow-lg transition-shadow">
                  <CardHeader>
                    <div className="flex items-center justify-between">
                      <CardTitle className="text-lg">{project.name}</CardTitle>
                      <div className={`w-3 h-3 rounded-full ${getProjectStatusColor(project.status)}`}></div>
                    </div>
                    <CardDescription>
                      最終更新: {project.lastUpdated.toLocaleDateString('ja-JP')}
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      <div>
                        <div className="flex justify-between text-sm mb-2">
                          <span>進捗</span>
                          <span>{project.progress}%</span>
                        </div>
                        <Progress value={project.progress} className="h-2" />
                      </div>
                      
                      <div className="grid grid-cols-2 gap-4 text-sm">
                        <div>
                          <p className="text-gray-600">売上</p>
                          <p className="font-semibold">{formatCurrency(project.revenue)}</p>
                        </div>
                        <div>
                          <p className="text-gray-600">CVR</p>
                          <p className="font-semibold">{project.cvr.toFixed(2)}%</p>
                        </div>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          </TabsContent>

          {/* アラートタブ */}
          <TabsContent value="alerts">
            <Card>
              <CardHeader>
                <CardTitle>アラート履歴</CardTitle>
                <CardDescription>システム通知とアラートの管理</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {alerts.map(alert => (
                    <div
                      key={alert.id}
                      className={`p-4 rounded border ${alert.resolved ? 'bg-gray-50 opacity-60' : 'bg-white'}`}
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center">
                          <Badge variant={getAlertColor(alert.type)} className="mr-3">
                            {alert.type}
                          </Badge>
                          <div>
                            <p className={`font-medium ${alert.resolved ? 'text-gray-500' : 'text-gray-900'}`}>
                              {alert.title}
                            </p>
                            <p className={`text-sm ${alert.resolved ? 'text-gray-400' : 'text-gray-600'}`}>
                              {alert.message}
                            </p>
                            <p className="text-xs text-gray-400 mt-1">
                              {new Date(alert.timestamp).toLocaleString('ja-JP')}
                            </p>
                          </div>
                        </div>
                        
                        {!alert.resolved && (
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => resolveAlert(alert.id)}
                          >
                            解決
                          </Button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
};

export default ExecutiveDashboard;