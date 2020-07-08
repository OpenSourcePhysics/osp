package demo;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import org.opensourcephysics.frames.TableFrame;

public class TableDemo {

	
	static int stride = 1;
	
	public static void main(String[] args) {
		TableFrame table = new TableFrame("Table Demo");
		// table.setVisible(true);
		// table.setStride(1);
		table.setSize(400, 400);
		for (double x = -10, dx = 0.1; x < 10; x += dx) {
			table.appendRow(new double[] { x, Math.sin(x) });
		}
		table.dataPanel.dataRowTable.getTableHeader().setBackground(Color.blue);
		table.setVisible(true);
		table.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
		new Timer(1000, new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				table.setStride(stride = stride * 2);
				table.refreshTable();
				if (stride > 50) {
					((Timer) e.getSource()).stop();
					table.dataPanel.dataRowTable.getTableHeader().setBackground(Color.yellow);
				}
			}

		}).start();

	}

}
