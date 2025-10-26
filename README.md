# library-multi-agent-system

## Library Agent System (JADE-based)

This project is a multi-agent system built using **JADE** to manage a library system. The system can:

* Respond with book recommendations based on the user's favorite genre.
* Detect overdue borrowed books and report them automatically or through the GUI, if the button is pressed.
* Borrow/Return books based on the title and user ID.

---

## Required

* JADE Framework
* An IDE - for this I used IntelliJ IDEA

---

## File Structure

users.txt      // Format: userId, userName, favoriteGenre
books.txt      // Format: title, author, genre, status[, userId, dueDate] - only when borrowed

---

## How to Run

1. Download JADE library and add it to your Module Settings in the project.

2. Run the project, specifically LibraryGUI.java. This starts the GUI and the JADE platform.

---

## What It Contains

In the GUI, if you type the userID you can get recommendations of books based on their favourite genre (which is in their file - users.txt).

You can borrow/return books based on the userID and the title of the book, with the appropriate buttons. 

You can also check the overdue books by entering first the userID, and then press the button.

Every 30 seconds, there are checks if any borrowed books are past their due date, and an alert is printed in the console ( [ALERT] Book overdue: <title> | Borrowed by: <username> (ID: <userID>) | Due: <date> ).
