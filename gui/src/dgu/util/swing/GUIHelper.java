package dgu.util.swing;

import dgu.util.StringOps;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Enumeration;
import javax.swing.UIManager;


/**
 * Provides some functionality to help GUIs set defaults, etc.
 * 
 * @author David Underhill
 */
public abstract class GUIHelper {

  //<editor-fold defaultstate="collapsed" desc="        Constants         ">
  
  /** default background color */
  public static final Color DEFAULT_BG_COLOR = new Color(238, 238, 238);
  
  /** default foreground color */
  public static final Color DEFAULT_FG_COLOR = Color.BLACK;
  
  /** default font */
  public static final Font DEFAULT_FONT = new Font("Tahoma", Font.PLAIN, 11);
  
  /** default font bolded */
  public static final Font DEFAULT_FONT_BOLD = new Font("Tahoma", Font.BOLD, 11);
  public static final Font DEFAULT_FONT_BOLD_BIG = new Font("Tahoma", Font.BOLD, 14);
  public static final Font DEFAULT_FONT_BOLD_XBIG = new Font("Tahoma", Font.BOLD, 18);
  
  /** smaller font size version of the default font */
  public static final Font DEFAULT_FONT_SMALL = new Font("Tahoma", Font.PLAIN, 9);
  
  //</editor-fold>
  

  //<editor-fold defaultstate="collapsed" desc=" Default Initializations ">
  
  /**
   * Sets the defaults for GUIs in this project - all components created *AFTER* this
   * is called will be created with these defaults unless specifically overridden.
   */
  public static void setGUIDefaults() {
    setUIFont(new javax.swing.plaf.FontUIResource(DEFAULT_FONT));
    UIManager.put("ScrollBar.width", Integer.valueOf(14));
    UIManager.put("Label.font", DEFAULT_FONT);
    
    try {
        UIManager.setLookAndFeel(new com.jgoodies.looks.plastic.PlasticXPLookAndFeel());
    } catch (Exception e) {}
  }

  /**
   * Sets the default font for all Swing componenets
   * 
   * @param f  the font to use
   */
  public static void setUIFont(javax.swing.plaf.FontUIResource f) {
    Enumeration keys = UIManager.getDefaults().keys();
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      Object value = UIManager.get(key);

      if (value instanceof javax.swing.plaf.FontUIResource) {
        UIManager.put(key, f);
      }
    }
  }

  //</editor-fold>
  

  //<editor-fold defaultstate="collapsed" desc="   Dialog Box Methods    ">
  
  /**
   * Displays an error message from a thrown Excpetion
   *
   * @param e  the exception whose message will be shown in a JOptionPane MessageDialog box
   */
  public static void displayError(Exception e) {
    javax.swing.JOptionPane.showMessageDialog(
            null, 
            "Error: " + StringOps.splitIntoLines(e.getMessage()), 
            "ERROR", javax.swing.JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Displays an error message
   *
   * @param msg  the message which will be shown in a JOptionPane MessageDialog box
   */
  public static void displayError(String msg) {
    javax.swing.JOptionPane.showMessageDialog(
            null, 
            "Error: " + StringOps.splitIntoLines(msg), 
            "ERROR", javax.swing.JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Displays an input dialog box with no default value set
   *
   * @param msg           the message which will be shown in a JOptionPane InputDialog box
   *
   * @return  value specified by the user
   */
  public static String getInput(String msg) {
    return getInput(msg, "");
  }

  /**
   * Displays an input dialog box
   *
   * @param msg           the message which will be shown in a JOptionPane InputDialog box
   * @param defaultValue  the default value for the InputDialog box
   *
   * @return  value specified by the user
   */
  public static String getInput(String msg, String defaultValue) {
    return javax.swing.JOptionPane.showInputDialog(StringOps.splitIntoLines(msg), defaultValue);
  }

  /**
   * Displays an confirm dialog box
   *
   * @param title  the title of the message box
   * @param msg    the message which will be shown in a JOptionPane MessageDialog box
   * @param type   what kind of JOptionPane to use
   *
   * @return  value chosen by the user
   */
  public static int confirmDialog(String title, String msg, int type) {
    return javax.swing.JOptionPane.showConfirmDialog(
            null, 
            msg, 
            title, 
            type,
            javax.swing.JOptionPane.QUESTION_MESSAGE);
  }

  /**
   * Displays a message
   *
   * @param msg  the message which will be shown in a JOptionPane MessageDialog box
   */
  public static void displayMessage(String msg) {
    javax.swing.JOptionPane.showMessageDialog(
            null, 
            StringOps.splitIntoLines(msg), 
            "Alert", 
            javax.swing.JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Displays a message
   *
   * @param title  the title of the message box
   * @param msg    the message which will be shown in a JOptionPane MessageDialog box
   */
  public static void displayMessage(String title, String msg) {
    javax.swing.JOptionPane.showMessageDialog(
            null, 
            StringOps.splitIntoLines(msg), 
            title, 
            javax.swing.JOptionPane.INFORMATION_MESSAGE);
  }
  
  //</editor-fold>
  
  /**
   * Gets an integer from the user within the specified range.
   * @param msg  The prompt to show the user.
   * @param min  The minimum allowed value.
   * @param def  The default value/
   * @param max  The maximum allowed value.
   * @return  The user-specified value.
   */
  public static int getIntFromUser( String msg, int min, int def, int max ) {
        int v;
        while( true ) {
            try {
                v = Integer.valueOf( getInput(msg, String.valueOf(def)) );
                if( v < min || v > max )
                    displayError( "Error: must be between " + min + " and " + max + "." );
                else
                    return v;
            }
            catch( NumberFormatException e ) {
                displayError(e);
            }
        }
    }

    public static final void drawCeneteredString( String s, Graphics2D gfx, int x, int y ) {
        // center the string horizontally
        x -= gfx.getFontMetrics().stringWidth( s ) / 2;
        gfx.drawString( s, x, y );
    }
}
