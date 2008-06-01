//Filename: JLabelBound.java
//Revision: $Revision: 1.1 $
//Rev Date: $Date: 2007/02/20 15:25:22 $

package dgu.util.swing.binding;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.delegate.BoundComponent;
import dgu.util.swing.binding.delegate.JLabelComponentDelegate;
import dgu.util.translator.SelfTranslator;
import dgu.util.translator.TypeTranslator;
import javax.swing.JLabel;

//</editor-fold>


/**
 * Describes a text field which holds a value of type T as a String
 * @author David Underhill
 */
public class JLabelBound<FROM> extends JLabel implements BoundComponent {
    
    //<editor-fold defaultstate="collapsed" desc="         Fields          ">
    
    private final JLabelComponentDelegate<FROM> delegate;
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** Instantiates an unbound JLabelBound */
    public JLabelBound() {
        super();
        delegate = new JLabelComponentDelegate<FROM>( this, this );
    }
    
    /** 
     * Instantiates an unbound JTextField
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JLabelBound( TypeTranslator<FROM, String> translator, String varName ) {
        super();
        delegate = new JLabelComponentDelegate<FROM>( this, this, translator, varName );
    }
    
    /** 
     * Instantiates a bound JTextField
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JLabelBound( TypeTranslator<FROM, String> translator, Object boundItem, String varName ) {
        super();
        delegate = new JLabelComponentDelegate<FROM>( this, this, translator, boundItem, varName );
    }
    
    /** 
     * Instantiates an unbound JTextField
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JLabelBound( TypeTranslator<FROM, String> translator, String getterName, String setterName ) {
        this( translator, null, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JTextField
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JLabelBound( TypeTranslator<FROM, String> translator, Object boundItem, String getterName, String setterName ) {
        super();
        delegate = new JLabelComponentDelegate<FROM>( this, this, translator, boundItem, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JTextField 
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public JLabelBound( TypeTranslator<FROM, String> translator, Object boundItem, String getterName, String setterName, int indexAt ) {
        super();
        delegate = new JLabelComponentDelegate<FROM>( this, this, translator, boundItem, getterName, setterName, indexAt );
    }
    
    //</editor-fold>
            
    
    //<editor-fold defaultstate="collapsed" desc="String Assumed Constrctrs">
        
    /** 
     * Instantiates an unbound JLabel; generic type MUST be String
     *
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JLabelBound( String varName ) {
        super();
        delegate = (JLabelComponentDelegate<FROM>)new JLabelComponentDelegate<String>( 
                this, this, new SelfTranslator<String>(), varName );
    }
    
    /** 
     * Instantiates a bound JLabel; generic type MUST be String
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JLabelBound( Object boundItem, String varName ) {
        super();
        delegate = (JLabelComponentDelegate<FROM>)new JLabelComponentDelegate<String>( 
                this, this, new SelfTranslator<String>(), boundItem, varName );
    }
    
    /** 
     * Instantiates a bound JLabel; generic type MUST be String
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JLabelBound( Object boundItem, String getterName, String setterName ) {
        super();
        delegate = (JLabelComponentDelegate<FROM>)new JLabelComponentDelegate<String>( 
                this, this, new SelfTranslator<String>(), boundItem, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JLabel; generic type MUST be String
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public JLabelBound( Object boundItem, String getterName, String setterName, int indexAt ) {
        super();
        delegate = (JLabelComponentDelegate<FROM>)new JLabelComponentDelegate<String>( 
                this, this, new SelfTranslator<String>(), boundItem, getterName, setterName, indexAt );
    }
    
    //</editor-fold>
    
        
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
    
    /** gets the delete which controls the binding */
    public JLabelComponentDelegate<FROM> getBindingDelegate() {
        return delegate;
    }
    
    //</editor-fold>
    
}
