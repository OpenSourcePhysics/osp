/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.lang.reflect.Method;

import javax.swing.JFrame;
import javax.swing.JMenuItem;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.Data;
import org.opensourcephysics.display.OSPFrame;

/**
 * This is a Tool interface for osp data transfers via XML.
 *
 * @author Wolfgang Christian and Doug Brown
 * @version 0.1
 */
public interface Tool {
	/**
	 * Sends a job to this tool and specifies a tool to reply to.
	 *
	 * @param job     the Job
	 * @param replyTo the tool to notify when the job is complete (may be null)
	 */
	public void send(Job job, Tool replyTo);

	/**
	 * 
	 * @param item
	 * @param data 
	 * @param replyTo
	 * @param andDisplay
	 * @param toolClass
	 */
	static boolean setSendAction(JMenuItem item, String toolName, Object data, Tool replyTo, boolean andDisplay) {
		try {
			Class<?> toolClass = Class.forName("org.opensourcephysics.tools." + toolName); //$NON-NLS-1$
			item.addActionListener((e) -> {
				try {
					Method m = toolClass.getMethod("getTool", (Class[]) null); //$NON-NLS-1$
					Tool tool = (Tool) m.invoke(null, (Object[]) null);
					tool.send(new LocalJob(data), replyTo);
					if (andDisplay) {
						if (tool instanceof OSPFrame) {
							((OSPFrame) tool).setKeepHidden(false);
						}
						((JFrame) tool).setVisible(true);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			});
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			OSPLog.finest("Cannot instantiate " + toolName + ":\n" + ex.getMessage()); //$NON-NLS-1$
			return false;
		}
	}

	public static void reply(Tool replyTo, Job job, Tool from, Data reply) {
		job.setXML(new XMLControlElement(reply).toXML());
		replyTo.send(job, from);
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
 * Copyright (c) 2024 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
