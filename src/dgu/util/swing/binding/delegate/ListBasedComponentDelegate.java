//Filename: ListBasedComponentDelegate.java
//Revision: $Revision: 1.16 $
//Rev Date: $Date: 2007/04/02 06:40:10 $

package dgu.util.swing.binding.delegate;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.Binding;
import dgu.util.translator.TypeTranslator;
import java.util.AbstractList;
import java.util.Vector;

//</editor-fold>


/**
 * A delegate for a JList, JComboBox, or other type which is bound to an AbstractList.  Other related 
 * bound components may be bound to this ListBasedComponentDelegate.  Such related components would be 
 * member variables of the Objects in the AbstractList.  When a particular item in the JList is selected, 
 * the relateed bound components will be refreshed with the values from the corresponding Object in the 
 * AbstractList.
 * 
 * @author David Underhill
 */
public abstract class ListBasedComponentDelegate extends GenericJComponentDelegate<AbstractList, AbstractList> implements BoundDelegateContainer {
        
    //<editor-fold defaultstate="collapsed" desc="         Fields          ">
    
    /** components whose bindings are contained within the objects contained in this list */
    protected Vector<BoundComponent> components = new Vector<BoundComponent>();
    
    /** the index of the subcomponent which determines the names of the 
     *  entries in the list (-1 [default] => number them instead)       */
    protected int primaryComponentIndex = -1;
    
    protected AbstractList myList = null;
    
    private boolean removing = false;
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** 
     * Instantiates an unbound JList Delegate 
     *
     * @param  owner   the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     */
    public ListBasedComponentDelegate( BoundComponent owner, BoundComponent compon ) {
        super( owner, compon );
    }
    
    /** 
     * Instantiates a bound JList Delegate
     *
     * @param  owner   the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public ListBasedComponentDelegate( BoundComponent owner, BoundComponent compon, TypeTranslator<AbstractList, AbstractList> translator, String varName ) {
        super( true, owner, compon, translator, varName );
    }
    
    /** 
     * Instantiates a bound JList Delegate
     *
     * @param  owner   the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public ListBasedComponentDelegate( BoundComponent owner, BoundComponent compon, TypeTranslator<AbstractList, AbstractList> translator, Object boundItem, String varName ) {
        super( true, owner, compon, translator, boundItem, varName );
    }
    
    /** 
     * Instantiates a bound JList Delegate
     *
     * @param  owner   the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public ListBasedComponentDelegate( BoundComponent owner, BoundComponent compon, TypeTranslator<AbstractList, AbstractList> translator, Object boundItem, String getterName, String setterName ) {
        super( true, owner, compon, translator, boundItem, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JList Delegate
     *
     * @param  owner   the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public ListBasedComponentDelegate( BoundComponent owner, BoundComponent compon, TypeTranslator<AbstractList, AbstractList> translator, Object boundItem, String getterName, String setterName, int indexAt ) {
        super( true, owner, compon, translator, boundItem, getterName, setterName, indexAt );
    }
    
    //</editor-fold>

    
    //<editor-fold defaultstate="collapsed" desc="   Update the Binding    ">

    /** tells the boundObject about changes to the AbstractList */
    public final void save() {
        if( binding() == null ) return;
        binding().setValue( myList );
    }
    
    protected String updateItemText( int numOn ) {
        Object item = myList.get( numOn );
        
        BoundComponent primaryComponent = null;
            if( primaryComponentIndex >= 0 )
                primaryComponent = components.get(primaryComponentIndex);
        
        String title = null;
        if( primaryComponent != null ) {
            title = primaryComponent.getBindingDelegate().getBinding().bindToNewObject( item ).getValue().toString();
        } else if( primaryComponentIndex == -2 ) {
            try {
                Object o = myList.get( numOn );
                if( o != null ) title = o.toString();
            } catch( Exception e ) { }
        }
        return title;
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
    
    /** get the binding this is bound to */
    private Binding<AbstractList, AbstractList> binding() { 
        return super.getBinding(); 
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="    Pass-thru to JList   ">
    
    /**
     * Returns the smallest selected cell index; <i>the selection</i> when only
     * a single item is selected in the list. When multiple items are selected,
     * it is simply the smallest selected index. Returns {@code -1} if there is
     * no selection.
     */
    public abstract int getSelectedIndex();
    
    /**
     * Selects a single cell. Does nothing if the given index is greater
     * than or equal to the model size. This is a convenience method that uses
     * {@code setSelectionInterval} on the selection model. Refer to the
     * documentation for the selection model class being used for details on
     * how values less than {@code 0} are handled.
     */
    public abstract void setSelectedIndex(int index);
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="  BoundDelegateContainer Methods ">
    
    /** adds a new item to the list */
    public final void addItem( Object o ) {
        //get the current list
        if( myList == null ) return;
        
        //add an item to the list
        myList.add( o );
        
        //refresh the list
        load();
        
        //select the new value
        setSelectedIndex( myList.size() - 1 );
    }
    
    /** moves the selected item down one (if it is the last item, this does nothing) */
    public final void moveSelectedItemDown() {
        moveItemDown( getSelectedIndex() );
    }
    
    /** moves the specified item down one (if the index is invalid or is the last item, this does nothing) */
    public final void moveItemDown( int index ) {
        swapItems( index, index + 1, index + 1 );
    }
    
    /** moves the selected item up one (if it is the first item, this does nothing) */
    public final void moveSelectedItemUp() {
        moveItemUp( getSelectedIndex() );
    }
    
    /** moves the specified item up one (if the index is invalid or is the first item, this does nothing) */
    public final void moveItemUp( int index ) {
        swapItems( index, index - 1, index - 1 );
    }
    
    /** swaps two items in the list (if either index is invalid this returns false and does nothing) */
    public final boolean swapItems( int index1, int index2 ) {
        //get the current list
        if( myList == null ) return false;
        
        if( index1 >= 0 && index1 < myList.size() &&
            index2 >= 0 && index2 < myList.size() &&
            index1 != index2 ) {
                
            Object tmp = myList.get( index1 );
            myList.set( index1, myList.get(index2) );
            myList.set( index2, tmp );
            
            return true;
        }
        
        return false;
    }
    
    /** swaps two items in the list (if either index is invalid, this does nothing) and selects the specified item */
    public final void swapItems( int index1, int index2, int selectIndex ) {
        if( swapItems( index1, index2 ) ) {
            load();
            selectItem( selectIndex );
        }
    }
    
    /** removes the currently selected item from the list (do nothing if nothing is selected) */
    public final void removeSelectedItem() {
        removeItem( getSelectedIndex() );
    }
    
    /** removes the specified item from the list (do nothing if the index is invalid) */
    public void removeItem( int index ) {
        //get the current list
        if( myList == null ) return;
        
        //remove the item at the current index if it is a valid index
        if( index >= 0 && index < myList.size() ) myList.remove( index );
        
        //refresh the list
        removing = true;
        load();
        
        //load the new item at this index, if it exists, the previous index otherwise, or none if there are no items left
        if( myList.size() > 0 ) {
            if( myList.size() > index  )
                setSelectedIndex( index ); //select the new item at this index
            else
                setSelectedIndex( index - 1 ); //select the previous index if the old index was invalid
        }
        
        removing = false;
    }
    
    /** add bound component to this (b's parent will be set to this) */
    public final void addBoundComponent( BoundComponent b ) {
        components.add( b );
        b.getBindingDelegate().setBoundParent( this );
    }
    
    /** get bound components this contains */
    public final Vector<BoundComponent> getBoundComponents() {
        return components;
    }
    
    /** 
     * gets the index of the component which whose string value is used as the value to 
     * represent that collection, or -1 to just number the sets 
     */
    public final int getPrimaryComponent() {
        return primaryComponentIndex;
    }
    
    /** 
     * sets the index of the component which whose string value is used as the value to 
     * represent that collection, or -1 to just number the sets
     * 
     * @param primComponIndex  index of the BoundComponent which is the primary component in the 
     *                         getBoundComponents() vector.
     */
    public final void setPrimaryComponent( int primComponIndex ) {
        this.primaryComponentIndex = primComponIndex;
    }
    
    /** update all components within this component */
    public final void updateSubcomponentBindings() {
        
        //get the index we're on
        int index = getSelectedIndex();
        
        //get the item we're on
        Object objectOn;
        if( myList != null && index >= 0 && index < myList.size() ) //check to see if the index is valid
            objectOn = myList.get( index );
        else
            objectOn = null; //if index isn't valid, then bind everything to null
        
        //bind each component to the current selected item in the list
        for( BoundComponent b : components ) {
            BoundDelegateComponent d = b.getBindingDelegate();
            
            if( d.getBinding().getIndexAt() >= 0 ) {
                if( index < 0 )
                    d.changeBinding( myList, 0 ); //bind to index 0 (which will not exist, but 
                else {                             // if we bind to -1 we won't get bound to the list next time)
                    
                    //if the index is the same, then load the values NOW at the index because they
                    // may have changed => this occurs when an item is removed.  Though the index 
                    // is unchanged, the data HAS changed because the item at this index is new.
                    // In this case, the binding will just be rebound to the same thing, but allow 
                    // this so that any listeners are notified of the change (really a rebinding here).
                    if( index == d.getBinding().getIndexAt() ) {
                        //if the LAST item has been deleted, back up to the previous item, if we aren't already at the first item
                        //index will be size() if the last item has been deleted (size() has already decreased by 1!
                        if( removing && index==myList.size() ) {
                            index -= 1;
                            d.changeBinding( myList, index );
                        }
                        
                        if( index < myList.size() )
                            d.load();
                    }
                    d.changeBinding( myList, index ); //bind to an index in the list!
                }
            } else
                d.changeBinding( objectOn, -1 ); //bind to the object itself
            
            //if the item is a container, select the first item in it
            if( d.isContainer() ) 
                ((BoundContainer)b).getBindingDelegate().selectItem( 0 );
        }
    }
    
    //</editor-fold>
    
}
