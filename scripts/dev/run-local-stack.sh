#!/bin/bash

# Script to run the complete microservices stack locally
# This script starts all required services and infrastructure

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to wait for a service to be ready
wait_for_service() {
    local host=$1
    local port=$2
    local service=$3
    local max_attempts=30
    local attempt=1
    
    print_info "Waiting for $service to be ready..."
    
    while ! nc -z "$host" "$port" >/dev/null 2>&1; do
        if [ $attempt -eq $max_attempts ]; then
            print_error "$service failed to start on $host:$port"
            return 1
        fi
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo
    print_success "$service is ready on $host:$port"
    return 0
}

# Function to check Docker
check_docker() {
    if ! command_exists docker; then
        print_error "Docker is not installed"
        exit 1
    fi
    
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker daemon is not running"
        exit 1
    fi
}

# Function to check Docker Compose
check_docker_compose() {
    if ! command_exists docker-compose; then
        print_error "Docker Compose is not installed"
        exit 1
    fi
}

# Function to load environment variables
load_env() {
    if [ -f .env ]; then
        export $(cat .env | grep -v '^#' | xargs)
        print_success "Environment variables loaded"
    else
        print_warning ".env file not found. Using default values."
    fi
}

# Function to start infrastructure services
start_infrastructure() {
    print_info "Starting infrastructure services..."
    
    # Create docker-compose file if it doesn't exist
    if [ ! -f docker-compose.yml ]; then
        print_info "Creating docker-compose.yml..."
        cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: microservices-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD:-rootpassword}
      MYSQL_DATABASE: ${DB_NAME:-microservices}
    ports:
      - "${DB_PORT:-3306}:3306"
    volumes:
      - ./data/mysql:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  mongodb:
    image: mongo:5.0
    container_name: microservices-mongodb
    environment:
      MONGO_INITDB_ROOT_USERNAME: ${MONGO_USER:-admin}
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD:-adminpassword}
      MONGO_INITDB_DATABASE: ${MONGO_DB:-microservices}
    ports:
      - "${MONGO_PORT:-27017}:27017"
    volumes:
      - ./data/mongodb:/data/db

  redis:
    image: redis:7-alpine
    container_name: microservices-redis
    command: redis-server --requirepass ${REDIS_PASSWORD:-redispassword}
    ports:
      - "${REDIS_PORT:-6379}:6379"
    volumes:
      - ./data/redis:/data

  rabbitmq:
    image: rabbitmq:3-management
    container_name: microservices-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER:-admin}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:-adminpassword}
    ports:
      - "${RABBITMQ_PORT:-5672}:5672"
      - "15672:15672"
    volumes:
      - ./data/rabbitmq:/var/lib/rabbitmq

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.17.10
    container_name: microservices-elasticsearch
    environment:
      - discovery.type=single-node
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
      - xpack.security.enabled=false
    ports:
      - "${ES_PORT:-9200}:9200"
    volumes:
      - ./data/elasticsearch:/usr/share/elasticsearch/data

  kibana:
    image: docker.elastic.co/kibana/kibana:7.17.10
    container_name: microservices-kibana
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    ports:
      - "5601:5601"
    depends_on:
      - elasticsearch

  prometheus:
    image: prom/prometheus:latest
    container_name: microservices-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./config/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./data/prometheus:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'

  grafana:
    image: grafana/grafana:latest
    container_name: microservices-grafana
    ports:
      - "3000:3000"
    volumes:
      - ./data/grafana:/var/lib/grafana
      - ./config/grafana/provisioning:/etc/grafana/provisioning
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false

  zipkin:
    image: openzipkin/zipkin:latest
    container_name: microservices-zipkin
    ports:
      - "9411:9411"

networks:
  default:
    name: microservices-network
    driver: bridge
EOF
    fi
    
    # Start infrastructure with Docker Compose
    docker-compose up -d
    
    # Wait for services to be ready
    wait_for_service localhost "${DB_PORT:-3306}" "MySQL"
    wait_for_service localhost "${MONGO_PORT:-27017}" "MongoDB"
    wait_for_service localhost "${REDIS_PORT:-6379}" "Redis"
    wait_for_service localhost "${RABBITMQ_PORT:-5672}" "RabbitMQ"
    wait_for_service localhost "${ES_PORT:-9200}" "Elasticsearch"
}

# Function to build microservices
build_services() {
    print_info "Building microservices..."
    
    if [ -f pom.xml ]; then
        if command_exists mvn; then
            mvn clean package -DskipTests
        else
            ./mvnw clean package -DskipTests
        fi
        print_success "Build completed"
    else
        print_warning "No pom.xml found. Skipping build."
    fi
}

# Function to start microservices
start_services() {
    print_info "Starting microservices..."
    
    # Start services in order
    services=(
        "eureka-server:${EUREKA_PORT:-8761}"
        "config-server:${CONFIG_SERVER_PORT:-8888}"
        "api-gateway:${API_GATEWAY_PORT:-8080}"
        "auth-service:${AUTH_SERVICE_PORT:-8081}"
        "user-service:${USER_SERVICE_PORT:-8082}"
        "product-service:${PRODUCT_SERVICE_PORT:-8083}"
        "order-service:${ORDER_SERVICE_PORT:-8084}"
        "notification-service:${NOTIFICATION_SERVICE_PORT:-8085}"
    )
    
    for service_info in "${services[@]}"; do
        IFS=':' read -r service port <<< "$service_info"
        
        if [ -f "$service/target/$service-*.jar" ]; then
            print_info "Starting $service on port $port..."
            
            # Start service in background
            java -jar "$service/target/$service-*.jar" \
                --server.port="$port" \
                --spring.profiles.active=dev \
                > "logs/$service.log" 2>&1 &
            
            # Save PID
            echo $! > "logs/$service.pid"
            
            # Wait for service to start
            sleep 5
            wait_for_service localhost "$port" "$service"
        else
            print_warning "$service JAR not found. Skipping."
        fi
    done
}

# Function to stop all services
stop_services() {
    print_info "Stopping all services..."
    
    # Stop microservices
    for pid_file in logs/*.pid; do
        if [ -f "$pid_file" ]; then
            PID=$(cat "$pid_file")
            if kill -0 "$PID" 2>/dev/null; then
                kill "$PID"
                print_info "Stopped process $PID"
            fi
            rm "$pid_file"
        fi
    done
    
    # Stop Docker containers
    if command_exists docker-compose; then
        docker-compose down
        print_success "All services stopped"
    fi
}

# Function to show service status
show_status() {
    echo
    echo "======================================"
    echo "Service Status"
    echo "======================================"
    
    # Check infrastructure services
    print_info "Infrastructure Services:"
    docker-compose ps
    
    echo
    print_info "Microservices:"
    
    # Check microservices
    services=(
        "Eureka Server:${EUREKA_PORT:-8761}"
        "Config Server:${CONFIG_SERVER_PORT:-8888}"
        "API Gateway:${API_GATEWAY_PORT:-8080}"
        "Auth Service:${AUTH_SERVICE_PORT:-8081}"
        "User Service:${USER_SERVICE_PORT:-8082}"
        "Product Service:${PRODUCT_SERVICE_PORT:-8083}"
        "Order Service:${ORDER_SERVICE_PORT:-8084}"
        "Notification Service:${NOTIFICATION_SERVICE_PORT:-8085}"
    )
    
    for service_info in "${services[@]}"; do
        IFS=':' read -r service port <<< "$service_info"
        if nc -z localhost "$port" 2>/dev/null; then
            echo -e "  ${GREEN}✓${NC} $service (port $port)"
        else
            echo -e "  ${RED}✗${NC} $service (port $port)"
        fi
    done
    
    echo
    echo "======================================"
    echo "Access URLs:"
    echo "======================================"
    echo "API Gateway:        http://localhost:${API_GATEWAY_PORT:-8080}"
    echo "Eureka Dashboard:   http://localhost:${EUREKA_PORT:-8761}"
    echo "Config Server:      http://localhost:${CONFIG_SERVER_PORT:-8888}"
    echo "RabbitMQ Console:   http://localhost:15672 (admin/adminpassword)"
    echo "Kibana:            http://localhost:5601"
    echo "Prometheus:        http://localhost:9090"
    echo "Grafana:           http://localhost:3000 (admin/admin)"
    echo "Zipkin:            http://localhost:9411"
    echo "======================================"
}

# Function to show logs
show_logs() {
    local service=$1
    
    if [ -z "$service" ]; then
        print_info "Available logs:"
        ls -1 logs/*.log 2>/dev/null | sed 's|logs/||g' | sed 's|.log||g'
    else
        if [ -f "logs/$service.log" ]; then
            tail -f "logs/$service.log"
        else
            print_error "Log file for $service not found"
        fi
    fi
}

# Main function
main() {
    local command=${1:-"start"}
    
    echo "=========================================="
    echo "Local Stack Management"
    echo "=========================================="
    echo
    
    # Check prerequisites
    check_docker
    check_docker_compose
    
    # Load environment
    load_env
    
    # Create logs directory
    mkdir -p logs
    
    case "$command" in
        start)
            start_infrastructure
            build_services
            start_services
            show_status
            ;;
        stop)
            stop_services
            ;;
        restart)
            stop_services
            sleep 5
            start_infrastructure
            start_services
            show_status
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs "$2"
            ;;
        build)
            build_services
            ;;
        infrastructure)
            start_infrastructure
            ;;
        *)
            echo "Usage: $0 {start|stop|restart|status|logs|build|infrastructure}"
            echo
            echo "Commands:"
            echo "  start         - Start all services (infrastructure + microservices)"
            echo "  stop          - Stop all services"
            echo "  restart       - Restart all services"
            echo "  status        - Show status of all services"
            echo "  logs [service] - Show logs for a specific service"
            echo "  build         - Build microservices only"
            echo "  infrastructure - Start infrastructure services only"
            exit 1
            ;;
    esac
}

# Handle Ctrl+C
trap 'echo -e "\n${YELLOW}Interrupted. Run \"$0 stop\" to stop all services.${NC}"; exit 1' INT

# Run main function
main "$@"