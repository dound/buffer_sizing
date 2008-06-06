package dgu.bufsizing;

import java.awt.Graphics2D;

/**
 * Settings for the PlanetLab traffic generator.
 * @author David Underhill
 */
public class PlanetLab extends TrafficGenerator {
    
    public PlanetLab( String name ) {
        super( name );
    }
    
    public void drawIcon( Graphics2D gfx, int x, int y, int height, int width ) {
        throw new UnsupportedOperationException( "Not yet implemented." );
    }

    public String getTrafficTypeString() {
        return "PlanetLab";
    }
}
