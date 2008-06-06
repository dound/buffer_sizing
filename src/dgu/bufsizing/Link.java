package dgu.bufsizing;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Information about a link.
 * @author David Underhill
 */
public class Link implements Drawable {
    protected Node src;
    protected Node dst;
    private int throughput;
    
    public Link( Node src, Node dst ) {
        this.src = src;
        this.dst = dst;
    }
    
    public void draw( Graphics2D gfx ) {
        gfx.setColor( Color.BLACK );
        gfx.drawLine( src.getX(), src.getY(), dst.getX(), dst.getY() );
    }
    
    public String toString() {
        return src.toString() + " ===> " + dst.toString();
    }

    public int getThroughput() {
        return throughput;
    }

    public void setThroughput(int throughput) {
        this.throughput = throughput;
    }
}
