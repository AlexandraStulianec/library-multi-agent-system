import java.io.*;
import java.util.*;

public class DatabaseHelper {

    private static final String BOOKS_FILE = "books.txt";
    private static final String USERS_FILE = "users.txt";

    public static List<String[]> readBooks() {
        return readFile(BOOKS_FILE);
    }

    public static List<String[]> readUsers() {
        return readFile(USERS_FILE);
    }

    private static List<String[]> readFile(String filename) {
        List<String[]> data = new ArrayList<>();
        File file = new File(filename);

        // Create the file if it doesn't exist
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Read the file and return the data
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                data.add(line.split(","));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static void updateBookStatus(String title, String status, String userID, String dueDate) {
        List<String[]> books = readBooks();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(BOOKS_FILE))) {
            for (String[] book : books) {
                if (book[0].equalsIgnoreCase(title)) {
                    // Expand array if too short
                    if (book.length < 6) {
                        book = Arrays.copyOf(book, 6);
                    }
                    book[3] = status;
                    book[4] = userID;
                    book[5] = dueDate;
                }

                // Trim line when available
                if (book[3].equalsIgnoreCase("Available")) {
                    bw.write(String.join(",", Arrays.copyOf(book, 4)));
                } else {
                    bw.write(String.join(",", Arrays.copyOf(book, 6)));
                }
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
