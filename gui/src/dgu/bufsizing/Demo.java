package dgu.bufsizing;

import java.awt.Composite;
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
    public static final int DEFAULT_RTT                     = 50;
    public static final int DEFAULT_RATE_LIMIT_KBPS         = 62500;
    public static final int DEFAULT_DATA_POINTS_TO_KEEP     = 10000;
    
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
        for( Node n : genericNodes )
            n.draw( gfx );
        
        for( TrafficGenerator t : trafficGenerators )
            t.draw( gfx );
        
        for( Router r : routers )
            r.draw( gfx );
    }
}
