package demoJS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class CursorTest2 {
	
	// begin JavaScript methods
	
	static  {
		/**
		 * @j2sNative
		 * 
		 *  $("body").append('<link href="https://physlets.org/swingjs/ipad.css" rel="stylesheet" type="text/css">');
		 * 	$("body").append('<div class="custom-cursor" id="customCursor" style="z-index: 10000000;"></div>');
		 * 		
		 *  setCustomCursor = function(e) {
		 *    var x = e.getXOnScreen$();
		 *    var y = e.getYOnScreen$();
		 *    var c = $('#customCursor')
		 *    	.css({"left": x + "px", "top": y + "px", "display": "block"});
		 *  }
		 */
	}


	protected static void jsCustomCursor(String action, MouseEvent e) {
		System.out.println(action + " " + e);
		/**
		 * @j2sNative 
		 * switch (action) { 
		 * case "pressed":
		 *   $('#customCursor').css({"display":'block'});
		 *   setCustomCursor(e); 
		 *   break; 
		 * case "released": 
		 *   $('#customCursor').css({"display":'none'});
		 *   break;
		 * case "dragged": 
		 * case "moved": 
		 *   setCustomCursor(e); 
		 *   break;
		 * }
		 * 
		 */
	}

	// end JavaScript methods

	static boolean clickToggle, dragging, pressed;
	
	public static void main(String[] args) {
		JFrame frame = new JFrame("Custom Cursor Example");
		frame.setLayout(new BorderLayout());
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());
		JLabel label = new JLabel("JavaScript Cursor");
		panel.add(label);

    JPanel drawingPanel = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // Get the dimensions of the panel
            int width = getWidth();
            int height = getHeight();
            // Calculate the coordinates for the center of the panel
            int centerX = width / 2;
            int centerY = height / 2;
            // Define the radius of the circle
            int radius = Math.min(width, height) / 4;
            // Set the color to red
            // Draw the circle
            g.setColor(clickToggle ? Color.BLUE : Color.RED);
            g.fillOval(centerX - radius, centerY - radius, 2 * radius, 2 * radius);
        }
    };
    
    drawingPanel.setName("drawingPanel");
    
    drawingPanel.addMouseListener( new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (dragging) {
					dragging = false;
					return;
				}
				// flash blue circle
				clickToggle = !clickToggle;
				jsCustomCursor("clicked", e);
				drawingPanel.repaint();
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (pressed) // multitouch will press multiple times before release
					return;
				dragging = false;
				pressed = true;
				jsCustomCursor("pressed", e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				pressed = false;
				jsCustomCursor("released", e);
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {}
		});
    
    drawingPanel.addMouseMotionListener( new MouseMotionListener() {


			@Override
			public void mouseDragged(MouseEvent e) {
				dragging = true;
				jsCustomCursor("dragged", e);
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				jsCustomCursor("moved", e);
			}

    });

    frame.add(drawingPanel,BorderLayout.CENTER);
        
		frame.add(panel,BorderLayout.SOUTH);
		frame.setSize(300, 300);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

	}

}
