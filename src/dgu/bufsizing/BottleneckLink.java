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
    public static final BasicStroke STROKE_BOTTLENECK_SEL_OUTLINE = new BasicStroke( 9.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER ); 
    public static final Color COLOR_QUEUE_FILL = new Color( 0xFF, 0xAA, 0x11 );
    
    // don't have JFreeChart worry about sorting data or looking for duplicates (performance!)
    private static final boolean AUTOSORT_SETTING = false;
    private static final boolean ALLOW_DUPS_SETTING = true;
    
    // user-defined variables on this bottleneck link
    private boolean useRuleOfThumb = true;
    private int numFlows = 1;
    private int bufSize_msec;
    private int rateLimit_bps;
    private boolean selected;
    
    // recently collected data
    private long time_offset_ns8 = 0;
    private long prev_time_offset_begin_ns8 = 0;
    private long prev_time_offset_end_ns8 = 0;
    private long bytes_sent_since_last_update = 0;
    private int  queueOcc_bytes = -1;
    private static final int SEC_DIV_8NS  = 125000000;
    private static final int MSEC_DIV_8NS = 125000;
    
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
    private boolean forceSet;
    
    /** Returns the current time in units of 8ns (with millisecond resolution) */
    public static final long currentTime8ns() {
        return System.currentTimeMillis() * MSEC_DIV_8NS;
    }
    
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
                           int bufSize_msec, int rateLimit_kbps, int dataPointsToKeep ) 
                           throws IllegalArgValException {
        super( src, dst, queueID );
        
        prepareXYSeries( dataThroughput, dataPointsToKeep );
        prepareXYSeries( dataQueueOcc,   dataPointsToKeep );
        prepareXYSeries( dataDropRate,   dataPointsToKeep );
        
        prepareXYSeries( dataBufSize,   dataPointsToKeep );
        prepareXYSeries( dataRateLimit, dataPointsToKeep );
        
        // set the initial values
        this.bufSize_msec = bufSize_msec;
        this.rateLimit_bps = rateLimit_kbps * 1000;
        this.selected = false;
        
        // update the plots appropriately
        forceSet = true;
        setBufSize_msec( bufSize_msec );
        setRateLimit_kbps( rateLimit_kbps );
        forceSet = false;
    }
    static float temporary_counter = 0.0f; // xxx temporary
    
    /**
     * Constructs the current gradient color.
     * @param goodness  1.0 => green, 0.0 => red, between the two => constant hue mix
     * @return Color based on the current goodness
     */
    public static Color getCurrentGradientColor( float goodness ) {
        float extra = 2.0f * (0.5f - Math.abs( goodness - 0.5f ));
        return new Color( 1.0f - goodness, goodness, extra );
    }
    
    public void draw( Graphics2D gfx ) {
        // get a local copy of the current utilization
        float saturation = instantaneousUtilization;
        float queue_usage = instantaneousQueueOcc;
        
        // xxx temporary, just to test the visuals
        temporary_counter = temporary_counter + 0.01f;
        if( temporary_counter > 1.0f ) temporary_counter = 0.0f;
        saturation = queue_usage = temporary_counter;
        
        // draw the outline of the bottleneck link
        if( selected ) {
            gfx.setColor( Drawable.COLOR_SELECTED );
            gfx.setStroke( STROKE_BOTTLENECK_SEL_OUTLINE );
        }
        else
            gfx.setStroke( STROKE_BOTTLENECK_OUTLINE );
        gfx.drawLine( src.getX(), src.getY(), dst.getX(), dst.getY() );
        
        // draw the inner part of the bottleneck link (redder => less saturated/lower utilization)
        gfx.setColor( getCurrentGradientColor(saturation) );
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
        gfx.setColor( COLOR_QUEUE_FILL );
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
    
    /**
     * Synchronizes the approximate offset between our time domain and the 
     * router's timestamps.  It uses current time and doesn't do anything smart 
     * about incorporating round trip times as small differences here will not
     * matter in the long run.  We just need something "reasonably close."  This
     * method will only have an effect the first time it is called.
     * 
     * @param rtr_time_ns8  the router time in units of 8ns
     * @return true if the update is not older than the most recent update
     */
    public synchronized boolean prepareForUpdate( long rtr_time_ns8 ) {
        // initialize the offset if not already done
        if( time_offset_ns8 == 0 ) {
            // convert millis to units of 8ns
            time_offset_ns8 = currentTime8ns() - rtr_time_ns8;
        }
        
        // update 
        if( prev_time_offset_end_ns8 <= rtr_time_ns8 ) {
            prev_time_offset_begin_ns8 = rtr_time_ns8;
            return true;
        }
        else
            return false;
    }
    
    public synchronized void setOccupancy( long rtr_time_ns8, int num_bytes ) {
        //add the new data point
        queueOcc_bytes = num_bytes;
        dataQueueOcc.add( rtr_time_ns8 + time_offset_ns8, queueOcc_bytes );
    }
    
    public synchronized void arrival( long rtr_time_ns8, int num_bytes ) {
        setOccupancy( rtr_time_ns8, queueOcc_bytes + num_bytes );
    }
    
    public synchronized void departure( long rtr_time_ns8, int num_bytes ) {
        setOccupancy( rtr_time_ns8, queueOcc_bytes - num_bytes );
        bytes_sent_since_last_update += num_bytes;
    }
    
    public synchronized void dropped( long rtr_time_ns8, int num_bytes ) {
        setOccupancy( rtr_time_ns8, queueOcc_bytes - num_bytes );
    }
    
    public synchronized void refreshInstantaneousValues( long rtr_time_ns8 ) {
        // don't compute instantaneous values until we have received > 1 packet
        if( prev_time_offset_end_ns8 == 0 ) {
            prev_time_offset_end_ns8 = rtr_time_ns8;
            bytes_sent_since_last_update = 0;
            return;
        }
        
        // compute xput b/w end of last update and now (+1 to avoid div by 0 ... 8ns won't matter)
        float time_passed_ns8 = rtr_time_ns8 - prev_time_offset_end_ns8 + 1;
        float throughput_bps = (bytes_sent_since_last_update * SEC_DIV_8NS) / time_passed_ns8;
        
        // set new instantaneous utilizatoin value
        if( throughput_bps < rateLimit_bps )
            instantaneousUtilization = throughput_bps / (float)rateLimit_bps;
        else
            instantaneousUtilization = 1.0f;
        
        // compute the new instantaneous queue occupancy
        int bufSize_bytes = getBufSize_bytes(useRuleOfThumb);
        if( queueOcc_bytes < bufSize_bytes )
            instantaneousQueueOcc = queueOcc_bytes / (float)bufSize_bytes;
        else
            instantaneousQueueOcc = 1.0f;
        
        // plot the new throughput value
        long t = rtr_time_ns8 + time_offset_ns8;
        dataThroughput.add( t, throughput_bps, false );
        extendUserDataPoints( t );
    }
    
    public synchronized void clearData() {
        dataThroughput.clear( false );
        dataQueueOcc.clear(   false );
        dataDropRate.clear(   false );
        dataBufSize.clear(    false );
        dataRateLimit.clear(  false );
    }
    
    public synchronized void extendUserDataPoints( long time_ns ) {
        // remove the old temporary endpoints of user-controlled values
        if( dataBufSize.getItemCount() > 0 ) {
            dataBufSize.remove( dataBufSize.getItemCount() - 1, false );
            dataRateLimit.remove( dataRateLimit.getItemCount() - 1, false );
        }
        
        // add the new updated endpoints of user-controlled values and refresh the plot
        dataBufSize.add(   time_ns, getBufSize_bytes(this.useRuleOfThumb), false );
        dataRateLimit.add( time_ns, this.rateLimit_bps, false );
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
        return bufSize_msec * rateLimit_bps * 1000 / (8 * (useRuleOfThumb ? 1 : numFlows));
    }
    
    public int getBufSize_packets( boolean useRuleOfThumb ) {
        return bufSize_msec * rateLimit_bps * 1000 / (8 * 1500 * (useRuleOfThumb ? 1 : numFlows));
    }
    
    private void updateBufSize() {
        // tell the router about the new buffer size in terms of packets
        this.src.getController().command( RouterCmd.CMD_SET_BUF_SZ, queueID, getBufSize_packets(useRuleOfThumb) );
        
        // refresh the GUI
        if( DemoGUI.me != null ) DemoGUI.me.setBufferSizeText( this );
    }

    public synchronized void setBufSize_msec( int bufSize_msec ) {
        if( this.bufSize_msec == bufSize_msec && !forceSet )
            return;
        
        // add the end point of the old buffer size
        long t = currentTime8ns();
        dataBufSize.add( t, getBufSize_bytes(this.useRuleOfThumb), false  );
        
        // set the new buffer size
        this.bufSize_msec = bufSize_msec;
        
        // tell the router about the new buffer size in terms of packets
        updateBufSize();
        
        // add the start point of the new buffer size
        dataBufSize.add( t, this.getBufSize_bytes(this.useRuleOfThumb), false  );
        
        // add the temporary start point of the new buffer size
        dataBufSize.add( t, this.getBufSize_bytes(this.useRuleOfThumb), false  );
    }

    public int getRateLimit_kbps() {
        return rateLimit_bps * 1000;
    }

    public synchronized void setRateLimit_kbps(int rateLimit_kbps) {
        // translate the requested to rate to the register value to get the closest rate
        int tmp_rate = (int)(rateLimit_kbps * 1.5); // switch at the halfway point
        byte real_value = 2;
        while( tmp_rate < 1000 * 1000) {
            tmp_rate *= 2;
            real_value += 1;
            
            // stop at the max value
            if( real_value == 16 )
                break;
        }
        
        // translate the requested rate into the closest attainable rate
        rateLimit_kbps = RouterController.translateRateLimitRegToBitsPerSec( real_value ) / 1000;
        
        // do nothing if the requested rate hasn't changed since the last request
        if( this.rateLimit_bps == rateLimit_kbps * 1000 && !forceSet )
            return;
        
        // add the end point of the old rate
        long t = currentTime8ns();
        dataRateLimit.add( t, this.rateLimit_bps, false  );
        
        // set the new buffer size
        this.rateLimit_bps = rateLimit_kbps * 1000;
        if( DemoGUI.me != null ) DemoGUI.me.setRateLimitText( this );
        updateBufSize();
        
        // tell the router about the new rate limit
        src.getController().command( RouterCmd.CMD_SET_RATE, queueID, real_value );
        
        // add the start point of the new rate
        dataRateLimit.add( t, this.rateLimit_bps, false  );
        
        // add the temporary end point of the new rate
        dataRateLimit.add( t, this.rateLimit_bps, false  );
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
    
    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    public String toString() {
        return src.toString() + " ---> " + dst.toString();
    }
}
