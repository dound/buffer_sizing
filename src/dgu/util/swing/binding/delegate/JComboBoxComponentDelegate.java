//Filename: JComboBoxComponentDelegate.java
//Revision: $Revision: 1.10 $
//Rev Date: $Date: 2007/04/02 06:40:10 $
//Author's Note: This class is IDENTICAL to JListComponentDelegate except JComboBoxComponentDelegate is 
//               extended by JComboBox instead of JList and DefaultListModel has been changed to a 
//               DefaultComboBoxModel.  This class also adapts to differences in methods available 
//               to the DefaultComboBoxModel from the DefaultListModel.  Instances of JList in javadoc 
//               comments have also been appropriately replaced.

package dgu.util.swing.binding.delegate;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.Binding;
import dgu.util.swing.binding.IntermediateField;
import dgu.util.swing.binding.JComboBoxBound;
import dgu.util.translator.TypeTranslator;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import java.util.AbstractList;
import java.util.Vector;

//</editor-fold>


/**
 * A delegate for a JComboBox which is bound to an AbstractList.  Other related bound components may be 
 * bound to this ListBasedComponentDelegate.  Such related components would be member variables of the 
 * Objects in the AbstractList.  When a particular item in the JComboBox is selected, the relateed bound 
 * components will be refreshed with the values from the corresponding Object in the AbstractList.
 * 
 * @author David Underhill
 */
public class JComboBoxComponentDelegate extends ListBasedComponentDelegate {   
        
    //<editor-fold defaultstate="collapsed" desc="         Fields          ">
    
    /** a model to hold the names for each entry in the list */
    private DefaultComboBoxModel displayModel = new DefaultComboBoxModel();
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** 
     * Instantiates an unbound JComboBox Delegate 
     *
     * @param  owner   the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     */
    public JComboBoxComponentDelegate( BoundComponent owner, JComboBoxBound compon ) {
        super( owner, compon );
    }
    
    /** 
     * Instantiates a bound JComboBox Delegate
     *
     * @param  owner   the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JComboBoxComponentDelegate( BoundComponent owner, JComboBoxBound compon, TypeTranslator<AbstractList, AbstractList> translator, String varName ) {
        super( owner, compon, translator, varName );
        completeInit();
    }
    
    /** 
     * Instantiates a bound JComboBox Delegate
     *
     * @param  owner   the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JComboBoxComponentDelegate( BoundComponent owner, JComboBoxBound compon, TypeTranslator<AbstractList, AbstractList> translator, Object boundItem, String varName ) {
        super( owner, compon, translator, boundItem, varName );
        completeInit();
    }
    
    /** 
     * Instantiates a bound JComboBox Delegate
     *
     * @param  owner   the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JComboBoxComponentDelegate( BoundComponent owner, JComboBoxBound compon, TypeTranslator<AbstractList, AbstractList> translator, Object boundItem, String getterName, String setterName ) {
        super( owner, compon, translator, boundItem, getterName, setterName );
        completeInit();
    }
    
    /** 
     * Instantiates a bound JComboBox Delegate
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
    public JComboBoxComponentDelegate( BoundComponent owner, JComboBoxBound compon, TypeTranslator<AbstractList, AbstractList> translator, Object boundItem, String getterName, String setterName, int indexAt ) {
        super( owner, compon, translator, boundItem, getterName, setterName, indexAt );
        completeInit();
    }
    
    /** adds a focus listener which binds any changes in the component to the object it is bound to when focus is lost */
    protected void completeInit() {
        //set the model
        me().setModel( displayModel );
        
        //set the list box to the current value
        load();
        
        //when the selected item changes, update subcomponents shown
        me().addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateSubcomponentBindings(); //update the subcomponents AND refresh the text of the menu
            }
        });
    }
    
    //</editor-fold>

    
    //<editor-fold defaultstate="collapsed" desc="   Update the Binding    ">
    
    /** load the current contents for this component from the bound object */
    public void load() {
        if( binding() == null ) return;

        //rebind subcomponents to the current list
        updateSubcomponentBindings();

        //get the current list
        myList = binding().getValue();

        //remove extra entries from the list
        if( myList == null ) {
            displayModel.removeAllElements();
            return; //no list to build, so just quit
        } else {
            //while( displayModel.getSize() > list.size() )
            //    displayModel.removeElementAt( displayModel.getSize() - 1 );
            displayModel.removeAllElements();
        }

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
            setOrAddToModel( displayModel, numOn, title );

            //go to the next element
            numOn += 1;
        }

        setModel( displayModel );
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
    
    /** get the JComboBox this is bound to */
    private JComboBox me() { return (JComboBox)super.getMe(); }
    
    /** get the binding this is bound to */
    private Binding<AbstractList, AbstractList> binding() { 
        return super.getBinding(); 
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="    Pass-thru to JComboBox   ">
    
    /**
     * Returns the data model that holds the list of items displayed
     * by the <code>JComboBox</code> component.
     *
     * @return the <code>ListModel</code> that provides the displayed
     *				list of items
     */
    public final javax.swing.ListModel getModel() {
        return me().getModel();
    }
    
    /**
     * Sets the model that represents the contents or "value" of the
     * list, notifies property change listeners, and then clears the
     * list's selection.
     *
     * @param model  the <code>ListModel</code> that provides the
     *						list of items for display
     */
    public void setModel( javax.swing.ListModel model ) {
        DefaultComboBoxModel cboModel;
        if( model instanceof DefaultComboBoxModel )
            cboModel = (DefaultComboBoxModel)model;
        else {
            //turn it into a DefaultComboBoxModel
            cboModel = new DefaultComboBoxModel();
            for( int i=0; i<model.getSize(); i++ )
                cboModel.addElement( model.getElementAt( i ) );
        }
        
        me().setModel( cboModel );
    }
    
    /**
     * Returns the smallest selected cell index; <i>the selection</i> when only
     * a single item is selected in the list. When multiple items are selected,
     * it is simply the smallest selected index. Returns {@code -1} if there is
     * no selection.
     */
    public final int getSelectedIndex() {
        return me().getSelectedIndex();
    }
    
    /**
     * Selects a single cell. Does nothing if the given index is greater
     * than or equal to the model size. This is a convenience method that uses
     * {@code setSelectionInterval} on the selection model. Refer to the
     * documentation for the selection model class being used for details on
     * how values less than {@code 0} are handled.
     */
    public final void setSelectedIndex(int index) {
        me().setSelectedIndex( index );
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="  BoundDelegateContainer Methods ">
    
    /** sets the specified element in the model to the specified text */
    protected final void setOrAddToModel( DefaultComboBoxModel model, int index, String text ) {
        boolean resetSelection = false;
        if( model.getSize() > index ) { //remove and re-insert the element ... can't just change it
            if( index == getSelectedIndex() ) resetSelection = true; //reselect the item
            model.removeElementAt( index );
        }
        
        model.insertElementAt( text, index );
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
                setOrAddToModel( displayModel, indexOfValue, updateItemText(indexOfValue) );
            } catch( Exception e ) {
                //the binding is invalid or something like that; leave the entry text in the list model as is
            }
        }
    }
    
    /** selects the item at the specified index (if the index does not exist, this is ignored) */
    public final void selectItem( int index ) {
        if( index >= 0 && index < displayModel.getSize() )
            setSelectedIndex( index );
    }
    
    //</editor-fold>
    
}
