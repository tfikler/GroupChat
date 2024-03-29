import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class GroupChat {
    private static final int PORT = 9922;
    private static Set<Client> clients = new HashSet<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server is running on port " + PORT);

        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Client client = new Client(clientSocket);
                clients.add(client);
                new ClientHandler(client).start();
            }
        } finally {
            serverSocket.close();
        }
    }

    public static class Client {
        private String clientName;
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;

        public Client(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        }

        public void sendMessage(String message) {
            writer.println(message);
        }

        public String getClientName() {
            return clientName;
        }

        public String getPort() {
            return Integer.toString(socket.getPort());
        }

        public void setClientName(String clientName) {
            this.clientName = clientName;
        }

        public String getIpAddress() {
            return socket.getInetAddress().getHostAddress();
        }

        public String getIPAndNameFormattedForPrint() {
            return "(" + clientName + " [" + getIpAddress() + ":" + getPort() + "])";
        }

        public void close() throws IOException {
            socket.close();
            reader.close();
            writer.close();
        }

        public InputStream getInputStream() throws IOException {
            return socket.getInputStream();
        }
    }

    private static class ClientHandler extends Thread {
        private Client client;

        public ClientHandler(Client client) {
            this.client = client;
        }

        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));

                // Send welcome message to the new client
                client.sendMessage("Welcome to RUNI Computer Networks 2024 chat server! There are " + (clients.size() - 1) + " users connected.");
                client.sendMessage("Please enter your nickname: ");
                client.setClientName(reader.readLine());
                System.out.println("New client has joined to the party! AKA: " + client.getIPAndNameFormattedForPrint());

                // Notify other clients about the new connection
                for (Client otherClient : clients) {
                    if (otherClient != client) {
                        otherClient.sendMessage(client.getIpAddress() + " has joined to the chat!");
                    }
                }

                String message;
                while ((message = reader.readLine()) != null) {
                    // Broadcast the message to all clients
                    for (Client otherClient : clients) {
                        if (otherClient != client) {
                            otherClient.sendMessage(client.getIPAndNameFormattedForPrint()+ ": " + message + "\n");
                        } else {
                            otherClient.sendMessage("(You): " + message);
                        }
                    }
                }
            } catch (IOException e) {
                // Print error if needed
            } finally {
                // Remove the client's writer and decrement the client count
                clients.remove(client);
                System.out.println("Client " + client.getIPAndNameFormattedForPrint() + " has left the party! (LOSER)");

                // Notify other clients about the disconnection
                for (Client otherClient : clients) {
                    otherClient.sendMessage(client.getIPAndNameFormattedForPrint() + " left");
                    otherClient.sendMessage("There are " + clients.size() + " users connected.");
                }
                try {
                    client.close();
                } catch (IOException e) {
                    // Print error if needed
                }
            }
        }
    }
}
