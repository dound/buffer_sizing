package dgu.bufsizing;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Information about a bottleneck link.
 * @author David Underhill
 */
public class BottleneckLink extends Link {
    
    public BottleneckLink( Node src, Node dst ) {
        super( src, dst );
    }
    
    public void draw( Graphics2D gfx ) {
        gfx.setColor( new Color( blah ) );
        gfx.drawLine( src.getX(), src.getY(), dst.getX(), dst.getY() );
    }
    
    public String toString() {
        return src.toString() + " ---> " + dst.toString();
    }
    
}
