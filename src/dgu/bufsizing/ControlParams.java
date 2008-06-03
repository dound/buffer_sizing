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

    private int queueSizeBytes;
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
        if( rateLim > 2 )
            setRateLimit( rateLim - 1 );
    }
    
    public void increaseRateLim() {
        if( rateLim < 16 )
            setRateLimit( rateLim + 1 );
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
    
    public StringPair formatBits( long b, boolean toBytes ) {
        long bytes = b / (toBytes ? 8 : 1);
        int units = 0;
        while( bytes >= 10000 ) {
            bytes /= 1000;
            units += 1;
        }
        String strUnit;
        switch( units ) {
            case  0: strUnit = "";  break;
            case  1: strUnit = "k"; break;
            case  2: strUnit = "M"; break;
            case  3: strUnit = "G"; break;
            case  4: strUnit = "T"; break;
            case  5: strUnit = "P"; break;
            default: strUnit = "?"; break;
        }
        
        ControlParams.StringPair ret = new ControlParams.StringPair();
        ret.a = Long.toString( bytes );
        ret.b = strUnit + (toBytes ? "B" : "b");
        return ret;
    }
    
    /** define the packet size as the normal MTU since that is what steady-state TCP will send */
    public static final int BYTES_PER_PACKET = 1500;
    
    public void recomputeBufferSize() {
        int bufSizeOrig = linkBW * delay / 1000;
        int bufSizeNew = (int)(bufSizeOrig / Math.sqrt(numFlows));
        StringPair strOrig = formatBits(bufSizeOrig, true);
        StringPair strNew = formatBits(bufSizeNew, true);
        
        if( useNumFlows ) {
            MasterGUI.me.lblNotCurBufSizeVal.setText( "(trad => " + strOrig.both() + ")" );
            MasterGUI.me.lblCurBufSizeVal.setText( strNew.a );
            MasterGUI.me.lblCurBufSizeUnits.setText( strNew.b );
            queueSizeBytes = bufSizeNew / 8;
        }
        else {
            MasterGUI.me.lblNotCurBufSizeVal.setText( "(new => " + strNew.both() + ")" );
            MasterGUI.me.lblCurBufSizeVal.setText( strOrig.a );
            MasterGUI.me.lblCurBufSizeUnits.setText( strOrig.b );
            queueSizeBytes = bufSizeOrig / 8;
        }
        
        // update the router with its new buffer size (it takes the number in packets)
        int queueSizeInPackets = queueSizeBytes / ControlParams.BYTES_PER_PACKET + 1;
        rtrCommander.command( RouterCmd.CMD_SET_BUF_SZ.code, queueSizeInPackets );
        
        MasterGUI.me.lblBufSizeVal.setText( Integer.toString(queueSizeInPackets) );
        MasterGUI.range2.setRange(0, queueSizeBytes);
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
        if( payload_bw_bps < 1000 || payload_bw_bps > 1000 * 1000 * 1000 )
            throw( new IllegalArgValException("Payload rate must be between 1kbps and 1Gbps") );
        
        this.payloadBW = payload_bw_bps;
        sendCommands( ClientCmd.CMD_BPS.code, this.payloadBW, SendValueType.SV_SPLIT );
    }

    public void setRateLimit( int newRate ) {
        rateLim = newRate;
        rtrCommander.command( RouterCmd.CMD_SET_RATE.code, newRate );
        double rate = 1000 * 1000 * 1000; // base rate is 1Gbps
        for( int i=2; i<newRate; i++ )
            rate /= 2;
        
        StringPair sp = formatBits( ((long)rate) * 8, false );
        MasterGUI.me.lblRateLimVal.setText( sp.a );
        MasterGUI.me.lblRateLimUnits.setText( sp.b + "ps (" + rateLim +")" ); 
        if( !MasterGUI.rangeUseAuto ) MasterGUI.range.setRange( 0, rate * 8.0 );
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
        protected final OutputStream out;

        /** input stream to read from the socket */
        protected final InputStream in;

        /** 
         * Sets up a client commander with the specified socket
         * @param clientSocket  the socket to communicate with
         */
        public ClientCommander(final Socket clientSocket) {
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
                        setRateLimit( ret );
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
        
        private boolean first = true;
        public void run() {
            DatagramSocket dsocket;
            byte[] buf;
            DatagramPacket packet;
            long prev_time = 0;
            long prev_bytes = 0;
            
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
                    long time     = ((long)sec)*1000*1000 + ((long)usec);
                    long bytes    = ((long)8) * bytes_to_int( buf,  8 );
                    int queue_occ_words  = bytes_to_int( buf, 12 ); /* 8-byte words! */
                    int queue_occ_bytes = queue_occ_words * 8;
                    
                    /* ignore old packets which arrive out or order */
                    if( time < prev_time )
                        continue;
                    
                    /* determine if we can compute xput yet */
                    if( first ) {
                        first = false;
                        prev_time = time;
                        prev_bytes = bytes;
                        continue;
                    }

                    /* determine diffs */
                    long diff_times = time - prev_time;
                    long diff_bytes = bytes - prev_bytes;
                    if( diff_bytes < 0 )
                        diff_bytes = 0;

                    /* save new prev values */
                    prev_time = time;
                    prev_bytes = bytes;

                    /* compute throughput in bits per second */       
                    double throughput = diff_bytes * 8 / diff_times;
                    double time_millis = time / 1000.0;
                    
                    /* update graph with new data points if not paused */
                    if( !MasterGUI.pause ) {
                        /** add new data points to the graph (if not paused) */
                        synchronized(MasterGUI.me) {
                            MasterGUI.dataXput.add( time_millis, throughput      );
                            MasterGUI.dataOcc.add(  time_millis, queue_occ_bytes );
                            MasterGUI.dataQS.add(   time_millis, queueSizeBytes  );
                        }
                    }
                    
                    /* update instantaneous readings */
                    StringPair p = formatBits( (int)throughput, false );
                    MasterGUI.me.lblXputVal.setText( p.a );
                    MasterGUI.me.lblXputUnits.setText( p.b );
                    
                    p = formatBits( queue_occ_bytes, true );
                    MasterGUI.me.lblQOccVal.setText( p.a );
                    MasterGUI.me.lblQOccUnits.setText( p.b );
                } catch( IOException e ) {
                    System.err.println( "Error: UDP stats receive failed: " + e.getMessage() );
                    System.exit( 1 );
                }
            }
        }
    }
}
//