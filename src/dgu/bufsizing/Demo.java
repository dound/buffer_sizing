package dgu.bufsizing;

import java.awt.Graphics2D;
import java.util.LinkedList;

/**
 * Information about a demo.
 * @author David Underhill
 */
public class Demo {
    public static final int DEFAULT_ROUTER_CONTROLLER_PORT  = 10272;
    public static final int DEFAULT_TRAFFIC_CONTROLLER_PORT = 10752;
    public static final int DEFAULT_BUFFER_SIZE_MSEC        = 250;
    public static final int DEFAULT_RATE_LIMIT_KBPS         = 16000;
    public static final int DEFAULT_DATA_POINTS_TO_KEEP     = 10000;
    
    public LinkedList<Router> routers = new LinkedList<Router>();
    public LinkedList<TrafficGenerator> trafficGenerators = new LinkedList<TrafficGenerator>();
    public LinkedList<Node> genericNodes = new LinkedList<Node>();
    
    public void addRouter( Router r ) {
        routers.add( r );
    }
    
    public void addTrafficGenerator( TrafficGenerator t ) {
        trafficGenerators.add( t );
    }
    
    public void addGenericNode( Node n ) {
        genericNodes.add( n );
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
        gfx.clearRect(0, 0, 1028, 250);
        
        // draw links first
        for( Node n : genericNodes )
            n.drawLinks( gfx );
        
        for( TrafficGenerator t : trafficGenerators )
            t.drawLinks( gfx );
        
        for( Router r : routers )
            r.drawLinks( gfx );
        
        // then draw nodes
        for( Node n : genericNodes )
            n.drawNode( gfx );
        
        for( TrafficGenerator t : trafficGenerators )
            t.drawNode( gfx );
        
        for( Router r : routers )
            r.drawNode( gfx );
    }
}
