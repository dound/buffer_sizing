package dgu.bufsizing;

import java.awt.Graphics2D;

/**
 * Settings for the Tomahawk traffic generator.
 * @author David Underhill
 */
public class Tomahawk extends TrafficGenerator {
    private int numFlows;
    private int output_Mbps;

    public Tomahawk( String name, int numFlows, int output_Mbps ) {
        super( name );
        this.numFlows = numFlows;
        this.output_Mbps = output_Mbps;
    }
    
    public void drawIcon( Graphics2D gfx, int x, int y, int height, int width ) {
        throw new UnsupportedOperationException( "Not yet implemented." );
    }

    public String getTrafficTypeString() {
        return "Tomahawk";
    }

    public int getNumFlows() {
        return numFlows;
    }

    public void setNumFlows( int numFlows ) {
        this.numFlows = numFlows;
    }

    public int getOutput_Mbps() {
        return output_Mbps;
    }

    public void setOutput_Mbps( int output_Mbps ) {
        this.output_Mbps = output_Mbps;
    }
}
