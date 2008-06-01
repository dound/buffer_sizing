package dgu.bufsizing;

import dgu.util.IllegalArgValException;

/**
 * Simple structure to hold our control parameters for the binding to use.
 * @author David Underhill
 */
public class ControlParams {
    private int linkBW = 100 * 1000 * 1000; /* 100 Mbps */
    private int delay = 100; /* ms */
    private int numGen = 0;
    private int payloadBW = 1000; /* bps */
    private int numFlows = 1;
    private boolean useNumFlows = false;

    public static class StringPair {
        public String a, b;
        String both() { return a + b; }
    }
    
    public StringPair formatBits( int b ) {
        int bytes = b / 8;
        int units = 0;
        while( bytes >= 10000 ) {
            bytes /= 1000;
            units += 1;
        }
        String strUnit;
        switch( units ) {
            case  0: strUnit = "B";  break;
            case  1: strUnit = "kB"; break;
            case  2: strUnit = "MB"; break;
            case  3: strUnit = "GB"; break;
            case  4: strUnit = "TB"; break;
            case  5: strUnit = "PB"; break;
            default: strUnit = "?B"; break;
        }
        
        ControlParams.StringPair ret = new ControlParams.StringPair();
        ret.a = Integer.toString( bytes );
        ret.b = strUnit;
        return ret;
    }
    
    public void recomputeBufferSize() {
        int bufSizeOrig = linkBW * delay / 1000;
        int bufSizeNew = (int)(bufSizeOrig / Math.sqrt(numFlows));
        StringPair strOrig = formatBits(bufSizeOrig);
        StringPair strNew = formatBits(bufSizeNew);
        
        if( useNumFlows ) {
            MasterGUI.me.lblNotCurBufSizeVal.setText( "(trad => " + strOrig.both() + ")" );
            MasterGUI.me.lblCurBufSizeVal.setText( strNew.a );
            MasterGUI.me.lblCurBufSizeUnits.setText( strNew.b );
        }
        else {
            MasterGUI.me.lblNotCurBufSizeVal.setText( "(new => " + strNew.both() + ")" );
            MasterGUI.me.lblCurBufSizeVal.setText( strOrig.a );
            MasterGUI.me.lblCurBufSizeUnits.setText( strOrig.b );
        }
    }
    
    public int getLinkBW() {
        return linkBW;
    }

    public void setLinkBW(int bw_bps) throws IllegalArgValException {
        if( bw_bps <= 1000 || bw_bps > 1000 * 1000 * 1000 )
            throw( new IllegalArgValException("Link Bandwidth must be between 1kbps and 1Gbps") );
        
        this.linkBW = bw_bps;
        recomputeBufferSize();
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) throws IllegalArgValException {
        if( delay <= 0 || delay > 10000 )
            throw( new IllegalArgValException("Delay must be between 1 and 10000 ms") );
        
        this.delay = delay;
        recomputeBufferSize();
    }

    public int getNumGen() {
        return numGen;
    }

    public void setNumGen(int num_gen) throws IllegalArgValException {
        this.numGen = num_gen;
        if( num_gen <= 0 || num_gen > 100 )
            throw( new IllegalArgValException("must be between 1 and 100") );
    }

    public int getPayloadBW() {
        return payloadBW;
    }

    public void setPayloadBW(int payload_bw_bps) throws IllegalArgValException {
        if( payload_bw_bps <= 1000 || payload_bw_bps > 1000 * 1000 * 1000 )
            throw( new IllegalArgValException("Payload rate must be between 1kbps and 1Gbps") );
        
        this.payloadBW = payload_bw_bps;
    }

    public int getNumFlows() {
        return numFlows;
    }

    public void setNumFlows(int num_flows) throws IllegalArgValException {
        this.numFlows = num_flows;
        recomputeBufferSize();
    }

    public boolean isUseNumFlows() {
        return useNumFlows;
    }

    public void setUseNumFlows(boolean useNumFlows) {
        this.useNumFlows = useNumFlows;
    }
}
