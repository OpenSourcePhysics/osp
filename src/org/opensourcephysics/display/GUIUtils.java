/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreePath;

import org.jibble.epsgraphics.EpsGraphics2D;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.media.core.NumberField;

import swingjs.api.js.DOMNode;

public class GUIUtils {

	/**
	 * Installs mouse and touch dragging on a panel within a JFrame or JDialog.
	 * 
	 * Allows dragging the window by dragging anywhere inside the dialog while
	 * preserving interactive controls (buttons, editable text fields, scrollbars,
	 * and tree node selection / folder expansion).
	 * 
	 * This includes resizing along edges if the frame or dialog is resizable
	 * 
	 * @author ChatGPT+, adapted by Bob Hanson
	 */
	public static class FullFrameDragHandler {
	
		private static String DRAG_LISTENER_KEY = "Full_Frame_Drag_Listener_Installed"; 
		private static String WINDOW_LISTENER_KEY = "Full_Frame_Drag_WindowListener_Attached";
		private static String STATE_CHANGED_KEY = "Full_Frame_Drag_Tab_State_Listenerer_Installed"; 
	
		/**
		 * Installs the drag handler and welcome screen renderer on the given JPanel
		 * instance.
		 * 
		 * @param c the JPanel instance
		 */
		public static void installFullFrameDragHandler(Window window, JComponent c) {
			if (window == null)
				window = (Window) c.getTopLevelAncestor();
			if (!(window instanceof JFrame) && !(window instanceof JDialog))
				return;
			attachToWindow(window);			
			attachThis(c, window);
		}
	
		public static void attachThis(JComponent c, Window window) {
			// Attach to panel and all existing chidren in the window
			attachThisAndChildHierarchy(c, window);
			// Listen for tab changes 
			attachStateListeners(c, window);
		}

		private static void attachThisAndChildHierarchy(JComponent comp, Window w) {
			if (comp == null)
				return;
	
			// Attach to this component if eligible
			attachFullFrameDragAdapter(comp, w);
	
			if (comp instanceof JScrollPane) {
				JScrollPane sp = (JScrollPane) comp;
				JViewport vp = sp.getViewport();
				if (vp != null) {
					attachThis(vp, w);
					if (vp.getView() != null) {
						attachThisAndChildHierarchy((JComponent) vp.getView(), w);
					}
				}
			} 
			if (comp instanceof Container) {
				Container c = (Container) comp;
				boolean isSplitPane = (c instanceof JSplitPane);
				// BH the first component of a JSplitPane is always its UI-based divider
				for (Component child : c.getComponents()) {
					if ((child instanceof JComponent) &&  (!isSplitPane || child.getClass().getName().indexOf("Divider") < 0))
						attachThisAndChildHierarchy((JComponent) child, w);
				}
			}
		}
	
		/**
		 * Attaches drag listeners and window listeners to the window, root pane, and
		 * layered pane.
		 */
		private static void attachToWindow(Window w) {
			// exclude JApplet, JInternalFrame, JLightweightFrame, JWindow
			if (w instanceof RootPaneContainer) {
				JRootPane rp = ((RootPaneContainer) w).getRootPane();
				if (rp != null) {
					if (rp.getClientProperty(WINDOW_LISTENER_KEY) != null) {
						return; // already attached
					}
					rp.putClientProperty(WINDOW_LISTENER_KEY, Boolean.TRUE);
	
					attachFullFrameDragAdapter(rp, w);
					if (rp.getLayeredPane() != null) {
						attachFullFrameDragAdapter(rp.getLayeredPane(), w);
					}
					if (rp.getContentPane() != null) {
						attachFullFrameDragAdapter((JComponent)rp.getContentPane(), w);
					}
				}
			}
		}
	
		private static void attachStateListeners(JComponent root, Window w) {
			if (root == null)
				return;
			boolean isTabbedPane = false;
			if (root instanceof JViewport || (isTabbedPane = (root instanceof JTabbedPane))) {
				if (root.getClientProperty(STATE_CHANGED_KEY) == null) {
					root.putClientProperty(STATE_CHANGED_KEY, Boolean.TRUE);
					root.addContainerListener(new ContainerAdapter() {
						@Override
						public void componentAdded(ContainerEvent e) {
							attachThisAndChildHierarchy((JComponent) e.getChild(), w);
						}
					});
					if (isTabbedPane) {
						JTabbedPane tabbedPane = (JTabbedPane) root;
						tabbedPane.addChangeListener(new ChangeListener() {
							@Override
							public void stateChanged(ChangeEvent e) {
								attachThisAndChildHierarchy((JComponent) tabbedPane.getSelectedComponent(), w);
							}
						});
					} else {
						JViewport vp = (JViewport) root;
						vp.addChangeListener(new ChangeListener() {
							@Override
							public void stateChanged(ChangeEvent e) {
								attachThisAndChildHierarchy((JComponent) vp.getView(), w);
							}
						});						
					}
				}
			}
			for (Component child : root.getComponents()) {
				if (child instanceof JComponent) {
					attachStateListeners((JComponent) child, w);
				}
			}
		}
	
		/**
		 * Determines whether a component should receive the window drag listener.
		 * Interactive components (buttons, editable text fields, dropdowns, scrollbars,
		 * and UI-based SplitPaneDividers [excluded already])
		 * are excluded already so their primary interactions are preserved.
		 */
		private static boolean isEligibleForDrag(Component comp) {
			return (comp != null 
					&& !(comp instanceof AbstractButton)
					&& !(comp instanceof JComboBox)
					&& !(comp instanceof JScrollBar)
					&& !(comp instanceof JEditorPane 
							&& ((JTextComponent) comp).isEditable())
					&& !(comp instanceof JTextComponent 
							&& ((JTextComponent) comp).isEditable())
					
					);
		}
	
		private static void attachFullFrameDragAdapter(JComponent comp, Window window) {
			if (!isEligibleForDrag(comp))
				return;

			JComponent jc = (JComponent) comp;
			if (jc.getClientProperty(DRAG_LISTENER_KEY) != null) {
				return; // already attached
			}
			jc.putClientProperty(DRAG_LISTENER_KEY, Boolean.TRUE);
			FullFrameDragMouseAdapter adapter = new FullFrameDragMouseAdapter(window);
			comp.addMouseListener(adapter);
			comp.addMouseMotionListener(adapter);
		}
	
		/**
		 * MouseAdapter that implements dragging of the top-level window when pressing
		 * on empty space of the dialog, tree, or viewports.
		 * 
		 * This includes resizing along edges.
		 * 
		 */
		private static class FullFrameDragMouseAdapter extends MouseAdapter {
			private Window window;
			private Point mouseLoc = new Point();
			private Point windowLoc = new Point();
			private Dimension windowDim = new Dimension();
			private boolean isDragging = false;
			private boolean isResizing = false;
	
			public FullFrameDragMouseAdapter(Window w) {
				this.window = w;
			}
	
			@Override
			public void mousePressed(MouseEvent e) {
				isDragging = false;
				if (SwingUtilities.isRightMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) {
					return;
				}
				Component component = (Component) e.getSource();
				// If event is on a JTree, hit-test to see if user clicked a node
				if (component instanceof JTree) {
					JTree tree = (JTree) component;
					TreePath path = tree.getPathForLocation(e.getX(), e.getY());
					if (path != null) {
						return; // Clicked on a node -> normal tree selection / open
					}
					int row = tree.getClosestRowForLocation(e.getX(), e.getY());
					if (row >= 0) {
						Rectangle bounds = tree.getRowBounds(row);
						if (bounds != null && e.getY() >= bounds.y && e.getY() <= bounds.y + bounds.height
								&& e.getX() <= bounds.x + bounds.width) {
							return; // Clicked near row expander handle or node text
						}
					}
				}
				if (window == null)
					return;
				Point startPt = e.getLocationOnScreen();
				// BH added: Don't allow resizing if window is not resizable 
				if ((window instanceof JFrame) && ((JFrame) window).isResizable()
						|| (window instanceof JDialog) && ((JDialog) window).isResizable()) {
					Point ptInWindow = SwingUtilities.convertPoint(component, e.getPoint(), window);
					int cornerSize = 20;
					if (ptInWindow.x >= window.getWidth() - cornerSize && ptInWindow.y >= window.getHeight() - cornerSize) {
						mouseLoc.setLocation(startPt);
						windowDim.setSize(window.getSize());
						isResizing = true;
						isDragging = false;
						return;
					}
				}
				mouseLoc.setLocation(startPt);
				windowLoc.setLocation(window.getLocation());
				isDragging = true;
				isResizing = false;
			}
	
			@Override
			public void mouseDragged(MouseEvent e) {
				if (!isDragging && !isResizing)
					return;
				if (window != null) {
					Point curPt = e.getLocationOnScreen();
					if (curPt != null) {
						int dx = curPt.x - mouseLoc.x;
						int dy = curPt.y - mouseLoc.y;
						if (isResizing) {
							int newW = Math.max(400, windowDim.width + dx);
							int newH = Math.max(300, windowDim.height + dy);
							window.setSize(newW, newH);
							window.validate();
							window.repaint();
							// setupResizer(w);
						} else {
							window.setLocation(windowLoc.x + dx, windowLoc.y + dy);
							window.repaint();
						}
					}
				}
			}
	
			@Override
			public void mouseReleased(MouseEvent e) {
				isDragging = false;
				isResizing = false;
			}
		}
	
	}

	// colors used to "disable/enable" TitledBorders and other non-Components
	private static Color enabledColor, disabledColor;
	static {
		enabledColor = UIManager.getColor("Label.foreground"); //$NON-NLS-1$
		if (enabledColor == null)
			enabledColor = Color.BLACK;
		disabledColor = UIManager.getColor("Label.disabledForeground"); //$NON-NLS-1$
		if (disabledColor == null)
			disabledColor = UIManager.getColor("Label.disabledText"); //$NON-NLS-1$
		if (disabledColor == null)
			disabledColor = Color.LIGHT_GRAY;
	}

	private GUIUtils() {
	} // prohibits instantiation

	/**
	 * Converts TeX-like notation for Greek symbols to unicode characters.
	 * 
	 * @param input
	 * @return
	 * @deprecated use TeXParser class.
	 */
	@Deprecated
	public static String parseTeX(String input) {
		return TeXParser.parseTeX(input);
	}

	/**
	 * Removes TeX subscripting from the input.
	 * 
	 * @param input
	 * @return
	 * @deprecated use TeXParser class.
	 */
	@Deprecated
	public static String removeSubscripting(String input) {
		return TeXParser.removeSubscripting(input);
	}

	/**
	 * Finds an instance of a class in the given container.
	 *
	 * @param container Container
	 * @param c         Class
	 * @return Component
	 */
	public static Component findInstance(Container container, Class<?> c) {
		if ((container == null) || c.isInstance(container)) {
			return container;
		}
		Component[] components = container.getComponents();
		for (int i = 0, n = components.length; i < n; i++) {
			if (c.isInstance(components[i])) {
				return components[i];
			}
			if (components[i] instanceof Container) {
				Component comp = findInstance((Container) components[i], c);
				if (c.isInstance(comp)) {
					return comp;
				}
			}
		}
		return null;
	}

	/**
	 * Shows all drawing and table frames.
	 *
	 * Usually invoked when a model is initialized but may be invoked at other times
	 * to show frames that have been closed.
	 */
	public static void showDrawingAndTableFrames() {
		Frame[] frames = Frame.getFrames();
		for (int i = 0; i < frames.length; i++) {
			if (!frames[i].isDisplayable()) {
				continue;
			}
			if ((frames[i].getName() != null) && (frames[i].getName().indexOf("Tool") > -1)) { //$NON-NLS-1$
				continue;
			}
			if (OSPFrame.class.isInstance(frames[i])) {
				if (DataTableFrame.class.isInstance(frames[i])) {
					((DataTableFrame) frames[i]).refreshTable(DataTable.MODE_MODEL);
				}
				frames[i].setVisible(true);
				((OSPFrame) frames[i]).invalidateImage(); // make sure buffers are up to date
				frames[i].repaint(); // repaint if frame is already showing
				frames[i].toFront();
			}
		}
		if (OSPRuntime.isApplet) {
			OSPRuntime.applet.getRootPane().repaint();
		}
	}

	/**
	 * Renders all OSPFrames whose animated property is true.
	 *
	 * Usually invoked by an animation thread after every animation step.
	 */
	public static void renderAnimatedFrames() {
		Frame[] frames = Frame.getFrames();
		for (int i = 0; i < frames.length; i++) {
			if (!frames[i].isDisplayable() || !OSPFrame.class.isInstance(frames[i])) {
				continue;
			}
			if (((OSPFrame) frames[i]).isAnimated()) {
				((OSPFrame) frames[i]).render();
			}
		}
		if (OSPRuntime.isApplet && OSPRuntime.applet instanceof Renderable) {
			((Renderable) OSPRuntime.applet).render();
		}
	}

	/**
	 * Repaints all OSPFrames whose animated property is true.
	 *
	 * Usually invoked by a control's single-step button.
	 */
	public static void repaintAnimatedFrames() {
		Frame[] frames = Frame.getFrames();
		for (int i = 0; i < frames.length; i++) {
			if (!frames[i].isDisplayable() || !OSPFrame.class.isInstance(frames[i])) {
				continue;
			}
			if (((OSPFrame) frames[i]).isAnimated()) {
				((OSPFrame) frames[i]).invalidateImage(); // make sure buffers are up to date
				((OSPFrame) frames[i]).repaint();
			}
		}
	}

	/**
	 * Repaints all OSPFrames.
	 */
	public static void repaintOSPFrames() {
		Frame[] frames = Frame.getFrames();
		for (int i = 0; i < frames.length; i++) {
			if (!frames[i].isVisible() || !frames[i].isDisplayable() || !OSPFrame.class.isInstance(frames[i])) {
				continue;
			}
			((OSPFrame) frames[i]).repaint();
		}
	}

	/**
	 * Clears the data in animated DrawingFrames and repaints the frame's content.
	 *
	 * All frames are cleared if <code> clearAll<\code> is true; otherwise only
	 * frames whose <code>autoClear<\code> flag is true will be cleared.
	 *
	 * @param clearAll clears all frames if true
	 */
	public static void clearDrawingFrameData(boolean clearAll) {
		Frame[] frames = Frame.getFrames();
		for (int i = 0; i < frames.length; i++) {
			if (!frames[i].isDisplayable()) {
				continue;
			}
			if (OSPFrame.class.isInstance(frames[i])) {
				OSPFrame frame = ((OSPFrame) frames[i]);
				if (clearAll || frame.isAutoclear()) {
					frame.clearDataAndRepaint();
				}
			}
		}
	}

	/**
	 * Sets the IgnorRepaint for all animated frames to the given value.
	 * 
	 * @param ignoreRepaint boolean
	 */
	public static void setAnimatedFrameIgnoreRepaint(boolean ignoreRepaint) {
		Frame[] frames = Frame.getFrames();
		for (int i = 0; i < frames.length; i++) {
			if (!frames[i].isDisplayable() || !DrawingFrame.class.isInstance(frames[i])) {
				continue;
			}
			if (((DrawingFrame) frames[i]).isAnimated()) {
				DrawingPanel dp = ((DrawingFrame) frames[i]).getDrawingPanel();
				if (dp != null) {
					dp.setIgnoreRepaint(ignoreRepaint);
				}
			}
		}
	}

	/**
	 * Enables and disables the menu bars in DrawingFrames and DrawingFrame3D.
	 *
	 * Usually invoked when a model is initialized but may be invoked at other
	 * times.
	 */
	public static void enableMenubars(boolean enable) {
		Frame[] frames = Frame.getFrames();
		for (int i = 0; i < frames.length; i++) {
			if (!frames[i].isDisplayable()) {
				continue;
			}
			if ((frames[i].getName() != null) && (frames[i].getName().indexOf("Tool") > -1)) { //$NON-NLS-1$
				continue;
			}
			Class<?> frame3d = null;
			try {
				frame3d = Class.forName("org.opensourcephysics.display3d.core.DrawingFrame3D"); //$NON-NLS-1$
			} catch (ClassNotFoundException ex) {
			}
			if (DrawingFrame.class.isInstance(frames[i]) || ((frame3d != null) && frame3d.isInstance(frames[i]))) {
				JMenuBar bar = ((JFrame) frames[i]).getJMenuBar();
				if (bar != null) {
					for (int j = 0, n = bar.getMenuCount(); j < n; j++) {
						bar.getMenu(j).setEnabled(enable);
					}
				}
			}
		}
	}

	/**
	 * Disposes all OSP frames except the given frame.
	 *
	 * Usually invoked when the control window is being closed.
	 *
	 * @param frame will not be disposed
	 */
	public static void closeAndDisposeOSPFrames(Frame frame) {
		Frame[] frames = Frame.getFrames();
		for (int i = 0; i < frames.length; i++) {
			if (frames[i] == frame) {
				continue;
			}
			// if (frames[i] instanceof
			// org.opensourcephysics.controls.Launcher.LauncherFrame)continue;
			if (OSPFrame.class.isInstance(frames[i])) {
				((OSPFrame) frames[i]).setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				((OSPFrame) frames[i]).setVisible(false);
				((OSPFrame) frames[i]).dispose();
			}
		}
	}

	/**
	 * Pops up a "Save File" file chooser dialog and takes user through process of
	 * saving a file.
	 *
	 * @param parent the parent component of the dialog, can be <code>null</code>;
	 *               see <code>showDialog</code> in class JFileChooser for details
	 * @return the file or null if an error occurred:
	 */
	public static File showSaveDialog(Component parent) {
		return showSaveDialog(parent, DisplayRes.getString("GUIUtils.Title.Save")); //$NON-NLS-1$
	}

	/**
	 * Pops up a "Save File" file chooser dialog and takes user through process of
	 * saving a file.
	 *
	 * @param parent the parent component of the dialog, can be <code>null</code>;
	 *               see <code>showDialog</code> in class JFileChooser for details
	 * @param title
	 * @return the file or null if an error occurred:
	 */
	public static File showSaveDialog(Component parent, String title) {
		// JFileChooser fileChooser = new JFileChooser();
		JFileChooser fileChooser = OSPRuntime.getChooser();
		// fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (fileChooser == null) {
			return null;
		}
		String oldTitle = fileChooser.getDialogTitle();
		fileChooser.setDialogTitle(title);
		int result = fileChooser.showSaveDialog(parent);
		fileChooser.setDialogTitle(oldTitle);
		if (result != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		OSPRuntime.chooserDir = fileChooser.getCurrentDirectory().toString();
		File file = fileChooser.getSelectedFile();
		/** j2sIgnore */
		{
			if (file.exists()) {
				int selected = JOptionPane.showConfirmDialog(parent,
						DisplayRes.getString("DrawingFrame.ReplaceExisting_message") + " " + file.getName() //$NON-NLS-1$ //$NON-NLS-2$
								+ DisplayRes.getString("DrawingFrame.QuestionMark"), //$NON-NLS-1$
						DisplayRes.getString("DrawingFrame.ReplaceFile_option_title"), //$NON-NLS-1$
						JOptionPane.YES_NO_CANCEL_OPTION);
				if (selected != JOptionPane.YES_OPTION) {
					return null;
				}
			}
		}
		return file;
	}
	
	/**
	 * Shows a file chooser and opens a file when running in Java.
	 * Not applicable when running in JavaScrpipt.
	 * @param parent
	 * @return
	 */
  public static File showOpenDialog(Component parent) {
  	if(OSPRuntime.isJS) {
  		return null;
  	}
    JFileChooser fileChooser = OSPRuntime.getChooser(); // new JFileChooser();
    int result = fileChooser.showOpenDialog(parent);
    if(result!=JFileChooser.APPROVE_OPTION) {
      return null;
    }
    OSPRuntime.chooserDir = fileChooser.getCurrentDirectory().toString();
    File file = fileChooser.getSelectedFile();
    return file;
  }

	/**
	 * Returns the enabled text color.
	 * 
	 * @return the enabled color
	 */
	public static Color getEnabledTextColor() {
		return enabledColor;
	}

	/**
	 * Returns the disabled text color.
	 * 
	 * @return the disabled color
	 */
	public static Color getDisabledTextColor() {
		return disabledColor;
	}

	/**
	 * Test the time to render a drawable component.
	 * 
	 * @param drawable
	 */
	public static void timingTest(Drawable drawable) {
		DrawingPanel dp = new DrawingPanel();
		DrawingFrame df = new DrawingFrame(dp);
		df.setVisible(true);
		dp.addDrawable(drawable);
		dp.scale();
		dp.setPixelScale();
		Graphics g2 = dp.getGraphics();
		if (g2 == null) {
			return;
		}
		//long startTime = System.currentTimeMillis();
		drawable.draw(dp, g2); // first drawing often takes longer because of initialization
		//System.out.print("first drawing=" + (System.currentTimeMillis() - startTime)); //$NON-NLS-1$
		//startTime = System.currentTimeMillis(); // reset the time
		for (int i = 0; i < 5; i++) {
			drawable.draw(dp, g2);
		}
		//System.out.println("  avg time/drawing=" + ((System.currentTimeMillis() - startTime) / 5)); //$NON-NLS-1$
		g2.dispose();
	}

	/**
	 * Saves the contents of the specified component in the given file format. Note
	 * method requires Java 1.4
	 *
	 * @param comp
	 * @param outputFile       the output file
	 * @param outputFileFormat output file format. One of eps, gif, jpeg, or png
	 */
	public static void saveImage(JComponent comp, File outputFile, String outputFileFormat) throws IOException {
		FileOutputStream fos = null;
		try {
			// BH 2020.02.25 so that file name displays with the right extension initially
			File file = fixExtension(outputFile, outputFileFormat);
			fos = new FileOutputStream(file);
			if (outputFileFormat.equals("eps")) { //$NON-NLS-1$
				EpsGraphics2D g = new EpsGraphics2D("", fos, 0, 0, comp.getWidth(), comp.getHeight()); //$NON-NLS-1$
				comp.paint(g);
				g.scale(0.24, 0.24); // Set resolution to 300 dpi (0.24 = 72/300)
				// g.setColorDepth(EpsGraphics2D.BLACK_AND_WHITE); // Black & white
				g.close();
			} else {
				BufferedImage bi = new BufferedImage(comp.getWidth(), comp.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
				Graphics g = bi.getGraphics();
				comp.paint(g);
				g.dispose();
//				if (false && // BH 2020.02.25
//						outputFileFormat.equals("gif")) { //$NON-NLS-1$
//					GIFEncoder encoder = new GIFEncoder(bi);
//					encoder.Write(fos);
//				} else {
				ImageIO.write(bi, outputFileFormat, fos);
//				}
				fos.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}

	/**
	 * BH 2020.02.26 adding file extension if missing; ("jpeg" to "jpg")
	 * 
	 * @param file
	 * @param ext
	 * @return File object
	 */
	private static File fixExtension(File file, String ext) {
		String fileName = file.getAbsolutePath();
		String extension = XML.getExtension(fileName);
		if ((extension == null) || ".".equals(extension)) { //$NON-NLS-1$
			fileName = XML.stripExtension(fileName) + "." + (ext.equals("jpeg") ? "jpg" : ext); //$NON-NLS-1$
			file = new File(fileName);
		}
		return file;
	}

	/**
	 * Saves the contents of the specified component in the given file format. Pops
	 * open a save file dialog to allow the user to select the output file. Note
	 * method requires Java 1.4
	 *
	 * @param component        comp the component
	 * @param outputFileFormat output file format. One of eps, jpeg, or png
	 * @param parent           dialog parent
	 */
	public static void saveImage(JComponent component, String outputFileFormat, Component parent) {
		File outputFile = GUIUtils.showSaveDialog(component, DisplayRes.getString("GUIUtils.Title.SaveImage")); //$NON-NLS-1$
		if (outputFile == null) {
			return;
		}
		try {
			GUIUtils.saveImage(component, outputFile, outputFileFormat);
		} catch (IOException ioe) {
			JOptionPane.showMessageDialog(parent,
					"An error occurred while saving the file " + outputFile.getName() + ".'"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public static void saveImageAs(JComponent component, String type, String title, String description,
			String[] extensions) {
		JFileChooser chooser = OSPRuntime.createChooser(title, description, extensions);
		String fileName = OSPRuntime.chooseFilename(chooser);
		if (fileName == null) {
			return;
		}
		File file = fixExtension(new File(fileName), extensions[0]);
		/** j2sIgnore */
		{
			if (file.exists()) { // BH 2020.02.25
				int selected = JOptionPane.showConfirmDialog(null,
						DisplayRes.getString("DrawingFrame.ReplaceExisting_message") //$NON-NLS-1$
								+ " " + file.getName() + DisplayRes.getString("DrawingFrame.QuestionMark"), //$NON-NLS-1$ //$NON-NLS-2$
						DisplayRes.getString("DrawingFrame.ReplaceFile_option_title"), //$NON-NLS-1$
						JOptionPane.YES_NO_CANCEL_OPTION);
				if (selected != JOptionPane.YES_OPTION) {
					return;
				}
			}
		}
		try {
			GUIUtils.saveImage(component, file, type);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Creates a custom cursor from an image. If an exception occurs, a predefined
	 * cursor is returned.
	 *
	 * @param image                the Image
	 * @param hotspot              the position of the cursor hotspot
	 * @param name                 the name of the cursor
	 * @param predefinedCursorType one of the predefined Cursor types
	 */
	public static Cursor createCustomCursor(Image image, Point hotspot, String name, int predefinedCursorType) {
		try {
			return Toolkit.getDefaultToolkit().createCustomCursor(image, hotspot, name);
		} catch (Exception ex) {
			try {
				return Cursor.getPredefinedCursor(predefinedCursorType);
			} catch (Exception ex1) {
			}
		}
		return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
	}

	public static JTextPane newJTextPane() {
		return (OSPRuntime.antiAliasText ? new JTextPane() {
			@Override
			public void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				RenderingHints rh = g2.getRenderingHints();
				rh.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				super.paintComponent(g);
			}
			
			@Override
			public void setPage(URL url) throws IOException {
				if (!checkScrollPage(this, url))
					super.setPage(url);
			}
		} : new JTextPane() {
			// BH 2020.04.04 only problem here is that with the override, we get the wrong
			// background color. SwingJS detects that the
			// JTextPane has overridden paintComponent, which is not generally a problem,
			// but
			// in this case it is, because we can't paint the background via the graphics,
			// or so it seeems. Anyway, the solution is to move the check for antialiasing
			// text
			// outside the override.

			@Override
			public void setPage(URL url) throws IOException {
				if (!checkScrollPage(this, url))
					super.setPage(url);
			}
			
//			public void paintComponent(Graphics g) {
//				super.paintComponent(g);
//			}
//			
//			public void removeNotify() {
//				System.err.println("GUIUTILS pane removed " + getText());
//				super.removeNotify();
//			}
//			public void addNotify() {
//				System.err.println("GUIUTILS pane added " + getText());
//				super.addNotify();
//			}
//			public void setText(String text) {
//			System.out.println("GUIUTILS setting text to " + text);
//			super.setText(text);
//		}

		});
	}

	/**
	 * Don't reload the page if local and we are just scrolling
	 * @param pane
	 * @param url
	 * @return
	 */
	protected static boolean checkScrollPage(JTextPane pane, URL url) {
		URL stream = (URL) pane.getDocument().getProperty("stream");
		if (stream == null || url == null || !stream.getPath().equals(url.getPath())
				|| !"file".equals(stream.getProtocol())) {
			return false;
		}
		String reference = url.getRef();
		if (reference != null) {
			pane.scrollToReference(reference);
		}
		return true;
	}

	public static JTextArea newJTextArea() {
		return (OSPRuntime.antiAliasText ? new JTextArea() {
			@Override
			public void paintComponent(Graphics g) {
				{
					Graphics2D g2 = (Graphics2D) g;
					RenderingHints rh = g2.getRenderingHints();
					rh.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				}
				super.paintComponent(g);
			}

		} : new JTextArea()

		// BH 2020.04.04 only problem here is that with the override, we get the wrong
		// background color. SwingJS detects that the
		// JTextPane has overridden paintComponent, which is not generally a problem,
		// but
		// in this case it is, because we can't paint the background via the graphics,
		// or so it seeems. Anyway, the solution is to move the check for antialiasing
		// text
		// outside the override.
		);
	}

	public static JToolBar getParentToolBar(Container c) {
		while ((c = c.getParent()) != null && !(c instanceof JToolBar)) {
		}
		return (JToolBar) c;
	}

	// added by DB 2020.11.02
	public static JToolBar getParentOrSelfToolBar(Container c) {
		if (!(c instanceof JToolBar)) {
			while ((c = c.getParent()) != null && !(c instanceof JToolBar)) {
			}
		}
		return (JToolBar) c;
	}

	public static JViewport getParentViewport(Container c) {
		while ((c = c.getParent()) != null && !(c instanceof JViewport)) {
		}
		return (JViewport) c;
	}

	/**
	 * Allow a simple prompt in JavaScript
	 * @param c
	 * @param message
	 * @param title
	 * @param messageType
	 * @param value
	 * @return
	 * @throws HeadlessException
	 */
	public static String showInputDialog(Component c, String message, String title, int messageType, String value)
			throws HeadlessException {
		// we force no listener for JavaScript -- simple prompt.
		return (String) JOptionPane.showInputDialog(OSPRuntime.isJS ? null : c, message, title, messageType, null, null,
				value);
	}

	/**
	 * Align a component's right edge with the reference component or within the
	 * visible viewport.
	 * 
	 * Note that this does NOT set Java values, only JavaScript. It is called just
	 * after menu.show().
	 * 
	 * Version 2 -- adjusts horizontal and vertical
	 * 
	 * @author Bob Hanson
	 */
	public static void alignComponentRight(JComponent c, JComponent ref) {
		// offloaded to JSUtil.java
		Window container = SwingUtilities.getWindowAncestor(ref);
		if (container != null) {
			OSPRuntime.jsutil.alignComponentToContainer(c, container, 1, 1);
		}
	}

	/**
	 * Installs the window resize handler on the specified window.
	 *
	 * @param window the top-level Window (TFrame or JDialog)
	 */
	public static void installResizeHandler(JFrame window) {
	
		window.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				GUIUtils.setupResizer(window);
			}
	
			@Override
			public void componentResized(ComponentEvent e) {
				GUIUtils.setupResizer(window);
			}
		});
	}

	/**
	 * Installs the window resize handler on the specified window.
	 *
	 * @param window the top-level Window (TFrame or JDialog)
	 * 
	 * @author Bob Hanson
	 */
	public static void installGestureHandler(Window window) {
		if (window == null || !OSPRuntime.isJS)
			return;
	
		window.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				GUIUtils.isolateWindowGestures(window);
			}
	
			@Override
			public void componentResized(ComponentEvent e) {
				GUIUtils.isolateWindowGestures(window);
			}
		});
	}

	/**
	 * Isolates the window DOM node from propagating gestures to the host HTML page
	 * in SwingJS. Prevents pinch-to-zoom, two-finger pan, overscroll
	 * rubber-banding, and touch drag bubbling from escaping the Tracker window to
	 * the surrounding webpage.
	 *
	 * @param window the top-level Window (TFrame)
	 * 
	 * @author ChatGPT+
	 */
	public static void isolateWindowGestures(Window w) {
		OSPRuntime.jsutil.isolateWindowGestures(w);
	}

	public static void disposeElement(String id) {
		DOMNode.dispose(DOMNode.getElement(id));
	}

	public static void addWindowOrientationChangeListener(Runnable onOrient) {
		/**
		 * @j2sNative window.addEventListener(window.onorientationchange ?
		 *            "orientationchange" : "resize", function() {
		 *            console.log("Orientation changed"); onOrient.run$(); }, false);
		 */
	}

	public static void virtualNumberPad(NumberField nf, String action) {
		switch (action) {
		case "add":
			/**
			 * @j2sNative
			 * 
			 * 			J2S.Mobile.addNumberPad(nf);
			 */
			//$FALL-THROUGH$
		case "show":
			/**
			 * @j2sNative
			 * 
			 * 			J2S.Mobile.showNumberPad(nf);
			 */
		}
	}

	private final static String[] resizerNodeStyles = new String[] {
//			"background-color", "red", 
			 "z-index", "1000002",
			 "cursor", "nwse-resize",
			   "width", "30px",
			   "height", "30px",
			   "margin-left", "-15px",
			   "margin-top", "-15px",					
			   "background-image", "linear-gradient(red, red)",
			   "background-repeat", "no-repeat",
			   "background-position", "center",
			   "background-size", "10px 10px"
			};
	
	private final static String[] rubberBandStyles = new String[] {
			"border", "2px dashed #FF00FF", 
			"z-index", "1000002", 
			"position", "absolute",
			"left", "0px", 
			"top", "0px", 
			"display", "none", 
			"pointer-events", "none",
			"touch-action", "none",
			"user-select", "none",
			"webkit-user-select", "none",
			"cursor", "nwse-resize"			};

	
	private static final int MIN_WIDTH = 320;
	private static final int MIN_HEIGHT = 220;

	/**
	 * Configures the SwingJS window resizer DOM element for touch responsiveness
	 * and visual affordance.
	 *
	 * @param window the window whose resizer should be configured
	 */
	public static void setupResizer(Window frame) {
		if (frame == null || !OSPRuntime.isJS)
			return;
		// note that we can change these styles to anything we want
		boolean useAI = false;
	    OSPRuntime.jsutil.setFrameResizer(frame, MIN_WIDTH, MIN_HEIGHT, resizerNodeStyles, rubberBandStyles, useAI);
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
 * Copyright (c) 2026 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
