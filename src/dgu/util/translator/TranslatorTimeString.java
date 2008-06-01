//Filename: TranslatorTimeString.java
//Revision: $Revision: 1.1 $
//Rev Date: $Date: 2007/03/31 21:34:51 $

package dgu.util.translator;


/**
 * Stores a value as a Integer and can translate to and from an String.  The String
 * may specify the unit of time using its last character.  That String is resulting
 * from the translation from an Integer will be in the base unit.  The String may 
 * be specified as a Double, though any fraction of a second will be rounded to the 
 * nearest integer value.
 *
 * @author David Underhill
 */
public class TranslatorTimeString extends TypeTranslator<Integer, String> {
    
    private final int secondsInBaseUnit;
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">

    /** Instantiates a translator with the base unit of one second */
    public TranslatorTimeString() {
        this.secondsInBaseUnit = 1;
    }
    
    /** 
     * Instantiates a translator with the base unit of the specified number of seconds
     */
    public TranslatorTimeString( int secondsInBaseUnit ) {
        this.secondsInBaseUnit = secondsInBaseUnit;
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
     * Gets the Integer representation of the specified String in terms of the seconds in the base unit.  The
     * suffixes 's', 'm', 'h', and 'd' may be used to specify the units on the valueToTranslate.  If no unit 
     * is supplied, then 's' is assumed.
     *
     * @param  valueToTranslate  some String which may either be an Integer or a Double with some unit specification (e.g. 1.5h)
     *
     * @return the Integer representation of the valueToTranslate
     *
     * @throws TranslationFailedException  thrown if the value cannot be translated
     */
    public Integer translate2( String valueToTranslate ) throws TranslationFailedException {
        try {
            if( valueToTranslate.length()==0 )
                valueToTranslate = "0";
                
            Character lastChar = valueToTranslate.charAt( valueToTranslate.length() - 1 );
            if( Character.isLetter(lastChar) ) {
                double value = Double.valueOf( valueToTranslate.substring( 0, valueToTranslate.length()-1 ) ); //allow decimels
                
                switch( lastChar ) {
                    case 's': return (int)Math.round( (value *     1) / secondsInBaseUnit );
                    case 'm': return (int)Math.round( (value *    60) / secondsInBaseUnit );
                    case 'h': return (int)Math.round( (value *  3600) / secondsInBaseUnit );
                    case 'd': return (int)Math.round( (value * 86400) / secondsInBaseUnit );
                }
            }
                
            
            return (int)Math.round( Double.valueOf( valueToTranslate ) );
        } catch( NumberFormatException e ) {
            throw( new TranslationFailedException( "String to Integer translation failed: cannot convert the string `" + valueToTranslate + "`" ) );
        }
    }
    
    //</editor-fold>
    
}
