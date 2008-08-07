package dgu.bufsizing;

import dgu.bufsizing.control.IperfController;
import dgu.util.StringOps;
import dgu.util.swing.GUIHelper;
import dgu.util.swing.binding.JComboBoxBound;
import dgu.util.swing.binding.JSliderBound;
import dgu.util.swing.binding.delegate.ListBasedComponentDelegate;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
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
    public static final int DEFAULT_RTT = 75;
    public static final int TIME_BETWEEN_REFRESHES = 250;
    private static final int NUM_IPERF_CONTROLLERS = 2;
    
    public static final String VERSION = "v0.03b";
    public static final java.awt.Image icon = java.awt.Toolkit.getDefaultToolkit().getImage("dgu.gif");
    private static JFreeChart chartXput, chartOcc, chartResults;
    public static DemoGUI me;
    public final Demo demo;
    
    public static final XYSeriesCollection collXput = new XYSeriesCollection();
    public static final XYSeriesCollection collOcc  = new XYSeriesCollection();
    public static final XYSeriesCollection collRes  = new XYSeriesCollection();
    
    public static final int CANVAS_WIDTH  = 1028;
    public static final int CANVAS_HEIGHT =  250;
    private BufferedImage img = new BufferedImage( CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_RGB );
    private final Graphics2D gfx = (Graphics2D)img.getGraphics();   
    
    public static boolean freezeCharts = false;
    
    /** Creates new form DemoGUI */
    public DemoGUI( final Demo d ) {
        me = this;
        demo = d;
        
        setTitle( "Experimenting with Programmable Routers in Real Networks " + VERSION );
        GUIHelper.setGUIDefaults();
        createChartXput();
        createChartOcc();
        createChartResults();
        initComponents();
        pnlTGen.setVisible(false);
        initPopup();
        prepareBindings();
        setIconImage( icon );
       
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width - 1024) / 2, (screenSize.height - 768) / 2, 1024, 768);
        
        // starts a thread to keep the topology map refreshed
        new Thread() {
            public void run() {
                gfx.setBackground( Color.WHITE );
                gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                gfx.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                gfx.setFont( GUIHelper.DEFAULT_FONT_BOLD );
                gfx.setComposite( Drawable.COMPOSITE_OPAQUE );
                
                while( true ) {
                    d.redraw( gfx );
                    lblMap.setIcon( new ImageIcon( img ) );
                    BottleneckLink b = getSelectedBottleneck();
                    if( b != null ) {
                        synchronized( b ) {
                            b.extendUserDataPoints( BottleneckLink.currentTime8ns() );
                            if( !freezeCharts )
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
        
        //startDummyStatsThread();
        
        // start the stats listener threads
        for( Router r : demo.getRouters() )
            r.startStatsListener();
        
        // start the iperf controllers
        for( int i=0; i<NUM_IPERF_CONTROLLERS; i++ )
             new IperfController(IperfController.BASE_PORT + i);
    }
    
    public static final int RATE_LIM_VALUE_COUNT = 17;
    public static final long RATE_LIM_MAX_RATE = 4L * 1000L * 1000L * 1000L;
    private static final long RATE_LIM_MAX_RATE_ALLOWED = 1000L * 1000L * 1000L;
    public static final int RATE_LIM_MIN_REG_VAL = 2;
    JMenu mnuRateLim = new JMenu("Rate Limit");
    javax.swing.JCheckBoxMenuItem[] mnuRateLimVal = new JCheckBoxMenuItem[RATE_LIM_VALUE_COUNT];
    javax.swing.JCheckBoxMenuItem mnuToggleGraph;
    void initPopup() {
        // create the popup menu
        final JPopupMenu mnuPopup = new JPopupMenu();
        
        // add rate limit submenu to the popup menu
        mnuPopup.add( mnuRateLim );
        
        // add choices in the rate limit submenu
        long rate = RATE_LIM_MAX_RATE;
        for( int i=1; i<RATE_LIM_VALUE_COUNT; i++ ) {
            final int index = i;
            mnuRateLimVal[i] = new JCheckBoxMenuItem( this.formatBits(rate, false, UnitTime.TIME_SEC).both() );
            mnuRateLimVal[i].setSelected(false);
            mnuRateLimVal[i].addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    BottleneckLink bl = DemoGUI.me.getSelectedBottleneck();
                    if( bl != null )
                        bl.setRateLimitReg(index-1);
                }
            });
            
            // don't enable invalid choices
            if( rate > RATE_LIM_MAX_RATE_ALLOWED )
                mnuRateLimVal[i].setEnabled(false);
            
            mnuRateLim.add( mnuRateLimVal[i] );
            rate /= 2;
        }
        
        // add a delay / RTT submenu
        mnuSetRTT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BottleneckLink bl = DemoGUI.me.getSelectedBottleneck();
                if( bl != null ) {
                    int rtt = GUIHelper.getIntFromUser("What should the value of RTT be statically set to (ms)?", 
                                                       0, bl.getRTT_ms(), 1000);
                    bl.setRTT_ms(rtt);
                }
            }
        });
        mnuPopup.add(mnuSetRTT);
        
        // add a toggle for queue occ vs other chart
        mnuToggleGraph = new JCheckBoxMenuItem( "Toggle Graph" );
        mnuToggleGraph.setSelected(false);
        mnuToggleGraph.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if( mnuToggleGraph.isSelected() )
                    pnlChartRight.setChart( chartOcc );
                else
                    pnlChartRight.setChart( chartResults );
            }
        });
        mnuPopup.add(mnuToggleGraph);
        
        // attach the popup to other components
        final MouseAdapter pl = new MouseAdapter() {
            public void mousePressed(MouseEvent e) { showPopupIfTriggered(e); }
            public void mouseReleased(MouseEvent e) { showPopupIfTriggered(e); }

            /** listens for mouse clicks and fires the popup menu when appropriate */
            private void showPopupIfTriggered(MouseEvent e) {
                if( e.isPopupTrigger() )
                    mnuPopup.show( e.getComponent(), e.getX(), e.getY() );
            }
        };
        lblMap.addMouseListener( pl );
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
        boolean b = coll==collRes;
        domain.setTickLabelsVisible(b);
        domain.setTickMarksVisible(b);
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
            "Size (kB)",
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
    
    public XYLineAndShapeRenderer resultsRenderer = new XYLineAndShapeRenderer(true, false);
    private void createChartResults() {
        chartResults = prepareChart(
            "Utilization",
            "Number of Flows",
            "Buffer Size (kB) to Achieve 100% Link Utilization",
            collRes
        );    
         
        XYPlot plot = (XYPlot) chartResults.getPlot();
        XYLineAndShapeRenderer renderer = resultsRenderer;
        
        // theoretical rule of thumb
        renderer.setSeriesPaint(0, new Color(128,0,0));
        renderer.setSeriesStroke(0, new BasicStroke(3f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL));
        
        // theoretical guido
        renderer.setSeriesPaint(1, new Color(0,128,0));
        renderer.setSeriesStroke(1, new BasicStroke(3f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL));
        
        // measured
        renderer.setSeriesPaint(2, new Color(0,0,128));
        renderer.setSeriesStroke(2, new BasicStroke(1f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL));
        renderer.setSeriesLinesVisible(2, true);
        renderer.setSeriesShapesVisible(2, true);
        
        // measured (today's results)
        renderer.setSeriesPaint(3, new Color(128,0,128));
        renderer.setSeriesLinesVisible(3, false);
        renderer.setSeriesShapesVisible(3, true);
        
        // measured (current test)
        renderer.setSeriesPaint(4, new Color(0,0,0));
        renderer.setSeriesLinesVisible(4, false);
        renderer.setSeriesShapesVisible(4, true);
        
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
            d.addBoundComponent( slCustomBufferSize  );
            d.addBoundComponent( slNumFlows  );
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
                            switch(b.getBufSizeRule()) {
                                case RULE_OF_THUMB:  optRuleOfThumb.setSelected(true); break;
                                case FLOW_SENSITIVE: optGuido.setSelected(true);       break;
                                case CUSTOM:         optCustom.setSelected(true);      break;
                                default:             throw( new Error("Bad case in cboBottleneck's actionPerformed") );
                            }

                            // bind this bottleneck's data to the chart and remove old data
                            DemoGUI.collXput.removeAllSeries( false );
                            DemoGUI.collXput.addSeries( b.getDataThroughput(), false );
                            DemoGUI.collXput.addSeries( b.getDataRateLimit(), false );
                            DemoGUI.collOcc.removeAllSeries( false );
                            DemoGUI.collOcc.addSeries( b.getDataQueueOcc(), false );
                            DemoGUI.collOcc.addSeries( b.getDataBufSize(), false );
                            DemoGUI.collRes.removeAllSeries( false );
                            DemoGUI.collRes.addSeries( b.getDataRTheROT(), false  );
                            DemoGUI.collRes.addSeries( b.getDataRTheGuido(), false  );
                            DemoGUI.collRes.addSeries( b.getDataRMea(), false  );
                            DemoGUI.collRes.addSeries( b.getDataRToday(), false );
                            DemoGUI.collRes.addSeries( b.getDataRCur(), false );
                            
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
        DemoGUI.collRes.manuallyNotifyListeners();
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
            int size_msec        = l.getRTT_ms();
            int sizeROT_bytes    = l.getActualBufSize(BufferSizeRule.RULE_OF_THUMB);
            int sizeFS_bytes     = l.getActualBufSize(BufferSizeRule.FLOW_SENSITIVE);
            int sizeCustom_bytes = l.getActualBufSize(BufferSizeRule.CUSTOM);
            
            String strROT    = formatBits(sizeROT_bytes*8,    true, UnitTime.TIME_NONE).both();
            String strFS     = formatBits(sizeFS_bytes*8,     true, UnitTime.TIME_NONE).both();
            String strCustom = formatBits(sizeCustom_bytes*8, true, UnitTime.TIME_NONE).both();
            
            this.lblRuleOfThumb.setText( strROT );
            this.lblGuido.setText( strFS );
            this.lblCustom.setText( strCustom );
            this.slCustomBufferSize.repaint(); // redraw it so the markers for buf size are adjusted correctly
        }
    }
    
    public synchronized void setRateLimitText( BottleneckLink l ) {
        int selectedIndex = l.getRateLimit_regValue();
        for( int i=1; i<RATE_LIM_VALUE_COUNT; i++ )
            mnuRateLimVal[i].setSelected(i == selectedIndex);
    }
    
    public synchronized void setNumFlowsText( BottleneckLink l ) {
        int numFlows = l.getNumFlows();
        this.lblNumFlows.setText( "Number of Flows = " + numFlows );
    }

    private static boolean debug = false;
    private static int cb = 0;
    /** called whenever a slider is painted */
    public void sliderCallback(JSliderBound slider, java.awt.Graphics g) {
        if( slider == this.slCustomBufferSize ) {
            BottleneckLink bl = this.getSelectedBottleneck();
            if( bl == null )
                return;
            
            // half dimensions of the triangle
            int w = 6, h = 6, insetWidth = 8;
            
            // dimensions of the slider
            int xOff = insetWidth;
            int range = slider.getMaximum() - slider.getMinimum();
            
            BufferSizeRule[] rules = new BufferSizeRule[]{BufferSizeRule.RULE_OF_THUMB,
                                                          BufferSizeRule.FLOW_SENSITIVE};
            boolean top = true;
            for( BufferSizeRule rule : rules ) {
                // where should the the point be for this rule?
                int bufSize = bl.getActualBufSize(rule);
                if( debug && top ) { bufSize = cb; cb = (cb + (int)(.01*range)); if( cb > range ) cb = 0; }
                float xPer = bufSize / (float)range;
                int x = xOff;
                if( xPer <= 1.0 )
                    x += (int)(xPer * (slider.getWidth() - 2*insetWidth));
                else
                    x += slider.getWidth() - insetWidth; // off the charts!
                int y = top ? 5 : 11;
                
                // generate the coords for the triangle
                int[] xCoords = new int[]{x, x-w, x+w};
                int[] yCoords = new int[]{y, y-h, y-h};
                
                // paint the Rule of Thumb point on the slider
                g.setColor(top ? Color.RED : Color.BLUE);
                g.fillPolygon(xCoords, yCoords, 3);
                
                top = false;
                h = -h; // flip orientation of the triangle
            }
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
        optGroupTGen = new javax.swing.ButtonGroup();
        pnlDetails = new javax.swing.JPanel();
        slNumFlows = new JSliderBound( "numFlows" );
        pnlChartXput = new ChartPanel(chartXput);
        pnlSizing = new javax.swing.JPanel();
        optRuleOfThumb = new javax.swing.JRadioButton();
        optCustom = new javax.swing.JRadioButton();
        slCustomBufferSize = new JSliderBound( "customBufSize" );
        optGuido = new javax.swing.JRadioButton();
        lblCustom = new javax.swing.JLabel();
        lblRuleOfThumb = new javax.swing.JLabel();
        lblGuido = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        cboNode = new JComboBoxBound( "getRouters", "" );
        cboBottleneck = new JComboBoxBound( "getBottlenecks", "" );
        lblBottleneck = new javax.swing.JLabel();
        lblNode = new javax.swing.JLabel();
        pnlChartRight = new ChartPanel(chartResults);
        btnClearThisData = new javax.swing.JButton();
        lblNumFlows = new javax.swing.JLabel();
        pnlTGen = new javax.swing.JPanel();
        optIperf = new javax.swing.JRadioButton();
        optHarpoon = new javax.swing.JRadioButton();
        optTomahawk = new javax.swing.JRadioButton();
        optPlanetLab = new javax.swing.JRadioButton();
        pnlMap = new javax.swing.JPanel();
        lblMap = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        pnlDetails.setLayout(null);

        slNumFlows.setBorder(null);
        slNumFlows.setMajorTickSpacing(250);
        slNumFlows.setMaximum(200);
        slNumFlows.setMinorTickSpacing(100);
        slNumFlows.setValue(0);
        pnlDetails.add(slNumFlows);
        slNumFlows.setBounds(515, 25, 220, 16);

        pnlChartXput.setBorder(null);
        pnlChartXput.setLayout(null);
        pnlDetails.add(pnlChartXput);
        pnlChartXput.setBounds(0, 93, 509, 397);

        pnlSizing.setBorder(javax.swing.BorderFactory.createTitledBorder("Buffer Sizing Formula"));
        pnlSizing.setLayout(null);

        optGroupRule.add(optRuleOfThumb);
        optRuleOfThumb.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        optRuleOfThumb.setText("<html>Rule of Thumb: RTT &#183; C</html>");
        optRuleOfThumb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optRuleOfThumbActionPerformed(evt);
            }
        });
        pnlSizing.add(optRuleOfThumb);
        optRuleOfThumb.setBounds(10, 15, 170, 15);
        optRuleOfThumb.getAccessibleContext().setAccessibleName("Rule of Thumb (RTT * C)");

        optGroupRule.add(optCustom);
        optCustom.setFont(new java.awt.Font("Arial", 0, 12));
        optCustom.setText("Custom");
        optCustom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optCustomActionPerformed(evt);
            }
        });
        pnlSizing.add(optCustom);
        optCustom.setBounds(10, 55, 170, 15);

        slCustomBufferSize.setMajorTickSpacing(65536);
        slCustomBufferSize.setMaximum(524288);
        slCustomBufferSize.setMinorTickSpacing(5242880);
        slCustomBufferSize.setValue(0);
        pnlSizing.add(slCustomBufferSize);
        slCustomBufferSize.setBounds(30, 70, 165, 17);

        optGroupRule.add(optGuido);
        optGuido.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        optGuido.setText("<html>RTT &#183; C / &#8730;N</html>");
        optGuido.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optGuidoActionPerformed(evt);
            }
        });
        pnlSizing.add(optGuido);
        optGuido.setBounds(10, 35, 170, 15);

        lblCustom.setFont(new java.awt.Font("Courier", 0, 12)); // NOI18N
        lblCustom.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblCustom.setText("1000kB");
        lblCustom.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblCustomMouseClicked(evt);
            }
        });
        pnlSizing.add(lblCustom);
        lblCustom.setBounds(175, 55, 60, 15);

        lblRuleOfThumb.setFont(new java.awt.Font("Courier", 0, 12)); // NOI18N
        lblRuleOfThumb.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblRuleOfThumb.setText("10kB");
        pnlSizing.add(lblRuleOfThumb);
        lblRuleOfThumb.setBounds(175, 15, 60, 15);

        lblGuido.setFont(new java.awt.Font("Courier", 0, 12));
        lblGuido.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblGuido.setText("100MB");
        pnlSizing.add(lblGuido);
        lblGuido.setBounds(175, 35, 60, 15);

        pnlDetails.add(pnlSizing);
        pnlSizing.setBounds(240, 5, 245, 90);
        pnlDetails.add(jSeparator1);
        jSeparator1.setBounds(0, 0, 1025, 10);

        pnlDetails.add(cboNode);
        cboNode.setBounds(80, 10, 130, 25);

        pnlDetails.add(cboBottleneck);
        cboBottleneck.setBounds(80, 40, 130, 25);

        lblBottleneck.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblBottleneck.setText("Bottleneck:");
        pnlDetails.add(lblBottleneck);
        lblBottleneck.setBounds(5, 40, 70, 25);

        lblNode.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblNode.setText("Node:");
        pnlDetails.add(lblNode);
        lblNode.setBounds(5, 10, 70, 25);

        pnlChartRight.setBorder(null);
        pnlChartRight.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                pnlChartRightMouseClicked(evt);
            }
        });
        pnlChartRight.setLayout(null);
        pnlDetails.add(pnlChartRight);
        pnlChartRight.setBounds(508, 93, 509, 397);

        btnClearThisData.setText("Clear Data");
        btnClearThisData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearThisDataActionPerformed(evt);
            }
        });
        pnlDetails.add(btnClearThisData);
        btnClearThisData.setBounds(5, 70, 205, 20);

        lblNumFlows.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblNumFlows.setText("Number of Flows = 0");
        lblNumFlows.setOpaque(true);
        pnlDetails.add(lblNumFlows);
        lblNumFlows.setBounds(515, 5, 220, 18);

        pnlTGen.setBorder(javax.swing.BorderFactory.createTitledBorder("Traffic Generator"));
        pnlTGen.setLayout(null);

        optGroupTGen.add(optIperf);
        optIperf.setFont(new java.awt.Font("Arial", 0, 12));
        optIperf.setSelected(true);
        optIperf.setText("Iperf");
        optIperf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optIperfActionPerformed(evt);
            }
        });
        pnlTGen.add(optIperf);
        optIperf.setBounds(10, 15, 100, 15);

        optGroupTGen.add(optHarpoon);
        optHarpoon.setFont(new java.awt.Font("Arial", 0, 12));
        optHarpoon.setText("Harpoon");
        optHarpoon.setEnabled(false);
        optHarpoon.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optHarpoonActionPerformed(evt);
            }
        });
        pnlTGen.add(optHarpoon);
        optHarpoon.setBounds(10, 53, 100, 15);

        optGroupTGen.add(optTomahawk);
        optTomahawk.setFont(new java.awt.Font("Arial", 0, 12));
        optTomahawk.setText("Tomahawk");
        optTomahawk.setEnabled(false);
        optTomahawk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optTomahawkActionPerformed(evt);
            }
        });
        pnlTGen.add(optTomahawk);
        optTomahawk.setBounds(10, 34, 100, 15);

        optGroupTGen.add(optPlanetLab);
        optPlanetLab.setFont(new java.awt.Font("Arial", 0, 12));
        optPlanetLab.setText("Planet Lab");
        optPlanetLab.setEnabled(false);
        optPlanetLab.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optPlanetLabActionPerformed(evt);
            }
        });
        pnlTGen.add(optPlanetLab);
        optPlanetLab.setBounds(10, 72, 100, 15);

        pnlDetails.add(pnlTGen);
        pnlTGen.setBounds(770, 5, 120, 90);

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


private void setBufSizeOption( BufferSizeRule rule ) {
    BottleneckLink b = getSelectedBottleneck();
    if( b != null && b.getBufSizeRule() != rule )
        b.setBufSizeRule( rule );
}

private void setTGenOption( Class cls ) {
    BottleneckLink b = getSelectedBottleneck();
    if( b != null && b.getTGen().getClass() != cls )
        b.setTGen(cls);
}
    
private void optRuleOfThumbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optRuleOfThumbActionPerformed
    setBufSizeOption( BufferSizeRule.RULE_OF_THUMB );
}//GEN-LAST:event_optRuleOfThumbActionPerformed

private void optCustomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optCustomActionPerformed
    setBufSizeOption( BufferSizeRule.CUSTOM );
}//GEN-LAST:event_optCustomActionPerformed

private void btnClearThisDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearThisDataActionPerformed
    BottleneckLink b = getSelectedBottleneck();
    if( b != null )
        b.clearData();
}//GEN-LAST:event_btnClearThisDataActionPerformed

private void pnlChartRightMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pnlChartRightMouseClicked
    freezeCharts = !freezeCharts;
}//GEN-LAST:event_pnlChartRightMouseClicked

private void optGuidoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optGuidoActionPerformed
    setBufSizeOption( BufferSizeRule.FLOW_SENSITIVE );
}//GEN-LAST:event_optGuidoActionPerformed

private void lblCustomMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblCustomMouseClicked
    long numBits;
    do {
        String input = GUIHelper.getInput("What should the custom buffer size be?", 
                                          formatBits(8*slCustomBufferSize.getValue(), true, UnitTime.TIME_NONE).both());
        if( input == null )
            return;
        
        try {
            numBits = StringOps.strToBits(input);
        }
        catch( NumberFormatException e ) {
            numBits = -1;
        }
    }
    while( numBits < 0 );
    slCustomBufferSize.setValue( (int)(numBits / 8) );
}//GEN-LAST:event_lblCustomMouseClicked

private void optIperfActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optIperfActionPerformed
    setTGenOption( Iperf.class );
}//GEN-LAST:event_optIperfActionPerformed

private void optHarpoonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optHarpoonActionPerformed
    setTGenOption( Harpoon.class );
}//GEN-LAST:event_optHarpoonActionPerformed

private void optTomahawkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optTomahawkActionPerformed
    setTGenOption( Tomahawk.class );
}//GEN-LAST:event_optTomahawkActionPerformed

private void optPlanetLabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optPlanetLabActionPerformed
    setTGenOption( PlanetLab.class );
}//GEN-LAST:event_optPlanetLabActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClearThisData;
    private dgu.util.swing.binding.JComboBoxBound cboBottleneck;
    private dgu.util.swing.binding.JComboBoxBound cboNode;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lblBottleneck;
    private javax.swing.JLabel lblCustom;
    private javax.swing.JLabel lblGuido;
    private javax.swing.JLabel lblMap;
    private javax.swing.JLabel lblNode;
    private javax.swing.JLabel lblNumFlows;
    private javax.swing.JLabel lblRuleOfThumb;
    private javax.swing.JRadioButton optCustom;
    private javax.swing.ButtonGroup optGroupRule;
    private javax.swing.ButtonGroup optGroupTGen;
    private javax.swing.JRadioButton optGuido;
    private javax.swing.JRadioButton optHarpoon;
    private javax.swing.JRadioButton optIperf;
    private javax.swing.JRadioButton optPlanetLab;
    private javax.swing.JRadioButton optRuleOfThumb;
    private javax.swing.JRadioButton optTomahawk;
    private org.jfree.chart.ChartPanel pnlChartRight;
    private org.jfree.chart.ChartPanel pnlChartXput;
    private javax.swing.JPanel pnlDetails;
    private javax.swing.JPanel pnlMap;
    private javax.swing.JPanel pnlSizing;
    private javax.swing.JPanel pnlTGen;
    private dgu.util.swing.binding.JSliderBound slCustomBufferSize;
    private dgu.util.swing.binding.JSliderBound slNumFlows;
    // End of variables declaration//GEN-END:variables
    static final JMenuItem mnuSetRTT = new JMenuItem("Set RTT");
    
    private void startDummyStatsThread() {
        // starts a dummy thread to generate bogus measured data for testing
        new Thread() {
            public void run() {
                int bfsz = 100;
                int i = 0;
                
                while( true ) {
                    BottleneckLink b = getSelectedBottleneck();
                    if( b != null ) {
                        double c = Math.random();
                        bfsz = (int)(0.1 * Math.random() * 200 + 0.9 * bfsz);
                        b.noteCurrentMeasuredResult(bfsz, c);
                        if( c >= 0.95 ) {
                            b.addMeasuredResult(bfsz);
                            
                            i = (i + 1) % BottleneckLink.interestingN.length;
                            DemoGUI.me.slNumFlows.setValue( BottleneckLink.interestingN[i] );
                        }
                    }
                    try {
                        Thread.sleep( 100 );
                    } catch( InterruptedException e ) {
                        // no-op
                    }
                }
            }
        }.start();
    }
}
