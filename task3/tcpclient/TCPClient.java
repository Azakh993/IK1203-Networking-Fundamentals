package tcpclient;

import java.net.*;
import java.io.*;

public class TCPClient {
    private static int buffer_size = 1024;
    private static boolean shutdown_boolean;
    private static Integer timeout_ms;
    private static Integer limit_bytes;

    public TCPClient(boolean shutdown, Integer timeout, Integer limit) {
        shutdown_boolean = shutdown;
        timeout_ms = timeout == null || timeout < 0 ? 0 : timeout;
        limit_bytes = limit;
    }

    public byte[] askServer(String hostname, int port, byte[] toServerBytes) throws IOException {
        if (toServerBytes.length == 0) {
            return askServer(hostname, port);
        }

        Socket socket = new Socket(hostname, port);
        write_to_socket(socket, toServerBytes);
        byte[] bytesFromServer = retrieve_from_socket(socket);
        socket.close();
        return bytesFromServer;
    }

    public byte[] askServer(String hostname, int port) throws IOException {
        Socket socket = new Socket(hostname, port);
        byte[] bytesFromServer = retrieve_from_socket(socket);
        socket.close();
        return bytesFromServer;
    }

    private void write_to_socket(Socket socket, byte[] bytesToServer) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(bytesToServer);
        if (shutdown_boolean) {
            socket.shutdownOutput();
        }
    }

    private byte[] retrieve_from_socket(Socket socket) throws IOException {
        ByteArrayOutputStream dynamicBuffer = new ByteArrayOutputStream();

        if (limit_bytes != null && buffer_size < limit_bytes) {
            buffer_size = limit_bytes;
        }

        byte[] buffer = new byte[buffer_size];

        try {
            InputStream inputStream = socket.getInputStream();
            socket.setSoTimeout(timeout_ms);

            int readBytes;

            if (limit_bytes == null) {
                while ((readBytes = inputStream.read(buffer)) != -1) {
                    dynamicBuffer.write(buffer, 0, readBytes);
                }
            } else {
                int limit = limit_bytes;
                while ((readBytes = inputStream.read(buffer, 0, limit)) != -1) {
                    dynamicBuffer.write(buffer, 0, readBytes);

                    limit -= readBytes;
                    if (limit == 0) {
                        break;
                    }
                }
            }
        } catch (SocketTimeoutException exception) {
            System.out.println("Socket Timeout!\n");
        }

        return dynamicBuffer.toByteArray();
    }
}