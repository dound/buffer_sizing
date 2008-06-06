package dgu.bufsizing;

import dgu.util.IllegalArgValException;
import java.awt.Graphics2D;

/**
 * Information about a router.
 * @author David Underhill
 */
public class Router extends Node {
    
    public Router( String name ) {
        super( name );
    }
    
    public void draw( Graphics2D gfx ) {
        throw new UnsupportedOperationException( "Not yet implemented." );
        
        
    }

    public String getTypeString() {
        return "NetFGPA";
    }
}
