//Filename: CustomTabOrder.java
//Revision: $Revision: 1.2 $
//Rev Date: $Date: 2007/02/13 06:08:12 $

package dgu.util.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;

/**
 * Allows users to easily generate focus traversal policies with sets of tab orderings
 *
 * @author David Underhill
 */
public class CustomTabOrderSet extends FocusTraversalPolicy {
    private java.util.Vector<Component[]> compArray;
    private Component[] lastSetOn;
    
    //Constructors
    /** Default constructor 
     * @param compArray A multidimensional array of components.  The first dimension identifies the set.  The second dimension 
     *                  identifies components in the set.  Traversing stays within the current tab set.
     */
    public CustomTabOrderSet( final java.util.Vector<Component[]> compArray ) {
        this.compArray = compArray;
    }
    
    public Component getComponentAfter(Container focusCycleRoot, Component aComponent) {
        Component ret = findNext( lastSetOn, aComponent );                              //check the set which had the compon 1st
        if( ret!= null ) return ret;                                                    //(allows diff sets to contain same compon)
        
        for( Component[] set : compArray ) {                                            //loop over each component set
            if( set != lastSetOn ) {                                                    //check if not already checked
                ret = findNext( set, aComponent );                                      //see if this has our compon
                if( ret != null ) { lastSetOn = set; return ret; }                      //if next compon found, return it
            }
        }
        
        return compArray.get(0)[0];                                                     //shouldn't get here, but a return is req.
    }
    
    public Component getComponentBefore(Container focusCycleRoot, Component aComponent) {
        Component ret = findPrev( lastSetOn, aComponent );                              //check the set which had the compon 1st
        if( ret!= null ) return ret;                                                    //(allows diff sets to contain same compon)
        
        for( Component[] set : compArray ) {                                            //loop over each component set
            if( set != lastSetOn ) {                                                    //check if not already checked
                ret = findPrev( set, aComponent );                                      //see if this has our compon
                if( ret != null ) { lastSetOn = set; return ret; }                      //if next compon found, return it
            }
        }
        
        return compArray.get(0)[0];                                                     //shouldn't get here, but a return is req.
    }

    public Component getDefaultComponent(Container focusCycleRoot) { return compArray.get(0)[0]; }

    public Component getLastComponent(Container focusCycleRoot) { 
        int lastItemInLastSet = compArray.lastElement().length-1;
        return compArray.lastElement()[lastItemInLastSet];
    }

    public Component getFirstComponent(Container focusCycleRoot) { return compArray.get(0)[0]; }

    /**
     * Looks through the given set and returns the component after c if c is found
     * @param set set of components to look in
     * @param c component to look for
     * @return component after c in the set
     */
    private Component findNext( Component[] set, Component c ) {
        if( set == null ) return null;
        
        for( int i=0; i<set.length; i++ )                                               //loop over each component in the set
            if( set[i] == c )                                                           //if this compon is the one supplied as an arg
                if( i == (set.length-1) )                                               //see if this is the last element
                    return set[0];                                                      //last: return first element in the set
                else
                    return set[i+1];                                                    //not last: return the next item in the set
        
        return null;
    }
    
    /**
     * Looks through the given set and returns the component before c if c is found
     * @param set set of components to look in
     * @param c component to look for
     * @return component before c in the set
     */
    private Component findPrev( Component[] set, Component c ) {
        if( set == null ) return null;
        
        for( int i=set.length-1; i>=0; i-- )                                            //loop over each component in the set
                if( set[i] == c )                                                       //if this compon is the one supplied as an arg
                    if( i == 0 )                                                        //see if this is the first element
                        return set[set.length-1];                                       //first: return last element in the set
                    else
                        return set[i-1];                                                //not first: return the prev item in the set
        
        return null;
    }
}
