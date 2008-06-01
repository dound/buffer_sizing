//Filename: TranslatorIntegerString.java
//Revision: $Revision: 1.2 $
//Rev Date: $Date: 2007/02/20 13:13:59 $

package dgu.util.translator;


/**
 * Stores a value as a String and can translate to and from an Integer.
 * @author David Underhill
 */
public class TranslatorIntegerString extends TypeTranslator<Integer, String> {
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">

    /**
     * Instantiates a translator
     */
    public TranslatorIntegerString() {
        /* Intentionally Blank */
    }

    //</editor-fold>


    //<editor-fold defaultstate="collapsed" desc="     Worker Methods      ">

    /**
     * gets the String representation of the specified Integer
     *
     * @param  valueToTranslate  some Integer
     *
     * @return the String representation of the valueToTranslate (null if valueToTranslate is null)
     */
    public String translate1( Integer valueToTranslate ) {
        return ( valueToTranslate == null ) ? null : valueToTranslate.toString();
    }
    
    /**
     * gets the Integer representation of the specified String
     *
     * @param  valueToTranslate  some String
     *
     * @return the Integer representation of the valueToTranslate
     *
     * @throws TranslationFailedException  thrown if the value cannot be translated
     */
    public Integer translate2( String valueToTranslate ) throws TranslationFailedException {
        try {
            return Integer.valueOf( valueToTranslate );
        } catch( NumberFormatException e ) {
            throw( new TranslationFailedException( "String to Integer translation failed: cannot convert the string `" + valueToTranslate + "`" ) );
        }
    }
    
    //</editor-fold>
    
}
