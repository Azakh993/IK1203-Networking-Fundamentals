package tcpclient;

import java.net.*;
import java.io.*;

public class TCPClient {
	private final static int BUFFER_SIZE = 1024;
	private static boolean shutdown_boolean;
	private static Integer timeout_ms;
	private static Integer limit_bytes;

	public TCPClient(boolean shutdown, Integer timeout, Integer limit) {
		shutdown_boolean = shutdown;
		timeout_ms = timeout == null || timeout < 0 ? 0 : timeout;
		limit_bytes = limit;
	}

	public byte[] askServer( String hostname, int port, byte[] toServerBytes ) throws IOException {
		if ( toServerBytes.length == 0 ) {
			return askServer( hostname, port );
		}

		Socket socket = new Socket( hostname, port );

		write_to_socket( socket, toServerBytes );
		byte[] bytesFromServer = retrieve_from_socket( socket );

		socket.close();

		return bytesFromServer;
	}

	public byte[] askServer( String hostname, int port ) throws IOException{
		Socket socket = new Socket( hostname, port );

		byte[] bytesFromServer = retrieve_from_socket( socket );

		socket.close();

		return bytesFromServer;
	}

	private void write_to_socket( Socket socket, byte[] bytesToServer ) throws IOException {
		OutputStream outputStream = socket.getOutputStream();
		outputStream.write( bytesToServer );
		if (shutdown_boolean)
			socket.shutdownOutput(); // Check
	}

	private byte[] retrieve_from_socket( Socket socket ) throws IOException {
		ByteArrayOutputStream dynamicBuffer = new ByteArrayOutputStream();
		byte[] buffer = new byte[ BUFFER_SIZE ];

		socket.setSoTimeout( timeout_ms ); // Check
		InputStream inputStream = socket.getInputStream();

		Integer limit = limit_bytes;

		while ( ( limit == null || limit > 0 ) && inputStream.read( buffer ) != -1 ) { // Check
			dynamicBuffer.write( buffer );
			if( limit != null ) {
				limit--;
			}
		}

		return dynamicBuffer.toByteArray();
	}
}