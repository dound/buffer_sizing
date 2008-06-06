package dgu.bufsizing;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Ellipse2D;

/**
 * Information about a router.
 * @author David Underhill
 */
public class Router extends Node {
    private static final int   ROUTER_DIAMETER = 75;
    private static final Paint PAINT_ROUTER    = new GradientPaint(   0,   0, Color.BLUE,
                                                                    100, 100, new Color(0,0,128),
                                                                    true );
    
    private final Ellipse2D objForDrawing;
    
    public Router( String name, int x, int y ) {
        super( name, x, y );
        objForDrawing = new Ellipse2D.Float( x, y, ROUTER_DIAMETER, ROUTER_DIAMETER );
    }
    
    public void drawNode( Graphics2D gfx ) {
        // draw the router
        gfx.setPaint( PAINT_ROUTER );
        gfx.fill( objForDrawing );
        
        // put its name on top
        gfx.setPaint( Color.WHITE );
        drawName( gfx, getX(), getY() + ROUTER_DIAMETER / 2, ROUTER_DIAMETER );
        
        // restore the default paint
        gfx.setPaint( PAINT_DEFAULT );
    }

    public String getTypeString() {
        return "NetFGPA";
    }
}
