import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import java.time.LocalDate;
import java.util.*;

public class BookManagerAgent extends Agent {
    private Map<String, Queue<String>> reservations = new HashMap<>();

    protected void setup() {
        System.out.println(getLocalName() + " started.");
        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String[] parts = msg.getContent().split(":");
                    String userID = parts[0];
                    String action = parts[1];
                    String bookTitle = parts[2];
                    String response = processRequest(userID, action, bookTitle);

                    ACLMessage reply = msg.createReply();
                    reply.setContent(response);
                    send(reply);
                } else {
                    block();
                }
            }
        });

//        addBehaviour(new TickerBehaviour(this, 86400000) {
//            protected void onTick() {
//                checkReservations();
//            }
//        });
    }

    private String processRequest(String userID, String action, String bookTitle) {
        List<String[]> books = DatabaseHelper.readBooks();
        for (String[] book : books) {
            if (book[0].equalsIgnoreCase(bookTitle)) {
                switch (action) {
                    case "BORROW": return handleBorrow(userID, book);
                    // case "RESERVE": return handleReservation(userID, bookTitle);
                    case "RETURN": return handleReturn(userID, book);

                }
            }
        }
        return "Book not found";
    }

    private String handleBorrow(String userID, String[] book) {
        if (book[3].equalsIgnoreCase("Available")) {
            // Book is available, borrow it
            String dueDate = LocalDate.now().plusWeeks(2).toString();
            DatabaseHelper.updateBookStatus(book[0], "Borrowed", userID, dueDate);
            return "SUCCESS:" + dueDate;
        }
        return "FAIL:Book unavailable";
    }

    private String handleReturn(String userID, String[] book) {
        if (!book[3].equals("Borrowed") || !book[4].equals(userID)) {
            return "FAIL:Book not borrowed by user";
        }
        DatabaseHelper.updateBookStatus(book[0], "Available", "", "");
        return "SUCCESS:Book returned";

        // Optional: notify next in reservation queue
    }

//    private String handleReservation(String userID, String title) {
//        reservations.computeIfAbsent(title, k -> new LinkedList<>()).add(userID);
//        return "RESERVED:Position " + reservations.get(title).size();
//    }

//    private void checkReservations() {
//        List<String[]> books = DatabaseHelper.readBooks();
//        for (String[] book : books) {
//            if (book[3].equals("Available") && reservations.containsKey(book[0])) {
//                Queue<String> queue = reservations.get(book[0]);
//                if (!queue.isEmpty()) {
//                    String userID = queue.poll();
//                    System.out.println("[ALERT] " + userID + ": " + book[0] + " is now available!");
//                }
//            }
//        }
//    }
}
