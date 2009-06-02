package dgu.bufsizing.control;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Interacts with the RED controller.
 * @author David Underhill
 */
public class RedController extends Controller {
    public static final int PORT = 1508;

    public RedController( String ip ) {
        super( ip, PORT );
    }
    
    public String getTypeString() {
        return "Red Controller";
    }

    /**
     * Executes the specified command.
     */
    public synchronized void updateRedParameters(double k, double alpha, double maxp, double mint) {
        OutputStream myOut = out;
        if(myOut == null) {
            System.err.println("red controller connection dead -- not sending params");
            return;
        }
        DataOutputStream dout = new DataOutputStream(myOut);
        try {
            System.err.println("Sending: ");
            System.out.println("    k="+k);
            System.out.println("    a="+alpha);
            System.out.println("    maxp="+maxp);
            System.out.println("    mint="+mint);

            dout.writeInt((int)(k*10));
            dout.writeInt((int)(alpha*1000000));
            dout.writeInt((int)(maxp*1000000));
            dout.writeInt((int)(mint*1000000));
        }
        catch(IOException e) {
            System.err.println("write to red controller socket failed");
            try {
                dout.close();
            }
            catch(IOException e2) {}
        }
    }
}