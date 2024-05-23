/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

/*
 * The org.opensourcephysics.media.core package defines the Open Source Physics
 * media framework for working with video and other media.
 *
 * Copyright (c) 2024  Douglas Brown and Wolfgang Christian.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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
 */
package org.opensourcephysics.media.core;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;

import org.opensourcephysics.controls.ControlsRes;
import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.display.OSPRuntime;

/**
 * This VideoRecorder records to a scratch file which is then copied as needed.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public abstract class ScratchVideoRecorder implements VideoRecorder {

	// static fields
	protected static JFileChooser chooser;
	protected static JTextComponent chooserField;
	protected static String ext; // current file extension for chooser
	protected static boolean ignoreChooser;
	protected static String tempDirectory;
	protected static String tempFilePrefix = "osp_"; //$NON-NLS-1$
	

	// instance fields
	protected VideoType videoType; // type of video being recorded
	protected Dimension dim; // dimension of new video
	protected Image frameImage; // most recently added frame
	protected double frameDuration = 100; // milliseconds
	protected int frameCount; // number of frames recorded
	protected String scratchName; // base name of scratch files
	protected int scratchNumber = 0; // appended to base name for uniqueness
	protected File scratchFile; // active scratch file
	protected boolean canRecord; // scratch file ready to accept added frames
	protected boolean hasContent; // scratch file has added frames
	protected boolean isSaved; // scratch file has been saved to saveFile
	protected File saveFile = null; // file to which scratch will be copied
	protected boolean saveChanges = false; // true to ask to save changes
	protected String tempFileBasePath;
	protected String tempFileType = "png"; //$NON-NLS-1$
	protected ArrayList<File> tempFiles = new ArrayList<File>();
	protected String suggestedFileName;
	protected String chosenExtension;
	protected String baseDir;

	/**
	 * Constructs a ScratchVideoRecorder for the specified video type.
	 *
	 * @param vidType the video type
	 */
	public ScratchVideoRecorder(VideoType vidType) {
		videoType = vidType;
		ext = videoType.getDefaultExtension();
		SimpleDateFormat formatter = new SimpleDateFormat("ssSSS"); //$NON-NLS-1$
		scratchName = tempFilePrefix + formatter.format(new Date());
		ShutdownHook shutdownHook = new ShutdownHook();
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		try {
			createScratch();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		if (chooser == null) {
			chooser = new JFileChooser(new File(OSPRuntime.chooserDir));
			chooser.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent e) {
					if (!ignoreChooser) {
						FileFilter filter = (FileFilter) e.getNewValue();
						if (filter instanceof VideoFileFilter) {
							VideoFileFilter vidFilter = (VideoFileFilter) filter;
							String ext = vidFilter.getDefaultExtension();
							if (ext != null)
								setChooserExtension(ext);
						}
					}
				}
			});
			// find chooser field
			String temp = "untitled.tmp"; //$NON-NLS-1$
			chooser.setSelectedFile(new File(temp));
			chooserField = getTextComponent(chooser, temp);
		}
	}

	/**
	 * Creates a new video (scratch file) and sets fileName to null.
	 *
	 * @throws IOException
	 */
	@Override
	public void createVideo() throws IOException {
		// create scratch file if none
		if (scratchFile == null) {
			createScratch();
			if (scratchFile == null) {
				OSPLog.severe("No scratch file"); //$NON-NLS-1$
				return;
			}
		}
		// replace scratch file with new one if needed
		if (scratchFile != null && hasContent) {
			// save movie if not saved and saveChanges flag is true
			if (saveChanges && !isSaved) {
				String query = MediaRes.getString("ScratchVideoRecorder.Dialog.SaveVideo.Message"); //$NON-NLS-1$
				int n = JOptionPane.showConfirmDialog(null, query,
						MediaRes.getString("ScratchVideoRecorder.Dialog.SaveVideo.Title"), JOptionPane.YES_NO_OPTION); //$NON-NLS-1$
				if (n == JOptionPane.YES_OPTION) {
					saveVideo();
				}
			}
			createScratch();
			saveFile = null;
			dim = null; // new video will be size of first image unless dim is set externally
		}
	}

	/**
	 * Creates a new video and sets the destination file name. If fileName is null,
	 * the user can select a file with a chooser.
	 *
	 * @param fileName name of the file to which the video will be written
	 * @throws IOException
	 */
	@Override
	public void createVideo(String fileName) throws IOException {
		createVideo(fileName, null);
	}

	@Override
	public void createVideo(String fileName, String trkPath) throws IOException {
		File file = null;
		if (fileName == null) {
			file = selectFile();
			if (file == null)
				return; // Canceled by user
			fileName = file.getAbsolutePath();
		} else {
			file = new File(fileName);
		}
		baseDir = XML.getDirectoryPath(fileName);
		createVideo();
		saveFile = file;
	}

	/**
	 * Sets the size of the video.
	 *
	 * @param dimension the dimensions of the new video
	 */
	@Override
	public void setSize(Dimension dimension) {
		dim = dimension;
	}

	/**
	 * Sets the time duration per frame.
	 *
	 * @param millis the duration per frame in milliseconds
	 */
	@Override
	public void setFrameDuration(double millis) {
		frameDuration = millis;
	}

	/**
	 * Adds a video frame with the specified image.
	 *
	 * @param image the image to be drawn on the video frame.
	 * @throws IOException
	 */
	@Override
	public void addFrame(Image image) throws IOException {
		if (image == null) {
			return;
		}
		frameImage = image;
		if ((scratchFile == null) || (hasContent && isSaved)) {
			createVideo();
		}
		if (scratchFile == null) {
			return;
		}
		if (!canRecord) {
			canRecord = startRecording();
			isSaved = false;
			hasContent = false;
		}
		if (canRecord && append(image)) {
			hasContent = true;
			frameCount++;
		}
	}
	
	/**
	 * Gets the VideoType.
	 *
	 * @return the video type
	 */
	public VideoType getVideoType() {
		return videoType;
	}

	/**
	 * Gets the current scratch video.
	 *
	 * @return the video
	 * @throws IOException
	 */
	@Override
	public Video getVideo() throws IOException {
		if (isSaved && saveFile != null) {
			return videoType.getVideo(saveFile.getAbsolutePath());
		}
		saveScratch();
		return videoType.getVideo(scratchFile.getAbsolutePath());
	}

	/**
	 * Saves the scratch video to the current file or chooser file.
	 *
	 * @return the full path to the saved file
	 * @throws IOException
	 */
	@Override
	public String saveVideo() throws IOException {
		if (saveFile != null) {
			return saveVideo(saveFile.getAbsolutePath());
		}
		return saveVideoAs();
	}

	/**
	 * Saves the current scratch video to the specified file name.
	 *
	 * @param fileName the file name
	 * @return the full path of the saved file
	 * @throws IOException
	 */
	@Override
	public String saveVideo(String fileName) throws IOException {
		if (scratchFile == null) {
			return null;
		}
		if (fileName == null) {
			return saveVideoAs();
		}
		setFileName(fileName);
		if (saveFile == null) {
			throw new IOException("could not write to read-only file"); //$NON-NLS-1$
		}
		saveScratch();
		// copy scratch to fileName
		int buffer = 8192;
		byte[] data = new byte[buffer];
		int count = 0;
		int total = 0;
		FileInputStream fin = new FileInputStream(scratchFile);
		InputStream in = new BufferedInputStream(fin);
		FileOutputStream fout = new FileOutputStream(saveFile);
		OutputStream out = new BufferedOutputStream(fout);
		while ((count = in.read(data, 0, buffer)) != -1) {
			out.write(data, 0, count);
			total += count;
		}
		out.flush();
		out.close();
		in.close();
		isSaved = true;
		OSPLog.fine("copied " + total + " bytes from " + //$NON-NLS-1$ //$NON-NLS-2$
				scratchFile.getName() + " to " + saveFile.getAbsolutePath()); //$NON-NLS-1$
		scratchFile.delete();
		return saveFile.getAbsolutePath();
	}

	/**
	 * Saves the scratch video to a file picked from a chooser.
	 *
	 * @return the full path of the saved file
	 * @throws IOException
	 */
	@Override
	public String saveVideoAs() throws IOException {
		File file = selectFile();
		if (file != null) {
			return saveVideo(file.getAbsolutePath());
		}
		return null;
	}

	/**
	 * Gets the file name of the destination video.
	 * 
	 * @return the file name
	 */
	@Override
	public String getFileName() {
		return (saveFile == null) ? null : saveFile.getAbsolutePath();
	}

	/**
	 * Sets the file name. May be null.
	 * 
	 * @param path the file name
	 */
	@Override
	public void setFileName(String path) {
		if (saveFile != null && saveFile.getAbsolutePath().equals(path)) {
			return;
		}
		File file = new File(path);
		if (file.exists() && !file.canWrite()) {
			saveFile = null;
		} else {
			saveFile = file;
		}
	}

	/**
	 * Suggests a file name. May be null.
	 * 
	 * @param name the file name
	 */
	public void suggestFileName(String name) {
		suggestedFileName = name;
	}

	/**
	 * Discards the current video and resets the recorder to a ready state.
	 */
	@Override
	public void reset() {
		if (scratchFile != null) {
			try {
				saveScratch();
			} catch (IOException e) {
			}
			scratchFile.delete();
		}
		hasContent = false;
	}

	// ________________________________ static methods
	// _________________________________

	/**
	 * Sets the extension used in the chooser.
	 */
	public static void setChooserExtension(String extension) {
		if (extension != null) {
			ext = extension;
		}
		if (ext != null && chooser != null && chooser.isVisible()) {
			final String name = (chooserField == null) ? "*." + ext //$NON-NLS-1$
					: XML.stripExtension(chooserField.getText()) + "." + ext; //$NON-NLS-1$
			Runnable runner = new Runnable() {
				@Override
				public void run() {
					File dir = chooser.getCurrentDirectory();
					String path = XML.getResolvedPath(name, dir.getAbsolutePath());
					chooser.setSelectedFile(new File(path));
					if (chooserField != null) {
						chooserField.setText(name);
					}
				}

			};
			SwingUtilities.invokeLater(runner);
		}
	}

	// ________________________________ private methods
	// _________________________________

	/**
	 * Creates the scratch file.
	 *
	 * @throws IOException
	 */
	protected void createScratch() throws IOException {
		if (hasContent || scratchFile == null) {
			String fileName = scratchName;
			if (hasContent) {
				fileName += "-" + scratchNumber++; //$NON-NLS-1$
			}
			reset();
			fileName = OSPRuntime.tempDir + "/" + fileName + getScratchExtension(); //$NON-NLS-1$
			scratchFile = new File(fileName);
			hasContent = false;
			canRecord = false;
			OSPLog.finest(scratchFile.getAbsolutePath());
		}
	}

	/**
	 * Returns the extension to use for the scratch file.
	 * 
	 * @return the extension
	 */
	protected String getScratchExtension() {
		return ".tmp"; //$NON-NLS-1$
	}

	/**
	 * Shows a save dialog used to set the output movie file.
	 * 
	 * @return the movie file, or none if canceled by user
	 */
	protected File selectFile() {
		ignoreChooser = true;
		File file = null;
		String chooserExt = null;
		chooser.setDialogTitle(MediaRes.getString("VideoIO.Dialog.SaveVideoAs.Title")); //$NON-NLS-1$
		chooser.resetChoosableFileFilters();
		VideoFileFilter[] filters = videoType.getFileFilters();
		boolean isZipType = videoType instanceof VideoIO.ZipImageVideoType;
		if (filters != null && filters.length > 0) {
			VideoFileFilter preferred = videoType.getDefaultFileFilter();
			if (preferred == null)
				preferred = filters[0];
			ext = preferred.getDefaultExtension();
			chooserExt = ext;
			if (isZipType) {
				VideoIO.ZipImageVideoType z = (VideoIO.ZipImageVideoType) videoType;
				ext = z.getImageExtension();
				VideoIO.ZipImageVideoType.checkZipContents = false;
			}
			chooser.setAcceptAllFileFilterUsed(false);
			for (int i = 0; i < filters.length; i++) {
				chooser.addChoosableFileFilter(filters[i]);
			}
			chooser.setFileFilter(preferred);	
		} else {
			chooser.setAcceptAllFileFilterUsed(true);
		}
		String filename = suggestedFileName;
		if (filename == null)
			filename = MediaRes.getString("VideoIO.FileName.Untitled"); //$NON-NLS-1$
		if (chooserExt != null) {
			filename += "." + chooserExt; //$NON-NLS-1$
		}
		chooser.setSelectedFile(new File(filename));
		if (chooserField != null)
			chooserField.setText(filename); // just in case line above not enough!
		ignoreChooser = false;
		int result = chooser.showSaveDialog(null);
		VideoIO.ZipImageVideoType.checkZipContents = true;
		if (result == JFileChooser.APPROVE_OPTION) {
			file = chooser.getSelectedFile();
			file = getFileToBeSaved(file);
			chosenExtension = XML.getExtension(file.getName());
			if (file.exists()) {
				if (file.canWrite()) {
					int selected = JOptionPane.showConfirmDialog(null, " \"" + file.getName() + "\" " //$NON-NLS-1$ //$NON-NLS-2$
							+ MediaRes.getString("VideoIO.Dialog.FileExists.Message"), //$NON-NLS-1$
							MediaRes.getString("VideoIO.Dialog.FileExists.Title"), //$NON-NLS-1$
							JOptionPane.YES_NO_CANCEL_OPTION);
					if (selected != JOptionPane.YES_OPTION) {
						file = null;
					} else {
						VideoIO.requiresReload(file.getPath());
					}
				} else {
					JOptionPane.showMessageDialog(null, ControlsRes.getString("Dialog.ReadOnly.Message"), //$NON-NLS-1$
							ControlsRes.getString("Dialog.ReadOnly.Title"), //$NON-NLS-1$
							JOptionPane.PLAIN_MESSAGE);
					file = null;
				}
			}
		}
		return file;
	}

	/**
	 * Return the file that will be saved if the specified file is selected. This is
	 * needed by ImageVideoRecorder since it strips and/or appends digits to the
	 * selected file name. This default implementation returns the file itself.
	 *
	 * @param file the file selected with the chooser
	 * @return the file (or first file) to be saved
	 */
	protected File getFileToBeSaved(File file) {
		return file;
	}

	/**
	 * Called by the garbage collector when this recorder is no longer in use.
	 */
	@Override
	protected void finalize() {
		//BH !!! 
		deleteTempFiles();
	}

	/**
	 * Deletes the temporary files.
	 */
	protected void deleteTempFiles() {
		deleteFiles(tempFiles);
	}
	
	/**
	 * Deletes files.
	 */
	protected void deleteFiles(ArrayList<File> files) {
		if (files == null)
			return;
		synchronized (files) {
			for (File next : files) {
				next.delete();
			}
			files.clear();
		}
	}


	private JTextComponent getTextComponent(Container c, String toMatch) {
		Component[] comps = c.getComponents();
		for (int i = 0; i < comps.length; i++) {
			if ((comps[i] instanceof JTextComponent) && toMatch.equals(((JTextComponent) comps[i]).getText())) {
				return (JTextComponent) comps[i];
			}
			if (comps[i] instanceof Container) {
				JTextComponent tc = getTextComponent((Container) comps[i], toMatch);
				if (tc != null) {
					return tc;
				}
			}
		}
		return null;
	}

	// ______________________________ abstract methods
	// _________________________________

	/**
	 * Saves the current video to the scratch file.
	 *
	 * @throws IOException
	 */
	abstract protected void saveScratch() throws IOException;

	/**
	 * Starts the video recording process using current dimension dim.
	 *
	 * @return true if video recording successfully started
	 */
	abstract protected boolean startRecording();

	/**
	 * Appends a frame to the current video.
	 *
	 * @param image the image to append
	 * @return true if image successfully appended
	 */
	abstract protected boolean append(Image image);

	/**
	 * A class to delete all scratch files on shutdown.
	 */
	class ShutdownHook extends Thread {
		@Override
		public void run() {
			if (scratchFile != null) {
				try {
					saveScratch();
				} catch (Exception ex) {
				}
				scratchFile.delete();
			}
			deleteTempFiles();
		}

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
