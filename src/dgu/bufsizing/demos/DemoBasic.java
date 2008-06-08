package dgu.bufsizing.demos;

import dgu.bufsizing.*;
import dgu.bufsizing.Node.Importance;
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
        Router la  = new Router( "Los Angeles", "LA", Importance.IMPORTANT, 100, 125, Demo.DEFAULT_ROUTER_CONTROLLER_PORT );
        demo.addRouter( la );
        
        Receiver hou = new Receiver( "Houston", "HOU", Importance.IMPORTANT, 500, 200 );
        demo.addGenericNode( hou );
        
        Receiver ny = new Receiver( "New York", "NY", Importance.NIL, 975, 35 );
        demo.addGenericNode( ny );
        
        Receiver dc = new Receiver( "Washington, D.C.", "DC", Importance.NIL, 925, 75 );
        demo.addGenericNode( dc );
        
        // add our traffic generators
        TrafficGenerator su = new Harpoon( "Stanford", "SU", Importance.NIL, 30, 25 );
        demo.addTrafficGenerator( su );
        
        // add the links between nods
        new Link( su, la, Link.NF2C0  );
        new Link( la, su, Link.NF2C0  );
        new BottleneckLink( la, hou, Link.NF2C1, 
                Demo.DEFAULT_BUFFER_SIZE_MSEC, Demo.DEFAULT_RATE_LIMIT_KBPS, 
                Demo.DEFAULT_DATA_POINTS_TO_KEEP  );
        new Link( hou, la, Link.NF2C1  );
        
        new BottleneckLink( la, ny, Link.NF2C2, 
                Demo.DEFAULT_BUFFER_SIZE_MSEC, Demo.DEFAULT_RATE_LIMIT_KBPS, 
                Demo.DEFAULT_DATA_POINTS_TO_KEEP  );
        
        new Link( hou, dc, Link.NF2C1 );
        new Link( dc, ny, Link.NF2C1 );
        
        return demo;
    }
}
