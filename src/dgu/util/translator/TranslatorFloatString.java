package dgu.util.translator;


/**
 * Stores a value as a String and can translate to and from an Float.
 * @author David Underhill
 */
public class TranslatorFloatString extends TypeTranslator<Float, String> {
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">

    /**
     * Instantiates a translator
     */
    public TranslatorFloatString() {
        /* Intentionally Blank */
    }

    //</editor-fold>


    //<editor-fold defaultstate="collapsed" desc="     Worker Methods      ">

    /**
     * gets the String representation of the specified Float
     *
     * @param  valueToTranslate  some Float
     *
     * @return the String representation of the valueToTranslate (null if valueToTranslate is null)
     */
    public String translate1( Float valueToTranslate ) {
        return ( valueToTranslate == null ) ? null : valueToTranslate.toString();
    }
    
    /**
     * gets the Float representation of the specified String
     *
     * @param  valueToTranslate  some String
     *
     * @return the Float representation of the valueToTranslate
     *
     * @throws TranslationFailedException  thrown if the value cannot be translated
     */
    public Float translate2( String valueToTranslate ) throws TranslationFailedException {
        try {
            return Float.valueOf( valueToTranslate );
        } catch( NumberFormatException e ) {
            throw( new TranslationFailedException( "String to Float translation failed: cannot convert the string `" + valueToTranslate + "`" ) );
        }
    }
    
    //</editor-fold>
    
}
