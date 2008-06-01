//Filename: JMenuComponentDelegate.java
//Revision: $Revision: 1.10 $
//Rev Date: $Date: 2007/04/02 06:40:10 $
//Author's Note: This class is a mix between ButtonGroupDelegate and JComboBoxComponentDelegate.

package dgu.util.swing.binding.delegate;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.Binding;
import dgu.util.swing.binding.IntermediateField;
import dgu.util.swing.binding.JMenuBound;
import dgu.util.translator.TypeTranslator;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import java.util.AbstractList;
import java.util.Vector;

//</editor-fold>


/**
 * A delegate for a JMenu which is bound to an AbstractList.  Other related bound components may be 
 * bound to this ListBasedComponentDelegate.  Such related components would be member variables of the 
 * Objects in the AbstractList.  When a particular submenu in the JMenu is selected, the relateed bound 
 * components will be refreshed with the values from the corresponding Object in the AbstractList.
 * 
 * @author David Underhill
 */
public class JMenuComponentDelegate extends ListBasedComponentDelegate {  
    
    //<editor-fold defaultstate="collapsed" desc="         Fields          ">
    
    private final Vector<JMenuItem> menusAtTop = new Vector<JMenuItem>();
    private final Vector<JMenuItem> menusAtBottom = new Vector<JMenuItem>();
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** 
     * Instantiates an unbound JMenu Delegate 
     *
     * @param  owner   the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     */
    public JMenuComponentDelegate( BoundComponent owner, JMenuBound compon ) {
        super( owner, compon );
    }
    
    /** 
     * Instantiates a bound JMenu Delegate
     *
     * @param  owner   the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JMenuComponentDelegate( BoundComponent owner, JMenuBound compon, TypeTranslator<AbstractList, AbstractList> translator, String varName ) {
        super( owner, compon, translator, varName );
        completeInit();
    }
    
    /** 
     * Instantiates a bound JMenu Delegate
     *
     * @param  owner   the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JMenuComponentDelegate( BoundComponent owner, JMenuBound compon, TypeTranslator<AbstractList, AbstractList> translator, Object boundItem, String varName ) {
        super( owner, compon, translator, boundItem, varName );
        completeInit();
    }
    
    /** 
     * Instantiates a bound JMenu Delegate
     *
     * @param  owner   the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JMenuComponentDelegate( BoundComponent owner, JMenuBound compon, TypeTranslator<AbstractList, AbstractList> translator, Object boundItem, String getterName, String setterName ) {
        super( owner, compon, translator, boundItem, getterName, setterName );
        completeInit();
    }
    
    /** 
     * Instantiates a bound JMenu Delegate
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
    public JMenuComponentDelegate( BoundComponent owner, JMenuBound compon, TypeTranslator<AbstractList, AbstractList> translator, Object boundItem, String getterName, String setterName, int indexAt ) {
        super( owner, compon, translator, boundItem, getterName, setterName, indexAt );
        completeInit();
    }
    
    /** adds a focus listener which binds any changes in the component to the object it is bound to when focus is lost */
    protected void completeInit() {
        //create the submenus
        load();
    }
    
    //</editor-fold>

    
    //<editor-fold defaultstate="collapsed" desc="   Update the Binding    ">
    
    /** load the current contents for this component from the bound object */
    public void load() {
        if( binding() == null ) return;

        //get the current list
        myList = binding().getValue();

        //remove extra entries from the list
        if( myList == null ) {
            me().removeAll();
            return; //no list to build, so just quit
        } else {
            //while( displayModel.getSize() > list.size() )
            //    displayModel.removeElementAt( displayModel.getSize() - 1 );
            me().removeAll();
        }

        //add to the top
        for( JMenuItem item : this.menusAtTop ) {
            if( item == null )
                ((JMenuBound)me()).addSeperatorToParent();
            else
                ((JMenuBound)me()).addToParent( item );
        }

        //rebind subcomponents to the current list
        updateSubcomponentBindings();

        //track which item in the list we're currently adding to the display model
        int numOn = 0; 

        //loop over each item in this list and get its title
        for( Object item : myList ) {
            //get the string value of the primary component if there is one
            String title = updateItemText( numOn );

            //if there was no title for the primary component (or there is no prim compon) then just make a numeric title
            if( title == null )
                title = "Item " + Integer.toString( numOn );

            //set the title
            setOrAddToModel( me(), numOn, title );

            //go to the next element
            numOn += 1;
        }

        //add to the bottom
        for( JMenuItem item : this.menusAtBottom ) {
            if( item == null )
                ((JMenuBound)me()).addSeperatorToParent();
            else
                ((JMenuBound)me()).addToParent( item );
        }
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
    
    /** get the JMenu this is bound to */
    private JMenu me() { return (JMenu)super.getMe(); }
    
    /** get the binding this is bound to */
    private Binding<AbstractList, AbstractList> binding() { 
        return super.getBinding(); 
    }
    
    
    /**
     * Adds a JMenuItem to the top of this menu
     *
     * @param  ti  the JMenuItem to add to this JMenu
     */
    public void addMenuAtTop( JMenuItem ti ) {
        this.menusAtTop.add( ti );
        load();
    }

    /**
     * Removes the JMenuItem from the top of this menu
     *
     * @param  index  remove the MenuAtTop at the specified index.
     *
     * @throws ArrayIndexOutOfBoundsException if the index was invalid.
     */
    public void removeMenuAtTop( int index ) throws ArrayIndexOutOfBoundsException {
        this.menusAtTop.remove( index );
        load();
    }

    /**
     * Gets the specified JMenuItem from the top of this menu
     *
     * @return the JMenuItem at the specified index in the top of this menu
     *
     * @throws ArrayIndexOutOfBoundsException if the index was invalid.
     */
    public JMenuItem getMenuAtTop( int index ) throws ArrayIndexOutOfBoundsException {
        return menusAtTop.get( index );
    }
    
    /** gets the JMenuItems being added at the top of the JMenu */
    public Vector<JMenuItem> getMenusAtTop() {
        return menusAtTop;
    }
    
    /**
     * Adds a JMenuItem to the bottom of this menu
     *
     * @param  ti  the JMenuItem to add to this JMenu
     */
    public void addMenuAtBottom( JMenuItem ti ) {
        this.menusAtBottom.add( ti );
        load();
    }

    /**
     * Removes the JMenuItem from the bottom of this menu
     *
     * @param  index  remove the MenuAtBottom at the specified index.
     *
     * @throws ArrayIndexOutOfBoundsException if the index was invalid.
     */
    public void removeMenuAtBottom( int index ) throws ArrayIndexOutOfBoundsException {
        this.menusAtBottom.remove( index );
        load();
    }

    /**
     * Gets the specified JMenuItem from the bottom of this menu
     *
     * @return the JMenuItem at the specified index in the bottom of this menu
     *
     * @throws ArrayIndexOutOfBoundsException if the index was invalid.
     */
    public JMenuItem getMenuAtBottom( int index ) throws ArrayIndexOutOfBoundsException {
        return menusAtBottom.get( index );
    }
    
    /** gets the JMenuItems being added at the bottom of the JMenu */
    public Vector<JMenuItem> getMenusAtBottom() {
        return menusAtBottom;
    }
    
    //</editor-fold>
           
    
    //<editor-fold defaultstate="collapsed" desc="  BoundDelegateContainer Methods ">
    
    /**
     * Returns the index of the menu item which is selected, or -1 if there is no selected item.
     */
    public int getSelectedIndex() {
        for( int i=0; i<size(); i++ ) {
            if( getItem(i).isSelected() ) 
                return i;
        }
        
        return -1;
    }
    
    private boolean inSetSelectedIndex = false;
    /**
     * Sets a single menu item as selected (deselects any other menu items)
     */
    public void setSelectedIndex(int index) {
        for( int i=0; i<size(); i++ ) {
           getItem(i).setSelected( i==index );
        }
        
        //reflect the newly select menu
        if( !inSetSelectedIndex ) {
            inSetSelectedIndex = true;
            updateSubcomponentBindings(); //update the subcomponents AND refresh the text of the menu
            inSetSelectedIndex = false;
        }        
    }
    
    /**
     * gets the item at the selected index (other than menus above/below) in this container
     */
    private final JMenuItem getItem( int index ) {
        return me().getItem( index + this.menusAtTop.size() );
    }
    
    /**
     * inserts the item at the selected index (other than menus above/below) in this container
     */
    private final void insert( int index, JMenuItem newItem ) {
        me().insert( newItem, index + this.menusAtTop.size() );
    }
    
    /**
     * removes the item at the selected index (other than menus above/below) in this container
     */
    private final void remove( int index ) {
        me().remove( index + this.menusAtTop.size() );
    }
    
    /** removes the specified item from the list (do nothing if the index is invalid) */
    public void removeItem( int index ) {
        super.removeItem( index );
        load(); //refresh again
        
        //load the new item at this index, if it exists, the previous index otherwise, or none if there are no items left
        if( myList.size() > 0 ) {
            if( myList.size() > index  )
                setSelectedIndex( index ); //select the new item at this index
            else
                setSelectedIndex( index - 1 ); //select the previous index if the old index was invalid
        }
    }
    
    /**
     * Returns the number of items (other than menus above/below) in this container
     */
    private final int size() {
        int a = me().getItemCount();
        int b = this.menusAtBottom.size();
        int c = this.menusAtTop.size();
        return a - b - c;
    }
    
    /** sets the specified element in the model to the specified text */
    protected final void setOrAddToModel( final JMenu menu, final int index, final String text ) {
        boolean resetSelection = false;
        if( size() > index ) { //remove and re-insert the element ... can't just change it
            if( index == getSelectedIndex() ) resetSelection = true; //reselect the item
            remove( index );
        }
        
        //create the new menu and add a listener to set the clicked menu as the selected menu
        final JRadioButtonMenuItem newItem = new JRadioButtonMenuItem(text);
        newItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setSelectedIndex( index ); 
                updateSubcomponentBindings();
            }
        });
        insert( index, newItem );
        if( resetSelection ) selectItem( index );
    }
    
    /** notify the parent of a bound component change in value */
    public final void notifyBoundComponentChange( BoundComponent c ) {
        //get the index of the component in the list of subcomponents
        int indexOfCompon = components.indexOf( c );
        
        //if the component is the primary component, update the list text
        if( indexOfCompon == primaryComponentIndex ) { 
            try {
                //if there is no list, there is nothing to update
                if( myList == null ) return;
                
                //determine what entry in the list needs to be updated
                int indexOfValue;
                if( c.getBindingDelegate().getBinding().getIndexAt() >= 0 ) //if the item is bound to this list
                    indexOfValue = c.getBindingDelegate().getBinding().getIndexAt(); //then get the index is was modifying
                else
                    //otherwise, get the index the bound object in ourl ist of objects
                    indexOfValue = myList.indexOf( c.getBindingDelegate().getBinding().getBoundItem() );
                
                //update the list
                setOrAddToModel( me(), indexOfValue, updateItemText(indexOfValue) );
                
            } catch( Exception e ) {
                //the binding is invalid or something like that; leave the entry text in the list model as is
            }
        }
    }
    
    /** selects the item at the specified index (if the index does not exist, this is ignored) */
    public final void selectItem( int index ) {
        if( index >= 0 && index < size() )
            setSelectedIndex( index );
    }
    
    //</editor-fold>
    
}
