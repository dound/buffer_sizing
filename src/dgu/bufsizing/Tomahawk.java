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

    public Tomahawk( String dstIP, String name, String nameShort, Importance importance, int x, int y, int commandPort, int numFlows, int output_Mbps ) {
        super( dstIP, name, nameShort, importance, x, y );
        this.numFlows = numFlows;
        this.output_Mbps = output_Mbps;
        
        controller = new TomahawkController( commandPort );
    }
    
    public void drawIcon( Graphics2D gfx ) {
        gfx.drawImage( ICON, getX()-ICON_WIDTH/2, getY()-ICON_HEIGHT/2, ICON_WIDTH, ICON_HEIGHT, null );
    }

    public String getTrafficTypeString() {
        return "Tomahawk";
    }
    
    public int getNumFlows() {
        return numFlows;
    }

    public void setNumFlows(int numFlows) {
        System.err.println("Warning: " + getTrafficTypeString() + "::setNumFlows not yet implemented");
        this.numFlows = numFlows;
    }

    public int getOutput_Mbps() {
        return output_Mbps;
    }

    public void setOutput_Mbps( int output_Mbps ) {
        this.output_Mbps = output_Mbps;
    }
}
