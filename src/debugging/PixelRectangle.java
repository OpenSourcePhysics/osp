/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see: 
 * <http://www.opensourcephysics.org/>
 */

package debugging;
import java.awt.*; // uses Abstract Window Toolkit (awt)
import org.opensourcephysics.display.*;

/**
 * PixelRectangle demonstrates how to draw an object using the AWT drawing API.
 *
 * @author Wolfgang Christian, Jan Tobochnik, Harvey Gould
 * @version 1.0  05/16/05
 */
public class PixelRectangle implements Drawable {
  int left, top;     // position of rectangle in pixels
  int width, height; // size of rectangle in pixels

  /**
   * Constructs a PixelRectangle with position and dimensions values given in pixel coordinates.
   *
   * @param left int
   * @param top int
   * @param width int
   * @param height int
   */
  public PixelRectangle(int left, int top, int width, int height) {
    this.left = left; // location of left edge
    this.top = top;   // location of top edge
    this.width = width;
    this.height = height;
  }

  /**
   * Draws this rectangle using the AWT drawing API.
   * Required to implement the Drawable interface.
   *
   * @param panel DrawingPanel
   * @param g Graphics
   */
  public void draw(DrawingPanel panel, Graphics g) {
  	Graphics2D g2 = (Graphics2D) g;
  	Shape clipShape = g2.getClip();
  	g2.setClip(0, 0, panel.getWidth(), panel.getHeight());
    // this method implements the Drawable interface
    g.setColor(Color.RED);                // set drawing color to red
    g.fillRect(left, top, width, height); // draws rectangle
    g2.setClip(clipShape);
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
