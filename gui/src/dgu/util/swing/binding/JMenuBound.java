//Filename: JMenuBound.java
//Revision: $Revision: 1.2 $
//Rev Date: $Date: 2007/02/22 02:41:00 $

package dgu.util.swing.binding;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.delegate.BoundContainer;
import dgu.util.swing.binding.delegate.JMenuComponentDelegate;
import dgu.util.translator.SelfTranslator;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import java.util.AbstractList;
import java.util.Vector;

//</editor-fold>


/**
 * Describes a menu functioning as a list which holds a value of 
 * type T as a String displayed in a JRadioButtonMenuItem and may
 * may signal other bound components to update their bindings based 
 * on which menu is selected (much like JListBound which updates 
 * bindings based on which item in the list is selected).
 *
 * @author David Underhill
 */
public class JMenuBound extends JMenu implements BoundContainer {
     
    //<editor-fold defaultstate="collapsed" desc="         Fields          ">
    
    private final JMenuComponentDelegate delegate;
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** Instantiates an unbound JMenuBound */
    public JMenuBound() {
        super();
        delegate = new JMenuComponentDelegate( this, this );
    }
    
    /** 
     * Instantiates an unbound JMenu
     *
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JMenuBound( String varName ) {
        super();
        delegate = new JMenuComponentDelegate( this, this, new SelfTranslator<AbstractList>(), varName );
    }
    
    /** 
     * Instantiates a bound JMenu
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JMenuBound( Object boundItem, String varName ) {
        super();
        delegate = new JMenuComponentDelegate( this, this, new SelfTranslator<AbstractList>(), boundItem, varName );
    }
        
    /** 
     * Instantiates an unbound JMenu
     *
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JMenuBound( String getterName, String setterName ) {
        this( null, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JMenu
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JMenuBound( Object boundItem, String getterName, String setterName ) {
        super();
        delegate = new JMenuComponentDelegate( this, this, new SelfTranslator<AbstractList>(), boundItem, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JMenu 
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public JMenuBound( Object boundItem, String getterName, String setterName, int indexAt ) {
        super();
        delegate = new JMenuComponentDelegate( this, this, new SelfTranslator<AbstractList>(), boundItem, getterName, setterName, indexAt );
    }
    
    //</editor-fold>
            
        
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
    
    /**
     * Appends a menu item to the end of top of this menu. 
     *
     * @param menuItem the <code>JMenuItem</code> to be added
     * @return the <code>JMenuItem</code> which was added
     */
    public JMenuItem add(JMenuItem menuItem) {
        delegate.addMenuAtTop( menuItem );
        return menuItem;
    }
    
    /**
     * Appends a component to the end of top of this menu. 
     *
     * @param sep the <code>JMenuItem</code> to be added
     * @return the <code>JMenuItem</code> which was added
     */
    public JSeparator add(JSeparator sep) {
        delegate.addMenuAtTop( null );
        return sep;
    }
    
    /**
     * Pass-through to the parent.  Only for use by the delegate.
     *
     * @param menuItem the <code>JMenuItem</code> to be added
     */
    public void addToParent(JMenuItem menuItem) {
        super.add( menuItem );
    }
    
    /**
     * Pass-through to the parent.  Only for use by the delegate.
     */
    public void addSeperatorToParent() {
        super.add( new JSeparator() );
    }
    
    /** gets the delete which controls the binding */
    public JMenuComponentDelegate getBindingDelegate() {
        return delegate;
    }
        
    //</editor-fold>
    
}
