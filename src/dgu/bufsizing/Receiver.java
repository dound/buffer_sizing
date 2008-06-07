package dgu.bufsizing;

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
    private static final int RECEIVER_DIAMETER = 50;
    private static final Paint PAINT_RECEIVER = new GradientPaint(  0,  0, Color.YELLOW,
                                                                   50, 50, Color.WHITE,
                                                                   true );
    private final Ellipse2D objForDrawing;
    
    public Receiver( String name, int x, int y ) {
        super( name, x, y );
        objForDrawing = new Ellipse2D.Float( x, y, RECEIVER_DIAMETER, RECEIVER_DIAMETER );
    }
    
    public void drawNode( Graphics2D gfx ) {
        // draw the receiver
        gfx.setPaint( PAINT_RECEIVER );
        gfx.fill( objForDrawing );
        
        // restore the default paint
        gfx.setPaint( PAINT_DEFAULT );
        
        // put its name on top
        drawName( gfx, getX(), getY() + RECEIVER_DIAMETER / 3, RECEIVER_DIAMETER );
    }

    public String getTypeString() {
        return "Receiver";
    }
}
