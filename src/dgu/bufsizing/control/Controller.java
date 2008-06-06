package dgu.bufsizing.control;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Skeleton controller for receiving and sending commands to a client.
 * @author David Underhill
 */
public abstract class Controller {    
    /** the socket which connects this Command to its client */
    protected final Socket s;

    /** output stream to write to the socket */
    protected final OutputStream out;

    /** input stream to read from the socket */
    protected final InputStream in;
    
    /** converts the four bytes starting at offset into an integer */
    public static int bytes_to_int( byte[] buf, int offset ) {
        int ret;
        ret =   buf[offset] << 24;
        ret += (buf[offset+1] << 16);
        ret += (buf[offset+2] << 8);
        ret +=  buf[offset+3];
        return ret;
    }

    /** 
     * Connect to the client on the specified port.  If an error occurs, the 
     * program will terminate.
     * 
     * @param port  the TCP port to connect on
     */
    public Controller( int port ) {
        // setup socket for listening for new clients connection requests
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket( port );
        }
        catch( IOException e ) {
          System.err.println( Integer.toString( port ) + ": " + e.getMessage() );
          System.exit( 1 );
          serverSocket = null;
        }
        
        // connect to the client
        Socket clientSocket;
        try {
            System.out.println( "Waiting for " + getTypeString() + " to connect" );
            clientSocket = serverSocket.accept();
            System.out.println( "New " + getTypeString() + " client: " + clientSocket.getRemoteSocketAddress().toString() );
        } catch(IOException e) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
            clientSocket = null;
        }
        this.s = clientSocket;

        //try to establish the I/O streams: if we can't establish either, then close the socket
        OutputStream tmp;
        try {
            tmp = s.getOutputStream();
        } catch( IOException e ) {
            System.err.println( "Client Socket Setup Error: " + e.getMessage() );
            System.exit( 1 );
            out = null;
            in = null;
            return;
        } 
        out = tmp;

        InputStream tmp2;
        try {
            tmp2 = s.getInputStream();
        } catch( IOException e ) {
            System.err.println( "Client Socket Setup Error: " + e.getMessage() );
            System.exit( 1 );
            in = null;
            return;
        }
        in = tmp2;
    }
    
    public abstract String getTypeString();

    protected synchronized void sendCommand( byte code, int value ) {
        try {
            //write the code (one byte)
            out.write( code );

            //write each byte in the value
            out.write(  value >> 24 );
            out.write( (value >> 16) & 0x000000FF );
            out.write( (value >>  8) & 0x000000FF );
            out.write(  value        &  0x000000FF );
        } catch( IOException e ) {
            System.err.println( "command " + code + " / " + value + " => failed: " + e.getMessage() );
            System.exit( 1 );
        }
    }
}
