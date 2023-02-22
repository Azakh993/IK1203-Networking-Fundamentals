import tcpclient.TCPClient;

import java.net.*;
import java.io.*;
import java.util.HashMap;

public class HTTPAsk {
    private final static String NEWLINE = "\r\n";
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
                    parse_and_set_parameters(unparsedRequest);
                    System.out.println("Processed request...");

                    OutputStream output = socket.getOutputStream();
                    boolean parameters_valid = verify_parameters(output);

                    if (parameters_valid) {
                        byte[] serverResponse = ask_server();
                        output_response(output, serverResponse);
                    }

                    socket.close();
                    System.out.println("Closed connection!");

                } catch (Exception exception) {
                    System.err.println(exception);
                }
            }
        } catch (Exception exception){
            System.err.println(exception);
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

    private static void parse_and_set_parameters(String unparsedRequest) {
        String[] requestComponents = unparsedRequest.split("[?&= ]");
        HashMap< String, Object > arguments_map = new HashMap<>();

        if (requestComponents[0].equals("GET")) {
            arguments_map.put("GET", "VALID");
        }

        if (requestComponents[1].equals("/ask")) {
            arguments_map.put("/ask", "VALID");
        }

        int lastRequestParameterIndex = requestComponents.length - 1;
        if (requestComponents[lastRequestParameterIndex].equals("HTTP/1.1")) {
            arguments_map.put("HTTP/1.1", "VALID");
        }

        for (int i = 2; i < lastRequestParameterIndex; i++) {
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


    private static boolean verify_parameters(OutputStream output) throws IOException {
        boolean hostProvided = arguments.containsKey("hostname");
        boolean portProvided = arguments.containsKey("port");
        boolean getValid = arguments.containsKey("GET") && arguments.get("GET").equals("VALID");
        boolean askValid = arguments.containsKey("/ask") && arguments.get("/ask").equals("VALID");
        boolean httpValid = arguments.containsKey("HTTP/1.1") && arguments.get("HTTP/1.1").equals("VALID");

        String HTTP400 = "HTTP/1.1 400 Bad Request" + NEWLINE;
        String HTTP404 = "HTTP/1.1 404 Not Found" + NEWLINE;
        String HTTP505 = "HTTP/1.1 505 HTTP Version Not Supported" + NEWLINE;
        String HTTP200 = "HTTP/1.1 200 OK" + NEWLINE;
        String connection = "Connection: close" + NEWLINE;
        String contentType = "Content-Type: text/plain" + NEWLINE;

        if (!getValid) {
            output.write((HTTP400 + connection + contentType + NEWLINE).getBytes());
            return false;
        } else if (!askValid) {
            output.write((HTTP404 + connection + contentType + NEWLINE).getBytes());
            return false;
        } else if (!hostProvided || !portProvided) {
            output.write((HTTP400 + connection + contentType + NEWLINE).getBytes());
            return false;
        } else if (!httpValid) {
            output.write((HTTP505 + connection + contentType + NEWLINE).getBytes());
            return false;
        } else {
            output.write((HTTP200 + connection + contentType).getBytes());
            return true;
        }
    }

    private static byte[] ask_server() {
        String hostName = (String) arguments.get("hostname");
        Integer port = (Integer) arguments.get("port");
        String dataToSend = (String) arguments.get("string");
        Integer limit = (Integer) arguments.get("limit");
        Integer timeout = (Integer) arguments.get("timeout");
        boolean shutdown = arguments.containsKey("shutdown") ? (Boolean) arguments.get("shutdown") : false;

        byte[] serverResponse = new byte[0];

        try {
            TCPClient tcpClient = new TCPClient(shutdown, timeout, limit);
            return dataToSend == null ?
                    tcpClient.askServer(hostName, port) :
                    tcpClient.askServer(hostName, port, (dataToSend + NEWLINE).getBytes());
        } catch (IOException exception) {
            return serverResponse;
        }
    }


    private static void output_response(OutputStream output, byte[] serverResponse) throws IOException {
        output.write(("Content-Length: " + serverResponse.length + NEWLINE + NEWLINE).getBytes());
        if (serverResponse.length > 0) {
            output.write(serverResponse);

            char lastCharOfResponse = (char) serverResponse[serverResponse.length - 1];
            if (lastCharOfResponse != '\n') {
                output.write(NEWLINE.getBytes());
            }
        }
    }
}

