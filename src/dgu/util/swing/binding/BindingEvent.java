//Filename: BindingEvent.java
//Revision: $Revision: 1.3 $
//Rev Date: $Date: 2007/02/25 05:03:16 $

package dgu.util.swing.binding;

import dgu.util.swing.binding.delegate.BoundComponent;


/**
 * An event which indicates that a Binding is changing.  The Binding  
 * changed or being changed is provided.
 *
 * @author David Underhill
 */
public class BindingEvent extends java.awt.AWTEvent {
    
    public final BoundComponent component;
    
    /** 
     * instantiate a BindingEvent with the Binding it is alerting on
     *
     * @param component  the component whose binding is affected (a JLabelBound, etc.)
     * @param binding    the binding 
     */
    public BindingEvent( final BoundComponent component,  final Binding binding ) {
        super( binding, -1 ); //id = -1 (no meaning for us)
        this.component = component;
    }
    
    /** returns the component involved in this BindingEvent */
    public BoundComponent getBoundComponent() {
        return component;
    }
    
    /** returns the binding involved in this BindingEvent */
    public Binding getBinding() {
        return (super.source instanceof Binding) ? (Binding)super.source : null;
    }
}
