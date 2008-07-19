//Filename: CustomTabOrder.java
//Revision: $Revision: 1.2 $
//Rev Date: $Date: 2007/02/13 06:08:12 $

package dgu.util.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;

/**
 * Allows users to easily generate focus traversal policies
 *
 * @author David Underhill
 */
public class CustomTabOrder extends FocusTraversalPolicy {
    private Component[] compArray;
    
    //Constructors
    /** Default constructor */
    public CustomTabOrder( final Component[] compArray ) {
        this.compArray = compArray;
    }
    
    public Component getComponentAfter(Container focusCycleRoot, Component aComponent) {
        for( int i=0; i<compArray.length-1; i++ )                                       //loop over each component (except the last)
            if( compArray[i].equals( aComponent ) )                                     //if this compon is the one supplied as an arg
                return compArray[i+1];                                                  //then return the next item in the array

        return compArray[0];                                                            //must have been on the last compon; go to 1st
    }

    public Component getComponentBefore(Container focusCycleRoot, Component aComponent) {
        for( int i=compArray.length-1; i>0; i-- )                                       //loop over each component (except the first)
            if( compArray[i].equals( aComponent ) )                                     //if this compon is the one supplied as an arg
                return compArray[i-1];                                                  //then return the previous item in the array

        return compArray[compArray.length-1];                                           //must have been on the 1st compon; go to last
    }

    public Component getDefaultComponent(Container focusCycleRoot) { return compArray[0]; }

    public Component getLastComponent(Container focusCycleRoot) { return compArray[compArray.length-1]; }

    public Component getFirstComponent(Container focusCycleRoot) { return compArray[0]; }
    
    /**
     * Creates a custom FocusTraversalPolicy for the specified container which will allow the traversal of the specified components 
     * in the given order
     * @param policyOwner the container to create the policy for
     * @param compArray array of components to create the traversal policy for
     */
    public static final void createCustomTabOrder( final Container policyOwner, final Component[] compArray ) {
        policyOwner.setFocusTraversalPolicy( new FocusTraversalPolicy() {
            public Component getComponentAfter(Container focusCycleRoot, Component aComponent) {
                for( int i=0; i<compArray.length-1; i++ )                               //loop over each component (except the last)
                    if( compArray[i].equals( aComponent ) )                             //if this compon is the one supplied as an arg
                        return compArray[i+1];                                          //then return the next item in the array
                
                return compArray[0];                                                    //must have been on the last compon; go to 1st
            }

            public Component getComponentBefore(Container focusCycleRoot, Component aComponent) {
                for( int i=compArray.length-1; i>0; i-- )                               //loop over each component (except the first)
                    if( compArray[i].equals( aComponent ) )                             //if this compon is the one supplied as an arg
                        return compArray[i-1];                                          //then return the previous item in the array
                
                return compArray[compArray.length-1];                                   //must have been on the 1st compon; go to last
            }

            public Component getDefaultComponent(Container focusCycleRoot) { return compArray[0]; }

            public Component getLastComponent(Container focusCycleRoot) { return compArray[compArray.length-1]; }

            public Component getFirstComponent(Container focusCycleRoot) { return compArray[0]; }
        } );
    }
}
