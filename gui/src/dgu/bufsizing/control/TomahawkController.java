package dgu.bufsizing.control;

/**
 * Interacts with the Tomahawk client.
 * @author David Underhill
 */
public class TomahawkController extends Controller {
    public enum TomahawkCmd {
        CMD_FLOWS((byte)0),
        CMD_BPS((byte)1),
        CMD_INTERVAL((byte)2),
        CMD_EXIT((byte)3);
        
        public final byte code;
        TomahawkCmd( byte code ) { this.code = code; }
    }
    
    public TomahawkController( String ip, int port ) {
        super( ip, port );
    }

    public String getTypeString() {
        return "Tomahawk";
    }
    
    /**
     * Executes the specified command.
     * 
     * @param cmd        the command to execute
     * @param value      the value to send with this command
     */
    public synchronized void command( TomahawkCmd cmd, int value ) {
        sendCommand( cmd.code, value );
    }
}
