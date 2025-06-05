import java.io.*;
import java.net.*;

public class Handler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public Handler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            username = in.readLine(); // Receive username

            if (username == null || username.trim().isEmpty() || Server.clients.containsKey(username)) {
                out.println("USERNAME REJECTED: Name already taken");
                socket.close();
                return;
            }

            // Username is valid
            out.println("USERNAME ACCEPTED");

            Server.clients.put(username, out);
            Server.broadcastUserList();
            Server.broadcastMessage("Server", username + " has joined the chat");

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("bye")) break;
                Server.broadcastMessage(username, message);
            }

        } catch (IOException e) {
            System.err.println("Connection lost with " + username);
        } finally {
            try {
                if (username != null) {
                    Server.clients.remove(username);
                    Server.broadcastUserList();
                    Server.broadcastMessage("Server", username + "left the chat");
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
