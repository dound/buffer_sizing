package dgu.bufsizing.control;

import dgu.bufsizing.BottleneckLink;
import dgu.bufsizing.DemoGUI;
import java.io.IOException;

/**
 * Interacts with the NetFPGA router client.
 * @author David Underhill
 */
public class RouterController extends Controller {
    public enum RouterCmd {
        CMD_SET_RATE((byte)1),    /* kbps */
        CMD_SET_BUF_SZ((byte)3);  /* packets */
        
        public final byte code;
        RouterCmd( byte code ) { this.code = code; }
    }
    
    UpdateInfoProcessor uip = new UpdateInfoProcessor();
    
    public RouterController( String ip, int port ) {
        super( ip, port );
    }

    public void startUpdateInfoProcessorThread() {
        uip.start();
    }
    
    public String getTypeString() {
        return "Router";
    }
    
    public static int translateRateLimitRegToBitsPerSec( int reg ) {
        // cap it to reasonable values
        if( reg < 2 )
            reg = 2;
        else if( reg > 16 )
            reg = 16;
            
        // determine how much to divide the base rate by based on the bit
        int div = 1;
        while( reg-- > 2 )
            div *= 2;

        // 1Gbps is the base
        return (1000*1000*1000) / (div / 2);
    }
    
    /**
     * Executes the specified command.
     * 
     * @param cmd        the command to execute
     * @param queueNum   the queue this command affects (must be 0, 1, 2, or 3)
     * @param value      the value to send with this command
     * 
     * @return the value returned by the command (0 if the command is a SET command)
     */
    public synchronized int command( RouterCmd cmd, byte queueNum, int value ) {
        sendCommand( cmd.code, queueNum, value );
        return 0;
    }
    
    public class UpdateInfo {
        public int sec;
        public int usec;
        public int arrived;
        public int departed;
        public int current; 
    }
    
    /**
     * Retrieves an update info packet from the stream.
     */
    public void receiveUpdate(final UpdateInfo u) throws IOException {
        u.sec      = readInt();
        u.usec     = readInt();
        u.arrived  = readInt();
        u.departed = readInt();
        u.current  = readInt();
    }
    
    /**
     * Process update info packets which are simply summaries of the full event 
     * capture traffic.
     */
    public class UpdateInfoProcessor extends Thread {
        public static final boolean USE_PACKETS = false;
        
        /**
         * Listens for new event capture packets and processes them with the 
         * assumption that they are for the first bottleneck on the first router.
         */
        public void run() {
            final UpdateInfo u = new UpdateInfo();
            long lastErrorMsg_millis = 0;
            boolean gotUpdate = false;
            
            // always assume first bottleneck on the first router for now
            BottleneckLink b = DemoGUI.me.demo.getRouters().get(0).getBottleneckLinkAt(0);
            
            // auto-extend timing is weird, so just plot line with updates ...
            // updates are infrequent in this mode anyway
            b.setAutoThreshLines(false);
            
            /* listen for updates until the end of time */
            while (true) {
                // try to get the next update
                try {
                    receiveUpdate(u);
                    gotUpdate = true;
                } catch( IOException e ) {
                    gotUpdate = false;
                    
                    if( lastErrorMsg_millis + Controller.TIME_BETWEEN_ERROR_MSGS_MILLIS < System.currentTimeMillis() ) {
                        System.err.println( "Error: stats receive failed: " + e.getMessage() );
                        lastErrorMsg_millis = System.currentTimeMillis();
                    }
                    
                    // wait a little before trying again after a failure
                    DemoGUI.msleep(500);
                }
                
                // process each update we receive
                if( gotUpdate ) {
                    long timestamp_usec = ((u.sec * 1000L * 1000L) + u.usec);
                    long timestamp_8ns = timestamp_usec * 1000L / 8L;
                    
                    // set the occupancy at the start of this packet
                    b.setOccupancy( timestamp_8ns, u.current, false );
                    
                    // don't plot the occupancy for these yet because we're doing all arrivals,
                    // then all departures, which isn't the right order, which means the max can
                    // be larger than reality (and in fact probably would be, and often larger than max buffer size!)
                    b.arrival( timestamp_8ns, u.arrived, true );
                    b.departure( timestamp_8ns, u.departed, true );
                    
                    // now that we have the end value, plot that value (would be even better if we knew the end timestamp
                    b.plotCurrentOccupancy(timestamp_8ns);
                    
                    //System.err.println( "update at " + timestamp_8ns + " => cur=" + u.current + " / arr=" + u.arrived + " / dep=" + u.departed );
                    
                    // refresh instantaneous readings over the interval from the previous
                    //  update to the time of the last event in this update
                    b.refreshInstantaneousValues( timestamp_8ns );
                }
            }
        }
    }
}
