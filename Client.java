import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class Client {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton darkModeButton;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    private boolean darkMode = false;
    private volatile boolean running = true;

    public Client(String username) {
        this.username = username;
        initializeUI();

        try {
            socket = new Socket("localhost", 5000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Send username to server
            out.println(username);

            // Wait for server response to accept/reject username
            String response = in.readLine();
            if (response == null) {
                throw new IOException("Server closed connection");
            }

            if (response.equalsIgnoreCase("USERNAME ACCEPTED")) {
                chatArea.append("Connected as " + username + "\n");
                frame.setVisible(true);
                new Thread(this::receiveMessages).start();
            } else if (response.startsWith("USERNAME REJECTED")) {
                JOptionPane.showMessageDialog(null, "Username rejected: " + response.substring("USERNAME_REJECTED".length()));
                closeConnection();
                System.exit(0);
            } else {
                throw new IOException("Unexpected server response: " + response);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not connect to server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void initializeUI() {
        frame = new JFrame("Chat - " + username);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Comic Sans MS", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.setEnabled(false);
        darkModeButton = new JButton("Dark Mode");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        bottomPanel.add(darkModeButton, BorderLayout.WEST);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setBorder(BorderFactory.createTitledBorder("Online Users"));
        userList.setPreferredSize(new Dimension(100, 0));

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.add(userList, BorderLayout.EAST);


        messageField.getDocument().addDocumentListener(
                (SimpleDocumentListener) () -> sendButton.setEnabled(!messageField.getText().trim().isEmpty())
        );

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        darkModeButton.addActionListener(e -> toggleDarkMode());
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && running) {
            out.println(message);
            messageField.setText("");
        }
    }

    private void receiveMessages() {
        try {
            String message;
            while (running && (message = in.readLine()) != null) {
                if (message.startsWith("[USER LIST]")) {
                    updateUserList(message.substring(10));
                } else {
                    chatArea.append(message + "\n");
                }
            }
        } catch (IOException e) {
            if (running) {
                showDisconnectedMessage();
            }
        } finally {
            closeConnection();
        }
    }

    private void updateUserList(String list) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            if (!list.isEmpty()) {
                for (String user : list.split(",")) {
                    userListModel.addElement(user);
                }
            }
        });
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;
        Color bg = darkMode ? new Color(35, 34, 34) : Color.WHITE;
        Color fg = darkMode ? Color.WHITE : Color.BLACK;

        chatArea.setBackground(bg);
        chatArea.setForeground(fg);
        messageField.setBackground(bg);
        messageField.setForeground(fg);
        userList.setBackground(bg);
        userList.setForeground(fg);
        frame.getContentPane().setBackground(bg);
    }

    private void showDisconnectedMessage() {
        SwingUtilities.invokeLater(() -> {
            chatArea.append("Disconnected from server.\n");
            sendButton.setEnabled(false);
            messageField.setEnabled(false);
            JOptionPane.showMessageDialog(frame, "Disconnected from server.", "Connection Lost", JOptionPane.ERROR_MESSAGE);
        });
    }

    private void closeConnection() {
        running = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }


    @FunctionalInterface
    interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update();
        @Override default void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
        @Override default void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
        @Override default void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String username = JOptionPane.showInputDialog(null, "Enter your username:", "Login", JOptionPane.PLAIN_MESSAGE);
            if (username != null) {
                username = username.trim();
                if (!username.isEmpty() && !username.contains(",")) {
                    new Client(username);
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid username. Commas and empty names not allowed.");
                }
            } else {
                JOptionPane.showMessageDialog(null, "Username is required to join the chat.");
            }
        });
    }
}
