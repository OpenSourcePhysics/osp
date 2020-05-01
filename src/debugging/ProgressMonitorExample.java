package debugging;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.ProgressMonitor;
import javax.swing.UIManager;

import javajs.async.AsyncSwingWorker;

public class ProgressMonitorExample {
	 
	private static ActionListener createStartTaskActionListenerOrig(Component parent) {
	      //for progress monitor dialog title
	      UIManager.put("ProgressMonitor.progressText", "Test Progress");
	      return (ae) -> {
	          new Thread(() -> {
	              //creating ProgressMonitor instance
	              ProgressMonitor pm = new ProgressMonitor(parent, "Test Task",
	                      "Task starting", 0, 100);

	              //decide after 100 millis whether to show popup or not
	              pm.setMillisToDecideToPopup(100);
	              //after deciding if predicted time is longer than 100 show popup
	              pm.setMillisToPopup(100);
	              for (int i = 1; i <= 100; i++) {
	                  //updating ProgressMonitor note
	                  pm.setNote("Task step: " + i);
	                  //updating ProgressMonitor progress
	                  pm.setProgress(i);
	                  try {
	                      //delay for task simulation
	                      TimeUnit.MILLISECONDS.sleep(200);
	                  } catch (InterruptedException e) {
	                      System.err.println(e);
	                  }
	              }
	              pm.setNote("Task finished");
	          }).start();
	      };
	  }


	static class AsyncTask extends AsyncSwingWorker {

		AsyncTask(Component owner, String title, int delayMillis, int min, int max) {
			super(owner, title, delayMillis, min, max);
		}

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
			// no report in this demo 
		}
		
		@Override
		public String getNote(int progress) {
			return "Task step: " + progress;
		}
	}

	  private static ActionListener createStartTaskActionListener(Component parent) {
	      return (ae) -> {
	          new Thread(() -> {	        	  
	        	  new AsyncTask(parent, "Test Progress", 200, 1, 100).execute();
	          }).start();
	      };
	  }

	 
	
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