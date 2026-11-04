import jade.core.AID;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LibraryGUI extends JFrame {
    private JTextField userIdField, bookTitleField;
    private JTextArea outputArea;
    private AgentContainer container;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LibraryGUI().initialize());
    }

    private void initialize() {
        setTitle("Library Management System - Multi-Agent System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 500);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(0, 3, 5, 5));

        // User ID and Book Title fields
        inputPanel.add(new JLabel("User ID:"));
        userIdField = new JTextField();
        inputPanel.add(userIdField);

        inputPanel.add(new JLabel("")); // spacer

        inputPanel.add(new JLabel("Book Title:"));
        bookTitleField = new JTextField();
        inputPanel.add(bookTitleField);

        inputPanel.add(new JLabel("")); // spacer

        // Action Buttons - Row 1
        JButton borrowBtn = new JButton("Borrow Book");
        borrowBtn.addActionListener(this::handleBorrow);
        inputPanel.add(borrowBtn);

        JButton returnBtn = new JButton("Return Book");
        returnBtn.addActionListener(this::handleReturn);
        inputPanel.add(returnBtn);

        // Row 2
        JButton recommendBtn = new JButton("Get Recommendations");
        recommendBtn.addActionListener(this::handleRecommend);
        inputPanel.add(recommendBtn);

        JButton checkOverdueBtn = new JButton("Check Overdue Books");
        checkOverdueBtn.addActionListener(this::handleCheckOverdue);
        inputPanel.add(checkOverdueBtn);

        // Row 3
        JButton checkFinesBtn = new JButton("Check Fines");
        checkFinesBtn.addActionListener(this::handleCheckFines);
        inputPanel.add(checkFinesBtn);

//        JButton payFineBtn = new JButton("Pay Fine");
//        payFineBtn.addActionListener(this::handlePayFine);
//        inputPanel.add(payFineBtn);

        // Row 4
        JButton inventoryBtn = new JButton("Inventory Report");
        inventoryBtn.addActionListener(this::handleInventoryReport);
        inputPanel.add(inventoryBtn);

        JButton statsBtn = new JButton("User Statistics");
        statsBtn.addActionListener(this::handleUserStats);
        inputPanel.add(statsBtn);

        // Row 5
        JButton addBookBtn = new JButton("Add New Book");
        addBookBtn.addActionListener(this::handleAddBook);
        inputPanel.add(addBookBtn);

        JButton popularBtn = new JButton("Popular Books");
        popularBtn.addActionListener(this::handlePopularBooks);
        inputPanel.add(popularBtn);

        panel.add(inputPanel, BorderLayout.NORTH);

        // Output Area
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        add(panel);
        setVisible(true);

        startJADE();

        // Load initial sample data
//        loadSampleData();
    }

    private void startJADE() {
        jade.core.Runtime rt = jade.core.Runtime.instance();

        ProfileImpl profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "true");

        container = rt.createMainContainer(profile);

        try {
            container.createNewAgent("BookManagerAgent", BookManagerAgent.class.getName(), null).start();
            container.createNewAgent("RecommendationAgent", RecommendationAgent.class.getName(), null).start();
            container.createNewAgent("FineManagerAgent", FineManagerAgent.class.getName(), null).start();
            container.createNewAgent("InventoryAgent", InventoryAgent.class.getName(), null).start();
            container.createNewAgent("AnalyticsAgent", AnalyticsAgent.class.getName(), null).start();

            outputArea.append("All agents started successfully!\n\n");
        } catch (StaleProxyException e) {
            e.printStackTrace();
            outputArea.append("Error starting agents: " + e.getMessage() + "\n");
        }
    }

    private void handleBorrow(ActionEvent e) {
        String userID = userIdField.getText().trim();
        String bookTitle = bookTitleField.getText().trim();

        if (userID.isEmpty() || bookTitle.isEmpty()) {
            outputArea.append("Error: Please fill all fields\n");
            return;
        }

        outputArea.append("[Request] User " + userID + " wants to borrow: " + bookTitle + "\n");

        new Thread(() -> {
            try {
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent(userID + ":BORROW:" + bookTitle);
                msg.addReceiver(new AID("BookManagerAgent", AID.ISLOCALNAME));
                container.acceptNewAgent("BorrowTemp", new jade.core.Agent() {
                    protected void setup() {
                        send(msg);
                        ACLMessage reply = blockingReceive();
                        SwingUtilities.invokeLater(() -> {
                            outputArea.append("[BookManager] " + reply.getContent() + "\n");
                            outputArea.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        });
                        doDelete();
                    }
                }).start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void handleReturn(ActionEvent e) {
        String userID = userIdField.getText().trim();
        String bookTitle = bookTitleField.getText().trim();

        if (userID.isEmpty() || bookTitle.isEmpty()) {
            outputArea.append("Error: Please fill all fields\n");
            return;
        }

        outputArea.append("[Request] User " + userID + " wants to return: " + bookTitle + "\n");

        new Thread(() -> {
            try {
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent(userID + ":RETURN:" + bookTitle);
                msg.addReceiver(new AID("BookManagerAgent", AID.ISLOCALNAME));
                container.acceptNewAgent("ReturnTemp", new jade.core.Agent() {
                    protected void setup() {
                        send(msg);
                        ACLMessage reply = blockingReceive();
                        SwingUtilities.invokeLater(() -> {
                            outputArea.append("[BookManager] " + reply.getContent() + "\n");
                            outputArea.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        });
                        doDelete();
                    }
                }).start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void handleRecommend(ActionEvent e) {
        String userID = userIdField.getText().trim();
        if (userID.isEmpty()) {
            outputArea.append("Error: Please enter User ID\n");
            return;
        }

        outputArea.append("[Request] Getting recommendations for user: " + userID + "\n");

        new Thread(() -> {
            try {
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent(userID);
                msg.addReceiver(new AID("RecommendationAgent", AID.ISLOCALNAME));
                container.acceptNewAgent("RecTemp", new jade.core.Agent() {
                    protected void setup() {
                        send(msg);
                        ACLMessage reply = blockingReceive();
                        SwingUtilities.invokeLater(() -> {
                            outputArea.append("[RecommendationAgent]\n" + reply.getContent() + "\n");
                            outputArea.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        });
                        doDelete();
                    }
                }).start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void handleCheckOverdue(ActionEvent e) {
        String userID = userIdField.getText().trim();
        if (userID.isEmpty()) {
            outputArea.append("Error: Please enter User ID\n");
            return;
        }

        outputArea.append("[Request] Checking overdue books for: " + userID + "\n");

        new Thread(() -> {
            try {
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent("OVERDUE:" + userID);
                msg.addReceiver(new AID("FineManagerAgent", AID.ISLOCALNAME));
                container.acceptNewAgent("OverdueTemp", new jade.core.Agent() {
                    protected void setup() {
                        send(msg);
                        ACLMessage reply = blockingReceive();
                        SwingUtilities.invokeLater(() -> {
                            outputArea.append("[FineManagerAgent]\n" + reply.getContent() + "\n");
                            outputArea.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        });
                        doDelete();
                    }
                }).start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void handleCheckFines(ActionEvent e) {
        String userID = userIdField.getText().trim();
        if (userID.isEmpty()) {
            outputArea.append("Error: Please enter User ID\n");
            return;
        }

        outputArea.append("[Request] Checking fines for user: " + userID + "\n");

        new Thread(() -> {
            try {
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent("GET_TOTAL_FINES:" + userID);
                msg.addReceiver(new AID("FineManagerAgent", AID.ISLOCALNAME));
                container.acceptNewAgent("FineCheckTemp", new jade.core.Agent() {
                    protected void setup() {
                        send(msg);
                        ACLMessage reply = blockingReceive();
                        SwingUtilities.invokeLater(() -> {
                            outputArea.append("[FineManager] " + reply.getContent() + "\n");
                            outputArea.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        });
                        doDelete();
                    }
                }).start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

//    private void handlePayFine(ActionEvent e) {
//        String userID = userIdField.getText().trim();
//        if (userID.isEmpty()) {
//            outputArea.append("❌ Error: Please enter User ID\n");
//            return;
//        }
//
//        String amountStr = JOptionPane.showInputDialog(this, "Enter payment amount ($):", "Pay Fine", JOptionPane.QUESTION_MESSAGE);
//        if (amountStr == null || amountStr.trim().isEmpty()) return;
//
//        try {
//            double amount = Double.parseDouble(amountStr.trim());
//            outputArea.append("💵 [Request] Processing payment of $" + amount + " for user: " + userID + "\n");
//
//            new Thread(() -> {
//                try {
//                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
//                    msg.setContent("PAY_FINE:" + userID + ":" + amount);
//                    msg.addReceiver(new AID("FineManagerAgent", AID.ISLOCALNAME));
//                    container.acceptNewAgent("FinePayTemp", new jade.core.Agent() {
//                        protected void setup() {
//                            send(msg);
//                            ACLMessage reply = blockingReceive();
//                            SwingUtilities.invokeLater(() -> {
//                                outputArea.append("[FineManager] " + reply.getContent() + "\n");
//                                outputArea.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
//                            });
//                            doDelete();
//                        }
//                    }).start();
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            }).start();
//        } catch (NumberFormatException ex) {
//            outputArea.append("❌ Error: Invalid amount\n");
//        }
//    }

    private void handleInventoryReport(ActionEvent e) {
        outputArea.append("[Request] Generating inventory report...\n");

        new Thread(() -> {
            try {
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent("GET_INVENTORY_REPORT");
                msg.addReceiver(new AID("InventoryAgent", AID.ISLOCALNAME));
                container.acceptNewAgent("InventoryTemp", new jade.core.Agent() {
                    protected void setup() {
                        send(msg);
                        ACLMessage reply = blockingReceive();
                        SwingUtilities.invokeLater(() -> {
                            outputArea.append("[InventoryAgent]\n" + reply.getContent() + "\n");
                            outputArea.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        });
                        doDelete();
                    }
                }).start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void handleUserStats(ActionEvent e) {
        String userID = userIdField.getText().trim();
        if (userID.isEmpty()) {
            outputArea.append("Error: Please enter User ID\n");
            return;
        }

        outputArea.append("[Request] Getting statistics for user: " + userID + "\n");

        new Thread(() -> {
            try {
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent("GET_USER_STATS:" + userID);
                msg.addReceiver(new AID("AnalyticsAgent", AID.ISLOCALNAME));
                container.acceptNewAgent("StatsTemp", new jade.core.Agent() {
                    protected void setup() {
                        send(msg);
                        ACLMessage reply = blockingReceive();
                        SwingUtilities.invokeLater(() -> {
                            outputArea.append("[AnalyticsAgent]\n" + reply.getContent() + "\n");
                            outputArea.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        });
                        doDelete();
                    }
                }).start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void handleAddBook(ActionEvent e) {
        JTextField titleField = new JTextField();
        JTextField authorField = new JTextField();
        JTextField genreField = new JTextField();
        JTextField copiesField = new JTextField();

        Object[] message = {
                "Title:", titleField,
                "Author:", authorField,
                "Genre:", genreField,
                "Number of Copies:", copiesField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Add New Book", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                String title = titleField.getText().trim();
                String author = authorField.getText().trim();
                String genre = genreField.getText().trim();
                int copies = Integer.parseInt(copiesField.getText().trim());

                outputArea.append("[Request] Adding " + copies + " copy/copies of '" + title + "'\n");

                new Thread(() -> {
                    try {
                        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                        msg.setContent("ADD_BOOK:" + title + ":" + author + ":" + genre + ":" + copies);
                        msg.addReceiver(new AID("InventoryAgent", AID.ISLOCALNAME));
                        container.acceptNewAgent("AddBookTemp", new jade.core.Agent() {
                            protected void setup() {
                                send(msg);
                                ACLMessage reply = blockingReceive();
                                SwingUtilities.invokeLater(() -> {
                                    outputArea.append("[InventoryAgent] " + reply.getContent() + "\n");
                                    outputArea.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                                });
                                doDelete();
                            }
                        }).start();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
            } catch (NumberFormatException ex) {
                outputArea.append("Error: Invalid number of copies\n");
            }
        }
    }

    private void handlePopularBooks(ActionEvent e) {
        outputArea.append("[Request] Fetching most popular books...\n");

        new Thread(() -> {
            try {
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent("GET_POPULAR_BOOKS:5");
                msg.addReceiver(new AID("AnalyticsAgent", AID.ISLOCALNAME));
                container.acceptNewAgent("PopularTemp", new jade.core.Agent() {
                    protected void setup() {
                        send(msg);
                        ACLMessage reply = blockingReceive();
                        SwingUtilities.invokeLater(() -> {
                            outputArea.append("[AnalyticsAgent]\n" + reply.getContent() + "\n");
                            outputArea.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        });
                        doDelete();
                    }
                }).start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }
}