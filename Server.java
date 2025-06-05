import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    // Stores username and corresponding output stream
    public static Map<String, PrintWriter> clients = new ConcurrentHashMap<>();

    // Send updated user list to all clients
    public static void broadcastUserList() {
        String userList = String.join(",", clients.keySet());
        for (PrintWriter writer : clients.values()) {
            writer.println("[USER LIST]" + userList);
        }
    }

    // Send message to all clients
    public static void broadcastMessage(String sender, String message) {
        for (PrintWriter writer : clients.values()) {
            writer.println(sender + ": " + message);
        }
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Server started on port 5000...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());
                new Thread(new Handler(clientSocket)).start();
            }

        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
