# Smart Recipe Meal Planner - Backend API

Backend REST API for the Smart Recipe Meal Planner with Nutritional Analysis application.

## Tech Stack

- **Framework:** Spring Boot 3.2.5 with Java 17
- **Database:** PostgreSQL (local), AWS RDS PostgreSQL (production)
- **Authentication:** JWT with Spring Security 6
- **Documentation:** SpringDoc OpenAPI / Swagger UI
- **ML Library:** Apache Commons Math 3 (SimpleRegression)
- **Build Tool:** Apache Maven

## Project Structure

```
src/main/java/com/mealplanner/api/
    config/         Security, CORS, JWT, OpenAPI configuration
    controller/     REST API controllers for all entities
    dto/            Request/Response DTOs with validation
    model/          JPA entities and enums
    repository/     Spring Data JPA repositories
    service/        Business logic and ML forecasting
    exception/      Global exception handler
```

## Entities

- **User** - Registration, login, profile management
- **Recipe** - CRUD with ingredients, nutritional calculations
- **Ingredient** - Nutritional data per 100g
- **MealPlan** - Weekly meal schedules with entries
- **UserDietaryProfile** - Dietary goals, allergies, restrictions
- **ShoppingList** - Auto-generated from meal plans

## API Endpoints

### Authentication (Public)
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Authenticate user

### Recipes (Protected)
- `GET /api/recipes` - List all recipes
- `GET /api/recipes/{id}` - Get recipe by ID
- `POST /api/recipes` - Create recipe
- `PUT /api/recipes/{id}` - Update recipe
- `DELETE /api/recipes/{id}` - Delete recipe
- `GET /api/recipes/search?keyword=` - Search recipes
- `GET /api/recipes/difficulty/{level}` - Filter by difficulty

### Ingredients (Protected)
- Full CRUD at `/api/ingredients`
- `GET /api/ingredients/search?keyword=` - Search ingredients

### Meal Plans (Protected)
- Full CRUD at `/api/meal-plans`

### Dietary Profiles (Protected)
- Full CRUD at `/api/dietary-profiles`

### Shopping Lists (Protected)
- Full CRUD at `/api/shopping-lists`
- `PATCH /api/shopping-lists/items/{id}/toggle` - Toggle item

### Dashboard and Forecast (Protected)
- `GET /api/dashboard` - Dashboard summary data
- `GET /api/forecast` - Nutritional trend forecast

## Running Locally

### Prerequisites
- Java 17
- Maven 3.8+
- PostgreSQL 15+

### Setup
1. Create PostgreSQL database: `CREATE DATABASE mealplanner;`
2. Update `application.properties` with your database credentials
3. Run: `mvn spring-boot:run`
4. Access Swagger UI: http://localhost:8080/swagger-ui.html

## Static Analysis

Run all analysis tools:
```
mvn verify
```

Analysis tools configured:
- SpotBugs with FindSecBugs (bug and security detection)
- PMD (code style and complexity)
- JaCoCo (code coverage, minimum 60%)

## CI/CD Pipeline

GitHub Actions workflow at `.github/workflows/ci-cd.yml`:
- CI: Build, test, static analysis, Trivy security scan
- CD: Deploy to AWS EC2 (on push to main only)

## Infrastructure

Terraform configuration in `terraform/` for AWS deployment:
- EC2 t2.micro for backend API
- RDS PostgreSQL for database
- S3 for frontend static hosting
- VPC with public and private subnets
