//Filename: JTextFieldBound.java
//Revision: $Revision: 1.3 $
//Rev Date: $Date: 2007/02/22 15:35:13 $

package dgu.util.swing.binding;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.delegate.BoundComponent;
import dgu.util.swing.binding.delegate.JTextComponentDelegate;
import dgu.util.translator.SelfTranslator;
import dgu.util.translator.TypeTranslator;
import javax.swing.JTextField;

//</editor-fold>


/**
 * Describes a text field which holds a value of type FROM as a String
 * @author David Underhill
 */
public class JTextFieldBound<FROM> extends JTextField implements BoundComponent {
    
    //<editor-fold defaultstate="collapsed" desc="         Fields          ">
    
    private final JTextComponentDelegate<FROM> delegate;
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** Instantiates an unbound JTextFieldBound */
    public JTextFieldBound() {
        super();
        delegate = new JTextComponentDelegate<FROM>( this, this );
    }
    
    /** 
     * Instantiates an unbound JTextField
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JTextFieldBound( TypeTranslator<FROM, String> translator, String varName ) {
        super();
        delegate = new JTextComponentDelegate<FROM>( this, this, translator, varName );
    }
    
    /** 
     * Instantiates a bound JTextField
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JTextFieldBound( TypeTranslator<FROM, String> translator, Object boundItem, String varName ) {
        super();
        delegate = new JTextComponentDelegate<FROM>( this, this, translator, boundItem, varName );
    }
    
    /** 
     * Instantiates an unbound JTextField
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JTextFieldBound( TypeTranslator<FROM, String> translator, String getterName, String setterName ) {
        super();
        delegate = new JTextComponentDelegate<FROM>( this, this, translator, null, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JTextField
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JTextFieldBound( TypeTranslator<FROM, String> translator, Object boundItem, String getterName, String setterName ) {
        super();
        delegate = new JTextComponentDelegate<FROM>( this, this, translator, boundItem, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JTextField 
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public JTextFieldBound( TypeTranslator<FROM, String> translator, Object boundItem, String getterName, String setterName, int indexAt ) {
        super();
        delegate = new JTextComponentDelegate<FROM>( this, this, translator, boundItem, getterName, setterName, indexAt );
    }
    
    //</editor-fold>
            
    
    //<editor-fold defaultstate="collapsed" desc="String Assumed Constrctrs">
        
    /** 
     * Instantiates an unbound JTextField; generic type MUST be String
     *
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JTextFieldBound( String varName ) {
        super();
        delegate = (JTextComponentDelegate<FROM>)new JTextComponentDelegate<String>( 
                this, this, new SelfTranslator<String>(), varName );
    }
    
    /** 
     * Instantiates a bound JTextField; generic type MUST be String
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JTextFieldBound( Object boundItem, String varName ) {
        super();
        delegate = (JTextComponentDelegate<FROM>)new JTextComponentDelegate<String>( 
                this, this, new SelfTranslator<String>(), boundItem, varName );
    }
    
    /** 
     * Instantiates a bound JTextField; generic type MUST be String
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JTextFieldBound( Object boundItem, String getterName, String setterName ) {
        super();
        delegate = (JTextComponentDelegate<FROM>)new JTextComponentDelegate<String>( 
                this, this, new SelfTranslator<String>(), boundItem, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JTextField; generic type MUST be String
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public JTextFieldBound( Object boundItem, String getterName, String setterName, int indexAt ) {
        super();
        delegate = (JTextComponentDelegate<FROM>)new JTextComponentDelegate<String>( 
                this, this, new SelfTranslator<String>(), boundItem, getterName, setterName, indexAt );
    }
    
    //</editor-fold>
    
        
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
    
    /** gets the delete which controls the binding */
    public JTextComponentDelegate<FROM> getBindingDelegate() {
        return delegate;
    }
    
    //</editor-fold>
    
}
