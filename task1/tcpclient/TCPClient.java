package tcpclient;

import java.net.*;
import java.io.*;

public class TCPClient {
	private final static int BUFFER_SIZE = 1024;
	private final static int TIME_OUT = 5000;

	public TCPClient() {
	}

	public byte[] askServer( String hostname, int port, byte[] toServerBytes ) throws IOException {
		if ( toServerBytes.length == 0 ) {
			return askServer( hostname, port );
		}

		byte[] bytesFromServer = new byte[ 0 ];
		try {
			Socket socket = new Socket( hostname, port );

			write_to_socket( socket, toServerBytes );
			bytesFromServer = retrieve_from_socket( socket );

			socket.close();
		}
		catch ( Exception exception ) {
			exception.printStackTrace();
		}

		return bytesFromServer;
	}

	public byte[] askServer( String hostname, int port ) throws IOException {
		Socket socket = new Socket( hostname, port );

		byte[] bytesFromServer = retrieve_from_socket( socket );

		socket.close();

		return bytesFromServer;
	}

	private void write_to_socket( Socket socket, byte[] bytesToServer ) throws IOException {
		OutputStream outputStream = socket.getOutputStream();
		outputStream.write( bytesToServer );
	}

	private byte[] retrieve_from_socket( Socket socket ) throws IOException {
		ByteArrayOutputStream dynamicBuffer = new ByteArrayOutputStream();
		byte[] buffer = new byte[ BUFFER_SIZE ];

		socket.setSoTimeout( TIME_OUT );
		InputStream inputStream = socket.getInputStream();

		while ( inputStream.read( buffer ) != -1 ) {
			dynamicBuffer.write( buffer );
		}

		return dynamicBuffer.toByteArray();
	}
}