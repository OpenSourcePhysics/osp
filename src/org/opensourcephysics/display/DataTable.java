/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.media.core.NumberField;
import org.opensourcephysics.tools.DataToolTab;

import javajs.async.SwingJSUtils.Performance;

/**
 * DataTable displays multiple TableModels in a table. The first TableModel
 * usually contains the independent variable for the other TableModel so that
 * the visibility of column[0] can be set to false for subsequent TableModels.
 *
 * @author Joshua Gould
 * @author Wolfgang Christian
 * @created February 21, 2002
 * @version 1.0
 */
@SuppressWarnings("serial")
public class DataTable extends JTable {

	/**
	 * A marker type for TableModels that are associated with DataTable. 
	 * 
	 * @author hansonr
	 *
	 */
	public static abstract class OSPTableModel extends AbstractTableModel {
//
//		@Override
//		public boolean isCellEditable(int rowIndex, int columnIndex) {
//			// TableTrackView.TextColumnTableModel only
//			return false;
//		}
//
//		@Override
//		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
//			// TableTrackView.TextColumnTableModel only
//		}
	}

	public static final String PROPERTY_DATATABLE_FORMAT = "format";

	private static final int MODE_MASK_NEW = 0xF;
	public static final int MODE_CREATE = 0x01;
	public static final int MODE_CLEAR = 0x02;
	public static final int MODE_MODEL = 0x03;
	public static final int MODE_TAB = 0x04;


	private static final int MODE_MASK_TRACK = 0x1000;
	public static final int MODE_TRACK_REFRESH = 0x1100;
	public static final int MODE_TRACK_STATE   = 0x1200;
	public static final int MODE_TRACK_STEP = 0x1300;
	public static final int MODE_TRACK_SELECTEDPOINT = 0x1400;
	public static final int MODE_TRACK_STEPS = 0x1500;
	public static final int MODE_TRACK_LOADED = 0x1600;
	public static final int MODE_COL_SETVISIBLE = 0x1700;
	public static final int MODE_TRACK_LOADER = 0x1800;
	public static final int MODE_TRACK_CHOOSE = 0x1900;
	public static final int MODE_TRACK_SELECT = 0x1A00;
	public static final int MODE_TRACK_TRANSFORM = 0x1B00;
	public static final int MODE_TRACK_DATA = 0x1C00;
	public static final int MODE_TRACK_FUNCTION = 0x1D00;
	
	private static final int MODE_MASK_ROW = 0x2000;
	public static final int MODE_APPEND_ROW = 0x2100;
	public static final int MODE_INSERT_ROW = 0x2200;
	public static final int MODE_DELETE_ROW = 0x2300;

	private static final int MODE_MASK_COL = 0x4000;
	public static final int MODE_COLUMN = 0x4100;
	public static final int MODE_CELLS = 0x4200;

	public static final int MODE_VALUES = 0x8000;

	private static final int MODE_MASK_STYLE = 0x800000;
	public static final int MODE_PATTERN = 0x810000;
	public static final int MODE_FUNCTION = 0x820000;
	public static final int MODE_FORMAT = 0x830000;

	public static final int MODE_SELECT  = 0x1000000;
	public static final int MODE_HEADER  = 0x2000000;
	public static final int MODE_SHOW    = 0x4000000;
	public static final int MODE_REFRESH     = 0x8000000;

	public static final int MODE_SET_TAINTED = 0x10000000;

	private static final int MODE_MASK_REBUILD = //
			MODE_MASK_NEW | MODE_MASK_TRACK | //
			MODE_MASK_ROW | MODE_MASK_COL | MODE_MASK_STYLE | //
			MODE_HEADER | MODE_VALUES | MODE_SELECT | MODE_SHOW | MODE_REFRESH;

	public static final int MODE_CANCEL = 0;
	public static final int MODE_UNKNOWN = MODE_MASK_REBUILD;

	private static final Color PANEL_BACKGROUND = javax.swing.UIManager.getColor("Panel.background"); //$NON-NLS-1$
	private static final Color LIGHT_BLUE = new Color(204, 204, 255);

	protected static final String NO_PATTERN = DisplayRes.getString("DataTable.FormatDialog.NoFormat"); //$NON-NLS-1$
	public static final String rowName = DisplayRes.getString("DataTable.Header.Row"); //$NON-NLS-1$

	private static final DoubleRenderer defaultDoubleRenderer = new DoubleRenderer();

	private HashMap<String, PrecisionRenderer> precisionRenderersByColumnName = new HashMap<String, PrecisionRenderer>();
	private HashMap<String, UnitRenderer> unitRenderersByColumnName = new HashMap<String, UnitRenderer>();

	/**
	 * aka JTable.dataModel
	 * 
	 */
	protected OSPDataTableModel dataTableModel;

	protected RowNumberRenderer rowNumberRenderer;

	// int refreshDelay = 0; // time in ms to delay refresh events
	// Timer refreshTimer;

	protected int maximumFractionDigits = 3;
	protected int labelColumnWidth = 40, minimumDataColumnWidth = 24;
	protected NumberFormatDialog formatDialog;
	protected int clickCountToSort = 1;

	private BitSet selectedModelRows = new BitSet();
	private BitSet selectedColumns = new BitSet();
	
	protected int mode;

	/**
	 * Constructs a DataTable with a default data model
	 */
	public DataTable() {
		super();
		init();
	}

	/**
	 * Called by JTable's default constructor.
	 * 
	 */
    @Override
	public void setModel(TableModel dataModel) {
    	// totally ignore JTable's default
    	super.setModel(dataTableModel = createTableModel());
    }

    protected OSPDataTableModel createTableModel() {
    	return new OSPDataTableModel();
	}
    
    @Override
	public void addColumnSelectionInterval(int index0, int index1) {
       dataTableModel.addColumnSelectionInterval(boundColumn(index0), boundColumn(index1));
    }
    
	protected void init() {
		setAutoCreateColumnsFromModel(false);
		setColumnModel(new DataTableColumnModel());
// BH Let Table UI do its thing
//		refreshTimer = new Timer(refreshDelay, new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent evt) {
//				refreshTableNow(mode);
//			}
//		});
//		refreshTimer.setRepeats(false);
//		refreshTimer.setCoalesce(true);
		setColumnSelectionAllowed(true);
		setGridColor(Color.blue);
		setSelectionBackground(LIGHT_BLUE);
		JTableHeader header = getTableHeader();
		header.setForeground(Color.blue); // set text color
		TableCellRenderer headerRenderer = new HeaderRenderer(getTableHeader().getDefaultRenderer());
		getTableHeader().setDefaultRenderer(headerRenderer);
		setSelectionForeground(Color.red); // foreground color for selected cells
		setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		setColumnSelectionAllowed(true);

		// add column sorting using a SortDecorator
		header.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!OSPRuntime.isPopupTrigger(e) && !e.isControlDown() && !e.isShiftDown()
						&& e.getClickCount() == clickCountToSort) {
					TableColumnModel tcm = getColumnModel();
					int vc = tcm.getColumnIndexAtX(e.getX());
					int mc = convertColumnIndexToModel(vc);
					if (dataTableModel.getSortedColumn() != mc) {
						sort(mc);
					}
				}
			}

		});
	}

	@Override
	public void addColumn(TableColumn c) {
		super.addColumn(c);
	}
	
	@Override
	public int convertColumnIndexToModel(int viewIndex) {
		return (viewIndex < 0 ? viewIndex
				: ((DataTableColumnModel) getColumnModel()).convertColumnIndexToModel(viewIndex));
	}

	/**
	 * Sets the maximum number of fraction digits to display in a named column
	 *
	 * @param maximumFractionDigits maximum number of fraction digits to display
	 * @param columnName            name of the column
	 */
	public void setMaximumFractionDigits(String columnName, int maximumFractionDigits) {
		precisionRenderersByColumnName.put(columnName, new PrecisionRenderer(maximumFractionDigits));
	}

	/**
	 * Sets the formatting pattern for a named column
	 *
	 * @param pattern    the pattern
	 * @param columnName name of the column
	 */
	public void setFormatPattern(String columnName, String pattern) {
		if ((pattern == null) || pattern.equals("")) { //$NON-NLS-1$
			precisionRenderersByColumnName.remove(columnName);
		} else {
			precisionRenderersByColumnName.put(columnName, new PrecisionRenderer(pattern));
		}
		firePropertyChange(PROPERTY_DATATABLE_FORMAT, null, columnName); // $NON-NLS-1$
	}

	/**
	 * Sets the units and tooltip for a named column.
	 *
	 * @param columnName name of the column
	 * @param units      the units string (may be null)
	 * @param tootip     the tooltip (may be null)
	 */
	public void setUnits(String columnName, String units, String tooltip) {
		if (units == null) {
			unitRenderersByColumnName.remove(columnName);
		} else {
			TableCellRenderer renderer = precisionRenderersByColumnName.get(columnName);
			if (renderer == null)
				renderer = getDefaultRenderer(Double.class);
//			TableCellRenderer renderer = getDefaultRenderer(Double.class);
//      for (String next: precisionRenderersByColumnName.keySet()) {
//        if(next.equals(columnName)) {
//          renderer = precisionRenderersByColumnName.get(columnName);
//        }
//      }
			UnitRenderer unitRenderer = new UnitRenderer(renderer, units, tooltip);
			unitRenderersByColumnName.put(columnName, unitRenderer);
		}
	}

	/**
	 * Gets the formatting pattern for a named column
	 *
	 * @param columnName name of the column
	 * @return the pattern
	 */
	public String getFormatPattern(String columnName) {
		PrecisionRenderer r = precisionRenderersByColumnName.get(columnName);
		return (r == null) ? "" : r.pattern; //$NON-NLS-1$
	}

	/**
	 * Gets the names of formatted columns Added by D Brown 24 Apr 2011
	 *
	 * @return array of names of columns with non-null formats
	 */
	public String[] getFormattedColumnNames() {
		return precisionRenderersByColumnName.keySet().toArray(new String[0]);
	}

	/**
	 * Gets the formatted value at a given row and column. Added by D Brown 6 Oct
	 * 2010
	 *
	 * @param row the row number
	 * @param col the column number
	 * @return the value formatted as displayed in the table
	 */
	public Object getFormattedValueAt(int row, int col) {
		Object value = getValueAt(row, col);
		if (value == null)
			return null;
		TableCellRenderer renderer = getCellRenderer(row, col);
		Component c = renderer.getTableCellRendererComponent(DataTable.this, value, false, false, 0, 0);
		if (c instanceof JLabel) {
			String s = ((JLabel) c).getText().trim();
			// strip units, if any
			if (renderer instanceof UnitRenderer) {
				String units = ((UnitRenderer) renderer).units;
				if (!"".equals(units)) { //$NON-NLS-1$
					int n = s.lastIndexOf(units);
					if (n > -1)
						s = s.substring(0, n);
				}
			}
			return s;
		}
		return value;
	}

	/**
	 * Gets the format setter dialog.
	 *
	 * @param names    the column name choices
	 * @param selected the initially selected names
	 * @return the format setter dialog
	 */
	public NumberFormatDialog getFormatDialog(String[] names, String[] selected) {
		if (formatDialog == null) {
			formatDialog = new NumberFormatDialog();
			// center on screen
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			int x = (dim.width - formatDialog.getBounds().width) / 2;
			int y = (dim.height - formatDialog.getBounds().height) / 2;
			formatDialog.setLocation(x, y);
		}
		formatDialog.setColumns(names, selected);
		return formatDialog;
	}

	/**
	 * Sets the maximum number of fraction digits to display for cells that have
	 * type Double
	 *
	 * @param maximumFractionDigits - maximum number of fraction digits to display
	 */
	public void setMaximumFractionDigits(int maximumFractionDigits) {
		this.maximumFractionDigits = maximumFractionDigits;
		setDefaultRenderer(Double.class, new PrecisionRenderer(maximumFractionDigits));
	}

	/**
	 * Gets the maximum number of digits in the table.
	 * 
	 * @return int
	 */
	public int getMaximumFractionDigits() {
		return maximumFractionDigits;
	}

	/**
	 * Returns the minimum table width.
	 * 
	 * @return minimum table width.
	 */
	public int getMinimumTableWidth() {
		int n = getColumnCount();
		return (dataTableModel.rowNumberVisible ? labelColumnWidth - minimumDataColumnWidth : 0)
				+ n * minimumDataColumnWidth;
	}

	/**
	 * Sets the label column width
	 * 
	 * @param w the width
	 */
	public void setLabelColumnWidth(int w) {
		labelColumnWidth = w;
		((DataTableColumnModel) getColumnModel()).invalidateWidths();
	}

	@Override
	public void resizeAndRepaint() {
		super.resizeAndRepaint();
	}

	/**
	 * Sets the display row number flag. Table displays row number.
	 *
	 * @param b <code>true<\code> if table display row number
	 */
	public void setRowNumberVisible(boolean b) {
		if (dataTableModel.isRowNumberVisible() != b) {
			if (b && (rowNumberRenderer == null)) {
				rowNumberRenderer = new RowNumberRenderer(this);
			}
			dataTableModel.setRowNumberVisible(b);
		}
	}

	/**
	 * Sets the stride of a TableModel in the DataTable.
	 *
	 * @param tableModel
	 * @param stride
	 */
	public void setStride(TableModel tableModel, int stride) {
		dataTableModel.setStride(tableModel, stride);
	}

	/**
	 * Sets the visibility of a column of a TableModel in the DataTable.
	 *
	 * @param tableModel
	 * @param columnIndex
	 * @param b
	 */
	public void setColumnVisible(TableModel tableModel, int columnIndex, boolean b) {
		dataTableModel.setColumnVisible(tableModel, columnIndex, b);
	}

	/**
	 * Gets the display row number flag.
	 *
	 * @return The rowNumberVisible value
	 */
	public boolean isRowNumberVisible() {
		return dataTableModel.isRowNumberVisible();
	}

	/**
	 * Returns an appropriate renderer for the cell specified by this row and
	 * column. If the <code>TableColumn</code> for this column has a non-null
	 * renderer, returns that. If the <code>TableColumn</code> for this column has
	 * the same name as a name specified in the setMaximumFractionDigits method,
	 * returns the appropriate renderer. If not, finds the class of the data in this
	 * column (using <code>getColumnClass</code>) and returns the default renderer
	 * for this type of data.
	 *
	 * @param row    Description of Parameter
	 * @param column Description of Parameter
	 * @return The cellRenderer value
	 */
	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		int i = convertColumnIndexToModel(column);
		if ((i == 0) && dataTableModel.isRowNumberVisible()) {
			return rowNumberRenderer;
		}
		UnitRenderer unitRenderer = null;
		TableCellRenderer baseRenderer = null;
		try {
			// find units renderer
			// BH 2020.02.14 efficiencies -- needs doublechecking
			TableColumn tableColumn = getColumnModel().getColumn(column);
			unitRenderer = unitRenderersByColumnName.get(tableColumn.getHeaderValue());
//      Iterator<String> keys = unitRenderersByColumnName.keySet().iterator();
//      while(keys.hasNext()) {
//        String columnName = keys.next();
//        if(tableColumn.getHeaderValue().equals(columnName)) {
//          unitRenderer = unitRenderersByColumnName.get(columnName);
//          break;
//        }
//      }
			// find base renderer
			baseRenderer = tableColumn.getCellRenderer();
			if (baseRenderer == null) {
				String key = (String) tableColumn.getHeaderValue();
				baseRenderer = precisionRenderersByColumnName.get(key);
				if (baseRenderer == null && key.endsWith(DataToolTab.SHIFTED)) {
					baseRenderer = precisionRenderersByColumnName.get(key.substring(0, key.length() - 1));
				}
			}
//      if (baseRenderer==null) {
//	      keys = precisionRenderersByColumnName.keySet().iterator();
//	      while(keys.hasNext()) {
//	        String columnName = keys.next();
//	        if(tableColumn.getHeaderValue().equals(columnName)) {
//	        	baseRenderer = precisionRenderersByColumnName.get(columnName);
//	        	break;
//	        }
//	        else if(tableColumn.getHeaderValue().equals(columnName+DataToolTab.SHIFTED)) {
//	        	baseRenderer = precisionRenderersByColumnName.get(columnName);
//	        	break;
//	        }
//	      }
//      }
		} catch (Exception ex) {
		}
		// if no precision base renderer, use default
		if (baseRenderer == null) {
			if (getColumnClass(column).equals(Double.class)) {
				baseRenderer = defaultDoubleRenderer;
			} else {
				baseRenderer = getDefaultRenderer(getColumnClass(column));
			}
		}
		// return unit renderer if defined
		if (unitRenderer != null) {
			unitRenderer.setBaseRenderer(baseRenderer);
			return unitRenderer;
		}
		return baseRenderer;
	}

	/**
	 * Gets the precision renderer, if any, for a given columnn name. Added by D
	 * Brown Dec 2010
	 *
	 * @param columnName the name
	 * @return the PrecisionRenderer, or null if none
	 */
	public TableCellRenderer getPrecisionRenderer(String columnName) {
		return precisionRenderersByColumnName.get(columnName);
	}

	/**
	 * Sets the delay time for table refresh timer. Only called by TableTrackView
	 * dispose
	 *
	 * @param delay the delay in millisecond
	 * @deprecated
	 */
	public void setRefreshDelay(int delay) {
//		if (delay > 0) {
//			refreshTimer.setDelay(delay);
//			refreshTimer.setInitialDelay(delay);
//		} else {
//			refreshTimer.stop();
//		}
//		refreshDelay = delay;
	}

	/**
	 * Clear the table and stop refreshing. Only called by TableTrackView dispose.
	 *
	 * @param delay the delay in millisecond
	 */
	public void dispose() {
		clear();
//		setRefreshDelay(-1);
	}

	/*
	 * @deprecated Refresh the table by rebuilding everything.
	 * 
	 */
	public void refreshTable() {
		refreshTable(MODE_UNKNOWN);
	}

	/**
	 * Refresh the data in the DataTable, as well as other changes to the table,
	 * such as row number visibility. Changes to the TableModels displayed in the
	 * table will not be visible until this method is called.
	 */
	public void refreshTable(int mode) {
//		if (refreshDelay > 0) {
//			refreshTimer.start();
//		} else {
//			OSPRuntime.postEvent(new Runnable() {
//				@Override
//				public synchronized void run() {
		refreshTableNow(mode);

//
//			});
//		}
	}

	/**
	 * Performs the action for the refresh timer and refreshTable() method by
	 * refreshing the data in the DataTable.
	 *
	 * @param cause allows more nuanced refresh
	 */

	protected void refreshTableNow(int mode) {
		OSPLog.debug(Performance.timeCheckStr("DataTable.refreshTable0 " + mode,
				Performance.TIME_MARK));
		// BH every sort of refresh goes through here
		boolean columnsChanged;
		
		int mask = this.mode = mode;
		switch (mode) {
		case MODE_CANCEL: // 0x00;
			return;
		case MODE_SET_TAINTED: // 0x10000000
			dataTableModel.columnCount = -1;
			dataTableModel.rowCount = -1;
			return;
			//
			// column (structure) changes
			//
		default:
		case MODE_CREATE: // 0x01;
		case MODE_CLEAR: // 0x02;
		case MODE_MODEL: // 0x03;
		case MODE_TAB: // 0x04;
			mask = MODE_MASK_NEW;
			columnsChanged = true;
		break;
		case MODE_TRACK_REFRESH: // 0x1100;
		case MODE_TRACK_STATE: // 0x1200;
		case MODE_TRACK_STEPS: // 0x1500;
		case MODE_TRACK_LOADED: // 0x1600;
		case MODE_TRACK_LOADER: // 0x1800;
			mask = MODE_MASK_TRACK; // 0x1000;
			columnsChanged = true;
			break;
		case MODE_COLUMN: // 0x2400;
		case MODE_CELLS: // 0x2800;
			mask = MODE_MASK_COL;
			columnsChanged = true;
			break;
		case MODE_REFRESH: // 0x8000000;
			columnsChanged = true;
			break;
			//  row/rendering changes
		case MODE_COL_SETVISIBLE: // 0x1700;
		case MODE_TRACK_STEP: // 0x1300;
		case MODE_TRACK_SELECTEDPOINT: // 0x1400;
		case MODE_TRACK_TRANSFORM: // 0x1B00;
		case MODE_TRACK_DATA: // 0x1C00;
		case MODE_TRACK_FUNCTION: // 0x1D00;
		case MODE_TRACK_CHOOSE: // 0x1900;
		case MODE_TRACK_SELECT: // 0x1A00;
			mask = MODE_MASK_TRACK; // 0x1000;
			columnsChanged = false;
			break;
		case MODE_INSERT_ROW: // 0x2100;
		case MODE_DELETE_ROW: // 0x2200;
		case MODE_APPEND_ROW: // 0x2300;
			mask = MODE_MASK_ROW;
			columnsChanged = false;
			break;
		case MODE_PATTERN: // 0x810000;
		case MODE_FUNCTION: // 0x820000;
		case MODE_FORMAT: // 0x830000;
			mode = MODE_MASK_STYLE;
			columnsChanged = false;
			break;
		case MODE_VALUES:  // 0x4000;
		case MODE_SELECT:  // 0x1000000;
		case MODE_HEADER:  // 0x2000000;
		case MODE_SHOW:    // 0x4000000;
			columnsChanged = false;
			break;
		}

		OSPLog.debug(">>>>DataTable.refreshTableNow " + Integer.toHexString(mode) + " " + columnsChanged);


//		columnsChanged = true;
		dataTableModel.refresh(mask);
		if (columnsChanged) {
			dataTableModel.fireTableStructureChanged();
		} else {
			repaint();
		}
		OSPLog.debug(Performance.timeCheckStr("DataTable.refreshTable1 " + Integer.toHexString(mode),
		Performance.TIME_MARK));
	}

	/**
	 * Add a TableModel object to the table model list.
	 *
	 * @param tableModel
	 */
	public void add(TableModel tableModel) {
		dataTableModel.add(tableModel);
	}

	/**
	 * Remove a TableModel object from the table model list.
	 *
	 * @param tableModel
	 */
	public void remove(TableModel tableModel) {
		dataTableModel.remove(tableModel);
	}

	/**
	 * Remove all TableModels from the table model list.
	 */
	public void clear() {
		dataTableModel.clear();
	}

	/**
	 * A wrapper for a TableModel
	 *
	 */
	protected static class DataTableElement {

		final TableModel tableModel;

		/**
		 * the column "return" value from find
		 */
		int foundColumn = -1;

		/**
		 * indicating if a column is visible
		 */
		final BitSet bsColVis = new BitSet();

		/**
		 * The number of actual rows per visible row
		 */
		int stride = 1;

		/**
		 * Constructor DataTableElement
		 *
		 * @param t
		 */
		public DataTableElement(TableModel t) {
			tableModel = t;
			bsColVis.set(0, t.getColumnCount());
		}

		/**
		 * Method setStride
		 *
		 * @param _stride
		 */
		public void setStride(int _stride) {
			stride = _stride;
		}

		/**
		 * Method setColumnVisible
		 *
		 * @param columnIndex
		 * @param visible
		 */
		public void setColumnVisible(int columnIndex, boolean visible) {
			bsColVis.set(columnIndex, visible);
		}

		/**
		 * Method getStride
		 *
		 * @return
		 */
		public int getStride() {
			return stride;
		}

		/**
		 * Method getColumnCount
		 *
		 * @return
		 */
		public int getVisibleColumnCount() {
			return bsColVis.cardinality();
		}

		/**
		 * Method getValueAt
		 *
		 * @param rowIndex
		 * @param columnIndex
		 * @return
		 */
		public Object getValueAt(int rowIndex, int columnIndex) {
			return tableModel.getValueAt(rowIndex, columnIndex);
		}

		/**
		 * Method getColumnName
		 *
		 * @param columnIndex
		 * @return
		 */
		public String getColumnName(int columnIndex) {
			return tableModel.getColumnName(columnIndex);
		}

		/**
		 * Method getColumnClass
		 *
		 * @param columnIndex
		 * @return
		 */
		public Class<?> getColumnClass(int columnIndex) {
			return ((OSPDataTableModel) tableModel).getColumnClass(columnIndex);
		}

		/**
		 * Method getRowCount
		 *
		 * @return
		 */
		public int getRowCount() {
			return tableModel.getRowCount();
		}
		
		@Override
		public String toString() {
			return "DataTableElement " + tableModel.getRowCount() + "x" + tableModel.getColumnCount() + " vis=" + bsColVis;
		}
	}

	/*
	 * DefaultDataTableModel acts on behalf of the TableModels that the DataTable
	 * contains. It combines data from these multiple sources and allows the
	 * DataTable to display data is if the data were from a single source.
	 *
	 * @author jgould
	 * 
	 * @created February 21, 2002
	 */
	public class OSPDataTableModel extends AbstractTableModel implements TableModelListener {

		final private ArrayList<DataTableElement> dataTableElements;
		boolean rowNumberVisible;
		private SortDecorator decorator;
		protected TableModelEvent lastModelEvent;

		protected int columnCount;
		private int rowCount;
		protected boolean useDefaultColumnClass;

		/**
		 * The TableModelListener will clue us in
		 */
		protected void setTainted() {
			columnCount = rowCount = -1;
		}

	    public void addColumnSelectionInterval(int coli, int colj) {
	        columnModel.getSelectionModel().addSelectionInterval(coli, colj);
			int labelCol = convertColumnIndexToView(0);
			selectedColumns.clear();
			int[] selected = getSelectedColumns(); // selected view columns
			for (int i = 0; i < selected.length; i++) {
				if (selected[i] == labelCol) {
					continue;
				}
				int modelCol = convertColumnIndexToModel(selected[i]);
				selectedColumns.set(modelCol);
			}
			if (selectedColumns.isEmpty()) {
				clearSelection();
			}
	    }

		public OSPDataTableModel() {
			dataTableElements = new ArrayList<DataTableElement>();
			decorator = new SortDecorator();
			addTableModelListener(new TableModelListener() {
				@Override
				public void tableChanged(TableModelEvent e) {
					setTainted();
					decorator.allocate();
				}

			});
		}

		/**
		 * Find the dataTableElement associated with this column.
		 *
		 * @param rowNumberVisible
		 * @param dataTableElements
		 * @param icol
		 * @return
		 */
		private DataTableElement find(int icol) {
			if (rowNumberVisible) {
				icol--;
			}
			for (int i = 0, ncol = 0, n = dataTableElements.size(); i < n; i++) {
				DataTableElement dte = dataTableElements.get(i);
				int nvis = dte.getVisibleColumnCount();
				if (ncol + nvis > icol) {
					i = icol - ncol;
					BitSet bs = dte.bsColVis;
					// have undercounted by the number of hidden columns in this element
					// up to this column
					for (int j = ncol; j < icol; j++) {
						if (!bs.get(j)) {
							i++;
						}
					}
					dte.foundColumn = i;
					return dte;
				}
				ncol += nvis;
			}
			return null; // this shouldn't happen
		}

		/**
		 * Sorts the table rows using the given column.
		 * 
		 * @param col int
		 */
		public void sort(int col) {
			decorator.sort(col);
		}

		/**
		 * Gets the sorted column. Added by D Brown 2010-10-24.
		 * 
		 * @return the
		 */
		public int getSortedColumn() {
			return decorator.getSortedColumn();
		}

		public void resetSort() {
			decorator.reset();
		}

		public int getSortedRow(int j) {
			return decorator.getSortedRow(j);
		}

		/**
		 * Method setColumnVisible
		 *
		 * @param tableModel
		 * @param columnIndex If negative, update to all columns visible
		 * @param b
		 */
//		@Override
		public void setColumnVisible(TableModel tableModel, int columnIndex, boolean b) {
			DataTableElement dte = findElementContaining(tableModel);
			if (columnIndex >= 0) {
				dte.setColumnVisible(columnIndex, b);
			} else {
				dte.bsColVis.clear();
				int n = dte.tableModel.getColumnCount();
				dte.bsColVis.set(0, n);
				//OSPLog.debug("DataTable n=" +n + " " + dte);
			}
		}

		/**
		 * Method setStride
		 *
		 * @param tableModel
		 * @param stride
		 */
//		@Override
		public void setStride(TableModel tableModel, int stride) {
			findElementContaining(tableModel).setStride(stride);
		}

		/**
		 * Method setRowNumberVisible
		 *
		 * @param b
		 */
//		@Override
		public void setRowNumberVisible(boolean b) {
			rowNumberVisible = b;
		}

		/**
		 * Method setValueAt modified by Doug Brown 12/19/2013
		 *
		 * @param value
		 * @param rowIndex
		 * @param columnIndex
		 */
		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			decorator.setValueAt(value, rowIndex, columnIndex);
		}

		protected void setElementValue(Object value, int rowIndex, int columnIndex) {
			if (dataTableElements.size() == 0 || rowNumberVisible && columnIndex == 0) {
				return;
			}
			DataTableElement dte = find(columnIndex);
			int stride = dte.getStride();
			rowIndex = rowIndex * stride;
			if (rowIndex >= dte.getRowCount()) {
				return;
			}
			dte.tableModel.setValueAt(value, rowIndex, dte.foundColumn);
		}

		/**
		 * Method isRowNumberVisible
		 *
		 * @return
		 */
//		@Override
		public boolean isRowNumberVisible() {
			return rowNumberVisible;
		}

		/**
		 * Method getColumnName
		 *
		 * @param columnIndex
		 * @return the name
		 */
		@Override
		public String getColumnName(int columnIndex) {

			if (columnIndex >= getColumnCount()) {
				return "unknown"; //$NON-NLS-1$
			}

			if ((dataTableElements.size() == 0) && !rowNumberVisible) {
				return null;
			}
			if (rowNumberVisible) {
				if (columnIndex == 0) {
					return rowName;
				}
			}
			DataTableElement dte = find(columnIndex);
			return dte.getColumnName(dte.foundColumn);
		}

		/**
		 * Method getRowCount
		 *
		 * @return
		 */
		@Override
		public synchronized int getRowCount() {
			if (rowCount >= 0)
				return rowCount;
			int n = 0;
			for (int i = dataTableElements.size(); --i >= 0;) {
				DataTableElement dte = dataTableElements.get(i);
				int stride = dte.getStride();
				n = Math.max(n, (dte.getRowCount() + stride - 1) / stride);
			}
			return rowCount = n;
		}

		/**
		 * Method getColumnCount
		 *
		 * @return the number of VISIBLE columns
		 */
		@Override
		public synchronized int getColumnCount() {
			if (columnCount >= 0)
				return columnCount;
			int n = 0;
			for (int i = dataTableElements.size(); --i >= 0;) {
				n += dataTableElements.get(i).getVisibleColumnCount();
			}
			return columnCount = (rowNumberVisible ? n + 1 : n);
		}

		/**
		 * Method getValueAt
		 *
		 * @param rowIndex
		 * @param columnIndex
		 * @return
		 */
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return decorator.getValueAt(rowIndex, columnIndex);
		}

		public Object getElementValue(int rowIndex, int columnIndex) {
			if (dataTableElements.size() == 0) {
				return null;
			}
			if (rowNumberVisible) {
				if (columnIndex == 0) {
					return Integer.valueOf(rowIndex);
				}
			}
			DataTableElement dte = find(columnIndex);
			int stride = dte.getStride();
			rowIndex = rowIndex * stride;
			if (rowIndex >= dte.getRowCount()) {
				return null;
			}
			return dte.getValueAt(rowIndex, dte.foundColumn);
		}
		

		/**
		 * Get an array of column values ready for sorting.
		 * 
		 * @param columnIndex
		 * @param objects
		 * @return
		 */
		public Object[] getElementValues(int columnIndex, Object[] objects) {
			if (dataTableElements.size() > 0) {
				boolean asRow = (rowNumberVisible && columnIndex == 0);
				DataTableElement dte = find(columnIndex);
				int stride = dte.getStride();
				for (int i = 0, rowIndex = 0, n = objects.length, nr = dte.getRowCount(); i < n
						&& rowIndex < nr; i++, rowIndex += stride) {
					objects[i] = (asRow ? Integer.valueOf(rowIndex) : dte.getValueAt(rowIndex, dte.foundColumn));
				}
			}
			return objects;
		}



		/**
		 * Method getColumnClass
		 *
		 * @param columnIndex
		 * @return
		 */
		@Override
		public Class<?> getColumnClass(int columnIndex) {

			if (columnIndex >= getColumnCount()) {
				return Object.class;
			}

			if (!useDefaultColumnClass)
			{
				return super.getColumnClass(columnIndex);
			}
			if (rowNumberVisible) {
				if (columnIndex == 0) {
					return Integer.class;
				}
			}
			if ((columnIndex == 0) && rowNumberVisible) {
				columnIndex--;
			}
			
			
			DataTableElement dte = find(columnIndex);
			return dte.getColumnClass(dte.foundColumn);
		}

		/**
		 * Method isCellEditable
		 *
		 * @param rowIndex
		 * @param columnIndex
		 * @return true if editable
		 */
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return (columnIndex < getColumnCount() && isElementEditable(rowIndex, columnIndex));
		}

		public boolean isElementEditable(int row, int col) {
			return false;
		}

		/**
		 * Method remove
		 *
		 * @param tableModel
		 */
//		@Override
		public void remove(TableModel tableModel) {
			tableModel.removeTableModelListener(this);
			dataTableElements.remove(findElementContaining(tableModel));
		}

		/**
		 * Method clear
		 */
//		@Override
		public void clear() {
			for (int i = dataTableElements.size(); --i >= 0;)
				dataTableElements.get(i).tableModel.removeTableModelListener(this);
			dataTableElements.clear();
		}

		/**
		 * Method add
		 *
		 * @param tableModel
		 */
//		@Override
		public void add(TableModel tableModel) {
			setTainted();
			dataTableElements.add(new DataTableElement(tableModel));
			tableModel.addTableModelListener(this);
		}

		/**
		 * returns the DataTableElement that contains the specified TableModel
		 *
		 * @param tableModel
		 * @return Description of the Returned Value
		 */
		private DataTableElement findElementContaining(TableModel tableModel) {
			for (int i = dataTableElements.size(); --i >= 0;) {
				DataTableElement dte = dataTableElements.get(i);
				if (dte.tableModel == tableModel) {
					return dte;
				}
			}
			return null;
		}

		public class SortDecorator {
			private int[] indexes = new int[0];
			private int sortedColumn; // added by D Brown 2010-10-24

			/**
			 * Constructor SortDecorator
			 * 
			 * @param model
			 */
			public SortDecorator() {
			}

			/**
			 * Gets the sorted row number for a given real model row Added by D Brown Dec
			 * 2017
			 * 
			 * @param realModelRow the unsorted row number
			 * @return the sorted row number
			 */
			public int getSortedRow(int realModelRow) {
				for (int i = 0; i < indexes.length; i++) {
					if (indexes[i] == realModelRow)
						return i;
				}
				return -1;
			}

			protected Object getValueAt(int row, int column) {
				if (column >= getColumnCount()) {
					return null;
				}
				if (indexes.length <= row) {
					allocate();
				}
				Object o = getElementValue(indexes[row], column);
//				OSPLog.debug("DataTable getValueAt " + row + " "+ column 
//						+ " = " + o 
//						+ " " + getColumnModel().getColumnCount()
//						+ " " + getColumnModel().getColumn(column).getWidth()
//						+ " " + getColumnModel().getTotalColumnWidth());
				return o;
			}

			public void setValueAt(Object aValue, int row, int column) {
				if (indexes.length <= row) {
					allocate();
				}
				setElementValue(aValue, indexes[row], column);
			}

			public void sort(int column) {
				sortedColumn = column;
				int rowCount = getRowCount();
				if (indexes.length <= rowCount) {
					allocate();
				}

				// new faster sort method added by D Brown 2015-05-16
				try {
					Object[] data = getElementValues(column, new Object[rowCount]);
					Object[][] sortArray = new Object[rowCount][2];
					for (int i = 0; i < rowCount; i++) {
						sortArray[i][0] = data[i];
						sortArray[i][1] = Integer.valueOf(indexes[i]);
					}
					// Comparators can be static; either Number or String, depending upon the column type
					if (Number.class.isAssignableFrom(getColumnClass(column))) {
						Arrays.sort(sortArray, nCompare);
					} else { 
						Arrays.sort(sortArray, sCompare);					
					}
					for (int i = 0; i < rowCount; i++) {
						indexes[i] = ((Integer)sortArray[i][1]).intValue();
					}
				} catch (Exception e) {
				}
			}

			// added by D Brown 2010-10-24
			public int getSortedColumn() {
				return sortedColumn;
			}

//			public void swap(int i, int j) {
//				int tmp = indexes[i];
//				indexes[i] = indexes[j];
//				indexes[j] = tmp;
//			}
//
//			public int compare(int i, int j, int column) {
//				Object io = getElementValue(i, column);
//				Object jo = getElementValue(j, column);
//				if ((io != null) && (jo == null)) {
//					return 1;
//				}
//				if ((io == null) && (jo != null)) {
//					return -1;
//				}
//				if ((io == null) && (jo == null)) {
//					return 0;
//				}
//				if ((io instanceof Number) && (jo instanceof Number)) {
//					double a = ((Number) io).doubleValue();
//					double b = ((Number) jo).doubleValue();
//					return (b < a) ? -1 : ((b > a) ? 1 : 0);
//				}
//				return jo.toString().compareTo(io.toString());
//			}
//
			private void allocate() {
				indexes = new int[getRowCount()];
				for (int i = 0; i < indexes.length; ++i) {
					indexes[i] = i;
				}
			}

			public void reset() {
				allocate();
				sortedColumn = -1;
			}

		}

		/**
		 * This will be from the DataSet sub-TableModels, specifically for APPEND for
		 * now.
		 * 
		 */
		@Override
		public void tableChanged(TableModelEvent e) {
			lastModelEvent = e;
		}

		protected void refresh(int mask) {
			String type;
			switch (mask) {
			default:
			case MODE_MASK_REBUILD:
				type = "rebuild";
				break;
			case MODE_MASK_NEW:
				type = "new";
				break;
			case MODE_MASK_TRACK:
				type = "track";
				break;
			case MODE_HEADER:
				type = "header";
				break;
			case MODE_MASK_ROW:
				type = "row";
				break;
			case MODE_MASK_COL:
				type = "col";
				break;
			case MODE_VALUES:
				type = "values";
				break;
			case MODE_MASK_STYLE:
				type = "style";
				break;
			case MODE_SELECT:
				type = "select";
				break;
			case MODE_SHOW:
				type = "show";
				break;
			}
			updateFormats();
			updateColumnModel(null);
			OSPLog.debug("OSPDataTableModel rebuild " + type);
		}

	}
	
	public class DataTableColumnModel extends DefaultTableColumnModel {

		private DataTableColumnModel() {
			super();
		}
		
		public class DataTableColumn extends TableColumn {

			private boolean isSizeSet;

			private DataTableColumn(int modelIndex) {
				super(modelIndex);
				setHeaderValue(getModel().getColumnName(modelIndex));
			}

		}

		/**
		 * New labelColumnWidth requires resetting min/max widths.
		 */
		private void invalidateWidths() {
			for (int i = tableColumns.size(); --i >= 0;) {
				((DataTableColumn) tableColumns.get(i)).isSizeSet = false;
			}
		}

		/**
		 * Read DataTableModel.dataElements into DataTableColumnModel.tableColumns.
		 * 
		 * This method has full control over the column model. It does not call any
		 * methods in JTable. Specifically, we do not need to fire any events or call
		 * JTable.createDefaultColumnsFromModel().
		 * 
		 * We do not need to recreate any table columns from scratch. We just need to
		 * adjust them as necessary.
		 * 
		 * @author hansonr
		 */
		private void updateColumnModel() {
			
			// create a map of the current column set (actually displayed)

			Vector<TableColumn> newColumns = new Vector<>();
			Map<String, DataTableColumn> map = new HashMap<>();
			for (int i = tableColumns.size(); --i >= 0;) {
				DataTableColumn tc = (DataTableColumn) tableColumns.get(i);
				map.put((String) tc.getHeaderValue(), tc);
			}

			// run through the updated TableModel, creating new columns
			// only when necessary. No events are fired.

			int n = dataTableModel.getColumnCount();
			for (int i = 0; i < n; i++) {
				String name = dataTableModel.getColumnName(i);
				DataTableColumn tc = map.get(name);
				if (tc == null) {
					tc = new DataTableColumn(i);
					tc.addPropertyChangeListener(this);
					totalColumnWidth = -1;
				} else {
					tc.setModelIndex(i);
					tableColumns.remove(tc);
				}
				tc.isSizeSet = false;
				newColumns.add(tc);
			}
			// any columns remaining in tableColumns are disposable
			// because they are no longer being shown.
			for (int i = tableColumns.size(); --i >= 0;) {
				TableColumn tc = tableColumns.get(i);
				tc.removePropertyChangeListener(this);
				totalColumnWidth = -1;
			}
			tableColumns = newColumns;
			selectionModel.clearSelection();
		}

		public int convertColumnIndexToModel(int viewIndex) {
			if (dataTableModel.getColumnCount() != tableColumns.size())
				updateColumnModel();
				
			return getColumn(viewIndex).getModelIndex();
		}

//		public void createDefaultColumns() {
//
//			selectionModel.clearSelection();
//			// remove and dispose of all table columns
//			while (getColumnCount() > 0) {
//				TableColumn column = getColumn(0);
//				int columnIndex = tableColumns.indexOf(column);
//				if (columnIndex >= 0) {
//					column.removePropertyChangeListener(this);
//					tableColumns.removeElementAt(columnIndex);
//					totalColumnWidth = -1;
//				}
//			}
//			// recreate tableColumns with columns from dataTableModel
//			int n = dataTableModel.getColumnCount();
//			for (int i = 0; i < n; i++) {
//				addColumn(new DataTableColumn(i));
//			}
//		}

	    @Override
		public void addColumn(TableColumn c) {
	    	super.addColumn(c);
	    	dataTableModel.columnCount = -1;
	    }
	    
		/**
		 * Method getColumn
		 *
		 * @param columnIndex
		 * @return
		 */
		@Override
		public TableColumn getColumn(int columnIndex) {
			if (columnIndex < 0 || columnIndex >= tableColumns.size())
				return new TableColumn(0);
			DataTableColumn tableColumn = (DataTableColumn) tableColumns.elementAt(columnIndex);
			if (tableColumn.isSizeSet)
				return tableColumn;
			tableColumn.isSizeSet = true;
			String headerValue = (String) tableColumn.getHeaderValue();
			if (headerValue != null) {
				if (headerValue.equals(rowName) && (tableColumn.getModelIndex() == 0)) {
					tableColumn.setMaxWidth(labelColumnWidth);
					tableColumn.setMinWidth(labelColumnWidth);
					tableColumn.setResizable(false);
				} else {
					tableColumn.setMinWidth(minimumDataColumnWidth);
				}
			}
			return tableColumn;
		}

//		public void moveColumnQuietly(int oldI, int newI) {
//			if (oldI == newI) {
//				return;
//			}
//			TableColumn aColumn = tableColumns.elementAt(oldI);
//			tableColumns.removeElementAt(oldI);
//			tableColumns.insertElementAt(aColumn, newI);
//			boolean selected = selectionModel.isSelectedIndex(oldI);
//			selectionModel.removeIndexInterval(oldI, oldI);
//			selectionModel.insertIndexInterval(newI, 1, true);
//			if (selected) {
//				selectionModel.addSelectionInterval(newI, newI);
//			} else {
//				selectionModel.removeSelectionInterval(newI, newI);
//			}
//		}

		private void setModelColumnOrder(int[] modelColumns) {
			selectionModel.clearSelection();
			int[] current = getModelColumnOrder();
			if (Arrays.equals(current, modelColumns))
				return;
			int max = 0;
			for (int i = current.length; --i >= 0;) {
				if (current[i] > max)
					max = current[i];
			}
			int map[] = new int[max + 1];
			for (int i = current.length; --i >= 0;) {
				map[current[i]] = i + 1; // so 0 is undefined
			}
			Vector<TableColumn> newCols = new Vector<>();
			for (int i = 0, n = modelColumns.length; i < n; i++) {
				int j = (modelColumns[i] > max ? 0 : map[modelColumns[i]]);
				if (j > 0)
					newCols.add(tableColumns.get(j - 1));
			}
			tableColumns = newCols;
		}

		/**
		 * Get modelIndex array for currently visible columns.
		 * 
		 * @return
		 */
		private int[] getModelColumnOrder() {
			int[] modelColumns = new int[getModel().getColumnCount()];
			for (int i = modelColumns.length; --i >= 0;) {
				modelColumns[i] = getColumn(i).getModelIndex();
			}
			return modelColumns;
		}

	}

	/**
	 * A default double renderer for the table
	 */
	protected static class DoubleRenderer extends DefaultTableCellRenderer {
		NumberField numberField;

		/**
		 * Constructor
		 */
		public DoubleRenderer() {
			super();
			numberField = new NumberField(0);
			setHorizontalAlignment(SwingConstants.RIGHT);
			setBackground(Color.WHITE);
		}

		@Override
		public void setValue(Object value) {
			if (value == null) {
				setText(""); //$NON-NLS-1$
				return;
			}
			numberField.setValue((Double) value);
			setText(numberField.getText());
		}

		/**
		 * Gets the number format
		 */
		DecimalFormat getFormat() {
			return numberField.getFormat();
		}

	}

	/**
	 * A settable precision double renderer for the table
	 */
	protected static class PrecisionRenderer extends DefaultTableCellRenderer {
		DecimalFormat numberFormat;
		String pattern;

		/**
		 * PrecisionRenderer constructor
		 *
		 * @param precision - maximum number of fraction digits to display
		 */
		private PrecisionRenderer(int precision) {
			super();
			numberFormat = (DecimalFormat) NumberFormat.getInstance();
			numberFormat.setMaximumFractionDigits(precision);
			setHorizontalAlignment(SwingConstants.RIGHT);
			setBackground(Color.WHITE);
		}

		/**
		 * PrecisionRenderer constructor
		 *
		 * @param pattern a formatting pattern
		 */
		private PrecisionRenderer(String pattern) {
			super();
			numberFormat = (DecimalFormat) NumberFormat.getInstance();
			numberFormat.applyPattern(pattern);
			this.pattern = pattern;
			setHorizontalAlignment(SwingConstants.RIGHT);
		}

		/**
		 * Sets the maximum number of fraction digits to display
		 *
		 * @param precision - maximum number of fraction digits to display
		 */
		public void setPrecision(int precision) {
			numberFormat.setMaximumFractionDigits(precision);
		}

		@Override
		public void setValue(Object value) {
			setText((value == null) ? "" : numberFormat.format(value)); //$NON-NLS-1$
		}

	}

	protected static class RowNumberRenderer extends JLabel implements TableCellRenderer {
		JTable table;

		/**
		 * RowNumberRenderer constructor
		 *
		 * @param _table Description of Parameter
		 */
		private RowNumberRenderer(JTable _table) {
			super();
			table = _table;
			setHorizontalAlignment(SwingConstants.RIGHT);
			setOpaque(true); // make background visible.
			setForeground(Color.black);
			setBackground(PANEL_BACKGROUND);
		}

		/**
		 * returns a JLabel that is highlighted if the row is selected.
		 *
		 * @param table
		 * @param value
		 * @param isSelected
		 * @param hasFocus
		 * @param row
		 * @param column
		 * @return
		 */
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			if (table.isRowSelected(row)) {
				int[] i = table.getSelectedColumns();
				if ((i.length == 1) && (table.convertColumnIndexToModel(i[0]) == 0)) {
					setBackground(PANEL_BACKGROUND);
				} else {
					setBackground(Color.gray);
				}
			} else {
				setBackground(PANEL_BACKGROUND);
			}
			setText(value.toString());
			return this;
		}

	}

	/**
	 * A cell renderer that adds units to displayed values. Added by D Brown Dec
	 * 2010
	 */
	protected static class UnitRenderer implements TableCellRenderer {
		private TableCellRenderer baseRenderer;
		private String units;
		private String tooltip;

		/**
		 * UnitRenderer constructor
		 *
		 * @param renderer a TableCellRenderer
		 * @param factor   a conversion factor
		 */
		private UnitRenderer(TableCellRenderer renderer, String units, String tooltip) {
			super();
			this.units = units;
			this.tooltip = tooltip;
			setBaseRenderer(renderer);
		}

		/**
		 * Sets the base renderer.
		 * 
		 * @param renderer the base renderer
		 */
		private void setBaseRenderer(TableCellRenderer renderer) {
			this.baseRenderer = renderer;
		}

		/**
		 * Returns the rendered component.
		 */
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			Component c = baseRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (c instanceof JLabel && units != null) {
				JLabel label = (JLabel) c;
				if (label.getText() != null && !label.getText().equals("")) //$NON-NLS-1$
					label.setText(label.getText() + units);
				label.setToolTipText(tooltip);
			}
			return c;
		}

	}

	public class NumberFormatDialog extends JDialog {
		JButton closeButton, cancelButton, helpButton, applyButton;
		JLabel patternLabel, sampleLabel;
		JTextField patternField, sampleField;
		java.text.DecimalFormat sampleFormat;
		String[] displayedNames;
		Map<String, String> realNames = new HashMap<String, String>();
		Map<String, String> prevPatterns = new HashMap<String, String>();
		JList<String> columnList;
		JScrollPane columnScroller;

		private NumberFormatDialog() {
			super(JOptionPane.getFrameForComponent(DataTable.this), true);
			setLayout(new BorderLayout());
			setTitle(DisplayRes.getString("DataTable.NumberFormat.Dialog.Title")); //$NON-NLS-1$
			// create sample format
			sampleFormat = (java.text.DecimalFormat) java.text.NumberFormat.getNumberInstance();
			// create buttons
			closeButton = new JButton(DisplayRes.getString("Dialog.Button.Close.Text")); //$NON-NLS-1$
			closeButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
				}
			});
			applyButton = new JButton(DisplayRes.getString("Dialog.Button.Apply.Text")); //$NON-NLS-1$
			applyButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					patternField.getAction().actionPerformed(e);
				}
			});
			cancelButton = new JButton(DisplayRes.getString("GUIUtils.Cancel")); //$NON-NLS-1$
			cancelButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					for (String displayedName : displayedNames) {
						String name = realNames.get(displayedName);
						setFormatPattern(name, prevPatterns.get(name));
					}
					setVisible(false);
					refreshTableNow(MODE_CANCEL);
				}
			});
			helpButton = new JButton(DisplayRes.getString("GUIUtils.Help")); //$NON-NLS-1$
			helpButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String tab = "      "; //$NON-NLS-1$
					String nl = System.getProperty("line.separator", "/n"); //$NON-NLS-1$ //$NON-NLS-2$
					JOptionPane.showMessageDialog(formatDialog,
							DisplayRes.getString("DataTable.NumberFormat.Help.Message1") + nl + nl + //$NON-NLS-1$
					tab + DisplayRes.getString("DataTable.NumberFormat.Help.Message2") + nl + //$NON-NLS-1$
					tab + DisplayRes.getString("DataTable.NumberFormat.Help.Message3") + nl + //$NON-NLS-1$
					tab + DisplayRes.getString("DataTable.NumberFormat.Help.Message4") + nl + //$NON-NLS-1$
					tab + DisplayRes.getString("DataTable.NumberFormat.Help.Message5") + nl + nl + //$NON-NLS-1$
					DisplayRes.getString("DataTable.NumberFormat.Help.Message6") + " PI.", //$NON-NLS-1$ //$NON-NLS-2$
							DisplayRes.getString("DataTable.NumberFormat.Help.Title"), //$NON-NLS-1$
							JOptionPane.INFORMATION_MESSAGE);
				}

			});
			// create labels and text fields
			patternLabel = new JLabel(DisplayRes.getString("DataTable.NumberFormat.Dialog.Label.Format")); //$NON-NLS-1$
			sampleLabel = new JLabel(DisplayRes.getString("DataTable.NumberFormat.Dialog.Label.Sample")); //$NON-NLS-1$
			patternField = new JTextField(6);
			patternField.setAction(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String pattern = patternField.getText();
					if (pattern.indexOf(NO_PATTERN) > -1)
						pattern = ""; //$NON-NLS-1$
					// substitute 0 for other digits
					for (int i = 1; i < 10; i++) {
						pattern = pattern.replaceAll(String.valueOf(i), "0"); //$NON-NLS-1$
					}
					int i = pattern.indexOf("0e0"); //$NON-NLS-1$
					if (i > -1) {
						pattern = pattern.substring(0, i) + "0E0" + pattern.substring(i + 3); //$NON-NLS-1$
					}
					try {
						showNumberFormatAndSample(pattern);
						// apply pattern to all selected columns
						java.util.List<String> selectedColumns = columnList.getSelectedValuesList();
						for (Object displayedName : selectedColumns) {
							String name = realNames.get(displayedName.toString());
							setFormatPattern(name, pattern);
						}
						refreshTableNow(MODE_PATTERN);
					} catch (RuntimeException ex) {
						patternField.setBackground(new Color(255, 153, 153));
						patternField.setText(pattern);
						return;
					}
				}

			});
			patternField.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						patternField.setBackground(Color.white);
					} else {
						patternField.setBackground(Color.yellow);
						// refresh sample format after text changes
						Runnable runner = new Runnable() {
							@Override
							public void run() {
								String pattern = patternField.getText();
								if (pattern.indexOf(NO_PATTERN) > -1)
									pattern = ""; //$NON-NLS-1$
								// substitute 0 for other digits
								for (int i = 1; i < 10; i++) {
									pattern = pattern.replaceAll(String.valueOf(i), "0"); //$NON-NLS-1$
								}
								int i = pattern.indexOf("0e0"); //$NON-NLS-1$
								if (i > -1) {
									pattern = pattern.substring(0, i) + "0E0" + pattern.substring(i + 3); //$NON-NLS-1$
								}
								if (pattern.equals("") || pattern.equals(NO_PATTERN)) { //$NON-NLS-1$
									TableCellRenderer renderer = DataTable.this.getDefaultRenderer(Double.class);
									Component c = renderer.getTableCellRendererComponent(DataTable.this, Math.PI, false,
											false, 0, 0);
									if (c instanceof JLabel) {
										String text = ((JLabel) c).getText();
										sampleField.setText(text);
									}
								} else {
									try {
										sampleFormat.setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
										sampleFormat.applyPattern(pattern);
										sampleField.setText(sampleFormat.format(Math.PI));
									} catch (Exception e) {
									}
								}
							}
						};
						SwingUtilities.invokeLater(runner);
					}
				}

			});
			patternField.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					patternField.setBackground(Color.white);
					patternField.getAction().actionPerformed(null);
				}

			});
			sampleField = new JTextField(6);
			sampleField.setEditable(false);
			// column scroller (list is instantiated in setColumns() method)
			columnScroller = new JScrollPane();
			columnScroller.setPreferredSize(new Dimension(160, 120));
			// assemble dialog
			JPanel formatPanel = new JPanel(new GridLayout());
			JPanel patternPanel = new JPanel();
			patternPanel.add(patternLabel);
			patternPanel.add(patternField);
			formatPanel.add(patternPanel);
			JPanel samplePanel = new JPanel();
			samplePanel.add(sampleLabel);
			samplePanel.add(sampleField);
			formatPanel.add(samplePanel);
			add(formatPanel, BorderLayout.NORTH);
			JPanel columnPanel = new JPanel(new BorderLayout());
			columnPanel.setBorder(
					BorderFactory.createTitledBorder(DisplayRes.getString("DataTable.FormatDialog.ApplyTo.Title"))); //$NON-NLS-1$
			columnPanel.add(columnScroller, BorderLayout.CENTER);
			add(columnPanel, BorderLayout.CENTER);
			JPanel buttonPanel = new JPanel();
			buttonPanel.add(helpButton);
			buttonPanel.add(applyButton);
			buttonPanel.add(closeButton);
			buttonPanel.add(cancelButton);
			add(buttonPanel, BorderLayout.SOUTH);
			pack();
		}

		private void showNumberFormatAndSample(int[] selectedIndices) {
			if (selectedIndices == null || selectedIndices.length == 0) {
				showNumberFormatAndSample(""); //$NON-NLS-1$
			} else if (selectedIndices.length == 1) {
				String name = realNames.get(displayedNames[selectedIndices[0]]);
				String pattern = getFormatPattern(name);
				showNumberFormatAndSample(pattern);
			} else {
				// do all selected indices have same pattern?
				String name = realNames.get(displayedNames[selectedIndices[0]]);
				String pattern = getFormatPattern(name);
				for (int i = 1; i < selectedIndices.length; i++) {
					name = realNames.get(displayedNames[selectedIndices[i]]);
					if (!pattern.equals(getFormatPattern(name))) {
						pattern = null;
						break;
					}
				}
				showNumberFormatAndSample(pattern);
			}

		}

		private void showNumberFormatAndSample(String pattern) {
			if (pattern == null) {
				sampleField.setText(""); //$NON-NLS-1$
				patternField.setText(""); //$NON-NLS-1$
				return;
			}
			if (pattern.equals("") || pattern.equals(NO_PATTERN)) { //$NON-NLS-1$
				TableCellRenderer renderer = DataTable.this.getDefaultRenderer(Double.class);
				Component c = renderer.getTableCellRendererComponent(DataTable.this, Math.PI, false, false, 0, 0);
				if (c instanceof JLabel) {
					String text = ((JLabel) c).getText();
					sampleField.setText(text);
				}
				patternField.setText(NO_PATTERN);
			} else {
				sampleFormat.setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
				sampleFormat.applyPattern(pattern);
				sampleField.setText(sampleFormat.format(Math.PI));
				patternField.setText(pattern);
			}
		}

		private void setColumns(String[] names, String[] selected) {
			displayedNames = new String[names.length];
			realNames.clear();
			for (int i = 0; i < names.length; i++) {
				String s = TeXParser.removeSubscripting(names[i]);
				// add white space for better look
				displayedNames[i] = "   " + s + " "; //$NON-NLS-1$ //$NON-NLS-2$
				realNames.put(displayedNames[i], names[i]);
				if (selected != null) {
					for (int j = 0; j < selected.length; j++) {
						if (selected[j] != null && selected[j].equals(names[i])) {
							selected[j] = displayedNames[i];
						}
					}
				}
			}
			prevPatterns.clear();
			for (String name : names) {
				prevPatterns.put(name, getFormatPattern(name));
			}
			// create column list and add to scroller
			columnList = new JList<String>(displayedNames);
			columnList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
			columnList.setVisibleRowCount(-1);
			columnList.addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					showNumberFormatAndSample(columnList.getSelectedIndices());
				}
			});
			columnScroller.setViewportView(columnList);
			pack();
			int[] indices = null;
			if (selected != null) {
				// select requested names
				indices = new int[selected.length];
				for (int j = 0; j < indices.length; j++) {
					inner: for (int i = 0; i < displayedNames.length; i++) {
						if (displayedNames[i].equals(selected[j])) {
							indices[j] = i;
							break inner;
						}
					}
				}
				columnList.setSelectedIndices(indices);
			} else
				showNumberFormatAndSample(indices);
		}

	}

	/**
	 * A header cell renderer that identifies sorted columns. Added by D Brown
	 * 2010-10-24
	 */
	public class HeaderRenderer implements TableCellRenderer {
		//DrawingPanel panel = new DrawingPanel();
		TableCellRenderer renderer;
//		protected JLabel textLine = new JLabel();
		String text = "";
		//DrawableTextLine textLine = new DrawableTextLine("", 0, -6); //$NON-NLS-1$

		/**
		 * Constructor HeaderRenderer
		 * 
		 * @param renderer
		 */
		public HeaderRenderer(TableCellRenderer renderer) {
			this.renderer = renderer;
			//textLine.setJustification(TextLine.CENTER);
			//panel.addDrawable(textLine);
		}

		public TableCellRenderer getBaseRenderer() {
			return renderer;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int col) {
			// value is column name
			String name = (value == null) ? "" : value.toString(); //$NON-NLS-1$
//			textLine.setText(name);
//			if (OSPRuntime.isMac()) {
//				name = TeXParser.removeSubscripting(name);
//			}
			Component c = renderer.getTableCellRendererComponent(table, name, isSelected, hasFocus, row, col);
			if (!(c instanceof JComponent)) {
				return c;
			}
			JComponent comp = (JComponent) c;
			int sortCol = dataTableModel.getSortedColumn();
			Font font = comp.getFont();
//			if (OSPRuntime.isMac()) {
				// textline doesn't work on OSX
				comp.setFont((sortCol != convertColumnIndexToModel(col)) ? font.deriveFont(Font.PLAIN)
						: font.deriveFont(Font.BOLD));
				if (comp instanceof JLabel) {
					JLabel label = (JLabel) comp;
					label.setHorizontalAlignment(SwingConstants.CENTER);
					if (label.getText().indexOf("{") >= 0) {
						String s = TeXParser.toHTML(label.getText());
						label.setText(s);
					}
				}
				return comp;
			}
//			java.awt.Dimension dim = comp.getPreferredSize();
//			dim.height += 1;
//			panel.setPreferredSize(dim);
//			javax.swing.border.Border border = comp.getBorder();
//			if (border instanceof javax.swing.border.EmptyBorder) {
//				border = BorderFactory.createLineBorder(Color.LIGHT_GRAY);
//			}
//			panel.setBorder(border);
//			// set font: bold if sorted column
//			textLine.setFont((sortCol != convertColumnIndexToModel(col)) ? font : font.deriveFont(Font.BOLD));
//			textLine.setFore(comp.getForeground());
//			textLine.setBackground(comp.getBackground());
//			panel.setBackground(comp.getBackground());
//			return panel;
//		}

	}

	public void sort(int col) {
		dataTableModel.sort(col);
	}

	/**
	 * from DataToolTable Sets the model column order -- for DataToolTab Loader only
	 *
	 * @param modelColumns array of model column numbers in view column order
	 */
	public void setModelColumnOrder(int[] modelColumns) {
		// for each view column i
		((DataTableColumnModel) getColumnModel()).setModelColumnOrder(modelColumns);
	}

	/**
	 * Gets the model column order. -- for DataToolTab Loader
	 *
	 * @return array of model column numbers in view column order
	 */
	public int[] getModelColumnOrder() {
		return ((DataTableColumnModel) getColumnModel()).getModelColumnOrder();
	}

//	public void setModelColumnOrder(int[] modelColumns) {
//		if (modelColumns == null) {
//			return;
//		}
//		// for each model column i
//		for (int i = 0; i < modelColumns.length; i++) {
//			// find its current view column and move it if needed
//			for (int j = i; j < modelColumns.length; j++) {
//				if (convertColumnIndexToModel(j) == modelColumns[i]) {
//					if (j != i) {
//						moveColumn(j, i);
//					}
//					break;
//				}
//			}
//		}
//	}
//

	/**
	 * This method, called by Jtable.tableChanged(TableModelEvent), is never called,
	 * because we have set autoCreateColumnsFromModel false.
	 * 
	 * All updating is handled exclusively by
	 * DataTableColumnModel.updateColumnModel().
	 */
	@Deprecated
	@Override
	public void createDefaultColumnsFromModel() {
	}

	private void updateFormats() {
		// code added by D Brown to update decimal separator Jan 2018
		try {
			// try block needed to catch occasional ConcurrentModificationException
			for (String key : precisionRenderersByColumnName.keySet()) {
				PrecisionRenderer renderer = precisionRenderersByColumnName.get(key);
				renderer.numberFormat.setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
			}
			defaultDoubleRenderer.getFormat().setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
		} catch (Exception e) {
		}
	}

	public int getSortedRow(int i) {
		return dataTableModel.getSortedRow(i);
	}

	public void resetSort() {
		dataTableModel.resetSort();
	}


    @Override
	public int getRowCount() {
    	// no sortManager -- we do that ourselves
        return dataTableModel.getRowCount();
    }
    
    protected void refreshTable(int mode, boolean resortAndReselect) {
		// model for this table assumed to be a SortDecorator
		// always reset the decorator before changing table structure
		int col = dataTableModel.getSortedColumn();
		dataTableModel.resetSort();
		// save selected rows and columns
		int[] rows = getSelectedRows();
		int[] cols = getSelectedColumns();
		// refresh table
		refreshTableNow(mode);
		// sort if needed
		if (col > -1)
			sort(col);
		// restore selected rows and columns
		for (int i = 0, n =  getRowCount(); i < rows.length; i++) {
			if (rows[i] < n)
				addRowSelectionInterval(rows[i], rows[i]);
		}
		for (int i = 0, n =  getColumnCount(); i < cols.length; i++) {
			if (cols[i] < n)
				addColumnSelectionInterval(cols[i], cols[i]);
		}
	}

	public void updateColumnModel(int[] modelColumns) {
		dataTableModel.setTainted();
		((DataTableColumnModel) getColumnModel()).updateColumnModel();
	}

	private static Comparator<Object[]> nCompare = new Comparator<Object[]>() {
		@Override
		public int compare(Object[] a, Object[] b) {
			return (a[0] != null && b[0] != null
					? Double.compare(((Number)a[0]).doubleValue(), ((Number)b[0]).doubleValue())
					: a[0] != null ? 1 : b[0] != null ? -1 : 0);
		}
	};
	private static Comparator<Object[]> sCompare = new Comparator<Object[]>() {
		@Override
		public int compare(Object[] a, Object[] b) {
			return (a[0] != null && b[0] != null
					? a[0].toString().compareTo(b[0].toString())
					: a[0] != null ? 1 : b[0] != null ? -1 : 0);
		}
	};

    private int boundColumn(int col) {
        if (col< 0 || col >= getColumnCount()) {
            throw new IllegalArgumentException("Column index out of range");
        }
        return col;
    }

	public void setSelectedColumnsFromBitSet() {
		// we can use i and j to pick up a block of columns at a time
		for (int i = selectedColumns.nextSetBit(0), j = 0; i >= 0; i = selectedColumns.nextSetBit(j + 1)) {
			j = selectedColumns.nextClearBit(i + 1);
			addColumnSelectionInterval(convertColumnIndexToView(i), convertColumnIndexToView(j));
		}
	}

	protected void setColumnSelectionFromJTable() {
		selectedColumns.clear();
		int[] selected = getSelectedColumns(); // selected view columns
		int labelCol = convertColumnIndexToView(0);
		OSPLog.debug("DataTable.setColumnSelectionFromJTable " + Arrays.toString(selected));
		
		for (int i = 0; i < selected.length; i++) {
			if (selected[i] == labelCol) {
				continue;
			}
			int modelCol = convertColumnIndexToModel(selected[i]);
			selectedColumns.set(modelCol);
		}
		if (selectedColumns.isEmpty() || selectedModelRows.isEmpty()) {
			clearSelection();
		}
	}

	/**
	 * Gets the selected model rows in ascending order.
	 *
	 * @return the selected rows
	 */
	protected int[] getSelectedModelRows() {
		int[] rows = new int[selectedModelRows.cardinality()];
		for (int pt = 0, i = selectedModelRows.nextSetBit(0); i >= 0; i = selectedModelRows.nextSetBit(i + 1)) {
			rows[pt++] = i;
		}
		return rows;
	}


	/**
	 * Converts a model row index (i.e., displayed in the "row" column) to the
	 * corresponding table row number.
	 *
	 * @param row the table row
	 * @return the model row
	 */
	protected int getViewRow(int row) {
		int col = convertColumnIndexToView(0);
		for (int i = 0, n = getRowCount(); i < n; i++) {
			if (row == (Integer) getValueAt(i, col)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Sets the selected model rows.
	 *
	 * @param rows the model rows to select
	 */
	public void setSelectedModelRows(int[] rows) {
		int n = getRowCount();
		if (n < 1) {
			return;
		}
		removeRowSelectionInterval(0, n - 1);
		TreeSet<Integer> viewRows = new TreeSet<Integer>();
		for (int i = 0; i < rows.length; i++) {
			int row = getViewRow(rows[i]);
			if (row > -1) {
				viewRows.add(row);
			}
		}
		int start = -1, end = -1;
		for (int next : viewRows) {
			if (start == -1) {
				start = next;
				end = start;
				continue;
			}
			if (next == end + 1) {
				end = next;
				continue;
			}
			addRowSelectionInterval(start, end);
			start = next;
			end = next;
		}
		if (start > -1) {
			addRowSelectionInterval(start, end);
		}
	}

	protected void setSelectedRowsFromJTable() {
		selectedModelRows.clear();
		int[] rows = getSelectedRows(); // selected view rows
		OSPLog.debug("DataTable.setRowSelectionFromJTable " + Arrays.toString(rows));
		
		
		for (int i = 0; i < rows.length; i++) {
			selectedModelRows.set(getModelRow(rows[i]));
		}
		
		OSPLog.debug("DataTable.setRowS " + selectedModelRows + Arrays.toString(rows));
		
	}

	protected boolean haveSelectedRows() {
		return !selectedModelRows.isEmpty();
	}

	public BitSet getSelectedModelRowsBS() {
		return selectedModelRows;
	}

	public void setSelectedModelRowsBS(BitSet rows) {
		selectedModelRows = rows;
	}

	/**
	 * Converts a table row index to the corresponding model row number (i.e.,
	 * displayed in the "row" column).
	 *
	 * @param row the table row
	 * @return the model row
	 */
	protected int getModelRow(int row) {
		int labelCol = convertColumnIndexToView(0);
		return (Integer) getValueAt(row, labelCol);
	}

}

/*
 * Open Source Physics software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.
 *
 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be
 * released under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston MA 02111-1307 USA or view the license online at
 * http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
