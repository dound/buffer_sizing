//Filename: BoundDelegateContainer.java
//Revision: $Revision: 1.2 $
//Rev Date: $Date: 2007/02/22 15:20:59 $

package dgu.util.swing.binding.delegate;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import java.util.Vector;

//</editor-fold>


/**
 * This interface specifies methods which extend a normal BoundComponent into one 
 * which may contain other BoundDelegateComponents.
 *
 * @author David Underhill
 */
public interface BoundDelegateContainer extends BoundDelegateComponent {
        
    /** add bound component to this (b's parent will be set to this) */
    public void addBoundComponent( BoundComponent b );
    
    /** get bound components this contains */
    public Vector<BoundComponent> getBoundComponents();
    
    /** notify the parent of a bound component change in value */
    public void notifyBoundComponentChange( BoundComponent c );

    /** selects the item at the specified index (if the index does not exist, this is ignored) */
    public void selectItem( int index );
    
    /** update all components within this component */
    public void updateSubcomponentBindings();

    
        /** 
     * gets the index of the component which whose string value is used as the value to 
     * represent that collection, or -1 to just number the sets 
     */
    public int getPrimaryComponent();
    
    /** 
     * sets the index of the component which whose string value is used as the value to 
     * represent that collection, or -1 to just number the sets
     * 
     * @param primComponIndex  index of the BoundComponent which is the primary component in the 
     *                         getBoundComponents() vector.
     */
    public void setPrimaryComponent( int primComponIndex );

}
