package dgu.bufsizing;

import dgu.bufsizing.control.TomahawkController;
import dgu.bufsizing.control.TomahawkController.TomahawkCmd;
import dgu.util.swing.GUIHelper;
import java.awt.Graphics2D;

/**
 * Settings for the Tomahawk traffic generator.
 * @author David Underhill
 */
public class Tomahawk extends TrafficGenerator {
    private static final java.awt.Image ICON = java.awt.Toolkit.getDefaultToolkit().getImage("images/tomahawk.jpg");
    private int xput_bps = 0;
    private final TomahawkController controller;

    /** sets up a Tomahawk traffic generator operating over the default controller port */
    public Tomahawk( String dstIP ) {
        this( dstIP, "Tomahawk", "Tomahawk", Importance.IMPORTANT, 0, 0, Demo.DEFAULT_TRAFFIC_CONTROLLER_PORT );
    }
    
    public Tomahawk( String dstIP, String name, String nameShort, Importance importance, int x, int y, int commandPort ) {
        super( dstIP, name, nameShort, importance, x, y );    
        
        String ip = GUIHelper.getInput("What is the IP or hostname of the tomahawk controller server?", "");
        controller = new TomahawkController( ip, commandPort );
    }
    
    public void drawIcon( Graphics2D gfx ) {
        gfx.drawImage( ICON, getX()-ICON_WIDTH/2, getY()-ICON_HEIGHT/2, ICON_WIDTH, ICON_HEIGHT, null );
    }

    public String getTrafficTypeString() {
        return "Tomahawk";
    }
    
    public void setNumFlows(int numFlows) {
        System.err.println("Warning: " + getTrafficTypeString() + "::setNumFlows not yet implemented");
        controller.command(TomahawkCmd.CMD_FLOWS, numFlows);
    }
    
    public void destroy() {
        controller.command(TomahawkCmd.CMD_EXIT, 0);
    }

    public int getXput_bps() {
        return xput_bps;
    }

    public void setXput_bps( int bps ) {
        this.xput_bps = bps;
        controller.command(TomahawkCmd.CMD_BPS, bps);
    }
}
