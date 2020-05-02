package javajs.async;

import java.awt.Component;

import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import javajs.async.SwingJSUtils.StateHelper;
import javajs.async.SwingJSUtils.StateMachine;

/**
 * Executes asynchronous tasks using a SwingWorker in Java or JavaScript,
 * equivalently.
 * 
 * Unlike a standard SwingWorker, AsyncSwingWorker may itself be asynchronous.
 * For example, it might load a file asynchronously, or carry out a background
 * process in JavaScript much like one might be done in Java, but with only a
 * single thread.
 * 
 * Whereas a standard SwingWorker would execute done() long before the
 * asynchronous task completed, this class will wait until progress has been
 * asynchronously set greater or equal to its max value or the task is canceled
 * before executing that method.
 * 
 * Three methods must be supplied by the subclass:
 * 
 * void initAsync()
 * 
 * int doInBackgroundAsync(int progress)
 * 
 * void doneAsync()
 * 
 * Both initAsync() and doneAsync() are technically optional - they may be
 * empty. doInBackgroundAsync(), however, is the key method where, like
 * SwingWorker's doInBackground, the main work is done. The supplied progress
 * parameter reminds the subclass of where it is at, and the return value allows
 * the subclass to update the progress field in both the SwingWorker and the
 * ProgressMonitor.
 * 
 * 
 * @author hansonr
 *
 */
public abstract class AsyncSwingWorker extends SwingWorker<Void, Void> implements StateMachine {
	

	public static final String DONE_ASYNC = "DONE_ASYNC";
	public static final String CANCELED_ASYNC = "CANCELED_ASYNC";

	protected int progressAsync;
	
	/**
	 * Override to provide initial tasks.
	 */
	abstract public void initAsync();
	
	/**
	 * Given the last progress, do some portion of the task that the SwingWorker would do in the background, and return the new progress.
	 * returning max or above will complete the task.
	 * 
	 * @param progress
	 * @return new progress
	 */
	abstract public int doInBackgroundAsync(int progress);
	
	/**
	 * Do something when the task is finished or canceled.
	 * 
	 */
	abstract public void doneAsync();


	protected ProgressMonitor progressMonitor;
	protected int delayMillis;
	protected String note;
	protected int min;
	protected int max;
	protected int progressPercent;

	/**
	 * Construct an asynchronous SwingWorker task that optionally will display a
	 * ProgressMonitor. Progress also can be monitored by adding a PropertyChangeListener
	 * to the AsyncSwingWorker and looking for the "progress" event, just the same as for a 
	 * standard SwingWorker.
	 * 
	 * @param owner optional owner for the ProgressMonitor, typically a JFrame or JDialog.
	 * 
	 * @param title A non-null title indicates we want to use a ProgressMonitor with that title line.
	 * 
	 * @param delayMillis A positive number indicating the delay we want before executions, during which progress will be reported. 
	 * 
	 * @param min  The first progress value. No range limit.
	 * 
	 * @param max  The last progress value. No range limit; may be greater than min.
	 * 
	 */
	public AsyncSwingWorker(Component owner, String title, int delayMillis, int min, int max) {
		if (title != null) {
			progressMonitor = new ProgressMonitor(owner, title, "", Math.min(min,  max), Math.max(min, max));
			progressMonitor.setProgress(Math.min(min,  max)); // displays monitor
		}
		this.delayMillis = Math.max(1, delayMillis);
		this.min = min;
		this.max = max;
	}

	public int getMinimum() {
		return min;
	}

	public void setMinimum(int min) {
		this.min = min;
		if (progressMonitor != null)
			progressMonitor.setMinimum(min);
	}

	public int getMaximum() {
		return max;
	}

	public void setMaximum(int max) {
		if (progressMonitor != null)
			progressMonitor.setMaximum(max);
		this.max = max;
	}


	public int getProgressPercent() {
		return progressPercent;
	}

	public void setNote(String note) {
		this.note = note;
		if (progressMonitor != null)
			progressMonitor.setNote(note);
	}


	
	/**
	 * Cancel the asynchronous process.
	 * 
	 */
	public void cancelAsync() {
		helper.interrupt();
	}

	/**
	 * Check to see if the asynchronous process has been canceled. 
	 *
	 * @return true if StateHelper is not alive anymore
	 * 
	 */
	public boolean isCanceledAsync() {
		return !helper.isAlive();
	}
	
	/**
	 * Check to see if the asynchronous process is completely done.
	 * 
	 * @return true only if the StateMachine is at STATE_DONE
	 * 
	 */
	public boolean isDoneAsync() {
		return helper.getState() == STATE_DONE;
	}

	/**
	 * Override to set a more informed note for the ProcessMonitor.
	 * 
	 * @param progress
	 * @return
	 */
	public String getNote(int progress) {
		return String.format("Completed %d%%.\n", progress);
	}
	
	/**
	 * Retrieve the last note delivered by the ProcessMonitor.
	 * 
	 * @return
	 */
	public String getNote() {
		return note;
	}

	public int getProgressAsync() {
		return progressAsync;
	}

	/**
	 * Set the [min,max] progress safely.
	 * 
	 * SwingWorker only allows progress between 0 and 100. 
	 * This method safely translates [min,max] to [0,100].
	 * 
	 * @param n
	 */
	public void setProgressAsync(int n) {
		n = (max > min ? Math.max(min, Math.min(n, max))
				: Math.max(max, Math.min(n, min)));
		progressAsync = n;
		n = (int) ((n - min) * 100 / (max - min));
		n = (n < 0 ? 0 : n > 100 ? 100 : n);
		progressPercent = n;
	}
	
	
	///// the StateMachine /////
	
	
	private final static int STATE_INIT = 0;
	private final static int STATE_LOOP = 1;
	private final static int STATE_WAIT = 2;
	private final static int STATE_DONE = 99;

	private StateHelper helper;
	
	/**
	 * The StateMachine's main loop.
	 * 
	 * Note that a return from this method will exit doInBackground, trigger the
	 * isDone() state on the underying worker, and scheduling its done() for
	 * execution on the AWTEventQueue.
	 *
	 * Since this happens essentially immediately, it is unlikely that
	 * SwingWorker.isCancelled() will ever be true. Thus, the SwingWorker task
	 * itself won't be cancelable in Java or in JavaScript, since its
	 * doInBackground() method is officially complete, and isDone() is true well
	 * before we are "really" done. FutureTask will not set isCancelled() true once
	 * the task has run.
	 * 
	 * We are using an asynchronous task specifically because we want to have the
	 * opportunity for the ProgressMonitor to report in JavaScript. We will have to
	 * cancel our task and report progress explicitly using our own methods.
	 * 
	 */
	@Override
	public boolean stateLoop() {
		while (helper.isAlive()) {
			switch (helper.getState()) {
			case STATE_INIT:
				setProgressAsync(min);
				initAsync();
				helper.setState(STATE_WAIT);
				continue;
			case STATE_LOOP:
				if (checkCanceled()) {
					helper.setState(STATE_DONE);
					firePropertyChange("state", null, CANCELED_ASYNC);
					continue;
				} else {
					progressAsync = doInBackgroundAsync(progressAsync);
					setProgressAsync(progressAsync);
					setNote(getNote(progressAsync));
					setProgress(progressPercent);
					if (progressMonitor != null)
						progressMonitor.setProgress(max > min ? progressAsync : max + min - progressAsync);
					helper.setState(progressAsync == max ? STATE_DONE : STATE_WAIT);
					continue;
				}
			case STATE_WAIT:
				helper.setState(STATE_LOOP);
				helper.sleep(delayMillis);
				return true;
			default:
			case STATE_DONE:
				if (progressMonitor != null)
					progressMonitor.close();
				// Put the doneAsync() method on the AWTEventQueue
				// just as for SwingWorker.done().
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						doneAsync();
						firePropertyChange("state", null, DONE_ASYNC);
					}

				});
				return false;
			}
		}
		return false;
	}

    private boolean checkCanceled() {
    	if (isMonitorCanceled() || isCancelled()) {
    		helper.interrupt();
    		return true;
    	}
		return false;
	}

	//// final SwingWorker methods not to be used by subclasses ////

	private boolean isMonitorCanceled() {
		return (progressMonitor != null && progressMonitor.isCanceled());
	}

	/**
	 * see SwingWorker, made final here.
	 * 
	 */
	@Override
	final protected Void doInBackground() throws Exception {
		helper = new StateHelper(this);
		setProgressAsync(min);
		helper.next(STATE_INIT);
		return null;
	}

	/**
	 * see SwingWorker, made final here. Nothing to do.
	 * 
	 */
	@Override
	final public void done() {
	}

}