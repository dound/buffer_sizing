//Filename: JRadioButtonBound.java
//Revision: $Revision: 1.1 $
//Rev Date: $Date: 2007/02/20 15:25:22 $

package dgu.util.swing.binding;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.delegate.BoundComponent;
import dgu.util.swing.binding.delegate.ButtonGroupDelegate;
import dgu.util.swing.binding.delegate.JToggleButtonComponentDelegate;
import dgu.util.translator.SelfTranslator;
import dgu.util.translator.TypeTranslator;
import javax.swing.ButtonGroup;

//</editor-fold>


/**
 * Describes a group of JRadioButtonBound's whose selection value of type TO maps to 
 * the bound value of type FROM.
 *
 * @author David Underhill
 */
public class ButtonGroupBound<FROM, TO> extends ButtonGroup implements BoundComponent {
    
    //<editor-fold defaultstate="collapsed" desc="         Fields          ">
    
    private final ButtonGroupDelegate<FROM, TO> delegate;
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** Instantiates an unbound ButtonGroup */
    public ButtonGroupBound() {
        super();
        delegate = new ButtonGroupDelegate<FROM, TO>( this, this );
    }
    
    /** 
     * Instantiates an unbound ButtonGroup
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public ButtonGroupBound( TypeTranslator<FROM, TO> translator, String varName ) {
        super();
        delegate = new ButtonGroupDelegate<FROM, TO>( this, this, translator, varName );
    }
    
    /** 
     * Instantiates a bound ButtonGroup
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public ButtonGroupBound( TypeTranslator<FROM, TO> translator, Object boundItem, String varName ) {
        super();
        delegate = new ButtonGroupDelegate<FROM, TO>( this, this, translator, boundItem, varName );
    }
    
    /** 
     * Instantiates an unbound ButtonGroup
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public ButtonGroupBound( TypeTranslator<FROM, TO> translator, String getterName, String setterName ) {
        this( translator, null, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound ButtonGroup
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public ButtonGroupBound( TypeTranslator<FROM, TO> translator, Object boundItem, String getterName, String setterName ) {
        super();
        delegate = new ButtonGroupDelegate<FROM, TO>( this, this, translator, boundItem, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound ButtonGroup 
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public ButtonGroupBound( TypeTranslator<FROM, TO> translator, Object boundItem, String getterName, String setterName, int indexAt ) {
        super();
        delegate = new ButtonGroupDelegate<FROM, TO>( this, this, translator, boundItem, getterName, setterName, indexAt );
    }
    
    //</editor-fold>
        
    
    //<editor-fold defaultstate="collapsed" desc="SameSelf Assmd Constrctrs">
        
    /** 
     * Instantiates an unbound ButtonGroup; generic types MUST be the same
     *
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public ButtonGroupBound( String varName ) {
        super();
        delegate = (ButtonGroupDelegate<FROM, TO>)new ButtonGroupDelegate<FROM, TO>( 
                this, this, new SelfTranslator(), varName );
    }
    
    /** 
     * Instantiates a bound ButtonGroup; generic types MUST be the same
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public ButtonGroupBound( Object boundItem, String varName ) {
        super();
        delegate = (ButtonGroupDelegate<FROM, TO>)new ButtonGroupDelegate<FROM, TO>( 
                this, this, new SelfTranslator(), boundItem, varName );
    }
    
    /** 
     * Instantiates a bound ButtonGroup; generic types MUST be the same
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public ButtonGroupBound( Object boundItem, String getterName, String setterName ) {
        super();
        delegate = (ButtonGroupDelegate<FROM, TO>)new ButtonGroupDelegate<FROM, TO>( 
                this, this, new SelfTranslator(), boundItem, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound ButtonGroup; generic types MUST be the same
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public ButtonGroupBound( Object boundItem, String getterName, String setterName, int indexAt ) {
        super();
        delegate = (ButtonGroupDelegate<FROM, TO>)new ButtonGroupDelegate<FROM, TO>( 
                this, this, new SelfTranslator(), boundItem, getterName, setterName, indexAt );
    }
    
    //</editor-fold>
    
            
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
    
    /**
     * Adds the button to the group.
     * @param b the button to be added
     */ 
    public void add( JRadioButtonBound<TO> b) {
        delegate.add( b );
    }
    
    /** gets the delete which controls the binding */
    public ButtonGroupDelegate<FROM, TO> getBindingDelegate() {
        return delegate;
    }
    
    //</editor-fold>
    
}
