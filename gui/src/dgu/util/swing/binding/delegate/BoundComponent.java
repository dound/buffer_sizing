//Filename: BoundComponent.java
//Revision: $Revision: 1.1 $
//Rev Date: $Date: 2007/02/20 15:25:23 $

package dgu.util.swing.binding.delegate;


/**
 * This interface specifies an accessor to a BoundDelegateComponent which binds the 
 * implementor to some Object.
 *
 * @author David Underhill
 */
public interface BoundComponent {
    
    /** gets the delete which controls the binding */
    public BoundDelegateComponent getBindingDelegate();
    
}
