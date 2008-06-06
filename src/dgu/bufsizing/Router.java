package dgu.bufsizing;

import java.awt.Graphics2D;

/**
 * Information about a router.
 * @author David Underhill
 */
public class Router extends Node {
    private int bufSize_msec;
    private int rate_Mbps;
    private int dataPointsToKeep;

    public Router( String name, int bufSize_msec, int rate_Mbps, int dataPointsToKeep ) {
        super( name );
        this.bufSize_msec = bufSize_msec;
        this.rate_Mbps = rate_Mbps;
        this.dataPointsToKeep = dataPointsToKeep;
    }
    
    public void draw( Graphics2D gfx ) {
        throw new UnsupportedOperationException( "Not yet implemented." );
    }

    public String getTypeString() {
        return "NetFGPA";
    }

    public int getBufSize() {
        return bufSize_msec;
    }

    public void setBufSize( int bufSize_msec ) {
        this.bufSize_msec = bufSize_msec;
    }

    public int getRate() {
        return rate_Mbps;
    }

    public void setRate( int rate_Mbps ) {
        this.rate_Mbps = rate_Mbps;
    }
}
