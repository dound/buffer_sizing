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
import java.io.*;
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
    public static final int TIME_BETWEEN_REFRESHES = 250;
    private static final int NUM_IPERF_CONTROLLERS = 1;
    
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
        lblNode.setVisible(false);
        cboNode.setVisible(false);
        lblBottleneck.setVisible(false);
        cboBottleneck.setVisible(false);
        initPopup();
        prepareBindings();
        setIconImage( icon );
        setSearchPrecision(1);
        readAutoModeParamsFromFileWrapper();
        
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
        
        // start the stats listener threads (! no longer use event cap directly ...)
        for( Router r : demo.getRouters() )
            r.startStatsListener();
        
                
        // start the iperf controller(s)
        int baseIPOctet = 84;
        int numIperfControllers = GUIHelper.getIntFromUser("How many traffic controllers are running?", 0, NUM_IPERF_CONTROLLERS, 9);
        for( int i=0; i<numIperfControllers; i++ ) {
            String ip = GUIHelper.getInput("What is the IP or hostname of iperf controller server #" + i + "?", "b" + (baseIPOctet + i));
            new IperfController(ip, IperfController.BASE_PORT);
        }
    }
    
    public static final int RATE_LIM_VALUE_COUNT = 17;
    public static final long RATE_LIM_MAX_RATE = 4L * 1000L * 1000L * 1000L;
    private static final long RATE_LIM_MAX_RATE_ALLOWED = 1000L * 1000L * 1000L;
    public static final int RATE_LIM_MIN_REG_VAL = 2;
    JMenu mnuRateLim = new JMenu("Rate Limit");
    javax.swing.JCheckBoxMenuItem[] mnuRateLimVal = new JCheckBoxMenuItem[RATE_LIM_VALUE_COUNT];
    javax.swing.JCheckBoxMenuItem mnuToggleGraph, mnuTogglePerData;
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
                        bl.setRateLimitReg(index);
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
        
        // add a toggle for actual vs percent xput/occ
        mnuTogglePerData = new JCheckBoxMenuItem( "Toggle Y-Axes as Percentage" );
        mnuTogglePerData.setSelected(true);
        mnuTogglePerData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                populateCollXputAndOcc();
            }
        });
        mnuPopup.add(mnuTogglePerData);
        
        // add options to control auto mode parameters
        JMenu mnuAutoModeConfig = new JMenu("Auto Mode Config");
        mnuPopup.add(mnuAutoModeConfig);
        
        JMenuItem mnuAMCFullUtilThresh = new JMenuItem("Full Utilization Threshold");
        mnuAMCFullUtilThresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                double ret = GUIHelper.getDoubleFromUser("What percentage utilization will be considered \"fully\" utilized?",
                                                         0.0, fullUtilThreshold*100.0, 100.0 );
                fullUtilThreshold = ret / 100.0f;
            }
        });
        mnuAutoModeConfig.add(mnuAMCFullUtilThresh);
        
        JMenuItem mnuAMCStabalizeFlowChange = new JMenuItem("Flow Change Stabilization Time");
        mnuAMCStabalizeFlowChange.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int ret = GUIHelper.getIntFromUser("How long (msec) to give the system to stabilize after changing the number of flows?",
                                                   0, flowStabilizeTime_msec, 60000  );
                flowStabilizeTime_msec = ret;
            }
        });
        mnuAutoModeConfig.add(mnuAMCStabalizeFlowChange);
        
        JMenuItem mnuAMCStabalizeBufSzChange = new JMenuItem("Buffer Size Change Stabilization Time");
        mnuAMCStabalizeBufSzChange.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int ret = GUIHelper.getIntFromUser("How long (msec) to give the system to stabilize after changing the buffer size?",
                                                   0, bufszStabilizeTime_msec, 60000  );
                bufszStabilizeTime_msec = ret;
            }
        });
        mnuAutoModeConfig.add(mnuAMCStabalizeBufSzChange);
        
        JMenuItem mnuAMCXputSampleTime = new JMenuItem("Throughput Sample Time");
        mnuAMCXputSampleTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int ret = GUIHelper.getIntFromUser("How long (msec) to sample the throughput to get a measurement for a given N and B?",
                                                   100, xputSampleTime_msec, 60000  );
                xputSampleTime_msec = ret;
            }
        });
        mnuAutoModeConfig.add(mnuAMCXputSampleTime);
        
        JMenuItem mnuAMCSearchPrecision = new JMenuItem("Search Precision");
        mnuAMCSearchPrecision.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int ret = GUIHelper.getIntFromUser("How precisely to determine the minimum buffer size in packets (1500B each)?",
                                                   1, searchPrecision_packets, 100  );
                setSearchPrecision(ret);
            }
        });
        mnuAutoModeConfig.add(mnuAMCSearchPrecision);
        
        // seperate parameters from print/load/save
        mnuAutoModeConfig.addSeparator();
        
        // menus to print, load, and save the parameter values
        JMenuItem mnuAMCPrintParams = new JMenuItem("Print Current Parameter Values To Stderr");
        mnuAMCPrintParams.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                System.err.println(getParamsAsString());
            }
        });
        mnuAutoModeConfig.add(mnuAMCPrintParams);
        
        JMenuItem mnuAMCReadFromFile = new JMenuItem("Read From Config File " + AUTO_MODE_PARAMS_FILE);
        mnuAMCReadFromFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                readAutoModeParamsFromFileWrapper();
            }
        });
        mnuAutoModeConfig.add(mnuAMCReadFromFile);
        
        JMenuItem mnuAMCSaveToFile = new JMenuItem("Save To Config File " + AUTO_MODE_PARAMS_FILE);
        mnuAMCSaveToFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAutoModeParamsToFileWrapper();
            }
        });
        mnuAutoModeConfig.add(mnuAMCSaveToFile);
        
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
    
    private void populateCollXputAndOcc() {
        DemoGUI.collXput.removeAllSeries(false);
        DemoGUI.collOcc.removeAllSeries(false);
        BottleneckLink bl = DemoGUI.me.getSelectedBottleneck();
        if( bl != null ) {
            if( !mnuTogglePerData.isSelected() ) {
                DemoGUI.collXput.addSeries( bl.getDataThroughput(), false );
                DemoGUI.collXput.addSeries( bl.getDataRateLimit(),  false );
                DemoGUI.collOcc.addSeries(  bl.getDataQueueOcc(),   false );
                DemoGUI.collOcc.addSeries(  bl.getDataBufSize(),    false );
            }
            else {
                DemoGUI.collXput.addSeries( bl.getDataThroughputPer(), false );
                DemoGUI.collOcc.addSeries(  bl.getDataQueueOccPer(),   false );
            }
            
            DemoGUI.me.refreshCharts();
        }
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
        renderer.setSeriesStroke(1, new BasicStroke(4f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL));
        plot.setRenderer(0, renderer);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        plot.getDomainAxis().setFixedAutoRange(1.0e9); // keep the domain at a constant range of values
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
        renderer.setSeriesStroke(1, new BasicStroke(4f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL));
        plot.setRenderer(0, renderer);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        plot.getDomainAxis().setFixedAutoRange(1.0e9); // keep the domain at a constant range of values
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
        renderer.setSeriesStroke(0, new BasicStroke(4f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL));
        
        // theoretical guido
        renderer.setSeriesPaint(1, new Color(0,255,0));
        renderer.setSeriesStroke(1, new BasicStroke(4f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL));
        
        // measured
        renderer.setSeriesPaint(2, new Color(0,128,255));
        renderer.setSeriesStroke(2, new BasicStroke(1f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL));
        renderer.setSeriesLinesVisible(2, true);
        renderer.setSeriesShapesVisible(2, true);
        
        // measured (today's results)
        renderer.setSeriesPaint(3, new Color(255,0,255));
        renderer.setSeriesLinesVisible(3, false);
        renderer.setSeriesShapesVisible(3, true);
        int diamondX[] = { 0, 6, 0, -6, 0 };
        int diamondY[] = { -6, 0, 6, 0, -6 };
        DemoGUI.me.resultsRenderer.setSeriesShape(3, new java.awt.Polygon(diamondX, diamondY, diamondX.length), false);
        
        // measured (current test)
        renderer.setSeriesPaint(4, new Color(0,0,0)); // dynamically set based on confidence values from experiment
        renderer.setSeriesLinesVisible(4, false);
        renderer.setSeriesShapesVisible(4, true);
        renderer.setSeriesVisibleInLegend(4, false, false);
        
        // measured range (current test range)
        renderer.setSeriesPaint(5, new Color(255,0,0));
        renderer.setSeriesStroke(5, new BasicStroke(3f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL));
        renderer.setSeriesLinesVisible(5, true);
        renderer.setSeriesShapesVisible(5, false);
        renderer.setSeriesVisibleInLegend(5, false, false);
        
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
                            populateCollXputAndOcc();
                            DemoGUI.collRes.removeAllSeries( false );
                            DemoGUI.collRes.addSeries( b.getDataRTheROT(), false  );
                            DemoGUI.collRes.addSeries( b.getDataRTheGuido(), false  );
                            DemoGUI.collRes.addSeries( b.getDataRMea(), false  );
                            DemoGUI.collRes.addSeries( b.getDataRToday(), false );
                            DemoGUI.collRes.addSeries( b.getDataRCur(), false );
                            DemoGUI.collRes.addSeries( b.getDataRCurRange(), false );
                            
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
        // wrapped in try-catch blocks b/c JFreeChart sometimes throws exceptions
        // due to bugs in it ... save our thread from dying from one of these
        try { DemoGUI.collXput.manuallyNotifyListeners(); } catch(Exception e) {System.err.println("collXput saved: " + e);}
        try { DemoGUI.collOcc.manuallyNotifyListeners(); } catch(Exception e) {System.err.println("collXput saved: " + e);}
        try { DemoGUI.collRes.manuallyNotifyListeners(); } catch(Exception e) {System.err.println("collXput saved: " + e);}
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
        optGroupMode = new javax.swing.ButtonGroup();
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
        btnClearMeasuredPoints = new javax.swing.JButton();
        lblNumFlows = new javax.swing.JLabel();
        pnlTGen = new javax.swing.JPanel();
        optIperf = new javax.swing.JRadioButton();
        optHarpoon = new javax.swing.JRadioButton();
        optTomahawk = new javax.swing.JRadioButton();
        optPlanetLab = new javax.swing.JRadioButton();
        pnlMode = new javax.swing.JPanel();
        optManual = new javax.swing.JRadioButton();
        optAutoForCurrentN = new javax.swing.JRadioButton();
        optAuto = new javax.swing.JRadioButton();
        btnClearRealTimePoints = new javax.swing.JButton();
        pnlMap = new javax.swing.JPanel();
        lblMap = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        pnlDetails.setLayout(null);

        slNumFlows.setMajorTickSpacing(250);
        slNumFlows.setMaximum(900);
        slNumFlows.setMinorTickSpacing(100);
        slNumFlows.setValue(0);
        pnlDetails.add(slNumFlows);
        slNumFlows.setBounds(515, 25, 220, 16);

        pnlChartXput.setLayout(null);
        pnlDetails.add(pnlChartXput);
        pnlChartXput.setBounds(0, 93, 509, 397);

        pnlSizing.setBorder(javax.swing.BorderFactory.createTitledBorder("Buffer Sizing Formula"));
        pnlSizing.setLayout(null);

        optGroupRule.add(optRuleOfThumb);
        optRuleOfThumb.setFont(new java.awt.Font("Arial", 0, 12));
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
        optGuido.setFont(new java.awt.Font("Arial", 0, 12));
        optGuido.setText("<html>RTT &#183; C / &#8730;N</html>");
        optGuido.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optGuidoActionPerformed(evt);
            }
        });
        pnlSizing.add(optGuido);
        optGuido.setBounds(10, 35, 170, 15);

        lblCustom.setFont(new java.awt.Font("Courier", 0, 12));
        lblCustom.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblCustom.setText("1000kB");
        lblCustom.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblCustomMouseClicked(evt);
            }
        });
        pnlSizing.add(lblCustom);
        lblCustom.setBounds(175, 55, 60, 15);

        lblRuleOfThumb.setFont(new java.awt.Font("Courier", 0, 12));
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

        pnlChartRight.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                pnlChartRightMouseClicked(evt);
            }
        });
        pnlChartRight.setLayout(null);
        pnlDetails.add(pnlChartRight);
        pnlChartRight.setBounds(508, 93, 509, 397);

        btnClearMeasuredPoints.setText("Clear Util");
        btnClearMeasuredPoints.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearMeasuredPointsActionPerformed(evt);
            }
        });
        pnlDetails.add(btnClearMeasuredPoints);
        btnClearMeasuredPoints.setBounds(125, 70, 115, 20);

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
        optIperf.setBounds(10, 15, 95, 15);

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
        optHarpoon.setBounds(10, 53, 95, 15);

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
        optTomahawk.setBounds(10, 34, 95, 15);

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
        optPlanetLab.setBounds(10, 72, 95, 15);

        pnlDetails.add(pnlTGen);
        pnlTGen.setBounds(750, 5, 115, 90);

        pnlMode.setBorder(javax.swing.BorderFactory.createTitledBorder("Mode"));
        pnlMode.setLayout(null);

        optGroupMode.add(optManual);
        optManual.setFont(new java.awt.Font("Arial", 0, 12));
        optManual.setSelected(true);
        optManual.setText("Manual");
        optManual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optManualActionPerformed(evt);
            }
        });
        pnlMode.add(optManual);
        optManual.setBounds(10, 15, 100, 15);

        optGroupMode.add(optAutoForCurrentN);
        optAutoForCurrentN.setFont(new java.awt.Font("Arial", 0, 12));
        optAutoForCurrentN.setText("<html>Auto for<br/>current N</html>");
        optAutoForCurrentN.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optAutoForCurrentNActionPerformed(evt);
            }
        });
        pnlMode.add(optAutoForCurrentN);
        optAutoForCurrentN.setBounds(10, 55, 100, 30);

        optGroupMode.add(optAuto);
        optAuto.setFont(new java.awt.Font("Arial", 0, 12));
        optAuto.setText("Automatic");
        optAuto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optAutoActionPerformed(evt);
            }
        });
        pnlMode.add(optAuto);
        optAuto.setBounds(10, 35, 100, 15);

        pnlDetails.add(pnlMode);
        pnlMode.setBounds(890, 5, 125, 90);

        btnClearRealTimePoints.setText("Clear Xput");
        btnClearRealTimePoints.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearRealTimePointsActionPerformed(evt);
            }
        });
        pnlDetails.add(btnClearRealTimePoints);
        btnClearRealTimePoints.setBounds(5, 70, 115, 20);

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

private void btnClearMeasuredPointsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearMeasuredPointsActionPerformed
    BottleneckLink b = getSelectedBottleneck();
    if( b != null )
        b.clearMeasuredPoints();
}//GEN-LAST:event_btnClearMeasuredPointsActionPerformed

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

private void enableComponForManual(boolean b) {
    pnlSizing.setEnabled(b);
    optRuleOfThumb.setEnabled(b);
    optGuido.setEnabled(b);
    optCustom.setEnabled(b);
    slCustomBufferSize.setEnabled(b);
    slNumFlows.setEnabled(b);
}

private void optManualActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optManualActionPerformed
    stopAutoStatsThread();
    enableComponForManual(true);
    optGuido.setSelected(true);
    optGuidoActionPerformed(null);
}//GEN-LAST:event_optManualActionPerformed

private void prepForAutoMode() {
    enableComponForManual(false);
    optCustom.setSelected(true);
    optCustomActionPerformed(null);
}

private void optAutoForCurrentNActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optAutoForCurrentNActionPerformed
    prepForAutoMode();
    final BottleneckLink b = getSelectedBottleneck();
    if( b != null ) {
        new Thread() {
            public void run() {
                autoStatsState = ThreadState.ON;
                
                int res = computeBufferSizeForN(b, slNumFlows.getValue());
                if( autoStatsState == ThreadState.ON )
                    b.addMeasuredResult( res );
                
                autoStatsState = ThreadState.OFF;
            }
        }.start();
    }
}//GEN-LAST:event_optAutoForCurrentNActionPerformed

private void optAutoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optAutoActionPerformed
    prepForAutoMode();
    startAutoStatsThread();
}//GEN-LAST:event_optAutoActionPerformed

private void btnClearRealTimePointsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearRealTimePointsActionPerformed
    BottleneckLink b = getSelectedBottleneck();
    if( b != null )
        b.clearData();
}//GEN-LAST:event_btnClearRealTimePointsActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClearMeasuredPoints;
    private javax.swing.JButton btnClearRealTimePoints;
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
    private javax.swing.JRadioButton optAuto;
    private javax.swing.JRadioButton optAutoForCurrentN;
    private javax.swing.JRadioButton optCustom;
    private javax.swing.ButtonGroup optGroupMode;
    private javax.swing.ButtonGroup optGroupRule;
    private javax.swing.ButtonGroup optGroupTGen;
    private javax.swing.JRadioButton optGuido;
    private javax.swing.JRadioButton optHarpoon;
    private javax.swing.JRadioButton optIperf;
    private javax.swing.JRadioButton optManual;
    private javax.swing.JRadioButton optPlanetLab;
    private javax.swing.JRadioButton optRuleOfThumb;
    private javax.swing.JRadioButton optTomahawk;
    private org.jfree.chart.ChartPanel pnlChartRight;
    private org.jfree.chart.ChartPanel pnlChartXput;
    private javax.swing.JPanel pnlDetails;
    private javax.swing.JPanel pnlMap;
    private javax.swing.JPanel pnlMode;
    private javax.swing.JPanel pnlSizing;
    private javax.swing.JPanel pnlTGen;
    private dgu.util.swing.binding.JSliderBound slCustomBufferSize;
    private dgu.util.swing.binding.JSliderBound slNumFlows;
    // End of variables declaration//GEN-END:variables
    static final JMenuItem mnuSetRTT = new JMenuItem("Set RTT");
    
    private enum ThreadState {
        OFF,
        TIME_TO_STOP,
        ON
    }
    
    /** sleep for the specified number of milliseconds */
    public static void msleep(int ms) {
        try {
            Thread.sleep( ms );
        } catch( InterruptedException e ) {
            // no-op
        }
    }
    
    public boolean showBufferSizeMarker() {
        return autoStatsState != ThreadState.OFF;
    }
    
    private static final boolean GEN_DEBUG_FAKE_STATS = false;
    private static ThreadState autoStatsState = ThreadState.OFF;
    private void startAutoStatsThread() {
        autoStatsState = ThreadState.ON;
        
        // starts a dummy thread to generate bogus measured data for testing
        new Thread() {
            public void run() {
                int bfsz_B = 100 * 1024;
                int i = 0;
                
                while( autoStatsState == ThreadState.ON ) {
                    BottleneckLink b = getSelectedBottleneck();
                    if( b != null ) {
                        if( GEN_DEBUG_FAKE_STATS ) {
                            double c = Math.random();
                            bfsz_B = (int)(0.1 * Math.random() * 200 * 1024 + 0.9 * bfsz_B);
                            b.noteCurrentMeasuredResult(bfsz_B, c);
                            if( c >= 0.95 ) {
                                b.addMeasuredResult(bfsz_B);

                                i = (i + 1) % BottleneckLink.interestingN.length;
                                DemoGUI.me.slNumFlows.setValue( BottleneckLink.interestingN[i] );
                            }
                        }
                        else {
                            for( int n : BottleneckLink.interestingN ) {
                                int res = computeBufferSizeForN(b, n);
                                if( autoStatsState != ThreadState.ON )
                                    break;
                                b.addMeasuredResult( res );
                            }
                        }
                    }
                    msleep(100);
                }
                
                autoStatsState = ThreadState.OFF;
            }
        }.start();
    }
    
    /** what percent of maximum throughput will be considered close 
     * enough to be called a link maximally utilized */
    private double fullUtilThreshold = 0.99;
    
    /** how long to wait for a new number of flows to stabalize */
    private int flowStabilizeTime_msec = 5000;
    
    /** how long to wait for a new buffer size to stabalize */
    private int bufszStabilizeTime_msec = 2000;
    
    /** how long to sample throughput */
    private int xputSampleTime_msec = 1000;
    
    /** how precise the search for the ideal buffer size should be */
    private int searchPrecision_packets;
    private int searchPrecision_bytes;
    private void setSearchPrecision(int numPackets) {
        searchPrecision_packets = numPackets;
        searchPrecision_bytes = searchPrecision_packets*BottleneckLink.BYTES_PER_PACKET;
    }
    
    private double getUpperBound_msec() {
        return getBound_msec( slCustomBufferSize.getMaximum() );
    }
    
    private double getExpectedBound_msec(int n) {
        BottleneckLink b = this.getSelectedBottleneck();
        if( b == null ) 
            return getUpperBound_msec();
        
        return getBound_msec( BottleneckLink.computeBufSize(BufferSizeRule.FLOW_SENSITIVE, b.getRTT_ms(), b.getRateLimit_kbps(), n) );
    }
    
    private double getBound_msec(int bfszStart) {
        return flowStabilizeTime_msec + (Math.ceil( Math.log(bfszStart / searchPrecision_bytes) / Math.log(2)) + 2) * (bufszStabilizeTime_msec + xputSampleTime_msec);
    }
    
    private String getParamsAsString() {
        double totalExpected_msec = 0;
        for( int n : BottleneckLink.interestingN )
            totalExpected_msec += getExpectedBound_msec(n);
        
        String strInterestingN = "" + BottleneckLink.interestingN[0];
        for( int i=1; i<BottleneckLink.interestingN.length; i++ )
            strInterestingN += ", " + BottleneckLink.interestingN[i];
        
        return  "Auto Mode Configuration Parameters:\n" +
                "    Full Utilization Threshold     = " + fullUtilThreshold*100 + "%\n" +
                "    Flow Change Stabilization Time = " + flowStabilizeTime_msec + "ms\n" +
                "    Buffer Size Change Stabilization Time = " + bufszStabilizeTime_msec + "ms\n" +
                "    Throughput Sample Time = " + xputSampleTime_msec + "ms\n" + 
                "    Search Precision = " + searchPrecision_packets + " packets (" + searchPrecision_bytes + ")\n" + 
                "\n" + 
                "    Interesting N Values = {" + strInterestingN + " }\n" +
                "    Time Upper Bound Per Flow = " + (int)(getUpperBound_msec() / 1000) + "sec\n" +
                "    Expected Time For All Interesting N = " + (int)(totalExpected_msec / 1000) + "sec (about " + (int)(totalExpected_msec / 60000) + "min)\n";
    }
    
    private static final String AUTO_MODE_PARAMS_FILE = "automode.conf";
    
    private void readAutoModeParamsFromFileWrapper() {
        try {
            readAutoModeParamsFromFile();
        }
        catch( Exception e ) {
            GUIHelper.displayError("Unable to read auto mode params file: " + e.getMessage());
        }
    }
    
    /** read parameters from AUTO_MODE_PARAMS_FILE */
    private void readAutoModeParamsFromFile() throws Exception {
        File file = new File(AUTO_MODE_PARAMS_FILE);
        FileInputStream fis = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);

        String fullUtil = br.readLine();
        String stabilizeFlowChange = br.readLine();
        String stabilizeBfSzChange = br.readLine();
        String sampleXput = br.readLine();
        String searchPrecision = br.readLine();
        if( searchPrecision == null )
            throw new Exception( "Not enough lines in " + AUTO_MODE_PARAMS_FILE + ", or file is missing" );

        this.fullUtilThreshold = Double.valueOf( fullUtil );
        this.flowStabilizeTime_msec = Integer.valueOf( stabilizeFlowChange );
        this.bufszStabilizeTime_msec = Integer.valueOf( stabilizeBfSzChange );
        this.xputSampleTime_msec = Integer.valueOf( sampleXput );
        setSearchPrecision( Integer.valueOf( searchPrecision ) );

        br.close();
        System.err.println("Successfully loaded auto mode paramters from " + AUTO_MODE_PARAMS_FILE);
        System.err.println(getParamsAsString());
    }
    
    private void saveAutoModeParamsToFileWrapper() {
        try {
            saveAutoModeParamsToFile();
        }
        catch( Exception e ) {
            GUIHelper.displayError("Unable to write auto mode params file: " + e.getMessage());
        }
    }
    
    /** save parameters to AUTO_MODE_PARAMS_FILE */
    private void saveAutoModeParamsToFile() throws Exception {
        File file = new File(AUTO_MODE_PARAMS_FILE);
        FileOutputStream fos = new FileOutputStream(file);
        OutputStreamWriter osr = new OutputStreamWriter(fos);
        BufferedWriter bw = new BufferedWriter(osr);

        bw.write( this.fullUtilThreshold + "\n" );
        bw.write( this.flowStabilizeTime_msec + "\n" );
        bw.write( this.bufszStabilizeTime_msec + "\n" );
        bw.write( this.xputSampleTime_msec + "\n" );
        bw.write( this.searchPrecision_packets + "\n" );

        bw.close();
        System.err.println("Successfully saved auto mode paramters from " + AUTO_MODE_PARAMS_FILE);
        System.err.println(getParamsAsString());
    }
    
    /** returns the measured buffer size in B needed to achieve maximum link utilization with n flows */
    private int computeBufferSizeForN(BottleneckLink b, int n) {
        long startTime = System.currentTimeMillis();
        
        // convenience ...
        dgu.util.swing.binding.JSliderBound bfsz = slCustomBufferSize;
        
        // initialize buffer size to its maximum size
        int bfszMax = bfsz.getMaximum();
        bfsz.setValue( bfszMax );
        
        // set the number of flows to the requested value
        slNumFlows.setValue(n);
        System.err.println("Measuring buffer size needed for n = " + n);
        System.err.print(getParamsAsString());
        
        // wait for the new # of flows to stabalize
        System.err.println("  Waiting for flows to stabalize ...");
        msleep(flowStabilizeTime_msec);
        
        // get the throughput for when the buffer size is maximized => maximum throughput
        int maxThroughput_bps = getAvgThroughputReading_bps(b, xputSampleTime_msec);
        int maxThroughputThresh_bps = (int)(fullUtilThreshold * maxThroughput_bps);
        System.err.println("  Max throughput = " + maxThroughput_bps + "bps ... thresh=" + maxThroughputThresh_bps );
        
        // debugging code (temporary!)
        int escapeWarnings = 0;
        double prevSpread = Double.POSITIVE_INFINITY;
        
        // perform a binary search for the minimum buffer size which maximizes throughput
        boolean first = true;
        int currentThroughput_bps = maxThroughput_bps;
        int bfszLo = 1;
        int bfszHi = bfszMax;
        do {
            b.noteCurrentMeasuredResultRange(bfszLo, bfszHi);
            
            if( autoStatsState != ThreadState.ON )
                return -1;
            
            System.err.println("  Measurement: bfsz=" + bfsz.getValue() + "B ... xput=" + currentThroughput_bps + "bps" );
            if( currentThroughput_bps >= maxThroughputThresh_bps ) {
                // link is saturated!
                // save current bf sz if it is the smallest to achieve this value
                bfszHi = bfsz.getValue();
                System.err.println( "  ==> fully utilized ... new min bfsz!" );
            }
            else {
                // link is not saturated!
                // save current bf sz as a new lo (no reason to try any smaller bf sizes)
                bfszLo = bfsz.getValue();
                System.err.println( "  ==> underutilized!" );
            }
            
            // pick a new buffer size to test
            int newBfSz;
            if( first && bfsz.getValue() == bfsz.getMaximum() ) {
                // skip straight to the expected result (can always go back up ...)
                newBfSz = b.getActualBufSize(BufferSizeRule.FLOW_SENSITIVE);
                first = false;
            }
            else {
                // set the buffer size halfway between our current min and max
                // buffer size for this search
                newBfSz = (bfszHi - bfszLo) / 2 + bfszLo;
            }
            
            // if the change is sufficiently small, the search is over
            double spread = Math.abs(bfszHi - bfszLo);
            if( spread <= searchPrecision_bytes )
                break;
            
            // debugging, temp: don't get stuck!
            if( spread > prevSpread ) {
                System.err.println("posible oscillation?  range grew from " + prevSpread + " to " + spread);
                if( escapeWarnings++ >= 3 ) {
                    System.err.println("escaping possible oscillation");
                    break;
                }
            }
            prevSpread = spread;

            // set the new buffer size
            bfsz.setValue( newBfSz );

            // give the new buffer size a chance to stabalize
            msleep(bufszStabilizeTime_msec);
            if( n == 1 ) // extra time for n == 1
                msleep(bufszStabilizeTime_msec);
            
            // get the throughput for this buffer size
            currentThroughput_bps = getAvgThroughputReading_bps(b, xputSampleTime_msec);
        }
        while( true );
        
        // cleanup the old progress point
        b.clearInProgressPoint();
        
        // compare expected and actual runtimes
        long runtime_msec = System.currentTimeMillis() - startTime;
        double expected_msec = getExpectedBound_msec(n);
        double delta_msec = runtime_msec - expected_msec;
        double abs_delta_msec = Math.abs(delta_msec);
        double percentOff = delta_msec / expected_msec * 100;
        String deltaNote = (runtime_msec > expected_msec) ? "Slower" : "Faster";
        System.err.println( "Runtime stats: " +
                            "    Upper bound: " + (int)(getUpperBound_msec() / 1000) + "sec\n" +
                            "    Expected:    " + (int)(expected_msec / 1000) + "sec\n" +
                            "    Actual:      " + (int)(runtime_msec / 1000) + "sec\n" +
                            "      =========> " + deltaNote + " than expected by " + (int)(abs_delta_msec / 1000) + "sec (" + (int)percentOff + "% from expected)\n" );
        
        // return the measured minimum buffer size value
        return bfszHi;
    }
    
    /** gets the average throughput reading over the specified time interval */
    private int getAvgThroughputReading_bps(BottleneckLink b, int time_msec) {
        b.resetXputMovingAverage();
        msleep(time_msec);
        return b.getXputMovingAverage();
    }
    
    /** Block until the automatic stats thread is off. */
    private void stopAutoStatsThread() {
        if( autoStatsState != ThreadState.OFF ) {
            // tell the stats thread to stop
            autoStatsState = ThreadState.TIME_TO_STOP;
            
            // wait for it to stop
            while( autoStatsState != ThreadState.OFF )
                msleep(100);
            
            // remove the unsettled point
            BottleneckLink b = getSelectedBottleneck();
            if( b != null )
                b.clearInProgressPoint();
        }
    }
}
