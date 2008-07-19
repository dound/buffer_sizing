//Filename: GenericDelegate.java
//Revision: $Revision: 1.10 $
//Rev Date: $Date: 2007/04/02 06:40:10 $

package dgu.util.swing.binding.delegate;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.Binding;
import dgu.util.swing.binding.BindingListener;
import dgu.util.swing.binding.BindingEvent;
import dgu.util.translator.TypeTranslator;
import java.util.Vector;

//</editor-fold>


/**
 * Binds some Object of type FROM to a component which requires type TO
 *
 * @author David Underhill
 */
public abstract class GenericDelegate<FROM, TO> implements BoundDelegateComponent {
    
    //<editor-fold defaultstate="collapsed" desc="         Fields          ">
    
    /** the owner of this delegate */
    private final BoundComponent owner;
    
    /** the Object this works for */
    private final BoundComponent me;
    
    /** whether or not this holds other components */
    private final boolean isContainer;
    
    /** the parent container of this object, if there is one */
    private BoundDelegateContainer parent = null;
    
    /** the object this field is bound to */
    private Binding<FROM, TO> binding;
    
    /** whether or not a validation error has occurred */
    private boolean validationError = false;
    
    /** a message describing the validation error if any */
    private String validationErrorMsg = "";
    
    /** whether or not to highlight on errors */
    private boolean highlightOnError = true;
    
    /** binding listeners */
    private Vector<BindingListener> listeners = new Vector<BindingListener>();
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** 
     * Instantiates an unbound Object Delegate 
     *
     * @param  owner   the BoundComponent which this belongs to
     * @param  compon  the component that this works for
     */
    public GenericDelegate( BoundComponent owner, BoundComponent compon ) {
        super();
        this.owner = owner;
        this.me = compon;
        this.isContainer = false;
        binding = null;
    }
    
    /** 
     * Instantiates an unbound Object Delegate
     *
     * @param  owner       the BoundComponent which this belongs to
     * @param  isContainer whether or not this component can contain others
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public GenericDelegate( boolean isContainer, BoundComponent owner, BoundComponent compon,
                                     TypeTranslator<FROM, TO> translator, String varName ) {
        super();
        this.owner = owner;
        this.me = compon;
        this.isContainer = isContainer;
        binding = new Binding<FROM, TO>( translator, null, varName );
    }
    
    /** 
     * Instantiates a bound Object Delegate
     *
     * @param  owner       the BoundComponent which this belongs to
     * @param  isContainer whether or not this component can contain others
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public GenericDelegate( boolean isContainer, BoundComponent owner, BoundComponent compon,
                                     TypeTranslator<FROM, TO> translator, Object boundItem, String varName ) {
        super();
        this.owner = owner;
        this.me = compon;
        this.isContainer = isContainer;
        binding = new Binding<FROM, TO>( translator, boundItem, varName );
    }
    
    /** 
     * Instantiates a bound Object Delegate
     *
     * @param  owner       the BoundComponent which this belongs to
     * @param  isContainer whether or not this component can contain others
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public GenericDelegate( boolean isContainer, BoundComponent owner, BoundComponent compon, 
                                     TypeTranslator<FROM, TO> translator, Object boundItem, String getterName, String setterName ) {
        super();
        this.owner = owner;
        this.me = compon;
        this.isContainer = isContainer;
        binding = new Binding<FROM, TO>( translator, boundItem, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound Object Delegate
     *
     * @param  owner       the BoundComponent which this belongs to
     * @param  isContainer whether or not this component can contain others
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public GenericDelegate( boolean isContainer, BoundComponent owner, BoundComponent compon, 
                                     TypeTranslator<FROM, TO> translator, Object boundItem, String getterName, String setterName, int indexAt ) {
        super();
        this.owner = owner;
        this.me = compon;
        this.isContainer = isContainer;
        binding = new Binding<FROM, TO>( translator, boundItem, getterName, setterName, indexAt );
    }
    
    /** suggested that this is used to load the component with its initial values after the constructor is done */
    protected abstract void completeInit();
    
    //</editor-fold>
        
    
    //<editor-fold defaultstate="collapsed" desc="     Listener Methods    ">
    
    /** 
     * Register a listener for this object
     * @param listener the object which is listening
     */
    public void addBindingListener( BindingListener listener ) { listeners.add( listener ); }
    
    /**
     * Notifies all listeners that the binding is about to change
     */
    public void notifyListenersOfBindingChanging() {
        BindingEvent e = new BindingEvent( this.me, this.getBinding() );
        for( BindingListener listener : listeners )
            listener.bindingChanging( e );
    }
    
    /**
     * Notifies all listeners that the binding has changed
     */
    public void notifyListenersOfBindingChanged() {
        BindingEvent e = new BindingEvent( this.me, this.getBinding() );
        for( BindingListener listener : listeners )
            listener.bindingChanged( e );
    }
    
    /**
     * Notifies all listeners that the binding has changed and been loaded
     */
    public void notifyListenersOfBindingLoaded() {
        BindingEvent e = new BindingEvent( this.me, this.getBinding() );
        for( BindingListener listener : listeners )
            listener.bindingLoaded( e );
    }
            
    /** 
     * Remove a registered listener from this object
     * @param listener the object which is listening to be removed
     */
    public void removeBindingListener( BindingListener listener ) { listeners.remove( listener ); }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="   Change the Binding    ">
    
    /** 
     * change what this object is bound to (will save the current value to its currently bound object)
     *
     * @param  b   the new binding
     */
    public final void changeBinding( Binding b ) {
        save();
        notifyListenersOfBindingChanging();
        binding = b;
        notifyListenersOfBindingChanged();
        load();
        notifyListenersOfBindingLoaded();
    }
    
    /** 
     * change what this object is bound to (will save the current value to its currently bound object)
     *
     * @param  newBoundItem  the object this is bound to (contains the value to be modified as specified by varName)
     */
    public void changeBinding( Object newBoundItem ) {
        changeBinding( binding.bindToNewObject( newBoundItem ) );
    }
        
    /** 
     * change what this object is bound to (will save the current value to its currently bound object)
     *
     * @param  newBoundItem  the object this is bound to (contains the value to be modified as specified by varName)
     * @param  index   the new index to bind to
     */
    public final void changeBinding( Object newBoundItem, int index ) {
        changeBinding( binding.bindToNewObject( newBoundItem, index ) );
    }
    
    /** 
     * change what this object is bound to (will save the current value to its currently bound object)
     *
     * @param  index   the new index to bind to
     */
    public final void changeBinding( int index ) {
        changeBinding( binding.bindToNewIndex( index ) );
    }
    
    /** 
     * change what this object is bound to (will save the current value to its currently bound object)
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public final void changeBinding( Object boundItem, String varName ) {
        changeBinding( new Binding<FROM, TO>( binding.getTranslator(), boundItem, varName ) );
    }
    
    /** 
     * change what this object is bound to (will save the current value to its currently bound object)
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public final void changeBinding( Object boundItem, String getterName, String setterName ) {
        changeBinding( new Binding<FROM, TO>( binding.getTranslator(), boundItem, getterName, setterName ) );
    }
    
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
    public final void changeBinding( Object boundItem, String getterName, String setterName, int indexAt ) {
        changeBinding( new Binding<FROM, TO>( binding.getTranslator(), boundItem, getterName, setterName, indexAt ) );
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="   Update the Binding    ">
    
    /** load the current text for this component from the bound object */
    public abstract void load();
    
    /** save the current text in this component to the bound object */
    public abstract void save();
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
    
    /** gets a reference to the BoundComponent this works for */
    public final BoundComponent getOwner() {
        return owner;
    }
    
    /** gets a reference to the Object this works for */
    public BoundComponent getMe() {
        return me;
    }
    
    /** gets the binding for this component */
    public final Binding<FROM, TO> getBinding() { return binding; }
    
    /** gets the parent container for this component */
    public final BoundDelegateContainer getBoundParent() {
        return parent;
    }
    
    /** sets the parent container for this component */
    public final void setBoundParent( BoundDelegateContainer c ) {
        parent = c;
    }
    
    /** whether or not this bound component is a container of other bound components */
    public final boolean isContainer() { return isContainer; }
    
    /** whether or not a validation error has occurred */
    public final boolean isValidated() { return !validationError; }
    
    /** draw the border to reflect the specified validation state */
    protected abstract void handleValidationState();

    /** whether or not to highlight with a red border on error */
    public final boolean highlightOnError() {
        return highlightOnError;
    }

    /** whether or not to highlight with a red border on error */
    public final void setHighlightOnError( boolean highlightOnError ) {
        this.highlightOnError = highlightOnError;
        if( !highlightOnError ) setValidationError( false ); //turn off the special highlighting if it was on
    }
    
    /** gets whether or not a validation error has occurred */
    public final boolean validationError() {
        return validationError;
    }

    /** sets whether or not a validation error has occurred and clears the validation error message */
    protected final void setValidationError( boolean validationError ) {
        this.validationError = validationError;
        this.validationErrorMsg = "";
        handleValidationState();
    }
    
    /** sets whether or not a validation error has occurred (also sets a validation error message too) */
    protected final void setValidationError( boolean validationError, String validationErrorMsg ) {
        this.validationError = validationError;
        this.validationErrorMsg = validationErrorMsg;
        handleValidationState();
    }
    
    /** gets the validation error message */ 
    protected final String getValidationErrorMsg() {
        return validationErrorMsg;
    }
    
    /** 
     * gets the validation error message prefaced by "\nError: " if there is an error message.  The
     * message is also broken into lines which are no more than 80 characters in length.
     */ 
    protected final String getFormattedValidationErrorMsg() {
        if( validationErrorMsg.length() > 0 )
            return dgu.util.StringOps.splitIntoLines( "<br>Error: ".concat( validationErrorMsg ), 80, "<br>", true );
        else 
            return "";
    }
    
    //</editor-fold>

   
    
}
