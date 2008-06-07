package dgu.bufsizing;

import dgu.util.IllegalArgValException;
import java.awt.Graphics2D;
import java.util.LinkedList;

/**
 * Informaiton about a node.
 * @author David Underhill
 */
public abstract class Node implements Drawable {
    // connection information
    public static int MAX_LINKS = 4;
    private Link[] link = new Link[MAX_LINKS];
    private boolean[] isBottleneck = new boolean[MAX_LINKS];
    private LinkedList<BottleneckLink> bottlenecks = new LinkedList<BottleneckLink>();
    private int numLinks = 0;
    
    // basic properties for GUI stuff
    private final String name;
    private final int x;
    private final int y;
    
    public Node( String name, int x, int y ) {
        this.name = name;
        this.x = x;
        this.y = y;
        
        for( int i=0; i<MAX_LINKS; i++ )
            link[i] = null;
    }
    
    public final void draw( Graphics2D gfx ) {
        drawLinks( gfx );
        drawNode(  gfx );
    }
    
    public final void drawLinks( Graphics2D gfx ) {
        for( int i=0; i<numLinks; i++ )
            link[i].draw( gfx );
    }
    
    protected final void drawName( Graphics2D gfx, int x, int y ) {
        // center the string horizontally
        x -= gfx.getFontMetrics().stringWidth( name ) / 2;
        gfx.drawString( name, x, y );
    }

    public abstract void drawNode( Graphics2D gfx );

    public abstract String getTypeString();

    private void addLink( Link l, boolean bottleneck ) throws IllegalArgValException {
        if( numLinks >= MAX_LINKS )
            throw( new IllegalArgValException("Exceeded maximum number of links") );
        
        link[numLinks] = l;
        isBottleneck[numLinks] = bottleneck;
        numLinks += 1;
    }
    
    /**
     * Adds a new basic link from this router to another.
     * @param l  the link
     * @throws dgu.util.IllegalArgValException  thrown if too many links are added
     */
    void addLink( Link l ) throws IllegalArgValException {
        addLink( l, false );
    }
    
    /**
     * Adds a new bottleneck link from this router to another.
     * @param l  the link
     * @throws dgu.util.IllegalArgValException  thrown if too many links are added
     */
    void addLink( BottleneckLink l ) throws IllegalArgValException {
        addLink( l, true );
        bottlenecks.add( l );
    }
    
    public Link getLink( int i ) {
        return link[i];
    }
    
    public LinkedList<BottleneckLink> getBottlenecks() {
        return bottlenecks;
    }
    
    public BottleneckLink getBottleneckLink( int i ) {
        return (BottleneckLink)link[i];
    }
    
    public String getName() {
        return name;
    }
    
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String toString() {
        return name + " (" + getTypeString() + ")";
    }
}
