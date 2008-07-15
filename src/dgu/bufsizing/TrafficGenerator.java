package dgu.bufsizing;

import java.awt.Graphics2D;

/**
 * Parameters for a traffic generator.
 * @author David Underhill
 */
public abstract class TrafficGenerator extends Node {
    protected static final int ICON_WIDTH  = 50;
    protected static final int ICON_HEIGHT = 50;
    protected final String dstIP;
    
    public TrafficGenerator( String dstIP, String name, String nameShort, Importance importance, int x, int y ) {
        super( name, nameShort, importance, x, y );
        this.dstIP = dstIP;
    }
    
    protected void drawNode( Graphics2D gfx ) {
        drawIcon( gfx );
        drawName( gfx, getX(), getY() + ICON_HEIGHT / 2 );
    }
    
    public abstract void drawIcon( Graphics2D gfx );
    
    public abstract String getTrafficTypeString();
    
    public void setNumFlows(int n) {
        System.err.println("Warning: " + getTrafficTypeString() + "::setNumFlows not yet implemented");
    }
    
    public String getTypeString() {
        return "Traffic Generator [" + getTrafficTypeString() + "]";
    }
    
    public void setXput_bps(int bps) {
        /* no-op by default */
    }
}
