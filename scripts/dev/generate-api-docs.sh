#!/bin/bash

# Script to generate API documentation for all microservices
# Supports OpenAPI/Swagger documentation generation

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

# Function to generate OpenAPI spec for a service
generate_openapi_spec() {
    local service_name=$1
    local service_port=$2
    local output_dir="docs/api/$service_name"
    
    print_info "Generating OpenAPI spec for $service_name..."
    
    # Create output directory
    mkdir -p "$output_dir"
    
    # Check if service is running
    if nc -z localhost "$service_port" 2>/dev/null; then
        # Try to fetch OpenAPI spec from running service
        if curl -s "http://localhost:$service_port/v3/api-docs" > "$output_dir/openapi.json" 2>/dev/null; then
            print_success "Downloaded OpenAPI spec for $service_name"
            
            # Convert to YAML if possible
            if command_exists yq; then
                yq eval -P "$output_dir/openapi.json" > "$output_dir/openapi.yaml"
                print_success "Converted to YAML format"
            fi
        else
            print_warning "Could not fetch OpenAPI spec from $service_name"
        fi
    else
        print_warning "$service_name is not running on port $service_port"
    fi
}

# Function to generate HTML documentation from OpenAPI spec
generate_html_docs() {
    local service_name=$1
    local spec_file="docs/api/$service_name/openapi.json"
    local output_file="docs/api/$service_name/index.html"
    
    if [ ! -f "$spec_file" ]; then
        print_warning "OpenAPI spec not found for $service_name"
        return
    fi
    
    print_info "Generating HTML documentation for $service_name..."
    
    # Create HTML with Swagger UI
    cat > "$output_file" << EOF
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>$service_name API Documentation</title>
    <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@4.5.0/swagger-ui.css">
    <style>
        body {
            margin: 0;
            padding: 0;
        }
        .swagger-ui .topbar {
            display: none;
        }
    </style>
</head>
<body>
    <div id="swagger-ui"></div>
    <script src="https://unpkg.com/swagger-ui-dist@4.5.0/swagger-ui-bundle.js"></script>
    <script src="https://unpkg.com/swagger-ui-dist@4.5.0/swagger-ui-standalone-preset.js"></script>
    <script>
        window.onload = function() {
            const ui = SwaggerUIBundle({
                url: "./openapi.json",
                dom_id: '#swagger-ui',
                deepLinking: true,
                presets: [
                    SwaggerUIBundle.presets.apis,
                    SwaggerUIStandalonePreset
                ],
                plugins: [
                    SwaggerUIBundle.plugins.DownloadUrl
                ],
                layout: "StandaloneLayout"
            });
            window.ui = ui;
        }
    </script>
</body>
</html>
EOF
    
    print_success "Generated HTML documentation for $service_name"
}

# Function to generate Postman collection
generate_postman_collection() {
    local service_name=$1
    local spec_file="docs/api/$service_name/openapi.json"
    local output_file="docs/api/$service_name/postman_collection.json"
    
    if [ ! -f "$spec_file" ]; then
        return
    fi
    
    print_info "Generating Postman collection for $service_name..."
    
    # Check if openapi-to-postman is available
    if command_exists openapi2postman; then
        openapi2postman -s "$spec_file" -o "$output_file"
        print_success "Generated Postman collection for $service_name"
    else
        print_warning "openapi2postman not found. Install with: npm install -g openapi-to-postman"
    fi
}

# Function to generate AsyncAPI documentation
generate_asyncapi_docs() {
    local service_name=$1
    local async_spec="$service_name/src/main/resources/asyncapi.yaml"
    local output_dir="docs/api/$service_name/async"
    
    if [ ! -f "$async_spec" ]; then
        return
    fi
    
    print_info "Generating AsyncAPI documentation for $service_name..."
    mkdir -p "$output_dir"
    
    # Copy spec file
    cp "$async_spec" "$output_dir/"
    
    # Generate HTML if asyncapi generator is available
    if command_exists asyncapi; then
        asyncapi generate html "$async_spec" -o "$output_dir/index.html"
        print_success "Generated AsyncAPI documentation for $service_name"
    else
        print_warning "AsyncAPI CLI not found. Install with: npm install -g @asyncapi/cli"
    fi
}

# Function to generate API index page
generate_index_page() {
    local index_file="docs/api/index.html"
    
    print_info "Generating API documentation index..."
    
    cat > "$index_file" << 'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Microservices API Documentation</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
            background-color: #f5f5f5;
        }
        h1 {
            color: #2c3e50;
            border-bottom: 3px solid #3498db;
            padding-bottom: 10px;
        }
        .service-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
            gap: 20px;
            margin-top: 30px;
        }
        .service-card {
            background: white;
            border-radius: 8px;
            padding: 20px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            transition: transform 0.2s, box-shadow 0.2s;
        }
        .service-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 8px rgba(0,0,0,0.15);
        }
        .service-card h2 {
            margin-top: 0;
            color: #2c3e50;
        }
        .service-card p {
            color: #666;
            margin: 10px 0;
        }
        .links {
            margin-top: 15px;
        }
        .links a {
            display: inline-block;
            margin-right: 15px;
            color: #3498db;
            text-decoration: none;
            font-weight: 500;
        }
        .links a:hover {
            text-decoration: underline;
        }
        .status {
            display: inline-block;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: bold;
        }
        .status.online {
            background-color: #2ecc71;
            color: white;
        }
        .status.offline {
            background-color: #e74c3c;
            color: white;
        }
        .generated-date {
            text-align: center;
            color: #999;
            margin-top: 40px;
            font-size: 14px;
        }
    </style>
</head>
<body>
    <h1>Microservices API Documentation</h1>
    
    <div class="service-grid">
        <div class="service-card">
            <h2>API Gateway</h2>
            <p>Central entry point for all microservices</p>
            <p>Port: 8080</p>
            <div class="links">
                <a href="api-gateway/index.html">OpenAPI Docs</a>
                <a href="api-gateway/openapi.json">OpenAPI Spec</a>
                <a href="api-gateway/postman_collection.json">Postman Collection</a>
            </div>
        </div>
        
        <div class="service-card">
            <h2>Auth Service</h2>
            <p>Authentication and authorization service</p>
            <p>Port: 8081</p>
            <div class="links">
                <a href="auth-service/index.html">OpenAPI Docs</a>
                <a href="auth-service/openapi.json">OpenAPI Spec</a>
                <a href="auth-service/postman_collection.json">Postman Collection</a>
            </div>
        </div>
        
        <div class="service-card">
            <h2>User Service</h2>
            <p>User management and profiles</p>
            <p>Port: 8082</p>
            <div class="links">
                <a href="user-service/index.html">OpenAPI Docs</a>
                <a href="user-service/openapi.json">OpenAPI Spec</a>
                <a href="user-service/postman_collection.json">Postman Collection</a>
            </div>
        </div>
        
        <div class="service-card">
            <h2>Product Service</h2>
            <p>Product catalog and inventory management</p>
            <p>Port: 8083</p>
            <div class="links">
                <a href="product-service/index.html">OpenAPI Docs</a>
                <a href="product-service/openapi.json">OpenAPI Spec</a>
                <a href="product-service/postman_collection.json">Postman Collection</a>
            </div>
        </div>
        
        <div class="service-card">
            <h2>Order Service</h2>
            <p>Order processing and management</p>
            <p>Port: 8084</p>
            <div class="links">
                <a href="order-service/index.html">OpenAPI Docs</a>
                <a href="order-service/openapi.json">OpenAPI Spec</a>
                <a href="order-service/postman_collection.json">Postman Collection</a>
            </div>
        </div>
        
        <div class="service-card">
            <h2>Notification Service</h2>
            <p>Email, SMS, and push notifications</p>
            <p>Port: 8085</p>
            <div class="links">
                <a href="notification-service/index.html">OpenAPI Docs</a>
                <a href="notification-service/openapi.json">OpenAPI Spec</a>
                <a href="notification-service/postman_collection.json">Postman Collection</a>
                <a href="notification-service/async/index.html">AsyncAPI Docs</a>
            </div>
        </div>
    </div>
    
    <div class="generated-date">
        Generated on: <span id="date"></span>
    </div>
    
    <script>
        document.getElementById('date').textContent = new Date().toLocaleString();
    </script>
</body>
</html>
EOF
    
    print_success "Generated API documentation index"
}

# Function to generate README for API docs
generate_api_readme() {
    local readme_file="docs/api/README.md"
    
    cat > "$readme_file" << 'EOF'
# API Documentation

This directory contains the API documentation for all microservices.

## Available Services

- **API Gateway** (Port 8080): Central entry point for all microservices
- **Auth Service** (Port 8081): Authentication and authorization
- **User Service** (Port 8082): User management
- **Product Service** (Port 8083): Product catalog
- **Order Service** (Port 8084): Order processing
- **Notification Service** (Port 8085): Notifications

## Documentation Formats

Each service documentation includes:
- **OpenAPI Specification** (JSON/YAML)
- **Interactive HTML Documentation** (Swagger UI)
- **Postman Collection** (for API testing)
- **AsyncAPI Documentation** (for event-driven APIs)

## Viewing Documentation

### Local Development
1. Open `docs/api/index.html` in a web browser
2. Click on any service to view its documentation

### With HTTP Server
```bash
# Using Python
python -m http.server 8000 --directory docs/api

# Using Node.js
npx http-server docs/api -p 8000

# Using PHP
php -S localhost:8000 -t docs/api
```

Then visit http://localhost:8000

## Generating Documentation

Run the generation script:
```bash
./scripts/dev/generate-api-docs.sh
```

Options:
- `--service <name>`: Generate docs for specific service
- `--format <format>`: Generate specific format (openapi, html, postman)
- `--all`: Generate all formats for all services

## API Testing

### Using Postman
1. Import the Postman collection from `docs/api/<service>/postman_collection.json`
2. Set up environment variables for base URLs
3. Run requests against local or deployed services

### Using curl
Example requests are included in each service's documentation.

## API Versioning

All APIs follow semantic versioning:
- `/v1/*` - Version 1 endpoints
- `/v2/*` - Version 2 endpoints (when available)

## Authentication

Most endpoints require authentication. See the Auth Service documentation for:
- JWT token generation
- OAuth2 flows
- API key management

## Rate Limiting

API Gateway implements rate limiting:
- 100 requests per minute for anonymous users
- 1000 requests per minute for authenticated users

## Support

For API-related questions:
- Check service-specific documentation
- Review example requests in Postman collections
- Contact the development team
EOF
    
    print_success "Generated API documentation README"
}

# Main function
main() {
    echo "=========================================="
    echo "API Documentation Generator"
    echo "=========================================="
    echo
    
    # Parse command line arguments
    SERVICE=""
    FORMAT=""
    ALL=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --service)
                SERVICE="$2"
                shift 2
                ;;
            --format)
                FORMAT="$2"
                shift 2
                ;;
            --all)
                ALL=true
                shift
                ;;
            *)
                print_error "Unknown option: $1"
                echo "Usage: $0 [--service <name>] [--format <format>] [--all]"
                exit 1
                ;;
        esac
    done
    
    # Create documentation directory
    mkdir -p docs/api
    
    # Define services
    declare -A services=(
        ["api-gateway"]=8080
        ["auth-service"]=8081
        ["user-service"]=8082
        ["product-service"]=8083
        ["order-service"]=8084
        ["notification-service"]=8085
    )
    
    # Generate documentation
    if [ ! -z "$SERVICE" ]; then
        # Generate for specific service
        if [ ${services[$SERVICE]+_} ]; then
            generate_openapi_spec "$SERVICE" "${services[$SERVICE]}"
            generate_html_docs "$SERVICE"
            generate_postman_collection "$SERVICE"
            generate_asyncapi_docs "$SERVICE"
        else
            print_error "Unknown service: $SERVICE"
            exit 1
        fi
    else
        # Generate for all services
        for service in "${!services[@]}"; do
            generate_openapi_spec "$service" "${services[$service]}"
            generate_html_docs "$service"
            generate_postman_collection "$service"
            generate_asyncapi_docs "$service"
        done
    fi
    
    # Generate index page and README
    generate_index_page
    generate_api_readme
    
    # Open documentation in browser
    if command_exists open; then
        print_info "Opening documentation in browser..."
        open "docs/api/index.html"
    elif command_exists xdg-open; then
        print_info "Opening documentation in browser..."
        xdg-open "docs/api/index.html"
    fi
    
    echo
    print_success "API documentation generated successfully!"
    print_info "Documentation available at: docs/api/index.html"
}

# Run main function
main "$@"