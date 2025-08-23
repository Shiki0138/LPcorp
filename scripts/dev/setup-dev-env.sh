#!/bin/bash

# Development Environment Setup Script
# This script sets up the complete development environment for the microservices platform

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

# Function to detect OS
detect_os() {
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        OS="linux"
        if [ -f /etc/debian_version ]; then
            DISTRO="debian"
        elif [ -f /etc/redhat-release ]; then
            DISTRO="redhat"
        else
            DISTRO="unknown"
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        OS="macos"
    elif [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
        OS="windows"
    else
        OS="unknown"
    fi
}

# Function to check Java version
check_java_version() {
    if command_exists java; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge 17 ]; then
            print_success "Java $JAVA_VERSION detected"
            return 0
        else
            print_warning "Java $JAVA_VERSION detected, but Java 17+ is required"
            return 1
        fi
    else
        print_error "Java not found"
        return 1
    fi
}

# Main setup function
main() {
    echo "=========================================="
    echo "Microservices Platform Development Setup"
    echo "=========================================="
    echo

    # Detect OS
    detect_os
    print_info "Detected OS: $OS"
    
    # Check for required tools
    print_info "Checking required tools..."
    
    # Check Java
    if ! check_java_version; then
        print_error "Please install Java 17 or higher"
        exit 1
    fi
    
    # Check Git
    if ! command_exists git; then
        print_error "Git is not installed. Please install Git first."
        exit 1
    else
        print_success "Git detected: $(git --version)"
    fi
    
    # Check Docker
    if ! command_exists docker; then
        print_warning "Docker is not installed. Docker is required for running services."
        INSTALL_DOCKER=true
    else
        print_success "Docker detected: $(docker --version)"
    fi
    
    # Check Docker Compose
    if ! command_exists docker-compose; then
        print_warning "Docker Compose is not installed."
        INSTALL_DOCKER_COMPOSE=true
    else
        print_success "Docker Compose detected: $(docker-compose --version)"
    fi
    
    # Check Maven
    if ! command_exists mvn; then
        print_info "Maven not found. Installing Maven wrapper..."
        if [ ! -f mvnw ]; then
            curl -s https://repo.maven.apache.org/maven2/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar -o .mvn/wrapper/maven-wrapper.jar
            curl -s https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw -o mvnw
            curl -s https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw.cmd -o mvnw.cmd
            chmod +x mvnw
        fi
        print_success "Maven wrapper installed"
    else
        print_success "Maven detected: $(mvn --version | head -1)"
    fi
    
    # Install development tools based on OS
    if [ "$INSTALL_DOCKER" = true ] || [ "$INSTALL_DOCKER_COMPOSE" = true ]; then
        print_info "Installing missing tools..."
        
        if [ "$OS" = "macos" ]; then
            if command_exists brew; then
                [ "$INSTALL_DOCKER" = true ] && brew install --cask docker
            else
                print_error "Homebrew not found. Please install Docker Desktop manually."
            fi
        elif [ "$OS" = "linux" ]; then
            if [ "$DISTRO" = "debian" ]; then
                print_info "Run: sudo apt-get update && sudo apt-get install docker.io docker-compose"
            elif [ "$DISTRO" = "redhat" ]; then
                print_info "Run: sudo yum install docker docker-compose"
            fi
        else
            print_info "Please install Docker Desktop from https://www.docker.com/products/docker-desktop"
        fi
    fi
    
    # Setup Git hooks
    print_info "Setting up Git hooks..."
    if [ -d .git ]; then
        cp hooks/pre-commit .git/hooks/pre-commit 2>/dev/null || true
        cp hooks/pre-push .git/hooks/pre-push 2>/dev/null || true
        chmod +x .git/hooks/pre-commit 2>/dev/null || true
        chmod +x .git/hooks/pre-push 2>/dev/null || true
        print_success "Git hooks installed"
    else
        print_warning "Not a Git repository. Skipping Git hooks setup."
    fi
    
    # Create necessary directories
    print_info "Creating project directories..."
    mkdir -p logs
    mkdir -p data/mysql
    mkdir -p data/mongodb
    mkdir -p data/redis
    mkdir -p data/elasticsearch
    mkdir -p data/prometheus
    mkdir -p data/grafana
    print_success "Directories created"
    
    # Download dependencies
    print_info "Downloading dependencies..."
    if [ -f pom.xml ]; then
        if command_exists mvn; then
            mvn dependency:go-offline -DskipTests || true
        else
            ./mvnw dependency:go-offline -DskipTests || true
        fi
    fi
    
    # Setup environment files
    print_info "Setting up environment files..."
    if [ ! -f .env ]; then
        cat > .env << EOF
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=microservices
DB_USER=root
DB_PASSWORD=rootpassword

# MongoDB Configuration
MONGO_HOST=localhost
MONGO_PORT=27017
MONGO_DB=microservices
MONGO_USER=admin
MONGO_PASSWORD=adminpassword

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redispassword

# RabbitMQ Configuration
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=admin
RABBITMQ_PASSWORD=adminpassword

# Elasticsearch Configuration
ES_HOST=localhost
ES_PORT=9200

# JWT Configuration
JWT_SECRET=your-secret-key-here
JWT_EXPIRATION=86400

# Service Ports
API_GATEWAY_PORT=8080
EUREKA_PORT=8761
CONFIG_SERVER_PORT=8888
AUTH_SERVICE_PORT=8081
USER_SERVICE_PORT=8082
PRODUCT_SERVICE_PORT=8083
ORDER_SERVICE_PORT=8084
NOTIFICATION_SERVICE_PORT=8085
EOF
        print_success "Environment file created"
        print_warning "Please update .env file with your specific configuration"
    else
        print_info "Environment file already exists"
    fi
    
    # Install additional development tools
    print_info "Checking additional development tools..."
    
    # Check for code quality tools
    TOOLS_TO_INSTALL=""
    
    if ! command_exists checkstyle; then
        TOOLS_TO_INSTALL="$TOOLS_TO_INSTALL checkstyle"
    fi
    
    if ! command_exists pmd; then
        TOOLS_TO_INSTALL="$TOOLS_TO_INSTALL pmd"
    fi
    
    if [ ! -z "$TOOLS_TO_INSTALL" ]; then
        print_info "Missing tools: $TOOLS_TO_INSTALL"
        if [ "$OS" = "macos" ] && command_exists brew; then
            print_info "You can install them with: brew install $TOOLS_TO_INSTALL"
        else
            print_info "Please install the missing tools manually"
        fi
    fi
    
    # Setup IDE configurations
    print_info "Setting up IDE configurations..."
    if [ ! -d .idea ] && command_exists idea; then
        print_info "IntelliJ IDEA detected. Run 'idea .' to open the project."
    fi
    
    if [ ! -d .vscode ] && command_exists code; then
        print_info "VS Code detected. Run 'code .' to open the project."
    fi
    
    # Final checks
    print_info "Running final checks..."
    
    # Check if Docker daemon is running
    if command_exists docker; then
        if docker info >/dev/null 2>&1; then
            print_success "Docker daemon is running"
        else
            print_warning "Docker daemon is not running. Please start Docker."
        fi
    fi
    
    # Setup completion message
    echo
    echo "=========================================="
    echo "Development Environment Setup Complete!"
    echo "=========================================="
    echo
    print_success "Your development environment is ready!"
    echo
    echo "Next steps:"
    echo "1. Update the .env file with your configuration"
    echo "2. Run './scripts/dev/run-local-stack.sh' to start all services"
    echo "3. Run 'make build' to build the project"
    echo "4. Access services at:"
    echo "   - API Gateway: http://localhost:8080"
    echo "   - Eureka Dashboard: http://localhost:8761"
    echo "   - Config Server: http://localhost:8888"
    echo
    print_info "Run 'make help' to see all available commands"
}

# Run main function
main "$@"