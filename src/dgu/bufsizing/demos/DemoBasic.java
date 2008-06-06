package dgu.bufsizing.demos;

import dgu.bufsizing.*;
import dgu.util.IllegalArgValException;

/**
 * A simple demo with a router in LA with a bottleneck link to Houston.
 * 
 * @author David Underhill
 */
public class DemoBasic {
    public void main( String args[] ) {
        try {
            createDemo().runDemo();
        }
        catch( IllegalArgValException e ) {
            System.err.println( "Unable to create demo: " + e.getMessage() );
            System.exit( 1 );
        }
    }
    
    public Demo createDemo() throws IllegalArgValException {
        Demo demo = new Demo();
        
        // add our routers
        Router la  = new Router( "LA1",  50, 125, Demo.DEFAULT_ROUTER_CONTROLLER_PORT );
        demo.addRouter( la );
        
        Router hou = new Router( "LA1", 500, 160, Demo.DEFAULT_ROUTER_CONTROLLER_PORT );
        demo.addRouter( hou );
        
        
        // add our traffic generators
        TrafficGenerator su = new Harpoon( "Harpoon",   0,  25 );
        demo.addTrafficGenerator( su );
        
        // add the links between nods
        new Link( su, la );
        new BottleneckLink( la, hou, 100, Demo.DEFAULT_RATE_LIMIT_KBPS, Demo.DEFAULT_DATA_POINTS_TO_KEEP, true );
        new Link( hou, la );
        
        return demo;
    }
}
