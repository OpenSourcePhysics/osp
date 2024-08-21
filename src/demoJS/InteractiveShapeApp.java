/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see: 
 * <http://www.opensourcephysics.org/>
 */

package demoJS;
import org.opensourcephysics.display.*;
import javax.swing.JFrame;

/**
 * InteractiveShapeApp tests the InteractiveShape class by creating a rectangle and an arrow.
 * Chapter 4 example from the OSP Users Guide.
 *
 * @author Wolfgang Christian
 * @version 1.0
 */
public class InteractiveShapeApp {
	
	static  {
		/**
		 * @j2sNative
		 * 
		 *  $("body").append('<link href="ipad.css" rel="stylesheet" type="text/css">');
		 * 
		 * 	$("body").append('<div class="custom-cursor" id="customCursor" style="z-index: 10000000;"></div>');
		 * 
		 * 
		 */
	}
	
  /**
   * Starts the InteractiveShapeApp application.
   * @param args String[]
   */
  public static void main(String[] args) {
    PlottingPanel panel = new PlottingPanel("x", "y", "Interactive Demo");
    panel.setPreferredMinMax(1, 10, 1, 10);
    DrawingFrame frame = new DrawingFrame(panel);
    // create interactive shapes and add them to the panel
    InteractiveShape ishape = InteractiveShape.createRectangle(3, 4, 2, 2);
    panel.addDrawable(ishape);
    InteractiveShape arrow = InteractiveShape.createArrow(3, 4, 1, 5);
    panel.addDrawable(arrow);
    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
 * For additional information and documentation on Open Source Physics,
 * please see <http://www.opensourcephysics.org/>.
 *
 * Copyright (c) 2007  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
