package dgu.util.translator;


/**
 * Stores a value as a String and can translate to and from an Long.
 * @author David Underhill
 */
public class TranslatorLongString extends TypeTranslator<Long, String> {
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">

    /**
     * Instantiates a translator
     */
    public TranslatorLongString() {
        /* Intentionally Blank */
    }

    //</editor-fold>


    //<editor-fold defaultstate="collapsed" desc="     Worker Methods      ">

    /**
     * gets the String representation of the specified Long
     *
     * @param  valueToTranslate  some Long
     *
     * @return the String representation of the valueToTranslate (null if valueToTranslate is null)
     */
    public String translate1( Long valueToTranslate ) {
        return ( valueToTranslate == null ) ? null : valueToTranslate.toString();
    }
    
    /**
     * gets the Long representation of the specified String
     *
     * @param  valueToTranslate  some String
     *
     * @return the Long representation of the valueToTranslate
     *
     * @throws TranslationFailedException  thrown if the value cannot be translated
     */
    public Long translate2( String valueToTranslate ) throws TranslationFailedException {
        try {
            return Long.valueOf( valueToTranslate );
        } catch( NumberFormatException e ) {
            throw( new TranslationFailedException( "String to Long translation failed: cannot convert the string `" + valueToTranslate + "`" ) );
        }
    }
    
    //</editor-fold>
    
}
