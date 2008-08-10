//Filename: TypeTranslator.java
//Revision: $Revision: 1.3 $
//Rev Date: $Date: 2007/02/28 06:05:13 $

package dgu.util.translator;


/**
 * Declares translation methods a parameter must have so it can be translated 
 * between a one type and another type.
 *
 * @author David Underhill
 */
public abstract class TypeTranslator<TYPE1, TYPE2> implements java.io.Serializable {

    /** the last exception thrown when the translation was attempted */
    private TranslationFailedException lastException = null;
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">

    /**
     * Instantiates a translator
     */
    public TypeTranslator() {
        /* Intentionally Blank */
    }

    //</editor-fold>


    //<editor-fold defaultstate="collapsed" desc="     Worker Methods      ">

    /**
     * whether or not the specified value can be translated to TYPE2
     *
     * @param  valueToTranslate  some value
     *
     * @return true if the translation is possible
     */
    public boolean canTranslate1( TYPE1 valueToTranslate ) {
        try {
            translate1( valueToTranslate ); //ignore the return; just try to translate ...
            return true;
        } catch( TranslationFailedException e ) {
            lastException = e;
            return false;
        }
    }
    
    /**
     * gets the TYPE2 representation of the item
     *
     * @param  valueToTranslate  some value
     *
     * @return the TYPE2 representation of the valueToTranslate
     *
     * @throws TranslationFailedException  thrown if the value cannot be translated
     */
    public abstract TYPE2 translate1( TYPE1 valueToTranslate ) throws TranslationFailedException;
    
    /**
     * whether or not the specified value can be translated to TYPE1
     *
     * @param  valueToTranslate  some value
     *
     * @return true if the translation is possible
     */
    public boolean canTranslate2( TYPE2 valueToTranslate ) {
        try {
            translate2( valueToTranslate ); //ignore the return; just try to translate ...
            return true;
        } catch( TranslationFailedException e ) {
            lastException = e;
            return false;
        }
    }
    
    /**
     * gets the TYPE1 representation of the item
     *
     * @param  valueToTranslate  some value
     *
     * @return the TYPE1 representation of the valueToTranslate
     *
     * @throws TranslationFailedException  thrown if the value cannot be translated
     */
    public abstract TYPE1 translate2( TYPE2 valueToTranslate ) throws TranslationFailedException;
    
    //</editor-fold>

    /** gets the last exception thrown when the translation was attempted (null if none have been thown) */
    public TranslationFailedException getLastException() {
        return lastException;
    }

}
