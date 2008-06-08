package dgu.bufsizing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Ellipse2D;

/**
 * A node which is neither a traffic generator or router.
 * @author David Underhill
 */
public class Receiver extends Node {
    private static final int RECEIVER_DIAMETER = 10;
    private static final Paint PAINT_RECEIVER = new GradientPaint(  0,  0, Color.YELLOW,
                                                                   50, 50, Color.WHITE,
                                                                   true );
    private final Ellipse2D objForDrawing;
    
    
    public Receiver( String name, String nameShort, int x, int y ) {
        super( name, nameShort, x, y );
        objForDrawing = new Ellipse2D.Float( x-RECEIVER_DIAMETER/2, y-RECEIVER_DIAMETER/2, RECEIVER_DIAMETER, RECEIVER_DIAMETER );
    }
    
    public void drawNode( Graphics2D gfx ) {
        // draw the receiver
        gfx.setPaint( PAINT_RECEIVER );
        gfx.fill( objForDrawing );
        
        // restore the default paint and draw a border around the object
        gfx.setPaint( PAINT_DEFAULT );
        gfx.setStroke( Drawable.STROKE_THICK );
        gfx.draw( objForDrawing );
        gfx.setStroke( Drawable.STROKE_DEFAULT );
        
        // put its name on top
        drawName( gfx, getX(), getY() + RECEIVER_DIAMETER / 2 + gfx.getFontMetrics().getHeight() );
    }

    public String getTypeString() {
        return "Receiver";
    }
}
