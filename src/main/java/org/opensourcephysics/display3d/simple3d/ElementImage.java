/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display3d.simple3d;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * <p>Title: ElementImage</p>
 * <p>Description: An image using the painter's algorithm</p>
 * @author Francisco Esquembre
 * @version March 2005
 */
public class ElementImage extends Element implements org.opensourcephysics.display3d.core.ElementImage {
  // Configuration variables
  private String imageFile = null;
  private double angle = 0.0;
  // Implementation variables
  private Resource resource = null;
  private Image image = null;
  private double[] coordinates = new double[3];
  private double[] size = new double[3];
  private double[] pixel = new double[3];     // The ouput of position projections
  private double[] pixelSize = new double[2]; // The ouput of size projections
  private Object3D[] objects = new Object3D[] {new Object3D(this, 0)};
  private AffineTransform transform = new AffineTransform();

  // -------------------------------------
  // New configuration methods
  // -------------------------------------
  public void setImageFile(String file) {
    this.imageFile = file;
    if(file!=null) {
      resource = ResourceLoader.getResource(file);
      if(resource!=null) {
        image = resource.getImage();
      }
    }
  }

  public String getImageFile() {
    return this.imageFile;
  }

  public void setImage(java.awt.Image image) {
    this.imageFile = null;
    this.image = image;
  }

  public void setRotationAngle(double angle) {
    this.angle = angle;
  }

  public double getRotationAngle() {
    return this.angle;
  }

  // -------------------------------------
  // Abstract part of Element or Parent methods overwritten
  // -------------------------------------
  Object3D[] getObjects3D() {
    if(!isReallyVisible()) {
      return null;
    }
    if(hasChanged()||needsToProject()) {
      projectPoints();
    }
    return objects;
  }

  void draw(Graphics2D _g2, int _index) {
    drawIt(_g2);
  }

  void drawQuickly(Graphics2D _g2) {
    if(!isReallyVisible()) {
      return;
    }
    if(hasChanged()||needsToProject()) {
      projectPoints();
    }
    drawIt(_g2);
  }

  // -------------------------------------
  // Interaction
  // -------------------------------------
  protected InteractionTarget getTargetHit(int x, int y) {
    if(!isReallyVisible()) {
      return null;
    }
    if(hasChanged()||needsToProject()) {
      projectPoints();
    }
    if(targetPosition.isEnabled()&&(Math.abs(pixel[0]-x)<SENSIBILITY)&&(Math.abs(pixel[1]-y)<SENSIBILITY)) {
      return targetPosition;
    }
    return null;
  }

  // -------------------------------------
  // Private methods
  // -------------------------------------
  private void projectPoints() {
    coordinates[0] = coordinates[1] = coordinates[2] = 0.0;
    sizeAndToSpaceFrame(coordinates);
    getDrawingPanel3D().project(coordinates, pixel);
    objects[0].setDistance(pixel[2]*getStyle().getDepthFactor());
    size[0] = getSizeX();
    size[1] = getSizeY();
    size[2] = getSizeZ();
    getDrawingPanel3D().projectSize(coordinates, size, pixelSize);
    setElementChanged(false);
    setNeedToProject(false);
  }

  private void drawIt(Graphics2D _g2) {
    if(image==null) {
      return;
    }
    if(angle!=0.0) {
      AffineTransform originalTransform = _g2.getTransform();
      transform.setTransform(originalTransform);
      transform.rotate(-angle, pixel[0], pixel[1]);
      _g2.setTransform(transform);
      _g2.drawImage(image, (int) (pixel[0]-pixelSize[0]/2), (int) (pixel[1]-pixelSize[1]/2), (int) pixelSize[0], (int) pixelSize[1], null);
      _g2.setTransform(originalTransform);
    } else {
      _g2.drawImage(image, (int) (pixel[0]-pixelSize[0]/2), (int) (pixel[1]-pixelSize[1]/2), (int) pixelSize[0], (int) pixelSize[1], null);
    }
  }

  // ----------------------------------------------------
  // XML loader
  // ----------------------------------------------------

  /**
   * Returns an XML.ObjectLoader to save and load object data.
   * @return the XML.ObjectLoader
   */
  public static XML.ObjectLoader getLoader() {
    return new Loader();
  }

  static private class Loader extends org.opensourcephysics.display3d.core.ElementImage.Loader {
    public Object createObject(XMLControl control) {
      return new ElementImage();
    }

  }

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
 * Copyright (c) 2007  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
