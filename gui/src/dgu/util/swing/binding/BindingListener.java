//Filename: BindingListener.java
//Revision: $Revision: 1.3 $
//Rev Date: $Date: 2007/03/29 08:15:16 $

package dgu.util.swing.binding;


/**
 * The listener interface for receiving binding events.  The class that
 * is interesting in processing a binding change event either implements 
 * this interface (and all the methods it contains) or extends the abstract
 * BindingAdapter class (overriding only methods of interest).
 *
 *<p>The listener object created from that class is then registered with a 
 * component using the component's addBindingListener method.  A binding 
 * event is generated immediately before the binding changes, immediately 
 * after the binding is changed, and immediately after the component's loaded
 * from the changed binding.
 *
 * @author David Underhill
 */
public interface BindingListener {

    /**
     * Invoked when the binding is about to change.  See the class description 
     * for {@link BindingEvent} for a definition of binding event.
     */
    public void bindingChanging( BindingEvent e );
    
    /**
     * Invoked when the binding has just finished changing but before it is loaded.  
     * See the class description 
     * for {@link BindingEvent} for a definition of binding event.
     */
    public void bindingChanged( BindingEvent e );
    
    /**
     * Invoked when the binding has finished changing and been loaded.  See the class description 
     * for {@link BindingEvent} for a definition of binding event.
     */
    public void bindingLoaded( BindingEvent e );
    
}
