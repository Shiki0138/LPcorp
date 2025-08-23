# Inventory Service

A comprehensive inventory management service with advanced stock management capabilities, real-time tracking, demand forecasting, and supplier integration.

## 🚀 Features

### Core Inventory Management
- **Real-time Stock Tracking**: Live inventory levels across multiple locations
- **Multi-location Support**: Manage inventory across warehouses, stores, and distribution centers
- **Stock Reservations**: Reserve inventory for orders with expiration handling
- **Batch & Serial Number Tracking**: Complete traceability for products
- **Expiry Date Management**: Handle perishable goods with expiry tracking

### Advanced Stock Operations
- **Stock Transfers**: Move inventory between locations
- **Stock Adjustments**: Manual corrections and cycle counting
- **Valuation Methods**: FIFO, LIFO, and weighted average costing
- **Reorder Point Management**: Automated low-stock alerts
- **Dead Stock Identification**: Identify slow-moving and obsolete inventory

### Analytics & Reporting
- **Inventory Turnover Analysis**: Track product performance
- **ABC Analysis**: Classify products by sales value
- **Demand Forecasting**: Multiple forecasting algorithms
- **Supplier Performance**: Track delivery times and quality
- **Stock Valuation Reports**: Current inventory values

### Supplier Management
- **Supplier Profiles**: Comprehensive supplier information
- **Purchase Order Automation**: Automated PO generation
- **Performance Tracking**: Rating and performance metrics
- **Lead Time Management**: Track and optimize delivery times
- **Contract Management**: Handle supplier agreements

## 🏗️ Architecture

### Domain-Driven Design
- **Entities**: Product, StockLevel, StockMovement, Supplier, PurchaseOrder
- **Value Objects**: Batch info, pricing, dimensions
- **Events**: Domain events for all inventory operations
- **Services**: Business logic encapsulation

### Event-Driven Architecture
- **Event Bus**: Real-time event publishing and subscription
- **Event Store**: Complete audit trail of all operations
- **Event Handlers**: Reactive processing of domain events
- **Integration Events**: External system notifications

## 📦 Installation

```bash
# Clone the repository
git clone <repository-url>
cd inventory-service

# Install dependencies
npm install

# Setup environment variables
cp .env.example .env

# Run database migrations
npm run migrate

# Start the service
npm start
```

## 🔧 Configuration

### Environment Variables

```bash
# Database
DATABASE_URL=postgresql://user:password@localhost:5432/inventory
DATABASE_POOL_SIZE=10

# Redis (for caching and sessions)
REDIS_URL=redis://localhost:6379

# Event Bus
EVENT_BUS_TYPE=redis
EVENT_STORE_ENABLED=true

# External Services
SUPPLIER_API_TIMEOUT=30000
NOTIFICATION_SERVICE_URL=http://localhost:3001

# Security
JWT_SECRET=your-secret-key
API_RATE_LIMIT=1000
```

## 🚀 Quick Start

### 1. Add Stock to Location

```javascript
POST /api/inventory/stock/add
{
  "productId": "prod-123",
  "locationId": "warehouse-01",
  "quantity": 100,
  "costPerUnit": 25.50,
  "batchInfo": {
    "batchNumber": "BATCH-001",
    "expiryDate": "2024-12-31",
    "supplierLot": "SUP-LOT-001"
  }
}
```

### 2. Check Stock Availability

```javascript
GET /api/inventory/products/prod-123/availability?quantity=50
{
  "isAvailable": true,
  "totalAvailable": 100,
  "requiredQuantity": 50,
  "shortfall": 0,
  "locationBreakdown": [
    {
      "locationId": "warehouse-01",
      "available": 100,
      "total": 100,
      "reserved": 0
    }
  ]
}
```

### 3. Reserve Stock for Order

```javascript
POST /api/inventory/stock/reserve
{
  "productId": "prod-123",
  "locationId": "warehouse-01",
  "quantity": 25,
  "reservationId": "order-456",
  "expiresAt": "2024-01-15T10:00:00Z"
}
```

### 4. Transfer Between Locations

```javascript
POST /api/inventory/stock/transfer
{
  "productId": "prod-123",
  "fromLocationId": "warehouse-01",
  "toLocationId": "store-01",
  "quantity": 30,
  "reason": "store_replenishment"
}
```

## 📊 Analytics Examples

### Inventory Turnover Analysis

```javascript
GET /api/inventory/analytics/turnover?periodDays=365&locationId=warehouse-01
{
  "summary": {
    "totalProducts": 150,
    "totalValue": 125000.00,
    "averageTurnover": 3.2
  },
  "classification": {
    "fast": [/* High turnover products */],
    "medium": [/* Medium turnover products */],
    "slow": [/* Low turnover products */],
    "dead": [/* No movement products */]
  }
}
```

### ABC Analysis

```javascript
GET /api/inventory/analytics/abc?periodDays=365
{
  "classification": {
    "A": { "products": [/* Top 80% value */], "percentage": 80 },
    "B": { "products": [/* Next 15% value */], "percentage": 15 },
    "C": { "products": [/* Bottom 5% value */], "percentage": 5 }
  }
}
```

### Demand Forecasting

```javascript
GET /api/inventory/products/prod-123/forecast?method=weighted_moving_average&forecastPeriodDays=30
{
  "forecast": [
    {
      "date": "2024-01-16",
      "predictedDemand": 12,
      "confidenceInterval": {
        "lower": 8,
        "upper": 16,
        "confidence": 0.95
      }
    }
  ],
  "metadata": {
    "method": "weighted_moving_average",
    "accuracy": 85.2,
    "trend": 0.15
  }
}
```

## 🔄 Event System

### Stock Level Events

```javascript
// Stock level changed
{
  "type": "StockLevelChanged",
  "data": {
    "stockLevelId": "stock-123",
    "productId": "prod-123",
    "locationId": "warehouse-01",
    "previousQuantity": 100,
    "newQuantity": 125,
    "changeAmount": 25,
    "reason": "purchase"
  }
}

// Reorder point reached
{
  "type": "StockReorderPointReached",
  "data": {
    "stockLevelId": "stock-123",
    "productId": "prod-123",
    "currentQuantity": 5,
    "reorderPoint": 10,
    "reorderQuantity": 50
  }
}
```

### Purchase Order Events

```javascript
// Purchase order created
{
  "type": "PurchaseOrderCreated",
  "data": {
    "purchaseOrderId": "po-123",
    "orderNumber": "PO-20240115-001",
    "supplierId": "supplier-456",
    "totalAmount": 2500.00,
    "itemCount": 5
  }
}
```

## 🧪 Testing

### Run Tests

```bash
# Run all tests
npm test

# Run unit tests only
npm run test:unit

# Run integration tests only
npm run test:integration

# Run with coverage
npm run test:coverage
```

### Test Coverage

- **Unit Tests**: Service layer and domain logic
- **Integration Tests**: API endpoints and workflows
- **Event Tests**: Event publishing and handling
- **Repository Tests**: Data access layer

## 📈 Performance

### Optimization Features

- **Database Indexing**: Optimized queries for large datasets
- **Caching**: Redis caching for frequently accessed data
- **Batch Operations**: Bulk updates for performance
- **Event Batching**: Efficient event processing
- **Connection Pooling**: Optimized database connections

### Monitoring

```javascript
GET /api/inventory/health
{
  "status": "healthy",
  "version": "1.0.0",
  "uptime": 86400,
  "database": "connected",
  "eventBus": "operational",
  "metrics": {
    "totalProducts": 1500,
    "totalLocations": 25,
    "activeReservations": 45,
    "pendingPOs": 12
  }
}
```

## 🔗 Integration

### Order Service Integration

```javascript
// Subscribe to order events
eventBus.subscribe('OrderCreated', async (event) => {
  // Reserve stock for order items
  for (const item of event.data.items) {
    await stockManagementService.reserveStock({
      productId: item.productId,
      locationId: item.fulfillmentLocationId,
      quantity: item.quantity,
      reservationId: event.data.orderId
    });
  }
});
```

### External Supplier APIs

```javascript
// Supplier integration example
class SupplierApiIntegration {
  async sendPurchaseOrder(purchaseOrder, supplier) {
    if (supplier.apiConfig) {
      await this.callSupplierAPI(supplier.apiConfig, purchaseOrder);
    } else {
      await this.sendEmailPO(supplier.contactInfo.email, purchaseOrder);
    }
  }
}
```

## 🛡️ Security

### API Security
- **JWT Authentication**: Secure API access
- **Rate Limiting**: Prevent API abuse
- **Input Validation**: Comprehensive request validation
- **CORS Configuration**: Cross-origin request handling

### Data Security
- **Audit Trails**: Complete operation history
- **Access Control**: Role-based permissions
- **Data Encryption**: Sensitive data protection
- **Backup Strategy**: Regular automated backups

## 📋 API Reference

### Stock Management Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/inventory/stock/add` | Add stock to location |
| POST | `/api/inventory/stock/remove` | Remove stock from location |
| POST | `/api/inventory/stock/transfer` | Transfer between locations |
| POST | `/api/inventory/stock/adjust` | Adjust stock quantities |
| POST | `/api/inventory/stock/reserve` | Reserve stock for orders |
| DELETE | `/api/inventory/reservations/{id}` | Release stock reservation |

### Analytics Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/inventory/analytics/turnover` | Inventory turnover analysis |
| GET | `/api/inventory/analytics/abc` | ABC classification analysis |
| GET | `/api/inventory/analytics/dead-stock` | Dead stock identification |
| GET | `/api/inventory/analytics/reorder` | Reorder recommendations |
| GET | `/api/inventory/analytics/valuation` | Stock valuation report |

### Purchase Order Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/inventory/purchase-orders` | Create purchase order |
| PUT | `/api/inventory/purchase-orders/{id}/approve` | Approve purchase order |
| POST | `/api/inventory/purchase-orders/{id}/receive` | Receive goods |
| POST | `/api/inventory/purchase-orders/auto-generate` | Generate automatic POs |

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For support and questions:
- Email: support@inventory-service.com
- Documentation: [docs.inventory-service.com](https://docs.inventory-service.com)
- Issues: [GitHub Issues](https://github.com/your-org/inventory-service/issues)