package dgu.bufsizing;

import dgu.util.IllegalArgValException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

/**
 * Simple structure to hold our control parameters for the binding to use.
 * @author David Underhill
 */
public class ControlParams {
    private int linkBW = 100 * 1000 * 1000; /* 100 Mbps */
    private int delay = 100; /* ms */
    private int payloadBW = 1000; /* bps */
    private short numFlows = 1;
    private boolean useNumFlows = false;
    
    private ServerSocket serverSocket;
    private LinkedList<ClientHandlerThread> clients = new LinkedList<ClientHandlerThread>();

    public void sendCommands( int code, int val ) {
        for( ClientHandlerThread c : clients )
            c.command(code, val);
    }
    
    public void waitForClients( int port, int numGen ) {
        //setup socket for listening for new clients connection requests
        try {
            serverSocket = new ServerSocket(port);
        }
        catch( IOException e ) {
          System.err.println( Integer.toString(port) + ": " + e.getMessage() );
          System.exit( 1 );
        }    
        
        int num = 0;
        while( numGen > 0 ) {
            numGen -= 1;
            num += 1;
            
            Socket clientSocket;
            try {
                System.out.println( "Waiting for client #" + num + " to connect (" + numGen + " left after this one)" );
                clientSocket = serverSocket.accept();
                System.out.println( "New client: " + clientSocket.getRemoteSocketAddress().toString() );
                ClientHandlerThread s = new ClientHandlerThread( clientSocket );
                clients.add(s);
                s.start();
            } catch(IOException e) {
                //if the socket has closed, then this exception was just reporting this: stop execution
                if( serverSocket.isClosed() ) break;

                System.err.println( e.getMessage() );
                System.exit( 1 );
            }
        }
    }
    
    public static class StringPair {
        public String a, b;
        String both() { return a + b; }
    }
    
    public StringPair formatBits( int b ) {
        int bytes = b / 8;
        int units = 0;
        while( bytes >= 10000 ) {
            bytes /= 1000;
            units += 1;
        }
        String strUnit;
        switch( units ) {
            case  0: strUnit = "B";  break;
            case  1: strUnit = "kB"; break;
            case  2: strUnit = "MB"; break;
            case  3: strUnit = "GB"; break;
            case  4: strUnit = "TB"; break;
            case  5: strUnit = "PB"; break;
            default: strUnit = "?B"; break;
        }
        
        ControlParams.StringPair ret = new ControlParams.StringPair();
        ret.a = Integer.toString( bytes );
        ret.b = strUnit;
        return ret;
    }
    
    public void recomputeBufferSize() {
        int bufSizeOrig = linkBW * delay / 1000;
        int bufSizeNew = (int)(bufSizeOrig / Math.sqrt(numFlows));
        StringPair strOrig = formatBits(bufSizeOrig);
        StringPair strNew = formatBits(bufSizeNew);
        
        if( useNumFlows ) {
            MasterGUI.me.lblNotCurBufSizeVal.setText( "(trad => " + strOrig.both() + ")" );
            MasterGUI.me.lblCurBufSizeVal.setText( strNew.a );
            MasterGUI.me.lblCurBufSizeUnits.setText( strNew.b );
        }
        else {
            MasterGUI.me.lblNotCurBufSizeVal.setText( "(new => " + strNew.both() + ")" );
            MasterGUI.me.lblCurBufSizeVal.setText( strOrig.a );
            MasterGUI.me.lblCurBufSizeUnits.setText( strOrig.b );
        }
    }
    
    public int getLinkBW() {
        return linkBW;
    }

    public void setLinkBW(int bw_bps) throws IllegalArgValException {
        if( bw_bps <= 1000 || bw_bps > 1000 * 1000 * 1000 )
            throw( new IllegalArgValException("Link Bandwidth must be between 1kbps and 1Gbps") );
        
        this.linkBW = bw_bps;
        recomputeBufferSize();
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) throws IllegalArgValException {
        if( delay <= 0 || delay > 10000 )
            throw( new IllegalArgValException("Delay must be between 1 and 10000 ms") );
        
        this.delay = delay;
        recomputeBufferSize();
    }

    public int getPayloadBW() {
        return payloadBW;
    }

    public void setPayloadBW(int payload_bw_bps) throws IllegalArgValException {
        if( payload_bw_bps <= 1000 || payload_bw_bps > 1000 * 1000 * 1000 )
            throw( new IllegalArgValException("Payload rate must be between 1kbps and 1Gbps") );
        
        this.payloadBW = payload_bw_bps;
        sendCommands( 1, this.payloadBW );
    }

    public int getNumFlows() {
        return numFlows;
    }

    public void setNumFlows(int num_flows) throws IllegalArgValException {
        if( num_flows <= 0 || num_flows > 65535 )
            throw( new IllegalArgValException("Number of flows must be between 1 and 65535") );
        
        this.numFlows = (short)num_flows;
        recomputeBufferSize();
        sendCommands( 0, this.numFlows );
    }

    public boolean isUseNumFlows() {
        return useNumFlows;
    }

    public void setUseNumFlows(boolean useNumFlows) {
        this.useNumFlows = useNumFlows;
    }

    /**
     * Monitor the connection with a client.  Notifies the server if the connection is lost or data is received.
     */
    public class ClientHandlerThread extends Thread {
        /** the socket which connects this handler to its client */
        private final Socket s;

        /** output stream to write to the socket */
        private final BufferedOutputStream out;

        /** input stream to read from the socket */
        private final BufferedInputStream in;

        /** 
         * Sets up a client handler with the specified socket
         * @param clientSocket  the socket to communicate with
         */
        public ClientHandlerThread(final Socket clientSocket) {
            this.s = clientSocket;

            //try to establish the I/O streams: if we can't establish either, then close the socket
            BufferedOutputStream tmp;
            try {
                tmp = new BufferedOutputStream( s.getOutputStream() );
            } catch( IOException e ) {
                System.err.println( "Client Socket Setup Error: " + e.getMessage() );
                System.exit( 1 );
                out = null;
                in = null;
                return;
            } 
            out = tmp;

            BufferedInputStream tmp2;
            try {
                tmp2 = new BufferedInputStream( s.getInputStream() );
            } catch( IOException e ) {
                System.err.println( "Client Socket Setup Error: " + e.getMessage() );
                System.exit( 1 );
                in = null;
                return;
            }
            in = tmp2;
        }
        
        /** Monitors the input stream for stats. */
        public void run() {
            while( true ) {
                try {
                    int kbytes = in.read() << 24;
                    kbytes += in.read() << 16;
                    kbytes += in.read() << 8;
                    kbytes += in.read();
                    System.out.println( "got kbytes " + kbytes );
                } catch( Exception e  ) {
                    //if the socket has closed (reset), then this exception was just reporting this: stop execution
                    if( s.isClosed() || (e.getMessage()!=null && e.getMessage().equals("connection reset")) ) { 
                        return;
                    }

                    //if the socket is still open, this exception is unexpected: log it and terminate the client
                    System.err.println( "Client Read Error: " + e.getMessage() );
                    return;
                }
            }
        }
        
        public void command( int code, int value ) {
            try {
                //write the code (one byte)
                byte b = (byte)code;
                out.write(b);
                
                //write each byte in the value
                out.write( value >> 24 );
                out.write( (value >> 16) & 0x000000FF );
                out.write( (value >> 8) & 0x000000FF );
                out.write( value & 0x000000FF );
            } catch( IOException e ) {
                System.err.println( "Command " + code + " / " + value + " => failed: " + e.getMessage() );
                System.exit( 1 );
            }
        }
    }
}
