package dgu.bufsizing;

import dgu.bufsizing.control.EventProcessor;
import dgu.bufsizing.control.RouterController;
import dgu.util.swing.GUIHelper;
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
    private static final java.awt.Image ICON = java.awt.Toolkit.getDefaultToolkit().getImage("images/nf2router.gif");
    protected static final int ICON_WIDTH  = 50;
    protected static final int ICON_HEIGHT = 50;
    
    public static final int   ROUTER_DIAMETER = 10;
    private static final Paint PAINT_ROUTER    = new GradientPaint(   0,   0, Color.BLUE,
                                                                    100, 100, new Color(0,0,128),
                                                                    true );
    private final Ellipse2D objForDrawing;
    
    private final RouterController controller;
    private final int statsPort;
    
    public Router( String name, String nameShort, Importance importance, int x, int y, int commandPort, int statsPort ) {
        super( name, nameShort, importance, x, y );
        objForDrawing = new Ellipse2D.Float( x-ROUTER_DIAMETER/2, y-ROUTER_DIAMETER/2, ROUTER_DIAMETER, ROUTER_DIAMETER );
        
        String ip = GUIHelper.getInput("What is the IP or hostname of the router controller server?", "LA1");
        System.out.println( "Will connect to router controller at " + ip );
        
        controller = new RouterController( ip, commandPort );
        this.statsPort = statsPort;
    }
    
    public void startStatsListener() {
        controller.startUpdateInfoProcessorThread();
        
        //if( statsPort != 0 )
        //    new EventProcessor( statsPort ).start();
    }
    
    protected void drawNode( Graphics2D gfx ) {
        // draw the router
        gfx.drawImage( ICON, getX()-ICON_WIDTH/2+10, getY()-ICON_HEIGHT/2-10, ICON_WIDTH, ICON_HEIGHT, null );
        
        // put its name on top
        drawName( gfx, getX(), getY() + 10 + gfx.getFontMetrics().getHeight() );
    }

    public int getNameY( Graphics2D gfx ) {
        return getY() + ROUTER_DIAMETER / 2 + gfx.getFontMetrics().getHeight();
    }
    
    public int getQueueY( Graphics2D gfx ) {
        return getY() + ROUTER_DIAMETER / 2 + gfx.getFontMetrics().getHeight();
    }
    
    public String getTypeString() {
        return "NetFGPA";
    }
    
    RouterController getController() {
        return controller;
    }
}
