import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import java.util.*;

public class InventoryAgent extends Agent {
    private static final int LOW_STOCK_THRESHOLD = 3;
    private Map<String, Integer> bookCopies = new HashMap<>();
    private Map<String, Integer> borrowedCopies = new HashMap<>();

    protected void setup() {
        System.out.println(getLocalName() + " started.");

        // Initialize inventory
        loadInventory();

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String content = msg.getContent();
                    String[] parts = content.split(":");
                    String response = "";

                    if (parts[0].equals("CHECK_AVAILABILITY")) {
                        response = checkAvailability(parts[1]); // bookTitle
                    } else if (parts[0].equals("ADD_BOOK")) {
                        response = addBook(parts[1], parts[2], parts[3], Integer.parseInt(parts[4])); // title, author, genre, copies
                    } else if (parts[0].equals("REMOVE_BOOK")) {
                        response = removeBook(parts[1]); // bookTitle
                    } else if (parts[0].equals("GET_INVENTORY_REPORT")) {
                        response = generateInventoryReport();
                    } else if (parts[0].equals("UPDATE_COPIES")) {
                        response = updateCopies(parts[1], Integer.parseInt(parts[2])); // bookTitle, change
                    }

                    ACLMessage reply = msg.createReply();
                    reply.setContent(response);
                    send(reply);
                } else {
                    block();
                }
            }
        });

        // Weekly inventory check
        addBehaviour(new TickerBehaviour(this, 604800000) { // 7 days
            protected void onTick() {
                checkLowStock();
            }
        });
    }

    private String checkAvailability(String bookTitle) {
        int available = bookCopies.getOrDefault(bookTitle, 0) - borrowedCopies.getOrDefault(bookTitle, 0);
        return "AVAILABILITY:" + bookTitle + " has " + available + " copies available (Total: " +
                bookCopies.getOrDefault(bookTitle, 0) + ", Borrowed: " +
                borrowedCopies.getOrDefault(bookTitle, 0) + ")";
    }

    private String addBook(String title, String author, String genre, int copies) {
        List<String[]> books = DatabaseHelper.readBooks();

        for (int i = 0; i < copies; i++) {
            String[] newBook = {title, author, genre, "Available", "", ""};
            books.add(newBook);
        }

        DatabaseHelper.writeAllBooks(books);
        bookCopies.put(title, bookCopies.getOrDefault(title, 0) + copies);

        return "SUCCESS:Added " + copies + " copy/copies of '" + title + "' to inventory";
    }

    private String removeBook(String bookTitle) {
        List<String[]> books = DatabaseHelper.readBooks();
        List<String[]> remainingBooks = new ArrayList<>();

        for (String[] book : books) {
            if (!book[0].equalsIgnoreCase(bookTitle) || !book[3].equals("Available")) {
                remainingBooks.add(book);
            }
        }

        int removed = books.size() - remainingBooks.size();
        if (removed > 0) {
            DatabaseHelper.writeAllBooks(remainingBooks);
            bookCopies.put(bookTitle, bookCopies.getOrDefault(bookTitle, 0) - removed);
            return "SUCCESS:Removed " + removed + " copy/copies of '" + bookTitle + "' from inventory";
        }
        return "FAIL:No available copies to remove";
    }

    private String generateInventoryReport() {
        List<String[]> books = DatabaseHelper.readBooks();
        Map<String, Integer> totalCopies = new HashMap<>();
        Map<String, Integer> borrowedCopiesMap = new HashMap<>();

        for (String[] book : books) {
            String title = book[0];
            totalCopies.put(title, totalCopies.getOrDefault(title, 0) + 1);
            if (book[3].equals("Borrowed")) {
                borrowedCopiesMap.put(title, borrowedCopiesMap.getOrDefault(title, 0) + 1);
            }
        }

        StringBuilder report = new StringBuilder("╔══════════════════════════════════════════════════════════╗\n");
        report.append("║                    INVENTORY REPORT                         ║\n");
        report.append("╚══════════════════════════════════════════════════════════════╝\n\n");

        for (String title : totalCopies.keySet()) {
            int total = totalCopies.get(title);
            int borrowed = borrowedCopiesMap.getOrDefault(title, 0);
            int available = total - borrowed;
            report.append(String.format("%s\n", title));
            report.append(String.format("   Total: %d | Available: %d | Borrowed: %d\n", total, available, borrowed));

            if (available < LOW_STOCK_THRESHOLD) {
                report.append("   LOW STOCK - Order more copies!\n");
            }
            report.append("\n");
        }

        if (totalCopies.isEmpty()) {
            report.append("No books in inventory.\n");
        }

        return report.toString();
    }

    private String updateCopies(String bookTitle, int change) {
        List<String[]> books = DatabaseHelper.readBooks();
        List<String[]> updatedBooks = new ArrayList<>();

        if (change > 0) {
            // Add copies - need template from existing book
            String[] template = findBookTemplate(books, bookTitle);
            if (template != null) {
                for (int i = 0; i < change; i++) {
                    String[] newBook = Arrays.copyOf(template, template.length);
                    newBook[3] = "Available";
                    newBook[4] = "";
                    newBook[5] = "";
                    updatedBooks.add(newBook);
                }
            }
        }

        // Keep existing books (except those being removed if change is negative)
        for (String[] book : books) {
            if (!book[0].equalsIgnoreCase(bookTitle) || change >= 0) {
                updatedBooks.add(book);
            } else if (change < 0 && book[3].equals("Available")) {
                // Remove available copies only
                change++;
                if (change >= 0) {
                    continue; // Skip adding this book
                }
                updatedBooks.add(book);
            } else {
                updatedBooks.add(book);
            }
        }

        DatabaseHelper.writeAllBooks(updatedBooks);
        loadInventory(); // Reload inventory counts
        return "SUCCESS:Inventory updated for '" + bookTitle + "'";
    }

    private String[] findBookTemplate(List<String[]> books, String bookTitle) {
        for (String[] book : books) {
            if (book[0].equalsIgnoreCase(bookTitle)) {
                return book;
            }
        }
        return null;
    }

    private void checkLowStock() {
        List<String[]> books = DatabaseHelper.readBooks();
        Map<String, Integer> availableCount = new HashMap<>();

        for (String[] book : books) {
            if (book[3].equals("Available")) {
                availableCount.put(book[0], availableCount.getOrDefault(book[0], 0) + 1);
            }
        }

        for (Map.Entry<String, Integer> entry : availableCount.entrySet()) {
            if (entry.getValue() < LOW_STOCK_THRESHOLD) {
                System.out.println("[INVENTORY ALERT] Low stock for '" + entry.getKey() +
                        "' - Only " + entry.getValue() + " copies available");
            }
        }
    }

    private void loadInventory() {
        List<String[]> books = DatabaseHelper.readBooks();
        bookCopies.clear();
        borrowedCopies.clear();

        for (String[] book : books) {
            String title = book[0];
            bookCopies.put(title, bookCopies.getOrDefault(title, 0) + 1);
            if (book[3].equals("Borrowed")) {
                borrowedCopies.put(title, borrowedCopies.getOrDefault(title, 0) + 1);
            }
        }
    }
}