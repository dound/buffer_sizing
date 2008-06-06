package dgu.bufsizing;

import java.awt.Graphics2D;
import java.util.LinkedList;
import java.util.Vector;

/**
 * Information about a router.
 * @author David Underhill
 */
public class Router extends Node {
    public static int MAX_LINKS = 4;
    private Vector<Link> link = new Vector<Link>( MAX_LINKS );
    private Vector<BottleneckLink> bottleneck = new Vector<BottleneckLink>();
    
    public Router( String name ) {
        super( name );
    }
    
    public void draw( Graphics2D gfx ) {
        throw new UnsupportedOperationException( "Not yet implemented." );
    }

    public String getTypeString() {
        return "NetFGPA";
    }
    
    public Link getLink( int i ) {
        return link.get(i);
    }
}
