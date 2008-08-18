package dgu.bufsizing.control;

import dgu.bufsizing.DemoGUI;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;

/**
 * Skeleton controller for receiving and sending commands to a client.
 * @author David Underhill
 */
public abstract class Controller {
    private static final HashMap<String, String> nameToIP = new HashMap<String, String>();
    static {
        // names <-> ip mapping (lower case!)
        nameToIP.put("la1", "64.57.23.66");
        nameToIP.put("la2", "64.57.23.67");
        nameToIP.put("b81", "171.64.74.81");
        nameToIP.put("b82", "171.64.74.82");
        nameToIP.put("b83", "171.64.74.83");
        nameToIP.put("b84", "171.64.74.84");
    }
    private static final String getIPFromNameOrIP(String ip) {
        String name = ip.toLowerCase();
        String mappedIP = nameToIP.get(name);
        if( mappedIP == null )
            return ip;
        else
            return mappedIP;
    }
    
    public static final long TIME_BETWEEN_ERROR_MSGS_MILLIS = 5000;
    
    protected String serverIP;
    protected int serverPort;
    boolean tryingToConnect = false;
    
    /** the socket which connects this Command to its client */
    protected Socket s;

    /** output stream to write to the socket */
    protected OutputStream out;

    /** input stream to read from the socket */
    protected InputStream in;

    /** 
     * Connect to the client on the specified port.
     * 
     * @param ip    the IP of the client to connect to
     * @param port  the TCP port to connect on
     */
    public Controller( String ip, int port ) {
        serverIP = getIPFromNameOrIP(ip);
        serverPort = port;
        asynchronousConnect();
    }
    
    private void asynchronousConnect() {
        if( tryingToConnect || (s!=null && out!=null && in!=null) )
            return;
        
        new Thread() { 
            public void run() {
                connect();
            }
        }.start();
    }
    
    /** Continuously tries to connect to the server. */
    private void connect() {
        tryingToConnect = true;
        close();
        
        int tries = 0;
        while( s==null ) {
            if( tries++ > 0 ) {
                System.err.println(getName() + ": Retrying to establish connection (try #" + tries + ")");
                close();
            }
            else
                System.err.println(getName() + ": Trying to establish connection");

            // setup socket for listening for new clients connection requests
            try {
                s = new Socket(serverIP, serverPort);
            }
            catch( IOException e ) {
                System.err.println( getName() + ": " + e.getMessage() );
                s = null;
            }

            //try to establish the I/O streams: if we can't establish either, then close the socket
            try {
                if( s != null )
                    out = s.getOutputStream();
            } catch( IOException e ) {
                System.err.println( getName() + ": Client Socket Setup Error: " + e.getMessage() );
                out = null;
            } 
            
            try {
                if( out != null )
                    in = s.getInputStream();
            } catch( IOException e ) {
                System.err.println( getName() + ": Client Socket Setup Error: " + e.getMessage() );
                in = null;
            }

            if( in == null ) {
                System.err.println( getName() + ": Failed to establish connection! (will retry in 1 second)");
                DemoGUI.msleep(1000); 
            }
        }
        
        System.err.println(getName() + ": now connected");
        tryingToConnect = false;
    }
     
    private synchronized void close() {
        if( s != null ) {
            try {
                s.close();
            } catch(IOException e){}
            s = null;
        }
        if( out != null ) {
            try {
                out.close();
            } catch(IOException e){}
            out = null;
        }
        if( in != null ) {
            try {
                in.close();
            } catch(IOException e){}
            in = null;
        }
    }
    
    public abstract String getTypeString();
    public String getName() { return getTypeString() + " controller (" + serverIP + ":" + serverPort + ")"; }

    protected synchronized void sendCommand( byte code, int value ) {
        OutputStream myOut = out;
        
        if(tryingToConnect || myOut==null) {
            System.err.println(getName() + ": could not send command " + code + "," + value);
            asynchronousConnect();
            return;
        }
        
        try {
            //write the code (one byte)
            myOut.write( code );

            //write each byte in the value
            myOut.write(  value >> 24 );
            myOut.write( (value >> 16) & 0x000000FF );
            myOut.write( (value >>  8) & 0x000000FF );
            myOut.write(  value        &  0x000000FF );
        } catch( IOException e ) {
            System.err.println( getName() + ": command " + code + " / " + value + " => failed: " + e.getMessage() );
            asynchronousConnect();
        }
    }
    
    public int readByteOrDie() throws IOException {
        InputStream myIn = in;
        
        if(tryingToConnect || myIn==null) {
            asynchronousConnect();
            throw new IOException("input socket is not open");
        }
        
        int ret = myIn.read();
        if( ret == -1 ) {
            asynchronousConnect();
            throw new IOException("Socket received EOF");
        }
        
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
