package demoJS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;

public class CursorTest {

	public static void main(String[] args) {
		JFrame frame = new JFrame("Custom Cursor Example");
		frame.setLayout(new BorderLayout());
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());
		JLabel label = new JLabel("JavaScript Cursor");

		JButton button = new JButton();
		button.setText("Add EventListener");

		button.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
			/** @j2sNative console.log('JavaScript addEventListener.'); 
				debugger; 
				const customCursor = document.getElementById('customCursor');
		        
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
		            customCursor.style.left = `${x}px`;
		            customCursor.style.top = `${y}px`;
		            customCursor.style.display = 'block';
		          
		        }
		
		        document.addEventListener('mousemove', moveCustomCursor);
		        document.addEventListener('touchmove', moveCustomCursor);
		
		        document.addEventListener('mousedown', () => {
		            customCursor.style.backgroundColor = 'green'; // Change to your desired click color
		            console.log('mousedown event');
		        });
		        document.addEventListener('mouseup', () => {
		            customCursor.style.backgroundColor = 'transparent'; // Change back to transparent
		            console.log('mouseup event');
		        });
		
		        document.addEventListener('touchstart', () => {
		            customCursor.style.backgroundColor = 'green'; // Change to your desired click color
		            console.log('touchstart event');
		        });
		        document.addEventListener('touchend', () => {
		            customCursor.style.backgroundColor = 'transparent'; // Change back to transparent
		            customCursor.style.display = 'none';
		            console.log('touchend event');
		        });
						   
				*/	
			}
			});

		panel.add(label);
		panel.add(button);

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
        
        drawingPanel.addMouseListener( new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {}

			@Override
			public void mousePressed(MouseEvent e) {
			/** @j2sNative console.log('JavaScript mousePressed.'); 
			  customCursor.style.backgroundColor = 'green'; // Change to your desired click color
			  customCursor.style.display = 'block'; 
			 */
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			/** @j2sNative console.log('JavaScript mouseReleased.'); 
			    customCursor.style.backgroundColor = 'transparent'; // Change back to transparent
		        customCursor.style.display = 'none';
			 */
			}

			@Override
			public void mouseEntered(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {}
		});
        
        frame.add(drawingPanel,BorderLayout.CENTER);
        
		frame.add(panel,BorderLayout.SOUTH);
		frame.setSize(300, 300);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

	}

}
