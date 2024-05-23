package debugging;

import javax.swing.JFrame;
import javax.swing.JTable;


public class TableBug {

	Object[][] data = { { "Kathy", "Smith", "Snowboarding", new Integer(5), new Boolean(false) },
			{ "John", "Doe", "Rowing", new Integer(3), new Boolean(true) },
			{ "Sue", "Black", "Knitting", new Integer(2), new Boolean(false) },
			{ "Jane", "White", "Speed reading", new Integer(20), new Boolean(true) },
			{ "Joe", "Brown", "Pool", new Integer(10), new Boolean(false) } };

	String[] columnNames = { "First Name", "Last Name", "Sport", "# of Years", "Vegetarian" };

	JFrame frame = new JFrame("Table Test");
	JTable table;

	TableBug() {

		table = new JTable(data, columnNames);

		frame.setContentPane(table);
		frame.setSize(500, 500);
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		new TableBug();

	}

}
