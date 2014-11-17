/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display3d.core.interaction;

/**
 *
 * <p>Title: InteractionListener</p>
 *
 * <p>Description: The interface for an object that can listen
 * to interaction events.</p>
 *
 * <p>Copyright: Open Source Physics project</p>
 *
 * <p>Company: </p>
 *
 * @author Francisco Esquembre
 * @version June 2005
 * @see InteractionEvent
 */
public interface InteractionListener {
  /**
   * The method of a registered listener that will be called when an
   * interaction takes place. Classes implementing this interface
   * need to register to the interaction source using the
   * <code>addInteractionListener</code> method.
   * @param _event InteractionEvent
   */
  void interactionPerformed(InteractionEvent _event);

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
