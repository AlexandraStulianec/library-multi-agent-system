import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import java.time.LocalDate;
import java.util.*;

public class BookManagerAgent extends Agent {
    private static final int MAX_BORROW_LIMIT = 3;

    protected void setup() {
        System.out.println(getLocalName() + " started.");

        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String[] parts = msg.getContent().split(":");
                    if (parts.length < 3) return;
                    String userID = parts[0];
                    String action = parts[1];
                    String bookTitle = parts[2];

                    if (action.equals("BORROW")) {
                        FSMBehaviour borrowFSM = createBorrowFSM(userID, bookTitle, msg);
                        addBehaviour(borrowFSM);
                    } else if (action.equals("RETURN")) {
                        String response = handleReturn(userID, bookTitle);
                        ACLMessage reply = msg.createReply();
                        reply.setContent(response);
                        send(reply);
                    }
                } else {
                    block();
                }
            }
        });
    }

    private FSMBehaviour createBorrowFSM(String userID, String bookTitle, ACLMessage originalMsg) {
        FSMBehaviour fsm = new FSMBehaviour(this);

        // Register states
        fsm.registerFirstState(new CheckAvailability(userID, bookTitle), "CheckAvailability");
        fsm.registerState(new CheckUserFines(userID), "CheckFines");
        fsm.registerState(new CheckBorrowLimit(userID), "CheckBorrowLimit");

        // Register final (last) states
        fsm.registerLastState(new DoBorrow(userID, bookTitle, originalMsg), "DoBorrow");
        fsm.registerLastState(new RejectBorrow(originalMsg, "Book not available"), "RejectNotAvailable");
        fsm.registerLastState(new RejectBorrow(originalMsg, "You have unpaid fines"), "RejectHasFines");
        fsm.registerLastState(new RejectBorrow(originalMsg, "Borrowing limit reached (max " + MAX_BORROW_LIMIT + " books)"), "RejectAtLimit");

        // Transitions
        fsm.registerTransition("CheckAvailability", "CheckFines", 0);      // available
        fsm.registerTransition("CheckAvailability", "RejectNotAvailable", 1); // not available

        fsm.registerTransition("CheckFines", "CheckBorrowLimit", 0);       // no fines
        fsm.registerTransition("CheckFines", "RejectHasFines", 1);         // has fines

        fsm.registerTransition("CheckBorrowLimit", "DoBorrow", 0);         // under limit
        fsm.registerTransition("CheckBorrowLimit", "RejectAtLimit", 1);    // at limit

        return fsm;
    }

    private class CheckAvailability extends OneShotBehaviour {
        private String userID, bookTitle;
        private boolean available;

        public CheckAvailability(String userID, String bookTitle) {
            this.userID = userID;
            this.bookTitle = bookTitle;
        }

        public void action() {
            List<String[]> books = DatabaseHelper.readBooks();
            for (String[] book : books) {
                if (book[0].equalsIgnoreCase(bookTitle) && book[3].equalsIgnoreCase("Available")) {
                    available = true;
                    return;
                }
            }
            available = false;
        }

        public int onEnd() {
            return available ? 0 : 1;
        }
    }

    private class CheckUserFines extends OneShotBehaviour {
        private String userID;
        private boolean hasFines;

        public CheckUserFines(String userID) {
            this.userID = userID;
        }

        public void action() {
            List<String[]> fines = DatabaseHelper.readFile("fines.txt");
            for (String[] fine : fines) {
                if (fine[0].equalsIgnoreCase(userID) && Double.parseDouble(fine[1]) > 0) {
                    hasFines = true;
                    return;
                }
            }
            hasFines = false;
        }

        public int onEnd() {
            return hasFines ? 1 : 0;
        }
    }

    private class CheckBorrowLimit extends OneShotBehaviour {
        private String userID;
        private boolean atLimit;

        public CheckBorrowLimit(String userID) {
            this.userID = userID;
        }

        public void action() {
            List<String[]> books = DatabaseHelper.readBooks();
            int count = 0;
            for (String[] book : books) {
                if (book.length > 4 && book[4].equalsIgnoreCase(userID) && book[3].equals("Borrowed")) {
                    count++;
                }
            }
            atLimit = (count >= MAX_BORROW_LIMIT);
        }

        public int onEnd() {
            return atLimit ? 1 : 0;
        }
    }

    private class DoBorrow extends OneShotBehaviour {
        private String userID, bookTitle;
        private ACLMessage originalMsg;

        public DoBorrow(String userID, String bookTitle, ACLMessage originalMsg) {
            this.userID = userID;
            this.bookTitle = bookTitle;
            this.originalMsg = originalMsg;
        }

        public void action() {
            String dueDate = LocalDate.now().plusWeeks(2).toString();
            DatabaseHelper.updateBookStatus(bookTitle, "Borrowed", userID, dueDate);
            sendToAnalytics("BORROW", userID, bookTitle, getGenre(bookTitle));

            ACLMessage reply = originalMsg.createReply();
            reply.setContent("SUCCESS:Book borrowed! Due date: " + dueDate);
            send(reply);
        }

        private String getGenre(String title) {
            List<String[]> books = DatabaseHelper.readBooks();
            for (String[] book : books) {
                if (book[0].equalsIgnoreCase(title)) return book[2];
            }
            return "Unknown";
        }
    }

    private class RejectBorrow extends OneShotBehaviour {
        private ACLMessage originalMsg;
        private String reason;

        public RejectBorrow(ACLMessage originalMsg, String reason) {
            this.originalMsg = originalMsg;
            this.reason = reason;
        }

        public void action() {
            ACLMessage reply = originalMsg.createReply();
            reply.setContent("FAIL:" + reason);
            send(reply);
        }
    }

    private String handleReturn(String userID, String bookTitle) {
        List<String[]> books = DatabaseHelper.readBooks();
        for (String[] book : books) {
            if (book[0].equalsIgnoreCase(bookTitle)) {
                if (!book[3].equals("Borrowed") || !book[4].equals(userID)) {
                    return "FAIL:Book not borrowed by this user";
                }
                String dueDate = book[5];
                if (!dueDate.isEmpty()) {
                    LocalDate due = LocalDate.parse(dueDate);
                    if (due.isBefore(LocalDate.now())) {
                        sendToFineManager(userID, bookTitle);
                    }
                }
                DatabaseHelper.updateBookStatus(bookTitle, "Available", "", "");
                sendToAnalytics("RETURN", userID, bookTitle, book[2]);
                return "SUCCESS:Book returned successfully!";
            }
        }
        return "FAIL:Book not found";
    }

    private void sendToAnalytics(String action, String userID, String bookTitle, String genre) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent(action + ":" + bookTitle + ":" + genre + ":" + userID);
        msg.addReceiver(new AID("AnalyticsAgent", AID.ISLOCALNAME));
        send(msg);
    }

    private void sendToFineManager(String userID, String bookTitle) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent("CALCULATE_FINE:" + userID + ":" + bookTitle);
        msg.addReceiver(new AID("FineManagerAgent", AID.ISLOCALNAME));
        send(msg);
    }
}