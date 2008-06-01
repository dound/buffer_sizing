//Filename: TranslatorBooleanString.java
//Revision: $Revision: 1.2 $
//Rev Date: $Date: 2007/02/20 13:13:59 $

package dgu.util.translator;


/**
 * Stores a value as a String and can translate to and from an Boolean.
 * @author David Underhill
 */
public class TranslatorBooleanString extends TypeTranslator<Boolean, String> {
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">

    /**
     * Instantiates a translator
     */
    public TranslatorBooleanString() {
        /* Intentionally Blank */
    }

    //</editor-fold>


    //<editor-fold defaultstate="collapsed" desc="     Worker Methods      ">

    /**
     * gets the String representation of the specified Boolean
     *
     * @param  valueToTranslate  some Boolean
     *
     * @return the String representation of the valueToTranslate (null if valueToTranslate is null)
     */
    public String translate1( Boolean valueToTranslate ) {
        return ( valueToTranslate == null ) ? null : valueToTranslate.toString();
    }
    
    /**
     * gets the Boolean representation of the specified String
     *
     * @param  valueToTranslate  some String
     *
     * @return the Boolean representation of the valueToTranslate
     *
     * @throws TranslationFailedException  thrown if the value cannot be translated
     */
    public Boolean translate2( String valueToTranslate ) throws TranslationFailedException {
        try {
            return Boolean.valueOf( valueToTranslate );
        } catch( NumberFormatException e ) {
            throw( new TranslationFailedException( "String to Boolean translation failed: cannot convert the string `" + valueToTranslate + "`" ) );
        }
    }
    
    //</editor-fold>
    
}
