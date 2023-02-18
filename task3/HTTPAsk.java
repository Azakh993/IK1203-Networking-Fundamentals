import tcpclient.TCPClient;

import java.net.*;
import java.io.*;
import java.util.HashMap;

public class HTTPAsk {
    private static HashMap< String, Object > arguments;

    public static void main(String[] args) {

        try {
            int server_port = Integer.parseInt(args[0]);
            ServerSocket serverSocket = new ServerSocket(server_port);

            while (true) {
                try {
                    System.out.println("\nAwaiting Connection...");

                    Socket socket = serverSocket.accept();
                    System.out.println("Connected...");

                    String unparsedRequest = read_request(socket);
                    set_arguments(unparsedRequest);
                    System.out.println("Processed request...");

                    get_server_response(socket);
                    System.out.println("Responded to query...");

                    socket.close();
                    System.out.println("Closed connection!");
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static String read_request(Socket socket) throws IOException {
        InputStream inputStream = socket.getInputStream();
        StringBuilder requestBuilder = new StringBuilder();

        int readByte;
        while ((readByte = inputStream.read()) != '\r') {
            char readChar = (char) readByte;
            requestBuilder.append(readChar);
        }
        return requestBuilder.toString();
    }

    private static void set_arguments(String unparsedRequest) {
        String[] requestComponents = unparsedRequest.split("[?&= ]");
        HashMap< String, Object > arguments_map = new HashMap<>();

        if (requestComponents[0].equals("GET")) {
            arguments_map.put("GET", "VALID");
        }

        if (requestComponents[1].equals("/ask")) {
            arguments_map.put("/ask", "VALID");
        }

        for (int i = 2; i < requestComponents.length - 1; i++) {
            String argumentType = requestComponents[i].trim();
            String argumentValue = requestComponents[++i].trim();

            switch (argumentType) {
                case "port", "timeout", "limit" -> {
                    Integer numericValue = Integer.parseInt(argumentValue);
                    arguments_map.put(argumentType, numericValue);
                }
                case "shutdown" -> {
                    boolean shutdown = Boolean.parseBoolean(argumentValue);
                    arguments_map.put(argumentType, shutdown);
                }
                default -> arguments_map.put(argumentType, argumentValue);
            }
        }
        arguments = arguments_map;
    }

    private static void get_server_response(Socket socket) throws IOException {
        OutputStream output = socket.getOutputStream();

        String hostName = (String) arguments.get("hostname");
        Integer port = (Integer) arguments.get("port");
        boolean getFirst = arguments.containsKey("GET") && arguments.get("GET").equals("VALID");
        boolean askSecond = arguments.containsKey("/ask") && arguments.get("/ask").equals("VALID");

        byte[] HTTP400 = "HTTP/1.1 400 Bad Request\r\n\r\n".getBytes();

        if (hostName == null || port == null || !getFirst || !askSecond) {
            output.write(HTTP400);
        } else {
            byte[] serverResponse = interact_with_server(output, hostName, port);
            output_server_response(output, serverResponse);
        }
    }

    private static byte[] interact_with_server(OutputStream output, String hostName, Integer port) throws IOException {
        String dataToSend = (String) arguments.get("string");
        Integer limit = (Integer) arguments.get("limit");
        Integer timeout = (Integer) arguments.get("timeout");
        boolean shutdown = arguments.containsKey("shutdown") ? (Boolean) arguments.get("shutdown") : false;

        TCPClient tcpClient = new TCPClient(shutdown, timeout, limit);
        byte[] serverResponse = new byte[0];

        try {

            if (dataToSend == null) {
                serverResponse = tcpClient.askServer(hostName, port);
            } else {
                serverResponse = tcpClient.askServer(hostName, port, (dataToSend + "\r\n").getBytes());
            }
        } catch (Exception exception) {
            byte[] HTTP404 = "HTTP/1.1 404 Not Found\r\n\r\n".getBytes();
            output.write(HTTP404);
        }

        return serverResponse;
    }

    private static void output_server_response(OutputStream output, byte[] serverResponse) throws IOException {
        byte[] HTTP200 = "HTTP/1.1 200 OK\r\n\r\n".getBytes();
        output.write(HTTP200);
        output.write(serverResponse);

        if(serverResponse.length > 0) {
            char lastCharOfResponse = (char) serverResponse[serverResponse.length - 1];
            if (lastCharOfResponse != '\n') {
                output.write('\r');
                output.write('\n');
            }
        }
    }
}

