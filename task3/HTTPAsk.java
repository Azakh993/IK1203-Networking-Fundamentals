import tcpclient.TCPClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.HashMap;

public class HTTPAsk {
    private final static String NEWLINE = "\r\n";
    private final static String CONNECTION = "Connection: close" + NEWLINE;
    private final static String CONTENT_TYPE = "Content-Type: text/plain" + NEWLINE;
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
                    String http_status_code = get_http_status_code(unparsedRequest);

                    if (http_status_code.equals("200")) {
                        byte[] serverResponse = ask_server();
                        int responseSize = serverResponse.length;
                        output_response_header(output, responseSize);
                        output_data(output, serverResponse);
                    } else {
                        output_response_header(output, http_status_code);
                    }
                    socket.close();
                    System.out.println("Closed connection!");

                } catch (Exception exception) {
                    System.err.println(exception);
                }
            }
        } catch (Exception exception) {
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


    private static String get_http_status_code(String unparsedParameters) {
        boolean hostProvided = arguments.containsKey("hostname");
        boolean portProvided = arguments.containsKey("port");
        boolean urlValid = verify_url(unparsedParameters);
        boolean getValid = arguments.containsKey("GET") && arguments.get("GET").equals("VALID");
        boolean askValid = arguments.containsKey("/ask") && arguments.get("/ask").equals("VALID");
        boolean httpValid = arguments.containsKey("HTTP/1.1") && arguments.get("HTTP/1.1").equals("VALID");

        if (!urlValid) {
            return "400";
        } else if (!getValid) {
            return "501";
        } else if (!askValid) {
            return "404";
        } else if (!hostProvided || !portProvided) {
            return "422";
        } else if (!httpValid) {
            return "505";
        } else {
            return "200";
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

    private static void output_response_header(OutputStream output, int responseSize) throws IOException {
        String HTTP200 = "HTTP/1.1 200 OK" + NEWLINE;
        String contentLength = "Content-Length: " + responseSize + NEWLINE;
        output.write((HTTP200 + contentLength + CONNECTION + CONTENT_TYPE + NEWLINE).getBytes());
    }

    private static void output_data(OutputStream output, byte[] serverResponse) throws IOException {
        if (serverResponse.length > 0) {
            output.write(serverResponse);

            char lastCharOfResponse = (char) serverResponse[serverResponse.length - 1];
            if (lastCharOfResponse != '\n') {
                output.write(NEWLINE.getBytes());
            }
        }
    }

    private static void output_response_header(OutputStream output, String http_status_code) throws IOException {
        byte[] HTTP400 = ("HTTP/1.1 400 Bad Request" + NEWLINE).getBytes();
        byte[] HTTP422 = ("HTTP/1.1 422 Unprocessable Entity" + NEWLINE).getBytes();
        byte[] HTTP404 = ("HTTP/1.1 404 Not Found" + NEWLINE).getBytes();
        byte[] HTTP501 = ("HTTP/1.1 501 Not Implemented" + NEWLINE).getBytes();
        byte[] HTTP505 = ("HTTP/1.1 505 HTTP Version Not Supported" + NEWLINE).getBytes();

        switch (http_status_code) {
            case "400" -> output.write(HTTP400);
            case "422" -> output.write(HTTP422);
            case "404" -> output.write(HTTP404);
            case "501" -> output.write(HTTP501);
            case "505" -> output.write(HTTP505);
        }
        output.write((CONNECTION + CONTENT_TYPE + NEWLINE).getBytes());
    }

    private static boolean verify_url(String unparsedParameters) {
        String requestWithoutGETandHTTP = unparsedParameters.split(" ")[1];

        try {
            new URL("http://localhost:1234" + requestWithoutGETandHTTP).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException exception) {
            return false;
        }
    }
}

