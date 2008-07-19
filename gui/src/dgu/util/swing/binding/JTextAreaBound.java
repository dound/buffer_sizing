//Filename: JTextAreaBound.java
//Revision: $Revision: 1.1 $
//Rev Date: $Date: 2007/02/20 15:25:22 $
//Author's Note: identical to JTextField except all occurrences of JTextField are replaced with JTextArea

package dgu.util.swing.binding;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.delegate.BoundComponent;
import dgu.util.swing.binding.delegate.JTextComponentDelegate;
import dgu.util.translator.SelfTranslator;
import dgu.util.translator.TypeTranslator;
import javax.swing.JTextArea;

//</editor-fold>


/**
 * Describes a text area which holds a value of type FROM as a String
 * @author David Underhill
 */
public class JTextAreaBound<FROM> extends JTextArea implements BoundComponent {
    
    //<editor-fold defaultstate="collapsed" desc="         Fields          ">
    
    private final JTextComponentDelegate<FROM> delegate;
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** Instantiates an unbound JTextAreaBound */
    public JTextAreaBound() {
        super();
        delegate = new JTextComponentDelegate<FROM>( this, this );
    }
    
    /** 
     * Instantiates an unbound JTextArea
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JTextAreaBound( TypeTranslator<FROM, String> translator, String varName ) {
        super();
        delegate = new JTextComponentDelegate<FROM>( this, this, translator, varName );
    }
    
    /** 
     * Instantiates a bound JTextArea
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JTextAreaBound( TypeTranslator<FROM, String> translator, Object boundItem, String varName ) {
        super();
        delegate = new JTextComponentDelegate<FROM>( this, this, translator, boundItem, varName );
    }
    
    /** 
     * Instantiates an unbound JTextArea
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JTextAreaBound( TypeTranslator<FROM, String> translator, String getterName, String setterName ) {
        this( translator, null, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JTextArea
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JTextAreaBound( TypeTranslator<FROM, String> translator, Object boundItem, String getterName, String setterName ) {
        super();
        delegate = new JTextComponentDelegate<FROM>( this, this, translator, boundItem, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JTextArea 
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public JTextAreaBound( TypeTranslator<FROM, String> translator, Object boundItem, String getterName, String setterName, int indexAt ) {
        super();
        delegate = new JTextComponentDelegate<FROM>( this, this, translator, boundItem, getterName, setterName, indexAt );
    }
    
    //</editor-fold>
        
    
    //<editor-fold defaultstate="collapsed" desc="String Assumed Constrctrs">
        
    /** 
     * Instantiates an unbound JTextArea; generic type MUST be String
     *
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JTextAreaBound( String varName ) {
        super();
        delegate = (JTextComponentDelegate<FROM>)new JTextComponentDelegate<String>( 
                this, this, new SelfTranslator<String>(), varName );
    }
    
    /** 
     * Instantiates a bound JTextArea; generic type MUST be String
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JTextAreaBound( Object boundItem, String varName ) {
        super();
        delegate = (JTextComponentDelegate<FROM>)new JTextComponentDelegate<String>( 
                this, this, new SelfTranslator<String>(), boundItem, varName );
    }
    
    /** 
     * Instantiates a bound JTextArea; generic type MUST be String
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JTextAreaBound( Object boundItem, String getterName, String setterName ) {
        super();
        delegate = (JTextComponentDelegate<FROM>)new JTextComponentDelegate<String>( 
                this, this, new SelfTranslator<String>(), boundItem, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JTextArea; generic type MUST be String
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public JTextAreaBound( Object boundItem, String getterName, String setterName, int indexAt ) {
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
