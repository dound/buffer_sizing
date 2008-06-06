package dgu.bufsizing;

import java.awt.Graphics2D;

/**
 * Settings for the Iperf traffic generator.
 * @author David Underhill
 */
public class Iperf extends TrafficGenerator {
    private boolean useTCP;
    private int output_Mbps;

    public Iperf( String name, boolean useTCP, int output_Mbps ) {
        super( name );
        this.useTCP = useTCP;
        this.output_Mbps = output_Mbps;
    }
    
    public void drawIcon( Graphics2D gfx, int x, int y, int height, int width ) {
        throw new UnsupportedOperationException( "Not yet implemented." );
    }

    public String getTrafficTypeString() {
        return "iperf";
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
