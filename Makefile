# Makefile for Microservices Platform

# Variables
MAVEN = ./mvnw
DOCKER = docker
DOCKER_COMPOSE = docker-compose
SERVICES = eureka-server config-server api-gateway auth-service user-service product-service order-service notification-service

# Colors
RED = \033[0;31m
GREEN = \033[0;32m
YELLOW = \033[0;33m
BLUE = \033[0;34m
NC = \033[0m # No Color

# Default target
.DEFAULT_GOAL := help

# Help target
.PHONY: help
help: ## Show this help message
	@echo "$(BLUE)Microservices Platform - Available Commands:$(NC)"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}'
	@echo ""
	@echo "$(YELLOW)Example usage:$(NC)"
	@echo "  make build"
	@echo "  make run"
	@echo "  make test"

# Setup targets
.PHONY: setup
setup: ## Set up development environment
	@echo "$(BLUE)Setting up development environment...$(NC)"
	@chmod +x scripts/dev/*.sh
	@./scripts/dev/setup-dev-env.sh

.PHONY: install
install: ## Install dependencies
	@echo "$(BLUE)Installing dependencies...$(NC)"
	$(MAVEN) clean install -DskipTests

# Build targets
.PHONY: build
build: ## Build all services
	@echo "$(BLUE)Building all services...$(NC)"
	$(MAVEN) clean package -DskipTests

.PHONY: build-docker
build-docker: ## Build Docker images for all services
	@echo "$(BLUE)Building Docker images...$(NC)"
	@for service in $(SERVICES); do \
		echo "$(YELLOW)Building $$service...$(NC)"; \
		$(DOCKER) build -t $$service:latest ./$$service || exit 1; \
	done

# Run targets
.PHONY: run
run: ## Run all services locally
	@echo "$(BLUE)Starting all services...$(NC)"
	@./scripts/dev/run-local-stack.sh start

.PHONY: run-infra
run-infra: ## Run infrastructure services only
	@echo "$(BLUE)Starting infrastructure services...$(NC)"
	$(DOCKER_COMPOSE) up -d

.PHONY: stop
stop: ## Stop all services
	@echo "$(BLUE)Stopping all services...$(NC)"
	@./scripts/dev/run-local-stack.sh stop

.PHONY: restart
restart: stop run ## Restart all services

# Test targets
.PHONY: test
test: ## Run all tests
	@echo "$(BLUE)Running all tests...$(NC)"
	$(MAVEN) test

.PHONY: test-unit
test-unit: ## Run unit tests only
	@echo "$(BLUE)Running unit tests...$(NC)"
	$(MAVEN) test -Dtest="*Test"

.PHONY: test-integration
test-integration: ## Run integration tests only
	@echo "$(BLUE)Running integration tests...$(NC)"
	$(MAVEN) test -Dtest="*IT"

.PHONY: test-coverage
test-coverage: ## Generate test coverage report
	@echo "$(BLUE)Generating test coverage report...$(NC)"
	$(MAVEN) clean test jacoco:report
	@echo "$(GREEN)Coverage reports generated in target/site/jacoco$(NC)"

# Quality targets
.PHONY: checkstyle
checkstyle: ## Run Checkstyle
	@echo "$(BLUE)Running Checkstyle...$(NC)"
	$(MAVEN) checkstyle:check

.PHONY: pmd
pmd: ## Run PMD
	@echo "$(BLUE)Running PMD...$(NC)"
	$(MAVEN) pmd:check

.PHONY: spotbugs
spotbugs: ## Run SpotBugs
	@echo "$(BLUE)Running SpotBugs...$(NC)"
	$(MAVEN) spotbugs:check

.PHONY: sonar
sonar: ## Run SonarQube analysis
	@echo "$(BLUE)Running SonarQube analysis...$(NC)"
	$(MAVEN) sonar:sonar

.PHONY: quality
quality: checkstyle pmd spotbugs ## Run all quality checks

# Documentation targets
.PHONY: docs
docs: ## Generate API documentation
	@echo "$(BLUE)Generating API documentation...$(NC)"
	@./scripts/dev/generate-api-docs.sh --all

.PHONY: javadoc
javadoc: ## Generate Javadoc
	@echo "$(BLUE)Generating Javadoc...$(NC)"
	$(MAVEN) javadoc:aggregate

# Service management
.PHONY: create-service
create-service: ## Create a new microservice (usage: make create-service name=<service-name> port=<port>)
	@if [ -z "$(name)" ]; then \
		echo "$(RED)Error: Service name is required. Usage: make create-service name=<service-name> port=<port>$(NC)"; \
		exit 1; \
	fi
	@if [ -z "$(port)" ]; then \
		echo "$(RED)Error: Port is required. Usage: make create-service name=<service-name> port=<port>$(NC)"; \
		exit 1; \
	fi
	@echo "$(BLUE)Creating new service: $(name) on port $(port)...$(NC)"
	@./scripts/dev/create-service.sh $(name) $(port)

# Deployment targets
.PHONY: deploy-local
deploy-local: build-docker ## Deploy to local Docker
	@echo "$(BLUE)Deploying to local Docker...$(NC)"
	$(DOCKER_COMPOSE) up -d

.PHONY: deploy-k8s
deploy-k8s: ## Deploy to Kubernetes
	@echo "$(BLUE)Deploying to Kubernetes...$(NC)"
	kubectl apply -f k8s/

# Database targets
.PHONY: db-migrate
db-migrate: ## Run database migrations
	@echo "$(BLUE)Running database migrations...$(NC)"
	$(MAVEN) flyway:migrate

.PHONY: db-clean
db-clean: ## Clean database
	@echo "$(YELLOW)WARNING: This will delete all data!$(NC)"
	@read -p "Are you sure? (y/N) " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		$(MAVEN) flyway:clean; \
	fi

# Monitoring targets
.PHONY: logs
logs: ## Show logs for all services
	@$(DOCKER_COMPOSE) logs -f

.PHONY: logs-service
logs-service: ## Show logs for specific service (usage: make logs-service name=<service-name>)
	@if [ -z "$(name)" ]; then \
		echo "$(RED)Error: Service name is required. Usage: make logs-service name=<service-name>$(NC)"; \
		exit 1; \
	fi
	@$(DOCKER_COMPOSE) logs -f $(name)

.PHONY: status
status: ## Show status of all services
	@./scripts/dev/run-local-stack.sh status

.PHONY: health
health: ## Check health of all services
	@echo "$(BLUE)Checking health of all services...$(NC)"
	@for port in 8761 8888 8080 8081 8082 8083 8084 8085; do \
		if curl -f http://localhost:$$port/actuator/health >/dev/null 2>&1; then \
			echo "$(GREEN)✓ Service on port $$port is healthy$(NC)"; \
		else \
			echo "$(RED)✗ Service on port $$port is not responding$(NC)"; \
		fi \
	done

# Cleanup targets
.PHONY: clean
clean: ## Clean build artifacts
	@echo "$(BLUE)Cleaning build artifacts...$(NC)"
	$(MAVEN) clean
	@rm -rf logs/*.log
	@rm -rf logs/*.pid

.PHONY: clean-docker
clean-docker: ## Clean Docker images and containers
	@echo "$(BLUE)Cleaning Docker resources...$(NC)"
	$(DOCKER_COMPOSE) down -v
	@$(DOCKER) system prune -f

.PHONY: clean-all
clean-all: clean clean-docker ## Clean everything

# Development shortcuts
.PHONY: dev
dev: run logs ## Start services and show logs

.PHONY: rebuild
rebuild: clean build ## Clean and build

.PHONY: refresh
refresh: clean build run ## Clean, build, and run

# Git hooks
.PHONY: install-hooks
install-hooks: ## Install Git hooks
	@echo "$(BLUE)Installing Git hooks...$(NC)"
	@cp hooks/pre-commit .git/hooks/pre-commit
	@cp hooks/pre-push .git/hooks/pre-push
	@chmod +x .git/hooks/pre-commit
	@chmod +x .git/hooks/pre-push
	@echo "$(GREEN)Git hooks installed successfully$(NC)"

# Version management
.PHONY: version
version: ## Show current version
	@grep -m1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/'

.PHONY: release
release: ## Create a new release (usage: make release version=<version>)
	@if [ -z "$(version)" ]; then \
		echo "$(RED)Error: Version is required. Usage: make release version=<version>$(NC)"; \
		exit 1; \
	fi
	@echo "$(BLUE)Creating release $(version)...$(NC)"
	$(MAVEN) versions:set -DnewVersion=$(version)
	$(MAVEN) versions:commit
	git add -A
	git commit -m "Release version $(version)"
	git tag -a v$(version) -m "Release version $(version)"
	@echo "$(GREEN)Release $(version) created. Don't forget to push tags!$(NC)"

.PHONY: check-updates
check-updates: ## Check for dependency updates
	@echo "$(BLUE)Checking for dependency updates...$(NC)"
	$(MAVEN) versions:display-dependency-updates

# Advanced targets
.PHONY: analyze
analyze: quality test-coverage sonar ## Run full analysis (quality + coverage + sonar)

.PHONY: benchmark
benchmark: ## Run performance benchmarks
	@echo "$(BLUE)Running performance benchmarks...$(NC)"
	$(MAVEN) jmh:benchmark

.PHONY: profile
profile: ## Run with profiling enabled
	@echo "$(BLUE)Running with profiling enabled...$(NC)"
	JAVA_OPTS="-XX:+UnlockCommercialFeatures -XX:+FlightRecorder" make run

# Troubleshooting
.PHONY: diagnose
diagnose: ## Run diagnostic checks
	@echo "$(BLUE)Running diagnostic checks...$(NC)"
	@echo "Java version:"
	@java -version
	@echo ""
	@echo "Maven version:"
	@$(MAVEN) --version
	@echo ""
	@echo "Docker version:"
	@$(DOCKER) --version
	@echo ""
	@echo "Docker Compose version:"
	@$(DOCKER_COMPOSE) --version
	@echo ""
	@echo "Services status:"
	@make status

# Catch-all target
%:
	@echo "$(RED)Error: Unknown target '$@'$(NC)"
	@echo "Run 'make help' to see available commands"
	@exit 1