/*
 * Filename: MasterGUI.java
 * Purpose:  GUI for the demo
 */

package dgu.bufsizing;

import dgu.util.swing.GUIHelper;
import dgu.util.swing.binding.*;
import dgu.util.translator.*;
import java.awt.BasicStroke;
import java.awt.Color;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.*;

/**
 * Simple GUI for controlling and reporting the status of the demo.
 * @author  David Underhill
 */
public class MasterGUI extends javax.swing.JFrame {
    /** the icon for the project */
    public static final java.awt.Image icon = java.awt.Toolkit.getDefaultToolkit().getImage("dgu.gif");
  
    public static final ControlParams ctl = new ControlParams();
    
    public static MasterGUI me;
    
    private static JFreeChart chart;
    public static final XYSeries dataXput = new XYSeries("Goodput");
    public static final XYSeries dataOcc  = new XYSeries("Queue Occupancy");
    public static final XYSeries dataQS   = new XYSeries("Queue Size");
    private static final XYSeriesCollection collXput = new XYSeriesCollection();
    private static final XYSeriesCollection collOcc  = new XYSeriesCollection();
    private static int tic = 0;
    
    private int getIntFromUser( String msg, int min, int def, int max ) {
        int v;
        while( true ) {
            try {
                v = Integer.valueOf( GUIHelper.getInput(msg, String.valueOf(def)) );
                if( v < min || v > max )
                    GUIHelper.displayError( "Error: must be between " + min + " and " + max + "." );
                else
                    return v;
            }
            catch( NumberFormatException e ) {
                GUIHelper.displayError(e);
            }
        }
    }
    
    /** Creates new form MasterGUI */
    public MasterGUI() {
        me = this;
        
        GUIHelper.setGUIDefaults();
        initComponents();
        setIconImage( icon );
        
        initializeData();
        createChart();
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBounds(10, 180, 1005, 545);
        getContentPane().add(chartPanel);
       
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width - 1024) / 2, (screenSize.height - 768) / 2, 1024, 768);
        
        // start the server sockets
        ctl.waitForClients( getIntFromUser("How many traffic generators will there be?",0,0,100),
                            getIntFromUser("On what port do you want to run on for your traffic generator commander(s)?",1,10000,65535) );
        ctl.waitForRouter( getIntFromUser("On what port do you want to run your router commander?",1,10001,65535) );

        // setup the buffer size
        ctl.recomputeBufferSize();
        
        // ask for the current rate limiter value
        ctl.refreshRateLim();
    }
    
    static private void initializeData() {
        collXput.addSeries(dataXput);
        collOcc.addSeries(dataOcc);
        collOcc.addSeries(dataQS);
        
        for( ; tic<10; tic++ ) {
            dataXput.add( tic, tic );
            dataOcc.add( tic, tic % 6 );
            dataQS.add( tic, 5 );
        }
    }
    
    static private void createChart() {
        chart = ChartFactory.createXYLineChart(
            "Throughput and Queue Occupancy vs. Time",
            "Time",
            "Throughput (Goodput) (bps)",
            collXput,
            PlotOrientation.VERTICAL,
            true, //legend
            false, //tooltips
            false //URLs
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
        domain.setStandardTickUnits( NumberAxis.createIntegerTickUnits() );
        domain.setAutoRange(true);
        
        ValueAxis range = plot.getRangeAxis();
        range.setLabelFont( GUIHelper.DEFAULT_FONT_BOLD_BIG );
        range.setStandardTickUnits( NumberAxis.createIntegerTickUnits() );
        range.setAutoRange(true);
       
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, new Color(0,196,0));
        renderer.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL));
        plot.setRenderer(0, renderer);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        
        NumberAxis range2 = new NumberAxis("Queue Occupancy / Size");
        plot.setRangeAxis(1, range2);
        plot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);
        range2.setLabelFont( GUIHelper.DEFAULT_FONT_BOLD_BIG );
        range2.setStandardTickUnits( NumberAxis.createIntegerTickUnits() );
        range2.setAutoRange(true);
        
        XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer(true, false);
        renderer2.setPaint(new Color(196,0,0));
        renderer2.setSeriesStroke(0, new BasicStroke(3f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL));
        renderer2.setSeriesStroke(1, new BasicStroke(3f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,10.0f,new float[]{10.0f,5.0f},0.0f));
        plot.setRenderer(1, renderer2);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        
        plot.setDataset(1, collOcc);
        plot.mapDatasetToRangeAxis(1, 1);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlControl = new javax.swing.JPanel();
        pnlBufControl = new javax.swing.JPanel();
        lblCurBufSizeVal = new javax.swing.JLabel();
        lblCurBufSize = new javax.swing.JLabel();
        lblCurBufSizeUnits = new javax.swing.JLabel();
        txtLinkBW = new JTextFieldBound<Integer>(new TranslatorIntegerString(), this.ctl, "linkBW");
        lblLinkBW = new javax.swing.JLabel();
        lblLinkBWUnits = new javax.swing.JLabel();
        lblDelay = new javax.swing.JLabel();
        txtDelay = new JTextFieldBound<Integer>(new TranslatorIntegerString(), this.ctl, "delay");
        lblNotCurBufSizeVal = new javax.swing.JLabel();
        lblUseNumFlows = new javax.swing.JLabel();
        chkUseNumFlows = new JCheckBoxBound(new SelfTranslator<Boolean>(), this.ctl, "isUseNumFlows", "setUseNumFlows");
        lblDelayUnits = new javax.swing.JLabel();
        pnlNetControl = new javax.swing.JPanel();
        txtNumFlows = new JTextFieldBound<Integer>(new TranslatorIntegerString(), this.ctl, "numFlows");
        lblNumGen = new javax.swing.JLabel();
        lblNumGenVal = new javax.swing.JLabel();
        lblPayloadBW = new javax.swing.JLabel();
        txtPayloadBW = new JTextFieldBound<Integer>(new TranslatorIntegerString(), this.ctl, "payloadBW");
        lblNumFlows = new javax.swing.JLabel();
        lblPayloadBWUnits2 = new javax.swing.JLabel();
        pnlRouterState = new javax.swing.JPanel();
        lblBufSize = new javax.swing.JLabel();
        lblRateLimVal = new javax.swing.JLabel();
        lblRateLim = new javax.swing.JLabel();
        lblRateLimUnits = new javax.swing.JLabel();
        lblQOcc = new javax.swing.JLabel();
        lblBufSizeUnits = new javax.swing.JLabel();
        lblBufSizeVal = new javax.swing.JLabel();
        lblXputVal = new javax.swing.JLabel();
        lblXputUnits = new javax.swing.JLabel();
        lblQOccVal = new javax.swing.JLabel();
        lblQOccUnits = new javax.swing.JLabel();
        lblXput = new javax.swing.JLabel();
        btnRateLimDown = new javax.swing.JButton();
        btnRateLimUp = new javax.swing.JButton();
        btnClearData = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Buffer Sizing Demo GUI");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(null);

        pnlControl.setBorder(javax.swing.BorderFactory.createTitledBorder("Demo Control"));
        pnlControl.setLayout(null);

        pnlBufControl.setBorder(javax.swing.BorderFactory.createTitledBorder("Router Buffer Size"));
        pnlBufControl.setLayout(null);

        lblCurBufSizeVal.setFont(new java.awt.Font("Tahoma", 1, 16));
        lblCurBufSizeVal.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblCurBufSizeVal.setText("0");
        pnlBufControl.add(lblCurBufSizeVal);
        lblCurBufSizeVal.setBounds(190, 20, 110, 20);

        lblCurBufSize.setFont(new java.awt.Font("Tahoma", 1, 16));
        lblCurBufSize.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblCurBufSize.setText("Current Buffer Size: ");
        pnlBufControl.add(lblCurBufSize);
        lblCurBufSize.setBounds(10, 20, 180, 20);

        lblCurBufSizeUnits.setFont(new java.awt.Font("Tahoma", 1, 16));
        lblCurBufSizeUnits.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblCurBufSizeUnits.setText("B");
        pnlBufControl.add(lblCurBufSizeUnits);
        lblCurBufSizeUnits.setBounds(305, 20, 50, 20);

        txtLinkBW.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        txtLinkBW.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtLinkBWActionPerformed(evt);
            }
        });
        txtLinkBW.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtLinkBWKeyPressed(evt);
            }
        });
        pnlBufControl.add(txtLinkBW);
        txtLinkBW.setBounds(190, 50, 110, 20);

        lblLinkBW.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblLinkBW.setText("Link Bandwidth:");
        pnlBufControl.add(lblLinkBW);
        lblLinkBW.setBounds(10, 50, 175, 20);

        lblLinkBWUnits.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblLinkBWUnits.setText("bps");
        pnlBufControl.add(lblLinkBWUnits);
        lblLinkBWUnits.setBounds(305, 50, 50, 20);

        lblDelay.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblDelay.setText("Delay Buffering:");
        pnlBufControl.add(lblDelay);
        lblDelay.setBounds(10, 80, 175, 20);

        txtDelay.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        txtDelay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtDelayActionPerformed(evt);
            }
        });
        txtDelay.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtDelayKeyPressed(evt);
            }
        });
        pnlBufControl.add(txtDelay);
        txtDelay.setBounds(190, 80, 110, 20);

        lblNotCurBufSizeVal.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        pnlBufControl.add(lblNotCurBufSizeVal);
        lblNotCurBufSizeVal.setBounds(220, 110, 140, 20);

        lblUseNumFlows.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblUseNumFlows.setText("Use # of Flows:");
        pnlBufControl.add(lblUseNumFlows);
        lblUseNumFlows.setBounds(10, 110, 175, 20);

        chkUseNumFlows.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkUseNumFlowsActionPerformed(evt);
            }
        });
        pnlBufControl.add(chkUseNumFlows);
        chkUseNumFlows.setBounds(190, 110, 21, 21);

        lblDelayUnits.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblDelayUnits.setText("ms");
        pnlBufControl.add(lblDelayUnits);
        lblDelayUnits.setBounds(305, 80, 50, 20);

        pnlControl.add(pnlBufControl);
        pnlBufControl.setBounds(10, 20, 370, 140);

        pnlNetControl.setBorder(javax.swing.BorderFactory.createTitledBorder("Network Traffic"));
        pnlNetControl.setLayout(null);

        txtNumFlows.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        txtNumFlows.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtNumFlowsActionPerformed(evt);
            }
        });
        txtNumFlows.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtNumFlowsKeyPressed(evt);
            }
        });
        pnlNetControl.add(txtNumFlows);
        txtNumFlows.setBounds(170, 80, 110, 20);

        lblNumGen.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblNumGen.setText("# of Traffic Generators:");
        pnlNetControl.add(lblNumGen);
        lblNumGen.setBounds(5, 20, 160, 20);

        lblNumGenVal.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblNumGenVal.setText("0");
        pnlNetControl.add(lblNumGenVal);
        lblNumGenVal.setBounds(170, 20, 110, 20);

        lblPayloadBW.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblPayloadBW.setText("Aggregate Payload Bandwidth:");
        pnlNetControl.add(lblPayloadBW);
        lblPayloadBW.setBounds(5, 50, 160, 20);

        txtPayloadBW.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        txtPayloadBW.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPayloadBWActionPerformed(evt);
            }
        });
        txtPayloadBW.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtPayloadBWKeyPressed(evt);
            }
        });
        pnlNetControl.add(txtPayloadBW);
        txtPayloadBW.setBounds(170, 50, 110, 20);

        lblNumFlows.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblNumFlows.setText("# of Flows:");
        pnlNetControl.add(lblNumFlows);
        lblNumFlows.setBounds(5, 80, 160, 20);

        lblPayloadBWUnits2.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblPayloadBWUnits2.setText("bps");
        pnlNetControl.add(lblPayloadBWUnits2);
        lblPayloadBWUnits2.setBounds(285, 50, 50, 20);

        pnlControl.add(pnlNetControl);
        pnlNetControl.setBounds(385, 20, 310, 140);

        pnlRouterState.setBorder(javax.swing.BorderFactory.createTitledBorder("Router State"));
        pnlRouterState.setLayout(null);

        lblBufSize.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblBufSize.setText("Buffer Size:");
        pnlRouterState.add(lblBufSize);
        lblBufSize.setBounds(10, 20, 140, 20);

        lblRateLimVal.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblRateLimVal.setText("0");
        pnlRouterState.add(lblRateLimVal);
        lblRateLimVal.setBounds(140, 50, 110, 20);

        lblRateLim.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblRateLim.setText("Rate Limiter:");
        pnlRouterState.add(lblRateLim);
        lblRateLim.setBounds(10, 50, 140, 20);

        lblRateLimUnits.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblRateLimUnits.setText("bps");
        pnlRouterState.add(lblRateLimUnits);
        lblRateLimUnits.setBounds(260, 50, 50, 20);

        lblQOcc.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblQOcc.setText("Instant. Queue Occupancy:");
        pnlRouterState.add(lblQOcc);
        lblQOcc.setBounds(10, 110, 140, 20);

        lblBufSizeUnits.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblBufSizeUnits.setText("packets");
        pnlRouterState.add(lblBufSizeUnits);
        lblBufSizeUnits.setBounds(260, 20, 50, 20);

        lblBufSizeVal.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblBufSizeVal.setText("0");
        pnlRouterState.add(lblBufSizeVal);
        lblBufSizeVal.setBounds(140, 20, 110, 20);

        lblXputVal.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblXputVal.setText("0");
        pnlRouterState.add(lblXputVal);
        lblXputVal.setBounds(140, 80, 110, 20);

        lblXputUnits.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblXputUnits.setText("bps");
        pnlRouterState.add(lblXputUnits);
        lblXputUnits.setBounds(260, 80, 50, 20);

        lblQOccVal.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblQOccVal.setText("0");
        pnlRouterState.add(lblQOccVal);
        lblQOccVal.setBounds(140, 110, 110, 20);

        lblQOccUnits.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblQOccUnits.setText("bps");
        pnlRouterState.add(lblQOccUnits);
        lblQOccUnits.setBounds(260, 110, 50, 20);

        lblXput.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblXput.setText("Instantaneous Throughput:");
        pnlRouterState.add(lblXput);
        lblXput.setBounds(10, 80, 140, 20);

        btnRateLimDown.setText("-");
        btnRateLimDown.setMargin(new java.awt.Insets(2, 2, 2, 2));
        btnRateLimDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRateLimDownActionPerformed(evt);
            }
        });
        pnlRouterState.add(btnRateLimDown);
        btnRateLimDown.setBounds(60, 51, 20, 20);

        btnRateLimUp.setText("+");
        btnRateLimUp.setMargin(new java.awt.Insets(2, 2, 2, 2));
        btnRateLimUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRateLimUpActionPerformed(evt);
            }
        });
        pnlRouterState.add(btnRateLimUp);
        btnRateLimUp.setBounds(40, 51, 20, 20);

        pnlControl.add(pnlRouterState);
        pnlRouterState.setBounds(700, 20, 310, 140);

        getContentPane().add(pnlControl);
        pnlControl.setBounds(5, 0, 1019, 170);

        btnClearData.setText("Clear Data");
        btnClearData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearDataActionPerformed(evt);
            }
        });
        getContentPane().add(btnClearData);
        btnClearData.setBounds(910, 170, 110, 30);
    }// </editor-fold>//GEN-END:initComponents

    private void txtNumFlowsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNumFlowsActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_txtNumFlowsActionPerformed

    private void txtPayloadBWActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPayloadBWActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_txtPayloadBWActionPerformed

    private void txtLinkBWActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtLinkBWActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_txtLinkBWActionPerformed

    private void txtDelayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtDelayActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_txtDelayActionPerformed

    private void chkUseNumFlowsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkUseNumFlowsActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_chkUseNumFlowsActionPerformed

    private void btnClearDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearDataActionPerformed
        dataOcc.clear();
        dataQS.clear();
        dataXput.clear();
        tic = 0;
}//GEN-LAST:event_btnClearDataActionPerformed

    private void txtLinkBWKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtLinkBWKeyPressed
        if( evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER )
            txtLinkBW.getBindingDelegate().save();
    }//GEN-LAST:event_txtLinkBWKeyPressed

    private void txtDelayKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtDelayKeyPressed
        if( evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER )
            txtDelay.getBindingDelegate().save();
    }//GEN-LAST:event_txtDelayKeyPressed

    private void txtPayloadBWKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtPayloadBWKeyPressed
        if( evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER )
            txtPayloadBW.getBindingDelegate().save();
    }//GEN-LAST:event_txtPayloadBWKeyPressed

    private void txtNumFlowsKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtNumFlowsKeyPressed
        if( evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER )
            txtNumFlows.getBindingDelegate().save();
    }//GEN-LAST:event_txtNumFlowsKeyPressed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // tell our traffic generators to shutdown
        ctl.shutdown();
    }//GEN-LAST:event_formWindowClosing

    private void btnRateLimUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRateLimUpActionPerformed
        ctl.increaseRateLim();
}//GEN-LAST:event_btnRateLimUpActionPerformed

    private void btnRateLimDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRateLimDownActionPerformed
        ctl.decreaseRateLim();
}//GEN-LAST:event_btnRateLimDownActionPerformed
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MasterGUI().setVisible(true);
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClearData;
    private javax.swing.JButton btnRateLimDown;
    private javax.swing.JButton btnRateLimUp;
    private dgu.util.swing.binding.JCheckBoxBound chkUseNumFlows;
    private javax.swing.JLabel lblBufSize;
    private javax.swing.JLabel lblBufSizeUnits;
    public javax.swing.JLabel lblBufSizeVal;
    private javax.swing.JLabel lblCurBufSize;
    public javax.swing.JLabel lblCurBufSizeUnits;
    public javax.swing.JLabel lblCurBufSizeVal;
    private javax.swing.JLabel lblDelay;
    private javax.swing.JLabel lblDelayUnits;
    private javax.swing.JLabel lblLinkBW;
    private javax.swing.JLabel lblLinkBWUnits;
    public javax.swing.JLabel lblNotCurBufSizeVal;
    private javax.swing.JLabel lblNumFlows;
    private javax.swing.JLabel lblNumGen;
    public javax.swing.JLabel lblNumGenVal;
    private javax.swing.JLabel lblPayloadBW;
    private javax.swing.JLabel lblPayloadBWUnits2;
    private javax.swing.JLabel lblQOcc;
    public javax.swing.JLabel lblQOccUnits;
    public javax.swing.JLabel lblQOccVal;
    private javax.swing.JLabel lblRateLim;
    public javax.swing.JLabel lblRateLimUnits;
    public javax.swing.JLabel lblRateLimVal;
    private javax.swing.JLabel lblUseNumFlows;
    private javax.swing.JLabel lblXput;
    public javax.swing.JLabel lblXputUnits;
    public javax.swing.JLabel lblXputVal;
    private javax.swing.JPanel pnlBufControl;
    private javax.swing.JPanel pnlControl;
    private javax.swing.JPanel pnlNetControl;
    private javax.swing.JPanel pnlRouterState;
    private dgu.util.swing.binding.JTextFieldBound txtDelay;
    private dgu.util.swing.binding.JTextFieldBound txtLinkBW;
    private dgu.util.swing.binding.JTextFieldBound txtNumFlows;
    private dgu.util.swing.binding.JTextFieldBound txtPayloadBW;
    // End of variables declaration//GEN-END:variables
    
}
