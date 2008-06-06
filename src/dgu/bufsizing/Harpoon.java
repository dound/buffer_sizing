package dgu.bufsizing;

import java.awt.Graphics2D;

/**
 * Settings for the Harpoon traffic generator.
 * @author David Underhill
 */
public class Harpoon extends TrafficGenerator {
    private static final java.awt.Image ICON = java.awt.Toolkit.getDefaultToolkit().getImage("harpoon.png");
    
    public Harpoon( String name, int x, int y ) {
        super( name, x, y );
    }
    
    public void drawIcon( Graphics2D gfx ) {
        gfx.drawImage( ICON, getX(), getY(), ICON_WIDTH, ICON_HEIGHT, null );
    }

    public String getTrafficTypeString() {
        return "Harpoon";
    }
}
