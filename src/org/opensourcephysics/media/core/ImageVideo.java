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
 * Copyright (c) 2017  Douglas Brown and Wolfgang Christian.
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.ResourceLoader;

import javajs.async.AsyncDialog;

/**
 * This is a Video assembled from one or more still images.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class ImageVideo extends VideoAdapter {
// instance fields
	protected Component observer = new JPanel(); // image observer
	protected Image[] images = new Image[0]; // image array
	protected BufferedImage rgbImage; // used when RGBSize < bufferedImage size
	protected String[] paths = new String[0]; // relative image paths
	protected boolean readOnly; // true if images are only loaded from files as needed
	protected double deltaT = 100; // frame duration in milliseconds
	protected Dimension rgbSize = new Dimension();

	/**
	 * Creates a read-only ImageVideo and loads a named image or image sequence.
	 * 
	 * The standard constructor.
	 *
	 * @param imageName the name of the image file
	 * @param sequence  true to automatically load image sequence, if any
	 * @throws IOException
	 */
	public ImageVideo(String imageName, String basePath, boolean sequence) throws IOException {
		readOnly = true;
		if (basePath == null) {
			basePath = XML.getDirectoryPath(imageName);
		}

		if (basePath != null) {
			baseDir = basePath; // could be .....trz! or could be relative
			setProperty("absolutePath", (imageName.startsWith(basePath) ? imageName : basePath + "/" + imageName));
		}
		append(imageName, sequence);
	}

	/**
	 * Creates a read-only ImageVideo and loads a named image or image sequence, asking the
	 * user if they want just the first image or all, if numbers are found.
	 * 
	 * No reference to this constructor in osp, ejs, or tracker
	 *
	 * @param imageName the name of the image file
	 * @throws IOException
	 */
	public ImageVideo(String imageName) throws IOException {
		readOnly = true;
		insert(imageName, 0);
	}

	/**
	 * Creates an editable ImageVideo from an image pasted from the clipboard. No questions are asked. 
	 *
	 * @param clipBoardImage the image
	 */
	public ImageVideo(Image clipBoardImage) {
		readOnly = false; 
		if (clipBoardImage != null) {
			insert(new Image[] { clipBoardImage }, 0, null);
		}
		// BH Q Should set readOnly = true? 
	}

	/**
	 * Creates an editable ImageVideo from another ImageVideo
	 *
	 * Replaces ImageVideo(BufferedImage[])
	 * 
	 * BH: Code moved here from VideoIO
	 * 
	 * Called by VideoIO.clone (which is not referenced?), untested.
	 *
	 * @param images the image array
	 */
	public ImageVideo(ImageVideo video) {
		readOnly = false;
		Image[] images = video.images;
		if (images == null)
			return;
		// BH 2021.09.11 was on images == null
		if (images.length > 0 && images[0] != null) {
			insert(images, 0, null);
		}
		rawImage = images[0];
		filterStack.addFilters(video.filterStack);
		// BH Q Should set readOnly = true? We only have an image stack and a filter.
	}
	
	/**
	 * Plays the video at the current rate.
	 */
	@Override
	public void play() {
		playing = true;
	}

	/**
	 * Stops the video.
	 */
	@Override
	public void stop() {
		playing = false;
	}

	/**
	 *
	 * @param n the desired frame number
	 */
	@Override
	public void setFrameNumber(int n) {
		super.setFrameNumber(n);
		rawImage = getImageAtFrame(getFrameNumber(), rawImage);
		updateBufferedImage(); // For SwingJS
		invalidateVideoAndFilter();
		notifyFrame(n, false); // only this subsclass does this asynchronously
	}

	/**
	 * Sets the frame duration in milliseconds.
	 *
	 * @param millis the desired frame duration in milliseconds
	 */
	public void setFrameDuration(double millis) {
		deltaT = millis;
	}

	/**
	 * not called
	 * Sets the video time in milliseconds.
	 *
	 * @param millis the desired time in milliseconds
	 */
	@Override
	public void setTime(double millis) {
		setFrameNumber(Math.min(Math.max((int) (millis / deltaT), 0), getFrameCount() - 1));
	}

	/**
	 * Gets the start time in milliseconds.
	 *
	 * @return the start time in milliseconds, or -1 if not known
	 */
	@Override
	public double getStartTime() {
		return 0;
	}

	/**
	 * Sets the start time in milliseconds. NOTE: the actual start time is normally
	 * set to the beginning of a frame.
	 *
	 * @param millis the desired start time in milliseconds
	 */
	@Override
	public void setStartTime(double millis) {
		/** not implemented */
	}

	/**
	 * Gets the end time in milliseconds.
	 *
	 * @return the end time in milliseconds, or -1 if not known
	 */
	@Override
	public double getEndTime() {
		return getDuration();
	}

	/**
	 * Sets the end time in milliseconds. NOTE: the actual end time is set to the
	 * end of a frame.
	 *
	 * @param millis the desired end time in milliseconds
	 */
	@Override
	public void setEndTime(double millis) {
		/** not implemented */
	}

	/**
	 * Gets the duration of the video.
	 *
	 * @return the duration of the video in milliseconds, or -1 if not known
	 */
	@Override
	public double getDuration() {
		return length() * deltaT;
	}

	/**
	 * Gets the start time of the specified frame in milliseconds.
	 *
	 * @param n the frame number
	 * @return the start time of the frame in milliseconds, or -1 if not known
	 */
	@Override
	public double getFrameTime(int n) {
		return n * deltaT;
	}

	/**
	 * Gets the image array.
	 *
	 * @return the image array
	 */
	public Image[] getImages() {
		return images;
	}

	/**
	 * Appends the named image or image sequence to the end of this video. This
	 * method will ask user whether to load sequences, if any.
	 *
	 * @param imageName the image name
	 * @throws IOException
	 */
	public void append(String imageName) throws IOException {
		insert(imageName, length());
	}

	/**
	 * Appends the named image or image sequence to the end of this video.
	 *
	 * @param imageName the image name
	 * @param sequence  true to automatically load image sequence, if any
	 * @throws IOException
	 */
	public void append(String imageName, boolean sequence) throws IOException {
		insert(imageName, length(), sequence);
	}

	/**
	 * Inserts the named image or image sequence at the specified index. This method
	 * will ask user whether to load sequences, if any.
	 *
	 * @param imageName the image name
	 * @param index     the index
	 * @throws IOException
	 */
	public void insert(String imageName, int index) throws IOException {
		loadImages(imageName, false, new Function<Object[], Void>() {

			@Override
			public Void apply(Object[] array) {
				if (array != null) {
					Image[] images = (Image[]) array[0];
					if (images == null || images.length > 0) {
						String[] paths = (String[]) array[1];
						insert(images, index, paths);
					}
				}
				return null;
			}
			
		});
	}

	/**
	 * Inserts the named image or image sequence at the specified index.
	 *
	 * @param imageName the image name
	 * @param index     the index
	 * @param sequence  true to automatically load image sequence, if any
	 * @throws IOException
	 */
	public void insert(String imageName, int index, boolean sequence) throws IOException {
		Object[] images_paths = loadImages(imageName, sequence, null);
		// from a TRZ file
		Image[] images = (Image[]) images_paths[0];
		if (images == null || images.length > 0) {
			String[] paths = (String[]) images_paths[1];
			insert(images, index, paths);
			if (paths.length > 1)
				size = getMaximumSize();
		}
	}

	/**
	 * Inserts an image at the specified index.
	 *
	 * @param image the image
	 * @param index the index
	 */
	public void insert(Image image, int index) {
		if (image == null) {
			return;
		}
		insert(new Image[] { image }, index, null);
	}

	/**
	 * Removes the image at the specified index.
	 *
	 * @param index the index
	 * @return the path of the image, or null if none removed
	 */
	public String remove(int index) {
		if (readOnly)
			return null;
		int len = images.length;
		if ((len == 1) || (len <= index)) {
			return null; // don't remove the only image
		}
		String removed = paths[index];
		Image[] newArray = new Image[len - 1];
		System.arraycopy(images, 0, newArray, 0, index);
		System.arraycopy(images, index + 1, newArray, index, len - 1 - index);
		images = newArray;
		String[] newPaths = new String[len - 1];
		System.arraycopy(paths, 0, newPaths, 0, index);
		System.arraycopy(paths, index + 1, newPaths, index, len - 1 - index);
		paths = newPaths;
		if (index < len - 1) {
			rawImage = getImageAtFrame(index, rawImage);
		} else {
			rawImage = getImageAtFrame(index - 1, rawImage);
		}
		setFrameCount(images.length);
		endFrameNumber = frameCount - 1;
		size = getMaximumSize();
		return removed;
	}

	/**
	 * Gets the maximum size of the images in this video.
	 *
	 * @return the maximum size of the images
	 */
	private Dimension getMaximumSize() {
		int w = images[0].getWidth(observer);
		int h = images[0].getHeight(observer);
		for (int i = 1; i < paths.length; i++) {
			Image img = getImageAtFrame(i, null);
			if (img == null)
				continue;
			w = Math.max(w, img.getWidth(observer));
			h = Math.max(h, img.getHeight(observer));
		}
		return new Dimension(w, h);
	}

	/**
	 * Gets the RGB size of this image video.
	 *
	 * @return the RGB size of the current image
	 */
	public Dimension getRGBSize() {
		return rgbSize;
	}

	/**
	 * Returns true if all of the images are associated with files.
	 *
	 * @return true if all images are file-based
	 */
	public boolean isFileBased() {
		return getValidPaths().length == paths.length;
	}

	/**
	 * Returns true if all images are loaded into memory.
	 *
	 * @return true if editable
	 */
	public boolean isEditable() {
		return !readOnly;
	}

	/**
	 * Sets the editable property.
	 * 
	 * @param edit true to edit
	 * @throws IOException
	 */
	public void setEditable(boolean edit) throws IOException {
		if (edit && isEditable())
			return; // already editable
		if (!edit && !isEditable())
			return; // already uneditable
		if (!edit) {
			saveInvalidImages();
		}
		readOnly = !edit;
		// reset arrays
		String[] thePaths = paths;
		paths = new String[0];
		images = new Image[0];
		System.gc();
		for (int i = 0; i < thePaths.length; i++) {
			if (thePaths[i] != null && thePaths[i].trim().length() > 0)
				append(thePaths[i], false);
		}
		if (frameCount < thePaths.length) {
			// setting end frame too high set it to (frameCount - 1)
			setEndFrameNumber(endFrameNumber + 1);
		}
	}

	/**
	 * Allows user to save invalid images, if any.
	 *
	 * @return true if saved
	 */
	public boolean saveInvalidImages() {
		// collect invalid paths and images
		ArrayList<String> pathList = new ArrayList<String>();
		ArrayList<Image> imageList = new ArrayList<Image>();
		for (int i = 0; i < paths.length; i++) {
			if (paths[i].equals("")) { //$NON-NLS-1$
				pathList.add(paths[i]);
				imageList.add(images[i]);
			}
		}
		if (pathList.isEmpty()) {
			return true;
		}
		// offer to save invalid paths
		int approved = JOptionPane.showConfirmDialog(null,
				MediaRes.getString("ImageVideo.Dialog.UnsavedImages.Message1") + XML.NEW_LINE //$NON-NLS-1$
						+ MediaRes.getString("ImageVideo.Dialog.UnsavedImages.Message2"), //$NON-NLS-1$
				MediaRes.getString("ImageVideo.Dialog.UnsavedImages.Title"), //$NON-NLS-1$
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		// if approved, use chooser to select file, save images and return true
		if (approved == JOptionPane.YES_OPTION) {
			try {
				ImageVideoRecorder recorder = new ImageVideoRecorder();
				recorder.setExpectedFrameCount(imageList.size());
				File file = recorder.selectFile();
				if (file == null) {
					return false;
				}
				String filePath = file.getAbsolutePath();
				BufferedImage[] imagesToSave = imageList.toArray(new BufferedImage[0]);
				String[] pathArray = ImageVideoRecorder.saveImages(filePath, imagesToSave);
				int j = 0;
				for (int i = 0; i < paths.length; i++) {
					if (paths[i].equals("")) { //$NON-NLS-1$
						String relativePath = XML.getPathRelativeTo(pathArray[j++], baseDir);
						paths[i] = XML.forwardSlash(relativePath);
//						paths[i] = pathArray[j++];
					}
				}
				if (getProperty("name") == null) { //$NON-NLS-1$
					setProperty("name", XML.getName(filePath)); //$NON-NLS-1$
					setProperty("path", filePath); //$NON-NLS-1$ // path is absolute here
					setProperty("absolutePath", filePath); //$NON-NLS-1$
				}
				return true;
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		// if declined, return false
		return false;
	}

	// _______________________ private/protected methods
	// ____________________________

	/**
	 * Gets the image for a specified frame number.
	 *
	 * @param frameNumber  the frame number
	 * @param defaultImage the image to return if no other found
	 */
	private Image getImageAtFrame(int frameNumber, Image defaultImage) {

		// It is not necessary to maintain images[]. ResourceLoader will cache
		// these easily enough as Resource objects.
		
		if (readOnly && frameNumber < paths.length) {
			if (frameNumber < images.length && images[frameNumber] != null) {
				return images[frameNumber];
			}
			if (!paths[frameNumber].equals("")) {//$NON-NLS-1$
				
//				OSPLog.debug(Performance.timeCheckStr("ImageVideo.getImageAtFrame0 " + frameNumber,
//						Performance.TIME_MARK));
//
				Image image = ResourceLoader.getVideoImage(getAbsolutePath(paths[frameNumber]));

//				OSPLog.debug(Performance.timeCheckStr("ImageVideo.getImageAtFrame1 " + frameNumber,
//						Performance.TIME_MARK));

				if (image != null) {
//not necessary					images[frameNumber] = image;
					return image;
				}
			}
		} else if (frameNumber < images.length && images[frameNumber] != null) {
			return images[frameNumber];
		}
		return defaultImage;
	}

	private int length() {
		if (readOnly)
			return paths.length;
		return images.length;
	}

	/**
	 * Loads an image or image sequence specified by name. This returns an Object[]
	 * containing an Image[] at index 0 and a String[] at index 1.
	 *
	 * @param imagePath the path to the first image in the sequence
	 * @param sequence  true to automatically load sequences (if whenDone is not null, TRUE signals that we have already asked)
	 * @param whenDone Aynchronous option, indicating that we want to ask about this
	 * @return an array of loaded images and their corresponding paths
	 * @throws IOException
	 */
	private Object[] loadImages(String imagePath, boolean sequence, Function<Object[], Void> whenDone) throws IOException {
		String[] zipPaths = VideoIO.getZippedImagePaths(imagePath);
		if (zipPaths != null && zipPaths[0].equals(imagePath)) {
			return loadImages(zipPaths, sequence, whenDone);
		}
		
		String path0 = imagePath;		
		Resource res = ResourceLoader.getResource(getAbsolutePath(imagePath));
		if (res == null) {
			throw new IOException("Image " + imagePath + " not found"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Image image = res.getImage();
		if (image == null) {
			throw new IOException("\"" + imagePath + "\" is not an image"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getProperty("name") == null) { //$NON-NLS-1$
			setProperty("name", XML.getName(imagePath)); //$NON-NLS-1$
			setProperty("path", imagePath); //$NON-NLS-1$
			setProperty("absolutePath", res.getAbsolutePath()); //$NON-NLS-1$
		}
		// if whenDone != null, then sequence is TRUE, but when whenDone is null, sequence may be true or false.
		// and if numbers are found, sequence is ignored.
		if (whenDone == null && !sequence) {
			Image[] imges = readOnly && images.length > 0? null: new Image[] { image };
			String[] paths = new String[] { imagePath };
			return new Object[] { imges, paths };
		}
		ArrayList<String> pathList = new ArrayList<String>();
		pathList.add(imagePath);

		// look for image sequence (numbered image names)		
		String name = XML.getName(imagePath);
		String extension = ""; //$NON-NLS-1$
		int i = imagePath.lastIndexOf('.');
		if (i > 0 && i < imagePath.length() - 1) {
			extension = imagePath.substring(i).toLowerCase();
			imagePath = imagePath.substring(0, i); // now free of extension
		}
		int len = imagePath.length();
		int digits = 0;
		while (digits <= 4 && --len >= 0 && Character.isDigit(imagePath.charAt(len))) {
			digits++;
		}
		int limit;
		switch (digits) {
		case 0:
			// no number found, so load single image
			Image[] imges = readOnly && images.length > 0? null: new Image[] { image };
			String[] paths = new String[] { imagePath + extension };
			Object[] ret = new Object[] { imges, paths };
			if (whenDone != null)
				whenDone.apply(ret);
			return ret;
		default:
			// 1 -> 10, 2 -> 100, etc.
			limit = (int) Math.pow(10, digits);
		}
		ArrayList<Image> imageList = new ArrayList<Image>();
		imageList.add(image);
		// image name ends with number, so look for sequence
		if (!sequence && whenDone != null) {
			// strip path from image name
			new AsyncDialog().showOptionDialog(null, "\"" + name + "\" " //$NON-NLS-1$ //$NON-NLS-2$
					+ MediaRes.getString("ImageVideo.Dialog.LoadSequence.Message") + XML.NEW_LINE + //$NON-NLS-1$
					MediaRes.getString("ImageVideo.Dialog.LoadSequence.Query"), //$NON-NLS-1$
					MediaRes.getString("ImageVideo.Dialog.LoadSequence.Title"), //$NON-NLS-1$
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
					new String[] { MediaRes.getString("ImageVideo.Dialog.LoadSequence.Button.SingleImage"), //$NON-NLS-1$
							MediaRes.getString("ImageVideo.Dialog.LoadSequence.Button.AllImages") }, //$NON-NLS-1$
					MediaRes.getString("ImageVideo.Dialog.LoadSequence.Button.AllImages"), new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							int sel = ((AsyncDialog) e.getSource()).getOption();
							switch (sel) {
							case JOptionPane.YES_OPTION:
								Image[] imges = readOnly && images.length > 0? null: imageList.toArray(new Image[0]);
								String[] paths = pathList.toArray(new String[0]);
								whenDone.apply(new Object[] { imges, paths });
								return;
							case JOptionPane.CANCEL_OPTION:
								whenDone.apply(new Object[] { new Image[0], new String[0] });
								return;
							case JOptionPane.NO_OPTION:
								try {
									loadImages(path0, true, whenDone);
								} catch (IOException e1) {
									whenDone.apply(null);
								}
								return;
							}

						}
			});
			return null;
		}
		String root = imagePath.substring(0, ++len);
		int n = Integer.parseInt(imagePath.substring(len));
		try {
			
			while (++n < limit) {
				boolean precacheImage = (!readOnly || imageList.isEmpty());
				// fill with leading zeros if nec
				String num = "000" + n;
				imagePath = root + (num.substring(num.length() - digits)) + extension;
				// load images only if not loading as needed
				if (precacheImage) {
					image = ResourceLoader.getImage(getAbsolutePath(imagePath));
					if (image == null) {
						break;
					}
				} else if (!ResourceLoader.checkExists(getAbsolutePath(imagePath))) {
					// if loading as needed, just check that the resource exists. It did not
					break;
				}
				// always add first image to list, but later images only if not loading as
				// needed
				if (precacheImage) {
					imageList.add(image);
				}
				pathList.add(imagePath);
			}
		} catch (NumberFormatException ex) {
			ex.printStackTrace();
		}
		Image[] imges = readOnly && images.length > 0? null: imageList.toArray(new Image[0]);
		String[] paths = pathList.toArray(new String[0]);
		Object[] ret = new Object[] { imges, paths };
		if (whenDone != null)
			whenDone.apply(ret);
		return ret;
	}
	
	/**
	 * Loads an image sequence. This returns an Object[]
	 * containing an Image[] at index 0 and a String[] at index 1.
	 *
	 * @param imagePaths array of paths
	 * @param sequence  true to automatically load sequences (if whenDone is not null, TRUE signals that we have already asked)
	 * @param whenDone Aynchronous option, indicating that we want to ask about this
	 * @return an array of loaded images and their corresponding paths
	 * @throws IOException
	 */
	private Object[] loadImages(String[] imagePaths, boolean sequence, Function<Object[], Void> whenDone) throws IOException {
		String path0 = imagePaths[0];

		Resource res = ResourceLoader.getResource(getAbsolutePath(path0));
		if (res == null) {
			throw new IOException("Image " + path0 + " not found"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Image image = res.getImage();
		if (image == null) {
			throw new IOException("\"" + path0 + "\" is not an image"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getProperty("name") == null) { //$NON-NLS-1$
			int n = path0.indexOf("!/");
			String path = n > 0? path0.substring(0, n): path0;
			setProperty("ext", XML.getExtension(path0));
			setProperty("name", XML.getName(path)); //$NON-NLS-1$
			setProperty("path", path); //$NON-NLS-1$
			setProperty("absolutePath", n > 0? path: res.getAbsolutePath()); //$NON-NLS-1$
		}

		ArrayList<Image> imageList = new ArrayList<Image>();
		imageList.add(image);
		
		boolean precacheImage = !readOnly && false;
		if (precacheImage) {
			for (int i = 0; i < imagePaths.length; i++) {
				image = ResourceLoader.getImage(getAbsolutePath(imagePaths[i]));
				if (image != null) {
					imageList.add(image);
				}
			}
		}
		Image[] images = imageList.toArray(new Image[imageList.size()]);
		Object[] ret = new Object[] { images, imagePaths };
		if (whenDone != null)
			whenDone.apply(ret);
		return ret;
	}

	/**
	 * Returns the valid paths (i.e., those that are not ""). Invalid paths are
	 * associated with pasted images rather than files.
	 *
	 * @return the valid paths
	 */
	public String[] getValidPaths() {
		ArrayList<String> pathList = new ArrayList<String>();
		for (int i = 0; i < paths.length; i++) {
			if (!paths[i].equals("")) {//$NON-NLS-1$
				pathList.add(paths[i]);
			}
		}
		return pathList.toArray(new String[0]);
	}

	/**
	 * Returns the valid paths (i.e., those that are not "") relative to a base path.
	 * Invalid paths are associated with pasted images rather than files.
	 * 
	 * @param base a base path
	 * @return the valid relative paths
	 */
	protected String[] getValidPathsRelativeTo(String base) {
		ArrayList<String> pathList = new ArrayList<String>();
		for (int i = 0; i < paths.length; i++) {
			if (!paths[i].equals("")) { //$NON-NLS-1$
				// be sure paths are absolute
				String absolutePath = getAbsolutePath(paths[i]);
				pathList.add(XML.getPathRelativeTo(absolutePath, base));
			}
		}
		return pathList.toArray(new String[0]);
	}

	/**
	 * Inserts images starting at the specified index.
	 *
	 * @param newImages  an array of images. May be null if readOnly
	 * @param index      the insertion index
	 * @param imagePaths array of image file paths.
	 */
	@SuppressWarnings("null")
	protected void insert(Image[] newImages, int index, String[] imagePaths) {
		if (imagePaths == null && (readOnly || newImages == null))
			return;
		int len = length();
		index = Math.min(index, len); // in case some prev images not successfully loaded
		int n = readOnly? imagePaths.length: newImages.length;
		// convert new images to BufferedImage if nec
		if (newImages != null) {
			BufferedImage[] buf = new BufferedImage[n];
			for (int i = 0; i < newImages.length; i++) {
				Image im = newImages[i];
				if (im instanceof BufferedImage) {
					buf[i] = (BufferedImage) im;
				} else {
					int w = im.getWidth(null);
					int h = im.getHeight(null);
					buf[i] = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
					Graphics2D g = buf[i].createGraphics();
					g.drawImage(im, 0, 0, null);
					g.dispose();
				}
				size.width = Math.max(size.width, buf[i].getWidth());
				size.height = Math.max(size.height, buf[i].getHeight());				
			}
			// insert new images
			Image[] newArray = new Image[len + n];
			System.arraycopy(images, 0, newArray, 0, index);
			System.arraycopy(buf, 0, newArray, index, n);
			System.arraycopy(images, index, newArray, index + n, len - index);
			images = newArray;
		}
		// create empty paths if null
		if (imagePaths == null) {
			// newImages must be non-null
			imagePaths = new String[newImages.length];
			for (int i = 0; i < imagePaths.length; i++) {
				imagePaths[i] = ""; //$NON-NLS-1$
			}
		}
		// insert new paths
		n = imagePaths.length;
		String[] newPaths = new String[len + n];
		System.arraycopy(paths, 0, newPaths, 0, index);
		System.arraycopy(imagePaths, 0, newPaths, index, n);
		System.arraycopy(paths, index, newPaths, index + n, len - index);
		paths = newPaths;
		rawImage = getImageAtFrame(index, rawImage);
		setFrameCount(length());
		endFrameNumber = frameCount - 1;
		if (coords == null) {
			size.width = rawImage.getWidth(observer);
			size.height = rawImage.getHeight(observer);
			refreshBufferedImage();
			// create coordinate system and relativeAspects
			coords = new ImageCoordSystem(frameCount, this);
			aspects = new DoubleArray(frameCount, 1);
		} else {
			size.width = Math.max(size.width, rawImage.getWidth(observer));
			size.height = Math.max(size.height, rawImage.getHeight(observer));
			coords.setLength(frameCount);
			aspects.setLength(frameCount);
		}
//		notifySize(getSize()); // NOP if loading.
	}

	@Override
	protected void updateBufferedImage() {
		refreshBufferedImage();
		if (!isValidImage) { // bufferedImage needs refreshing
			isValidImage = true;
			Graphics g = bufferedImage.createGraphics();
			
			// bufferedImage.setData(clearRaster);
			if (rawImage.getWidth(null) < bufferedImage.getWidth()
					|| rawImage.getHeight(null) < bufferedImage.getHeight()) {
				g.setColor(new Color(255, 255, 255, 255));
				g.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
				rgbSize.width = rawImage.getWidth(null);
				rgbSize.height = rawImage.getHeight(null);
			}
			else {
				rgbSize.width = bufferedImage.getWidth();
				rgbSize.height = bufferedImage.getHeight();
			}
			g.drawImage(rawImage, 0, 0, null);
			g.dispose();
		}
	}

	/**
	 * Gets the current video image after applying enabled filters.
	 *
	 * @return the current video image with filters applied
	 */
	@Override
	public BufferedImage getImage() {
		updateBufferedImage();
		if (filterStack.isEmpty() || !filterStack.isEnabled()) {
			return bufferedImage;
		} else if (!isValidFilteredImage) { // filteredImage needs refreshing
			isValidFilteredImage = true;
			if (rgbSize.width == bufferedImage.getWidth() &&
					rgbSize.height == bufferedImage.getHeight()) {
				filteredImage = filterStack.getFilteredImage(bufferedImage);
			}
			else {
				if (rgbImage == null 
						|| rgbImage.getWidth() != rgbSize.width 
						|| rgbImage.getHeight() != rgbSize.height)
					rgbImage = new BufferedImage(rgbSize.width, rgbSize.height, BufferedImage.TYPE_INT_RGB);
				Graphics g = rgbImage.createGraphics();
				g.drawImage(rawImage, 0, 0, null);
				rgbImage = filterStack.getFilteredImage(rgbImage);
				if (filteredImage == null 
						|| filteredImage.getWidth() != bufferedImage.getWidth() 
						|| filteredImage.getHeight() != bufferedImage.getHeight())
					filteredImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
				g = filteredImage.createGraphics();
				g.setColor(new Color(255, 255, 255, 255));
				g.fillRect(0, 0, filteredImage.getWidth(), filteredImage.getHeight());
				g.drawImage(rgbImage, 0, 0, null);	
			}
		}
		return filteredImage;
	}
	
	public static String getNextImagePathInSequence(String imagePath) {
		String ext = XML.getExtension(imagePath);
		ext = ext==null? "": "."+ext;
		String raw = XML.stripExtension(imagePath);
		int len = raw.length();
		if (len == 0)
			return imagePath;
		if (Character.isDigit(raw.charAt(len - 1))) {
			int i = Integer.parseInt(raw.substring(len-1));
			if (i < 9) {
				return raw.substring(0, len-1) + ++i + ext;
			}
			// rolling over from 9, so increment prev digit and add "0"
			String sub = getNextImagePathInSequence(raw.substring(0, len-1));
			return sub + "0" + ext;
		}
		return imagePath;
	}

	// ______________________________ static XML.Loader_________________________

	/**
	 * Returns an XML.ObjectLoader to save and load ImageVideo data.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

	/**
	 * A class to save and load ImageVideo data.
	 */
	static class Loader implements XML.ObjectLoader {
		/**
		 * Saves ImageVideo data to an XMLControl.
		 *
		 * @param control the control to save to
		 * @param obj     the ImageVideo object to save
		 */
		@Override
		public void saveObject(XMLControl control, Object obj) {
			ImageVideo video = (ImageVideo) obj;
			String vidBase = video.baseDir; //$NON-NLS-1$
			String trkBase = (String) video.getProperty("base"); //$NON-NLS-1$
			if (trkBase == null) {
				trkBase = vidBase;
			}
			String[] paths = video.getValidPathsRelativeTo(trkBase);
			String path = null;
			if (vidBase != null && vidBase.endsWith("zip!")) { 
				// zipped images--just save the zip file path
				String s = vidBase.substring(0, vidBase.indexOf("!"));
				s = XML.getPathRelativeTo(s, trkBase);
//				paths = new String[] {s};
				path = s;
			}
			else if (paths.length > 0) {
				path = paths[0];
			}
			if (paths.length > 0) {
				control.setValue("paths", paths); //$NON-NLS-1$
				control.setValue("path", path); //$NON-NLS-1$
				control.setBasepath(trkBase);
			}
			if (!video.filterStack.isEmpty()) {
				control.setValue("filters", video.filterStack.getFilters()); //$NON-NLS-1$
			}
			control.setValue("delta_t", video.deltaT); //$NON-NLS-1$
		}

		/**
		 * Creates a new ImageVideo.
		 *
		 * @param control the control
		 * @return the new ImageVideo
		 */
		@SuppressWarnings("unchecked")
		@Override
		public Object createObject(XMLControl control) {
			String[] paths = (String[]) control.getObject("paths"); //$NON-NLS-1$
			// legacy code that opens single image or sequence
			if (paths == null) {
				try {
					String path = control.getString("path"); //$NON-NLS-1$
					if (path != null) {
						String basepath = control.getBasepath();
						if (OSPRuntime.checkTempDirCache && basepath == null)
							path = OSPRuntime.tempDir + path;
						boolean known = control.getPropertyNamesRaw().contains("sequence");
						boolean seq = control.getBoolean("sequence") || !known; //$NON-NLS-1$
						return new ImageVideo(path, basepath, seq);
					}
				} catch (IOException ex) {
					ex.printStackTrace();
					return null;
				}
			}
			// pre-2007 code
			boolean[] sequences = (boolean[]) control.getObject("sequences"); //$NON-NLS-1$
			if (sequences != null && paths != null) {
				try {
					ImageVideo vid = new ImageVideo(paths[0], null, sequences[0]);
					for (int i = 1; i < paths.length; i++) {
						vid.append(paths[i], sequences[i]);
					}
					return vid;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return null;
			}
			
			// current (2007+) code
			if (paths == null || paths.length == 0) {
				return null;
			}
			ImageVideo vid = null;
			boolean sequenceLoaded = false;
			
			// "absolutePath" used by undo/redo
			if (control.getPropertyNamesRaw().contains("absolutePath")) {
				try {
					String path = control.getString("absolutePath"); //$NON-NLS-1$
					if (VideoIO.zipFileFilter.accept(new File(path))) {
						String[] imagePaths = VideoIO.getZippedImagePaths(path); // all absolute
						if (imagePaths != null && imagePaths.length > 0) {
							vid = new ImageVideo(imagePaths[0], null, true);
							sequenceLoaded = true;
						}
					}
					else {
						vid = new ImageVideo(path, null, false);
					}
				} catch (Exception ex) {
				}				
			}

			ArrayList<String> badPaths = null;
			for (int i = 0; i < paths.length; i++) {
				if (paths[i].toLowerCase().endsWith(".zip")
						|| paths[i].toLowerCase().contains(".zip!"))
					break;
				try {
					if (vid == null) {
						// will fail for zip images
						vid = new ImageVideo(paths[i], control.getBasepath(), false);
					} else if (!sequenceLoaded) {
						vid.append(paths[i], false);
					}
				} catch (Exception ex) {
					if (badPaths == null) {
						badPaths = new ArrayList<String>();
					}
					badPaths.add("\"" + paths[i] + "\""); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			if (vid == null) {
				// failed with paths array, but may still open with "path"?
				try {
					String path = control.getString("path"); //$NON-NLS-1$
					if (path != null 
							&& !path.toLowerCase().endsWith(".zip")
							&& !path.toLowerCase().contains(".zip!")) {
						if (OSPRuntime.checkTempDirCache)
							path = OSPRuntime.tempDir + path;
						return new ImageVideo(path, null, true);
					}
				} catch (IOException ex) {
				}				
			}
			if (vid == null && badPaths != null) {
				String s = badPaths.get(0);
				int len = s.length();
				for (int i = 1; i < badPaths.size(); i++) {
					s += ", " + badPaths.get(i); //$NON-NLS-1$
				}
				int maxLen = len + 100;
				if (s.length() > maxLen) {
					s = s.substring(0, len) + " ...";
				}
				JOptionPane.showMessageDialog(null, MediaRes.getString("ImageVideo.Dialog.MissingImages.Message") //$NON-NLS-1$
						+ ":\n" + s, //$NON-NLS-1$
						MediaRes.getString("ImageVideo.Dialog.MissingImages.Title"), //$NON-NLS-1$
						JOptionPane.WARNING_MESSAGE);
			}
			if (vid == null) {
				return null;
			}
			vid.rawImage = vid.images[0];
			vid.filterStack.addFilters((Collection<Filter>) control.getObject("filters")); //$NON-NLS-1$
			String path = paths[0];
			String ext = XML.getExtension(path);
			VideoType type = VideoIO.getVideoType(ImageVideoType.TYPE_IMAGE, ext);
			if (type != null)
				vid.setProperty("video_type", type); //$NON-NLS-1$
			vid.deltaT = control.getDouble("delta_t"); //$NON-NLS-1$
			return vid;
		}

		/**
		 * This does nothing, but is required by the XML.ObjectLoader interface.
		 *
		 * @param control the control
		 * @param obj     the ImageVideo object
		 * @return the loaded object
		 */
		@Override
		public Object loadObject(XMLControl control, Object obj) {
			return obj;
		}

	}

	@Override
	public String getTypeName() {
		return ImageVideoType.TYPE_IMAGE;
	}
	
	@Override
	public String toString() {
		return this.getTypeName() + " " + this.frameCount;
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
