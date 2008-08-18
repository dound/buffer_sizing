package dgu.bufsizing;

import java.awt.Graphics2D;

/**
 * A node which is neither a traffic generator or router.
 * @author David Underhill
 */
public class Receiver extends Node {
    private static final java.awt.Image ICON = DemoGUI.chooseImage("images/nf2router-lo.png", "images/nf2router.png");
    protected static final int ICON_WIDTH  = DemoGUI.ratioH768(50);
    protected static final int ICON_HEIGHT = DemoGUI.ratioH768(50);
    
    public Receiver( String name, String nameShort, Importance importance, int x, int y ) {
        super( name, nameShort, importance, x, y );
    }
    
    protected void drawNode( Graphics2D gfx ) {
        // draw the receiver as a router
        gfx.drawImage( ICON, getX()-ICON_WIDTH/2+DemoGUI.ratioH768(10), getY()-ICON_HEIGHT/2-DemoGUI.ratioH768(10), ICON_WIDTH, ICON_HEIGHT, null );
        
        // put its name on top
        drawName( gfx, getX(), getY() + DemoGUI.ratioH768(13) + gfx.getFontMetrics().getHeight() );
    }

    public String getTypeString() {
        return "Receiver";
    }
}
