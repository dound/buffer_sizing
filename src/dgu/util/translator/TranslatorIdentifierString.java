//Filename: TranslationIdentifierString.java
//Revision: $Revision: 1.2 $
//Rev Date: $Date: 2007/03/01 04:14:50 $

package dgu.util.translator;


/**
 * Stores a value as a String which meets the requirements as an identifier.  When 
 * asked to translate, it allows appropriate identifiers to pass through and throws
 * a TranslationFailedException otherwise.  Spaces are converted to underscores.
 *
 * @author David Underhill
 */
public class TranslatorIdentifierString extends TypeTranslator<String, String> {
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">

    /**
     * Instantiates a translator
     */
    public TranslatorIdentifierString() {
        /* Intentionally Blank */
    }

    //</editor-fold>


    //<editor-fold defaultstate="collapsed" desc="     Worker Methods      ">

    /**
     * returns the String (identifier) passed in
     *
     * @param  valueToTranslate  some identifier (assumed to valid)
     *
     * @return the identifier
     */
    public String translate1( String valueToTranslate ) {
        return valueToTranslate;
    }
    
    /**
     * Checks to see if the specified valueToTranslate is valid identifier
     *
     * @param  valueToTranslate  some String (should be an identifier)
     *
     * @return the identifier representation of the valueToTranslate (spaces will be 
     *         replaced with underscores).
     *
     * @throws TranslationFailedException  thrown if the valueToTranslate is an invalid identifier
     */
    public String translate2( String valueToTranslate ) throws TranslationFailedException {
        valueToTranslate = valueToTranslate.replaceAll( " ", "_" ); //replace spaces with underscores
        if( valueToTranslate.matches( "[a-zA-Z_][a-zA-Z_0-9]*" ) )
            return valueToTranslate;
        else
            throw( new TranslationFailedException( "Invalid Identifier: an identifier must start with a letter " +
                    " or underscore and consist of all letters, numbers, and underscores.  " ) );
    }
    
    //</editor-fold>
    
}
