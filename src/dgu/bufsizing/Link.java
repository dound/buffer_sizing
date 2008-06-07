package dgu.bufsizing;

import dgu.util.IllegalArgValException;
import java.awt.BasicStroke;
import java.awt.Graphics2D;

/**
 * Information about a link.
 * @author David Underhill
 */
public class Link<SOURCE_TYPE extends Node> implements Drawable {
    public static final int NF2C0 = 0;
    public static final int NF2C1 = 1;
    public static final int NF2C2 = 2;
    public static final int NF2C3 = 3;
    
    public static final BasicStroke STROKE_BOTTLENECK = new BasicStroke( 5.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER ); 
    
    protected final SOURCE_TYPE src;
    protected final Node dst;
    protected final byte queueID;
    
    /**
     * Constructs a new uni-directional link between src and dst.
     * @param src               The source of data on this link.
     * @param dst               The endpoint of this link.
     * @param queueID           The interface index for the queue on src for this link.
     * @throws IllegalArgValException  thrown if too many links already exist from src
     */
    public Link( SOURCE_TYPE src, Node dst, int queueID ) throws IllegalArgValException {
        this.src = src;
        this.dst = dst;
        this.queueID = (byte)queueID;
        
        src.addLink( this );
    }
    
    public void draw( Graphics2D gfx ) {
        gfx.drawLine( src.getX(), src.getY(), dst.getX(), dst.getY() );
    }
    
    public String toString() {
        return src.toString() + " ===> " + dst.toString();
    }
}
