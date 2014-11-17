/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.media.core.VideoGrabber;
import org.opensourcephysics.media.core.VideoType;

/**
 * A video capture tool.
 * This simply forwards jobs to the media tool.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class VideoCaptureTool implements Tool, VideoTool {
	
  private VideoCaptureTool mediaCap;

  /**
   * Public constructor.
   */
  public VideoCaptureTool() {
    /** empty block */
  }

  /**
   * Protected constructor for subclasses to prevent circular references.
   *
   * @param ignored ignored
   */
  protected VideoCaptureTool(boolean ignored) {
    /** empty block */
  }

  /**
   * Adds a frame to the video if it is recording.
   *
   * @param image the frame to be added
   * @return true if frame was added
   */
  public boolean addFrame(BufferedImage image) {
    return getGrabber().addFrame(image);
  }

  /**
   * Clear the video from the tool in preparation for a new video.
   */
  public void clear() {
    getGrabber().clear();
  }

  /**
   * Sets the recording flag.
   *
   * @param record true to record rendered images
   */
  public void setRecording(boolean record) {
    getGrabber().setRecording(record);
  }

  /**
   * Gets the recording flag.
   *
   * @return true if recording rendered images
   */
  public boolean isRecording() {
    return getGrabber().isRecording();
  }

  /**
   * Sets the visibility.
   *
   * @param visible true to set this visible
   */
  public void setVisible(boolean visible) {
    getGrabber().setVisible(visible);
  }

  /**
   * Gets the visibility.
   *
   * @return true if visible
   */
  public boolean isVisible() {
    return getGrabber().isVisible();
  }

  /**
   * Determines whether this tool is functional.
   *
   * @return true if media capture is available
   */
  public boolean canCapture() {
    return getGrabber()!=null;
  }

  /**
   * Sets the video type.
   *
   * @param type the video type
   */
  public void setVideoType(VideoType type) {
    getGrabber().setVideoType(type);
  }

  /**
   * Sets the frame rate.
   *
   * @param fps the frame rate in frames per second
   */
  public void setFrameRate(double fps) {
    getGrabber().setFrameRate(fps);
  }

  /**
   * Adds frame specified by image file path.
   *
   * @param job the job
   * @param replyTo the tool to reply to
   * @throws RemoteException
   */
  public void send(Job job, Tool replyTo) throws RemoteException {
    if(job==null) {
      return;
    }
    XMLControl control = new XMLControlElement(job.getXML());
    String path = control.getString("imagepath"); //$NON-NLS-1$
    if(path!=null) {
      BufferedImage image = ResourceLoader.getBufferedImage(path);
      if(image!=null) {
        addFrame(image);
      }
    }
  }

  /**
   * Gets the shared Tool.
   *
   * @return the shared VideoCaptureTool
   */
  public static VideoCaptureTool getTool() {
    return VideoGrabber.getTool();
  }

  private VideoCaptureTool getGrabber() {
    if(mediaCap==null) {
      mediaCap = new VideoGrabber();
    }
    return mediaCap;
  }

}

/*
 * Open Source Physics software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.
 *
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
