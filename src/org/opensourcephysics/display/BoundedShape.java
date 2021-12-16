/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;

public class BoundedShape extends InteractiveShape implements Selectable {
  
  final static int CENTER = 0;
  final static int BOTTOM = 1;
  final static int LEFT = 2;
  final static int TOP = 3;
  final static int RIGHT = 4;
  final static int CORNER = 5;
  final static int NONE = 6;
  
  int hotspot = NONE;
  int delta = 3;
  int deltaSqr = delta*delta;
  int d2 = 2*delta+1;
  boolean selected = false;
  boolean hideBounds = false;
  Color boundsColor = new Color(128, 128, 255);
  boolean widthDrag = false;
  boolean heightDrag = false;
  boolean xyDrag = true;
  boolean rotateDrag = false;
  Shape pixelBounds = new Rectangle2D.Double(0, 0, 0, 0); // bounding rectangle in pixel coordinates.
  Point2D.Double[] hotSpots = new Point2D.Double[6];
  XYDelegate xyDelegate = new XYDelegate();

  Point2D.Double pt00 = new Point2D.Double();
  /**
   * Constructs a BoundedShape object for the given shape.
   *
   * @param s Shape
   * @param x double
   * @param y double
   */
  public BoundedShape(Shape s, double x, double y) {
    super(s, x, y);
    for(int i = 0, n = hotSpots.length; i<n; i++) {
      hotSpots[i] = pt00;//new Point2D.Float(0, 0);
    }
  }

  /**
   * Creates a bounded rectangle.
   * @param x
   * @param y
   * @param w
   * @param h
   * @return the interactive rectangle
   */
  public static BoundedShape createBoundedRectangle(double x, double y, double w, double h) {
    Shape shape = new Rectangle2D.Double(-w/2, -h/2, w, h);
    return new BoundedShape(shape, x, y);
  }

  /**
   * Creates a bounded rectangle.
   * @param x
   * @param y
   * @param b base
   * @param h height
   * @return the rectangle
   */
  public static BoundedShape createBoundedTriangle(double x, double y, double b, double h) {
    GeneralPath path = new GeneralPath();
    path.moveTo((float) (-b/2), (float) (-h/2));
    path.lineTo((float) (+b/2), (float) (-h/2));
    path.lineTo(0, (float) (h/2));
    path.closePath();
    Shape shape = path;
    return new BoundedShape(shape, x, y);
  }

  /**
   * Creates a bounded arrow.
   * @param x
   * @param y
   * @param w base
   * @param h height
   * @return the arrow
   */
  public static BoundedShape createBoundedArrow(double x, double y, double w, double h) {
    InteractiveArrow ia = new InteractiveArrow(x, y, w, h);
    ia.hideBounds = false;
    return ia;
  }

  /**
   * Creates a bounded arrow.
   * @param x
   * @param y
   * @param w base
   * @param h height
   * @return the arrow
   */
  public static BoundedShape createBoundedCenteredArrow(double x, double y, double w, double h) {
    InteractiveCenteredArrow ica = new InteractiveCenteredArrow(x, y, w, h);
    ica.hideBounds = false;
    return ica;
  }

  /**
   * Creates a bounded image.
   * @param x
   * @param y
   * @param image
   * @return the rectangle
   */
  public static BoundedShape createBoundedImage(Image image, double x, double y) {
    return new BoundedImage(image, x, y);
  }

  /**
   * Creates a bounded ellipse.
   *
   * @param x
   * @param y
   * @param w
   * @param h
   * @return BoundedShape
   */
  public static BoundedShape createBoundedEllipse(double x, double y, double w, double h) {
    Shape shape = new Ellipse2D.Double(-w/2, -h/2, w, h);
    return new BoundedShape(shape, x, y);
  }

  /**
   * Creates a bounded circle.
   *
   * @param x
   * @param y
   * @param d the diameter
   * @return the circle
   */
  public static BoundedShape createBoundedCircle(double x, double y, double d) {
    Shape shape = new Ellipse2D.Double(-d/2, -d/2, d, d);
    return new BoundedShape(shape, x, y);
  }

  @Override
public void setSelected(boolean selected) {
    this.selected = selected;
  }

  @Override
public boolean isSelected() {
    return selected;
  }

  /**
   * Sets the xy drag option.
   *
   * @param enable boolean
   */
  public void setXYDrag(boolean enable) {
    xyDrag = enable;
  }

  /**
   * Gets the xy drag boolean.
   *
   * @return boolean true if center can be dragged
   */
  public boolean isXYDrag() {
    return xyDrag;
  }

  /**
   * Sets the rotate drag option.
   *
   * @param enable boolean
   */
  public void setRotateDrag(boolean enable) {
    rotateDrag = enable;
  }

  /**
   * Gets the rotate drag option.
   * @return boolean
   */
  public boolean isRotateDrag() {
    return rotateDrag;
  }

  /**
   * Sets the width drag option.
   * @param enable boolean
   */
  public void setWidthDrag(boolean enable) {
    widthDrag = enable;
  }

  /**
   * Gets the width width drag option.
   *
   * @return boolean true if center can be dragged
   */
  public boolean isWidthDrag() {
    return widthDrag;
  }

  /**
   * Sets the height drag option.
   * @param enable boolean
   */
  public void setHeightDrag(boolean enable) {
    heightDrag = enable;
  }

  /**
   * Gets the height drag option.
   *
   * @return boolean true if center can be dragged
   */
  public boolean isHeightDrag() {
    return heightDrag;
  }

	@Override
	public java.awt.Cursor getPreferredCursor() {
		int c = Cursor.HAND_CURSOR;
		if (xyDrag && (hotspot == CENTER)) {
			c = Cursor.MOVE_CURSOR;
		} else if (rotateDrag && (hotspot == CORNER)) { // need better cursors!
			// hand
		} else if (widthDrag && (hotspot == LEFT)) {
			if (theta == 0)
				c = Cursor.W_RESIZE_CURSOR;
		} else if (widthDrag && (hotspot == RIGHT)) {
			if (theta == 0)
				c = Cursor.E_RESIZE_CURSOR;
		} else if (heightDrag && (hotspot == TOP)) {
			if (theta == 0)
				c = Cursor.N_RESIZE_CURSOR;
		} else if (heightDrag && (hotspot == BOTTOM)) {
			if (theta == 0)
				c = Cursor.S_RESIZE_CURSOR;
		} else if (selected) {
			c = Cursor.CROSSHAIR_CURSOR;
		}
		return Cursor.getPredefinedCursor(c);
	}

  @Override
public void toggleSelected() {
    selected = !selected;
  }

  @Override
public Interactive findInteractive(DrawingPanel panel, int xpix, int ypix) {
    if(isInside(panel, xpix, ypix)) {
      return xyDelegate;
    }
    return null;
  }

  /**
   * Determines if the shape is enabled and if the given pixel coordinates are within the shape.
   *
   * @param panel DrawingPanel
   * @param xpix int
   * @param ypix int
   * @return boolean
   */
  @Override
public boolean isInside(DrawingPanel panel, int xpix, int ypix) {
    hotspot = NONE;
    if(!enabled) {
      return false;
    }
    if(selected) {
        hotspot = getHotSpotIndex(xpix, ypix, hotSpots);
        return true;
      }
    return pixelBounds.contains(xpix, ypix);
  }

  int getHotSpotIndex(int xpix, int ypix, Point2D.Double[] hotSpots) {
    for(int i = 0, n = hotSpots.length; i<n; i++) {
      double dx = xpix-hotSpots[i].x;
      double dy = ypix-hotSpots[i].y;
      if(dx*dx+dy*dy<=deltaSqr) {
        return i;
      }
    }
    return NONE;
  }

    /**
     * 
     * @param temp
     * @param ar   aspect ratio  -scale_y/scale_x
     * @return
     */
	Rectangle2D.Double computeScaledHotSpots(Shape temp, double ar) {

		Rectangle2D.Double rect = getBounds2D(temp);

		double sin = Math.sin(theta);
		double cos = Math.cos(theta);

		double w2 = rect.width / 2;
		double h2 = rect.height / 2;


		double xoff = this.xoff * toPixels.getScaleX();
		double yoff = this.yoff * toPixels.getScaleY();

		double left =  xoff - w2;  // left(fixed)
		double right = xoff + w2;  // right(fixed)
		double top =  yoff - h2;   // bottom(fixed)
		double bottom = yoff + h2; // top(fixed)

		double cx = rect.x - left; // = rect.x + w2 - xoff = rect.getCenterX() - xoff;
		double cy = rect.y - top;  // = rect.y + h2 - yoff = rect.getCenterY() - yoff;

		hotSpots[CENTER].setLocation(cx, cy);
		hotSpots[BOTTOM].setLocation( cx + xoff * cos   + bottom * sin / ar,   cy - xoff * sin        + bottom * cos);
		hotSpots[LEFT].setLocation(   cx + left * cos   + yoff * sin,          cy - left * sin * ar   + yoff * cos);
		hotSpots[TOP].setLocation(    cx + xoff * cos   + top * sin / ar,      cy - xoff * sin        + top * cos);
		hotSpots[RIGHT].setLocation(  cx + right * cos  + yoff * sin,          cy - right * sin * ar  + yoff * cos);
		hotSpots[CORNER].setLocation( cx + right * cos  + top * sin / ar,      cy - right * sin * ar  + top * cos);
		return rect;
	}

	Rectangle2D.Double computeFixedHotSpots(Shape temp) {
		
		Rectangle2D.Double rect = getBounds2D(temp);

		double sin = Math.sin(theta);
		double cos = Math.cos(theta);

		double w2 = rect.width / 2;
		double h2 = rect.height / 2;


		double xoff = this.xoff;
		double yoff = this.yoff;
		
		double left = xoff - w2;  
		double right = xoff + w2;  
		double top = yoff + h2;    
		double bottom = yoff - h2; 
		
		double cx = rect.x - left; // = rect.x + w2 - xoff = rect.getCenterX() - xoff; // center x
		double cy = rect.y + top;  // = rect.y + h2 + yoff = rect.getCenterY() + yoff; // center y
		hotSpots[CENTER].setLocation(cx, cy); // center
		hotSpots[BOTTOM].setLocation(  cx + xoff * cos   - bottom * sin,    cy - xoff * sin      - bottom * cos);
		hotSpots[LEFT].setLocation(    cx + left * cos   - yoff * sin,      cy - left * sin      - yoff * cos);
		hotSpots[TOP].setLocation(     cx + xoff * cos   - top * sin,       cy - xoff * sin      - top * cos);
		hotSpots[RIGHT].setLocation(   cx + right * cos  - yoff * sin,      cy - right * sin     - yoff * cos);
		hotSpots[CORNER].setLocation(  cx + right * cos  - top * sin,       cy - right * sin     - top * cos);
		return rect;
	}

	protected Double getBounds2D(Shape temp) {
		return (Double) (temp instanceof Double ? temp : temp.getBounds2D());
	}

/**
   * Sets the x and y coordinates using hotspots.
   *
   * @param y
   */
  void setHotSpotXY(double x, double y) {
    if(hideBounds) {
      setXY(x, y);
      return;
    }
    if(xyDrag&&selected&&(hotspot==CENTER)) {
      setXY(x, y);
    } else if(rotateDrag&&selected&&(hotspot==CORNER)) {
      if(pixelSized) {
        double r = -toPixels.getScaleY()/toPixels.getScaleX();
        double dx = x-this.x;
        double dy = y-this.y;
        theta = Math.atan2(r*dy, dx)-Math.atan2(height/2+yoff, (width/2+xoff));
      } else {
        double dx = x-this.x;
        double dy = y-this.y;
        double theta1 = Math.atan2(height/2+yoff, width/2+xoff);
        double theta2 = Math.atan2(dy, dx);
        setTheta(theta2-theta1);
      }
    } else if(widthDrag&&selected&&((hotspot==LEFT)||(hotspot==RIGHT))) {
      if(pixelSized) {
        double dx = toPixels.getScaleX()*(x-this.x)-xoff;
        double dy = toPixels.getScaleY()*(y-this.y)+yoff;
        BoundedShape.this.setWidth(2*Math.sqrt(dx*dx+dy*dy));
      } else {
        double dx = (x-this.x-xoff);
        double dy = (y-this.y-yoff);
        setWidth(2*Math.sqrt(dx*dx+dy*dy));
      }
    } else if(heightDrag&&selected&&((hotspot==TOP)||(hotspot==BOTTOM))) {
      if(pixelSized) {
        double dx = toPixels.getScaleX()*(x-this.x)-xoff;
        double dy = toPixels.getScaleY()*(y-this.y)+yoff;
        BoundedShape.this.setHeight(2*Math.sqrt(dx*dx+dy*dy));
      } else {
        double dx = (x-this.x-xoff);
        double dy = (y-this.y-yoff);
        setHeight(2*Math.sqrt(dx*dx+dy*dy));
      }
    }
  }

	/**
	 * Draws the shape.
	 *
	 * @param panel the drawing panel
	 * @param g     the graphics context
	 */
	@Override
	public void draw(DrawingPanel panel, Graphics g) {
		super.draw(panel, g);
		// these methods also set pixelBounds
		if (pixelSized) {
			getFixedBounds();
			if (!selected || hideBounds)
				return;
			drawFixedBounds(g);
		} else {
			getScaledBounds(panel);
			if (!selected || hideBounds)
				return;
			drawScaledBounds(g);
		}
	}

	  private void getScaledBounds(DrawingPanel panel) {
		    double ar = -toPixels.getScaleY()/toPixels.getScaleX();
		    Shape temp;
		    if(theta==0) {
		      temp = toPixels.createTransformedShape(getBounds2D(shape));
		      pixelBounds = computeScaledHotSpots(temp, ar);
		    } else {
		      // rotate the shape into standard position to get correct x-y bounds
		      temp = getRotateInstance(-theta,  x,  y).createTransformedShape(shape);
		      // the following alternate should also give the correct bounds in world coordinates
		      // Shape temp = new Rectangle2D.Double(x-width/2, y-height/2, width, height);
		      temp = toPixels.createTransformedShape(temp);
		      temp = computeScaledHotSpots(temp, ar);
		      double px = ((Double) temp).getCenterX()-xoff*toPixels.getScaleX();
		      double py = ((Double) temp).getCenterY()-yoff*toPixels.getScaleY();
		      if(panel.isSquareAspect()) {
		    	pixelBounds = getRotateInstance(-theta, px, py).createTransformedShape(temp);
		      } else {
		          trIS.setToTranslation(px, py);
		          trIS.scale(1, ar);
		          trIS.rotate(-theta);
		          trIS.scale(1, 1/ar);
		          trIS.translate(-px, -py);
		  		  pixelBounds = trIS.createTransformedShape(temp);

		// BH was:
//		        pixelBounds = getTranslateInstance(-px, -py).createTransformedShape(pixelBounds);
//		        pixelBounds = getScaleInstance(1, 1/ar).createTransformedShape(temp);
//		        pixelBounds = getRotateInstance(-theta).createTransformedShape(temp);
//		        pixelBounds = getScaleInstance(1, ar).createTransformedShape(temp);
//		        pixelBounds = getTranslateInstance(px, py).createTransformedShape(temp);
		      }
		    }
		  
	  }

	/**
	 * Draws the shape.
	 *
	 * @param g     the graphics context
	 */
	private void drawScaledBounds(Graphics g) {
		Graphics2D g2 = ((Graphics2D) g);
		g2.setPaint(boundsColor);
		g2.draw(pixelBounds);
		if (rotateDrag) {
			g2.fillOval((int) hotSpots[CORNER].getX() - delta, (int) hotSpots[CORNER].getY() - delta, d2, d2);
		}
		if (heightDrag) {
			g2.fillRect((int) hotSpots[TOP].getX() - delta, (int) hotSpots[TOP].getY() - delta, d2, d2);
			g2.fillRect((int) hotSpots[BOTTOM].getX() - delta, (int) hotSpots[BOTTOM].getY() - delta, d2, d2);
		}
		if (widthDrag) {
			g2.fillRect((int) hotSpots[LEFT].getX() - delta, (int) hotSpots[LEFT].getY() - delta, d2, d2);
			g2.fillRect((int) hotSpots[RIGHT].getX() - delta, (int) hotSpots[RIGHT].getY() - delta, d2, d2);
		}
		if (xyDrag) {
			g2.fillRect((int) hotSpots[CENTER].getX() - delta, (int) hotSpots[CENTER].getY() - delta, d2, d2);
			g2.setColor(edgeColor);
			g2.fillOval((int) hotSpots[CENTER].getX() - 1, (int) hotSpots[CENTER].getY() - 1, 3, 3);
			g2.setPaint(boundsColor);
		}
		g.setColor(Color.BLACK);
	}

	private void getFixedBounds() {
		Shape temp;
		if (theta == 0) {
			temp = getTranslateInstance(-x + pixelPt.x + xoff, -y + pixelPt.y - yoff)
					.createTransformedShape(getBounds2D(shape));
			pixelBounds = computeFixedHotSpots(temp);
		} else {
			// rotate the shape into standard position to get correct x-y bounds
			temp = getTranslateInstance(-x + pixelPt.x + xoff, -y + pixelPt.y - yoff)
					.createTransformedShape(shape);
			pixelBounds = getRotateInstance(-theta, pixelPt.getX(), pixelPt.getY())
					.createTransformedShape(computeFixedHotSpots(temp));
		}
	}

/**
   * Draws the shape.
   *
   * @param g  the graphics context
   */
  private void drawFixedBounds(Graphics g) {
    Graphics2D g2 = ((Graphics2D) g);
    g2.setPaint(boundsColor);
    g2.draw(pixelBounds);
    if(rotateDrag) {
      g2.fillOval((int) hotSpots[CORNER].getX()-delta, (int) hotSpots[CORNER].getY()-delta, d2, d2);
    }
    if(heightDrag) {
      g2.fillRect((int) hotSpots[TOP].getX()-delta, (int) hotSpots[TOP].getY()-delta, d2, d2);
      g2.fillRect((int) hotSpots[BOTTOM].getX()-delta, (int) hotSpots[BOTTOM].getY()-delta, d2, d2);
    }
    if(widthDrag) {
      g2.fillRect((int) hotSpots[LEFT].getX()-delta, (int) hotSpots[LEFT].getY()-delta, d2, d2);
      g2.fillRect((int) hotSpots[RIGHT].getX()-delta, (int) hotSpots[RIGHT].getY()-delta, d2, d2);
    }
    if(xyDrag) {
      g2.fillRect((int) hotSpots[CENTER].getX()-delta, (int) hotSpots[CENTER].getY()-delta, d2, d2);
      g2.setColor(edgeColor);
      g2.fillOval((int) hotSpots[CENTER].getX()-1, (int) hotSpots[CENTER].getY()-1, 3, 3);
      g2.setPaint(boundsColor);
    }
    g.setColor(Color.BLACK);
  }

/**
   * Gets a description of this object.
   * @return String
   */
  @Override
public String toString() {
    return "BoundedShape:"+"\n \t shape="+shapeClass+"\n \t x="+x+"\n \t y="+y+"\n \t width="+width+"\n \t height="+height+"\n \t theta="+theta; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
  }

  /**
   * Gets the XML object loader for this class.
   * @return ObjectLoader
   */
  public static XML.ObjectLoader getLoader() {
    return new BoundedShapeLoader();
  }

  /**
   *  A class to save and load BoundedShape in an XMLControl.
   */
  protected static class BoundedShapeLoader extends InteractiveShapeLoader {
    @Override
	public void saveObject(XMLControl control, Object obj) {
      super.saveObject(control, obj);
      BoundedShape boundedShape = (BoundedShape) obj;
      control.setValue("xy drag", boundedShape.isXYDrag());         //$NON-NLS-1$
      control.setValue("width drag", boundedShape.isWidthDrag());   //$NON-NLS-1$
      control.setValue("height drag", boundedShape.isHeightDrag()); //$NON-NLS-1$
      control.setValue("rotate drag", boundedShape.isRotateDrag()); //$NON-NLS-1$
    }

    @Override
	public Object createObject(XMLControl control) {
      return new BoundedShape(new Rectangle2D.Double(0, 0, 0, 0), 0, 0); // default shape is a rectangle for now
    }

    @Override
	public Object loadObject(XMLControl control, Object obj) {
      BoundedShape boundedShape = (BoundedShape) obj;
      boundedShape.setXYDrag(control.getBoolean("xy drag"));         //$NON-NLS-1$
      boundedShape.setWidthDrag(control.getBoolean("width drag"));   //$NON-NLS-1$
      boundedShape.setHeightDrag(control.getBoolean("height drag")); //$NON-NLS-1$
      boundedShape.setRotateDrag(control.getBoolean("rotate drag")); //$NON-NLS-1$
      super.loadObject(control, obj);
      return boundedShape;
    }

  }

  class XYDelegate extends AbstractInteractive implements Selectable {
    @Override
	public void draw(DrawingPanel panel, Graphics g) {}

    @Override
	public boolean isInside(DrawingPanel panel, int xpix, int ypix) {
      return BoundedShape.this.isInside(panel, xpix, ypix);
    }

    @Override
	public void setXY(double x, double y) {
      BoundedShape.this.setHotSpotXY(x, y);
    }

    @Override
	public void setSelected(boolean selectable) {
      BoundedShape.this.setSelected(selectable);
    }

    @Override
	public void toggleSelected() {
      BoundedShape.this.toggleSelected();
    }

    @Override
	public boolean isSelected() {
      return BoundedShape.this.isSelected();
    }

    @Override
	public Cursor getPreferredCursor() {
      return BoundedShape.this.getPreferredCursor();
    }

  }

//	static void testTransform() {
//		double px = 87;
//		double py = 33;
//		double ar = 0.3;
//		double theta = Math.PI / 3.2;
//		Shape temp = new Rectangle2D.Double(1, 2, 3, 4);
//		
//		// original: 
//		
//		temp = AffineTransform.getTranslateInstance(-px, -py).createTransformedShape(temp);
//		temp = AffineTransform.getScaleInstance(1, 1 / ar).createTransformedShape(temp);
//		temp = AffineTransform.getRotateInstance(-theta).createTransformedShape(temp);
//		temp = AffineTransform.getScaleInstance(1, ar).createTransformedShape(temp);
//		temp = AffineTransform.getTranslateInstance(px, py).createTransformedShape(temp);
//		System.out.println(((java.awt.geom.Path2D.Double) temp).getBounds2D());
//
//		//java.awt.geom.Rectangle2D$Double[x=-46.69756664428215,y=36.4809161227257,w=12.752972196426072,h=2.970603583150705]
//
//		System.out.println("---");
//
//		// using preConcatenate:
//		
//		temp = new Rectangle2D.Double(1, 2, 3, 4);
//		AffineTransform tr;
//		tr = AffineTransform.getTranslateInstance(-px, -py);
//		tr.preConcatenate(AffineTransform.getScaleInstance(1, 1 / ar));
//		tr.preConcatenate(AffineTransform.getRotateInstance(-theta));
//		tr.preConcatenate(AffineTransform.getScaleInstance(1, ar));
//		tr.preConcatenate(AffineTransform.getTranslateInstance(px, py));
//		temp = tr.createTransformedShape(temp);
//		System.out.println(((java.awt.geom.Path2D.Double) temp).getBounds2D());
//
//		// java.awt.geom.Rectangle2D$Double[x=-46.697566644282126,y=36.48091612272571,w=12.752972196426079,h=2.970603583150698]
//
//		// Applying transforms in reverse order; only one new object
//		
//		temp = new Rectangle2D.Double(1, 2, 3, 4);
//		tr.setToTranslation(px, py);
//		tr.scale(1, ar);
//		tr.rotate(-theta);
//		tr.scale(1, 1 / ar);
//		tr.translate(-px, -py);
//		temp = tr.createTransformedShape(temp);
//		System.out.println(((java.awt.geom.Path2D.Double) temp).getBounds2D());
//
//		// java.awt.geom.Rectangle2D$Double[x=-46.697566644282126,y=36.4809161227257,w=12.752972196426079,h=2.970603583150698]
//
//	}
}

/*
 * Open Source Physics software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.

 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be released
 * under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
