package dgu.bufsizing;

import java.awt.Graphics2D;

/**
 * A group of end hosts.
 * @author David Underhill
 */
public class EndHostCluster extends Node {
    private static final java.awt.Image ICON  = DemoGUI.chooseImage("images/servers-lo.png", "images/servers.png");
    protected static final int ICON_WIDTH  = DemoGUI.ratioH768(50);
    protected static final int ICON_HEIGHT = DemoGUI.ratioH768(54);
    
    public EndHostCluster( String name, String nameShort, Importance importance, int x, int y ) {
        super( name, nameShort, importance, x, y );
    }
    
    protected void drawNode( Graphics2D gfx ) {
        gfx.drawImage( ICON, getX()-ICON_WIDTH/2, getY()-ICON_HEIGHT/2, ICON_WIDTH, ICON_HEIGHT, null );
        
        // put its name on top
        drawName( gfx, getX(), getY() - 7 + ICON_HEIGHT / 2 + gfx.getFontMetrics().getHeight() );
    }

    public String getTypeString() {
        return "End Host Cluster";
    }
}
