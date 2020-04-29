package debugging;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

public class ProgressBarTest {
	
	int min = 0;
	int max = 10;
	int count = 0;  
	JSlider slider = new JSlider(JSlider.HORIZONTAL, min, max, count);
	
	ProgressBarTest(){
		JFrame frame = new JFrame("Async Progress Bar");
		frame.setLayout(new BorderLayout());

		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());

		JLabel label = new JLabel("Progress Bar Test");

		JButton button = new JButton();
		button.setText("Count");
		
		button.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {	
				count();
			}
			});

		panel.add(label);
		panel.add(button);
		
		JPanel sliderPanel = new JPanel();
		sliderPanel.setBackground(Color.GREEN);
		sliderPanel.setSize(300, 50);
		sliderPanel.add(slider);

		frame.setSize(300, 300);
		frame.add(panel, BorderLayout.NORTH);
		frame.add(sliderPanel, BorderLayout.SOUTH);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);	
	}
	
	void count() {
		count=0;
		System.out.println("\nCounting");
		longJob();
		slider.setValue(count);
	}
	
	void longJob() {
		System.out.println("Start long job.");
		Runnable counting = new Runnable() {
			@Override
			public void run() {
				while(count<max) {  // doing long job
          try { Thread.sleep(1000); } // sleep one second
          catch(InterruptedException ie) {
          	System.err.println("Thread interrupted.");
          }
          count++;
          slider.setValue(count);  // update progress bar
				}
				slider.setValue(count);
				System.out.println("Done long job.\n");
			} // end of run
		};
		
    Thread t = new Thread(counting);
    t.start();
		
	}

	public static void main(String[] args) {
     new ProgressBarTest();
	}

}

