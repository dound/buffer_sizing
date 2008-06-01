//Filename: TranslatorMulti.java
//Revision: $Revision: 1.1 $
//Rev Date: $Date: 2007/02/24 02:29:35 $

package dgu.util.translator;

import java.util.Vector;


/**
 * Can translate between two objects using different translaters depending
 * on what type the two objects actually are.  The user specifies which 
 * translators to use.
 *
 * @author David Underhill
 */
public class TranslatorMulti<FROM, TO> extends TypeTranslator<FROM, TO> {
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    private Class fromActual, toActual;
    
    private class Triplet {
        public Class from, to;
        public TypeTranslator<FROM, TO> translator;
        public Triplet( Class fromCls, Class toCls, TypeTranslator<FROM, TO> t ) { from=fromCls; to=toCls; translator=t; }
    }
    private Vector<Triplet> translators = new Vector<Triplet>();
    
    /** adds a translator to use */
    public void addTranslator( Class from, Class to, TypeTranslator t ) {
        translators.add( new Triplet(from, to, t) );
    }
        
    /**
     * Instantiates a translator for an Object
     */
    public TranslatorMulti() {
        /* Intentionally Blank */
    }
    
    //</editor-fold>


    //<editor-fold defaultstate="collapsed" desc="     Worker Methods      ">

    /**
     * gets the TO representation of the specified object FROM
     *
     * @param  valueToTranslate  some object of type FROM
     *
     * @return the TO representation of the valueToTranslate; null if FROM can't be translated to TO
     *
     * @throws TranslationFailedException  thrown if the value cannot be translated
     */
    public TO translate1( FROM valueToTranslate ) throws TranslationFailedException {
        fromActual = valueToTranslate.getClass();
        for( Triplet t : translators )
            if( t.from == valueToTranslate.getClass() )
                if( toActual == null || t.to == toActual )
                    return t.translator.translate1( valueToTranslate );
        
        throw( new TranslationFailedException( "Translation failed: translator from type " + 
                                               valueToTranslate.getClass().getName() + " not found." ) );
    }
    
    /**
     * gets the FROM representation of the specified TO object
     *
     * @param  valueToTranslate  some object of type TO
     *
     * @return the FROM representation of the valueToTranslate
     *
     * @throws TranslationFailedException  thrown if the value cannot be translated
     */
    public FROM translate2( TO valueToTranslate ) throws TranslationFailedException {
        toActual = valueToTranslate.getClass();
        for( Triplet t : translators )
            if( t.to == valueToTranslate.getClass() )
                if( fromActual == null || t.from == fromActual )
                    return t.translator.translate2( valueToTranslate );
        
        throw( new TranslationFailedException( "Translation failed: translator from type " + 
                                               valueToTranslate.getClass().getName() + " not found." ) );
    }
    
    //</editor-fold>
    
}
