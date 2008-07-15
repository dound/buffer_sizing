package dgu.bufsizing;

import java.awt.Graphics2D;
import java.util.LinkedList;

/**
 * Settings for the Iperf traffic generator.
 * @author David Underhill
 */
public class Iperf extends TrafficGenerator {
    private static final java.awt.Image ICON = java.awt.Toolkit.getDefaultToolkit().getImage("iperf.gif");
    private static final int INIT_PORT = 5000;
    private static final int MAX_IPERFS = 100;
    
    private boolean useTCP;
    private int output_Mbps;
    private LinkedList<Process> procs = new LinkedList<Process>();
    private int portOn = 5000;
    private final String cmd;
    
    /** creates a default iperf traffic generator which uses TCP */
    public Iperf(String dstIP) {
        this( dstIP, "iperf", "iperf", Importance.IMPORTANT, 0, 0, true, 0 );
    }
    
    public Iperf( String dstIP, String name, String nameShort, Importance importance, int x, int y, boolean useTCP, int output_Mbps ) {
        super( dstIP, name, nameShort, importance, x, y );
        this.useTCP = useTCP;
        this.output_Mbps = output_Mbps;
        this.cmd = "iperf -c " + dstIP + " -t 84600";
    }
    
    public void drawIcon( Graphics2D gfx ) {
        gfx.drawImage( ICON, getX()-ICON_WIDTH/2, getY()-ICON_HEIGHT/2, ICON_WIDTH, ICON_HEIGHT, null );
    }

    public String getTrafficTypeString() {
        return "iperf";
    }
    
    public void setNumFlows(int n) {
        Runtime run = Runtime.getRuntime();
        if( procs.size() < n ) {
            if( procs.size() == MAX_IPERFS ) {
                System.err.println("iperf maxed out on flows at "+ MAX_IPERFS + " (cant do " + n + ")");
                return;
            }
            
            // add new iperfs if we don't have enough (assume we have enough remote servers!)
            try {
                procs.add( run.exec(cmd) );
            }
            catch( Exception e ) {
                System.err.println( "iperf exec error: " + e.getMessage() );
                return;
            }
            
            // advance to the next available port
            portOn += 1;
            if( portOn >= INIT_PORT + MAX_IPERFS )
                portOn = INIT_PORT;
        } else {
            // kill existing iperfs if we have too many
            while( procs.size()  > n )
                procs.pop().destroy();
        }
    }

    public boolean getUseTCP() {
        return useTCP;
    }

    public void setUseTCP( boolean useTCP ) {
        this.useTCP = useTCP;
    }

    public int getOutput_Mbps() {
        return output_Mbps;
    }

    public void setOutput_Mbps( int output_Mbps ) {
        this.output_Mbps = output_Mbps;
    }
}
