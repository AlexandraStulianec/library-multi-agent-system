# library-multi-agent-system

This project is a multi-agent system built using **JADE** to manage a library system. The system can:

- Borrow and return books based on user ID and book title.
- Recommend books based on the user’s favourite genre.
- Detect overdue books and calculate fines automatically.
- Track inventory, add new books, and show popular books.
- Show user statistics and borrowing history.


## Required

- JADE framework
- An IDE (IntelliJ IDEA is used here)


## File Structure

- `users.txt` – Format: userId, userName, favoriteGenre  
- `books.txt` – Format: title, author, genre, status, userId, dueDate (userId and dueDate empty when available)  
- `fines.txt` – Format: userId, fineAmount  
- `borrow_history.txt` – Format: title, genre, userId, borrowDate, returnDate (NULL if not returned)


## How to Run

1. Download JADE and add it to your project’s module settings.
2. Run `LibraryGUI.java`. This starts the GUI and the JADE platform.


## What It Contains

The GUI has buttons for borrowing, returning, recommendations, check overdue books, check fines, inventory report, user stats, adding books, and popular books.
You type a user ID and a book title, then press the button you want.
Recommendations are based on the user’s favourite genre from `users.txt`.
Overdue books are checked automatically every 20 seconds, and an alert appears in the console: [ALERT] Book overdue: <title> | Borrowed by: userName (ID: userId) | Due: dueDate
You can also press “Check Overdue Books” to see overdue books for a specific user.
Fines are $0.50 per day late and are saved in `fines.txt`.
The inventory agent tracks how many copies of each book are available and alerts when stock is low (less than 3 copies).
The analytics agent records every borrow and return in `borrow_history.txt` and shows popular books and user statistics.

The FSM in BookManagerAgent makes decisions in sequence:
1. Is the book available? -> if no, reject immediately.
2. Does the user have unpaid fines? -> if yes, reject.
3. Has the user already borrowed 3 books? -> if yes, reject.
4. Otherwise, allow borrowing.

```text
Start
  │
  ▼
1. CheckAvailability
  │
  ├── Book available? ── YES ──▶ 2. CheckFines
  │
  └── NO ──▶ RejectNotAvailable (final)
                └─ "Book not available"

2. CheckFines
  │
  ├── No unpaid fines? ── YES ──▶ 3. CheckBorrowLimit
  │
  └── Has fines ──▶ RejectHasFines (final)
                       └─ "You have unpaid fines"

3. CheckBorrowLimit
  │
  ├── Borrowed < 3 books? ── YES ──▶ DoBorrow (final)
  │                                     └─ "Success, due date set"
  │
  └── Borrowed >= 3 books ──▶ RejectAtLimit (final)
                                 └─ "Borrowing limit reached (max 3 books)"
```

## Agents (5 types)

- BookManagerAgent – handles borrow and return requests.
- RecommendationAgent – gives book recommendations.
- FineManagerAgent – checks overdue books and calculates fines.
- InventoryAgent – manages book copies and stock.
- AnalyticsAgent – records borrowing history and provides statistics.
