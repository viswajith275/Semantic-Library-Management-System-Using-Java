# Library frontend starter structure

## FXML pages

| File | Role | Purpose |
|---|---|---|
| `Login.fxml` | Shared | Login and temporary manual role selection. |
| `LibrarianBooks.fxml` | Librarian | Book catalogue CRUD. |
| `Members.fxml` | Librarian | Read-only/all-member directory. |
| `LibrarianBorrows.fxml` | Librarian | All borrowing records, due and overdue tracking. |
| `UserBooks.fxml` | User | Semantic book search, filters and borrow entry point. |
| `UserBorrows.fxml` | User | Current loans, days left/overdue status and history. |
| `BookForm.fxml` | Librarian | Optional reusable add/edit-book dialog. |

## Controller pairing

Each main FXML page has a same-purpose controller in `src/main/java/com/slms/controllers`.
The files intentionally contain detailed TODO comments only; add UI injection, event actions,
service calls and navigation when you are ready to implement the screens.

## Suggested implementation order

1. Build the Login layout, then a small session object and central router.
2. Build the User Books page and connect semantic search/filtering.
3. Build User Borrowings with active/overdue/history states.
4. Build librarian Books CRUD, Members, then Borrow records.
5. Replace the temporary role chooser with real backend authentication/authorisation.
