package dgu.bufsizing;

import java.awt.Graphics2D;

/**
 * Parameters for a traffic generator.
 * @author David Underhill
 */
public abstract class TrafficGenerator extends Node {
    protected static final int ICON_WIDTH  = 50;
    protected static final int ICON_HEIGHT = 50;
    
    public TrafficGenerator( String name, String nameShort, int x, int y ) {
        super( name, nameShort, x, y );
    }
    
    public void drawNode( Graphics2D gfx ) {
        drawIcon( gfx );
        drawName( gfx, getX(), getY() + ICON_HEIGHT / 2 );
    }
    
    public abstract void drawIcon( Graphics2D gfx );
    
    public abstract String getTrafficTypeString();
    
    public String getTypeString() {
        return "Traffic Generator [" + getTrafficTypeString() + "]";
    }
}
