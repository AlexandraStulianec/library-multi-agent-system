import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class FineManagerAgent extends Agent {
    private static final double FINE_PER_DAY = 0.50; // $0.50 per day late
    private Map<String, Double> userFines = new HashMap<>();

    protected void setup() {
        System.out.println(getLocalName() + " started.");

        // Load existing fines from file
        loadFines();

        // Handle fine-related requests
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String content = msg.getContent();
                    String[] parts = content.split(":");
                    String response = "";

                    if (parts[0].equals("CALCULATE_FINE")) {
                        response = calculateFine(parts[1], parts[2]);
                    } else if (parts[0].equals("GET_TOTAL_FINES")) {
                        response = getTotalFines(parts[1]);
                    } else if (parts[0].equals("PAY_FINE")) {
                        response = payFine(parts[1], Double.parseDouble(parts[2]));
                    } else if (parts[0].equals("CHECK_OVERDUE_FINES")) {
                        response = checkAllOverdueFines();
                    } else if (parts[0].equals("GET_OVERDUE_BOOKS")) {
                        response = getOverdueBooks(parts[1]);
                    } else if (parts[0].equals("OVERDUE")) {
                        response = getOverdueBooks(parts[1]);
                    }

                    //  ONLY send a reply if the original message was a REQUEST
                    if (msg.getPerformative() == ACLMessage.REQUEST && !response.isEmpty()) {
                        ACLMessage reply = msg.createReply();
                        reply.setContent(response);
                        send(reply);
                    }
                } else {
                    block();
                }
            }
        });

        addBehaviour(new TickerBehaviour(this, 20000 ) { // Check for overdue books daily
            protected void onTick() {
                checkOverdueNotifications();
            }
        });

        // Daily check for fines
        addBehaviour(new TickerBehaviour(this, 20000000) { // Check daily
            protected void onTick() {
                updateFines();
            }
        });


    }

    private void checkOverdueNotifications() {
        List<String[]> books = DatabaseHelper.readBooks();
        List<String[]> users = DatabaseHelper.readUsers(); // Read users to map IDs to names

        for (String[] book : books) {
            if (book[3].equals("Borrowed")) {
                String dueDate = book[5];
                LocalDate due = LocalDate.parse(dueDate);
                if (due.isBefore(LocalDate.now())) {
                    String userId = book[4];
                    String userName = getUserName(userId, users);
                    System.out.println("[ALERT] Book overdue: " + book[0] +
                            " | Borrowed by: " + userName + " (ID: " + userId + ")" +
                            " | Due: " + book[5]);
                    // Here you could also trigger a message to BookManagerAgent if needed
                }
            }
        }
    }

    private String getUserName(String userId, List<String[]> users) {
        for (String[] user : users) {
            if (user[0].equalsIgnoreCase(userId)) {
                return user[1];
            }
        }
        return "Unknown";
    }


    private String getOverdueBooks(String userID) {
        List<String[]> books = DatabaseHelper.readBooks();
        StringBuilder sb = new StringBuilder("Overdue books for user " + userID + ":\n");

        boolean hasOverdue = false;
        for (String[] book : books) {
            if (book[3].equals("Borrowed") && book[4].equalsIgnoreCase(userID)) {
                LocalDate due = LocalDate.parse(book[5]);
                if (due.isBefore(LocalDate.now())) {
                    sb.append("- ").append(book[0]).append(" (Due: ").append(book[5]).append(")\n");
                    hasOverdue = true;
                }
            }
        }

        if (!hasOverdue) {
            sb.append("No overdue books.");
        }

        return sb.toString();
    }


    private String calculateFine(String userID, String bookTitle) {
        List<String[]> books = DatabaseHelper.readBooks();
        for (String[] book : books) {
            if (book[0].equalsIgnoreCase(bookTitle) && book[4].equalsIgnoreCase(userID)) {
                if (book[3].equals("Borrowed") && book.length > 5 && !book[5].isEmpty()) {
                    LocalDate dueDate = LocalDate.parse(book[5]);
                    if (dueDate.isBefore(LocalDate.now())) {
                        long daysLate = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
                        double fine = daysLate * FINE_PER_DAY;

                        // Add to user's total fines
                        userFines.put(userID, userFines.getOrDefault(userID, 0.0) + fine);
                        saveFines();

                        return "FINE:" + String.format("%.2f", fine) + " added for " + daysLate + " days late";
                    }
                }
            }
        }
        return "FINE:0.00 - No overdue fines";
    }

    private String getTotalFines(String userID) {
        double total = userFines.getOrDefault(userID, 0.0);
        if (total == 0) {
            return "TOTAL_FINES:$0.00 - No outstanding fines";
        }
        return "TOTAL_FINES:$" + String.format("%.2f", total);
    }

    private String payFine(String userID, double amount) {
        double currentFine = userFines.getOrDefault(userID, 0.0);
        if (amount <= 0) {
            return "PAYMENT_FAILED:Invalid amount";
        }

        if (amount >= currentFine) {
            userFines.remove(userID);
            saveFines();
            return "PAYMENT_SUCCESSFUL:Full payment of $" + String.format("%.2f", amount) + " received. Thank you!";
        } else {
            userFines.put(userID, currentFine - amount);
            saveFines();
            return "PAYMENT_PARTIAL:Paid $" + String.format("%.2f", amount) +
                    ". Remaining fine: $" + String.format("%.2f", currentFine - amount);
        }
    }

    private String checkAllOverdueFines() {
        List<String[]> books = DatabaseHelper.readBooks();
        StringBuilder report = new StringBuilder("Overdue Books Report:\n");
        boolean hasOverdue = false;

        for (String[] book : books) {
            if (book.length > 3 && book[3].equals("Borrowed") && book.length > 5 && !book[5].isEmpty()) {
                LocalDate dueDate = LocalDate.parse(book[5]);
                if (dueDate.isBefore(LocalDate.now())) {
                    long daysLate = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
                    double fine = daysLate * FINE_PER_DAY;
                    report.append(String.format("- %s (User: %s, Days Late: %d, Fine: $%.2f)\n",
                            book[0], book[4], daysLate, fine));
                    hasOverdue = true;
                }
            }
        }

        if (!hasOverdue) {
            report.append("No overdue books in the system.");
        }

        return report.toString();
    }

    private void updateFines() {
        List<String[]> books = DatabaseHelper.readBooks();
        for (String[] book : books) {
            if (book.length > 3 && book[3].equals("Borrowed") && book.length > 4 && !book[4].isEmpty()) {
                calculateFine(book[4], book[0]);
            }
        }
    }

    private void loadFines() {
        List<String[]> fines = DatabaseHelper.readFile("fines.txt");
        for (String[] fine : fines) {
            if (fine.length >= 2) {
                userFines.put(fine[0], Double.parseDouble(fine[1]));
            }
        }
    }

    private void saveFines() {
        DatabaseHelper.writeFines(userFines);
    }
}