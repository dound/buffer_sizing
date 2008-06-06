package dgu.bufsizing.control;

import java.io.IOException;

/**
 * Interacts with the NetFPGA router client.
 * @author David Underhi;;
 */
public class RouterController extends Controller {
    public enum RouterCmd {
        CMD_GET_RATE((byte)0),    /* kbps */
        CMD_SET_RATE((byte)1),    /* kbps */
        CMD_GET_BUF_SZ((byte)2),  /* packets */
        CMD_SET_BUF_SZ((byte)3);  /* packets */
        
        public final byte code;
        RouterCmd( byte code ) { this.code = code; }
    }
    
    public RouterController( int port ) {
        super( port );
    }

    public String getTypeString() {
        return "Router";
    }
    
    /**
     * Executes the specified command.
     * 
     * @param cmd        the command to execute
     * @param queueNum   the queue this command affects (must be 0, 1, 2, or 3)
     * @param value      the value to send with this command
     * 
     * @return the value returned by the command (0 if the command is a SET command)
     */
    public synchronized int command( RouterCmd cmd, byte queueNum, int value ) {
        // use last two bits to specifiy which queue this pertains to
        byte code_plus_queue = (byte)(cmd.code | (queueNum << 6));
        sendCommand( code_plus_queue, value );

        int ret = 0;
        if( cmd.code == RouterCmd.CMD_GET_BUF_SZ.code || cmd.code == RouterCmd.CMD_GET_RATE.code ) {
            try {
                //read each byte in the return value
                ret =   in.read() << 24;
                ret += (in.read() << 16);
                ret += (in.read() << 8);
                ret +=  in.read();
            } catch( IOException e ) {
                System.err.println( "command " + cmd.code + " / " + value + " => failed: " + e.getMessage() );
                System.exit( 1 );
            }
        }
        
        return ret;
    }
}
