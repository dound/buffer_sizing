//Filename: BoundDelegateComponent.java
//Revision: $Revision: 1.3 $
//Rev Date: $Date: 2007/03/07 04:22:49 $

package dgu.util.swing.binding.delegate;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.Binding;
import dgu.util.swing.binding.BindingListener;
import dgu.util.swing.binding.BindingEvent;

//</editor-fold>


/**
 * This interface specifies the methods which bind some object to some component.
 *
 * @author David Underhill
 */
public interface BoundDelegateComponent {
        
    //<editor-fold defaultstate="collapsed" desc="   Change the Binding    ">
    
    /** 
     * change what this object is bound to (will save the current value to its currently bound object)
     *
     * @param  b   the new binding
     */
    public void changeBinding( Binding b );
    
    /** 
     * change what this object is bound to (will save the current value to its currently bound object)
     *
     * @param  newBoundItem  the object this is bound to (contains the value to be modified as specified by varName)
     */
    public void changeBinding( Object newBoundItem );
    
    /** 
     * change what this object is bound to (will save the current value to its currently bound object)
     *
     * @param  newBoundItem  the object this is bound to (contains the value to be modified as specified by varName)
     * @param  index   the new index to bind to
     */
    public void changeBinding( Object newBoundItem, int index );
    
    /** 
     * change what this object is bound to (will save the current value to its currently bound object)
     *
     * @param  index   the new index to bind to
     */
    public void changeBinding( int index );
    
    /** 
     * change what this object is bound to (will save the current value to its currently bound object)
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public void changeBinding( Object boundItem, String varName );
    
    /** 
     * change what this object is bound to (will save the current value to its currently bound object)
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public void changeBinding( Object boundItem, String getterName, String setterName );
    
    /** 
     * change what this object is bound to (will save the current value to its currently bound object)
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public void changeBinding( Object boundItem, String getterName, String setterName, int indexAt );
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="   Update the Binding    ">
    
    /** load the current value for this component from the bound object */
    public void load();
    
    /** save the current value in this component to the bound object */
    public void save();
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
    
    /** 
     * Register a listener for this object
     * @param listener the object which is listening
     */
    public void addBindingListener( BindingListener listener );
    
    /**
     * Notifies all listeners that the binding is about to change
     */
    public void notifyListenersOfBindingChanging();
    
    /**
     * Notifies all listeners that the binding has changed
     */
    public void notifyListenersOfBindingChanged();
    
    /** 
     * Remove a registered listener from this object
     * @param listener the object which is listening to be removed
     */
    public void removeBindingListener( BindingListener listener );
    
    /** gets the binding for this component */
    public Binding getBinding();
    
    /** gets the parent container for this component */
    public BoundDelegateContainer getBoundParent();
    
    /** sets the parent container for this component */
    public void setBoundParent( BoundDelegateContainer c );
    
    /** whether or not this bound component is a container of other bound components */
    public boolean isContainer();
    
    /** whether or not a validation error has occurred */
    public boolean isValidated();
    
    /** whether or not to highlight with a red border on error */
    public boolean highlightOnError();

    /** whether or not to highlight with a red border on error */
    public void setHighlightOnError(boolean highlightOnError);
    
    //</editor-fold>
        
}
