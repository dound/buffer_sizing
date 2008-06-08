package dgu.bufsizing;

import java.awt.Graphics2D;

/**
 * Settings for the Iperf traffic generator.
 * @author David Underhill
 */
public class Iperf extends TrafficGenerator {
    private static final java.awt.Image ICON = java.awt.Toolkit.getDefaultToolkit().getImage("iperf.gif");
    private boolean useTCP;
    private int output_Mbps;

    public Iperf( String name, String nameShort, Importance importance, int x, int y, boolean useTCP, int output_Mbps ) {
        super( name, nameShort, importance, x, y );
        this.useTCP = useTCP;
        this.output_Mbps = output_Mbps;
    }
    
    public void drawIcon( Graphics2D gfx ) {
        gfx.drawImage( ICON, getX()-ICON_WIDTH/2, getY()-ICON_HEIGHT/2, ICON_WIDTH, ICON_HEIGHT, null );
    }

    public String getTrafficTypeString() {
        return "iperf";
    }

    public boolean getUseTCP() {
        return useTCP;
    }

    public void setUseTCP( boolean useTCP ) {
        this.useTCP = useTCP;
    }

    public int getOutput_Mbps() {
        return output_Mbps;
    }

    public void setOutput_Mbps( int output_Mbps ) {
        this.output_Mbps = output_Mbps;
    }
}
