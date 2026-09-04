# Mini Order & Inventory Management System

A production-quality, clean, and maintainable Spring Boot backend REST API for a **Mini Order & Inventory Management System**. Built with Java 17, Spring Boot 3, Spring Data JPA, Hibernate, and Maven, with comprehensive unit tests and OpenAPI/Swagger documentation.

---

## 🚀 Project Overview

The application manages core e-commerce order processing operations:
1. **Product Management**: Create, update, retrieve, search, and deactivate products with soft-delete safeguards to preserve order history.
2. **Customer Management**: Register and update customer profiles with email uniqueness validation.
3. **Transactional Order Management**: Atomic order creation (`POST /orders`) validating stock for all line items prior to deduction, price snapshotting, and transactional order cancellation restoring inventory.
4. **Reporting & Analytics**: Real-time customer spending reports, product sales performance, and top-selling product leaderboards.

---

## 🛠️ Technologies Used

* **Java**: 17
* **Spring Framework**: Spring Boot 3.3.4
* **Persistence**: Spring Data JPA, Hibernate ORM
* **Database**: H2 (In-Memory for default demo/tests), PostgreSQL / MySQL (Production configurable)
* **Build Tool**: Maven 3.9+
* **Validation**: Spring Boot Starter Validation (Bean Validation / Hibernate Validator)
* **API Documentation**: OpenAPI 3 / Swagger UI (`springdoc-openapi-starter-webmvc-ui:2.6.0`)
* **Testing**: JUnit 5, Mockito, AssertJ, Spring MockMvc

---

## 📐 Database Schema & Entity Relationships

```
+--------------------+        1      *       +--------------------+
|     Customer       |-----------------------|       Order        |
+--------------------+                       +--------------------+
| id (PK)            |                       | id (PK)            |
| name               |                       | customer_id (FK)   |
| email (UNIQUE)     |                       | order_date         |
| phone              |                       | total_amount       |
| created_at         |                       | status             |
+--------------------+                       +--------------------+
                                                        | 1
                                                        |
                                                        | *
+--------------------+        1      *       +--------------------+
|      Product       |-----------------------|     OrderItem      |
+--------------------+                       +--------------------+
| id (PK)            |                       | id (PK)            |
| name               |                       | order_id (FK)      |
| category           |                       | product_id (FK)    |
| price              |                       | quantity           |
| available_quantity |                       | unit_price         |
| active             |                       | total_price        |
| created_at         |                       +--------------------+
| updated_at         |
+--------------------+
```

### Key Relational Rules:
- **`Customer 1 ---- * Order`**: A customer can place multiple orders.
- **`Order 1 ---- * OrderItem`**: An order contains one or more line items.
- **`Product 1 ---- * OrderItem`**: A product can be referenced by multiple order items across orders.
- **Historical Price Preservation**: `OrderItem.unitPrice` captures the product price at the exact moment of order placement. Subsequent changes to `Product.price` will **never** alter historical order totals.

---

## ⚙️ Database Configuration

The system is configured in `src/main/resources/application.properties` to run seamlessly out of the box with an in-memory **H2 database**, requiring zero local setup.

### Production Environment Variable Overrides
To connect to **PostgreSQL** or **MySQL**, set the following environment variables:

| Environment Variable | Description | Example (PostgreSQL) | Example (MySQL) |
| :--- | :--- | :--- | :--- |
| `DB_URL` | JDBC Connection URL | `jdbc:postgresql://localhost:5432/orderinventorydb` | `jdbc:mysql://localhost:3306/orderinventorydb` |
| `DB_DRIVER` | JDBC Driver Class | `org.postgresql.Driver` | `com.mysql.cj.jdbc.Driver` |
| `DB_USERNAME` | Database User | `postgres` | `root` |
| `DB_PASSWORD` | Database Password | `secret` | `secret` |

---

## 🏃 How to Run the Application

### Prerequisites
- JDK 17+ installed
- Apache Maven 3.9+ installed

### Step-by-Step Instructions

1. **Navigate to project directory**:
   ```bash
   cd order-inventory-system
   ```

2. **Run Maven build & run unit tests**:
   ```bash
   mvn clean test
   ```

3. **Start the Spring Boot application**:
   ```bash
   mvn spring-boot:run
   ```

4. **Access Swagger UI & API Docs**:
   - **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   - **OpenAPI JSON Docs**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
   - **H2 Web Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:orderinventorydb`, Username: `sa`, Password: empty)

---

## 📚 API Endpoint Reference

### 1. Product APIs (`/products`)
- `POST /products` — Add a new product (Returns `201 CREATED`)
- `PUT /products/{id}` — Update product details (Returns `200 OK`)
- `GET /products/{id}` — Get product by ID (Returns `200 OK`)
- `GET /products?activeOnly=true` — List products (Returns `200 OK`)
- `GET /products/search?query={keyword}` — Search active products by name or category (Returns `200 OK`)
- `PATCH /products/{id}/deactivate` — Soft delete product set `active=false` (Returns `200 OK`)

### 2. Customer APIs (`/customers`)
- `POST /customers` — Create customer (Validates email uniqueness, returns `201 CREATED` or `409 CONFLICT`)
- `PUT /customers/{id}` — Update customer details (Returns `200 OK`)
- `GET /customers/{id}` — Get customer by ID (Returns `200 OK`)
- `GET /customers` — List all customers (Returns `200 OK`)
- `GET /customers/{id}/orders` — Retrieve customer order history (Returns `200 OK`)

### 3. Order APIs (`/orders`)
- `POST /orders` — Create order (Atomic transaction validating customer, active product status, and stock prior to deduction. Returns `201 CREATED`)
- `GET /orders/{id}` — Get order details with line items (Returns `200 OK`)
- `GET /orders?page=0&size=10` — List paginated orders sorted by date descending (Returns `200 OK`)
- `PUT /orders/{id}/cancel` — Cancel `CREATED` order & restore inventory (Returns `200 OK` or `400 BAD REQUEST`)

### 4. Reporting APIs (`/reports`)
- `GET /reports/customers/{customerId}` — Get customer spending metrics (`numberOfOrders`, `totalAmountSpent`, `averageOrderValue`)
- `GET /reports/products` — Get product sales report sorted by `quantitySold DESC`
- `GET /reports/top-products?limit=5` — Get top N products by quantity sold

---

## 🔒 Important Business Invariants

1. **Inventory Protection**: Product available quantity must never become negative.
2. **All-or-Nothing Order Creation**: Order processing is atomic (`@Transactional`). If stock for *any* requested item is insufficient, the entire order fails, and **no inventory is modified**.
3. **Historical Price Guarantee**: Order items store `unitPrice` at order creation time. Subsequent product price changes will not retroactively alter existing orders.
4. **Order Cancellation Rules**:
   - Cancelling a `CREATED` order restores inventory exactly once.
   - A `COMPLETED` or `CANCELLED` order **cannot** be cancelled.
5. **Customer Email Uniqueness**: Enforced via DB constraints and service validation (`HTTP 409 Conflict`).
6. **Soft Delete Product Deactivation**: Products referenced in order history are never physically deleted; they are marked `active = false`.

---

## 🧪 Unit Test Suite Verification

The project includes comprehensive JUnit 5 and Mockito unit tests verifying all core requirements:
- **Test 1**: Successful order creation (Order created, correct total, unit price, stock reduced, status `CREATED`).
- **Test 2**: Insufficient inventory failure (Order creation fails, no partial stock reduction, zero database mutations).
- **Test 3**: Invalid customer validation (Fails with HTTP 404, stock untouched).
- **Test 4**: Order cancellation (Order status updated to `CANCELLED`, inventory restored).
- **Test 5**: Cancellation of completed order (Fails with HTTP 400, stock unchanged).
- **Test 6**: Duplicate customer email (Fails with HTTP 409 Conflict).
- **Controller & Reporting Tests**: MockMvc REST API tests verifying status codes and error payloads.

Run all tests via:
```bash
mvn test
```
