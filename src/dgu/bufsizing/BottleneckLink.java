package dgu.bufsizing;

import dgu.bufsizing.control.RouterController;
import dgu.bufsizing.control.RouterController.RouterCmd;
import dgu.util.IllegalArgValException;
import dgu.util.swing.GUIHelper;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import org.jfree.data.xy.XYSeries;

/**
 * Information about a bottleneck link.
 * @author David Underhill
 */
public class BottleneckLink extends Link<Router> {
    private static final int QUEUE_WIDTH  = 50;
    private static final int QUEUE_HEIGHT = 12;
    public static final BasicStroke STROKE_OCC = new BasicStroke( 8.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL ); 
    public static final BasicStroke STROKE_BOTTLENECK = new BasicStroke( 5.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER ); 
    public static final BasicStroke STROKE_BOTTLENECK_OUTLINE = new BasicStroke( 7.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER ); 
    
    // don't have JFreeChart worry about sorting data or looking for duplicates (performance!)
    private static final boolean AUTOSORT_SETTING = false;
    private static final boolean ALLOW_DUPS_SETTING = true;
    
    // user-defined variables on this bottleneck link
    private boolean useRuleOfThumb = true;
    private int numFlows = 1;
    private int bufSize_msec;
    private int rateLimit_kbps;
    private boolean notifyOnChange;
    
    // empirical data collected from the router
    private final XYSeries dataThroughput = new XYSeries("Throughput",AUTOSORT_SETTING,ALLOW_DUPS_SETTING);
    private final XYSeries dataQueueOcc   = new XYSeries("Queue Occupancy",AUTOSORT_SETTING,ALLOW_DUPS_SETTING);
    private final XYSeries dataDropRate   = new XYSeries("Drop Rate",AUTOSORT_SETTING,ALLOW_DUPS_SETTING);
    
    // last throughput data point (replicated to avoid critical section / locking) (float => one word ~=> atomic)
    private float instantaneousUtilization = 0.0f;
    private float instantaneousQueueOcc    = 0.0f;
    
    // settings for buffer size and rate limit as set by the user
    private final XYSeries dataBufSize = new XYSeries("Buffer Size",AUTOSORT_SETTING,ALLOW_DUPS_SETTING);
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
     * @param queueID           The interface index for the queue on src for this link.
     * @param bufSize_msec      Initial buffer size in milliseconds
     * @param rateLimit_kbps    Initial rate limit in kilobits per second
     * @param dataPointsToKeep  Maximum number of data points to keep around
     * @param notifyOnChange    Whether to redraw any attached charts when the mutator methods are called.
     * @throws IllegalArgValException  thrown if too many links already exist from src
     */
    public BottleneckLink( Router src, Node dst, int queueID,
                           int bufSize_msec, int rateLimit_kbps, int dataPointsToKeep, boolean notifyOnChange ) 
                           throws IllegalArgValException {
        super( src, dst, queueID );
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
    static float temporary_counter = 0.0f; // xxx temporary
    public void draw( Graphics2D gfx ) {
        // get a local copy of the current utilization
        float saturation = instantaneousUtilization;
        float queue_usage = instantaneousQueueOcc;
        
        // xxx temporary, just to test the visuals
        temporary_counter = temporary_counter + 0.01f;
        if( temporary_counter > 1.0f ) temporary_counter = 0.0f;
        saturation = queue_usage = temporary_counter;
        
        // draw the outline of the bottleneck link
        gfx.setStroke( STROKE_BOTTLENECK_OUTLINE );
        gfx.drawLine( src.getX(), src.getY(), dst.getX(), dst.getY() );
        
        // draw the inner part of the bottleneck link (redder => less saturated/lower utilization)
        float extra = 2.0f * (0.5f - Math.abs( saturation - 0.5f ));
        gfx.setColor( new Color( 1.0f - saturation, saturation, extra ) );
        gfx.setStroke( STROKE_BOTTLENECK );
        gfx.drawLine( src.getX(), src.getY(), dst.getX(), dst.getY() );

        // draw the queue
        gfx.setColor( Drawable.COLOR_DEFAULT );
        gfx.setStroke( Drawable.STROKE_THICK );
        int x = src.getX() - QUEUE_WIDTH / 2;
        int y = src.getQueueY( gfx ) + QUEUE_HEIGHT * queueID;
        gfx.drawLine( x, y, x + QUEUE_WIDTH, y );
        gfx.drawLine( x, y + QUEUE_HEIGHT, x + QUEUE_WIDTH, y + QUEUE_HEIGHT );

        // fill the queue based on current occupancy
        extra = 2.0f * (0.5f - Math.abs( queue_usage - 0.5f ));
        gfx.setColor( new Color( queue_usage, 1.0f - queue_usage, extra ) );
        gfx.setStroke( STROKE_OCC );
        int width = (int)(QUEUE_WIDTH * queue_usage);
        int fillX = x + (QUEUE_WIDTH - width);
        int fillY = y + QUEUE_HEIGHT / 2;
        gfx.drawLine( fillX, fillY, fillX + width, fillY );
        
        // restore defaults
        gfx.setPaint( Drawable.PAINT_DEFAULT );
        gfx.setStroke( Drawable.STROKE_DEFAULT );
        
        // paint the name of the interface
        GUIHelper.drawCeneteredString( dst.getName(), gfx, x + QUEUE_WIDTH / 2, fillY + 4 );
    }
    
    public synchronized void addDataPoint( long time_msec, int throughput_kbps, long queueOcc_packets, float dropRate_percent ) {
        if( throughput_kbps < rateLimit_kbps )
            instantaneousUtilization = throughput_kbps / (float)rateLimit_kbps;
        else
            instantaneousUtilization = 1.0f;
        
        int bufSize_packets = getBufSize_packets(useRuleOfThumb);
        if( queueOcc_packets < bufSize_packets )
            instantaneousQueueOcc = queueOcc_packets / (float)bufSize_packets;
        else
            instantaneousQueueOcc = 1.0f;
        
        dataThroughput.add( time_msec, throughput_kbps,  notifyOnChange );
        dataQueueOcc.add(   time_msec, queueOcc_packets,   notifyOnChange );
        dataDropRate.add(   time_msec, dropRate_percent, notifyOnChange );
    }
    
    public boolean getUseRuleOfThumb() {
        return useRuleOfThumb;
    }

    public synchronized void setUseRuleOfThumb( boolean useRuleOfThumb ) {
        if( this.useRuleOfThumb == useRuleOfThumb )
            return;
        
        this.useRuleOfThumb = useRuleOfThumb;
        updateBufSize();
    }
    
    public int getNumFlows() {
        return numFlows;
    }

    public synchronized void adjustNumFlows( int adjust ) {
        this.numFlows += adjust;
        updateBufSize();
    }
    
    public int getBufSize_msec() {
        return bufSize_msec;
    }
    
    public int getBufSize_bytes( boolean useRuleOfThumb ) {
        return bufSize_msec * rateLimit_kbps / (8 * (useRuleOfThumb ? 1 : numFlows));
    }
    
    public int getBufSize_packets( boolean useRuleOfThumb ) {
        return bufSize_msec * rateLimit_kbps / (8 * 1500 * (useRuleOfThumb ? 1 : numFlows));
    }
    
    private void updateBufSize() {
        // tell the router about the new buffer size in terms of packets
        this.src.getController().command( RouterCmd.CMD_SET_BUF_SZ, queueID, getBufSize_packets(useRuleOfThumb) );
        
        // refresh the GUI
        if( DemoGUI.me != null ) DemoGUI.me.setBufferSizeText( this );
    }

    public synchronized void setBufSize_msec( int bufSize_msec ) {
        if( this.bufSize_msec == bufSize_msec )
            return;
        
        // remove the old fake endpoint (don't notify yet)
        dataBufSize.remove( dataBufSize.getItemCount() - 1, false );
        
        // add the real end point of the previous buffer size (don't notify yet)
        dataBufSize.add( System.currentTimeMillis(), this.bufSize_msec, false );
        
        // set the new buffer size
        this.bufSize_msec = bufSize_msec;
        
        // tell the router about the new buffer size in terms of packets
        updateBufSize();
        
        // add the real start point of the new buffer size (don't notify yet)
        dataBufSize.add( System.currentTimeMillis(), this.bufSize_msec, false );
        
        // add a fake endpoint at the end of time and refresh the graph
        dataBufSize.add( Long.MAX_VALUE, this.bufSize_msec, notifyOnChange );
    }

    public int getRateLimit_kbps() {
        return rateLimit_kbps;
    }

    public synchronized void setRateLimit_kbps(int rateLimit_kbps) {
        // translate the requested to rate to the register value to get the closest rate
        int tmp_rate = (int)(rateLimit_kbps * 1.5); // switch at the halfway point
        byte real_value = 2;
        while( tmp_rate < 1000 * 1000 ) {
            tmp_rate *= 2;
            real_value += 1;
            
            // stop at the max value
            if( real_value == 16 )
                break;
        }
        
        // translate the requested rate into the closest attainable rate
        rateLimit_kbps = RouterController.translateRateLimitRegToKilobitsPerSec( real_value );
        
        // do nothing if the requested rate hasn't changed since the last request
        if( this.rateLimit_kbps == rateLimit_kbps )
            return;
        
        // remove the old fake endpoint (don't notify yet)
        dataRateLimit.remove( dataRateLimit.getItemCount() - 1, false );
        
        // add the real end point of the previous buffer size (don't notify yet)
        dataRateLimit.add( System.currentTimeMillis(), this.rateLimit_kbps, false );
        
        // set the new buffer size
        this.rateLimit_kbps = rateLimit_kbps;
        if( DemoGUI.me != null ) DemoGUI.me.setRateLimitText( this );
        updateBufSize();
        
        // tell the router about the new rate limit
        src.getController().command( RouterCmd.CMD_SET_RATE, queueID, real_value );
        
        // add the real start point of the new buffer size (don't notify yet)
        dataRateLimit.add( System.currentTimeMillis(), this.rateLimit_kbps, false );
        
        // add a fake endpoint at the end of time and refresh the graph
        dataRateLimit.add( Long.MAX_VALUE, this.rateLimit_kbps, notifyOnChange );
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
