//Filename: TranslatorInstanceString.java
//Revision: $Revision: 1.4 $
//Rev Date: $Date: 2007/04/08 03:17:47 $

package dgu.util.translator;


/**
 * Can translate to an Object of type T from a string.  The Object is 
 * generated from the string by using the constructor in Object which 
 * has a single string as a parameter.  The string is generated from 
 * the Object using the toString method.
 *
 * @author David Underhill
 */
public class TranslatorInstanceString<T> extends TypeTranslator<T, String> {
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">

    private Class type;
    
    /**
     * Instantiates a translator for an Object
     */
    public TranslatorInstanceString() {
        this( Object.class );
    }
    
    /**
     * Instantiates a translator
     *
     * @param o  an instance of the object to translate (only used to get the class)
     */
    public TranslatorInstanceString( T o ) {
        this( o.getClass() );
    }
    
    /**
     * Instantiates a translator
     *
     * @param c  the class this translator translates from
     */
    public TranslatorInstanceString( Class c ) {
        this.type = c;
    }
    
    //</editor-fold>


    //<editor-fold defaultstate="collapsed" desc="     Worker Methods      ">

    /**
     * gets the String representation of the specified object T
     *
     * @param  valueToTranslate  some object T
     *
     * @return the String representation of the valueToTranslate (null if valueToTranslate is null)
     */
    public String translate1( T valueToTranslate ) {
        if( valueToTranslate == null ) return null;
        type = valueToTranslate.getClass();
        return valueToTranslate.toString();
    }
    
    /**
     * gets the T representation of the specified String
     *
     * @param  valueToTranslate  some String
     *
     * @return the T representation of the valueToTranslate
     *
     * @throws TranslationFailedException  thrown if the value cannot be translated
     */
    public T translate2( String valueToTranslate ) throws TranslationFailedException {
        try {
            Class cls = type.getClass();

            //find the constructor to use
            for( java.lang.reflect.Constructor c : cls.getConstructors() ) {
                if( c.getParameterTypes().length == 1 &&            //only 1 parameter
                    c.getParameterTypes()[0] == String.class ) {    //and it's of type string
                
                    //found the constructor: use it
                    return (T)c.newInstance( valueToTranslate );
                }
            }

            //constructor not found ... error
            throw( new TranslationFailedException( "String translation failed (Constructor Not Found)" ) );
        } catch( InstantiationException e ) {
            throw( new TranslationFailedException( "String translation failed (Instantiation Error): " +
                                                   "cannot convert the string `" + valueToTranslate + "`" ) );
        } catch( IllegalAccessException e ) {
            throw( new TranslationFailedException( "String translation failed (Illegal Access): " +
                                                   "cannot convert the string `" + valueToTranslate + "`" ) );
        } catch( java.lang.reflect.InvocationTargetException e ) {
            throw( new TranslationFailedException( "String translation failed (Invocation Error): " +
                                                   "cannot convert the string `" + valueToTranslate + "`" ) );
        }
    }
    
    //</editor-fold>
    
}
