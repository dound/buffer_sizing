package dgu.bufsizing;

import dgu.util.IllegalArgValException;
import java.io.*;
import java.net.*;
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

    private int queueSize;
    private int rateLim = -1;
    private ServerSocket serverSocketTGens;
    private ServerSocket serverSocketRtr;
    private LinkedList<ClientCommander> clients = new LinkedList<ClientCommander>();
    private RouterCommander rtrCommander;

    public void refreshBufferSize() {
        rtrCommander.command( RouterCmd.CMD_GET_BUF_SZ.code, 0 );
    }
    
    public void refreshRateLim() {
        rtrCommander.command( RouterCmd.CMD_GET_RATE.code, 0 );
    }
    
    public void decreaseRateLim() {
        if( rateLim > 0 )
            rtrCommander.command( RouterCmd.CMD_SET_RATE.code, rateLim - 1 );
    }
    
    public void increaseRateLim() {
        if( rateLim < 16 )
            rtrCommander.command( RouterCmd.CMD_SET_RATE.code, rateLim + 1 );
    }
    
    public enum SendValueType {
        SV_EXACT,  /* assigns value given to each client */
        SV_SPLIT;  /* assigns even chunks of value given to each client */
    }
    
    public void sendCommands( int code, int val, SendValueType type ) {
        int left_val = val;
        int left_num = clients.size();
        
        for( ClientCommander c : clients ) {
            if( type == SendValueType.SV_EXACT )
                c.command(code, val);
            else {
                // assign an even split to each client
                int actual = left_val / left_num;
                left_val -= actual;
                c.command(code, actual);
            }
        }
    }
    
    public void waitForClients( int numGen, int port ) {
        //setup socket for listening for new clients connection requests
        try {
            serverSocketTGens = new ServerSocket(port);
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
                clientSocket = serverSocketTGens.accept();
                System.out.println( "New client: " + clientSocket.getRemoteSocketAddress().toString() );
                ClientCommander s = new ClientCommander( clientSocket );
                clients.add(s);
            } catch(IOException e) {
                //if the socket has closed, then this exception was just reporting this: stop execution
                if( serverSocketTGens.isClosed() ) break;

                System.err.println( e.getMessage() );
                System.exit( 1 );
            }
        }
        
        MasterGUI.me.lblNumGenVal.setText( Integer.toString(numGen) );
    }
    
    public void waitForRouter( int port ) {
        //setup socket for listening for new clients connection requests
        try {
            serverSocketRtr = new ServerSocket(port);
        }
        catch( IOException e ) {
          System.err.println( Integer.toString(port) + ": " + e.getMessage() );
          System.exit( 1 );
        }    
        
        try {
            System.out.println( "Waiting for router to connect" );
            Socket clientSocket = serverSocketRtr.accept();
            System.out.println( "New router client: " + clientSocket.getRemoteSocketAddress().toString() );
            rtrCommander = new RouterCommander( clientSocket );
        } catch(IOException e) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }
        
        // start a thread to gather stats (listens on the same port, but UDP)
        new RouterStatsListenerThread(port).start();
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
    
    /** define the packet size as the normal MTU since that is what steady-state TCP will send */
    public static final int BYTES_PER_PACKET = 1500;
    
    public void recomputeBufferSize() {
        int bufSizeOrig = linkBW * delay / 1000;
        int bufSizeNew = (int)(bufSizeOrig / Math.sqrt(numFlows));
        StringPair strOrig = formatBits(bufSizeOrig);
        StringPair strNew = formatBits(bufSizeNew);
        
        if( useNumFlows ) {
            MasterGUI.me.lblNotCurBufSizeVal.setText( "(trad => " + strOrig.both() + ")" );
            MasterGUI.me.lblCurBufSizeVal.setText( strNew.a );
            MasterGUI.me.lblCurBufSizeUnits.setText( strNew.b );
            queueSize = bufSizeNew;
        }
        else {
            MasterGUI.me.lblNotCurBufSizeVal.setText( "(new => " + strNew.both() + ")" );
            MasterGUI.me.lblCurBufSizeVal.setText( strOrig.a );
            MasterGUI.me.lblCurBufSizeUnits.setText( strOrig.b );
            queueSize = bufSizeOrig;
        }
        
        // update the router with its new buffer size (it takes the number in packets)
        int queueSizeInPackets = queueSize / ControlParams.BYTES_PER_PACKET + 1;
        rtrCommander.command( RouterCmd.CMD_SET_BUF_SZ.code, queueSizeInPackets );
        
        MasterGUI.me.lblBufSizeVal.setText( Integer.toString(queueSizeInPackets) );
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
        sendCommands( ClientCmd.CMD_BPS.code, this.payloadBW, SendValueType.SV_SPLIT );
    }

    public int getNumFlows() {
        return numFlows;
    }

    public void setNumFlows(int num_flows) throws IllegalArgValException {
        if( num_flows <= 0 || num_flows > 65535 )
            throw( new IllegalArgValException("Number of flows must be between 1 and 65535") );
        
        this.numFlows = (short)num_flows;
        recomputeBufferSize();
        sendCommands( ClientCmd.CMD_FLOWS.code, this.numFlows, SendValueType.SV_SPLIT );
    }

    public boolean isUseNumFlows() {
        return useNumFlows;
    }

    public void setUseNumFlows(boolean useNumFlows) {
        this.useNumFlows = useNumFlows;
    }

    void shutdown() {
        sendCommands( ClientCmd.CMD_EXIT.code, 0, SendValueType.SV_EXACT );
    }

    public enum ClientCmd {
        CMD_FLOWS(0),
        CMD_BPS(1),
        CMD_INTERVAL(2),
        CMD_EXIT(3);
        
        public final int code;
        ClientCmd( int code ) { this.code = code; }
    }
    
    public enum RouterCmd {
        CMD_GET_RATE(0),
        CMD_SET_RATE(1),
        CMD_GET_BUF_SZ(2),
        CMD_SET_BUF_SZ(3);
        
        public final int code;
        RouterCmd( int code ) { this.code = code; }
    }
    
    /**
     * Monitor the connection with a client.
     */
    public class ClientCommander {
        /** the socket which connects this Command to its client */
        protected final Socket s;

        /** output stream to write to the socket */
        protected final BufferedOutputStream out;

        /** input stream to read from the socket */
        protected final BufferedInputStream in;

        /** 
         * Sets up a client commander with the specified socket
         * @param clientSocket  the socket to communicate with
         */
        public ClientCommander(final Socket clientSocket) {
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
        
        public void commandWrite( int code, int value ) {
            try {
                //write the code (one byte)
                byte b = (byte)code;
                out.write(b);
                
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
            
        public int command( int code, int value ) {
            commandWrite( code, value );
            return 0;
        }
    }
    
    public static int bytes_to_int( byte[] buf, int offset ) {
        int ret;
        ret =   buf[offset] << 24;
        ret += (buf[offset+1] << 16);
        ret += (buf[offset+2] << 8);
        ret +=  buf[offset+3];
        return ret;
    }
    
    /** Monitor the connection with a router. */
    public class RouterCommander extends ClientCommander {
        /** 
         * Sets up a router commander with the specified socket
         * @param clientSocket  the socket to communicate with
         */
        public RouterCommander(final Socket clientSocket) {
            super( clientSocket );
        }
        
        public int command( int code, int value ) {
            commandWrite( code, value );
            
            int ret = 0;
            if( code == RouterCmd.CMD_GET_BUF_SZ.code || code == RouterCmd.CMD_GET_RATE.code ) {
                try {
                    //read each byte in the return value
                    ret =   in.read() << 24;
                    ret += (in.read() << 16);
                    ret += (in.read() << 8);
                    ret +=  in.read();
                    
                    if( code == RouterCmd.CMD_GET_BUF_SZ.code ) {
                        MasterGUI.me.lblBufSizeVal.setText( Integer.toString(ret) );
                    }
                    else {
                        // convert the value of the register to something meaningful to the user
                        double rate = 1000 * 1000 * 1000; // base rate is 1Gbps
                        for( int i=1; i<ret; i++ )
                            rate /= 2;
                        StringPair sp = formatBits( ((int)rate) * 8 );
                        MasterGUI.me.lblRateLimVal.setText( sp.a );
                        MasterGUI.me.lblRateLimUnits.setText( sp.b );
                    }
                } catch( IOException e ) {
                    System.err.println( "command " + code + " / " + value + " => failed: " + e.getMessage() );
                    System.exit( 1 );
                }
            }
            return ret;
        }
    }
    
    /** Listen for stats announcements from the router. */
    public class RouterStatsListenerThread extends Thread {
        int port;
        
        RouterStatsListenerThread( int port ) {
            this.port = port;
        }
        
        public void run() {
            DatagramSocket dsocket;
            byte[] buf;
            DatagramPacket packet;
            long prev_total = 0;
            
            /* establish a socket for the stats port */
            try {
                dsocket = new DatagramSocket(port);
                buf = new byte[16];
                packet = new DatagramPacket(buf, buf.length);
            } catch( SocketException e ) {
                System.err.println( "Error: UDP socket setup failed for Router Stats Thread: " + e.getMessage() );
                System.exit( 1 );
                return;
            }
            
            /* listen for updates until the end of time */
            while (true) {
                try {
                    packet.setLength(buf.length);
                    dsocket.receive(packet);
                    
                    /* validate the size */
                    if( packet.getLength() < 16 ) {
                        System.err.println( "Warning: UDP packet too small to have stats" );
                        continue;
                    }
                    
                    /* extract the stats */
                    int sec       = bytes_to_int( buf,  0 );
                    int usec      = bytes_to_int( buf,  4 );
                    int bytes_sent = bytes_to_int( buf,  8 );
                    int queue_occ  = bytes_to_int( buf, 12 );
                    
                    /* compute throughput */
                    long total = ((long)sec)*1000*1000 + ((long)usec);
                    if( total < prev_total)
                        continue; /* ignore out of order packets */
                    
                    long diff = total - prev_total;
                    prev_total = total;
                    if( prev_total == 0 )
                        prev_total = total;
                        
                    double throughput = bytes_sent * 8 / diff;
                    double tic = total / 1024.0;
                    
                    /** add new data points to the graph */
                    MasterGUI.dataXput.add( tic, throughput );
                    MasterGUI.dataOcc.add(  tic, queue_occ  );
                    MasterGUI.dataQS.add(   tic, queueSize  );
                    
                    MasterGUI.me.lblXputVal.setText( Integer.toString((int)throughput) );
                    MasterGUI.me.lblQOccVal.setText( Integer.toString(queue_occ) );
                } catch( IOException e ) {
                    System.err.println( "Error: UDP stats receive failed: " + e.getMessage() );
                    System.exit( 1 );
                }
            }
        }
    }
}
