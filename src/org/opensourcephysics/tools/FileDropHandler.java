/*
 * The tracker package defines a set of video/image analysis tools
 * built on the Open Source Physics framework by Wolfgang Christian.
 *
 * Copyright (c) 2024 Douglas Brown, Wolfgang Christian, Robert M. Hanson
 *
 * Tracker is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tracker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tracker; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at <http://www.gnu.org/copyleft/gpl.html>
 *
 * For additional Tracker information and documentation, please see
 * <http://physlets.org/tracker/>.
 */
package org.opensourcephysics.tools;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.List;

import javax.swing.TransferHandler;


/**
 * A simple TransferHandler for handling via Drag&Drop.
 * 
 * Create an instance using new FileDropHandler(FileImporter)
 * 
 * where FileImporter is likely a JFrame that implements FileDropHandler.FileImporter
 *
 * @author Bob Hanson and Douglas Brown
 */
@SuppressWarnings("serial")
public class FileDropHandler extends TransferHandler {

	public interface FileImporter {

		boolean importData(Object data, Component component);
		
	}

	FileImporter frame;
	DataFlavor uriListFlavor; // for Linux

	/**
	 * Constructor.
	 * 
	 * @param frame the TFrame that will be the drop target
	 */
	public FileDropHandler(FileImporter frame) {
		this.frame = frame;
	}

	Boolean isDropOK = null;

	/**
	 * Check to see that we can import this file. It if is NOT a video-type file
	 * (mp4, jpg, etc) then set the drop action to COPY rather than MOVE.
	 * 
	 */
	@Override
	public boolean canImport(TransferSupport support) {

		return (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor));
	}

	@Override
	public boolean importData(TransferHandler.TransferSupport support) {
		if (!canImport(support))
			return false;
		List<File> fileList = getFileList(support.getTransferable());
		Object ret = fileList;
		try {
			if (fileList != null) {
				File f = fileList.get(0);
				if (f.getName().endsWith(".url")) {
					// handles file drop into 
//					[InternetShortcut]
//					URL=https://physlets.org/tracker/TestVideo/videos/collision-pucks.mp4
//					IDList=
//					HotKey=0
//					IconFile=C:\Users\hanso\AppData\Local\Mozilla\Firefox\Profiles\61sxx8ak.default-release\shortcutCache\idpQAUtmcsLXC4k0vaZC15FSd9nQt_sQb3DsXffBqxo=.ico
//					IconIndex=0

					String s = ResourceLoader.readAllAsString(new FileInputStream(f));
					s = s.substring(s.indexOf("URL=") + 4);
					ret = new URL(s.substring(0, s.indexOf("\n")).trim());
				}
				return frame.importData(ret, support.getComponent());
			}
		} catch (Exception e) {
			// ignore
		}
		return false;
	}

	/**
	 * Gets the file list from a Transferable.
	 * 
	 * @param t the Transferable
	 * @return a List of files
	 */
	@SuppressWarnings("unchecked")
	private List<File> getFileList(Transferable t) {
		try {
			return (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
		} catch (Exception e) {
			return null;
		}
	}
}
