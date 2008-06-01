//Filename: TranslationFailedException.java
//Revision: $Revision: 1.2 $
//Rev Date: $Date: 2007/02/20 13:13:59 $

package dgu.util.translator;


/**
 * Indicates a translation could not be accomplished
 * @author David Underhill
 */
public class TranslationFailedException extends Exception {
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** Instantiates a TranslationFailedException */
    public TranslationFailedException() {
        super( "Failed translation" );
    }
    
    /** Instantiates a TranslationFailedException with the specified message */
    public TranslationFailedException( String msg ) {
        super( msg );
    }
    
    //</editor-fold>
    
}
