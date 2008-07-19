//Filename: JListBound.java
//Revision: $Revision: 1.3 $
//Rev Date: $Date: 2007/02/22 15:28:41 $

package dgu.util.swing.binding;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.delegate.BoundContainer;
import dgu.util.swing.binding.delegate.JListComponentDelegate;
import dgu.util.translator.SelfTranslator;
import javax.swing.JList;
import java.util.AbstractList;

//</editor-fold>


/**
 * Describes a list which holds a value of type T as a String and may
 * may signal other bound components to update their bindings. 
 *
 * @author David Underhill
 */
public class JListBound extends JList implements BoundContainer {
     
    //<editor-fold defaultstate="collapsed" desc="         Fields          ">
    
    private final JListComponentDelegate delegate;
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** Instantiates an unbound JListBound */
    public JListBound() {
        super();
        delegate = new JListComponentDelegate( this, this );
    }
    
    /** 
     * Instantiates an unbound JList
     *
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JListBound( String varName ) {
        super();
        delegate = new JListComponentDelegate( this, this, new SelfTranslator<AbstractList>(), varName );
    }
    
    /** 
     * Instantiates a bound JList
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JListBound( Object boundItem, String varName ) {
        super();
        delegate = new JListComponentDelegate( this, this, new SelfTranslator<AbstractList>(), boundItem, varName );
    }
        
    /** 
     * Instantiates an unbound JList
     *
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JListBound( String getterName, String setterName ) {
        this( null, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JList
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JListBound( Object boundItem, String getterName, String setterName ) {
        super();
        delegate = new JListComponentDelegate( this, this, new SelfTranslator<AbstractList>(), boundItem, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JList 
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public JListBound( Object boundItem, String getterName, String setterName, int indexAt ) {
        super();
        delegate = new JListComponentDelegate( this, this, new SelfTranslator<AbstractList>(), boundItem, getterName, setterName, indexAt );
    }
    
    //</editor-fold>
            
        
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
    
    /** gets the delete which controls the binding */
    public JListComponentDelegate getBindingDelegate() {
        return delegate;
    }
    
    //</editor-fold>
    
}
