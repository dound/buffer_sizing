package dgu.bufsizing;

import dgu.util.swing.GUIHelper;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.LinkedList;

/**
 * Information about a demo.
 * @author David Underhill
 */
public class Demo {
    public static final String DEFAULT_DST_IP = "64.57.23.37";
    public static final int DEFAULT_ROUTER_CONTROLLER_PORT  = 10272;
    public static final int DEFAULT_TRAFFIC_CONTROLLER_PORT = 10752;
    public static final int DEFAULT_RTT                     = 100;
    public static final int DEFAULT_RATE_LIMIT_KBPS         = 62500;
    public static final int DEFAULT_DATA_POINTS_TO_KEEP     = 2000;
    
    public static final java.awt.Image RU_ICON = DemoGUI.chooseImage("images/logo-rice-lo.png", "images/logo-rice.png");
    public static final java.awt.Dimension RU_SIZE = DemoGUI.ratio1080(41, 50);
    
    public static final java.awt.Image SU_ICON = DemoGUI.chooseImage("images/logo-stanford-lo.png", "images/logo-stanford.png");
    public static final java.awt.Dimension SU_SIZE = DemoGUI.ratio1080(33, 50);
    
    public static final java.awt.Image NETFPGA_ICON = DemoGUI.chooseImage("images/logo-netfpga-lo.png", "images/logo-netfpga.png");
    public static final java.awt.Dimension NETFPGA_SIZE = DemoGUI.ratio1920(473, 125);
    
    public LinkedList<Router> routers = new LinkedList<Router>();
    public LinkedList<TrafficGenerator> trafficGenerators = new LinkedList<TrafficGenerator>();
    public LinkedList<Node> genericNodes = new LinkedList<Node>();
    
    public Node lastSelectedNode = null;
    public BottleneckLink lastSelectedBottleneckLink = null;
    
    public void addRouter( Router r ) {
        routers.add( r );
    }
    
    public void addTrafficGenerator( TrafficGenerator t ) {
        trafficGenerators.add( t );
    }
    
    public void addGenericNode( Node n ) {
        genericNodes.add( n );
    }
    
    public void clearData() {
        for( Router r : routers )
            for( BottleneckLink b : r.getBottlenecks() )
                b.clearData();
    }
    
    public void runDemo() {
        final Demo demo = this;
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new DemoGUI(demo).setVisible(true);
            }
        });
    }
    
    public LinkedList<Router> getRouters() {
        return routers;
    }
    
    public LinkedList<TrafficGenerator> getTrafficGenerators() {
        return trafficGenerators;
    }

    void redraw(Graphics2D gfx) {
        // clear the drawing space
        gfx.clearRect( 0, 0, DemoGUI.CANVAS_WIDTH, DemoGUI.CANVAS_HEIGHT );
        
        // draw the background image
        Composite compositeOriginal = gfx.getComposite();
        gfx.setComposite( Drawable.COMPOSITE_HALF );
        gfx.drawImage( Drawable.BACKGROUND_IMG, 0, 0, DemoGUI.CANVAS_WIDTH, DemoGUI.CANVAS_HEIGHT, null );
        gfx.setComposite( compositeOriginal );
        
        // draw the netfpga logo
        gfx.drawImage(NETFPGA_ICON, DemoGUI.ratioW1920(50), DemoGUI.ratioH1080(8), NETFPGA_SIZE.width, NETFPGA_SIZE.height, null);
        
        // draw the legend
        String legendLbl = "Increasing Utilization -->";
        int paddingX = 5;
        int paddingY = 2;
        int legendWidth = gfx.getFontMetrics().stringWidth( legendLbl ) + paddingX * 2;
        int legendHeight = gfx.getFontMetrics().getHeight() + 2 + paddingY;
        int legendPosX = 0;
        int legendPosY = DemoGUI.CANVAS_HEIGHT - legendHeight - 2;
        float cur = 0.0f;
        float step = 1.0f / legendWidth;
        for( int i=0; i<legendWidth; i++ ) {
            gfx.setColor( BottleneckLink.getCurrentGradientColor(cur).brighter() );
            cur += step;
            gfx.drawLine( i + legendPosX, legendPosY, i + legendPosX, legendPosY + legendHeight );
        }
        gfx.setPaint( Drawable.PAINT_DEFAULT );
        gfx.drawString( legendLbl, legendPosX + paddingX, legendPosY + legendHeight - paddingY - 2 );
        gfx.setStroke( Drawable.STROKE_THICK );
        gfx.drawRect( legendPosX, legendPosY, legendWidth, legendHeight);
        gfx.setStroke( Drawable.STROKE_DEFAULT );
        
        // draw links first
        for( Node n : genericNodes )
            n.drawLinks( gfx );
        
        for( TrafficGenerator t : trafficGenerators )
            t.drawLinks( gfx );
        
        for( Router r : routers )
            r.drawLinks( gfx );
        
        // then draw nodes
        for( Node n : genericNodes ) {
            n.draw( gfx );
         
            if( n.getName().equals("Rice") )
                gfx.drawImage(RU_ICON, n.getX() + TrafficGenerator.ICON_WIDTH / 2 + 3, n.getY() - RU_SIZE.height / 2, RU_SIZE.width, RU_SIZE.height, null);
        }
        
        for( TrafficGenerator t : trafficGenerators ) {
            t.draw( gfx );
            
            if( t.getName().equals("Stanford") )
                gfx.drawImage(SU_ICON, t.getX() - SU_SIZE.width / 2, t.getY() + TrafficGenerator.ICON_HEIGHT / 2 + 13, SU_SIZE.width, SU_SIZE.height, null);
        }
        
        for( Router r : routers )
            r.draw( gfx );
        
        BottleneckLink b = DemoGUI.me.getSelectedBottleneck();
        if( b != null ) {
            Font origFont = gfx.getFont();
            gfx.setFont(FONT_CBS);
            
            String s1 = "Buffer Size";
            String s2 = DemoGUI.me.getCurBufferSizeText();
            
            int x = DemoGUI.ratioW1920(775), y = DemoGUI.ratioH1080(85);
            int y2 = y + gfx.getFontMetrics().getHeight() - DemoGUI.ratioH1080(15);
            
            gfx.setComposite( Drawable.COMPOSITE_HALF );
            gfx.setPaint(Color.ORANGE);
            GUIHelper.drawCenteredString(s1, gfx, x, y );
            gfx.setPaint(Color.BLUE);
            GUIHelper.drawCenteredString(s2, gfx, x, y2 );
            gfx.setComposite( Drawable.COMPOSITE_OPAQUE );
            
            gfx.setStroke(Drawable.STROKE_THICK3);
            GUIHelper.drawCenteredStringOutline(s1, gfx, x, y, Color.BLACK );
            GUIHelper.drawCenteredStringOutline(s2, gfx, x, y2, Color.BLACK );
            
            gfx.setStroke(Drawable.STROKE_DEFAULT);
            gfx.setFont(origFont);
            gfx.setPaint(Drawable.PAINT_DEFAULT);
        }
    }
    
    private static final Font FONT_CBS = new Font("Tahoma", Font.BOLD, DemoGUI.ratioH1080(60));
}
