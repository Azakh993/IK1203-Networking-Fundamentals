import java.net.ServerSocket;
import java.net.Socket;

public class ConcHTTPAsk {
    public static void main(String[] args) {
        int server_port = Integer.parseInt(args[0]);
        int connectionCount = 1;

        try (ServerSocket serverSocket = new ServerSocket(server_port)) {
            while (true) {
                System.out.println("\nAwaiting connection (" + connectionCount + ")...");
                Socket socket = serverSocket.accept();

                System.out.println("Connection (" + connectionCount + ") established...");
                Runnable connectionInstance = new HTTPAsk(socket, connectionCount++);
                new Thread(connectionInstance).start();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}