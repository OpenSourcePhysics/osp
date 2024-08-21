package demoJS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Test of custom cursor in SwingJS. 
 * 
 * The cursor and its CSS are created once, on static initiation of this class.
 * 
 * 
 * 
 */
public class CursorTest {

	static  {
		/**
		 * @j2sNative
		 * 
		 *  $("body").append('<link href="ipad.css" rel="stylesheet" type="text/css">');
		 * 
		 * 	$("body").append('<div class="custom-cursor" id="customCursor" style="z-index: 10000000;"></div>');
		 * 
		 * 
		 */
	}

	
	class DrawingPanel extends JPanel implements MouseListener, MouseMotionListener {

		public boolean dragging;

		Object customCursor;
		Cursor myCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

		DrawingPanel() {
			setName("drawingPanel");
			addMouseListener(this);
			addMouseMotionListener(this);
			setCursor(myCursor);
		}
		@Override
		public void paint(Graphics g) {
			super.paint(g);
			// Get the dimensions of the panel
			int width = getWidth();
			int height = getHeight();
			// Calculate the coordinates for the center of the panel
			int centerX = width / 2;
			int centerY = height / 2;
			// Define the radius of the circle
			int radius = Math.min(width, height) / 4;
			// Set the color to red
			g.setColor(dragging ? Color.BLUE : customCursor == null ? Color.YELLOW : Color.RED);
			// Draw the circle
			g.fillOval(centerX - radius, centerY - radius, 2 * radius, 2 * radius);
		}

		protected void addEventListener() {
			customCursor = (customCursor != null ? null : /** @j2sNative $('#customCursor')[0] || */
					null);
			setCursor(customCursor == null ? myCursor : null);
			getParent().repaint();
		}

		@SuppressWarnings("unused")
		protected void moveMouseCursor(MouseEvent e, String mode) {
			// Function to move the custom cursor
			if (customCursor != null) {	
				String display = "block";
				String background = "rgba(0,255,0,0.3)";
				switch (mode) {
				case "move":
				case "enter":
					background = "transparent";
					break;
				case "drag":
				case "down":
					break;
				case "up":
				case "exit":
					background = "transparent";
					display = "none";
					break;
				}
				Point pt = e.getLocationOnScreen();
				// OK this is now the "x" and "y" below commented out.
				/** @j2sNative 
				//var event = e.bdata.jqevent;
				//var x = event.clientX;
				//var y = event.clientY;
				this.customCursor.style.left = pt.x + "px";
				this.customCursor.style.top = pt.y + "px";
	        	background && (this.customCursor.style.backgroundColor = background);
			    display && (this.customCursor.style.display = display);
				 * 
				 */
			}
			getParent().repaint();
		}
		
		@Override
		public void mouseDragged(MouseEvent e) {
			dragging = true;
			moveMouseCursor(e, "drag");
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			moveMouseCursor(e, "move");
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			System.out.println("CursorTest - mouse clicked\n" 
			+ "panel: " + getLocationOnScreen() + "\n"
			+ "cursor: " + e.getLocationOnScreen());
		}

		@Override
		public void mousePressed(MouseEvent e) {
			moveMouseCursor(e, "down");
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			moveMouseCursor(e, "up");
			dragging = false;
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			moveMouseCursor(e, "enter");
		}

		@Override
		public void mouseExited(MouseEvent e) {
			moveMouseCursor(e, "exit");
		}

	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("Custom Cursor Example");
		frame.setLayout(new BorderLayout());
		DrawingPanel drawingPanel = new CursorTest().new DrawingPanel();
		frame.add(drawingPanel, BorderLayout.CENTER);

		//JLabel label = new JLabel("JavaScript Cursor");
		JButton button = new JButton();
		button.setText("Add/Remove EventListener");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				drawingPanel.addEventListener();
			}
		});
		
		JPanel panel = new JPanel(new FlowLayout());
		//panel.add(label);
		panel.add(button);

		frame.add(panel, BorderLayout.NORTH);
		frame.setSize(300, 300);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	
	
}
