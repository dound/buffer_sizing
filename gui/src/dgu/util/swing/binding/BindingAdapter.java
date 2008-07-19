//Filename: BindingAdapter.java
//Revision: $Revision: 1.3 $
//Rev Date: $Date: 2007/03/29 08:15:16 $

package dgu.util.swing.binding;


/**
 * An abstract adapter class for receiving binding events.  The methods in 
 * this class are empty.  This class exists as a convenience for creating 
 * listener objects.
 * 
 * Extend this class to create a BindingListener and override the methods 
 * for the events of interest.  (If you implement the BindingListener interface, 
 * you have to define all of the methods in it. This abstract class defines null 
 * methods for them all, so you can only have to define methods for events you 
 * care about.)
 *
 * Create a listener object using the extended class and then register it with a 
 * component using the component's addBindingListener method.  A binding event
 * is generated immediately before the binding changes and immediately after the 
 * binding is changed.
 *
 * @author David Underhill
 */
public abstract class BindingAdapter implements BindingListener {
    
    /**
     * Invoked when the binding is about to change
     */
    public void bindingChanging( BindingEvent e ) { }
    
    /**
     * Invoked when the binding has just finished changing
     */
    public void bindingChanged( BindingEvent e ) { }
    
    /**
     * Invoked when the binding has just finished loading
     */
    public void bindingLoaded( BindingEvent e ) { }
    
}
