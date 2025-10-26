import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

import java.time.LocalDate;
import java.util.List;

public class RecommendationAgent extends Agent {
    protected void setup() {
        System.out.println(getLocalName() + " started.");
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String content = msg.getContent();
                    ACLMessage reply = msg.createReply();

                    if (content.startsWith("OVERDUE:")) {
                        String userID = content.split(":")[1];
                        String overdue = getOverdueBooks(userID);
                        reply.setContent(overdue);
                    } else {
                        String genre = getFavoriteGenre(content);
                        String recs = generateRecommendations(genre);
                        reply.setContent(recs);
                    }

                    send(reply);
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


    private String getFavoriteGenre(String userID) {
        List<String[]> users = DatabaseHelper.readUsers();
        for (String[] user : users) {
            if (user[0].equalsIgnoreCase(userID)) {
                String genre = user[2];
                //System.out.println("User " + userID + " favorite genre: " + genre); // Debug line
                return genre;
            }
        }
        return "General";
    }


    private String generateRecommendations(String genre) {
        List<String[]> books = DatabaseHelper.readBooks();
        StringBuilder sb = new StringBuilder("Recommended " + genre + " books:\n");

        for (String[] book : books) {
            //System.out.println("Checking book: " + book[0] + " with genre: " + book[2]); // Debug line
            if (book[2].equalsIgnoreCase(genre) && book[3].equalsIgnoreCase("Available")) {
                sb.append("- ").append(book[0]).append(" by ").append(book[1]).append("\n");
            }
        }

        if (sb.toString().equals("Recommended " + genre + " books:\n")) {
            sb.append("No available books in this genre. Try another genre!");
        }

        return sb.toString();
    }

}
