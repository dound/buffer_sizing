package dgu.bufsizing;

import java.util.LinkedList;

/**
 * Information about a demo.
 * @author David Underhill
 */
public class Demo {
    public static final int DEFAULT_ROUTER_CONTROLLER_PORT  = 10272;
    public static final int DEFAULT_TRAFFIC_CONTROLLER_PORT = 10752;
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
}
