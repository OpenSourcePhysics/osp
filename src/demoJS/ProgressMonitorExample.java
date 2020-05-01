package demoJS;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.UIManager;

import javajs.async.AsyncSwingWorker;

public class ProgressMonitorExample {

	/**
	 * SwingJS variant is just an anonymous subclass of AsyncSwingWorker.
	 * 
	 * The for-loop range [1,100] is set in the constructor. 
	 * 
	 * the body of the for loop is in doInBackground(i), but actually
	 * nothing is done here; index i (the progress field) is just incremented. 
	 * 
	 * The iteration delay is set to 200 ms, same as Java.
	 * 
	 * The label is set using getNote().
	 * 
	 * In Java there is a final setting of the ProgressMonitor note, but that is meaningless, as its
	 * dialog has been closed already.
	 * 
	 * @param parent
	 * @return
	 */
	private static ActionListener createStartTaskActionListener(Component parent) {
		UIManager.put("ProgressMonitor.progressText", "Test Progress");
		return (actionEvent) -> {
			new AsyncSwingWorker(parent, "Test Task", 200, 1, 100) {

				@Override
				public void initAsync() {
					progressMonitor.setMillisToDecideToPopup(100);
					progressMonitor.setMillisToPopup(100);
				}

				@Override
				public int doInBackgroundAsync(int i) {
					// nothing to do in this demo except increment i
					return ++i;
				}

				@Override
				public void doneAsync() {
					// nothing to do when done for this demo (the ProgressManager is closed already)
				}

				@Override
				public String getNote(int progress) {
					return "Task step: " + progress;
				}
			}.execute();
		};
	}

//
//	  
//	  
//	  private static ActionListener createStartTaskActionListenerOrig(Component parent) {
//		      //for progress monitor dialog title
//		      UIManager.put("ProgressMonitor.progressText", "Test Progress");
//		      return (ae) -> {
//		          new Thread(() -> { 
//		              //creating ProgressMonitor instance
//		              ProgressMonitor pm = new ProgressMonitor(parent, "Test Task",
//		                      "Task starting", 0, 100);
//
//		              //decide after 100 millis whether to show popup or not
//		              pm.setMillisToDecideToPopup(100);
//		              //after deciding if predicted time is longer than 100 show popup
//		              pm.setMillisToPopup(100);
//		              for (int i = 1; i <= 100; i++) {
//		                  //updating ProgressMonitor note
//		                  pm.setNote("Task step: " + i);
//		                  //updating ProgressMonitor progress
//		                  pm.setProgress(i);
//		                  try {
//		                      //delay for task simulation
//		                      TimeUnit.MILLISECONDS.sleep(200);
//		                  } catch (InterruptedException e) {
//		                      System.err.println(e);
//		                  }
//		              }
//		              pm.setNote("Task finished");
//		          }).start();
//		      };
//		  }

	
  public static void main(String[] args) {
      JFrame frame = createFrame("ProgressMonitor Example");
      JButton button = new JButton("start task");
      button.addActionListener(createStartTaskActionListener(frame));
      frame.add(button, BorderLayout.NORTH);
      frame.setVisible(true);
  }

 public static JFrame createFrame(String title) {
      JFrame frame = new JFrame(title);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setSize(new Dimension(800, 700));
      return frame;
  }
}