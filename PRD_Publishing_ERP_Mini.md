# PRD — Publishing ERP Mini

- Version: 1.0
- Date: 2026-10-17
- Status: Implementation-ready specification
- Goal: Demonstrate ability to develop and maintain a small publisher's ERP using Spring Boot, MyBatis, JSP, jQuery and MariaDB.
- Audience: Developer with PHP/ASP/C# web experience transitioning to Java/Spring; Codex implementation agent.

## 1. Product definition
A Korean-language internal back-office application for managing books, distributors/bookstores, orders, fulfillment, stock and sales summaries. It is a portfolio and demonstration system, **not** a production ERP. Use synthetic data only.

### Success criteria
1. On MacBook Neo (Apple Silicon, 8GB RAM), developer can start MariaDB and app without Docker.
2. `./mvnw clean verify` succeeds, including meaningful automated tests.
3. Browser can create/edit/search books and partners; create orders with multiple items; fulfill and cancel according to defined rules; inspect inventory ledger and summary.
4. A fresh clone can be run from README, with no secrets committed.
5. Optional `docker compose up --build -d` starts app and DB with persistent DB volume.
6. Portfolio includes architecture, ERD, screenshots, sample credentials (demo only), known limitations and maintenance case study.

### Non-goals
Accounting, tax invoices, payroll, shipping-carrier integrations, payment gateways, live storefront sync, real customer data, multi-company tenancy, high-scale HA.

## 2. Environment and technical decisions
- Java 21, Maven Wrapper, Spring Boot 3.5.x stable patch with compatible MyBatis Spring Boot starter (verify exact supported versions before implementation).
- Spring MVC + JSP/JSTL, embedded Tomcat, WAR packaging where required for JSP support; validate local and container startup. Do not silently replace JSP with Thymeleaf/React.
- jQuery, Bootstrap 5; Korean UI. Prefer local static assets rather than relying on public CDNs.
- MariaDB, MyBatis XML mapper (no JPA), Flyway migrations if compatibility verified; otherwise versioned SQL migrations with documented procedure.
- JUnit 5, Spring Boot Test, MyBatis integration tests; Testcontainers optional only if resource footprint allows; CI may run DB service.
- Spring Security for login/roles in later phase, password hashing with BCrypt; CSRF protection for state-changing requests, validation, escaping and authorization.
- VS Code and terminal; native local processes by default. Docker Compose is a second-stage deliverable, not a prerequisite.
- Configuration: `application.yml` with environment-variable placeholders; separate `local`, `test`, `demo` profiles. Do not store DB passwords in Git.

## 3. Users and roles
- ADMIN: manage books, partners, inventory adjustments, users; access reports.
- STAFF: manage books, partners, orders, fulfillment; view inventory/reports.
- Demo credentials are seeded only for explicit demo profile, with prominent warning and no public default credentials.

## 4. Functional requirements and acceptance criteria

### FR-01 Books (P0)
- Fields: internal id, ISBN-13 (optional but unique when provided), title (required), author, publisher, publication date, list price (KRW integer >=0), active status, timestamps.
- List: pagination, title/ISBN search, status filter, sort by newest.
- Create, edit, deactivate; do not physically delete referenced books.
- Acceptance: validation messages in Korean; duplicate ISBN rejected gracefully; existing orders retain historical unit prices.

### FR-02 Partners / distributors (P0)
- Fields: partner code unique, name, type (BOOKSTORE/DISTRIBUTOR), contact person, phone/email optional, address, active flag.
- CRUD, keyword search, deactivate. Orders cannot reference inactive partners for new orders.

### FR-03 Orders (P0)
- Order header: order number unique, partner id, ordered_at, status, notes, created_by, timestamps.
- Items: book id, quantity >0, unit_price >=0, line_total. Use integer KRW values or fixed decimal; no floating-point money.
- Create with multiple items in one transaction; reject missing/inactive books or partner; server calculates totals (never trust browser totals).
- Statuses: DRAFT, CONFIRMED, FULFILLED, CANCELLED. Transitions: DRAFT→CONFIRMED/CANCELLED; CONFIRMED→FULFILLED/CANCELLED. FULFILLED cannot be directly cancelled; use return workflow. Prevent repeated fulfillment.
- Concurrency: use DB row locking or atomic conditional stock update to prevent overselling.

### FR-04 Inventory and movements (P0)
- One inventory row per book, `quantity_on_hand` nonnegative.
- Movement ledger: id, book id, type (RECEIPT/SHIPMENT/RETURN/ADJUSTMENT), signed quantity, reference type/id, timestamp, actor, note.
- Receiving stock increases balance and creates ledger entry in same transaction.
- Fulfillment reduces stock and creates SHIPMENT movement in same transaction; insufficient stock returns clear error and rolls back whole order fulfillment.
- Returns of fulfilled orders: explicit return record with quantities <= shipped-not-yet-returned; returned units increase stock with ledger entry. Support partial returns in phase 2 if necessary; do not fake it.
- Stock adjustments require reason and ADMIN role once security is enabled.
- Repeated requests must not create duplicate shipment/return entries (idempotency or transition guard).

### FR-05 Reports (P1)
- Monthly sales totals based on fulfilled orders, distinguish gross fulfilled value from net of returns. State metric definition clearly.
- Top-selling books, partner sales, current low stock; date filters.
- Parameterized SQL via MyBatis `#{}`; never interpolate user input into raw SQL identifiers.

### FR-06 Authentication and authorization (P1)
- Login/logout, password hash, role-based authorization, CSRF-safe forms/AJAX, session management.
- No public registration. All business pages require login when enabled.

### FR-07 Maintenance evidence (P1)
- Document one reproducible simulated bug (e.g., duplicate fulfillment or concurrent stock decrement), failing regression test, fix, and test results in `docs/maintenance-case.md`.
- Keep Git history with small meaningful commits.

## 5. Data model
Required tables: `books`, `partners`, `users`, `orders`, `order_items`, `inventory`, `stock_movements`, `returns`, `return_items` (last two may be added in phase 2).
- FK constraints on all relationships, indexes on ISBN, partner code, order number, order date/status and ledger book/date.
- `orders` 1:N `order_items`; `books` 1:N `order_items`; `books` 1:1 `inventory`; `books` 1:N `stock_movements`; `partners` 1:N `orders`.
- DB schema/migrations and synthetic seed data tracked in repo. Prefer `utf8mb4`.
- Document policy for stock reservation: MVP does **not** reserve stock at confirmation; check stock at fulfillment.

## 6. Screen routes (suggested)
- `/login`
- `/dashboard`
- `/books`, `/books/new`, `/books/{id}/edit`
- `/partners`, `/partners/new`, `/partners/{id}/edit`
- `/orders`, `/orders/new`, `/orders/{id}`
- `/inventory`, `/inventory/receipts`, `/inventory/movements`
- `/reports/sales`
- Controllers return JSP views; JSON endpoints only for targeted AJAX interactions. Use POST for mutations and CSRF tokens.

## 7. Architecture / repository
```
publishing-erp-mini/
  pom.xml
  mvnw, .mvn/
  README.md, .gitignore, .env.example
  src/main/java/com/portfolio/erp/
    ErpApplication.java
    book/ (Controller, Service, Mapper, DTO, model)
    partner/
    order/
    inventory/
    report/
    security/
    common/ (errors, validation)
  src/main/resources/
    application.yml
    mapper/*.xml
    db/migration/
    static/css/, static/js/
  src/main/webapp/WEB-INF/jsp/
    common/, book/, partner/, order/, inventory/, report/
  src/test/
  docs/ (architecture.md, erd.md, screenshots/, maintenance-case.md)
  Dockerfile, compose.yaml
```
Separate transactional business logic from HTTP handlers; no SQL in controllers/JSP. Use DTOs and validation; mapper XML remains auditable.

## 8. Development phases and gates
**Phase 0 — bootstrap (day 1):** confirm ARM64 JDK, tool versions, Maven wrapper, JSP render, MariaDB connection; health check. Gate: page renders on localhost:8080 and DB test passes.

**Phase 1 — core master data (days 2–5):** schema migrations, books and partners CRUD/search/pagination; tests. Gate: fresh database seed + create/edit/search work and `mvn verify` passes.

**Phase 2 — transactions (days 6–10):** orders, fulfillment, inventory movements, cancellation, basic returns; integration tests for insufficient stock, duplicate fulfillment, rollback, concurrency. Gate: full demo scenario passes without negative stock.

**Phase 3 — reports/security (days 11–12):** sales reports, login, roles, CSRF and validation. Gate: unauthorized requests blocked; totals reconcile with test fixtures.

**Phase 4 — portfolio (days 13–14):** README, ERD, screenshots, maintenance case, test output, Dockerfile/Compose. Gate: fresh-clone setup and compose startup verified; no secrets in repository.

**Optional Phase 5 — VPS:** deploy to Contabo only after explicit approval; HTTPS, reverse proxy, firewall, backups, demo-only synthetic data, strong credentials, non-root container, monitoring, resource limits. Never publish the DB port publicly.

Schedule is aspirational, not a quality override. If gates fail, fix before moving on.

## 9. Performance / security / operations
- Optimize for 8GB MacBook: use local MariaDB, run Spring Boot directly, avoid Docker during everyday development; cap JVM heap reasonably (e.g. `-Xmx768m` to start, adjust after observing memory).
- DB connections pool conservative (e.g. 5), pagination on list endpoints, index common searches.
- Input validation server-side, prepared MyBatis binding, output escaping, least privilege DB account, no secrets in commits, no real PII.
- Document startup, shutdown, logs, backup/restore, schema migrations and known limitations.
- For Docker Compose: app and DB services, named persistent volume, healthcheck, `.env.example`, internal DB networking; database not published on host unless explicitly needed for local debugging.

## 10. Automated tests
- Unit: pricing/totals, order state machine, validation.
- Integration: mapper CRUD, FK/unique constraints, order create atomicity, stock receiving, fulfillment rollback, duplicate request handling, returns and reconciliation.
- Security: unauthenticated/unauthorized access denied, CSRF state changes protected.
- Smoke: JSP view renders and app starts on supported Java version.
- Record actual executed commands and results; do not claim passing tests that were not run.

## 11. Portfolio deliverables
- Public GitHub repo with runnable source and clean commit history.
- Korean README: purpose, screenshots, architecture, tech choices, environment setup, commands, demo scenario, tests, limitations.
- ERD and sample dataset (synthetic), API/routes list, troubleshooting guide.
- 2–3 minute demo script/video: book registration → partner → receipt → order → fulfillment → stock ledger → sales report.
- Maintenance case study showing a reproduced bug and regression test.

## 12. Codex execution protocol
1. Read this PRD, inspect repository, identify current state; do not assume files exist.
2. Verify toolchain and dependency compatibility; document chosen exact versions.
3. Write short implementation plan and execute **only the next unfinished phase** unless instructed to run all phases.
4. Implement complete, runnable files, not pseudocode or placeholder TODOs for phase acceptance criteria.
5. Run compilation and tests; fix failures; report exact commands/results and any environment limitations.
6. Make focused commits if repository and permissions permit; do not push, publish, provision paid resources, or deploy without user approval.
7. At phase end summarize completed functionality, changed files, test status, remaining risks, next phase.
8. Explain Java/Spring patterns with brief C#/ASP.NET comparisons where educationally useful.
9. Do not modify unrelated files, delete user data, or overwrite existing work without confirmation.

## 13. First Codex instruction
"Read `PRD_Publishing_ERP_Mini.md` and implement Phase 0 and Phase 1 in this repository. First verify Mac ARM64/JDK 21, Spring Boot–MyBatis compatibility and JSP runtime support. Use Maven Wrapper, Spring MVC + JSP/JSTL + jQuery + MariaDB + MyBatis XML; no JPA/React/Thymeleaf. Create migrations, synthetic seed data, book/partner CRUD with Korean UI, tests and README. Run available checks and report results; stop before Phase 2. Do not deploy or push without approval."
