package dgu.bufsizing;

import dgu.util.IllegalArgValException;
import java.awt.Color;
import java.awt.Graphics2D;
import org.jfree.data.xy.XYSeries;

/**
 * Information about a bottleneck link.
 * @author David Underhill
 */
public class BottleneckLink extends Link<Router> {
    // don't have JFreeChart worry about sorting data or looking for duplicates (performance!)
    private static final boolean AUTOSORT_SETTING = false;
    private static final boolean ALLOW_DUPS_SETTING = false;
    
    // user-defined variables on this bottleneck link
    private int bufSize_msec;
    private int rateLimit_kbps;
    private boolean notifyOnChange;
    
    // empirical data collected from the router
    private final XYSeries dataThroughput = new XYSeries("Throughput",AUTOSORT_SETTING,ALLOW_DUPS_SETTING);
    private final XYSeries dataQueueOcc   = new XYSeries("Queue Occupancy",AUTOSORT_SETTING,ALLOW_DUPS_SETTING);
    private final XYSeries dataDropRate   = new XYSeries("Drop Rate",AUTOSORT_SETTING,ALLOW_DUPS_SETTING);
    
    // last throughput data point (replicated to avoid critical section / locking) (float => one word ~=> atomic)
    private float instantaneousUtilization = 0.0f;
    
    // settings for buffer size and rate limit as set by the user
    private final XYSeries dataBufSize = new XYSeries("Max Throughput",AUTOSORT_SETTING,ALLOW_DUPS_SETTING);
    private final XYSeries dataRateLimit = new XYSeries("Max Link Rate",AUTOSORT_SETTING,ALLOW_DUPS_SETTING);
    
    /** Puts xys into manual notification mode and sets a limit on the number of data points it may track. */
    public static void prepareXYSeries( XYSeries xys, int maxDataPoints ) {
        xys.setNotify( false );
        xys.setMaximumItemCount( maxDataPoints );
    }
    
    /**
     * Constructs a new uni-directional bottleneck link between src and dst.
     * @param src               The source of data on this link.
     * @param dst               The endpoint of this link.
     * @param bufSize_msec      Initial buffer size in milliseconds
     * @param rateLimit_kbps    Initial rate limit in kilobits per second
     * @param dataPointsToKeep  Maximum number of data points to keep around
     * @param notifyOnChange    Whether to redraw any attached charts when the mutator methods are called.
     * @throws IllegalArgValException  thrown if too many links already exist from src
     */
    public BottleneckLink( Router src, Node dst, 
                           int bufSize_msec, int rateLimit_kbps, int dataPointsToKeep, boolean notifyOnChange ) 
                           throws IllegalArgValException {
        super( src, dst );
        this.notifyOnChange = notifyOnChange;
        
        prepareXYSeries( dataThroughput, dataPointsToKeep );
        prepareXYSeries( dataQueueOcc,   dataPointsToKeep );
        prepareXYSeries( dataDropRate,   dataPointsToKeep );
        
        prepareXYSeries( dataBufSize,   dataPointsToKeep );
        prepareXYSeries( dataRateLimit, dataPointsToKeep );
        
        // add an initial bogus data point (mutators require at least one data point to be present)
        dataBufSize.add(   0, 0, false );
        dataRateLimit.add( 0, 0, false );
        
        // set the initial values
        this.bufSize_msec = bufSize_msec;
        this.rateLimit_kbps = rateLimit_kbps;
        
        // update the plots appropriately
        setBufSize_msec( bufSize_msec );
        setRateLimit_kbps( rateLimit_kbps );
    }
    
    public void draw( Graphics2D gfx ) {
        // get a local copy of the current utilization
        float saturation = instantaneousUtilization;
        
        // setup context for drawing the bottleneck link (redder => less saturated/lower utilization)
        gfx.setColor( new Color( 1.0f - saturation, saturation, 0.0f ) );
        gfx.setStroke( STROKE_BOTTLENECK );

        gfx.drawLine( src.getX(), src.getY(), dst.getX(), dst.getY() );

        // restore defaults
        gfx.setColor( Drawable.COLOR_DEFAULT );
        gfx.setStroke( Drawable.STROKE_DEFAULT );
    }
    
    public synchronized void addDataPoint( long time_msec, int throughput_kbps, long queueOcc_bytes, float dropRate_percent ) {
        if( throughput_kbps < rateLimit_kbps )
            instantaneousUtilization = throughput_kbps / (float)rateLimit_kbps;
        else
            instantaneousUtilization = 1.0f;
        
        dataThroughput.add( time_msec, throughput_kbps,  notifyOnChange );
        dataQueueOcc.add(   time_msec, queueOcc_bytes,   notifyOnChange );
        dataDropRate.add(   time_msec, dropRate_percent, notifyOnChange );
    }
    
    public int getBufSize_msec() {
        return bufSize_msec;
    }

    public synchronized void setBufSize_msec( int bufSize_msec ) {
        // remove the old fake endpoint (don't notify yet)
        dataBufSize.remove( dataBufSize.getItemCount() - 1, false );
        
        // add the real end point of the previous buffer size (don't notify yet)
        dataBufSize.add( System.currentTimeMillis(), this.bufSize_msec, false );
        
        // set the new buffer size
        this.bufSize_msec = bufSize_msec;
        
        // add the real start point of the new buffer size (don't notify yet)
        dataBufSize.add( System.currentTimeMillis(), this.bufSize_msec, false );
        
        // add a fake endpoint at the end of time and refresh the graph
        dataBufSize.add( Long.MAX_VALUE, this.bufSize_msec, notifyOnChange );
    }

    public int getRateLimit_kbps() {
        return rateLimit_kbps;
    }

    public synchronized void setRateLimit_kbps(int rateLimit_kbps) {
        // remove the old fake endpoint (don't notify yet)
        dataRateLimit.remove( dataRateLimit.getItemCount() - 1, false );
        
        // add the real end point of the previous buffer size (don't notify yet)
        dataRateLimit.add( System.currentTimeMillis(), this.bufSize_msec, false );
        
        // set the new buffer size
        this.rateLimit_kbps = rateLimit_kbps;
        
        // add the real start point of the new buffer size (don't notify yet)
        dataRateLimit.add( System.currentTimeMillis(), this.bufSize_msec, false );
        
        // add a fake endpoint at the end of time and refresh the graph
        dataRateLimit.add( Long.MAX_VALUE, this.bufSize_msec, notifyOnChange );
    }

    public XYSeries getDataThroughput() {
        return dataThroughput;
    }

    public XYSeries getDataQueueOcc() {
        return dataQueueOcc;
    }

    public XYSeries getDataDropRate() {
        return dataDropRate;
    }

    public XYSeries getDataBufSize() {
        return dataBufSize;
    }

    public XYSeries getDataRateLimit() {
        return dataRateLimit;
    }
    
    public String toString() {
        return src.toString() + " ---> " + dst.toString();
    }
}
