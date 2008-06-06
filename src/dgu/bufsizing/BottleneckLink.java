package dgu.bufsizing;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Information about a bottleneck link.
 * @author David Underhill
 */
public class BottleneckLink extends Link {
    public static int THICKNESS = 4;
    
    private int bufSize_msec;
    private int rate_Mbps;
    private int dataPointsToKeep;
    
    public BottleneckLink( Node src, Node dst, int bufSize_msec, int rate_Mbps, int dataPointsToKeep ) {
        super( src, dst );
        this.bufSize_msec = bufSize_msec;
        this.rate_Mbps = rate_Mbps;
        this.dataPointsToKeep = dataPointsToKeep;
    }
    
    public void draw( Graphics2D gfx ) {
        // set color as gradient from green to red (with high saturation => green)
        int saturation = src.getSaturationTo(dst);
        gfx.setColor( new Color( 100 - saturation, saturation, 0 ) );
        
        // determine the starting point and line width/height
        int x, y, width, height;
        if( src.getX() < dst.getX() ) {
            x = src.getX();
            width = dst.getX() - src.getX();
        }
        else {
            x = dst.getX();
            width = src.getX() - dst.getX();
        }
        if( src.getY() < dst.getY() ) {
            y = src.getY();
            height = dst.getY() - src.getY();
        }
        else {
            y = dst.getY();
            height = src.getY() - dst.getY();
        }
        
        // make the smaller dimension WIDTH in size
        int diff;
        if( height < width ) {
            diff = THICKNESS - height;
            y += diff / 2;
            height = THICKNESS;
        }
        else {
            diff = THICKNESS - width;
            x += diff / 2;
            width = THICKNESS;
        }
        
        // draw the thick line
        gfx.drawRect( src.getX(), src.getY(), dst.getX(), dst.getY() );
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
    
    public String toString() {
        return src.toString() + " ---> " + dst.toString();
    }
    
}
