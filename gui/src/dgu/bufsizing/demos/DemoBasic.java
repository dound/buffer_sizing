package dgu.bufsizing.demos;

import dgu.bufsizing.*;
import dgu.bufsizing.Node.Importance;
import dgu.bufsizing.control.EventProcessor;
import dgu.util.IllegalArgValException;

/**
 * A simple demo with a router in LA with a bottleneck link to Houston.
 * 
 * @author David Underhill
 */
public class DemoBasic {
    public static void main( String args[] ) {
        try {
            createDemo().runDemo();
        }
        catch( IllegalArgValException e ) {
            System.err.println( "Unable to create demo: " + e.getMessage() );
            System.exit( 1 );
        }
    }
    
    public static Demo createDemo() throws IllegalArgValException {
        Demo demo = new Demo();
        
        // add our routers
        Router la  = new Router( "Los Angeles", "LA", Importance.IMPORTANT, 80, 145, Demo.DEFAULT_ROUTER_CONTROLLER_PORT, EventProcessor.DEFAULT_EVCAP_PORT );
        demo.addRouter( la );
        
        Receiver hou = new Receiver( "Houston", "HOU", Importance.IMPORTANT, 515, 190 );
        demo.addGenericNode( hou );
        
        EndHostCluster rice = new EndHostCluster( "Rice", "Rice", Importance.IMPORTANT, 455, 215 );
        demo.addGenericNode( rice );
        
        Receiver ny = new Receiver( "New York", "NY", Importance.NIL, 930, 85 );
        demo.addGenericNode( ny );
        
        Receiver dc = new Receiver( "Washington, D.C.", "DC", Importance.NIL, 895, 115 );
        demo.addGenericNode( dc );
        
        // add our traffic generators
        TrafficGenerator su = new Iperf( Demo.DEFAULT_DST_IP, "Stanford", "SU", Importance.IMPORTANT, 30, 100, true, 100000000 );
        demo.addTrafficGenerator( su );
        
        // add the links between nodes
        new Link( su, la, Link.NF2C0  );
        
        // links from LA
        new Link( la, su, Link.NF2C0  );
        new BottleneckLink( la, hou, Link.NF2C1, 
                Demo.DEFAULT_BUFFER_SIZE_MSEC, Demo.DEFAULT_RATE_LIMIT_KBPS, 
                Demo.DEFAULT_DATA_POINTS_TO_KEEP  );
        new Link( la, ny, Link.NF2C2 );
        new Link( la, dc, Link.NF2C3 );
        
        // links from Houston
        new Link( hou, la, Link.NF2C1  );
        new Link( hou, ny, Link.NF2C2  );
        new Link( hou, dc, Link.NF2C3  );
        new Link( hou, rice, Link.NF2C0 );
        
        // links from N
        new Link( ny, hou, Link.NF2C1 );
        new Link( ny, la,  Link.NF2C2 );
        new Link( ny, dc,  Link.NF2C3 );
        
        // links from DC
        new Link( dc, hou, Link.NF2C1 );
        new Link( dc, ny,  Link.NF2C2 );
        new Link( dc, la,  Link.NF2C3 );
        
        return demo;
    }
}
