//Filename: SelfTranslator.java
//Revision: $Revision: 1.1 $
//Rev Date: $Date: 2007/02/20 13:13:59 $

package dgu.util.translator;


/**
 * This translator does not do any translation; it just passes values through.  It is 
 * intended for use where a translator must be specified but isn't needed to convert
 * between types.
 *
 * @author David Underhill
 */
public class SelfTranslator<TYPE> extends TypeTranslator<TYPE, TYPE> {
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">

    /**
     * Instantiates a translator
     */
    public SelfTranslator() {
        /* Intentionally Blank */
    }

    //</editor-fold>


    //<editor-fold defaultstate="collapsed" desc="     Worker Methods      ">

    /**
     * gets the TYPE representation of the specified TYPE
     *
     * @param  valueToTranslate  some TYPE
     *
     * @return the TYPE passed in as valueToTranslate
     */
    public TYPE translate1( TYPE valueToTranslate ) {
        return valueToTranslate;
    }
    
    /**
     * gets the TYPE representation of the specified TYPE
     *
     * @param  valueToTranslate  some TYPE
     *
     * @return the TYPE passed in as valueToTranslate
     */
    public TYPE translate2( TYPE valueToTranslate ) {
        return valueToTranslate;
    }
        
    //</editor-fold>
    
}
