//Filename: JSliderBound.java
//Revision: $Revision: 1.0 $
//Rev Date: $Date: 2008/06/05 $

package dgu.util.swing.binding;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.delegate.BoundComponent;
import dgu.util.swing.binding.delegate.JSliderDelegate;
import dgu.util.translator.TypeTranslator;
import dgu.util.translator.SelfTranslator;
import javax.swing.JSlider;

//</editor-fold>


/**
 * Describes a checbox field which holds a value of type FROM as a Integer
 * @author David Underhill
 */
public class JSliderBound<FROM> extends JSlider implements BoundComponent {
    
    //<editor-fold defaultstate="collapsed" desc="         Fields          ">
    
    private final JSliderDelegate<FROM> delegate;
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** Instantiates an unbound JSliderBound */
    public JSliderBound() {
        super();
        delegate = new JSliderDelegate<FROM>( this, this );
    }
    
    /** 
     * Instantiates an unbound JSlider
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JSliderBound( TypeTranslator<FROM, Integer> translator, String varName ) {
        super();
        delegate = new JSliderDelegate<FROM>( this, this, translator, varName );
    }
    
    /** 
     * Instantiates a bound JSlider
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JSliderBound( TypeTranslator<FROM, Integer> translator, Object boundItem, String varName ) {
        super();
        delegate = new JSliderDelegate<FROM>( this, this, translator, boundItem, varName );
    }
    
    /** 
     * Instantiates an unbound JSlider
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JSliderBound( TypeTranslator<FROM, Integer> translator, String getterName, String setterName ) {
        this( translator, null, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JSlider
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JSliderBound( TypeTranslator<FROM, Integer> translator, Object boundItem, String getterName, String setterName ) {
        super();
        delegate = new JSliderDelegate<FROM>( this, this, translator, boundItem, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JSlider 
     *
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public JSliderBound( TypeTranslator<FROM, Integer> translator, Object boundItem, String getterName, String setterName, int indexAt ) {
        super();
        delegate = new JSliderDelegate<FROM>( this, this, translator, boundItem, getterName, setterName, indexAt );
    }
    
    //</editor-fold>
            
    
    //<editor-fold defaultstate="collapsed" desc="Integer Assumed Constrtrs">
        
    /** 
     * Instantiates an unbound JSlider; generic type MUST be Integer
     *
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JSliderBound( String varName ) {
        super();
        delegate = (JSliderDelegate<FROM>)new JSliderDelegate<Integer>( 
                this, this, new SelfTranslator<Integer>(), varName );
    }
    
    /** 
     * Instantiates a bound JSlider; generic type MUST be Integer
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JSliderBound( Object boundItem, String varName ) {
        super();
        delegate = (JSliderDelegate<FROM>)new JSliderDelegate<Integer>( 
                this, this, new SelfTranslator<Integer>(), boundItem, varName );
    }
    
    /** 
     * Instantiates an unbound JSlider; generic type MUST be Integer
     *
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JSliderBound( String getterName, String setterName ) {
        super();
        delegate = (JSliderDelegate<FROM>)new JSliderDelegate<Integer>( 
                this, this, new SelfTranslator<Integer>(), null, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JSlider; generic type MUST be Integer
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JSliderBound( Object boundItem, String getterName, String setterName ) {
        super();
        delegate = (JSliderDelegate<FROM>)new JSliderDelegate<Integer>( 
                this, this, new SelfTranslator<Integer>(), boundItem, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JSlider; generic type MUST be Integer
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public JSliderBound( Object boundItem, String getterName, String setterName, int indexAt ) {
        super();
        delegate = (JSliderDelegate<FROM>)new JSliderDelegate<Integer>( 
                this, this, new SelfTranslator<Integer>(), boundItem, getterName, setterName, indexAt );
    }
    
    //</editor-fold>
      
        
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
    
    /** gets the delete which controls the binding */
    public JSliderDelegate<FROM> getBindingDelegate() {
        return delegate;
    }
    
    //</editor-fold>
    
    public void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);       
        this.setBorder(null);
        
        if( dgu.bufsizing.DemoGUI.me != null )
            dgu.bufsizing.DemoGUI.me.sliderCallback(this, g);
    }
}
