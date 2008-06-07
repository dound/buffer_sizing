package dgu.bufsizing;

import dgu.bufsizing.control.RouterController;
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
    
    private final RouterController controller;
    
    
    public Router( String name, int x, int y, int commandPort ) {
        super( name, x, y );
        objForDrawing = new Ellipse2D.Float( x-ROUTER_DIAMETER/2, y-ROUTER_DIAMETER/2, ROUTER_DIAMETER, ROUTER_DIAMETER );
        controller = new RouterController( commandPort );
    }
    
    public void drawNode( Graphics2D gfx ) {
        // draw the router
        gfx.setPaint( PAINT_ROUTER );
        gfx.fill( objForDrawing );
        
        // put its name on top
        gfx.setPaint( Color.WHITE );
        drawName( gfx, getX(), getY() - ROUTER_DIAMETER / 4 );
        
        // restore the default paint and draw a border around the object
        gfx.setPaint( PAINT_DEFAULT );
        gfx.setStroke( Drawable.STROKE_THICK );
        gfx.draw( objForDrawing );
        gfx.setStroke( Drawable.STROKE_DEFAULT );
    }

    public String getTypeString() {
        return "NetFGPA";
    }
    
    RouterController getController() {
        return controller;
    }
}
