//Filename: TranslatorDoubleString.java
//Revision: $Revision: 1.2 $
//Rev Date: $Date: 2007/02/20 13:13:59 $

package dgu.util.translator;


/**
 * Stores a value as a String and can translate to and from an Double.
 * @author David Underhill
 */
public class TranslatorDoubleString extends TypeTranslator<Double, String> {
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">

    /**
     * Instantiates a translator
     */
    public TranslatorDoubleString() {
        /* Intentionally Blank */
    }

    //</editor-fold>


    //<editor-fold defaultstate="collapsed" desc="     Worker Methods      ">

    /**
     * gets the String representation of the specified Double
     *
     * @param  valueToTranslate  some Double
     *
     * @return the String representation of the valueToTranslate (null if valueToTranslate is null)
     */
    public String translate1( Double valueToTranslate ) {
        return ( valueToTranslate == null ) ? null : valueToTranslate.toString();
    }
    
    /**
     * gets the Double representation of the specified String
     *
     * @param  valueToTranslate  some String
     *
     * @return the Double representation of the valueToTranslate
     *
     * @throws TranslationFailedException  thrown if the value cannot be translated
     */
    public Double translate2( String valueToTranslate ) throws TranslationFailedException {
        try {
            return Double.valueOf( valueToTranslate );
        } catch( NumberFormatException e ) {
            throw( new TranslationFailedException( "String to Double translation failed: cannot convert the string `" + valueToTranslate + "`" ) );
        }
    }
    
    //</editor-fold>
    
}
