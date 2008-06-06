package dgu.bufsizing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;

/**
 * Designates an object which may be drawn.
 * @author David Underhill
 */
public interface Drawable {
    public static final BasicStroke STROKE_DEFAULT = new BasicStroke( 1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER ); 
    public static final Color       COLOR_DEFAULT  = Color.BLACK;
    public static final Paint       PAINT_DEFAULT  = COLOR_DEFAULT;
    
    public abstract void draw( Graphics2D gfx );
}
