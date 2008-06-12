package dgu.bufsizing.control;

import dgu.bufsizing.BottleneckLink;
import dgu.bufsizing.DemoGUI;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Process event capture packets.
 * @author David Underhill
 */
public class EventProcessor extends Thread {
    /** maximum length of a datagram */
    private static final int MAX_PACKET_LEN = 1500;
    
    /** number of queues in the header */
    private static final int NUM_QUEUES = 8;
    
    /** the port to listen on for event capture packets */
    int port;
    
    /** socket to use for listening */
    DatagramSocket dsocket;
    
    EventProcessor( int port ) {
        this.port = port;
        
        /* establish a socket for the stats port */
        try {
            dsocket = new DatagramSocket(port);
        } catch( SocketException e ) {
            System.err.println( "Error: UDP socket setup failed for Event Processor Thread: " + e.getMessage() );
            System.exit( 1 );
            return;
        }
    }
    
    /**
     * Listens for new event capture packets and processes them with the 
     * assumption that they are for the first bottleneck on the first router.
     */
    public void run() {
        byte[] buf = new byte[MAX_PACKET_LEN];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        /* listen for updates until the end of time */
        while (true) {
            try {
                packet.setLength(buf.length);
                dsocket.receive(packet);

                /* extract the stats (assume router 0) */
                EventProcessor.handleEventCapPacket( 0, buf );
            } catch( IOException e ) {
                System.err.println( "Error: UDP stats receive failed: " + e.getMessage() );
                System.exit( 1 );
            }
        }
    }
    
    /** defines event type codes */
    private static enum EventType {
        TYPE_TS((byte)0),
        TYPE_ARRIVE((byte)1),
        TYPE_DEPART((byte)2),
        TYPE_DROP((byte)3);
        
        EventType( final byte t ) { 
            type = t;
        }
        public final byte type;
    }
    
    /** reads byte i to i+3 to form an int */
    private static int extractInt( byte[] buf, int i ) {
        int ret = 0;
        ret  = (buf[i]   & 0x000000FF) << 24;
        ret += (buf[i+1] & 0x0000FF00) << 16;
        ret += (buf[i+2] & 0x00FF0000) << 8;
        ret += (buf[i+3] & 0xFF000000);
        return ret;
    }
    
    /** reads byte i to i+3 to form a long (use for unsigned ints which may use the MSB) */
    private static long extractUintAsLong( byte[] buf, int i ) {
        long ret = 0;
        ret  = (buf[i]   & 0x000000FF) << 24;
        ret += (buf[i+1] & 0x0000FF00) << 16;
        ret += (buf[i+2] & 0x00FF0000) << 8;
        ret += (buf[i+3] & 0xFF000000);
        return ret;
    }
    
    /**
     * Processes a buffer containing an event capture packet.
     * @param routerIndex  index of the router the data belongs to
     * @param buf          datagram containing an event capture payload
     */
    public static void handleEventCapPacket( int routerIndex, byte[] buf ) {
        // always assume first bottleneck for now
        BottleneckLink b = DemoGUI.me.demo.getRouters().get(routerIndex).getBottleneckLink(0);
        
        // start processing at byte 1 (byte 0 isn't too interesting)
        int index = 1;
        int num_events = buf[index] & 0xFF; /* cast to an int so we properly interpret values > 127 */
        index += 1;
        
        // skip the sequence number
        index += 4;
        
        // get the timestamp before the queue data
        long timestamp_8ns = extractUintAsLong(buf, index+NUM_QUEUES*8)<<32L + extractUintAsLong(buf,index+4);
        if( !b.prepareForUpdate( timestamp_8ns ) )
            return; // old, out-of-order packet
        
        // get queue occupancy data
        for( int i=0; i<NUM_QUEUES; i++ ) {
            // update the queue with its new absolute value
            b.setOccupancy( timestamp_8ns, 8 * extractInt(buf, index) );
            index += 4;
            
            //skip size in packets
            index += 4;
        }
        
        // already got the timestamp; keep going
        index += 8;
        
        // process each event
        long timestamp_adjusted_8ns = timestamp_8ns;
        for( int i=0; i<num_events; i++ ) {
            int type = buf[index] & 0xFF;
            if( type == EventType.TYPE_TS.type )
                timestamp_8ns = extractUintAsLong(buf, index+NUM_QUEUES*8)<<32L + extractUintAsLong(buf,index+4);  
            else {
                // determine the # of bytes involved and the offset
                int val = extractInt( buf, index );
                int queue_id = (val & 0x0000001C) >> 2;
                if( queue_id != 2 ) {
                    // only pay attention to NF2C1 for now
                    continue;
                }                
                int plen_bytes = ((val & 0x00001FE0) >> 5) * 8;
                timestamp_adjusted_8ns = timestamp_8ns + ((val & 0xFFFFE000) >> 13);
                
                if( type == EventType.TYPE_ARRIVE.type )
                    b.arrival( timestamp_adjusted_8ns, plen_bytes );
                if( type == EventType.TYPE_ARRIVE.type )
                    b.departure( timestamp_adjusted_8ns, plen_bytes );
                else
                    b.dropped( timestamp_adjusted_8ns, plen_bytes );
            }
        }
        
        // refresh instantaneous readings over the interval from the previous
        //  update to the time of the last event in this update
        b.refreshInstantaneousValues( timestamp_adjusted_8ns );
    }
}
