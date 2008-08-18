package dgu.bufsizing;

import dgu.bufsizing.control.RouterController;
import dgu.util.swing.GUIHelper;
import java.awt.Graphics2D;

/**
 * Information about a router.
 * @author David Underhill
 */
public class Router extends Node {
    private static final java.awt.Image ICON = DemoGUI.chooseImage("images/nf2router-lo.png", "images/nf2router.png");
    protected static final int ICON_WIDTH  = DemoGUI.ratioH768(50);
    protected static final int ICON_HEIGHT = DemoGUI.ratioH768(50);
    
    public static final int   ROUTER_DIAMETER = DemoGUI.ratioH768(10);
    
    private final RouterController controller;
    private final int statsPort;
    
    public Router( String name, String nameShort, Importance importance, int x, int y, int commandPort, int statsPort ) {
        super( name, nameShort, importance, x, y );
        
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
        gfx.drawImage( ICON, getX()-ICON_WIDTH/2+DemoGUI.ratioH768(13), getY()-ICON_HEIGHT/2-DemoGUI.ratioH768(10), ICON_WIDTH, ICON_HEIGHT, null );
        
        // put its name on top
        drawName( gfx, getX(), getY() + DemoGUI.ratioH768(15) + gfx.getFontMetrics().getHeight() );
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
