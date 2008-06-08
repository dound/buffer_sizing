package dgu.bufsizing;

import dgu.util.swing.GUIHelper;
import dgu.util.swing.binding.JComboBoxBound;
import dgu.util.swing.binding.JSliderBound;
import dgu.util.swing.binding.delegate.ListBasedComponentDelegate;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import javax.swing.ImageIcon;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.*;

/**
 * The GUI for our SIGCOMM demo.  Displays the topology and current link 
 * utilizatoins.  It also enables the user to modify buffer queues and 
 * bottlenecks in real-time.  In the future, we hope to support the modification 
 * of traffic in real-time too.  It can also displays detailed link information.
 * 
 * @author  David Underhill
 */
public class DemoGUI extends javax.swing.JFrame {
    public static final int TIME_BETWEEN_REFRESHES = 250;
    
    public static final String VERSION = "v0.01b";
    public static final java.awt.Image icon = java.awt.Toolkit.getDefaultToolkit().getImage("dgu.gif");
    private static JFreeChart chartXput, chartOcc;
    public static DemoGUI me;
    public final Demo demo;
    
    public static final XYSeriesCollection collXput = new XYSeriesCollection();
    public static final XYSeriesCollection collOcc  = new XYSeriesCollection();
    
    private static final int CANVAS_WIDTH  = 1028;
    private static final int CANVAS_HEIGHT =  250;
    private BufferedImage img = new BufferedImage( CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_RGB );
    private final Graphics2D gfx = (Graphics2D)img.getGraphics();   
    
    /** Creates new form DemoGUI */
    public DemoGUI( final Demo d ) {
        me = this;
        demo = d;
        
        setTitle( "Experimenting with Programmable Routers in Real Networks " + VERSION );
        GUIHelper.setGUIDefaults();
        createChartXput();
        createChartOcc();
        initComponents();
        prepareBindings();
        setIconImage( icon );
       
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width - 1024) / 2, (screenSize.height - 768) / 2, 1024, 768);
        
        // starts a thread to keep the topology map refreshed
        new Thread() {
            public void run() {
                gfx.setBackground( Color.WHITE );
                gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                gfx.setFont( GUIHelper.DEFAULT_FONT_BOLD );
                
                while( true ) {
                    d.redraw( gfx );
                    lblMap.setIcon( new ImageIcon( img ) );
                    BottleneckLink b = getSelectedBottleneck();
                    if( b != null ) {
                        synchronized( b ) {
                            b.extendUserDataPoints( System.currentTimeMillis() );
                            DemoGUI.me.refreshCharts();
                        }
                    }
                    try {
                        Thread.sleep( TIME_BETWEEN_REFRESHES );
                    } catch( InterruptedException e ) {
                        // no-op
                    }
                }
            }
        }.start();
    }
    
    private JFreeChart prepareChart( String title, String xAxis, String yAxis, XYSeriesCollection coll ) {
        JFreeChart chart = ChartFactory.createXYLineChart(
            title,
            xAxis,
            yAxis,
            coll,
            PlotOrientation.VERTICAL,
            true,  //legend
            false, //tooltips
            false  //URLs
        );    
         
        chart.setBackgroundPaint(GUIHelper.DEFAULT_BG_COLOR);
        chart.setBorderVisible(false);
        chart.setAntiAlias(false);
        chart.setTextAntiAlias(true);
        
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(false);
        plot.setRangeCrosshairVisible(true);
        
        ValueAxis domain = plot.getDomainAxis();
        domain.setLabelFont( GUIHelper.DEFAULT_FONT_BOLD_BIG );
        domain.setTickLabelsVisible(false);
        domain.setTickMarksVisible(false);
        domain.setAutoRange(true);
        
        ValueAxis range = plot.getRangeAxis();
        range.setLabelFont( GUIHelper.DEFAULT_FONT_BOLD_BIG );
        range.setStandardTickUnits( NumberAxis.createIntegerTickUnits() );
        range.setAutoRange( true );
        
        return chart;
    }
    
    private void createChartXput() {
        chartXput = prepareChart(
            "Throughput vs. Time",
            "Time",
            "Throughput (kbps)",
            collXput
        );
        
        XYPlot plot = (XYPlot) chartXput.getPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, new Color(0,0,128));
        renderer.setSeriesStroke(0, new BasicStroke(1f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL));
        renderer.setSeriesPaint(1, new Color(128,0,0));
        renderer.setSeriesStroke(1, new BasicStroke(3f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL));
        plot.setRenderer(0, renderer);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
    }
    
    private void createChartOcc() {
        chartOcc = prepareChart(
            "Buffer Occupancy and Size vs. Time",
            "Time",
            "Size (packets)",
            collOcc
        );    
         
        XYPlot plot = (XYPlot) chartOcc.getPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, new Color(0,0,128));
        renderer.setSeriesStroke(0, new BasicStroke(1f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL));
        renderer.setSeriesPaint(1, new Color(128,0,0));
        renderer.setSeriesStroke(1, new BasicStroke(3f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL));
        plot.setRenderer(0, renderer);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
    }
    
    BottleneckLink getSelectedBottleneck() {
        // get the bottleneck which is now selected (if any)
        Router rtr = (Router)cboBottleneck.getBindingDelegate().getBinding().getBoundItem();
        return rtr.getBottleneckLinkAt( cboBottleneck.getSelectedIndex() );
    }
    
    Node getSelectedNode() {
        // get the bottleneck which is now selected
        return (Node)cboBottleneck.getBindingDelegate().getBinding().getBoundItem();
    }
    
    void prepareBindings() {
        {
            ListBasedComponentDelegate d = cboBottleneck.getBindingDelegate();
            d.addBoundComponent( slBufferSize );
            d.addBoundComponent( slRateLimit );
            d.setPrimaryComponent( -2 );
            
            // manually populate the options whenever the combo box for bottlenecks is clicked
            cboBottleneck.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    try {
                        if( DemoGUI.me.demo.lastSelectedBottleneckLink != null )
                            DemoGUI.me.demo.lastSelectedBottleneckLink.setSelected( false );
                        
                        BottleneckLink b = getSelectedBottleneck();
                        synchronized( b ) {
                            // tell the link we're looking at it
                            DemoGUI.me.demo.lastSelectedBottleneckLink = b;
                            b.setSelected( true );
                            
                            // select the appropriate radio button for buffer sizing formula
                            if( b.getUseRuleOfThumb() )
                                optRuleOfThumb.setSelected( true );
                            else
                                optGuido.setSelected( true );

                            // bind this bottleneck's data to the chart and remove old data
                            DemoGUI.collXput.removeAllSeries( false );
                            DemoGUI.collXput.addSeries( b.getDataThroughput(), false );
                            DemoGUI.collXput.addSeries( b.getDataRateLimit(), false );
                            DemoGUI.collOcc.removeAllSeries( false );
                            DemoGUI.collOcc.addSeries( b.getDataQueueOcc(), false );
                            DemoGUI.collOcc.addSeries( b.getDataBufSize(), false );

                            DemoGUI.me.refreshCharts();
                            
                            // refresh the text
                            DemoGUI.me.setRateLimitText( b );
                            DemoGUI.me.setBufferSizeText( b );
                        }
                    } catch( Exception bleh ) {
                        //Do nothing, don't yet have a bottleneck
                    }
                }
            });
        }
        
        {
            ListBasedComponentDelegate d = cboNode.getBindingDelegate();
            d.addBoundComponent( cboBottleneck );
            d.setPrimaryComponent( -2 );
            
            // designate the selected node on click
            cboNode.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    try {
                        if( DemoGUI.me.demo.lastSelectedNode != null )
                            DemoGUI.me.demo.lastSelectedNode.setSelected( false );
                        
                        // tell the node we're looking at it
                        Node n = getSelectedNode();
                        DemoGUI.me.demo.lastSelectedNode = n;
                        n.setSelected( true );
                    }
                    catch( Exception e ) {
                        // node not yet selected
                    }
                }
            });
            
            d.changeBinding( demo );
            d.load();
            d.setSelectedIndex( 0 );
            
            // manually trigger the first selection
            cboNode.getActionListeners()[0].actionPerformed( null );
        }
    }
    
    /**
     * Refreshses the charts.  This should be called while the current 
     * bottleneck link's lock is held to prevent it data from changing during 
     * the refresh this induces.
     */
    private void refreshCharts() {
        DemoGUI.collXput.manuallyNotifyListeners();
        DemoGUI.collOcc.manuallyNotifyListeners();
    }
    
    public static class StringPair {
        public String a, b;
        String both() { return a + b; }
        public String toString() {
            return a + b;
        }
    }
    
    public enum UnitTime {
        TIME_NONE,
        TIME_MILLIS,
        TIME_SEC
    }
    
    public StringPair formatBits( long b, boolean toBytes, UnitTime timeUnits ) {
        long bytes = b / (toBytes ? 8 : 1);
        int units = 0;
        while( bytes >= 10000 ) {
            bytes /= (timeUnits==UnitTime.TIME_NONE ? 1024 : 1000);
            units += 1;
        }
        String strUnit;
        switch( units ) {
            case  0: strUnit = "";  break;
            case  1: strUnit = "k"; break;
            case  2: strUnit = "M"; break;
            case  3: strUnit = "G"; break;
            case  4: strUnit = "T"; break;
            case  5: strUnit = "P"; break;
            default: strUnit = "?"; break;
        }
        
        StringPair ret = new StringPair();
        ret.a = Long.toString( bytes );
        ret.b = strUnit + (toBytes ? "B" : "b");
        
        if( timeUnits == UnitTime.TIME_MILLIS )
            ret.b += "/ms";
        else if( timeUnits == UnitTime.TIME_SEC )
            ret.b += "/s";
        
        return ret;
    }
    
    public synchronized void setBufferSizeText( BottleneckLink l ) {
        synchronized( l ) {
            int size_msec        = l.getBufSize_msec();
            int size_old_bytes   = l.getBufSize_bytes(true);
            int size_old_packets = l.getBufSize_packets(true);
            int size_new_bytes   = l.getBufSize_bytes(false);
            int size_new_packets = l.getBufSize_packets(false);
            
            String str_size_old_bytes = formatBits(size_old_bytes*8,true,UnitTime.TIME_NONE).both();
            String str_size_new_bytes = formatBits(size_new_bytes*8,true,UnitTime.TIME_NONE).both();
            
            this.lblBufferSize.setText( "Buffer = " + size_msec + "ms" );
            this.optRuleOfThumb.setText( "Rule of Thumb = " 
                                         + str_size_old_bytes
                                         + " / " + size_old_packets + " pkts" );
            this.optGuido.setText( "Flow-Sensitive = " 
                                         + str_size_new_bytes
                                         + " / " + size_new_packets + " pkts" );
        }
    }
    
    public synchronized void setRateLimitText( BottleneckLink l ) {
        synchronized( l ) {
            this.lblRateLimit.setText( "Rate Limit = " 
                    + formatBits(1000*l.getRateLimit_kbps(), false, UnitTime.TIME_SEC) );
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        optGroupRule = new javax.swing.ButtonGroup();
        pnlDetails = new javax.swing.JPanel();
        lblBufferSize = new java.awt.Label();
        slBufferSize = new JSliderBound( "bufSize_msec" );
        lblRateLimit = new java.awt.Label();
        slRateLimit = new JSliderBound( "rateLimit_kbps" );
        pnlChartXput = new ChartPanel(chartXput);
        pnlSizing = new javax.swing.JPanel();
        optRuleOfThumb = new javax.swing.JRadioButton();
        optGuido = new javax.swing.JRadioButton();
        jSeparator1 = new javax.swing.JSeparator();
        cboNode = new JComboBoxBound( "getRouters", "" );
        cboBottleneck = new JComboBoxBound( "getBottlenecks", "" );
        lblBottleneck = new javax.swing.JLabel();
        lblNode = new javax.swing.JLabel();
        pnlChartOcc = new ChartPanel(chartOcc);
        pnlMap = new javax.swing.JPanel();
        lblMap = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        pnlDetails.setLayout(null);

        lblBufferSize.setAlignment(java.awt.Label.CENTER);
        lblBufferSize.setText("Buffer = 1000ms");
        pnlDetails.add(lblBufferSize);
        lblBufferSize.setBounds(510, 10, 250, 18);
        lblBufferSize.getAccessibleContext().setAccessibleName("Buffer = 1000ms !(1000kB / 512pkt)! vs. (1000 / 512))");

        slBufferSize.setMajorTickSpacing(100);
        slBufferSize.setMaximum(500);
        slBufferSize.setMinorTickSpacing(25);
        slBufferSize.setPaintTicks(true);
        slBufferSize.setValue(0);
        pnlDetails.add(slBufferSize);
        slBufferSize.setBounds(510, 15, 250, 45);

        lblRateLimit.setAlignment(java.awt.Label.CENTER);
        lblRateLimit.setText("Rate Limit = 100Mb/s");
        pnlDetails.add(lblRateLimit);
        lblRateLimit.setBounds(770, 10, 250, 18);

        slRateLimit.setMajorTickSpacing(100000);
        slRateLimit.setMaximum(1000000);
        slRateLimit.setMinorTickSpacing(50000);
        slRateLimit.setPaintTicks(true);
        slRateLimit.setValue(0);
        pnlDetails.add(slRateLimit);
        slRateLimit.setBounds(767, 15, 250, 45);

        pnlChartXput.setBorder(null);
        pnlChartXput.setLayout(null);
        pnlDetails.add(pnlChartXput);
        pnlChartXput.setBounds(0, 75, 509, 415);

        pnlSizing.setBorder(javax.swing.BorderFactory.createTitledBorder("Buffer Sizing Formula"));
        pnlSizing.setLayout(null);

        optGroupRule.add(optRuleOfThumb);
        optRuleOfThumb.setText("Rule of Thumb = 1000kB / 512pkt");
        optRuleOfThumb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optRuleOfThumbActionPerformed(evt);
            }
        });
        pnlSizing.add(optRuleOfThumb);
        optRuleOfThumb.setBounds(10, 15, 250, 22);

        optGroupRule.add(optGuido);
        optGuido.setText("Flow-Sensitive = 1000kB / 512pkt");
        optGuido.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optGuidoActionPerformed(evt);
            }
        });
        pnlSizing.add(optGuido);
        optGuido.setBounds(10, 35, 250, 22);

        pnlDetails.add(pnlSizing);
        pnlSizing.setBounds(245, 5, 265, 63);
        pnlDetails.add(jSeparator1);
        jSeparator1.setBounds(0, 0, 1025, 10);

        pnlDetails.add(cboNode);
        cboNode.setBounds(90, 10, 150, 25);

        pnlDetails.add(cboBottleneck);
        cboBottleneck.setBounds(90, 40, 150, 25);

        lblBottleneck.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblBottleneck.setText("Bottleneck:");
        pnlDetails.add(lblBottleneck);
        lblBottleneck.setBounds(5, 40, 80, 25);

        lblNode.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblNode.setText("Node:");
        pnlDetails.add(lblNode);
        lblNode.setBounds(5, 10, 80, 25);

        pnlChartOcc.setBorder(null);
        pnlChartOcc.setLayout(null);
        pnlDetails.add(pnlChartOcc);
        pnlChartOcc.setBounds(508, 75, 509, 415);

        getContentPane().add(pnlDetails);
        pnlDetails.setBounds(0, 249, 1028, 519);

        pnlMap.setLayout(null);

        lblMap.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        lblMap.setDoubleBuffered(true);
        pnlMap.add(lblMap);
        lblMap.setBounds(0, 0, 1018, 250);

        getContentPane().add(pnlMap);
        pnlMap.setBounds(0, 0, 1028, 250);
    }// </editor-fold>//GEN-END:initComponents

private void optRuleOfThumbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optRuleOfThumbActionPerformed
    try {
        BottleneckLink b = getSelectedBottleneck();
        if( !b.getUseRuleOfThumb() )
            b.setUseRuleOfThumb( true );
    } catch( Exception bleh ) {
        //Do nothing, don't yet have a bottleneck
    }
}//GEN-LAST:event_optRuleOfThumbActionPerformed

private void optGuidoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optGuidoActionPerformed
    try {
        BottleneckLink b = getSelectedBottleneck();
        if( b.getUseRuleOfThumb() )
            b.setUseRuleOfThumb( false );
    } catch( Exception bleh ) {
        //Do nothing, don't yet have a bottleneck
    }
}//GEN-LAST:event_optGuidoActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private dgu.util.swing.binding.JComboBoxBound cboBottleneck;
    private dgu.util.swing.binding.JComboBoxBound cboNode;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lblBottleneck;
    private java.awt.Label lblBufferSize;
    private javax.swing.JLabel lblMap;
    private javax.swing.JLabel lblNode;
    private java.awt.Label lblRateLimit;
    private javax.swing.ButtonGroup optGroupRule;
    private javax.swing.JRadioButton optGuido;
    private javax.swing.JRadioButton optRuleOfThumb;
    private org.jfree.chart.ChartPanel pnlChartOcc;
    private org.jfree.chart.ChartPanel pnlChartXput;
    private javax.swing.JPanel pnlDetails;
    private javax.swing.JPanel pnlMap;
    private javax.swing.JPanel pnlSizing;
    private dgu.util.swing.binding.JSliderBound slBufferSize;
    private dgu.util.swing.binding.JSliderBound slRateLimit;
    // End of variables declaration//GEN-END:variables
    
    /** Listens for radio button clicks and updates the rule of thumb setting appropriately. */
    private ActionListener varRadio = new ActionListener(){  
        public void actionPerformed(ActionEvent e) {
            LinkedList<BottleneckLink> c = (LinkedList<BottleneckLink>)cboBottleneck.getBindingDelegate().getBinding().getValue();
            BottleneckLink b = c.get( cboBottleneck.getBindingDelegate().getSelectedIndex() );
            b.setUseRuleOfThumb( e.getActionCommand().equals("rot") );
        }
    };
}
