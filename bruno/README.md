# Semantic Library API — Bruno collection

REST requests for the Spring Boot backend in `backend/`.

## Use

1. Start the backend with the admin password this collection expects:
   from `backend/` run `ADMIN_PASSWORD=admin123 mvn spring-boot:run`.
2. In Bruno, open this `bruno/` folder as a collection and select the **Local** environment.
3. Run **Auth / Admin Login** first — its script stores `adminToken`.
4. Run **Admin / Members / Create Member** and **Admin / Books / Create Book** to fill
   `memberCode` and `isbn` used by the requests that follow.

Requests with a path placeholder (`{{isbn}}`, `{{memberId}}`, `{{barcode}}`, `{{loanId}}`)
read the matching environment/runtime variable.

## Layout

- `Auth` — admin and member login / logout
- `Admin` — members, books, copies, shelves, circulation (uses `adminToken`)
- `Library` — semantic search and single book (uses `memberCode`)
- `Member` — personal pins and loans (uses `memberCode`)
