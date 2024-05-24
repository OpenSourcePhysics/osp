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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeMap;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.display.DisplayRes;
import org.opensourcephysics.display.GUIUtils;
import org.opensourcephysics.display.OSPButton;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.FontSizer;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * This is a GUI component for playing a VideoClip. It uses a subclass of
 * ClipControl to control the clip and updates its display based on
 * PropertyChangeEvents it receives from the ClipControl.
 *
 * @author Douglas Brown
 * @version 1.0
 */
@SuppressWarnings("serial")
public class VideoPlayer extends JComponent implements PropertyChangeListener {

	private static final String SLIDER_REFRESH = "refresh"; //$NON-NLS-1$
	private static final String SLIDER_PROPERTY = "property";
	private static final String SLIDER_NEWCLIP = "newClip";
	
	public static final String PROPERTY_VIDEOPLAYER_BACKBUTTON = "backbutton"; //$NON-NLS-1$
	public static final String PROPERTY_VIDEOPLAYER_PLAYING = "playing"; //$NON-NLS-1$
	public static final String PROPERTY_VIDEOPLAYER_SLIDER = "slider"; //$NON-NLS-1$
	public static final String PROPERTY_VIDEOPLAYER_STEPBUTTON = "stepbutton"; //$NON-NLS-1$

	public static final String PROPERTY_VIDEOPLAYER_FRAMEDURATION = "frameduration"; //$NON-NLS-1$
	public static final String PROPERTY_VIDEOPLAYER_STEPNUMBER = "stepnumber"; //$NON-NLS-1$
	public static final String PROPERTY_VIDEOPLAYER_VIDEOCLIP = "videoclip"; //$NON-NLS-1$

	public void addActionListener(PropertyChangeListener c) {
		addPropertyChangeListener(PROPERTY_VIDEOPLAYER_BACKBUTTON, c);
		addPropertyChangeListener(PROPERTY_VIDEOPLAYER_PLAYING, c);
		addPropertyChangeListener(PROPERTY_VIDEOPLAYER_SLIDER, c);
		addPropertyChangeListener(PROPERTY_VIDEOPLAYER_STEPBUTTON, c);
	}

	public void removeActionListener(PropertyChangeListener c) {
		removePropertyChangeListener(PROPERTY_VIDEOPLAYER_BACKBUTTON, c);
		removePropertyChangeListener(PROPERTY_VIDEOPLAYER_PLAYING, c);
		removePropertyChangeListener(PROPERTY_VIDEOPLAYER_SLIDER, c);
		removePropertyChangeListener(PROPERTY_VIDEOPLAYER_STEPBUTTON, c);
//	removePropertyChangeListener("inframe", c); 
//	removePropertyChangeListener("outframe", c); 
	}

	public void addFrameListener(PropertyChangeListener c) {
		addPropertyChangeListener(PROPERTY_VIDEOPLAYER_FRAMEDURATION, c);
		addPropertyChangeListener(PROPERTY_VIDEOPLAYER_STEPNUMBER, c);
		addPropertyChangeListener(PROPERTY_VIDEOPLAYER_VIDEOCLIP, c);
	}

	public void removeFrameListener(PropertyChangeListener c) {
		removePropertyChangeListener(PROPERTY_VIDEOPLAYER_FRAMEDURATION, c);
		removePropertyChangeListener(PROPERTY_VIDEOPLAYER_STEPNUMBER, c);
		removePropertyChangeListener(PROPERTY_VIDEOPLAYER_VIDEOCLIP, c);
//	addPropertyChangeListener("inframe", c); 
//	addPropertyChangeListener("outframe", c); 	
	}

// static fields
	protected static Icon inOutIcon, playIcon, grayPlayIcon, pauseIcon;
	protected static Icon resetIcon, loopIcon, noloopIcon, videoClipIcon;
	protected static Icon stepIcon, grayStepIcon, backIcon, grayBackIcon;
	private static GoToDialog goToDialog;
	private static NumberFormat timeFormat = NumberFormat.getNumberInstance();
	static {
		String path = "/org/opensourcephysics/resources/media/images/in_out.gif"; //$NON-NLS-1$
		inOutIcon = ResourceLoader.getResizableIcon(path);
		path = "/org/opensourcephysics/resources/media/images/play.gif"; //$NON-NLS-1$
		playIcon = ResourceLoader.getResizableIcon(path);
		path = "/org/opensourcephysics/resources/media/images/play_gray.gif"; //$NON-NLS-1$
		grayPlayIcon = ResourceLoader.getResizableIcon(path);
		path = "/org/opensourcephysics/resources/media/images/pause.gif"; //$NON-NLS-1$
		pauseIcon = ResourceLoader.getResizableIcon(path);
		path = "/org/opensourcephysics/resources/media/images/reset.gif"; //$NON-NLS-1$
		resetIcon = ResourceLoader.getResizableIcon(path);
		path = "/org/opensourcephysics/resources/media/images/looping_on.gif"; //$NON-NLS-1$
		loopIcon = ResourceLoader.getResizableIcon(path);
		path = "/org/opensourcephysics/resources/media/images/looping_off.gif"; //$NON-NLS-1$
		noloopIcon = ResourceLoader.getResizableIcon(path);
		path = "/org/opensourcephysics/resources/media/images/video_clip.gif"; //$NON-NLS-1$
		videoClipIcon = ResourceLoader.getResizableIcon(path);
		path = "/org/opensourcephysics/resources/media/images/step.gif"; //$NON-NLS-1$
		stepIcon = ResourceLoader.getResizableIcon(path);
		path = "/org/opensourcephysics/resources/media/images/step_gray.gif"; //$NON-NLS-1$
		grayStepIcon = ResourceLoader.getResizableIcon(path);
		path = "/org/opensourcephysics/resources/media/images/back.gif"; //$NON-NLS-1$
		backIcon = ResourceLoader.getResizableIcon(path);
		path = "/org/opensourcephysics/resources/media/images/back_gray.gif"; //$NON-NLS-1$
		grayBackIcon = ResourceLoader.getResizableIcon(path);
	}

	// instance fields
	protected VideoPanel vidPanel;
	protected ClipControl clipControl;
	private String[] readoutTypes;
	private String readoutType;
	private boolean inspectorButtonVisible = true;
	protected int height = 54;
	// GUI elements
	private JToolBar toolbar;
	protected OSPButton readout, playButton, resetButton;
	private JSpinner rateSpinner;
	private OSPButton stepButton, stepSizeButton, backButton, loopButton;
	private OSPButton inspectorButton;
	private JSlider slider;
	private Hashtable<Integer, JLabel> sliderLabels;
	private JLabel inLabel, outLabel;
	private ActionListener readoutListener, timeSetListener, goToListener;
	private String sliderCaret;
	private boolean disabled = false;
//	private boolean updatePosted;
	private int mouseX;
	private int maxEndFrame;
	private int sliderInset;
	private ActionListener popupItemListener;
	// for measuring 
	private boolean playStarted, ignoreRateSpinner;
	private javax.swing.Timer slowRateTimer;
	private Color defaultSpinnerColor;
	private static Color slowSpinnerColor = Color.red;
	private static Color cautionSpinnerColor = new Color(200, 80, 60);
	private double measuredRate;
	static private int ntest;

	/**
	 * Constructs a VideoPlayer to play the specified video clip.
	 *
	 * @param panel the video panel
	 * @param clip  the video clip
	 */
	public VideoPlayer(VideoPanel panel, VideoClip clip) {
		this(panel);
		setVideoClip(clip);
	}
	/**
	 * Constructs a VideoPlayer.
	 *
	 * @param panel the video panel
	 */
	public VideoPlayer(VideoPanel panel) {
		vidPanel = panel;
//		vidPanel.addComponentListener(new ComponentAdapter() {
//			@Override
//			public void componentResized(ComponentEvent e) {
//				if (vidPanel.isDisplayable() && vidPanel.isPlayerVisible()) {
//					setBounds();
//					vidPanel.repaint();
//				}
//			}
//
//		});
		timeFormat.setMinimumIntegerDigits(1);
		timeFormat.setMaximumFractionDigits(3);
		timeFormat.setMinimumFractionDigits(3);
	  	if (timeFormat instanceof DecimalFormat) {
	  		((DecimalFormat)timeFormat).setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
	  	}
		createGUI();
		clipControl = ClipControl.getControl(new VideoClip(null));
		clipControl.addPropertyChangeListener(this);
		getVideoClip().addPropertyChangeListener(this);
		updatePlayButtonsLater(false);
		updateSlider(SLIDER_REFRESH, null);
	}

	/**
	 * Sets the video clip.
	 *
	 * @param clip the video clip
	 */
	public void setVideoClip(VideoClip clip) {
		boolean playing = clipControl.isPlaying();
		stop();
		if (getVideoClip() == clip) {
			// save current control state
			boolean looping = clipControl.isLooping();
			double rate = clipControl.getRate();
			double duration = clipControl.getMeanFrameDuration();
			// replace clip control
			clipControl.removePropertyChangeListener(this);
			clipControl.dispose();
			clipControl = ClipControl.getControl(clip);
			clipControl.addPropertyChangeListener(this);
			// set state of new control
			clipControl.setLooping(looping);
			clipControl.setRate(rate);
			clipControl.setFrameDuration(duration);
			if (playing) {
				clipControl.play();
			}
			ClipInspector inspector = getVideoClip().inspector;
			if (inspector != null) {
				inspector.clipControl = clipControl;
			}
		} else {
			// clean up and replace old clip
			VideoClip oldClip = getVideoClip();
			oldClip.removePropertyChangeListener(this);
			oldClip.hideClipInspector();
			oldClip.dispose();
			// dispose of old video, if any
			Video video = oldClip.getVideo();
			if (video != null) {
				video.dispose();
			}
			oldClip.video = null;
			oldClip.removePropertyChangeListener(this);
			if (clip == null) {
				clip = new VideoClip(null);
			}
			clip.addPropertyChangeListener(this);
			// clean up and replace old clip control
			clipControl.removePropertyChangeListener(this);
			clipControl.dispose();
			// switch to new control
			clipControl = ClipControl.getControl(clip);
			clipControl.addPropertyChangeListener(this);
			// update display
			setReadoutTypes("frame time step", clip.readoutType); //$NON-NLS-1$
			updateLoopButton(clipControl.isLooping());
			updateValueAndPlayButtons();
			updateSlider(SLIDER_NEWCLIP, oldClip);
			System.gc();
		}
	}

	/**
	 * Gets the video clip.
	 *
	 * @return the video clip
	 */
	public VideoClip getVideoClip() {
		return (clipControl == null ? null : clipControl.getVideoClip());
	}

	/**
	 * Gets the current clip control.
	 *
	 * @return the clip control
	 */
	public ClipControl getClipControl() {
		return clipControl;
	}

	/**
	 * Sets the readout data types made available to the user.
	 *
	 * @param types        a list of data types. Supported types are "time", "step",
	 *                     "frame".
	 * @param typeToSelect the initially selected type
	 */
	public void setReadoutTypes(String types, String typeToSelect) {
		// put supported types into map sorted by list order
		TreeMap<Integer, String> map = new TreeMap<Integer, String>();
		String list = types.toLowerCase();
		int i = list.indexOf("time"); //$NON-NLS-1$
		if (i >= 0) {
			map.put(Integer.valueOf(i), "time"); //$NON-NLS-1$
		}
		i = list.indexOf("step"); //$NON-NLS-1$
		if (i >= 0) {
			map.put(Integer.valueOf(i), "step"); //$NON-NLS-1$
		}
		i = list.indexOf("frame"); //$NON-NLS-1$
		if (i >= 0) {
			map.put(Integer.valueOf(i), "frame"); //$NON-NLS-1$
		}
		if (map.isEmpty()) {
			return;
		}
		readoutTypes = map.values().toArray(new String[0]);
		if (typeToSelect == null)
			typeToSelect = readoutTypes[0];
		setReadoutType(typeToSelect);
	}

	/**
	 * Sets the type of data displayed in the readout.
	 *
	 * @param type "time", "step", or "frame"
	 */
	public void setReadoutType(String type) {
		String name = type.toLowerCase();
		String tip = " " + MediaRes.getString("VideoPlayer.Readout.ToolTip"); //$NON-NLS-1$ //$NON-NLS-2$
		if (name.indexOf("time") >= 0) { //$NON-NLS-1$
			readoutType = "time"; //$NON-NLS-1$
			String t = vidPanel.getTimeUnit();
			t = MediaRes.getString("VideoPlayer.Readout.ToolTip.Time") + " " +t;
			readout.setToolTipText(t + tip); //$NON-NLS-1$
		} else if (name.indexOf("step") >= 0) { //$NON-NLS-1$
			readoutType = "step"; //$NON-NLS-1$
			readout.setToolTipText(MediaRes.getString("VideoPlayer.Readout.ToolTip.Step") + tip); //$NON-NLS-1$
		} else if (name.indexOf("frame") >= 0) { //$NON-NLS-1$
			readoutType = "frame"; //$NON-NLS-1$
			readout.setToolTipText(MediaRes.getString("VideoPlayer.Readout.ToolTip.Frame") + tip); //$NON-NLS-1$
		}
		// add type to readoutTypes if not already present
		boolean isListed = false;
		for (int i = 0; i < readoutTypes.length; i++) {
			isListed = isListed || (readoutTypes[i].equals(readoutType));
		}
		if (!isListed) {
			String[] newList = new String[readoutTypes.length + 1];
			newList[0] = readoutType;
			for (int i = 0; i < readoutTypes.length; i++) {
				newList[i + 1] = readoutTypes[i];
			}
			readoutTypes = newList;
		}
		getVideoClip().readoutType = readoutType;
		updateValue();
	}

	/**
	 * Plays the clip.
	 */
	public void play() {
		clipControl.play();
	}

	/**
	 * Stops at the next step.
	 */
	public void stop() {
		clipControl.stop();
	}

	/**
	 * Steps forward one step.
	 */
	public void step() {
		stop();
		clipControl.step();
	}

	/**
	 * Steps back one step.
	 */
	public void back() {
		stop();
		clipControl.back();
	}

	/**
	 * Sets the play rate.
	 *
	 * @param rate the desired rate
	 */
	public void setRate(double rate) {
		JSpinner.NumberEditor editor = (JSpinner.NumberEditor)rateSpinner.getEditor();
		boolean caution = measuredRate < rate * 0.9 && measuredRate > 0;
		editor.getTextField().setForeground(caution? cautionSpinnerColor: defaultSpinnerColor);		
		clipControl.setRate(rate);
	}

	/**
	 * Gets the play rate.
	 *
	 * @return the current rate
	 */
	public double getRate() {
		return clipControl.getRate();
	}

	/**
	 * Turns on/off looping.
	 *
	 * @param looping <code>true</code> to turn looping on
	 */
	public void setLooping(boolean looping) {
		clipControl.setLooping(looping);
	}

	/**
	 * Gets the looping status.
	 *
	 * @return <code>true</code> if looping is on
	 */
	public boolean isLooping() {
		return clipControl.isLooping();
	}

	/**
	 * Sets the step number.
	 *
	 * @param n the desired step number
	 */
	public void setStepNumber(int n) {
		clipControl.setStepNumber(n);
	}

	/**
	 * Gets the step number.
	 *
	 * @return the current step number
	 */
	public int getStepNumber() {
		return clipControl.getStepNumber();
	}

	/**
	 * Gets the current frame number.
	 *
	 * @return the frame number
	 */
	public int getFrameNumber() {
		return clipControl.getFrameNumber();
	}

	/**
	 * Gets the current time in milliseconds. Includes the start time defined by the
	 * video clip.
	 *
	 * @return the current time
	 */
	public double getTime() {
		return clipControl.getTime() + clipControl.clip.getStartTime();
	}

	/**
	 * Gets the start time of the specified step in milliseconds. Includes the start
	 * time defined by the video clip.
	 *
	 * @param stepNumber the step number
	 * @return the time
	 */
	public double getStepTime(int stepNumber) {
		if (stepNumber < 0 || stepNumber >= clipControl.clip.getStepCount())
			return Double.NaN;
		return clipControl.getStepTime(stepNumber) + clipControl.clip.getStartTime();
	}

	/**
	 * Gets the start time of the specified frame in milliseconds. Includes the
	 * start time defined by the video clip.
	 *
	 * @param frameNumber the frame number
	 * @return the time
	 */
	public double getFrameTime(int frameNumber) {
		return clipControl.clip.getStartTime()
				+ (frameNumber - clipControl.clip.getStartFrameNumber()) * clipControl.getMeanFrameDuration();
	}

	/**
	 * Gets the mean step duration in milliseconds for the current video clip.
	 *
	 * @return the mean step duration
	 */
	public double getMeanStepDuration() {
		double duration = getClipControl().getMeanFrameDuration() * getVideoClip().getStepSize();
		return duration;
	}

	/**
	 * Shows or hides the inspector button. The inspector button shows and hides the
	 * clip inspector.
	 *
	 * @param visible <code>true</code> to show the inspector button
	 */
	public void setInspectorButtonVisible(final boolean visible) {
		if (visible == inspectorButtonVisible) {
			return;
		}
		inspectorButtonVisible = visible;
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				if (toolbar == null)
					return;
				if (visible) {
					toolbar.add(inspectorButton);
				} else {
					toolbar.remove(inspectorButton);
				}
				toolbar.revalidate();
			}

		};
		
		

		
		// this is only done upon TrackerPanel construction;
		if (OSPRuntime.isJS)
			runner.run();
		else
			SwingUtilities.invokeLater(runner);
	}

	/**
	 * Shows or hides the looping button.
	 * 
	 * Only invoked during VideoGrabber.createGUI
	 *
	 * @param visible <code>true</code> to show the looping button
	 */
	public void setLoopingButtonVisible(final boolean visible) {
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				if (visible) {
					toolbar.add(loopButton);
				} else {
					toolbar.remove(loopButton);
				}
				toolbar.revalidate();
			}

		};
		if (OSPRuntime.isJS)
			runner.run();
		else
			SwingUtilities.invokeLater(runner);
	}
	
	/**
	 * Responds to property change events.
	 *
	 * @param e the property change event
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		switch (e.getPropertyName()) {
		
		case ClipControl.PROPERTY_CLIPCONTROL_STEPNUMBER:
			updateValueAndPlayButtons();
			firePropertyChange(PROPERTY_VIDEOPLAYER_STEPNUMBER, null, e.getNewValue());
			// pass to VideoPanel
			return;
		case ClipControl.PROPERTY_CLIPCONTROL_FRAMEDURATION:
			updateValue();
			firePropertyChange(PROPERTY_VIDEOPLAYER_FRAMEDURATION, null, e.getNewValue());
			// pass to VideoPanel
			return;
		case ClipControl.PROPERTY_CLIPCONTROL_PLAYING:
			if (!(Boolean)e.getNewValue() && playStarted) { // stopped playing
				playStarted = false;
				measuredRate = clipControl.getMeasuredRate();
				JSpinner.NumberEditor editor = (JSpinner.NumberEditor)rateSpinner.getEditor();
				if (measuredRate / getRate() < 0.9) {
					ignoreRateSpinner = true;
					rateSpinner.setValue(measuredRate);
					editor.getTextField().setForeground(slowSpinnerColor);
					slowRateTimer.restart();
				}
				else {
					measuredRate = 0;
					editor.getTextField().setForeground(defaultSpinnerColor);					
				}
			}
			updatePlayButtonsLater((Boolean) e.getNewValue());
			firePropertyChange(PROPERTY_VIDEOPLAYER_PLAYING, null, e.getNewValue()); 
			// to TrackerPanel
			return;
			
		// just handle or ignore
			
		case ClipControl.PROPERTY_CLIPCONTROL_LOOPING:
			updateLoopButton((Boolean) e.getNewValue());
			return;
		case ClipControl.PROPERTY_CLIPCONTROL_RATE:
			rateSpinner.setValue(Double.valueOf(getRate()));
			return;
		case VideoClip.PROPERTY_VIDEOCLIP_STARTTIME:
			updateValue();
			return;
		default:
			return;
			
		// only the next four, using break, pass to updateSlider
			
		case VideoClip.PROPERTY_VIDEOCLIP_STARTFRAME:
		case VideoClip.PROPERTY_VIDEOCLIP_FRAMECOUNT:
			break;
		case VideoClip.PROPERTY_VIDEOCLIP_STEPSIZE:
			updateValue();
			break;
		case VideoClip.PROPERTY_VIDEOCLIP_STEPCOUNT:
			updateValueAndPlayButtons();
			break;
		}
		updateSlider(SLIDER_PROPERTY, e.getPropertyName());
	}

	/**
	 * Refreshes the GUI.
	 */
	public void refresh() {
		if (readoutType == null)
			return;
		stepButton.setToolTipText(MediaRes.getString("VideoPlayer.Button.StepForward.ToolTip")); //$NON-NLS-1$
		backButton.setToolTipText(MediaRes.getString("VideoPlayer.Button.StepBack.ToolTip")); //$NON-NLS-1$
		resetButton.setToolTipText(MediaRes.getString("VideoPlayer.Button.Reset.ToolTip")); //$NON-NLS-1$
		inspectorButton.setToolTipText(MediaRes.getString("VideoPlayer.Button.ClipSettings.ToolTip")); //$NON-NLS-1$
		loopButton.setToolTipText(MediaRes.getString("VideoPlayer.Button.Looping.ToolTip")); //$NON-NLS-1$
		setReadoutType(readoutType);
		updatePlayButtonsLater(clipControl.isPlaying());
		updateLoopButton(clipControl.isLooping());
		if (getVideoClip().inspector != null) {
			getVideoClip().inspector.refresh();
		}
	}

	@Override
	public void setLocale(Locale locale) {
		timeFormat = NumberFormat.getNumberInstance(locale);
	}

/**
	 * Enables and disables this component.
	 * 
	 * @param enabled true to enable
	 */
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		disabled = !enabled;
	}

	// _________________ private methods and inner classes __________________

//	/**
//	 * Sets the bounds of this player.
//	 */
//	private void setBounds() {
//		toolbar.revalidate();
//		height = playButton.getPreferredSize().height + 8;
//		int y = vidPanel.getHeight() - height;
//		int w = vidPanel.getWidth();
//		setBounds(0, y, w, height);
//		toolbar.revalidate();
//	}

	@Override
	protected void paintChildren(Graphics g) {
		if (!OSPRuntime.isJS)
			super.paintChildren(g);

	}

	/**
	 * Creates the visible components of this player.
	 */
	private void createGUI() {
		setLayout(new BorderLayout());
		// create toolbar
		toolbar = new JToolBar();
		toolbar.setFloatable(false);
		add(toolbar, BorderLayout.SOUTH);
		setBorder(BorderFactory.createEtchedBorder());
		playButton = new OSPButton(playIcon, pauseIcon);
		playButton.setDisabledIcon(grayPlayIcon);
		playButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				doPlay();
			}

		});
		playButton.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE)
					doPlay();
			}

		});
		// resetButton
		resetButton = new OSPButton(resetIcon);
		resetButton.setPressedIcon(resetIcon);
		resetButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				doReset();
			}
		});

		// create rate spinner
		final double minRate = 0.1, maxRate = 4;
		final SpinnerNumberModel model = new SpinnerNumberModel(1, minRate, maxRate, 0.1);
		rateSpinner = new JSpinner(model) {
			// override size methods so has same height as buttons
			@Override
			public Dimension getPreferredSize() {
				return getMinimumSize();
			}

			@Override
			public Dimension getMinimumSize() {
				Dimension dim = super.getMinimumSize();
				dim.height = Math.max(playButton.getPreferredSize().height, dim.height);
				dim.width = 5 * getFont().getSize() - 10 * FontSizer.getLevel();
				return dim;
			}

			@Override
			public Dimension getMaximumSize() {
				return getMinimumSize();
			}
		};
		final JSpinner.NumberEditor editor = new JSpinner.NumberEditor(rateSpinner, "0%"); //$NON-NLS-1$
		editor.getTextField().setHorizontalAlignment(SwingConstants.LEFT);
		editor.getTextField().setFont(new Font("Dialog", Font.PLAIN, 12)); //$NON-NLS-1$
		defaultSpinnerColor = editor.getTextField().getForeground();
		rateSpinner.setEditor(editor);
		rateSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (ignoreRateSpinner)
					return;
				Double rate = (Double) rateSpinner.getValue();
				setRate(rate);
//				model.setStepSize(rate >= 2 ? 0.5 : rate >= 0.2 ? 0.1 : 0.01);
			}
		});
		editor.getTextField().addKeyListener(new java.awt.event.KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					double prev = ((Double) rateSpinner.getValue()).doubleValue();
					try {
						// remove trailing %, if any
						String s = editor.getTextField().getText();
						if (s.endsWith("%")) //$NON-NLS-1$
							s = s.substring(0, s.length() - 1);
						int i = Integer.parseInt(s);
						double rate = Math.max(i / 100.0, minRate);
						rate = Math.min(rate, maxRate);
						if (rate != prev)
							rateSpinner.setValue(Double.valueOf(rate));
						else {
							int r = (int) (prev * 100);
							editor.getTextField().setText(String.valueOf(r) + "%"); //$NON-NLS-1$
						}
					} catch (NumberFormatException ex) {
						int r = (int) (prev * 100);
						editor.getTextField().setText(String.valueOf(r) + "%"); //$NON-NLS-1$
					}
					editor.getTextField().selectAll();
				}
			}

		});

		// create step button
		stepButton = new OSPButton(stepIcon);
		stepButton.setDisabledIcon(grayStepIcon);
		stepButton.addActionListener((e) ->  {
				doStepButton((e.getModifiers() & ActionEvent.SHIFT_MASK) == 1);
		});
		// create back button
		backButton = new OSPButton(backIcon);
		backButton.setDisabledIcon(grayBackIcon);
		backButton.addActionListener((e) -> {
			doBackButton((e.getModifiers() & ActionEvent.SHIFT_MASK) == 1);
		});
		// create mouse listener and add to step and back buttons
		MouseListener stepListener = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				fireButtonEvent(e.getSource() == stepButton 
						? PROPERTY_VIDEOPLAYER_STEPBUTTON : PROPERTY_VIDEOPLAYER_BACKBUTTON, true);
				// to TrackerPanel
			}

			@Override
			public void mouseExited(MouseEvent e) {
				fireButtonEvent(e.getSource() == stepButton 
						? PROPERTY_VIDEOPLAYER_STEPBUTTON : PROPERTY_VIDEOPLAYER_BACKBUTTON, false);
				// to TrackerPanel
			}

		};
		stepButton.addMouseListener(stepListener);
		backButton.addMouseListener(stepListener);
		// create listeners
		// inner popup menu listener classes
		readoutListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setReadoutType(e.getActionCommand());
			}
		};
		goToListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showGoToDialog();
			}
		};
		timeSetListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				VideoClip clip = getVideoClip();
				String time = " (" + vidPanel.getTimeUnit() + "):";
				String response = GUIUtils.showInputDialog(vidPanel,
						MediaRes.getString("VideoPlayer.Dialog.SetTime.Message")+time, //$NON-NLS-1$
						MediaRes.getString("VideoPlayer.Dialog.SetTime.Title") + " " + getFrameNumber(), //$NON-NLS-1$ //$NON-NLS-2$
						JOptionPane.PLAIN_MESSAGE, "" + (getTime() / 1000));
				if (response != null) {
					if (response.equals("")) //$NON-NLS-1$
						clip.setStartTime(Double.NaN);
					else
						try {
							double t = Double.parseDouble(response);
							double t0 = t * 1000 - clipControl.getTime();
							clip.setStartTime(t0);
						} catch (NumberFormatException ex) {
						}
				}
				ClipInspector inspector = clip.inspector;
				if (inspector != null && inspector.isVisible()) {
					inspector.t0Field.setValue(clip.getStartTime() / 1000);
				}
			}
		};

		// create slider
		slider = new JSlider(0, 9, 0);
		slider.setName("slider" + ++ntest);
		slider.setOpaque(false);
		slider.setMinorTickSpacing(1);
		slider.setSnapToTicks(true);
		slider.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		slider.addChangeListener((e) -> { doSliderChanged(); });
		inLabel = new JLabel(inOutIcon);
		outLabel = new JLabel(inOutIcon);
		sliderLabels = new Hashtable<Integer, JLabel>();
		sliderLabels.put(Integer.valueOf(0), inLabel);
		sliderLabels.put(Integer.valueOf(9), outLabel);
		slider.setLabelTable(sliderLabels);
		slider.setPaintLabels(true);
		// slip our listeners in ahead of UI
		// undefined in SwingJS
		MouseListener defaultUIMouseListener = slider.getMouseListeners()[0];
		MouseMotionListener defaultUIMouseMotionListener = slider.getMouseMotionListeners()[0];
		slider.removeMouseListener(defaultUIMouseListener);
		slider.removeMouseMotionListener(defaultUIMouseMotionListener);
		MouseInputAdapter inOutSetter = new MouseInputAdapter() {

			@Override
			public void mouseMoved(MouseEvent e) {
				sliderMovedAction(e);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				sliderPressedAction(e);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				sliderDraggedAction(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				sliderReleasedAction(e);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				vidPanel.setMouseCursor(Cursor.getDefaultCursor());
				fireButtonEvent(PROPERTY_VIDEOPLAYER_SLIDER, false);
				// to TrackerPanel
			}

		};
		slider.addMouseListener(inOutSetter);
		slider.addMouseMotionListener(inOutSetter);
		if (defaultUIMouseListener != null)
			slider.addMouseListener(defaultUIMouseListener);
		if (defaultUIMouseMotionListener != null)
			slider.addMouseMotionListener(defaultUIMouseMotionListener);
		// BH much better to just take out the UI listener and add it later, with
		InputMap im = slider.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap am = SwingUtilities.getUIActionMap(slider);
		if (am != null) {
			// TODO
			// SwingJS Slider may not implement KeyEvents yet
			am.put(im.get(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0)), null);
			am.put(im.get(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0)), null);
		}
		slider.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				doSliderKey(e.getKeyCode());
			}

		});
		// create readout
		readout = new OSPButton();
		readout.setHeightComponent(rateSpinner);
		readout.setText("0.00");
		readout.setForeground(new Color(204, 51, 51));
		readout.addActionListener((e) -> {
			doReadoutPopup();
		});
		readout.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (OSPRuntime.isPopupTrigger(e))
					readout.doClick(0);
			}
		});
		// create stepSize button
		stepSizeButton = new OSPButton();
		stepSizeButton.setHeightComponent(rateSpinner);
		stepSizeButton.setText("1");
		stepSizeButton.setForeground(new Color(204, 51, 51));
		stepSizeButton.addActionListener((e) -> {
			doStepSize();
		});
		stepSizeButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (OSPRuntime.isPopupTrigger(e))
					stepSizeButton.doClick(0);
			}
		});
		// create inspector button
		inspectorButton = new OSPButton(videoClipIcon);
		inspectorButton.addActionListener((e) ->  {
			doInspector();
		});
		// create loop button
		loopButton = new OSPButton(noloopIcon, loopIcon);
		loopButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				setLooping(!loopButton.isSelected());
			}

		});
		loopButton.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					setLooping(!loopButton.isSelected());
				}
			}

		});
		// add components to toolbar
		toolbar.add(readout);
		toolbar.add(rateSpinner);
		toolbar.add(resetButton);
		toolbar.add(playButton);
		toolbar.add(slider);
		toolbar.add(backButton);
		toolbar.add(stepSizeButton);
		toolbar.add(stepButton);
		toolbar.add(loopButton);
		if (inspectorButtonVisible) {
			toolbar.add(inspectorButton);
		}
		
		slowRateTimer = new javax.swing.Timer(1000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JSpinner.NumberEditor editor = (JSpinner.NumberEditor)rateSpinner.getEditor();
				editor.getTextField().setForeground(cautionSpinnerColor);
				rateSpinner.setValue(getRate());
				ignoreRateSpinner = false;
			}
		});
		slowRateTimer.setRepeats(false);

	}

	protected void doSliderKey(int keyCode) {
		if (disabled)
			return;
		switch (keyCode) {
		case KeyEvent.VK_PAGE_UP:
			back();
			break;
		case KeyEvent.VK_PAGE_DOWN:
			step();
			break;
		}
	}

	protected void doPlay() {
		if (disabled || !playButton.isEnabled())
			return;
		if (playButton.isSelected()) {
			stop();
		} else {
			playStarted = true;
			play();
		}
	}

	protected void doReset() {
		if (disabled)
			return;
		stop();
		clipControl.setStepNumber(0);
		updatePlayButtonsLater(false);
	}

	protected void fireButtonEvent(String name, boolean b) {
		if (disabled)
			return;
		firePropertyChange(name, null, b);
		// to TracakerPanel
	}

	private void doBackButton(boolean isShiftDown) {
		if (disabled)
			return;
		if (isShiftDown) {
			stop();
			setStepNumber(getStepNumber() - 5);
		} else
			back();
	}

	private void doStepButton(boolean isShiftDown) {
		if (disabled)
			return;
		if (isShiftDown) {
			stop();
			setStepNumber(getStepNumber() + 5);
		} else
			step();
	}

	private void doStepSize() {
		if (disabled)
			return;
		// inner popup menu listener class
		// create popup menu and add menu items
		JPopupMenu popup = new JPopupMenu();
		for (int i = 1; i < 6; i++) {
			JMenuItem item = new JMenuItem(String.valueOf(i));
			item.addActionListener(stepSizeBtnListener);
			popup.add(item);
		}
		popup.addSeparator();
		JMenuItem item = new JMenuItem(MediaRes.getString("VideoPlayer.Button.StepSize.Other")); //$NON-NLS-1$
		item.addActionListener(stepSizeBtnListener);
		popup.add(item);
		// show popup menu
		FontSizer.setFonts(popup);
		popup.show(stepSizeButton, 0, stepSizeButton.getHeight());
	}

	private void doInspector() {		
		if (disabled)
			return;
		Frame frame = null;
		Container c = vidPanel.getTopLevelAncestor();
		if (c instanceof Frame) {
			frame = (Frame) c;
		}
		ClipInspector inspector = getVideoClip().getClipInspector(clipControl, frame);
		if (inspector.isVisible()) {
			return;
		}
		Point p0 = new Frame().getLocation();
		Point loc = inspector.getLocation();
		if ((loc.x == p0.x) && (loc.y == p0.y)) {
			// center on screen
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			int x = (dim.width - inspector.getBounds().width) / 2;
			int y = (dim.height - inspector.getBounds().height) / 2;
			inspector.setLocation(x, y);
		}
		inspector.initialize();
		inspector.setVisible(true);
	}

	private void doReadoutPopup() {
		if (disabled || readoutTypes.length < 2) {
			return;
		}
		// create popup menu and add menu items
		JPopupMenu popup = new JPopupMenu();
		JMenu displayMenu = new JMenu(MediaRes.getString("VideoPlayer.Readout.Menu.Display")); //$NON-NLS-1$
		popup.add(displayMenu);
		JMenuItem item;
		for (int i = 0; i < readoutTypes.length; i++) {
			String type = readoutTypes[i];
			if (type.equals("step")) { //$NON-NLS-1$
				item = new JCheckBoxMenuItem(MediaRes.getString("VideoPlayer.Readout.MenuItem.Step")); //$NON-NLS-1$
				item.setSelected(type.equals(readoutType));
				item.setActionCommand(type);
				item.addActionListener(readoutListener);
				displayMenu.add(item);
			} else if (type.equals("time")) { //$NON-NLS-1$
				item = new JCheckBoxMenuItem(MediaRes.getString("VideoPlayer.Readout.MenuItem.Time")); //$NON-NLS-1$
				item.setSelected(type.equals(readoutType));
				item.setActionCommand(type);
				item.addActionListener(readoutListener);
				displayMenu.add(item);

				popup.addSeparator();
				if (getTime() != 0) {
					String s = MediaRes.getString("VideoPlayer.Popup.Menu.SetTimeToZero"); //$NON-NLS-1$
					item = new JMenuItem(s);
					item.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							if (disabled)
								return;
							double t0 = -clipControl.getTime();
							getVideoClip().setStartTime(t0);
						}
					});
					item.addActionListener(readoutListener);
					popup.add(item);
				}

				item = new JMenuItem(MediaRes.getString("VideoPlayer.Readout.Menu.SetTime")); //$NON-NLS-1$
				item.setActionCommand(type);
				item.addActionListener(timeSetListener);
				item.addActionListener(readoutListener);
				popup.add(item);
				item = new JMenuItem(MediaRes.getString("VideoPlayer.Readout.Menu.GoTo") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
				item.setActionCommand(type);
				item.addActionListener(goToListener);
				popup.add(item);
			} else {
				item = new JCheckBoxMenuItem(MediaRes.getString("VideoPlayer.Readout.MenuItem.Frame")); //$NON-NLS-1$
				item.setSelected(type.equals(readoutType));
				item.setActionCommand(type);
				item.addActionListener(readoutListener);
				displayMenu.add(item);
			}
		}
		// show popup menu
		FontSizer.setFonts(popup);
		popup.show(readout, 0, readout.getHeight());
	}

	private void doSliderChanged() {
		if (clipControl.isPlaying()) {
			// BH 2021.01.11
			// nasty feedback loop here
			// - Video gets image -- PROPERTY_VIDEO_FRAMENUMBER -->
			// -- ClipControl -- PROPERTY_CLIPCONTROL_FRAMENUMBER -->
			// ----- VideoPlayer.updateValue()
			// ------ SliderUI -- JSlider.changeListener
			// ------- VideoPlayer.doSliderChanged()
			// --------- VideoPlayer.setStepNumber()
			// -----------ClipControl.setStepNumber()
			// -------------........
			return;
		}
		VideoClip clip = getVideoClip();
		int i = slider.getValue(); // frame number
		if (i < clip.getStartFrameNumber()) {
			slider.setValue(clip.getStartFrameNumber());
			return;
		}
		if (i > clip.getEndFrameNumber()) {
			slider.setValue(clip.getEndFrameNumber());
			return;
		}
		OSPRuntime.setTimeout("VP-sliderState", 25, true, () -> {
			int stepNo = clip.frameToStep(i);
			int frameNo = clip.stepToFrame(stepNo);
			int currentStep = getStepNumber();
			boolean isIncluded = clip.includesFrame(i);
			if (stepNo != currentStep && !disabled) {
				setStepNumber(stepNo);
				return;
			}
			if (!isIncluded) {
				slider.setValue(frameNo);
				return;
			}
		});

	}

	protected ActionListener stepSizeBtnListener = (e) -> {
			int frameNumber = getFrameNumber();
			VideoClip clip = getVideoClip();
			try {
				clip.setStepSize(Integer.parseInt(e.getActionCommand()));
			} catch (NumberFormatException ex) {
				String cur = String.valueOf(getVideoClip().getStepSize());
				String input = GUIUtils.showInputDialog(vidPanel,
						MediaRes.getString("VideoPlayer.Dialog.StepSize.Message"), //$NON-NLS-1$
						MediaRes.getString("VideoPlayer.Dialog.StepSize.Title"), //$NON-NLS-1$
						JOptionPane.PLAIN_MESSAGE, cur);
				if (input != null) {
					int n = Integer.parseInt(input);
					clip.setStepSize(n);
				}
			}
			setStepNumber(clip.frameToStep(frameNumber));
			if (clip.inspector != null && clip.inspector.isVisible()) {
				clip.inspector.stepSizeField.setValue(clip.getStepSize());
			}
	};

	protected void sliderMovedAction(MouseEvent e) {
		sliderCaret = null;
		if (disabled)
			return;
		int yMin = slider.getHeight() - inLabel.getHeight() - 2;
		if (sliderInset == 0)
			sliderInset = slider.getInsets().left + 7;
		if (e.getY() > yMin) {
			int x = e.getX();
			VideoClip clip = getVideoClip();
			double pixPerFrame = (slider.getWidth() - 2.0 * sliderInset) / (clip.getFrameCount() - 1);
			String hint = " " + MediaRes.getString("VideoPlayer.InOutMarker.ToolTip"); //$NON-NLS-1$//$NON-NLS-2$
			int start = getVideoClip().getStartFrameNumber();
			int end = getVideoClip().getEndFrameNumber();
			int xend = (int) (sliderInset + end * pixPerFrame);
			int xstart = (int) (sliderInset + start * pixPerFrame);
			if ((xstart != xend || start == 0) && x < xend + 8 && x > xend - 8) {
				sliderCaret = "out"; //$NON-NLS-1$
				slider.setToolTipText(MediaRes.getString("VideoPlayer.OutMarker.ToolTip") + ": " + end + hint); //$NON-NLS-1$ //$NON-NLS-2$
			} else if (x < xstart + 8 && x > xstart - 8) {
				sliderCaret = "in"; //$NON-NLS-1$
				slider.setToolTipText(MediaRes.getString("VideoPlayer.InMarker.ToolTip") + ": " + start + hint); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		if (sliderCaret == null) {
			// slideMouseMotionListener.mouseMoved(e);
			vidPanel.setMouseCursor(Cursor.getDefaultCursor());
			slider.setToolTipText(MediaRes.getString("VideoPlayer.Slider.ToolTip")); //$NON-NLS-1$
		} else {
			vidPanel.setMouseCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			e.consume();
		}
	}

	protected void sliderPressedAction(MouseEvent e) {
		if (disabled)
			return;
		stop();
		int frameNum = clipControl.getFrameNumber();
		maxEndFrame = getVideoClip().getEndFrameNumber();
		int start = getVideoClip().getStartFrameNumber();
		int end = getVideoClip().getEndFrameNumber();
		// BH - reverse sense if we are dragging through the other end
		if (sliderCaret == "out" && frameNum < start)
			sliderCaret = "in";
		else if (sliderCaret == "in" && frameNum > end)
			sliderCaret = "out";
		if (OSPRuntime.isPopupTrigger(e)) {
			// inner popup menu listener classes
			// create popup menu and add menu items
			JPopupMenu popup = new JPopupMenu();
			JMenuItem item = new JMenuItem(MediaRes.getString("ClipInspector.Title") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
			item.addActionListener((ea) -> {
				if (disabled)
					return;
				Frame frame = null;
				Container c = vidPanel.getTopLevelAncestor();
				if (c instanceof Frame) {
					frame = (Frame) c;
				}
				ClipInspector inspector = getVideoClip().getClipInspector(clipControl, frame);
				if (inspector.isVisible()) {
					return;
				}
				Point p0 = new Frame().getLocation();
				Point loc = inspector.getLocation();
				if ((loc.x == p0.x) && (loc.y == p0.y)) {
					// center on screen
					Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
					int x = (dim.width - inspector.getBounds().width) / 2;
					int y = (dim.height - inspector.getBounds().height) / 2;
					inspector.setLocation(x, y);
				}
				inspector.initialize();
				inspector.setVisible(true);
			});
			popup.add(item);
			popup.addSeparator();
			boolean showTrim = false;
			if (getVideoClip().getVideo() == null || getVideoClip().getVideo().getFrameCount() == 1) {
				if (getVideoClip().getFrameCount() > getVideoClip().getEndFrameNumber() + 1) {
					showTrim = true;
				}
			}

			if (showTrim) {
				String s = MediaRes.getString("VideoPlayer.Slider.Popup.Menu.TrimFrames"); //$NON-NLS-1$
				item = new JMenuItem(s);
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						getVideoClip().trimFrameCount();
					}
				});
				popup.add(item);
				popup.addSeparator();
			}

			if (popupItemListener == null)
				popupItemListener = new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						VideoClip clip = getVideoClip();
						int val = clipControl.getFrameNumber();
						if ("in".equals(e.getActionCommand())) { //$NON-NLS-1$
							clip.setStartFrameNumber(val, maxEndFrame);
							if (clip.inspector != null && clip.inspector.isVisible()) {
								clip.inspector.startField.setValue(clip.getStartFrameNumber());
							}
						} else {
							clip.setEndFrameNumber(val);
							if (clip.inspector != null && clip.inspector.isVisible()) {
								clip.inspector.endField.setValue(clip.getEndFrameNumber());
							}
						}
						refresh();
					}
				};
			boolean isIn = ("in".equals(sliderCaret)); // $NON-NLS-1$
			if (sliderCaret == null || isIn) {
				String s = MediaRes.getString("VideoPlayer.Slider.Popup.Menu.SetIn"); //$NON-NLS-1$
				s += " (" + frameNum + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				item = new JMenuItem(s);
				item.setActionCommand("in");
				item.addActionListener(popupItemListener);
				popup.add(item);
			}
			if (!isIn) { 
				sliderCaret = "out";
				String s = MediaRes.getString("VideoPlayer.Slider.Popup.Menu.SetOut"); //$NON-NLS-1$
				s += " (" + frameNum + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				item = new JMenuItem(s);
				item.setActionCommand("out"); //$NON-NLS-1$
				item.addActionListener(popupItemListener);
				popup.add(item);
			}
			sliderCaret = null;

			boolean includeTimeItems = false;
			for (String type : readoutTypes) {
				if (type.equals("time")) //$NON-NLS-1$
					includeTimeItems = true;
			}
			if (includeTimeItems) {
				// set frame time to zero
				popup.addSeparator();
				if (getTime() != 0) {
					String s = MediaRes.getString("VideoPlayer.Popup.Menu.SetTimeToZero"); //$NON-NLS-1$
					item = new JMenuItem(s);
					item.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							if (disabled)
								return;
							double t0 = -clipControl.getTime();
							getVideoClip().setStartTime(t0);
						}
					});
					item.addActionListener(readoutListener);
					popup.add(item);
				}
				// set frame time
				item = new JMenuItem(MediaRes.getString("VideoPlayer.Readout.Menu.SetTime")); //$NON-NLS-1$
				item.setActionCommand("time"); //$NON-NLS-1$
				item.addActionListener(timeSetListener);
				item.addActionListener(readoutListener);
				popup.add(item);
			}
			// show popup menu
			FontSizer.setFonts(popup);
			popup.show(slider, e.getX(), e.getY());
			return;
		}
		if (sliderCaret != null) {
			// BH no! slideMouseListener.mousePressed(e);
			stop();
			mouseX = e.getX();
			switch (sliderCaret) {
			case "in": //$NON-NLS-1$
				vidPanel.setMessage(MediaRes.getString("VideoPlayer.InMarker.ToolTip") + ": " + start); //$NON-NLS-1$ //$NON-NLS-2$
				if (start != frameNum)
					SwingUtilities.invokeLater(() -> {
						slider.setValue(start);
					});
				break;
			case "out": //$NON-NLS-1$
				vidPanel.setMessage(MediaRes.getString("VideoPlayer.OutMarker.ToolTip") + ": " + end); //$NON-NLS-1$ //$NON-NLS-2$
				if (end != frameNum)
					SwingUtilities.invokeLater(() -> {
						slider.setValue(end);
					});
				break;
			}
			e.consume();
		}
	}

	protected void sliderDraggedAction(MouseEvent e) {
		if (disabled || sliderCaret == null) {
			return;
		}
		VideoClip clip = getVideoClip();
		clip.setAdjusting(true);
		int start = clip.getStartFrameNumber();
		int end = clip.getEndFrameNumber();
		boolean increasing = e.getX() > mouseX;
		mouseX = e.getX();
		int lastFrame = clip.getFrameCount() - 1;
		int val = Math
				.round(lastFrame * (e.getX() - sliderInset) / (slider.getWidth() - 2 * sliderInset));
		if (start == end)
			sliderCaret = (increasing ? "out" : "in");
		if (increasing) {
			val = Math.min(val, lastFrame + getVideoClip().getStepSize());
		} else {
			val = Math.min(val, lastFrame);
		}
		val = Math.max(val, 0);
		switch (sliderCaret) {
		case "in": //$NON-NLS-1$
			if (clip.setStartFrameNumber(val, maxEndFrame)) {
				int newStart = clip.getStartFrameNumber();
				SwingUtilities.invokeLater(() -> {
				vidPanel.setMessage(MediaRes.getString("VideoPlayer.InMarker.ToolTip") + ": " //$NON-NLS-1$ //$NON-NLS-2$
				+ newStart);
				});
				// reset start time if needed
				if (!clip.isDefaultStartTime) {
					double startTime = clip.getStartTime();
					startTime += (newStart - start) * clipControl.getMeanFrameDuration();
					clip.setStartTime(startTime);
				}
				clipControl.setStepNumber(0);
				if (clip.inspector != null && clip.inspector.isVisible()) {
					clip.inspector.startField.setValue(newStart);
					clip.inspector.t0Field.setValue(clip.getStartTime() / 1000);
				}
				updateValue();
			}
			break;
		case "out": //$NON-NLS-1$
			if (clip.setEndFrameNumber(val)) {
				vidPanel.setMessage(MediaRes.getString("VideoPlayer.OutMarker.ToolTip") + ": " + end); //$NON-NLS-1$ //$NON-NLS-2$
				clipControl.setStepNumber(clip.getStepCount() - 1);
				if (clip.inspector != null && clip.inspector.isVisible()) {
					clip.inspector.endField.setValue(clip.getEndFrameNumber());
				}
			}
			break;
		}
		e.consume();
	}

	protected void sliderReleasedAction(MouseEvent e) {
		if (disabled)
			return;
		if (sliderCaret == null) {
			// slideMouseListener.mouseReleased(e);
		} else {
			vidPanel.setMessage(null);
			e.consume();
		}
		getVideoClip().setAdjusting(false);
	}


//	boolean playing;

	/**
	 * Updates the play buttons based on the specified play state.
	 *
	 * @param playing <code>true</code> if the video is playing
	 */
	private void updatePlayButtonsLater(final boolean playing) {
		SwingUtilities.invokeLater(() -> {
			updatePlayButtonsPosted(playing);
		});
	}

	protected void updatePlayButtonsPosted(boolean playing) {
		//updatePosted = false;
		int stepCount = getVideoClip().getStepCount();
		boolean canPlay = stepCount > 1;
		playButton.setEnabled(canPlay && (playing || getStepNumber() < stepCount - 1));
		stepButton.setEnabled(canPlay && (playing || getStepNumber() < stepCount - 1));
		backButton.setEnabled(canPlay && (playing || getStepNumber() > 0));
		playButton.setSelected(playing);
		if (playing) {
			playButton.setToolTipText(MediaRes.getString("VideoPlayer.Button.Pause.ToolTip")); //$NON-NLS-1$
			playButton.setPressedIcon(pauseIcon);
			playButton.setIcon(pauseIcon);
		} else {
			playButton.setToolTipText(MediaRes.getString("VideoPlayer.Button.Play.ToolTip")); //$NON-NLS-1$
			playButton.setPressedIcon(playIcon);
			playButton.setIcon(playIcon);
		}
	}

	/**
	 * Updates the loop button based on the specified looping state.
	 *
	 * @param looping <code>true</code> if the video is looping
	 */
	private void updateLoopButton(boolean looping) {
		if (looping == loopButton.isSelected()) {
			return;
		}
		loopButton.setSelected(looping);
		if (looping) {
			loopButton.setPressedIcon(loopIcon);
			loopButton.setIcon(loopIcon);
		} else {
			loopButton.setPressedIcon(noloopIcon);
			loopButton.setIcon(noloopIcon);
		}
	}
	
	private void updateValueAndPlayButtons() {
		updateValue();
		updatePlayButtonsLater(clipControl.isPlaying());
	}

	/**
	 * Check that clipControl is within range and pin to start or end if needed,
	 * then update all GUI components to reflect this value;
	 */
	private void updateValue() {
		// update slider position
		int frameNumber = clipControl.getFrameNumber();
		int startFrame = getVideoClip().getStartFrameNumber();
		int endFrame = getVideoClip().getEndFrameNumber();
		if (frameNumber < startFrame)
			clipControl.setStepNumber(0);
		else if (frameNumber > endFrame)
			clipControl.setStepNumber(getVideoClip().getStepCount());
		slider.setValue(clipControl.getFrameNumber());
		// update readout
		int stepNumber = clipControl.getStepNumber();
		String display;
		if ("step".equals(readoutType)) { //$NON-NLS-1$
			if (stepNumber < 10) {
				display = "00" + stepNumber; //$NON-NLS-1$
			} else if (stepNumber < 100) {
				display = "0" + stepNumber; //$NON-NLS-1$
			} else {
				display = "" + stepNumber; //$NON-NLS-1$
			}
		} else if ("frame".equals(readoutType)) { //$NON-NLS-1$
			int n = clipControl.getFrameNumber();
			if (n < 10) {
				display = "00" + n; //$NON-NLS-1$
			} else if (n < 100) {
				display = "0" + n; //$NON-NLS-1$
			} else {
				display = "" + n; //$NON-NLS-1$
			}
		} else {
			// default readout is time
			// set formatting based on mean step duration
			if (timeFormat instanceof DecimalFormat) {
				DecimalFormat format = (DecimalFormat) timeFormat;
				double dur = getMeanStepDuration(); // millisec
				if (dur < 10) {
					format.applyPattern("0.00E0"); //$NON-NLS-1$
				} else if (dur < 100) {
					format.applyPattern(NumberField.DECIMAL_3_PATTERN);
				} else if (dur < 1000) {
					format.applyPattern(NumberField.DECIMAL_2_PATTERN);
				} else if (dur < 10000) {
					format.applyPattern(NumberField.DECIMAL_1_PATTERN);
				} else {
					format.applyPattern("0.00E0"); //$NON-NLS-1$
				}
			}
			display = timeFormat.format(getTime() / 1000.0);
		}
		readout.setText(display);
		// update rate spinner
		if (!ignoreRateSpinner)
			rateSpinner.setValue(getRate());
		// update stepSizeButton
		stepSizeButton.setText("" + getVideoClip().getStepSize()); //$NON-NLS-1$
		// set font sizes
		// update tooltips
		stepSizeButton.setToolTipText(MediaRes.getString("VideoPlayer.Button.StepSize.ToolTip")); //$NON-NLS-1$
		rateSpinner.setToolTipText(MediaRes.getString("VideoPlayer.Spinner.Rate.ToolTip")); //$NON-NLS-1$
		// if at last step, update play button
		if (stepNumber == getVideoClip().getStepCount() - 1)
			updatePlayButtonsLater(clipControl.isPlaying());
		FontSizer.setFonts(toolbar);
	}

	/**
	 * Updates the slider based on the current in and out points.
	 */
	private void updateSlider(String option, Object o) {
		// update slider

		//OSPLog.debug("VideoPlayer.updateSlider (Timeout) " + option + " " + o);
		Runnable r = new Runnable() {

			@Override
			public void run() {
				updateSliderAsync(option, o);
			}

		};
		
		if (o == null || o == "stepcount") {
			OSPRuntime.setTimeout("VP-updateSlider", 200, o != null, r);
		} else {
			r.run();
		}
	}

	protected void updateSliderAsync(String option, Object o) {
		VideoClip clip = getVideoClip();
		clip.setAdjusting(false);
		slider.setMinimum(0);
		slider.setMaximum(slider.getMinimum() + clip.getFrameCount() - 1);
		sliderLabels.clear();
		sliderLabels.put(Integer.valueOf(clip.getStartFrameNumber()), inLabel);
		sliderLabels.put(Integer.valueOf(clip.getEndFrameNumber()), outLabel);
		slider.repaint();
		switch (option) {
		case SLIDER_NEWCLIP:
			measuredRate = 0;
			firePropertyChange(PROPERTY_VIDEOPLAYER_VIDEOCLIP, (VideoClip) o, clip);
			System.gc();
			break;
		case SLIDER_REFRESH:
			setReadoutTypes("frame time step", "frame"); //$NON-NLS-1$ //$NON-NLS-2$
			refresh();
			break;
		case SLIDER_PROPERTY:
			// no additional changes
			break;
		}
	}

	public void showGoToDialog() {
		if (goToDialog == null) {
			goToDialog = new GoToDialog(this);
			// center dialog on videoPanel view
			Container c = getParent();
			while (c != null) {
				if (c instanceof JSplitPane) {
					Dimension dim = c.getSize();
					Point p = c.getLocationOnScreen();
					int x = (dim.width - goToDialog.getBounds().width) / 2;
					int y = (dim.height - goToDialog.getBounds().height) / 2;
					goToDialog.setLocation(p.x + x, p.y + y);
					break;
				}
				c = c.getParent();
			}
		} else {
			goToDialog.setPlayer(this);
		}
		goToDialog.setVisible(true);

	}

	/**
	 * GoToDialog inner class
	 */
	protected static class GoToDialog extends JDialog {

		static HashMap<VideoPlayer, String[]> prev = new HashMap<VideoPlayer, String[]>();

		VideoPlayer player;
		JButton okButton, cancelButton;
		JLabel frameLabel, timeLabel, stepLabel;
		JTextField frameField, timeField, stepField;
		KeyAdapter keyListener;
		FocusAdapter focusListener;
		String prevFrame, prevTime, prevStep;
		Color error_red = new Color(255, 140, 160);

		public GoToDialog(VideoPlayer vidPlayer) {
			super(JOptionPane.getFrameForComponent(vidPlayer.vidPanel), true);
			setPlayer(vidPlayer);
			JPanel contentPane = new JPanel(new BorderLayout());
			setContentPane(contentPane);
			// create buttons
			okButton = new JButton(DisplayRes.getString("GUIUtils.Ok")); //$NON-NLS-1$
			okButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String input = stepField.getText();
					if (input != null && !input.equals("")) //$NON-NLS-1$
						try {
							int n = Integer.parseInt(input);
							player.clipControl.setStepNumber(n);
							player.refresh();
						} catch (NumberFormatException ex) {
						}
					setVisible(false);
				}
			});
			cancelButton = new JButton(DisplayRes.getString("GUIUtils.Cancel")); //$NON-NLS-1$
			cancelButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
				}
			});

			// create key and focus listeners
			keyListener = new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					JTextField field = (JTextField) e.getSource();
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						okButton.doClick(0);
					} else {
						field.setBackground(Color.white);
					}
				}

				@Override
				public void keyReleased(KeyEvent e) {
					JTextField field = (JTextField) e.getSource();
					if (e.getKeyCode() != KeyEvent.VK_ENTER) {
						setValues(field);
					}
				}

			};
			focusListener = new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					JTextField field = (JTextField) e.getSource();
					field.setBackground(Color.white);
				}
			};

			// create input fields and labels
			frameField = new JTextField(6);
			frameField.addKeyListener(keyListener);
			frameField.addFocusListener(focusListener);
			timeField = new JTextField(6);
			timeField.addKeyListener(keyListener);
			timeField.addFocusListener(focusListener);
			stepField = new JTextField(6);
			stepField.addKeyListener(keyListener);
			stepField.addFocusListener(focusListener);
			frameLabel = new JLabel();
			timeLabel = new JLabel();
			stepLabel = new JLabel();

			// assemble
			Box box = Box.createVerticalBox();
			JPanel framePanel = new JPanel();
			framePanel.add(frameLabel);
			framePanel.add(frameField);
			box.add(framePanel);
			JPanel timePanel = new JPanel();
			timePanel.add(timeLabel);
			timePanel.add(timeField);
			box.add(timePanel);
			JPanel stepPanel = new JPanel();
			stepPanel.add(stepLabel);
			stepPanel.add(stepField);
			box.add(stepPanel);
			contentPane.add(box, BorderLayout.CENTER);

			JPanel buttonPanel = new JPanel();
			buttonPanel.add(okButton);
			buttonPanel.add(cancelButton);
			contentPane.add(buttonPanel, BorderLayout.SOUTH);
			refreshGUI();
			pack();
		}

		public void refreshGUI() {
			setTitle(MediaRes.getString("VideoPlayer.GoToDialog.Title")); //$NON-NLS-1$
			okButton.setText(DisplayRes.getString("GUIUtils.Ok")); //$NON-NLS-1$
			cancelButton.setText(DisplayRes.getString("GUIUtils.Cancel")); //$NON-NLS-1$
			frameLabel.setText(MediaRes.getString("VideoPlayer.Readout.MenuItem.Frame") + ":"); //$NON-NLS-1$ //$NON-NLS-2$
			String t = player.vidPanel.getTimeUnit();
			timeLabel.setText(MediaRes.getString("VideoPlayer.Readout.MenuItem.Time") + " ("+t+"):"); //$NON-NLS-1$ //$NON-NLS-2$
			stepLabel.setText(MediaRes.getString("VideoPlayer.Readout.MenuItem.Step") + ":"); //$NON-NLS-1$ //$NON-NLS-2$
			
			// set label sizes
			ArrayList<JLabel> labels = new ArrayList<JLabel>();
			labels.add(frameLabel);
			labels.add(timeLabel);
			labels.add(stepLabel);
			Font font = frameLabel.getFont();
			// display panel labels
			int w = 0;
			for (Iterator<JLabel> it = labels.iterator(); it.hasNext();) {
				JLabel next = it.next();
				Rectangle2D rect = font.getStringBounds(next.getText() + " ", OSPRuntime.frc); //$NON-NLS-1$
				w = Math.max(w, (int) rect.getWidth() + 1);
			}
			Dimension labelSize = new Dimension(w, 20);
			for (Iterator<JLabel> it = labels.iterator(); it.hasNext();) {
				JLabel next = it.next();
				next.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
				next.setPreferredSize(labelSize);
				next.setHorizontalAlignment(SwingConstants.TRAILING);
			}

		}

		public void setPlayer(VideoPlayer vidPlayer) {
			if (player != null && player != vidPlayer) {
				prev.put(player, new String[] { prevFrame, prevTime, prevStep });
				String[] former = prev.get(vidPlayer);
				if (former != null) {
					prevFrame = former[0];
					prevTime = former[1];
					prevStep = former[2];
					frameField.setText(prevFrame);
					timeField.setText(prevTime);
					stepField.setText(prevStep);
				}
			}
			player = vidPlayer;
		}

		private void setValues(JTextField inputField) {
			String input = inputField.getText();
			if ("".equals(input)) { //$NON-NLS-1$
				prevFrame = ""; //$NON-NLS-1$
				prevTime = ""; //$NON-NLS-1$
				prevStep = ""; //$NON-NLS-1$
			} else {
				VideoClip clip = player.getVideoClip();
				if (inputField == frameField) {
					prevFrame = input;
					try {
						int frameNum = Integer.parseInt(input);
						int entered = frameNum;
						frameNum = Math.max(clip.getFirstFrameNumber(), frameNum);
						frameNum = Math.min(clip.getEndFrameNumber(), frameNum);
						int stepNum = clip.frameToStep(frameNum);
						frameNum = clip.stepToFrame(stepNum);
						double t = player.getStepTime(stepNum) / 1000;
						prevTime = timeFormat.format(t);
						prevStep = String.valueOf(stepNum);
						if (frameNum != entered) {
							frameField.setBackground(error_red);
						}
					} catch (NumberFormatException ex) {
						prevTime = ""; //$NON-NLS-1$
						prevStep = ""; //$NON-NLS-1$
						frameField.setBackground(error_red);
					}
				} else if (inputField == timeField) {
					prevTime = input;
					try {
						input = input.replaceAll(",", "."); //$NON-NLS-1$ //$NON-NLS-2$
						double t = Double.valueOf(input) * 1000; // millisec
						// find step number
						double dt = player.getMeanStepDuration();
						int n = (int) ((t - clip.getStartTime()) / dt);
						int stepNum = Math.max(0, n);
						stepNum = Math.min(stepNum, clip.getStepCount() - 1);
						int frameNum = clip.stepToFrame(stepNum);
						double tmin = player.getFrameTime(clip.getFirstFrameNumber());
						double tmax = player.getFrameTime(clip.getLastFrameNumber());
						if (t < tmin || t > tmax) {
							timeField.setBackground(error_red);
						}
						prevFrame = String.valueOf(frameNum);
						prevStep = String.valueOf(stepNum);
					} catch (NumberFormatException ex) {
						prevFrame = ""; //$NON-NLS-1$
						prevStep = ""; //$NON-NLS-1$
						timeField.setBackground(error_red);
					}
				} else {
					try {
						int stepNum = Integer.parseInt(input);
						stepNum = Math.max(0, stepNum);
						stepNum = Math.min(clip.getStepCount() - 1, stepNum);
						int frameNum = clip.stepToFrame(stepNum);
						double t = player.getStepTime(stepNum) / 1000;
						prevFrame = String.valueOf(frameNum);
						prevTime = timeFormat.format(t);
						prevStep = String.valueOf(stepNum);
					} catch (NumberFormatException ex) {
					}
				}
			}
			frameField.setText(prevFrame);
			timeField.setText(prevTime);
			stepField.setText(prevStep);
		}

		@Override
		public void setVisible(boolean vis) {
			if (vis) {
				prevFrame = ""; //$NON-NLS-1$
				prevTime = ""; //$NON-NLS-1$
				prevStep = ""; //$NON-NLS-1$
				frameField.setText(prevFrame);
				timeField.setText(prevTime);
				stepField.setText(prevStep);
				frameField.setBackground(Color.white);
				timeField.setBackground(Color.white);
				stepField.setBackground(Color.white);
				FontSizer.setFonts(this, FontSizer.getLevel());
				refreshGUI();
				pack();
			}
			super.setVisible(vis);
		}

	}

	public void dispose() {
	//	System.out.println("VideoPlayer.dispose");
		clipControl.dispose();
		clipControl = null;
		if (toolbar != null)
			toolbar.removeAll();
		toolbar = null;
		vidPanel = null;
	}

	@Override
	public void finalize() {
		OSPLog.finalized(this);
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
