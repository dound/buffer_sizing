package dgu.bufsizing.demos;

import dgu.bufsizing.*;
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
        Router la  = new Router( "LA", 50, 125, Demo.DEFAULT_ROUTER_CONTROLLER_PORT );
        demo.addRouter( la );
        
        Receiver hou = new Receiver( "Houston", 500, 160 );
        demo.addGenericNode( hou );
        
        // add our traffic generators
        TrafficGenerator su = new Harpoon( "Harpoon",   0,  25 );
        demo.addTrafficGenerator( su );
        
        // add the links between nods
        new Link( su, la, Link.NF2C0  );
        new Link( la, su, Link.NF2C0  );
        new BottleneckLink( la, hou, Link.NF2C1, 100, Demo.DEFAULT_RATE_LIMIT_KBPS, Demo.DEFAULT_DATA_POINTS_TO_KEEP, true  );
        new Link( hou, la, Link.NF2C1  );
        
        return demo;
    }
}
