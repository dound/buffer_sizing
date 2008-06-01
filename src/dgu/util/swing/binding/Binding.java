//Filename: Binding.java
//Revision: $Revision: 1.7 $
//Rev Date: $Date: 2007/03/20 14:06:45 $

package dgu.util.swing.binding;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import dgu.util.translator.TypeTranslator;
import dgu.util.translator.TranslationFailedException;

//</editor-fold>


/**
 * Describes a binding which holds a value of type T as a String
 * @author David Underhill
 */
public class Binding<FROM, TO> {
    
    //<editor-fold defaultstate="collapsed" desc="         Fields          ">
    
    private Method getter, setter;
    private TypeTranslator<FROM, TO> translator;
    private Object boundItem;
    private int indexAt = -1;
    
    private String getterName;
    private String setterName;
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** 
     * Instantiates a binding 
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public Binding( TypeTranslator<FROM, TO> translator, Object boundItem, String varName ) {
        this( translator, boundItem, "get" + nameForMethod(varName), "set" + nameForMethod(varName) );
    }
    
    /** 
     * Instantiates a binding 
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public Binding( TypeTranslator<FROM, TO> translator, Object boundItem, String getterName, String setterName ) {
        this( translator, boundItem, getterName, setterName, -1 );
    }
    
    /** 
     * Instantiates a binding 
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public Binding( TypeTranslator<FROM, TO> translator, Object boundItem, String getterName, String setterName, int indexAt ) {
        super();
        
        this.translator = translator;
        this.boundItem = boundItem;
        this.indexAt = indexAt;
        this.getterName = getterName;
        this.setterName = setterName;
        
        if( indexAt == -1 ) {
            getter = findMethod( getterName, new Class[]{} );                   //no params
            setter = findMethod( setterName, new Class[]{null} );               //one param of any type
        } else {
            getter = findMethod( getterName, new Class[]{int.class} );      //one param: int [the index]
            setter = findMethod( setterName, new Class[]{int.class,null} ); //two params: int [the index] then one of any type
        }
    }
    
    /** capitalizes first letter in name */
    private static String nameForMethod( String name ) {
        String ret = "";
        if( name != null && name.length() > 0 ) {
            ret = String.valueOf( Character.toUpperCase( name.charAt(0) ) );
            
            if( name.length() > 1 )
                ret = ret.concat( name.substring( 1 ) );
        }
        return ret;
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="     Worker Methods      ">
    
    /** bind to a new index on the same object */
    public Binding bindToNewIndex( int index ) {
        return new Binding( translator, boundItem, this.getGetterName(), this.getSetterName(), index );
    }
    
    /** bind to a new object and index */
    public Binding bindToNewObject( Object newBoundItem, int index ) {
        return new Binding( translator, newBoundItem, this.getGetterName(), this.getSetterName(), index );
    }
    
    /** bind to a new object (no index) */
    public Binding bindToNewObject( Object newBoundItem ) {
        return new Binding( translator, newBoundItem, this.getGetterName(), this.getSetterName(), -1 );
    }

    /** 
     * find the method to get/set the variable held by this component
     * 
     * @param methodName      name of the method to find
     * @param expectedParams  the types of the paramters the method must take (null elements in this array are treated
     *                        as a wildcard and will allow any type in that position)
     */
    private Method findMethod( String methodName, Class[] expectedParams ) {
        if( boundItem == null || methodName == null || methodName.length() == 0 ) return null;
        
        for( Method m : boundItem.getClass().getMethods() ) {
            if( m.getName().equals( methodName )  ) {
                
                //see if the parameters match
                Class[] actualParams = m.getParameterTypes();
                if( actualParams.length == expectedParams.length ) {
                    boolean allMatch = true;
                    for( int i=0; i<actualParams.length; i++ ) {
                        //treat expected parameters which are null as a "wildcard" (i.e. allow any type)
                        if( (expectedParams[i] != null) && (actualParams[i] != expectedParams[i]) ) {
                            allMatch = false;
                            break;
                        }
                    }
                    
                    if( allMatch ) return m;
                }
            }
        }

        String paramsTxt = "";
        for( int i=0; i<expectedParams.length; i++ ) {
            paramsTxt = paramsTxt.concat( expectedParams[i].getName() );
            if( i+1 != expectedParams.length ) paramsTxt = paramsTxt.concat( ", " );
        }
        
        throw( new Error( "Method " + methodName + "( " + paramsTxt + " ) not found in class " + boundItem.getClass().getName() ) );
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
    
    /** gets the object this binding is to */
    public Object getBoundItem() {
        return boundItem;
    }
    
    /** gets the getter's name */
    public String getGetterName() { return getterName; }
    
    /** gets the setter's name */
    public String getSetterName() { return setterName; }
    
    /** get the index in the bounditem which we are looking at */
    public int getIndexAt() { return indexAt; }
    
    /** 
     * gets the actual value stored in the object 
     *
     * @throws IllegalArgumentException  thrown if the value could not be set to the specified value because the getter threw an exception
     */
    public FROM getValue() throws IllegalArgumentException {
        if( getter == null ) return null;
        
        try {
            if( indexAt >= 0 )
                return (FROM)getter.invoke( boundItem, indexAt );
            else
                return (FROM)getter.invoke( boundItem );
        } catch( IllegalAccessException e ) {
            throw( new Error( "Cannot access the method " + getter.getName() + " in " + boundItem.getClass().getName() ) );
        } catch( InvocationTargetException e ) {
            //throw( new IllegalArgumentException( "Exception thrown by the method " + getter.getName() + " in " + boundItem.getClass().getName() ) );
            String msg = e.getCause().getMessage();
            throw( new IllegalArgumentException( msg ) ); //rethrow the exception as an IllegalArgumentException
        }
    }
    
    /** 
     * gets the translated value from the value stored in the object 
     *
     * @throws IllegalArgumentException  thrown if the value could not be set to the specified value because the getter threw an exception
     * @throws TranslationFailedException  thrown if the value cannot be translated
     */
    public TO getTranslatedValue() throws IllegalArgumentException, TranslationFailedException {        
        return getTranslator().translate1( getValue() );
    }
    
    /** 
     * sets the value stored in the object to the specified value 
     *
     * @throws IllegalArgumentException  thrown if the value could not be set to the specified value because the setter threw an exception
     */
    public void setValue( FROM value ) throws IllegalArgumentException {
        if( setter == null ) return;
        
        try {
            if( indexAt >= 0 )
                setter.invoke( boundItem, indexAt, value );
            else
                setter.invoke( boundItem, value );
        } catch( IllegalAccessException e ) {
            throw( new Error( "Cannot access the method " + setter.getName() + " in " + boundItem.getClass().getName() ) );
        } catch( InvocationTargetException e ) {
            //throw( new IllegalArgumentException( "Exception thrown by the method " + setter.getName() + " in " + boundItem.getClass().getName() ) );
            String msg = e.getCause().getMessage();
            throw( new IllegalArgumentException( msg ) ); //rethrow the exception as an IllegalArgumentException
        }
    }
    
    /** 
     * sets the actual value from the specified value to be translated
     *
     * @throws IllegalArgumentException  thrown if the value could not be set to the specified value because the setter threw an exception
     * @throws TranslationFailedException  thrown if the value cannot be translated
     */
    public void setFromTranslatedValue( TO value ) throws IllegalArgumentException, TranslationFailedException {
        setValue( getTranslator().translate2( value ) );
    }
    
    /**
     * whether or not this value can be used to set the value of the bound object
     * @return true if the value can be translated
     */
    public boolean canSetFromTranslatedValue( TO value ) {
        return getTranslator().canTranslate2( value );
    }
    
    /** gets the translator */
    public TypeTranslator<FROM, TO> getTranslator() {
        return translator;
    }
        
    //</editor-fold>

}
