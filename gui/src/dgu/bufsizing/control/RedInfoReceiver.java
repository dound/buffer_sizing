package dgu.bufsizing.control;

import dgu.bufsizing.DemoGUI;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Interacts with the NetFPGA router client.
 * @author David Underhill
 */
public class RedInfoReceiver extends Controller {
    public static final int PORT = 1509;

    RedInfoProcessor rip = new RedInfoProcessor();

    public RedInfoReceiver( String ip ) {
        super( ip, PORT );
        rip.start();
    }
    
    public String getTypeString() {
        return "Red Info Receiver";
    }

    public double meanAvg = -1;
    public double mean = -1;
    public double sdevAvg = -1;
    public double sdev = -1;
    int sdevCount = 0;

    public synchronized double getSdev() {
        return sdev;
    };

    public synchronized double getSdevAvg() {
        return sdevAvg;
    };

    public synchronized double getMeanAvg() {
        return meanAvg;
    };

    public synchronized void resetSdevAvg() {
        sdevCount = 0;
    }

    public synchronized void updateSdev(double d, double m) {
        double alpha = 0.5;

        sdev = d;
        mean = m;
        sdevCount += 1;
        if(sdevCount == 1) {
            sdevAvg = sdev;
            meanAvg = mean;
        }
        else {
            sdevAvg = (alpha)*sdev + (1-alpha)*sdevAvg;
            meanAvg = (alpha)*mean + (1-alpha)*meanAvg;
        }

        System.err.println("sdev="+sdev + " // sdev_avg="+sdevAvg + " // meanavg=" + meanAvg);
    };

    public class RedInfoProcessor extends Thread {
        /**
         * Listens for new data.
         */
        public void run() {
            /* listen for updates until the end of time */
            while (true) {
                // try to get the next update
                try {
                    if(in != null) {
                        DataInputStream din = new DataInputStream(in);
                        updateSdev(din.readDouble(), din.readDouble());
                    }
                } catch( IOException e ) {
                    System.err.println( "Error: RED stats receive failed: " + e.getMessage() );
                    
                    // wait a little before trying again after a failure
                    DemoGUI.msleep(500);
                }
            }
        }
    }
}
