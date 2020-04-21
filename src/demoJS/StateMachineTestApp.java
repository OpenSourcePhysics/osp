/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see: 
 * <http://www.opensourcephysics.org/>
 */


// gets needed classes, asterisk * means get all classes in controls subdirectory
package demoJS;
import org.opensourcephysics.controls.*;

import javajs.async.SwingJSUtils;
import javajs.async.SwingJSUtils.StateHelper;
import javajs.async.SwingJSUtils.StateMachine;

/**
 * Demonstrates how to use a StateHelper rather in place of a Tread
 *
 * @author Wolfgang Christian
 * @version 1.0 02/04/20
 */
public class StateMachineTestApp extends AbstractCalculation implements Runnable, StateMachine {

	Thread animationThread;
	
	int counter=0;
	long t0=0;
	long t1=0;
	double dt=0;

	private StateHelper stateHelper;
	private int delay = (/** @j2sNative 100 || */ 100);
	private final static int STATE_INIT = 0;
	private final static int STATE_LOOP = 1;
	private final static int STATE_DONE = 2;

	StateMachineTestApp() {
		/** @j2sNative debugger; */
	}

	public void run() {
		stateHelper = new SwingJSUtils.StateHelper(this);
		stateHelper.setState(STATE_INIT);
		stateHelper.sleep(0);
	}

	@Override
	public boolean stateLoop() {
		while (animationThread != null && !animationThread.isInterrupted() && stateHelper.isAlive()) {
			switch (stateHelper.getState()) {
			default:
			case STATE_INIT:
				control.println("Starting");
				t1=(/**@j2sNative performance.now() || */System.currentTimeMillis());
				dt=(t1-t0)/1000.0;
				stateHelper.setState(STATE_LOOP);
				stateHelper.sleep(delay);
				return true;
			case STATE_LOOP:
				control.println("Running counter="+counter);
				t1=(/**@j2sNative performance.now() || */System.currentTimeMillis());
				dt=(t1-t0)/1000.0;
				counter++;
				stateHelper.sleep(delay);
				return true;
			case STATE_DONE:
				control.println("Ending");
				return false;
			}
		}
		return false;
	}

	/**
	 * Do a calculation.
	 */
	public void calculate() { // Does a calculation
		/** @j2sNative debugger; */
		control.println("Calculate pressed");
		control.setValue("counter", counter); // show value
		control.setValue("dt", dt); // show value
	}

	/**
	 * Reset the program to its initial state.
	 */
	public void reset() {
		counter=0;
		dt=0;
		animationThread=null;
		control.setValue("counter", counter); // show value
		control.setValue("dt", dt); // show value
	}

	public void startHelper() {
		control.println("Start pressed");
		if (animationThread != null) return; // already running
		animationThread = new Thread(this);
		t0=(/**@j2sNative performance.now() || */System.currentTimeMillis());
		animationThread.start();
	}

	public void stopHelper() {
		control.println("Stop pressed");
		stateHelper.setState(STATE_DONE);
		animationThread = null; // signal the animation to stop
	}

	/**
	 * Starts the Java application.
	 * 
	 * @param args command line parameters
	 */
	public static void main(String[] args) { // Create a calculation control structure using this class
		CalculationControl control = CalculationControl.createApp(new StateMachineTestApp());
		control.addButton("startHelper", "Start");
		control.addButton("stopHelper", "Stop");
	}
}