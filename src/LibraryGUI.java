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
        setTitle("Library System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 400);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inputPanel = new JPanel(new GridLayout(4, 3, 5, 5));
        inputPanel.add(new JLabel("User ID:"));
        userIdField = new JTextField();
        inputPanel.add(userIdField);

        inputPanel.add(new JLabel("Book Title:"));
        bookTitleField = new JTextField();
        inputPanel.add(bookTitleField);

        JButton borrowBtn = new JButton("Borrow");
        borrowBtn.addActionListener(this::handleBorrow);
        inputPanel.add(borrowBtn);

        JButton returnBtn = new JButton("Return");
        returnBtn.addActionListener(this::handleReturn);
        inputPanel.add(returnBtn);


        JButton recommendBtn = new JButton("Get Recommendations");
        recommendBtn.addActionListener(this::handleRecommend);
        inputPanel.add(recommendBtn);

        JButton checkOverdueBtn = new JButton("Check Overdue Books");
        checkOverdueBtn.addActionListener(this::handleCheckOverdue);
        inputPanel.add(checkOverdueBtn);

        panel.add(inputPanel, BorderLayout.NORTH);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        panel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        add(panel);
        setVisible(true);

        startJADE();
    }

    private void startJADE() {
        jade.core.Runtime rt = jade.core.Runtime.instance();

        ProfileImpl profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "true");

        container = rt.createMainContainer(profile);

        try {
            container.createNewAgent("BookManagerAgent", BookManagerAgent.class.getName(), null).start();
            container.createNewAgent("RecommendationAgent", RecommendationAgent.class.getName(), null).start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
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
                        SwingUtilities.invokeLater(() -> outputArea.append("[Agent] " + reply.getContent() + "\n"));
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
                        SwingUtilities.invokeLater(() -> outputArea.append("[Agent] " + reply.getContent() + "\n"));
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

        outputArea.append("[Request] Getting recommendations for the user with ID: " + userID + "\n");

        new Thread(() -> {
            try {
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent(userID);
                msg.addReceiver(new AID("RecommendationAgent", AID.ISLOCALNAME));
                container.acceptNewAgent("RecTemp", new jade.core.Agent() {
                    protected void setup() {
                        send(msg);
                        ACLMessage reply = blockingReceive();
                        SwingUtilities.invokeLater(() -> outputArea.append(reply.getContent() + "\n"));
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
                msg.addReceiver(new AID("RecommendationAgent", AID.ISLOCALNAME));
                container.acceptNewAgent("OverdueTemp", new jade.core.Agent() {
                    protected void setup() {
                        send(msg);
                        ACLMessage reply = blockingReceive();
                        SwingUtilities.invokeLater(() -> outputArea.append(reply.getContent() + "\n"));
                        doDelete();
                    }
                }).start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

}
