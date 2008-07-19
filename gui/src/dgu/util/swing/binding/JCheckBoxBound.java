//Filename: JCheckBoxBound.java
//Revision: $Revision: 1.1 $
//Rev Date: $Date: 2007/02/20 15:25:22 $

package dgu.util.swing.binding;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.delegate.BoundComponent;
import dgu.util.swing.binding.delegate.JToggleButtonComponentDelegate;
import dgu.util.translator.TypeTranslator;
import dgu.util.translator.SelfTranslator;
import javax.swing.JCheckBox;

//</editor-fold>


/**
 * Describes a checbox field which holds a value of type FROM as a Boolean
 * @author David Underhill
 */
public class JCheckBoxBound<FROM> extends JCheckBox implements BoundComponent {
    
    //<editor-fold defaultstate="collapsed" desc="         Fields          ">
    
    private final JToggleButtonComponentDelegate<FROM> delegate;
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** Instantiates an unbound JCheckBoxBound */
    public JCheckBoxBound() {
        super();
        delegate = new JToggleButtonComponentDelegate<FROM>( this, this );
    }
    
    /** 
     * Instantiates an unbound JCheckBox
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JCheckBoxBound( TypeTranslator<FROM, Boolean> translator, String varName ) {
        super();
        delegate = new JToggleButtonComponentDelegate<FROM>( this, this, translator, varName );
    }
    
    /** 
     * Instantiates a bound JCheckBox
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JCheckBoxBound( TypeTranslator<FROM, Boolean> translator, Object boundItem, String varName ) {
        super();
        delegate = new JToggleButtonComponentDelegate<FROM>( this, this, translator, boundItem, varName );
    }
    
    /** 
     * Instantiates an unbound JCheckBox
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JCheckBoxBound( TypeTranslator<FROM, Boolean> translator, String getterName, String setterName ) {
        this( translator, null, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JCheckBox
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JCheckBoxBound( TypeTranslator<FROM, Boolean> translator, Object boundItem, String getterName, String setterName ) {
        super();
        delegate = new JToggleButtonComponentDelegate<FROM>( this, this, translator, boundItem, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JCheckBox 
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public JCheckBoxBound( TypeTranslator<FROM, Boolean> translator, Object boundItem, String getterName, String setterName, int indexAt ) {
        super();
        delegate = new JToggleButtonComponentDelegate<FROM>( this, this, translator, boundItem, getterName, setterName, indexAt );
    }
    
    //</editor-fold>
            
    
    //<editor-fold defaultstate="collapsed" desc="Boolean Assumed Constrtrs">
        
    /** 
     * Instantiates an unbound JCheckBox; generic type MUST be Boolean
     *
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JCheckBoxBound( String varName ) {
        super();
        delegate = (JToggleButtonComponentDelegate<FROM>)new JToggleButtonComponentDelegate<Boolean>( 
                this, this, new SelfTranslator<Boolean>(), varName );
    }
    
    /** 
     * Instantiates a bound JCheckBox; generic type MUST be Boolean
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JCheckBoxBound( Object boundItem, String varName ) {
        super();
        delegate = (JToggleButtonComponentDelegate<FROM>)new JToggleButtonComponentDelegate<Boolean>( 
                this, this, new SelfTranslator<Boolean>(), boundItem, varName );
    }
    
    /** 
     * Instantiates an unbound JCheckBox; generic type MUST be Boolean
     *
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JCheckBoxBound( String getterName, String setterName ) {
        super();
        delegate = (JToggleButtonComponentDelegate<FROM>)new JToggleButtonComponentDelegate<Boolean>( 
                this, this, new SelfTranslator<Boolean>(), null, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JCheckBox; generic type MUST be Boolean
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JCheckBoxBound( Object boundItem, String getterName, String setterName ) {
        super();
        delegate = (JToggleButtonComponentDelegate<FROM>)new JToggleButtonComponentDelegate<Boolean>( 
                this, this, new SelfTranslator<Boolean>(), boundItem, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JCheckBox; generic type MUST be Boolean
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public JCheckBoxBound( Object boundItem, String getterName, String setterName, int indexAt ) {
        super();
        delegate = (JToggleButtonComponentDelegate<FROM>)new JToggleButtonComponentDelegate<Boolean>( 
                this, this, new SelfTranslator<Boolean>(), boundItem, getterName, setterName, indexAt );
    }
    
    //</editor-fold>
      
        
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
    
    /** gets the delete which controls the binding */
    public JToggleButtonComponentDelegate<FROM> getBindingDelegate() {
        return delegate;
    }
    
    //</editor-fold>
    
}
