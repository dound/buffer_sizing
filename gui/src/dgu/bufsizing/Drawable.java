package dgu.bufsizing;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Toolkit;

/**
 * Designates an object which may be drawn.
 * @author David Underhill
 */
public interface Drawable {
    public static final Image       BACKGROUND_IMG = Toolkit.getDefaultToolkit().getImage("images/usa_relief_cropped_squished.png");
    public static final BasicStroke STROKE_DEFAULT = new BasicStroke( 1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER ); 
    public static final BasicStroke STROKE_THICK   = new BasicStroke( 2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER ); 
    public static final BasicStroke STROKE_THICK3  = new BasicStroke( 3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER ); 
    public static final Color       COLOR_DEFAULT  = Color.BLACK;
    public static final Color       COLOR_SELECTED = Color.ORANGE;
    public static final Paint       PAINT_DEFAULT  = COLOR_DEFAULT;
    public static final Composite   COMPOSITE_OPAQUE = AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 1.0f );
    public static final Composite   COMPOSITE_HALF = AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.5f );
    
    public abstract void draw( Graphics2D gfx );
}
