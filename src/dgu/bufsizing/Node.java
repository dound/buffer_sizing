package dgu.bufsizing;

import java.awt.Graphics2D;

/**
 * Informaiton about a node.
 * @author David Underhill
 */
public abstract class Node implements Drawable {
    private String name;
    private int x;
    private int y;
    private int width;
    private int height;
    
    public Node( String name ) {
        this.name = name;
    }
    
    public abstract void draw( Graphics2D gfx );
    
    public abstract String getTypeString();

    public String getName() {
        return name;
    }
    
    public void setName( String name ) {
        this.name = name;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String toString() {
        return name + " (" + getTypeString() + ")";
    }
}
