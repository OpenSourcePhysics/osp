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
		 *  $("body").append('<link href="./ipad.css" rel="stylesheet" type="text/css">');
		 * 	$("body").append('<div class="custom-cursor" id="customCursor" style="z-index: 10000000;"></div>');
		 * 		
		 *  setCustomCursor = function(e) {
		 *    var x = e.getXOnScreen$();
		 *    var y = e.getYOnScreen$();
		 *    var c = $('#customCursor')
		 *    	.css({"left": x + "px", "top": y + "px"});
		 *  }
		 */
		
		/**
		 * @j2sNative
		 * 		
		 *  hideCustomCursor = function(e) {
		 *  	var x = e.getXOnScreen$();
		 *    var y = e.getYOnScreen$();
		 *    var c = $('#customCursor')
		 *    	.css({"left": x + "px", "top": y + "px", "display": "none"});
		 *  }
		 */
		
		/**
		 * @j2sNative
		 * 		
		 *  showCustomCursor = function(e) {
		 *  	var x = e.getXOnScreen$();
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
		 *   $('#customCursor').css({"backgroundColor":'yellow'});
		 *   setCustomCursor(e); 
		 *   break; 
		 * case "released": 
		 *   $('#customCursor').css({"background-color":'green'});
		 *   setCustomCursor(e);
		 *   break;
		 * case "dragged": 
		 * case "moved": 
		 *   setCustomCursor(e); 
		 *   break;
		 * case "entered": 
		 *   $('#customCursor').css({"backgroundColor":'green'});
		 *   $('#customCursor').css({"width":'20px'});
		 *   $('#customCursor').css({"height":'20px'});
		 *   showCustomCursor(e); 
		 *   break;	
		 * case "exited": 
		 *   $('#customCursor').css({"backgroundColor":'transparent'});
		 *   $('#customCursor').css({"width":'0px'});
		 *   $('#customCursor').css({"height":'0px'});
		 *   hideCustomCursor(e); 
		 *   break;
		 * }
		 * 
		 */
	}

	// end JavaScript methods

	static boolean clickToggle, dragging, pressed, inside;
	
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
				dragging = false;
				jsCustomCursor("released", e);
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				inside = true;
				jsCustomCursor("entered", e);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				inside = false;
				dragging=false;
				pressed = false;
				jsCustomCursor("exited", e);
			}
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
