/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.util.HashMap;
import java.util.Map;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.display.OSPRuntime;

/**
 * Toolbox stores tools that can exchange data using the Tool interface.
 *
 * @author Wolfgang Christian and Doug Brown
 * @version 0.1
 */
public class Toolbox {
	protected static Map<String, Tool> tools = new HashMap<String, Tool>();

	protected Toolbox() {
		/** empty block */
	}

	public static void addTool(String name, Tool tool) {
		if (OSPRuntime.isJS) { // external tools not supported in JavaScript.
			return;
		}
		if (tools.get(name) == null) {
			tools.put(name, tool);
			OSPLog.fine("Added to toolbox: " + name); //$NON-NLS-1$
		}
	}

	public static Tool getTool(String name) {
		if (tools.containsKey(name)) {
			// look for local tool
			Tool tool = tools.get(name);
			OSPLog.fine("Found local tool: " + name); //$NON-NLS-1$
			return tool;
		}
		return null;
	}

}

/*
 * Open Source Physics software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.
 * 
 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be
 * released under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston MA 02111-1307 USA or view the license online at
 * http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
