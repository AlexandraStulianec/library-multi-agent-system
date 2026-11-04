import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AnalyticsAgent extends Agent {
    private Map<String, Integer> borrowCount = new HashMap<>();
    private Map<String, Integer> genrePopularity = new HashMap<>();

    // Use separate ArrayLists instead of an inner class
    private List<String> historyBookTitles = new ArrayList<>();
    private List<String> historyGenres = new ArrayList<>();
    private List<String> historyUserIDs = new ArrayList<>();
    private List<LocalDate> historyBorrowDates = new ArrayList<>();
    private List<LocalDate> historyReturnDates = new ArrayList<>();

    protected void setup() {
        System.out.println(getLocalName() + " started.");

        // Load historical data
        loadBorrowHistory();

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String content = msg.getContent();
                    String[] parts = content.split(":");
                    String response = "";

                    if (parts[0].equals("GET_POPULAR_BOOKS")) {
                        response = getPopularBooks(Integer.parseInt(parts[1]));
                    } else if (parts[0].equals("GET_GENRE_TRENDS")) {
                        response = getGenreTrends();
                    } else if (parts[0].equals("GET_USER_STATS")) {
                        response = getUserStats(parts[1]);
                    } else if (parts[0].equals("GET_ANNUAL_REPORT")) {
                        response = generateAnnualReport();
                    } else if (parts[0].equals("BORROW")) {
                        // Direct message from BookManagerAgent: BORROW:bookTitle:genre:userID
                        if (parts.length >= 4) {
                            recordBorrow(parts[1], parts[2], parts[3]);
                            response = "RECORDED";
                        }
                    } else if (parts[0].equals("RETURN")) {
                        // Direct message from BookManagerAgent: RETURN:bookTitle:genre:userID
                        if (parts.length >= 4) {
                            recordReturn(parts[1], parts[3]);
                            response = "RECORDED";
                        }
                    }

                    if (!response.isEmpty()) {
                        ACLMessage reply = msg.createReply();
                        reply.setContent(response);
                        send(reply);
                    }
                } else {
                    block();
                }
            }
        });

        // Monthly analytics report
        addBehaviour(new TickerBehaviour(this, 2592000000L) {
            protected void onTick() {
                generateMonthlyReport();
            }
        });
    }

    private String getPopularBooks(int limit) {
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(borrowCount.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        StringBuilder sb = new StringBuilder("═══════════════════════════════════════════════\n");
        sb.append("      TOP " + limit + " MOST POPULAR BOOKS\n");
        sb.append("═══════════════════════════════════════════════\n\n");

        if (sorted.isEmpty()) {
            sb.append("No borrowing data available yet.\n");
        } else {
            for (int i = 0; i < Math.min(limit, sorted.size()); i++) {
                Map.Entry<String, Integer> entry = sorted.get(i);
                sb.append((i + 1) + ". " + entry.getKey() + "\n");
                sb.append("   Borrowed " + entry.getValue() + " times\n\n");
            }
        }

        return sb.toString();
    }

    private String getGenreTrends() {
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(genrePopularity.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int totalBorrows = historyBorrowDates.size();
        StringBuilder sb = new StringBuilder("═══════════════════════════════════════════════\n");
        sb.append("           GENRE POPULARITY TRENDS\n");
        sb.append("═══════════════════════════════════════════════\n\n");

        if (sorted.isEmpty()) {
            sb.append("No genre data available yet.\n");
        } else {
            for (Map.Entry<String, Integer> entry : sorted) {
                double percentage = totalBorrows > 0 ? (entry.getValue() * 100.0) / totalBorrows : 0;
                sb.append(String.format("%-15s %3d (%.1f%%)\n",
                        entry.getKey() + ":", entry.getValue(), percentage));
            }
        }

        return sb.toString();
    }

    private void recordBorrow(String bookTitle, String genre, String userID) {
        // Update counts
        borrowCount.put(bookTitle, borrowCount.getOrDefault(bookTitle, 0) + 1);
        genrePopularity.put(genre, genrePopularity.getOrDefault(genre, 0) + 1);

        // Store in history lists
        historyBookTitles.add(bookTitle);
        historyGenres.add(genre);
        historyUserIDs.add(userID);
        historyBorrowDates.add(LocalDate.now());
        historyReturnDates.add(null); // Not returned yet

        saveBorrowHistory();
        System.out.println("[Analytics] Recorded borrow: " + bookTitle + " by " + userID);
    }

    private void recordReturn(String bookTitle, String userID) {
        // Find the most recent borrow record without return date for this user and book
        for (int i = historyBorrowDates.size() - 1; i >= 0; i--) {
            if (historyBookTitles.get(i).equalsIgnoreCase(bookTitle) &&
                    historyUserIDs.get(i).equalsIgnoreCase(userID) &&
                    historyReturnDates.get(i) == null) {
                historyReturnDates.set(i, LocalDate.now());
                saveBorrowHistory();
                System.out.println("[Analytics] Recorded return: " + bookTitle + " by " + userID);
                return;
            }
        }
        System.out.println("[Analytics] Warning: No borrow record found for return: " + bookTitle + " by " + userID);
    }

    private String getUserStats(String userID) {
        int totalBorrows = 0;
        int currentBorrows = 0;
        Set<String> uniqueGenres = new HashSet<>();
        Map<String, Integer> genreCount = new HashMap<>();
        Set<String> uniqueBooks = new LinkedHashSet<>();

        for (int i = 0; i < historyUserIDs.size(); i++) {
            if (historyUserIDs.get(i).equalsIgnoreCase(userID)) {
                totalBorrows++;
                String genre = historyGenres.get(i);
                uniqueGenres.add(genre);
                genreCount.put(genre, genreCount.getOrDefault(genre, 0) + 1);
                uniqueBooks.add(historyBookTitles.get(i));

                if (historyReturnDates.get(i) == null) {
                    currentBorrows++;
                }
            }
        }

        StringBuilder sb = new StringBuilder("═══════════════════════════════════════════════\n");
        sb.append("      USER STATISTICS FOR " + userID + "\n");
        sb.append("═══════════════════════════════════════════════\n\n");
        sb.append("Total books borrowed: ").append(totalBorrows).append("\n");
        sb.append("Currently borrowed: ").append(currentBorrows).append("\n");
        sb.append("Genres explored: ").append(uniqueGenres.size()).append("\n");

        // Find favorite genre
        String favGenre = "None";
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : genreCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                favGenre = entry.getKey();
            }
        }
        sb.append("Favorite genre: ").append(favGenre);
        if (maxCount > 0) {
            sb.append(" (").append(maxCount).append(" books)");
        }
        sb.append("\n\n");

        if (!uniqueBooks.isEmpty()) {
            sb.append("Reading history:\n");
            for (String book : uniqueBooks) {
                sb.append("   • ").append(book).append("\n");
            }
        }

        if (totalBorrows == 0) {
            sb.append("This user hasn't borrowed any books yet.\n");
        }

        return sb.toString();
    }

    private String generateAnnualReport() {
        int year = LocalDate.now().getYear();
        int[] monthlyBorrows = new int[12];
        Map<String, Integer> yearlyGenreStats = new HashMap<>();
        int totalBorrows = 0;

        for (int i = 0; i < historyBorrowDates.size(); i++) {
            LocalDate borrowDate = historyBorrowDates.get(i);
            if (borrowDate.getYear() == year) {
                monthlyBorrows[borrowDate.getMonthValue() - 1]++;
                yearlyGenreStats.put(historyGenres.get(i),
                        yearlyGenreStats.getOrDefault(historyGenres.get(i), 0) + 1);
                totalBorrows++;
            }
        }

        StringBuilder sb = new StringBuilder("═══════════════════════════════════════════════\n");
        sb.append("         ANNUAL REPORT " + year + "\n");
        sb.append("═══════════════════════════════════════════════\n\n");
        sb.append("Total Borrows: ").append(totalBorrows).append("\n\n");

        if (totalBorrows > 0) {
            sb.append("Monthly Breakdown:\n");
            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            for (int i = 0; i < 12; i++) {
                if (monthlyBorrows[i] > 0) {
                    sb.append(String.format("  %s: %d borrows\n", months[i], monthlyBorrows[i]));
                }
            }

            sb.append("\nGenre Breakdown:\n");
            for (Map.Entry<String, Integer> entry : yearlyGenreStats.entrySet()) {
                double percentage = (entry.getValue() * 100.0) / totalBorrows;
                sb.append(String.format("  %s: %d (%.1f%%)\n",
                        entry.getKey(), entry.getValue(), percentage));
            }
        } else {
            sb.append("No borrowing data for " + year + "\n");
        }

        return sb.toString();
    }

    private void generateMonthlyReport() {
        System.out.println("[ANALYTICS] Monthly report generated:\n" + generateAnnualReport());
    }

    private void loadBorrowHistory() {
        List<String[]> history = DatabaseHelper.readFile("borrow_history.txt");
        for (String[] record : history) {
            if (record.length >= 5) {
                historyBookTitles.add(record[0]);
                historyGenres.add(record[1]);
                historyUserIDs.add(record[2]);
                historyBorrowDates.add(LocalDate.parse(record[3]));

                LocalDate returnDate = record[4].equals("NULL") ? null : LocalDate.parse(record[4]);
                historyReturnDates.add(returnDate);

                // Update counts
                borrowCount.put(record[0], borrowCount.getOrDefault(record[0], 0) + 1);
                genrePopularity.put(record[1], genrePopularity.getOrDefault(record[1], 0) + 1);
            }
        }
        System.out.println("[Analytics] Loaded " + historyBookTitles.size() + " borrow records");
    }

    private void saveBorrowHistory() {
        List<String[]> historyData = new ArrayList<>();
        for (int i = 0; i < historyBookTitles.size(); i++) {
            String[] row = {
                    historyBookTitles.get(i),
                    historyGenres.get(i),
                    historyUserIDs.get(i),
                    historyBorrowDates.get(i).toString(),
                    historyReturnDates.get(i) == null ? "NULL" : historyReturnDates.get(i).toString()
            };
            historyData.add(row);
        }
        DatabaseHelper.writeToFile("borrow_history.txt", historyData);
    }
}