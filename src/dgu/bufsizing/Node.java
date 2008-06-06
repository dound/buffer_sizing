package dgu.bufsizing;

import dgu.util.IllegalArgValException;
import java.awt.Graphics2D;

/**
 * Informaiton about a node.
 * @author David Underhill
 */
public abstract class Node implements Drawable {
    // connection information
    public static int MAX_LINKS = 4;
    private Link[] link = new Link[MAX_LINKS];
    private boolean[] isBottleneck = new boolean[MAX_LINKS];
    private int numLinks = 0;
    
    // basic properties for GUI stuff
    private String name;
    private int x;
    private int y;
    private int width;
    private int height;
    
    public Node( String name ) {
        this.name = name;
        
        for( int i=0; i<MAX_LINKS; i++ )
            link[i] = null;
    }
    
    public abstract void draw( Graphics2D gfx );
    
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
    }
    
    public Link getLink( int i ) {
        return link[i];
    }
    
    public BottleneckLink getBottleneckLink( int i ) {
        return (BottleneckLink)link[i];
    }
    
    public String getName() {
        return name;
    }
    
    public void setName( String name ) {
        this.name = name;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String toString() {
        return name + " (" + getTypeString() + ")";
    }
}
