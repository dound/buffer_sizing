//Filename: BoundContainer.java
//Revision: $Revision: 1.1 $
//Rev Date: $Date: 2007/02/20 15:25:23 $

package dgu.util.swing.binding.delegate;


/**
 * This interface specifies an accessor to a BoundDelegateContainer which is an 
 * extension of a normal BoundComponent in that it may contain other BoundComponents.
 *
 * @author David Underhill
 */
public interface BoundContainer extends BoundComponent {
    
    /** gets the delete which controls the binding */
    public BoundDelegateContainer getBindingDelegate();
    
}
