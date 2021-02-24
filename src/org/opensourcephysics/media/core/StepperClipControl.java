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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;

import javax.swing.SwingUtilities;

import org.opensourcephysics.controls.XML;

/**
 * This is a ClipControl that displays every step in a video clip.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class StepperClipControl extends ClipControl {
	
	private static final int DIVIDER = 5;
	
	// instance fields
	private javax.swing.Timer timer;
	private double frameDuration = 100; // milliseconds
	private double stepDuration; // milliseconds, with stepSize & rate adjustment
	private boolean playing = false;
	private boolean readyToStep = true;
	private boolean stepDisplayed = true;
	private int minDelay = 0; // milliseconds
	private int maxDelay = 2000; // milliseconds
	private long startTime;
	private int timerDelay; // adjusted on the fly
	private int playStepCount;
	private double playDuration;

	/**
	 * Constructs a StepperClipControl object.
	 *
	 * @param videoClip the video clip
	 */
	protected StepperClipControl(VideoClip videoClip) {
		super(videoClip);
		videoClip.addPropertyChangeListener(this);
		if (video != null) {
			if (video.getFrameCount() > 1 && video.isValid()) {
				double ti = video.getFrameTime(video.getStartFrameNumber());
				double tf = video.getFrameTime(video.getEndFrameNumber());
				int count = video.getEndFrameNumber() - video.getStartFrameNumber();
				if (count != 0 && (tf - ti) > 0) {
					frameDuration = (int) (tf - ti) / count;
				}
			}
		}
		timer = new javax.swing.Timer(getTimerDelay(), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				readyToStep = true;
				step();
			}

		});
		timer.setRepeats(false);
		// set coalesce to false to avoid combining events when trying to go too fast
		timer.setCoalesce(false);
	}

	/**
	 * Plays the clip.
	 */
	@Override
	public void play() {
		if (clip.getStepCount() == 1) {
			return;
		}
		startTime = System.currentTimeMillis();
		playDuration = 0;
		playStepCount = 0;
		playing = true;
		readyToStep = true;
		if (stepNumber == clip.getStepCount() - 1) {
			setStepNumber(0);
		} else {
//			step();
			timer.restart();
		}
		SwingUtilities.invokeLater(()->{
			support.firePropertyChange(ClipControl.PROPERTY_CLIPCONTROL_PLAYING, null, Boolean.TRUE);
		});
	}

	/**
	 * Stops at the next step.
	 */
	@Override
	public void stop() {
		timer.stop();
		readyToStep = true;
		stepDisplayed = true;
		playing = false;
		support.firePropertyChange(ClipControl.PROPERTY_CLIPCONTROL_PLAYING, null, Boolean.FALSE); //$NON-NLS-1$
	}

	/**
	 * Steps forward one step.
	 */
	@Override
	public void step() {
//		OSPLog.debug(Performance.timeCheckStr(">>>>Stepper.step0 " + this.getClass().getName(), Performance.TIME_MARK));

		if ((stepNumber >= clip.getStepCount() - 1) && !looping) {
			stop();
		} else if (stepDisplayed && (!playing || readyToStep)) {
			stepDisplayed = false;
			if (stepNumber < clip.getStepCount() - 1) {
				setStepNumber(stepNumber + 1);
			} else if (looping) {
				setStepNumber(0);
			}
			if (playing) {
				long dt = System.currentTimeMillis() - startTime;
				playDuration += dt;
				playStepCount++;
				startTime += dt; // = currentTime
				timerDelay -= Math.round(Math.round((dt - stepDuration) / DIVIDER));
				timerDelay = Math.min(maxDelay, Math.max(minDelay, timerDelay));
				timer.setInitialDelay(timerDelay);
				readyToStep = false;
				timer.restart();
			}
		}
//		OSPLog.debug(Performance.timeCheckStr("<<<<<Stepper.step1 " + this.getClass().getName(), Performance.TIME_MARK));
	}

	/**
	 * Steps back one step.
	 */
	@Override
	public void back() {
		if (stepDisplayed && (stepNumber > 0)) {
			stepDisplayed = false;
			setStepNumber(stepNumber - 1);
		}
	}

	/**
	 * Sets the step number.
	 *
	 * @param n the desired step number
	 */
	@Override
	public void setStepNumber(int n0) {
		int step = Math.min(Math.max(0, n0), clip.getStepCount() - 1);
		if (step == stepNumber && clip.stepToFrame(step) == getFrameNumber()) {
			return;
		}
		if (video == null) {
			super.setStepNumber(step);
			stepDisplayed = true;
			support.firePropertyChange(ClipControl.PROPERTY_CLIPCONTROL_STEPNUMBER, null, Integer.valueOf(step));
		} else {
			int frame = clip.stepToFrame(step);
			if (frame > video.getEndFrameNumber()) {
				super.setStepNumber(step);
				video.setVisible(false);
				stepDisplayed = true;
				support.firePropertyChange(ClipControl.PROPERTY_CLIPCONTROL_STEPNUMBER, null, Integer.valueOf(step));
			} else {
				video.setVisible(videoVisible);
				SwingUtilities.invokeLater(() -> {
					setStepNumberLater(frame, step);
				});
			}
		}
	}

	protected void setStepNumberLater(int frame, int step) {
		if (videoFrameNumber == frame) {
			stepDisplayed = true;
		} else if (video.getFrameNumber() == frame) { // setting frame number will have no effect
			super.setStepNumber(step);
			stepDisplayed = true;
			support.firePropertyChange(ClipControl.PROPERTY_CLIPCONTROL_STEPNUMBER, null, Integer.valueOf(step));
		} else {
			video.setFrameNumber(frame);
		}
	}

	/**
	 * Sets the play rate.
	 *
	 * @param newRate the desired rate
	 */
	@Override
	public void setRate(double newRate) {
		if ((newRate == 0) || (newRate == rate)) {
			return;
		}
		rate = Math.abs(newRate);
//    if (video==null) {
//	    int delay = (int) (getMeanFrameDuration()*clip.getStepSize()/rate);
//	    delay = Math.min(delay, maxDelay);
//	    delay = Math.max(delay, minDelay);
		timer.setInitialDelay(getTimerDelay());
//    }
		support.firePropertyChange(ClipControl.PROPERTY_CLIPCONTROL_RATE, null, new Double(rate));
	}

	/**
	 * Gets the average frame duration in milliseconds (for calculations).
	 *
	 * @return the frame duration in milliseconds
	 */
	@Override
	public double getMeanFrameDuration() {
		if (video != null && video.isValid()) {
			int count = video.getEndFrameNumber() - video.getStartFrameNumber();
			if (count != 0) {
				double ti = video.getFrameTime(video.getStartFrameNumber());
				double tf = video.getFrameTime(video.getEndFrameNumber());
				return timeStretch * (tf - ti) / count;
			}
			return timeStretch * video.getDuration() / video.getFrameCount();
		}
		return frameDuration;
	}

	/**
	 * Sets the frame duration.
	 *
	 * @param duration the desired frame duration in milliseconds
	 */
	@Override
	public void setFrameDuration(double duration) {
		duration = Math.abs(duration);
		if (duration == 0 || duration == getMeanFrameDuration()) {
			return;
		}
		if (video instanceof ImageVideo) {
			ImageVideo iVideo = (ImageVideo) video;
			iVideo.setFrameDuration(duration);
			frameDuration = duration;
			timer.setInitialDelay(getTimerDelay());
		} else if (video != null && video.isValid()) {
			double ti = video.getFrameTime(video.getStartFrameNumber());
			double tf = video.getFrameTime(video.getEndFrameNumber());
			int count = video.getEndFrameNumber() - video.getStartFrameNumber();
			if (count != 0) {
				timeStretch = duration * count / (tf - ti);
			}
		} else {
			frameDuration = duration;
			timer.setInitialDelay(getTimerDelay());
		}
		support.firePropertyChange(ClipControl.PROPERTY_CLIPCONTROL_FRAMEDURATION, null, new Double(duration));
	}

	/**
	 * Turns on/off looping.
	 *
	 * @param loops <code>true</code> to turn looping on
	 */
	@Override
	public void setLooping(boolean loops) {
		if (loops == looping) {
			return;
		}
		looping = loops;
		support.firePropertyChange(ClipControl.PROPERTY_CLIPCONTROL_LOOPING, null, Boolean.valueOf(loops));
	}

	/**
	 * Gets the playing status.
	 *
	 * @return <code>true</code> if playing
	 */
	@Override
	public boolean isPlaying() {
		return playing;
	}

	/**
	 * Gets the current video time in milliseconds measured from step 0.
	 *
	 * @return the current time
	 */
	@Override
	public double getTime() {
		if (video != null && video.isValid()) {
			int n = video.getFrameNumber();
			double videoTime = video.getFrameTime(n);
			int m = clip.stepToFrame(getStepNumber());
			if (m > video.getFrameCount() - 1) {
				int extra = m - video.getFrameCount() + 1;
				videoTime = video.getFrameTime(video.getFrameCount() - 1) + extra * frameDuration;
			}
			return (videoTime - video.getStartTime()) * timeStretch;
		}
		return stepNumber * frameDuration * clip.getStepSize();
	}

	/**
	 * Gets the start time of the specified step measured from step 0.
	 *
	 * @param stepNumber the step number
	 * @return the step time
	 */
	@Override
	public double getStepTime(int stepNumber) {
		if (video != null && video.isValid()) {
			int n = clip.stepToFrame(stepNumber);
			double videoTime = video.getFrameTime(n);
			if (n > video.getFrameCount() - 1) {
				int extra = n - video.getFrameCount() + 1;
				videoTime = video.getFrameTime(video.getFrameCount() - 1) + extra * frameDuration;
			}
			return (videoTime - video.getStartTime()) * timeStretch;
		}
		return stepNumber * frameDuration * clip.getStepSize();
	}

	/**
	 * Responds to property change events.
	 *
	 * @param e the property change event
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		String name = e.getPropertyName();
		switch (name) {
		case VideoClip.PROPERTY_VIDEOCLIP_STEPSIZE:
			timer.setInitialDelay(getTimerDelay());
			return;
		case Video.PROPERTY_VIDEO_FRAMENUMBER:
			stepDisplayed = true;
			super.propertyChange(e);
			if (playing) {
				step();
			}
			return;
		default:
			super.propertyChange(e);
			break;
		}
	}

	/**
	 * Gets the timer delay.
	 *
	 * @return the timer delay in milliseconds
	 */
	private int getTimerDelay() {
		double duration = frameDuration;
		if (video != null && video.isValid()) {
			int count = video.getEndFrameNumber() - video.getStartFrameNumber();
			if (count != 0) {
				double ti = video.getFrameTime(video.getStartFrameNumber());
				double tf = video.getFrameTime(video.getEndFrameNumber());
				duration = (tf - ti) / count;
			} else
				duration = video.getDuration() / video.getFrameCount();
		}
		stepDuration = duration * clip.getStepSize() / rate;
		int delay = (int)stepDuration;
		delay = Math.max(minDelay, Math.min(delay, maxDelay));
		timerDelay = delay;
		return delay;
	}
	
  @Override
	public double getMeasuredRate() {
  	if (playStepCount > 0) {
  		double measuredStepDuration = playDuration / playStepCount;
  		return rate * stepDuration / measuredStepDuration;
  	}
    return rate;
  }

	/**
	 * Returns an XML.ObjectLoader to save and load data for this class.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
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
