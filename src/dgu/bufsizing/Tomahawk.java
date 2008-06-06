package dgu.bufsizing;

import dgu.bufsizing.control.TomahawkController;
import java.awt.Graphics2D;

/**
 * Settings for the Tomahawk traffic generator.
 * @author David Underhill
 */
public class Tomahawk extends TrafficGenerator {
    private static final java.awt.Image ICON = java.awt.Toolkit.getDefaultToolkit().getImage("tomahawk.jpg");
    private int numFlows;
    private int output_Mbps;
    
    private final TomahawkController controller;

    public Tomahawk( String name, int x, int y, int commandPort, int numFlows, int output_Mbps ) {
        super( name, x, y );
        this.numFlows = numFlows;
        this.output_Mbps = output_Mbps;
        
        controller = new TomahawkController( commandPort );
    }
    
    public void drawIcon( Graphics2D gfx ) {
        gfx.drawImage( ICON, getX(), getY(), ICON_WIDTH, ICON_HEIGHT, null );
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
