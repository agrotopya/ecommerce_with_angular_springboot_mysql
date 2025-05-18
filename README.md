**Fibiyo E-Commerce Platform - Detailed Project Report**
![fibiyo](readmeimages/fibiyo.gif)
**1. Introduction and Project Summary**

Fibiyo is designed as a comprehensive e-commerce platform. It aims to provide a shopping experience for customers and enable sellers to list and sell their products. Additionally, an admin panel is included for platform management. The project is developed with a modern backend architecture (Spring Boot) and a reactive frontend (Angular). It incorporates innovative features such as artificial intelligence (AI) capabilities (image generation, review summarization), short video ("Feels") integration, and a subscription system.

**Purpose of the Report:** This report aims to explain in detail the system architecture, backend and frontend structure, technologies used, core components, and data flows of the Fibiyo project. It will also touch upon potential areas for future development.

---

**2. System Architecture**

The overall system architecture follows a typical three-tier (or n-tier) web application model and may include various microservice-like interactions (especially with external services like AI and payment).

**Diagram Description (Conceptual):**

```
+---------------------+      +-----------------------+      +---------------------+
|        Users        |----->|   Frontend (Angular)  |<---->|  Backend (Spring   |
| (Customer, Seller, |<-----|    (Web Browser)      |      |   Boot / Java)      |
|      Admin)         |      +-----------------------+      +----------+----------+
+---------------------+               ^    |                          |
                                      |    | (API Requests/Responses) |
                                      |    v                          v
                              +-------+---------+        +----------+----------+
                              | Load Balancer   |------->| API Gateway (Opt.)  |
                              | (Optional)      |        +---------------------+
                              +-----------------+                   |
                                                                    v
                                                      +-------------+-------------+
                                                      |    Database (MySQL)       |
                                                      +---------------------------+
                                                                    ^
                                                                    |
                                  +---------------------------------+---------------------------------+
                                  |                                 |                                 |
                      +-----------+-----------+        +------------+------------+       +------------+------------+
                      | Payment Service (Stripe)|        |   AI Service (OpenAI)   |       | File Storage Service  |
                      +-----------------------+        +-------------------------+       |   (Local/Cloud)       |
                                                                                         +-------------------------+
```

content_copydownload

Use code [with caution](https://support.google.com/legal/answer/13505487).

- **Users:** Customers, sellers, and admins accessing the platform via web browsers.
    
- **Frontend (Angular):** Presents the user interface, handles user interactions, and sends requests to the backend API. Runs in the browser.
    
- **Load Balancer (Optional):** Can be used to distribute the load across backend servers in high-traffic situations.
    
- **API Gateway (Optional):** If multiple microservices exist (though not strictly the case here, considering external service integrations), it can be used for request routing, security, rate limiting, etc. Currently, direct access to the backend might be the case.
    
- **Backend (Spring Boot):** Manages business logic, data processing, API endpoints, and external service integrations.
    
- **Database (MySQL):** Stores all persistent data (users, products, orders, etc.).
    
- **External Services:**
    
    - **Payment Service (Stripe):** Manages secure payment transactions.
        
    - **AI Service (OpenAI):** Used for image generation and potentially text-based AI features.
        
    - **File Storage Service:** Location where files like product images and "Feel" videos are stored (could be a local uploads/ folder or a cloud-based solution).
        

---

**3. Backend Architecture**

The backend is designed with a layered architecture using the Spring Boot framework. This structure aims for modularity, testability, and maintainability.

**3.1. Technologies and Libraries Used:**

- **Framework:** Spring Boot 3.4.5
    
- **Language:** Java 17
    
- **Database:** MySQL (defined via schema.sql)
    
- **ORM:** Spring Data JPA (with Hibernate implementation)
    
- **Security:** Spring Security, JWT (JSON Web Tokens)
    
- **API:** Spring Web (REST Controllers)
    
- **Build Tool:** Apache Maven
    
- **DTO Mapping:** MapStruct
    
- **Payment Integration:** Stripe Java SDK
    
- **AI Integration:** OpenAI Java SDK, WebClient (for HTTP requests)
    
- **Email:** Spring Boot Starter Mail
    
- **File Operations:** Thumbnailator (image compression), Apache Commons IO
    
- **Other:** Lombok, Jakarta Validation
    

**3.2. Directory Structure and Layering (Conceptual Layered Architecture):**

The backend code is logically divided into layers based on the principle of separation of responsibilities.

```
+---------------------------------------------------------------------------------+
| Presentation Layer (Web Layer)                                                  |
| +-----------------------------------------------------------------------------+ |
| |                       Controllers (`infrastructure/web/controller`)         | |
| | (Handling HTTP Requests, Preparing Responses, Calling Services)             | |
| +-----------------------------------------------------------------------------+ |
+------------------------------------^--------------------------------------------+
                                     | (Service Calls)
+------------------------------------v--------------------------------------------+
| Application Layer (Business Logic)                                              |
| +---------------------------+  +---------------------------+  +---------------+ |
| |    Services (`application/service`)                     |  | DTOs (`application/dto`)                    |  | Mappers       | |
| | (Business rules,        |  | (Data Transfer Objects)     |  | (`application/mapper`)| |
| |  validations,           |  +---------------------------+  +---------------+ |
| |  orchestration)         |  +---------------------------+  +---------------+ |
| +---------------------------+  | Exceptions (`application/exception`)        |  | Utilities     | |
|                                | (Custom Error Classes)      |  | (`application/util`)| |
|                                +---------------------------+  +---------------+ |
+------------------------------------^--------------------^--------------------^---+
                                     | (Repository Usage) | (Entity Usage)     | (Adapter Usage)
+------------------------------------v--------------------v--------------------v---+
| Domain Layer (Core Entities)                                                  |
| +---------------------------+  +-------------------------------------------+ |
| |    Entities (`domain/entity`)                         |  | Enums (`domain/enums`)                          | |
| | (Database Entities)       |  | (Constant Values)                         | |
| +---------------------------+  +-------------------------------------------+ |
+------------------------------------^--------------------^-----------------------+
                                     | (Database         | (External Service
                                     |  Interaction)     |  Interaction)
+------------------------------------v--------------------v-----------------------+
| Infrastructure Layer (Interaction with External World)                          |
| +------------------------------+  +------------------------------------------+ |
| | Persistence (`infrastructure/persistence`) |  | Security (`infrastructure/security`)           | |
| |  - Repositories              |  |  - JWT, UserDetails, EntryPoint, Filter  | |
| |  - Specifications          |  +------------------------------------------+ |
| +------------------------------+  +------------------------------------------+ |
|                                   | Adapters (`infrastructure/adapter`)        | |
|                                   |  - Email, AI, Storage, Payment           | |
|                                   +------------------------------------------+ |
+---------------------------------------------------------------------------------+
| Configuration Layer (`config`)                                                  |
| (Spring Bean Definitions, Security, CORS, OpenAI, Stripe, etc. settings)       |
+---------------------------------------------------------------------------------+
```

content_copydownload

Use code [with caution](https://support.google.com/legal/answer/13505487).

- **Presentation Layer (Web Layer - infrastructure/web/controller):**
    
    - Receives incoming HTTP requests from the outside world (e.g., /api/auth/login, /api/products).
        
    - Validates incoming requests (via DTO validation).
        
    - Triggers business logic by calling appropriate service methods.
        
    - Converts results returned from services into suitable HTTP responses (DTOs in JSON format) and sends them to the user.
        
    - Uses Spring MVC annotations like @RestController, @GetMapping, @PostMapping.
        
    - Uses @PreAuthorize for authorization.
        
- **Application Layer (application package):**
    
    - Contains the core business logic of the project.
        
    - **Services (application/service and impl):** Implement business rules, coordinate between different domain entities and repositories. Called by Controllers. For example, AuthServiceImpl manages user registration and login logic, while OrderServiceImpl contains order creation and update logic.
        
    - **DTOs (Data Transfer Objects - application/dto):** Used for data transfer between layers, especially in API requests and responses. Backend DTOs (e.g., LoginRequest, ProductResponse) and potentially frontend DTOs (if different) are defined here. They contain validation annotations (@NotBlank, @Size, etc.).
        
    - **Mappers (application/mapper):** Provide mapping between Entities and DTOs. MapStruct is used in the project (as seen from generated files like CartItemMapperImpl), reducing boilerplate conversion code.
        
    - **Exceptions (application/exception):** Contains custom error classes (e.g., ResourceNotFoundException, BadRequestException) and the global exception handler (GlobalExceptionHandler).
        
    - **Utilities (application/util):** Includes helper classes used throughout the project (e.g., SlugUtils).
        
- **Domain Layer (domain package):**
    
    - Defines the core business entities and rules of the project. Should be independent of other layers.
        
    - **Entities (domain/entity):** JPA entities corresponding to database tables (e.g., User, Product, Order). Contain JPA annotations like @Entity, @Table, @Id, @Column, @ManyToOne.
        
    - **Enums (domain/enums):** Defines sets of constant values used within the project (e.g., Role, OrderStatus, DiscountType).
        
- **Infrastructure Layer (infrastructure package):**
    
    - Deals with external systems and infrastructural concerns.
        
    - **Persistence (infrastructure/persistence):**
        
        - **Repositories (repository):** Spring Data JPA interfaces abstracting database operations (CRUD) (e.g., UserRepository, ProductRepository).
            
        - **Specifications (specification):** Used for creating dynamic and complex queries using the Spring Data JPA Specifications API (e.g., ProductSpecifications, OrderSpecifications).
            
    - **Security (infrastructure/security):** Contains classes related to authentication and authorization. Includes JwtTokenProvider (JWT creation and validation), JwtAuthenticationFilter (checks JWT on every request), CustomUserDetailsService (loads user details for Spring Security), AuthEntryPointJwt (handles unauthorized access errors), and classes for custom security checks like ProductSecurity, OrderSecurity.
        
    - **Adapters (infrastructure/adapter):** Contains classes that provide integration with external services (AI, Payment, Email, File Storage). This abstracts the implementation details of external services from the business logic. E.g., OpenAiServiceImpl (which also acts as an adapter) or LocalStorageServiceImpl.
        
- **Configuration Layer (config package):**
    
    - Contains Spring Boot configurations. Includes classes like SecurityConfig (CORS settings, JWT filter integration, endpoint authorization), StripeConfig (setting Stripe API key), OpenAiConfig (creating the OpenAI client bean), WebConfig (static file serving).
        

**3.3. Backend Component Diagram (Conceptual Packages and Interactions):**

This diagram shows how the main packages/modules in the backend interact with each other.

```
graph LR
    subgraph "Presentation (Web)"
        A[Controllers]
    end

    subgraph "Application (Business Logic)"
        B[Services]
        C[DTOs]
        D[Mappers]
        E[Exceptions]
    end

    subgraph "Domain (Core Entities)"
        F[Entities]
        G[Enums]
    end

    subgraph "Infrastructure (Altyapı)"
        H[Repositories]
        I[Security Components]
        J[Adapters (AI, Payment, etc.)]
        K[Storage Service]
    end

    subgraph "Configuration"
        L[Configuration Classes]
    end

    A -->|uses| B
    A -->|uses| C
    A -->|uses| E

    B -->|uses| D
    B -->|uses| C
    B -->|uses| F
    B -->|uses| G
    B -->|uses| H
    B -->|uses| J
    B -->|uses| K
    B -->|uses| I  // Services can use the security context

    D -->|maps| C
    D -->|maps| F

    H -->|accesses| F     // Repositories manage Entities
    H -->|accesses| G     // and can use Enums

    I -->|affects| A    // Security filters run before Controllers
    I -->|uses| F   // Uses the User entity

    J -->|external services| M{External Services (Stripe, OpenAI)}
    K -->|file system| N[File System/Cloud Storage]

    L -->|configures| I
    L -->|configures| J
    L -->|configures| K
```

content_copydownload

Use code [with caution](https://support.google.com/legal/answer/13505487).Mermaid

**Explanation:**

- **Controllers (A):** Receive HTTP requests from users, validate data using DTOs (C), call Services (B) for business logic, and can handle exceptions (E).
    
- **Services (B):** Contain the main business logic. Use Mappers (D) to convert between DTOs (C) and Entities (F). Use Repositories (H) for database operations. Call Adapters (J) or the Storage Service (K) for external services or file storage if needed. Can use project Enums (G) and the security context (I).
    
- **Repositories (H):** Interact with the database using JPA Entities (F).
    
- **Security Components (I):** Provide security mechanisms like JWT operations, user authentication, and authorization. Uses the User entity (F).
    
- **Adapters (J) / Storage Service (K):** Interact with external services (M) or the file system (N).
    
- **Configuration Classes (L):** Provide configuration for systems like Spring Security (I), OpenAI (J), Stripe (J), etc.
    

**3.4. Database Schema (schema.sql Summary):**

Your schema.sql file contains a detailed DDL (Data Definition Language) for MySQL 8.0. The main tables and their relationships are:

- **users**: User information, roles, social media login details, subscription info, AI quota details.
    
- **categories**: Hierarchical category structure (using parent_category_id).
    
- **products**: Product details, price, stock, SKU, image URLs, relationships with seller and category, approval and active status, AI features. Includes fields for soft delete (is_deleted) and optimistic locking (version).
    
- **product_images**: Multiple images belonging to a product.
    
- **coupons**: Discount coupons, types, values, validity dates.
    
- **orders**: Customer orders, statuses, address information, payment details, used coupon.
    
- **order_items**: Each product item within an order, price at the time of order.
    
- **payments**: Payment records, methods, transaction IDs, statuses.
    
- **reviews**: Product reviews, ratings, approval status.
    
- **notifications**: Notifications sent to users.
    
- **wishlist_items**: Products in users' wishlists.
    
- **carts**: Users' active shopping carts.
    
- **cart_items**: Product items within the cart.
    
- **feels**: Short video ("Feel") content uploaded by sellers, associated products, view and like counts.
    
- **feel_likes**: Tracks which users liked which "Feels".
    

Foreign key constraints, unique constraints, and indexes for performance are correctly defined in the schema. Strategies like ON DELETE SET NULL and ON DELETE CASCADE are used to maintain relational integrity. Calculated fields like product.finalAmount (GENERATED ALWAYS AS) are noted to be managed in the service layer now.

---

**4. Frontend Architecture**

The frontend is developed using Angular and TypeScript. It targets a modular structure, reactive programming (Signals), and best practices.

**4.1. Technologies and Libraries Used:**

- **Framework:** Angular (v19.2.x)
    
- **Language:** TypeScript (v5.7.x)
    
- **Styling:** SCSS
    
- **Build Tool:** Angular CLI
    
- **HTTP Client:** Angular HttpClient (likely with withFetch)
    
- **Routing:** Angular Router
    
- **Forms:** Angular Reactive Forms
    
- **State Management:** Angular Signals (preferred)
    
- **UI Library:** Angular Material (partially used: MatCard, MatFormField, MatInput, etc.)
    
- **Other:** ngx-infinite-scroll (potentially for infinite scrolling in places like the "Feels" list)
    

**4.2. Directory Structure and Modularity (Focused on Angular Standalone API):**

The frontend aims for modularity using Angular's modern Standalone API.

```
frontend/
├── src/
│   ├── app/
│   │   ├── app.component.ts           # Main application component
│   │   ├── app.config.ts              # Main application configuration (providers)
│   │   ├── app.routes.ts              # Main routing (with lazy loading)
│   │   │
│   │   ├── core/                      # Core services, guards, interceptors
│   │   │   ├── constants/             # Constants like API endpoints
│   │   │   ├── guards/                # auth.guard.ts, role.guard.ts
│   │   │   ├── interceptors/          # auth.interceptor.ts, error.interceptor.ts
│   │   │   └── services/              # api.service.ts, auth.service.ts, storage.service.ts, notification.service.ts (core)
│   │   │
│   │   ├── shared/                    # Shared, reusable elements
│   │   │   ├── components/            # PaginatorComponent, LoadingSpinnerComponent, ConfirmationModalComponent, NotificationContainerComponent
│   │   │   ├── models/                # Interfaces corresponding to backend DTOs
│   │   │   ├── enums/                 # Enums corresponding to backend enums
│   │   │   └── pipes/                 # nl2br.pipe.ts, time-ago.pipe.ts
│   │   │
│   │   ├── layout/                    # Main page layout components
│   │   │   ├── main-layout/           # Layout for general user interface
│   │   │   ├── admin-layout/          # Layout for admin panel
│   │   │   ├── header/
│   │   │   └── footer/
│   │   │
│   │   └── features/                  # Feature-based modules/folders (each may contain its own routes, components, and services)
│   │       ├── home/
│   │       ├── auth/
│   │       ├── products/
│   │       ├── categories/
│   │       ├── cart/
│   │       ├── checkout/
│   │       ├── orders/
│   │       ├── profile/
│   │       ├── wishlist/
│   │       ├── notifications/ (has components, service might be in core)
│   │       ├── feels/
│   │       ├── admin/
│   │       │   ├── dashboard/
│   │       │   ├── user-management/
│   │       │   ├── product-management/
│   │       │   ├── order-management/
│   │       │   └── components/ (shared components specific to admin)
│   │       ├── seller/
│   │       │   ├── dashboard/
│   │       │   ├── product-management/
│   │       │   ├── order-management/
│   │       │   └── components/ (shared components specific to seller)
│   │       ├── subscriptions/
│   │       └── reports/ (future)
│   │
│   ├── assets/
│   └── environments/
├── angular.json
├── package.json
└── tsconfig.json
```

content_copydownload

Use code [with caution](https://support.google.com/legal/answer/13505487).

- **core/**: Contains the fundamental logic of the application. ApiService handles communication with the backend. AuthService manages authentication and user state. AuthGuard and RoleGuard control route access. AuthInterceptor adds JWT to outgoing requests, ErrorInterceptor catches global errors.
    
- **shared/**: Contains reusable components across different parts of the project (e.g., PaginatorComponent, NotificationContainerComponent), data models (product.model.ts, user.model.ts, etc.), and enums (role.enum.ts, order-status.enum.ts, etc.).
    
- **layout/**: Components that form the main page structure. HeaderComponent (navigation, search, user menu), FooterComponent, and MainLayoutComponent and AdminLayoutComponent which contain these.
    
- **features/**: The main functional areas of the application. Each feature resides in its own subfolder, containing its components, services (if needed), and route definitions. For example, the products feature houses product listing, detail, and filtering components, and a ProductService. The admin and seller features also contain their sub-features (user management, product management, etc.). This structure is further simplified with the Standalone API.
    

**4.3. Frontend Component Diagram (Example Flow - Product Listing and Detail):**

This diagram illustrates how components and services interact when a user views a product list and navigates to a product detail page.

```
graph TD
    User -->|Route Navigation| AppRouter[app.routes.ts]

    AppRouter -->|Lazy Load| ProductsRoutes[products.routes.ts]
    ProductsRoutes -->|Routes to| PL[ProductListComponent]

    PL -->|Data Request| PS[ProductService]
    PS -->|HTTP GET (with Filters)| AS[ApiService]
    AS -->|API Call| BackendAPI[Backend API (/api/products)]
    BackendAPI -->|Page<Product>| AS
    AS -->|Page<Product>| PS
    PS -->|Page<Product>| PL

    PL -->|Renders via *ngFor| PCC[ProductCardComponent]
    PCC -->|@Input() product| ProductData[Product Data]
    PCC -->|User Click| User
    User -->|Add to Cart Click| PCC
    PCC -->|Add to Cart Request| CartService[CartService]
    User -->|Detail Link Click| AppRouter
    AppRouter -->|Lazy Load| ProductsRoutes
    ProductsRoutes -->|Routes with Route Param (:slug)| PD[ProductDetailComponent]

    PD -->|Data Request (with slug)| PS
    PS -->|HTTP GET (with slug)| AS
    AS -->|API Call| BackendAPI_Detail[/api/products/slug/:slug]
    BackendAPI_Detail -->|Product| AS
    AS -->|Product| PS
    PS -->|Product| PD
    PD -->|Displays| ProductDetailsUI[Product Details UI]

    PD -->|List Reviews| ReviewsComp[ProductReviewsComponent]
    ReviewsComp -->|Data Request| ReviewService[ReviewService]
    ReviewService -->|HTTP GET| AS
    AS -->|API Call| BackendAPI_Reviews[/api/reviews/product/:id]
    BackendAPI_Reviews -->|Page<Review>| AS
    AS -->|Page<Review>| ReviewService
    ReviewService -->|Page<Review>| ReviewsComp

    subgraph "Product Feature"
        PL
        PCC
        PD
        PS
        ReviewsComp
        ReviewService
    end

    subgraph "Core"
        AppRouter
        AS
        CartService
    end
```

content_copydownload

Use code [with caution](https://support.google.com/legal/answer/13505487).Mermaid

**Explanation:**

1. The user navigates to /products (or a filtered path).
    
2. app.routes.ts lazy loads the relevant products.routes.ts.
    
3. ProductListComponent becomes active.
    
4. ProductListComponent requests products from the backend (with filters and pagination info) using ProductService.
    
5. ProductService makes a request to the /api/products endpoint via ApiService.
    
6. The incoming Page<Product> data is returned to ProductListComponent.
    
7. ProductListComponent renders a ProductCardComponent for each product, passing the product data via @Input().
    
8. If the user clicks the detail link on a product card, they are routed to ProductDetailComponent with the :slug parameter.
    
9. ProductDetailComponent fetches the product details using the slug via ProductService.getPublicProductBySlug().
    
10. While displaying product details, ProductReviewsComponent (if present) fetches reviews via ReviewService using the relevant product ID.
    
11. If the user clicks the "Add to Cart" button, ProductCardComponent or ProductDetailComponent calls CartService.addItemToCart().
    

**4.4. State Management (Angular Signals):**

The .clinerules/rules.md file indicates a preference for Angular Signals. This is a modern approach to enhance reactivity and performance.

- **State in Services:** Global states like currentUser and isAuthenticated in AuthService are managed with signal. The cart state (cart and totalCartItems) in CartService also uses signal.
    
- **State in Components:** Components use signal for their local state or to read signals from services. computed signals are used for values derived from other signals (e.g., pages in PaginatorComponent). effect is used to run side effects (e.g., API calls, logging) in reaction to signal changes (as seen in the constructors of AuthService and CartService).
    

**4.5. Routing and Lazy Loading:**

- app.routes.ts contains the main routing configuration.
    
- **Lazy loading** is implemented using loadChildren for all modules/folders under features/. This optimizes the initial load time.
    
- Routes are protected using AuthGuard and RoleGuard.
    

---

**5. Deployment Architecture (Conceptual)**

Although specific details on how the project will be deployed to a live environment are not provided, a typical web application deployment architecture might look like this:

```
+---------------------+      +---------------------+      +---------------------+      +-----------------------+
|    User Browser     |----->|    CDN (Optional)   |----->|   Load Balancer     |----->|      Web Servers      |
+---------------------+      | (Frontend Statics)  |      |                     |      |  (Nginx/Apache etc.  |
                             +---------------------+      +----------+----------+      | Serving Angular Build)|
                                                                     |                 +-----------------------+
                                                                     |
                                                                     | (API Request Routing)
                                                                     v
                                                        +----------+----------+
                                                        |  Application Servers  |
                                                        |  (Spring Boot Jar   |
                                                        |   Cluster)          |
                                                        +----------+----------+
                                                                   |
                                                                   v
                                                         +---------+---------+
                                                         |  Database Server  |
                                                         |     (MySQL)       |
                                                         +-------------------+
                                                                   |
                                           +-----------------------+-----------------------+
                                           |                                               |
                               +-----------+-----------+                      +------------+------------+
                               | Payment Service (Stripe)|                      |   AI Service (OpenAI)   |
                               +-----------------------+                      +-------------------------+
```

content_copydownload

Use code [with caution](https://support.google.com/legal/answer/13505487).

- **CDN (Content Delivery Network):** Can be used for the frontend's static files (JS, CSS, images) to speed up global access.
    
- **Load Balancer:** Can be used for both frontend (web servers) and backend (application servers) to distribute incoming requests, improving performance and redundancy.
    
- **Web Servers:** Servers like Nginx or Apache serve the static files from the Angular build and route API requests to the backend application servers.
    
- **Application Servers:** Servers where the Spring Boot application runs, often in a cluster.
    
- **Database Server:** Server running the MySQL database. Replication or cluster setups can be used for redundancy and performance.
    
- **External Services:** Services like Stripe and OpenAI run on their own infrastructure and are integrated via APIs.
    

---

**6. Core Features (Based on API and Frontend Structure)**

1. **User Management and Authentication:**
    
    - Registration, login, forgot password, reset password.
        
    - Profile viewing and updating.
        
    - Role-based authorization (Customer, Seller, Admin).
        
2. **Category Management:**
    
    - Listing categories (active, all, subcategories, root categories).
        
    - Viewing category details (by ID and slug).
        
    - Admin: Create, update, delete categories.
        
3. **Product Management:**
    
    - Listing public, approved products (paginated, filtered, sorted).
        
    - Viewing product details by ID and slug.
        
    - Seller: Create new product, list/update/delete own products, set active status, upload images.
        
    - Admin: List all products, approve/reject products, set active status, delete products.
        
4. **Shopping Cart:**
    
    - Viewing cart, adding products, updating quantity, removing products, clearing cart.
        
5. **Wishlist:**
    
    - Viewing wishlist, adding/removing products, checking if a product is in the list.
        
6. **Order Management:**
    
    - Customer: Create order (from cart), list/view details of own orders, cancel order.
        
    - Admin/Seller: List orders (filtered), view order details, update order status, add tracking number.
        
7. **Review Management:**
    
    - Listing product reviews (approved/all).
        
    - Customer: Add review, delete own review.
        
    - Admin: List all reviews, approve/reject reviews, delete reviews.
        
8. **Coupon Management:**
    
    - Coupon validation (based on cart total).
        
    - Admin: Create, list, update, delete coupons.
        
9. **Notification System:**
    
    - Listing user's own notifications, seeing unread count.
        
    - Marking notifications as read (single/all), deleting notifications.
        
    - Automatic notification creation by the system (order status, registration, etc.).
        
10. **Payment System (Stripe Integration):**
    
    - Creating Stripe Checkout sessions for orders and subscriptions.
        
    - Updating payment status and orders by listening to Stripe webhooks.
        
11. **Subscription System:**
    
    - Viewing user's subscription status.
        
    - Creating payment sessions for subscriptions.
        
    - Periodically checking and updating expired subscriptions.
        
12. **"Feels" (Short Video) Feature:**
    
    - Seller: Create Feel (upload video/thumbnail), list/delete own Feels.
        
    - Public: List Feels (general, by product, by seller), view Feel details (with view counter).
        
    - Logged-in User: Like/unlike a Feel.
        
    - Admin: Manage Feels (activate/deactivate).
        
13. **AI Features (For Sellers):**
    
    - Generate/edit product image via prompt (with quota control).
        
    - Set generated AI image as the main product image.
        
    - (Future) AI-powered summarization of product reviews.
        

---

**7. API Documentation Summary**

The apidocs.md file provides detailed descriptions of the backend API endpoints, request and response DTOs, and authorization rules. This document serves as the primary reference source for frontend development. Its main sections include:

- General Information (Base URL, Authentication, Error Responses)
    
- Endpoint Groups by Controller:
    
    - Auth Controller
        
    - User Controller
        
    - Category Controller
        
    - Product Controller (Public, Seller, Admin)
        
    - SellerProductController
        
    - Review Controller
        
    - Cart Controller
        
    - Wishlist Controller
        
    - Order Controller
        
    - Coupon Controller
        
    - Notification Controller
        
    - Payment Controller (Stripe integration)
        
    - Subscription Controller
        
    - AdminUserController
        
    - FeelController
        
- Additional DTO Definitions
    

---

**8. Development Standards and Best Practices (Frontend Focused)**

The principles outlined in .clinerules/angular.md and .clinerules/rules.md aim to enhance the project's quality and maintainability:

- **Modularity:** Preference for Standalone API, feature-based folder structure.
    
- **Performance:** Lazy loading, Angular Signals, OnPush change detection, NgOptimizedImage, @defer blocks.
    
- **Code Quality:** Strict TypeScript usage (no any), meaningful naming, file naming standards, SCSS usage, code cleanliness.
    
- **Angular Best Practices:** inject() function, async pipe, trackBy, component composition.
    
- **Accessibility:** Semantic HTML and ARIA.
    
- **Security:** XSS prevention, sanitization.
    
- **Testing:** Unit tests with AAA pattern.
    

---

**9. Potential Development Areas and Future Work**

- **Comprehensive Testing:** Increase integration and E2E tests in addition to unit tests.
    
- **CI/CD Pipeline:** Set up automated build, test, and deployment processes.
    
- **Advanced State Management:** As the project grows, consider a more centralized state management solution (NgRx, Akita) in addition to or instead of Angular Signals.
    
- **Real-time Notifications:** WebSocket integration for instant notifications.
    
- **Detailed Reporting:** More comprehensive sales, product performance, etc., reports for Admin and Seller panels.
    
- **Advanced AI Features:** Review analysis, personalized product recommendations, chatbot integration.
    
- **Transition to Microservices Architecture (If needed):** If the project scales significantly, specific modules (e.g., payment, notification, AI) could be converted into separate microservices.
    
- **Mobile Application:** Develop native or cross-platform mobile apps for the platform.
    
- **UI/UX Improvements:** Continuous improvements and user testing to enhance the user experience.
    
- **Docker and Containerization:** For easier deployment and scalability.
    
- **Items in Frontend TODO.md:** Development of features not yet completed, particularly in the admin panel, seller panel, and reporting areas.
