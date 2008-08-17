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
    public static final long TIME_BETWEEN_ERROR_MSGS_MILLIS = 5000;
    
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
        this(port, true);
    }
    
    public Controller( int port, boolean askBeforeBlocking ) {    
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
            if(askBeforeBlocking && dgu.util.swing.GUIHelper.confirmDialog("Connect Confirm", "Wait for " + getTypeString() + " controller?", javax.swing.JOptionPane.YES_NO_OPTION)==javax.swing.JOptionPane.NO_OPTION)
                clientSocket = null;
            else {
                clientSocket = serverSocket.accept();
                System.out.println( "New " + getTypeString() + " client: " + clientSocket.getRemoteSocketAddress().toString() );
            }
        } catch(IOException e) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
            clientSocket = null;
        }
        this.s = clientSocket;

        //try to establish the I/O streams: if we can't establish either, then close the socket
        OutputStream tmp;
        try {
            if(s==null)
                tmp=null;
            else
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
            if(s==null)
                tmp2=null;
            else
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
        if(out==null) {
            System.err.println("could not send command " + code + "," + value);
            return;
        }
        
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
    
    public int readByteOrDie() throws IOException {
        if(in==null)
            throw(new IOException("input socket is not open"));
        
        int ret = in.read();
        if( ret == -1 )
            throw new IOException("Socket received EOF");
        
        return ret;
    }
    
    /** reads byte i to i+3 to form an int */
    public int readInt() throws IOException {
        // convert to signed ints, clearing any bits set due to sign extension
        int a = readByteOrDie() & 0x000000FF;
        int b = readByteOrDie() & 0x000000FF;
        int c = readByteOrDie() & 0x000000FF;
        int d = readByteOrDie() & 0x000000FF;
        
        // create the int
        return a<<24 | b<<16 | c<<8 | d;
    }
}
