package dgu.bufsizing;

import java.awt.Graphics2D;

/**
 * Designates an object which may be drawn.
 * @author David Underhill
 */
public interface Drawable {
    public abstract void draw( Graphics2D gfx );
}
