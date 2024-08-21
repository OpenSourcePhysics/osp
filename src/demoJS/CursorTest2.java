package demoJS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.*;

public class CursorTest2 {
	
	static  {
		/**
		 * @j2sNative
		 * 
		 *  $("body").append('<link href="https://physlets.org/swingjs/ipad.css" rel="stylesheet" type="text/css">');
		 * 
		 * 	$("body").append('<div class="custom-cursor" id="customCursor" style="z-index: 10000000;"></div>');
		 * 
		 * 
		 */
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("Custom Cursor Example");
		frame.setLayout(new BorderLayout());
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());
		JLabel label = new JLabel("JavaScript Cursor");
	/** @j2sNative console.log('JavaScript addEventListener.'); 
		debugger; 
const customCursor = document.getElementById('customCursor');
customCursor.style.backgroundColor = 'green';

// Function to move the custom cursor
  function setCustomCursor(x,y) {
      customCursor.style.left = `${x}px`;
      customCursor.style.top = `${y}px`;
      customCursor.style.display = 'block';
      console.log('setCustomCursor x=' + x+ '  y='+y);
  }
  
  // Function to move the custom cursor
  function moveCustomCursor(event) {
      let x, y;
      if (event.touches) {
          x = event.touches[0].clientX;
          y = event.touches[0].clientY;
      } else {
          x = event.clientX;
          y = event.clientY;
      }
      setCustomCursor(x,y);
  }

  document.addEventListener('mousemove', moveCustomCursor);
  document.addEventListener('touchmove', moveCustomCursor);

  document.addEventListener('mousedown', () => {
      customCursor.style.backgroundColor = 'yellow'; // Change to your desired click color
      console.log('mousedown event');
  });
  document.addEventListener('mouseup', () => {
      customCursor.style.backgroundColor = 'green'; // Change back to transparent
      console.log('mouseup event');
  });

  document.addEventListener('touchstart', () => {
      customCursor.style.backgroundColor = 'yellow'; // Change to your desired click color
      console.log('touchstart event');
  });
  document.addEventListener('touchend', () => {
      customCursor.style.backgroundColor = 'green'; // Change back to transparent
      //customCursor.style.display = 'none';
      console.log('touchend event');
  });
				   
		*/	

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
            g.setColor(Color.RED);
            // Draw the circle
            g.fillOval(centerX - radius, centerY - radius, 2 * radius, 2 * radius);
        }
    };
    
    drawingPanel.setName("drawingPanel");
    
    drawingPanel.addMouseListener( new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {}

			@Override
			public void mousePressed(MouseEvent e) {
			e.getX();
			System.err.println("mouse pressed getX="+e.getX()+ " onscreen x="+ e.getXOnScreen());
			/** @j2sNative
			  console.log('JavaScript mousePressed.'); 
			  customCursor.style.backgroundColor = 'yellow'; // Change to your desired click color
			  customCursor.style.display = 'block'; 
			 */
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			/** @j2sNative
			    console.log('JavaScript mouseReleased.'); 
			    customCursor.style.backgroundColor = 'green'; // Change back to red
			 */
			}

			@Override
			public void mouseEntered(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {}
		});
    
    drawingPanel.addMouseMotionListener( new MouseMotionListener() {


			@Override
			public void mouseDragged(MouseEvent e) {
				/** @j2sNative
	        debugger;
		    */
				int screenX=e.getXOnScreen();
				int screenY=e.getYOnScreen();
				Point loc= frame.getLocation();
				int topScreenX=screenX+loc.x;
				int topScreenY=screenY+loc.y;
				System.out.println("mouse dragged screenX="+screenX+ " topScreenX="+ topScreenX );
				System.out.println("mouse dragged screenY="+screenY+ " topScreenY="+ topScreenY );
				/** @j2sNative 
		    setCustomCursor(topScreenX, topScreenY);
		    */
				
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				// TODO Auto-generated method stub
				
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
