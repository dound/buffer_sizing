package dgu.bufsizing;

import dgu.util.swing.GUIHelper;
import dgu.util.swing.binding.BindingAdapter;
import dgu.util.swing.binding.BindingEvent;
import dgu.util.swing.binding.JComboBoxBound;
import dgu.util.swing.binding.JSliderBound;
import dgu.util.swing.binding.delegate.ListBasedComponentDelegate;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

/**
 * The GUI for our SIGCOMM demo.  Displays the topology and current link 
 * utilizatoins.  It also enables the user to modify buffer queues and 
 * bottlenecks in real-time.  In the future, we hope to support the modification 
 * of traffic in real-time too.  It can also displays detailed link information.
 * 
 * @author  David Underhill
 */
public class DemoGUI extends javax.swing.JFrame {
    public static final java.awt.Image icon = java.awt.Toolkit.getDefaultToolkit().getImage("dgu.gif");
    private static JFreeChart chart;
    public static DemoGUI me;
    private final Graphics2D gfx;
    private final Demo demo;
    
    /** Creates new form DemoGUI */
    public DemoGUI( Demo d ) {
        me = this;
        demo = d;
        
        GUIHelper.setGUIDefaults();
        initComponents();
        gfx = (Graphics2D)pnlMap.getGraphics();
        prepareBindings();
        setIconImage( icon );
       
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width - 1024) / 2, (screenSize.height - 768) / 2, 1024, 768);
    }
    
    void prepareBindings() {
        {
            ListBasedComponentDelegate d = cboBottleneck.getBindingDelegate();
            d.addBoundComponent( slBufferSize );
            d.addBoundComponent( slRateLimit );
            d.setPrimaryComponent( -2 );
            
            /* manually populate the bottleneck */
            d.addBindingListener( new BindingAdapter() {
                public void bindingLoaded( BindingEvent e ) {
                    try {
                        LinkedList<BottleneckLink> links = (LinkedList<BottleneckLink>)e.getBinding().getBoundItem();
                        BottleneckLink b = links.get( e.getBinding().getIndexAt() );
                        if( b.getUseRuleOfThumb() )
                            optRuleOfThumb.setSelected( true );
                        else
                            optGuido.setSelected( true );
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
            
            d.changeBinding( demo );
            d.load();
            d.setSelectedIndex( 0 );
        }
    }
    
    public static class StringPair {
        public String a, b;
        String both() { return a + b; }
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
            
            this.lblBufferSize.setText( "Buffer = " + size_msec + "ms" );
            this.optRuleOfThumb.setText( "Rule of Thumb = " 
                                         + formatBits(size_old_bytes*8,true,UnitTime.TIME_NONE) 
                                         + " / " + size_old_packets + "pkt" );
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
        pnlChart = new ChartPanel(chart);
        pnlSizing = new javax.swing.JPanel();
        optRuleOfThumb = new javax.swing.JRadioButton();
        optGuido = new javax.swing.JRadioButton();
        jSeparator1 = new javax.swing.JSeparator();
        cboNode = new JComboBoxBound( "getRouters", "" );
        cboBottleneck = new JComboBoxBound( "getBottlenecks", "" );
        lblBottleneck = new javax.swing.JLabel();
        lblNode = new javax.swing.JLabel();
        pnlMap = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        pnlDetails.setBorder(null);
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
        slBufferSize.setValue(100);
        pnlDetails.add(slBufferSize);
        slBufferSize.setBounds(510, 15, 250, 45);

        lblRateLimit.setAlignment(java.awt.Label.CENTER);
        lblRateLimit.setText("Rate Limit = 100Mb/s");
        pnlDetails.add(lblRateLimit);
        lblRateLimit.setBounds(770, 10, 250, 18);

        slRateLimit.setMajorTickSpacing(100);
        slRateLimit.setMaximum(1000);
        slRateLimit.setMinorTickSpacing(50);
        slRateLimit.setPaintTicks(true);
        slRateLimit.setValue(100);
        pnlDetails.add(slRateLimit);
        slRateLimit.setBounds(770, 15, 250, 45);

        pnlChart.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pnlChart.setLayout(null);
        pnlDetails.add(pnlChart);
        pnlChart.setBounds(5, 75, 1015, 440);

        pnlSizing.setBorder(javax.swing.BorderFactory.createTitledBorder("Buffer Sizing Formula"));
        pnlSizing.setLayout(null);

        optGroupRule.add(optRuleOfThumb);
        optRuleOfThumb.setText("Rule of Thumb = 1000kB / 512pkt");
        pnlSizing.add(optRuleOfThumb);
        optRuleOfThumb.setBounds(10, 15, 250, 22);

        optGroupRule.add(optGuido);
        optGuido.setText("Flow-Sensitive = 1000kB / 512pkt");
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

        getContentPane().add(pnlDetails);
        pnlDetails.setBounds(0, 249, 1028, 519);

        pnlMap.setBorder(null);
        pnlMap.setLayout(null);
        getContentPane().add(pnlMap);
        pnlMap.setBounds(0, 0, 1028, 250);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private dgu.util.swing.binding.JComboBoxBound cboBottleneck;
    private dgu.util.swing.binding.JComboBoxBound cboNode;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lblBottleneck;
    private java.awt.Label lblBufferSize;
    private javax.swing.JLabel lblNode;
    private java.awt.Label lblRateLimit;
    private javax.swing.ButtonGroup optGroupRule;
    private javax.swing.JRadioButton optGuido;
    private javax.swing.JRadioButton optRuleOfThumb;
    private org.jfree.chart.ChartPanel pnlChart;
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
