//Filename: IntermediateFieldDelegate.java
//Revision: $Revision: 1.3 $
//Rev Date: $Date: 2007/02/26 15:13:38 $

package dgu.util.swing.binding.delegate;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.Binding;
import dgu.util.swing.binding.IntermediateField;
import dgu.util.swing.binding.JRadioButtonBound;
import dgu.util.swing.binding.delegate.BoundDelegateComponent;
import dgu.util.swing.binding.delegate.BoundDelegateContainer;
import dgu.util.translator.SelfTranslator;
import java.util.Enumeration;
import java.util.Vector;

//</editor-fold>


/**
 * A delegate for an intermediate field of type TYPE.
 *
 * @author David Underhill
 */
public class IntermediateFieldDelegate<TYPE> extends GenericDelegate<TYPE, TYPE> implements BoundDelegateContainer { 
        
    //<editor-fold defaultstate="collapsed" desc="         Fields          ">
    
    /** components whose bindings are contained within the objects contained in this list */
    protected Vector<BoundComponent> components = new Vector<BoundComponent>();
    
    /** the index of the subcomponent which determines the names of the 
     *  entries in the list (-1 [default] => number them instead)       */
    protected int primaryComponentIndex = -1;
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** 
     * Instantiates an unbound ButtonGroup Delegate 
     *
     * @param  owner   the BoundComponent which this belongs to
     * @param  compon  the component that this works for
     */
    public IntermediateFieldDelegate( BoundComponent owner, IntermediateField compon ) {
        super( owner, compon );
    }
    
    /** 
     * Instantiates an unbound ButtonGroup Delegate
     *
     * @param  owner       the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public IntermediateFieldDelegate( BoundComponent owner, IntermediateField compon, String varName ) {
        super( true, owner, compon, new SelfTranslator<TYPE>(), varName );
        completeInit();
    }
    
    /** 
     * Instantiates a bound ButtonGroup Delegate
     *
     * @param  owner       the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public IntermediateFieldDelegate( BoundComponent owner, IntermediateField compon, Object boundItem, String varName ) {
        super( true, owner, compon, new SelfTranslator<TYPE>(), boundItem, varName );
        completeInit();
    }
    
    /** 
     * Instantiates a bound ButtonGroup Delegate
     *
     * @param  owner       the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public IntermediateFieldDelegate( BoundComponent owner, IntermediateField compon, 
                                      Object boundItem, String getterName, String setterName ) {
        super( true, owner, compon, new SelfTranslator<TYPE>(), boundItem, getterName, setterName );
        completeInit();
    }
    
    /** does nothing */
    protected void completeInit() {
        //nothing to do
    }
    
    //</editor-fold>
            
        
    //<editor-fold defaultstate="collapsed" desc="   Update the Binding    ">
    
    /** refreshes subcomponents bindings */
    public void load() {
        updateSubcomponentBindings();
    }
    
    /** tells all subcomponents to save their current value */
    public void save() {
        for( BoundComponent b : components )
            b.getBindingDelegate().save();
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
  
    /** get the ButtonGroup this is bound to */
    private IntermediateField me() { return (IntermediateField)super.getMe(); }
    
    /** get the binding this is bound to */
    private Binding<TYPE, TYPE> binding() { 
        return super.getBinding(); 
    }
    
    /** draw the border to reflect the specified validation state */
    protected void handleValidationState() {
        //ignore: field is invisible
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="  BoundDelegateContainer Methods ">
    
    /** add bound component to this (b's parent will be set to this) */
    public final void addBoundComponent( BoundComponent b ) {
        components.add( b );
        b.getBindingDelegate().setBoundParent( this );
    }
    
    /** get bound components this contains */
    public final Vector<BoundComponent> getBoundComponents() {
        return components;
    }
    
    /** update all components within this component */
    public final void updateSubcomponentBindings() {
        //re-bind each component to this field
        for( BoundComponent b : components ) {
            BoundDelegateComponent d = b.getBindingDelegate();
            
            d.changeBinding( this.getBinding().getValue() );
            
            //if the item is a container, select the first item in it
            if( d.isContainer() ) 
                ((BoundContainer)b).getBindingDelegate().selectItem( 0 );
        }
    }
    
    /** informs its parent */
    public void notifyBoundComponentChange( BoundComponent c ) {
        if( getBoundParent() != null )
            getBoundParent().notifyBoundComponentChange( me() );
    }
    
    /** does nothing */
    public void selectItem( int index ) {
        //does nothing
    }
    
    /** 
     * gets the value of the primary component if possible, otherwise just return this
     */
    private final Object getPrimaryComponentValue() {
        try {
            BoundComponent c = getBoundComponents().get( primaryComponentIndex );
            if( c instanceof IntermediateField ) {
                IntermediateField cc = (IntermediateField)c;
                return cc.getBindingDelegate().getPrimaryComponentValue().toString();
            } else {
                Object ret = c.getBindingDelegate().getBinding().getValue();
                if( ret == null ) 
                    return "null";
                else
                    return ret;
            }
        } catch( Exception e ) {
            return this;
        }
    }
    
    public String toString() {
        return getPrimaryComponentValue().toString();
    }
    
    /** 
     * gets the index of the component which whose string value is used as the value to 
     * represent that collection, or -1 to just number the sets 
     */
    public final int getPrimaryComponent() {
        return primaryComponentIndex;
    }
    
    /** 
     * sets the index of the component which whose string value is used as the value to 
     * represent that collection, or -1 to just number the sets
     * 
     * @param primComponIndex  index of the BoundComponent which is the primary component in the 
     *                         getBoundComponents() vector.
     */
    public final void setPrimaryComponent( int primComponIndex ) {
        this.primaryComponentIndex = primComponIndex;
    }
    
    //</editor-fold>
    
}
