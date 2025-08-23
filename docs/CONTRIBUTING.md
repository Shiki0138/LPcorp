# Contributing to Microservices Platform

Thank you for your interest in contributing to the Microservices Platform! This document provides guidelines and information for contributors.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Pull Request Process](#pull-request-process)
- [Issue Reporting](#issue-reporting)
- [Documentation](#documentation)

## Code of Conduct

This project adheres to a code of conduct that we expect all contributors to follow. Please read and follow our [Code of Conduct](CODE_OF_CONDUCT.md).

## Getting Started

### Prerequisites

Before contributing, ensure you have:
- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose
- Git
- Your favorite IDE (IntelliJ IDEA or VS Code recommended)

### Development Environment Setup

1. **Fork and clone the repository**
   ```bash
   git clone https://github.com/your-username/microservices-platform.git
   cd microservices-platform
   ```

2. **Set up the development environment**
   ```bash
   make setup
   ```

3. **Install dependencies**
   ```bash
   make install
   ```

4. **Start infrastructure services**
   ```bash
   make run-infra
   ```

5. **Build and run services**
   ```bash
   make build
   make run
   ```

6. **Verify everything is working**
   ```bash
   make health
   ```

## Development Workflow

### Branch Naming Convention

Use descriptive branch names following this pattern:
- `feature/<feature-name>` - for new features
- `bugfix/<bug-description>` - for bug fixes
- `hotfix/<critical-fix>` - for critical fixes
- `refactor/<refactor-description>` - for refactoring
- `docs/<doc-update>` - for documentation updates

Examples:
- `feature/user-authentication`
- `bugfix/order-calculation-error`
- `refactor/payment-service-cleanup`

### Commit Message Convention

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `chore`: Maintenance tasks
- `ci`: CI/CD changes
- `build`: Build system changes

**Examples:**
```
feat(user-service): add user profile management

fix(order-service): resolve order total calculation bug

docs(api): update authentication endpoint documentation

refactor(payment-service): simplify payment processing logic
```

### Git Hooks

The project includes pre-commit and pre-push hooks that automatically run:
- Code formatting checks
- Linting
- Unit tests
- Static analysis

Install the hooks:
```bash
make install-hooks
```

## Coding Standards

### Java Coding Style

We follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) with some modifications:

- **Line length**: 120 characters
- **Indentation**: 4 spaces (no tabs)
- **Package naming**: `com.microservices.<service-name>`
- **Class naming**: PascalCase
- **Method naming**: camelCase
- **Constants**: UPPER_SNAKE_CASE

### Code Quality Tools

The project uses several code quality tools:
- **Checkstyle**: Code style enforcement
- **PMD**: Static analysis
- **SpotBugs**: Bug detection
- **SonarQube**: Code quality analysis

Run quality checks:
```bash
make quality
```

### Architecture Guidelines

#### Service Design Principles

1. **Single Responsibility**: Each service should have a single, well-defined responsibility
2. **Loose Coupling**: Services should be loosely coupled and highly cohesive
3. **Database per Service**: Each service should have its own database
4. **API First**: Design APIs before implementation
5. **Stateless**: Services should be stateless where possible

#### Package Structure

```
src/main/java/com/microservices/{service-name}/
├── controller/          # REST controllers
├── service/            # Business logic
├── repository/         # Data access layer
├── model/             # Domain entities
├── dto/               # Data transfer objects
├── config/            # Configuration classes
├── exception/         # Custom exceptions
└── util/              # Utility classes
```

#### Naming Conventions

- **Controllers**: `{Entity}Controller`
- **Services**: `{Entity}Service` and `{Entity}ServiceImpl`
- **Repositories**: `{Entity}Repository`
- **DTOs**: `{Entity}Dto` or `{Entity}Request`/`{Entity}Response`
- **Entities**: `{Entity}` (no suffix)

### API Design Guidelines

#### RESTful API Design

1. **Use proper HTTP methods**
   - `GET`: Retrieve data
   - `POST`: Create new resources
   - `PUT`: Update entire resources
   - `PATCH`: Partial updates
   - `DELETE`: Remove resources

2. **URL naming conventions**
   ```
   GET    /api/v1/users          # Get all users
   GET    /api/v1/users/{id}     # Get user by ID
   POST   /api/v1/users          # Create new user
   PUT    /api/v1/users/{id}     # Update user
   PATCH  /api/v1/users/{id}     # Partial update
   DELETE /api/v1/users/{id}     # Delete user
   ```

3. **Response format**
   ```json
   {
     "data": {},
     "message": "Success",
     "timestamp": "2023-12-01T10:30:00Z",
     "status": 200
   }
   ```

4. **Error handling**
   ```json
   {
     "error": {
       "code": "USER_NOT_FOUND",
       "message": "User with ID 123 not found",
       "details": []
     },
     "timestamp": "2023-12-01T10:30:00Z",
     "status": 404
   }
   ```

#### OpenAPI Documentation

- Document all APIs using OpenAPI/Swagger annotations
- Provide examples for request/response bodies
- Include proper descriptions and error codes
- Use meaningful operation IDs

## Testing Guidelines

### Test Pyramid

Follow the test pyramid approach:
- **Unit Tests (70%)**: Fast, isolated tests for individual components
- **Integration Tests (20%)**: Test component interactions
- **End-to-End Tests (10%)**: Full system tests

### Testing Frameworks

- **JUnit 5**: Unit testing framework
- **Mockito**: Mocking framework
- **TestContainers**: Integration testing with Docker
- **Spring Boot Test**: Spring-specific testing utilities

### Test Naming Convention

```java
@Test
void shouldReturnUserWhenValidIdProvided() {
    // Test implementation
}

@Test
void shouldThrowExceptionWhenUserNotFound() {
    // Test implementation
}
```

### Test Structure

Use the **Arrange-Act-Assert** pattern:

```java
@Test
void shouldCalculateOrderTotalCorrectly() {
    // Arrange
    Order order = Order.builder()
        .addItem(new OrderItem("item1", 10.0, 2))
        .addItem(new OrderItem("item2", 15.0, 1))
        .build();

    // Act
    BigDecimal total = orderService.calculateTotal(order);

    // Assert
    assertThat(total).isEqualTo(new BigDecimal("35.0"));
}
```

### Test Coverage

- Maintain minimum 80% code coverage
- Focus on testing business logic
- Don't test trivial getters/setters
- Test edge cases and error conditions

Run tests and coverage:
```bash
make test-coverage
```

## Pull Request Process

### Before Creating a Pull Request

1. **Update your branch**
   ```bash
   git checkout main
   git pull upstream main
   git checkout your-feature-branch
   git rebase main
   ```

2. **Run all checks locally**
   ```bash
   make quality
   make test
   ```

3. **Update documentation** if necessary

4. **Add/update tests** for your changes

### Pull Request Template

When creating a PR, use this template:

```markdown
## Description
Brief description of the changes.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## How Has This Been Tested?
- [ ] Unit tests
- [ ] Integration tests
- [ ] Manual testing

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Code is commented where needed
- [ ] Documentation updated
- [ ] Tests added/updated
- [ ] All tests pass
```

### Review Process

1. **Automated Checks**: All CI checks must pass
2. **Code Review**: At least two approvals required
3. **Testing**: Manual testing if needed
4. **Documentation**: Ensure docs are updated

### Merge Strategy

- Use **squash and merge** for feature branches
- Use **merge commit** for release branches
- Delete feature branches after merging

## Issue Reporting

### Bug Reports

Use the bug report template and include:
- **Description**: Clear description of the issue
- **Steps to Reproduce**: Detailed steps
- **Expected Behavior**: What should happen
- **Actual Behavior**: What actually happens
- **Environment**: OS, Java version, etc.
- **Screenshots/Logs**: If applicable

### Feature Requests

Include:
- **Problem Description**: What problem does this solve?
- **Proposed Solution**: How should it work?
- **Alternatives**: Other solutions considered
- **Additional Context**: Screenshots, mockups, etc.

### Security Issues

For security vulnerabilities:
1. **Don't** create a public issue
2. Email security@microservices-platform.com
3. Include detailed reproduction steps
4. Allow time for patch development

## Documentation

### Types of Documentation

1. **Code Comments**: For complex logic
2. **API Documentation**: OpenAPI/Swagger specs
3. **README files**: For each service
4. **Architecture Documentation**: Design decisions
5. **User Documentation**: End-user guides

### Documentation Standards

- Write clear, concise documentation
- Use markdown for text documentation
- Include diagrams using Mermaid or PlantUML
- Keep documentation up to date with code changes
- Use proper grammar and spelling

### Generating Documentation

```bash
make docs          # Generate API docs
make javadoc      # Generate Javadoc
```

## Code Review Guidelines

### For Authors

- Keep PRs small and focused
- Provide clear descriptions
- Respond to feedback promptly
- Make requested changes
- Rebase instead of merge commits

### For Reviewers

- Be constructive and respectful
- Focus on code quality and maintainability
- Check for security issues
- Verify tests are adequate
- Approve when satisfied

## Release Process

### Versioning

We use [Semantic Versioning](https://semver.org/):
- **Major** (X.0.0): Breaking changes
- **Minor** (X.Y.0): New features (backward compatible)
- **Patch** (X.Y.Z): Bug fixes (backward compatible)

### Release Steps

1. Create release branch from `main`
2. Update version numbers
3. Update CHANGELOG.md
4. Test thoroughly
5. Create release PR
6. Tag release after merge
7. Deploy to production

## Getting Help

### Communication Channels

- **GitHub Issues**: For bugs and features
- **Discussions**: For questions and ideas
- **Slack**: For real-time communication
- **Email**: For security issues

### Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Docker Documentation](https://docs.docker.com/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)

## Recognition

Contributors are recognized in:
- CONTRIBUTORS.md file
- Release notes
- Annual contributor report

Thank you for contributing to the Microservices Platform! Your contributions make this project better for everyone.