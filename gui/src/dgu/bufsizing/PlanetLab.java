package dgu.bufsizing;

import java.awt.Graphics2D;

/**
 * Settings for the PlanetLab traffic generator.
 * @author David Underhill
 */
public class PlanetLab extends TrafficGenerator {
    private static final java.awt.Image ICON = java.awt.Toolkit.getDefaultToolkit().getImage("planetlab.png");
    
    public PlanetLab( String dstIP, String name, String nameShort, Importance importance, int x, int y ) {
        super( dstIP, name, nameShort, importance, x, y );
    }
    
    public void drawIcon( Graphics2D gfx ) {
        gfx.drawImage( ICON, getX()-ICON_WIDTH/2, getY()-ICON_HEIGHT/2, ICON_WIDTH, ICON_HEIGHT, null );
    }

    public String getTrafficTypeString() {
        return "PlanetLab";
    }
    
    public void setNumFlows(int n) {
        System.err.println("Warning: " + getTrafficTypeString() + "::setNumFlows not yet implemented");
    }
}
