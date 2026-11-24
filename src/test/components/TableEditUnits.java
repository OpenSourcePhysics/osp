package test.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class TableEditUnits extends JFrame {
    private JTable table;
    private DefaultTableModel model;

    public TableEditUnits() {
        setTitle("Single-Click JTable with Unit Handling");
        setSize(520, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        setBackground(Color.green);
        getContentPane().setBackground(Color.magenta);
        
    	JMenuBar m = new JMenuBar() {
    		public Dimension getPreferredSize() {
    			Dimension d = super.getPreferredSize();
    			d.height = 50;
    			return d;
    		}
    	};
    	m.add(new JLabel("osp"));
    	m.setBackground(Color.yellow);
    	setJMenuBar(m);

        // 1. Setup Columns & Data with Units
        String[] columns = {"Item", "Length", "Weight"};
        Object[][] data = {
            {"Beam A", "33 m", "12.5 kg"},
            {"Cable B", "15.5 m", "4.2 kg"},
            {"Pipe C", "8 m", "22 kg"}
        };

        model = new DefaultTableModel(data, columns);
        table = new JTable(model) {
        	public void paintComponent(Graphics g) {
        		super.paintComponent(g);
        		
        	}
        };
        table.setBackground(Color.pink);
        table.setRowHeight(28);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        // 2. Apply Single-Click Unit Editors to Columns 1 and 2
        table.getColumnModel().getColumn(1).setCellEditor(new UnitCellEditor(" m"));
        table.getColumnModel().getColumn(2).setCellEditor(new UnitCellEditor(" kg"));

        // Optional: Keep column 0 single-click editable as plain text
        DefaultCellEditor textEditor = new DefaultCellEditor(new JTextField());
        textEditor.setClickCountToStart(1);
        table.getColumnModel().getColumn(0).setCellEditor(textEditor);

        // 3. Log Saved Values
        model.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int col = e.getColumn();
                System.out.printf("Saved [Row %d, Col %d]: %s%n", row, col, model.getValueAt(row, col));
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(Color.RED);
        
        add(sp, BorderLayout.CENTER);
    }

    /**
     * Custom single-click editor that strips a unit suffix on edit
     * and re-appends it when saving.
     */
    static class UnitCellEditor extends DefaultCellEditor {
        private final String unitSuffix;
        private final JTextField textField;

        public UnitCellEditor(String unitSuffix) {
            super(new JTextField());
            this.unitSuffix = unitSuffix;
            this.textField = (JTextField) getComponent();
            this.textField.setBorder(BorderFactory.createLineBorder(new Color(0, 120, 215), 2));

            // Single-click activation
            setClickCountToStart(1);

            // Optional: Restrict typing inside the editor to valid numbers only
            ((AbstractDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                        throws BadLocationException {
                    if (string == null) return;
                    String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                    String next = new StringBuilder(current).insert(offset, string).toString();
                    if (next.matches("\\d*(\\.\\d*)?")) {
                        super.insertString(fb, offset, string, attr);
                    }
                }

                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                        throws BadLocationException {
                    if (text == null) return;
                    String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                    String next = new StringBuilder(current).replace(offset, offset + length, text).toString();
                    if (next.matches("\\d*(\\.\\d*)?")) {
                        super.replace(fb, offset, length, text, attrs);
                    }
                }
            });
        }

        // 1. Called when the user clicks the cell: strip the unit
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            String rawText = (value != null) ? value.toString() : "";
            if (rawText.endsWith(unitSuffix)) {
                rawText = rawText.substring(0, rawText.length() - unitSuffix.length()).trim();
            }
            textField.setText(rawText);
            textField.selectAll(); // Highlight the number so typing replaces it immediately
            return textField;
        }

        // 2. Called when the user finishes editing: append the unit
        @Override
        public Object getCellEditorValue() {
            String input = textField.getText().trim();
            if (input.isEmpty() || input.equals(".")) {
                return "0" + unitSuffix;
            }
            // Remove trailing decimal point if user typed "33."
            if (input.endsWith(".")) {
                input = input.substring(0, input.length() - 1);
            }
            return input + unitSuffix;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TableEditUnits().setVisible(true));
    }
}
