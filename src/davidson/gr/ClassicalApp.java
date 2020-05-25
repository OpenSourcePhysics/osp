package davidson.gr;

import org.opensourcephysics.controls.*;
import org.opensourcephysics.display.*;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.awt.Color;

import org.opensourcephysics.display.axes.PolarAxes;
import org.opensourcephysics.numerics.DoubleArray;
import java.awt.event.WindowEvent;
import javax.swing.event.SwingPropertyChangeSupport;
import java.beans.PropertyChangeSupport;

/**
 * ClassicalApp calculates and displays the classical trajectory of particle
 * near a spherically symmetric gravitational mass by extending
 * AbstractAnimation and implementing the doStep method.
 *
 * @author W. Christian
 * @version 1.0
 */
public class ClassicalApp extends AbstractSimulation implements InteractiveMouseHandler, PropertyChangeListener {

	static String PHIRATE = TeXParser.parseTeX("d$\\phi$/dt");
	static String PHI = TeXParser.parseTeX("$\\phi$");
	PropertyChangeSupport support = new SwingPropertyChangeSupport(this);
	DoubleArray scale = new DoubleArray("{-4,4,-4,4}");
	PlottingPanel plottingPanel = PlottingPanelFactory.createPolarType2("Newtonian Orbits", 1.0);
	DrawingFrame drawingFrame = new DrawingFrame("Particle Trajectories", plottingPanel);
	ArrayList<ClassicalTrajectory> trajectories = new ArrayList<>();
	ArrayList<ShellParticle> markers = new ArrayList<>();
	ClassicalTrajectory defaultTrajectory;
	ClassicalInspector inspector;
	boolean enableInspector = true;
	double dt = 0.1, time = 0;
	double maxTime = 1.0e5;

	/**
	 * Constructs ClassicalApp and initializes the drawing.
	 */
	public ClassicalApp() {
		plottingPanel.setAntialiasShapeOn(true);
		// plottingPanel.setAntialiasTextOn(true);
		plottingPanel.getCoordinateStringBuilder().setCoordinateLabels("r=", " $\\phi$=");
		drawingFrame.setSize(450, 450);
		drawingFrame.setAnimated(true);
		plottingPanel.setInteractiveMouseHandler(this);
	}

	/**
	 * Reinitializes the program if a property changes.
	 *
	 * Loading an XML file is a property change event.
	 *
	 * @param evt PropertyChangeEvent
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		boolean running = isRunning();
		if (evt.getPropertyName().equals("inspectorChange")) {
			if (!running) {
				inspector.initializeParticle(defaultTrajectory);
				plottingPanel.repaint();
			}
		}
		if (evt.getPropertyName().equals("xmlDefault")) { // EjsControlFrame fires this event when xml data changes
			if (running) {
				stopSimulation();
			}

			if (running) {
				startSimulation();
			}
		}
	}

	/**
	 * Does an animation step.
	 */
	@Override
	protected void doStep() {
		if (inspector != null && inspector.newInspectorData) {
			inspector.initializeParticle(defaultTrajectory);
		}
		for (int i = 0, n = trajectories.size(); i < n; i++) {
			trajectories.get(i).stepTime();
		}
		time += dt;
		plottingPanel.setMessage("t=" + ControlUtils.f2(time));
		if (defaultTrajectory != null) {
			if (inspector != null) {
				inspector.updateData(defaultTrajectory);
			}
			support.firePropertyChange("trajectoryChange", null, defaultTrajectory);
		}
		if (time >= maxTime) {
			control.calculationDone(null);
			stopSimulation();
			plottingPanel.setMessage("Simulation done.", DrawingPanel.TOP_LEFT);
		}
	}

	/**
	 * Resets the animation into a predefined state.
	 */
	@Override
	public void reset() {
		// set the parameters
		control.setValue("M", 1);
		control.setValue("dt", 0.1);
		control.setValue("editable inspector", true);
		control.setValue("autoscale", false);
		control.setValue("scale min max", scale.getDefault());
		control.setValue("maximum time", 1.0e4);
		enableStepsPerDisplay(true);
		initialize();
	}

	/**
	 * Initializes the animation;
	 */
	@Override
	public void initialize() {
		plottingPanel.setMessage(null, DrawingPanel.TOP_LEFT);
		// clears all trajectories
		plottingPanel.setMessage(null, DrawingPanel.TOP_LEFT);
		plottingPanel.removeDrawables(AbstractTrajectory.class); // XXX WC JS Bug
		trajectories.clear();
		markers.clear();
		maxTime = control.getDouble("maximum time");
		enableInspector = control.getBoolean("enable inspector");
		if ((inspector != null) && !enableInspector) {
			inspector.plot.setVisible(false);
		}
		dt = control.getDouble("dt");
		time = 0;
		plottingPanel.setMessage("t=" + ControlUtils.f2(time));
		if (control.getBoolean("autoscale")) {
			double[] array = scale.getArray(control.getString("scale min max"));
			((PolarAxes) plottingPanel.getAxes()).autospaceRings(true);
			plottingPanel.setAutoscaleX(true);
			plottingPanel.setAutoscaleY(true);
			plottingPanel.limitAutoscaleX(array[0], array[1]);
			plottingPanel.limitAutoscaleY(array[2], array[3]);
		} else {
			double[] array = scale.getArray(control.getString("scale min max"));
			((PolarAxes) plottingPanel.getAxes()).setDeltaR(1.0);
			plottingPanel.setPreferredMinMax(array[0], array[1], array[2], array[3]);
		}
	}

	/**
	 * Handles the mouse action.
	 *
	 * @param panel InteractivePanel
	 * @param evt   MouseEvent
	 */
	@Override
	public void handleMouseAction(InteractivePanel panel, MouseEvent evt) {
		if (evt.getClickCount() > 1) {
			if ((inspector == null) || !inspector.plot.isDisplayable() && defaultTrajectory != null) {
				inspector = new ClassicalInspector(this, defaultTrajectory);
				inspector.enableInteraction(control.getBoolean("editable inspector"));
				inspector.addPropertyChangeListener(this);
			} else {
				inspector.updateDataAndScale(defaultTrajectory);
				inspector.show();
			}
			defaultTrajectory.color = Color.MAGENTA;
		}
		panel.handleMouseAction(panel, evt);
		Interactive ia = panel.getInteractive();
		if (ia == null)
			return;
		if ((ia instanceof ClassicalTrajectory) && ((panel.getMouseAction() == InteractivePanel.MOUSE_CLICKED)
				|| (panel.getMouseAction() == InteractivePanel.MOUSE_PRESSED))) {
			defaultTrajectory.color = Color.RED;
			defaultTrajectory = (ClassicalTrajectory) ia;
			if ((inspector != null) && inspector.plot.isVisible()) {
				inspector.updateDataAndScale(defaultTrajectory);
			}
			defaultTrajectory.color = Color.MAGENTA;
			panel.repaint();
		}
		if ((ia == defaultTrajectory) && (inspector != null)
				&& (panel.getMouseAction() == InteractivePanel.MOUSE_DRAGGED)) {
			inspector.updateDataAndScale(defaultTrajectory);
		}
	}

	/**
	 * Adds a marker to the plot.
	 */
	public void createMarker() {
		final ShellParticle marker = new ShellParticle();
		marker.setStepSize(dt);
		OSPTableInspector editor = marker.edit();
		editor.setLocationRelativeTo(plottingPanel);

		final Control control = editor.getControl();
		editor.addWindowListener(new java.awt.event.WindowAdapter() {

			@Override
			public void windowClosed(WindowEvent e) {
				marker.setEnabled(control.getBoolean("draggable state"));
				marker.label = control.getString("label");
				double r = control.getDouble("r");
				double phi = control.getDouble(Trajectory.PHI);
				marker.setXY(r * Math.cos(phi), r * Math.sin(phi));
				markers.add(marker);
				plottingPanel.addDrawable(marker);
				plottingPanel.repaint();
			}
		});
		editor.setVisible(true);
		/* */
	}

	void createDefaultParticle() {
		defaultTrajectory = new ClassicalTrajectory();
		defaultTrajectory.setStepSize(dt);
		defaultTrajectory.M = control.getDouble("M");
		defaultTrajectory.initialize(new double[] { 1.0, 0.0, 0.0, 1.0, 0.0 });
		defaultTrajectory.color = Color.MAGENTA;
		trajectories.add(defaultTrajectory);
		plottingPanel.addDrawable(defaultTrajectory);
		if (inspector != null) {
			inspector.updateDataAndScale(defaultTrajectory);
		}
	}

	/**
	 * Adds a particle to the plot.
	 */
	public void createParticle() {
		final ClassicalTrajectory particle = new ClassicalTrajectory();
		particle.setStepSize(dt);
		particle.M = control.getDouble("M");
		OSPTableInspector editor = particle.edit();
		editor.setLocationRelativeTo(plottingPanel);
		final Control control = editor.getControl();
		editor.addWindowListener(new java.awt.event.WindowAdapter() {

			@Override
			public void windowClosed(WindowEvent e) {
				particle.setEnabled(control.getBoolean("draggable state"));
				particle.trackPoints = control.getInt("track points");
				particle.label = control.getString("label");
				double[] state = particle.getState();
				state[0] = control.getDouble("r");
				state[1] = control.getDouble("dr/dt");
				state[2] = control.getDouble(Trajectory.PHI);
				state[3] = control.getDouble(Trajectory.PHIRATE);
				state[4] = 0; // time
				particle.initialize(state);
				defaultTrajectory = particle;
				defaultTrajectory.color = Color.MAGENTA;
				trajectories.add(defaultTrajectory);
				plottingPanel.addDrawable(defaultTrajectory);
				if (inspector != null) {
					inspector.updateDataAndScale(defaultTrajectory);
				}
				support.firePropertyChange("trajectoryChange", null, defaultTrajectory);
				plottingPanel.repaint();
			}
		});
		editor.setVisible(true);
	}

	/**
	 * Returns an XML.ObjectLoader to save and load data for this program.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new ClassicalLoader();
	}

	/**
	 * Starts the Java application.
	 * 
	 * @param args command line parameters
	 */
	public static void main(String[] args) {
		OSPControl c = SimulationControl.createApp(new ClassicalApp(), args);
		c.addButton("createParticle", "Particle", "Adds a particle after the program has been initialized.");
		c.addButton("createMarker", "Marker", "Adds a shell marker after the program has been initialized.");
	}

	static class ClassicalLoader implements XML.ObjectLoader {

		/**
		 * Creates a ClassicalApp object.
		 *
		 * @param element XMLControl
		 * @return Object
		 */
		@Override
		public Object createObject(XMLControl element) {
			ClassicalApp app = new ClassicalApp();
			SimulationControl c = new SimulationControl(app);
			app.setControl(c);
			return app;
		}

		/**
		 * Saves data in the xml control.
		 *
		 * @param element XMLControl
		 * @param obj     Object
		 */
		@Override
		public void saveObject(XMLControl control, Object obj) {
			ClassicalApp app = (ClassicalApp) obj;
			control.setValue("trajectories", app.trajectories);
			control.setValue("markers", app.markers);
		}

		/**
		 * Loads data from the xml control.
		 *
		 * @param element XMLControl
		 * @param obj     Object
		 * @return Object
		 */
		@SuppressWarnings("unchecked")
		@Override
		public Object loadObject(XMLControl control, Object obj) {
			ClassicalApp app = (ClassicalApp) obj;
			app.initialize();
			app.trajectories = (ArrayList<ClassicalTrajectory>) control.getObject("trajectories");
			app.markers = (ArrayList<ShellParticle>) control.getObject("markers");
			int n = app.markers.size();
			for (int i = 0; i < n; i++) {
				app.plottingPanel.addDrawable((ShellParticle) app.markers.get(i));
			}
			n = app.trajectories.size();
			for (int i = 0; i < n; i++) {
				app.plottingPanel.addDrawable((ClassicalTrajectory) app.trajectories.get(i));
			}
			if (n > 0) {
				app.defaultTrajectory = (ClassicalTrajectory) app.trajectories.get(0);
				if (app.inspector != null) {
					app.inspector.updateDataAndScale(app.defaultTrajectory);
				}
			}
			app.plottingPanel.repaint();
			return obj;
		}
	}
}
