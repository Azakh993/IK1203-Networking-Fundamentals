package tcpclient;

import java.net.*;
import java.io.*;

public class TCPClient {
	private final static int BUFFER_SIZE = 1024;

	public TCPClient() {
	}

	public byte[] askServer( String hostname, int port, byte[] toServerBytes ) throws IOException {
		ByteArrayOutputStream receive_buffer = new ByteArrayOutputStream();
		byte[] buffer_array = new byte[ BUFFER_SIZE ];
		try {
			Socket socket = new Socket( hostname, port );

			OutputStream outputStream = socket.getOutputStream();
			outputStream.write( toServerBytes );

			InputStream inputStream = socket.getInputStream();
			while ( inputStream.read( buffer_array ) != -1 ) {
				receive_buffer.write( buffer_array );
			}
			socket.close();
		}
		catch ( Exception exception ) {
			exception.printStackTrace();
		}

		return receive_buffer.toByteArray();
	}

	public byte[] askServer( String hostname, int port ) throws IOException {
		return new byte[ 0 ];
	}
}
