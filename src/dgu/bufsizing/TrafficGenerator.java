package dgu.bufsizing;

import java.awt.Graphics2D;

/**
 * Parameters for a traffic generator.
 * @author David Underhill
 */
public abstract class TrafficGenerator extends Node {

    public TrafficGenerator( String name ) {
        super( name );
    }
    
    public void draw( Graphics2D gfx ) {
        throw new UnsupportedOperationException( "Not yet implemented." );
    }
    
    public abstract void drawIcon( Graphics2D gfx, int x, int y, int height, int width );   
    
    public abstract String getTrafficTypeString();
    
    public String getTypeString() {
        return "Traffic Generator [" + getTrafficTypeString() + "]";
    }

}
