package debugging;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Random;

import javax.swing.*;

import javafx.concurrent.Task;

public class ProgressBarTest implements PropertyChangeListener {
	
	int min = 0;
	int max = 10;
	int progress = 0; 
	boolean done=true;
	JProgressBar progressBar;
	JButton button;
	Task task;
	
	ProgressBarTest(){
		progressBar = new JProgressBar(min, max);
		progressBar.setString("Not running");
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
		
		JFrame frame = new JFrame("Async Progress Bar");
		frame.setLayout(new BorderLayout());

		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());

		JLabel label = new JLabel("Progress Bar Test");

		button = new JButton();
		button.setText("Count");
		
		button.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {	
				progressBar.setString("Running");
				button.setEnabled(false);
		    done = false;
		    task = new Task();
		    task.addPropertyChangeListener(ProgressBarTest.this);
		    task.execute();
			}
			});

		panel.add(label);
		panel.add(button);
		
		JPanel sliderPanel = new JPanel();
		sliderPanel.setBackground(Color.GREEN);
		sliderPanel.setSize(300, 50);
		sliderPanel.add(progressBar);

		frame.setSize(300, 300);
		frame.add(panel, BorderLayout.NORTH);
		frame.add(sliderPanel, BorderLayout.SOUTH);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);	
	}
	
  class Task extends SwingWorker{
    /*
     * Main task. Executed in background thread.
     */
    @Override
    public Void doInBackground() {
        int progress = 0;
        setProgress(0);
        while (progress < max) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            	System.err.println("Progress interrupted.");
            }
            progress++;
            setProgress(progress);
        }
        return null;
    }

    /*
     * Executed in event dispatching thread
     */
    @Override
    public void done() {
        Toolkit.getDefaultToolkit().beep();
        button.setEnabled(true);
        System.out.println("Done!\n");
        progressBar.setString("Done");
    }
}


	public static void main(String[] args) {
     new ProgressBarTest();
	}

  /**
   * Invoked when task's progress property changes.
   */
  public void propertyChange(PropertyChangeEvent evt) {
      if ("progress" == evt.getPropertyName()) {
          int progress = (Integer) evt.getNewValue();
          progressBar.setValue(progress);
      } 
  }

}

