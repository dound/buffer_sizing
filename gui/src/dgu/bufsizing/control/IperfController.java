package dgu.bufsizing.control;

import java.util.LinkedList;

/**
 * Interacts with iperf controller(s)
 * @author David Underhill
 */
public class IperfController extends Controller {
    public static final int BASE_PORT = 10273;
    public enum IperfCmd {
        CMD_SET_N((byte)0),
        CMD_SET_TGEN((byte)1);
        
        public final byte code;
        IperfCmd( byte code ) { this.code = code; }
    }
    
    /** information about a controller */
    private class ICInfo {
        public IperfController ic;
        public int n = 0;
        public ICInfo(IperfController ic) { this.ic = ic; }
    }
    
    /** list of all of the controllers which are available */
    private static LinkedList<ICInfo> controllers = new LinkedList<ICInfo>();
    
    /** total number of flows requested */
    private static int numFlows = 0;
    
    /** manages N across all iperf controllers such that the total N is the requested number */
    public static void setNumFlows(int n) {
        // ignore non-changes
        if( numFlows == n )
            return;
            
        // do nothing until we have a controller
        if( controllers.size() == 0 ) {
            System.err.println("unable to set numFlows -- no iperf controllers exist");
            return;
        }
        
        // generate new flows if the user want more
        while( n > numFlows ) {
            // determine which controller is responsible for the least # of flows
            ICInfo iciLeastFlows = controllers.get(0);
            for( ICInfo ici : controllers )
                if( iciLeastFlows.n > ici.n )
                    iciLeastFlows = ici;
            
            // tell the controller with the least # of flows to start another
            iciLeastFlows.n += 1;
            iciLeastFlows.ic.command(IperfCmd.CMD_SET_N, iciLeastFlows.n);
            numFlows += 1;
        }
        
        // stop old flows if the user want fewer
        while( n < numFlows ) {
            // determine which controller is responsible for the most # of flows
            ICInfo iciMostFlows = controllers.get(0);
            for( ICInfo ici : controllers )
                if( iciMostFlows.n < ici.n )
                    iciMostFlows = ici;
            
            // tell the controller with the least # of flows to start another
            iciMostFlows.n -= 1;
            iciMostFlows.ic.command(IperfCmd.CMD_SET_N, iciMostFlows.n);
            numFlows -= 1;
        }
    }
    
    public IperfController( int port ) {
        super( port, false );
        controllers.add(new ICInfo(this));
    }

    public String getTypeString() {
        return "Iperf";
    }
    
    /**
     * Executes the specified command.
     * 
     * @param cmd        the command to execute
     * @param value      the value to send with this command
     */
    public synchronized void command( IperfCmd cmd, int value ) {
        sendCommand( cmd.code, value );
    }
}
