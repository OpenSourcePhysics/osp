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
import java.awt.font.TextAttribute;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
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
import javax.swing.table.TableModel;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.media.core.NumberField;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.tools.FunctionEditor;

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
public class DataTable extends JTable {

//	public void paint(Graphics g) {
	//		System.out.println("DataTable.paint clip=" + g.getClipBounds(new Rectangle()));
	//		super.paint(g);
	//	}

	public static abstract class DataModel {

		public abstract int getRowCount();

		public abstract int getColumnCount();
		
		public abstract String getColumnName(int column);
		
		public abstract double getValueAt(int row, int column);
	
	}
	
	/**
	 * A marker type for TableModels that are associated with DataTable. including
	 * ComplexDataSet, DataSet, DataSetManager, Histogram, and 
	 * TableTrackView.TextColumnTableModel.
	 * 
	 * 
	 * @author hansonr
	 *
	 */
	public static abstract class OSPTableModel extends AbstractTableModel {
		public int getStride() {
			return 1;
		}
		
		public boolean isFoundOrdered() {
			return false;
		}
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
	public static final int MODE_TRACK_NEW = 0x1E00;
	
	private static final int MODE_MASK_ROW = 0x2000;
	public static final int MODE_APPEND_ROW = 0x2100;
	public static final int MODE_INSERT_ROW = 0x2200;
	public static final int MODE_DELETE_ROW = 0x2300;
	public static final int MODE_UPDATE_ROWS = 0x2400;

	private static final int MODE_MASK_COL = 0x4000;
	public static final int MODE_COLUMN = 0x4100;
	public static final int MODE_CELLS = 0x4200;

	public static final int MODE_VALUES = 0x8000;

	private static final int MODE_MASK_STYLE = 0x800000;
	public static final int MODE_PATTERN = 0x810000;
	public static final int MODE_FUNCTION = 0x820000;
	public static final int MODE_FORMAT = 0x830000;
	public static final int MODE_HIGHLIGHT = 0x840000;

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

	protected static final DoubleRenderer defaultDoubleRenderer = new DoubleRenderer();

	
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
	
	protected int mode;

	public boolean tainted;
	public boolean includeHeadersInCopiedData = true;

	public final static char SHIFTED = '`'; //$NON-NLS-1$

	/**
	 * Trim "'" from name
	 * 
	 * @param name
	 * @return
	 */
	public static String unshiftName(String name) {
		int pt = name.length() - 1;
		return (pt >= 0 && name.charAt(pt) == SHIFTED ? name.substring(0, pt) : name);
	}

	
	/**
	 * Constructs a DataTable with a default data model
	 */
	public DataTable() {
		super();
		init();
	}

	/**
	 * Called by JTable's default constructor to install
	 * the default table model, which we coerce to be an 
	 * OSPDataTableModel.
	 * 
	 */
    @Override
	public void setModel(TableModel dataModel) {
    	// totally ignore JTable's default
    	super.setModel(dataTableModel = createTableModel());
    }

    /**
     * Overridden in DataToolTable in order to use its own
     * implementation of OSPDataTableModel
     * @return
     */
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
					int vc = getColumnModel().getColumnIndexAtX(e.getX());
					int mc = convertColumnIndexToModel(vc);
					if (dataTableModel.getSortedColumn() != mc) {
						sort(mc);
					}
				}
			}

		});
		getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				updateRowSelection(e.getFirstIndex(), e.getValueIsAdjusting());
			}
		});

	}

	/**
	 * Overridden in DataToolTable
	 * 
	 * @param firstIndex
	 * @param isAdjusting
	 */
	protected void updateRowSelection(int firstIndex, boolean isAdjusting) {
		dataTableModel.setSelectedRowsFromJTable();
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
			PrecisionRenderer renderer = precisionRenderersByColumnName.get(columnName);
			if (renderer == null || !pattern.equals(renderer.pattern)) {
				precisionRenderersByColumnName.put(columnName, new PrecisionRenderer(pattern));
			}
			else 
				return;
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
			UnitRenderer unitRenderer = unitRenderersByColumnName.get(columnName);
			if (unitRenderer == null) {
				unitRenderer = new UnitRenderer(renderer, units, tooltip);
				unitRenderersByColumnName.put(columnName, unitRenderer);
			}
			else {
				unitRenderer.units = units;
				unitRenderer.tooltip = tooltip;
				unitRenderer.baseRenderer = renderer;
			}
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
		Component c = renderer.getTableCellRendererComponent(DataTable.this, value, false, false, 0, col);
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
	 * Sets the visibility of a column of a TableModel in the DataTable.
	 *
	 * @param model an OSPTableModel, actually
	 * @param columnIndex
	 * @param b
	 */
	public void setColumnVisible(TableModel model, int columnIndex, boolean b) {
		dataTableModel.setColumnVisible(model, columnIndex, b);
	}

	/**
	 * Reset bsColVis for all elements.
	 * 
	 */
	public void refreshColumnModel() {
		dataTableModel.refreshColumnModel();
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
		UnitRenderer unitRenderer = null;
		TableCellRenderer baseRenderer = null;
		try {
			TableColumn tableColumn = getColumnModel().getColumn(column);
//			int i = convertColumnIndexToModel(column);
			if (tableColumn.getModelIndex() == 0 && dataTableModel.isRowNumberVisible()) {
				return rowNumberRenderer;
			}
			// find units renderer
			// BH 2020.02.14 efficiencies -- needs doublechecking
			String key = (String) tableColumn.getHeaderValue();
			
			// remove subscripts with leading space (eg time-based RGB data from LineProfile)
			int k = key.indexOf("_{ "); // note space
			if (k > 0) {
				key = key.substring(0, k);
			}
			
			unitRenderer = unitRenderersByColumnName.get(key);
			// find base renderer
			baseRenderer = tableColumn.getCellRenderer();
			if (baseRenderer == null) {
				baseRenderer = precisionRenderersByColumnName.get(key);
				if (baseRenderer == null) {
					baseRenderer = precisionRenderersByColumnName.get(unshiftName(key));
				}
			}
		} catch (Exception ex) {
		}
		// if no precision base renderer, use default
		if (baseRenderer == null) {
			Class<?> c = getColumnClass(column);
			baseRenderer = (c == Double.class ? defaultDoubleRenderer : getDefaultRenderer(c));
		}
		// return unit renderer if defined
		if (unitRenderer == null)
			return baseRenderer;
		unitRenderer.setBaseRenderer(baseRenderer);
		return unitRenderer;
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
		//OSPLog.debug(Performance.timeCheckStr("DataTable.refreshTable0 " 
			//	+ Integer.toHexString(mode), Performance.TIME_MARK));
		// BH every sort of refresh goes through here
		boolean columnsChanged;
		boolean rowsChanged = false;
		
		int mask = this.mode = mode;
		switch (mode) {
		case MODE_CANCEL: // 0x00;
			return;
		case MODE_SET_TAINTED: // 0x10000000
			dataTableModel.setTainted();
			return;
			//
			// column (structure) changes
			//
		default:
		case MODE_UPDATE_ROWS: // 0x2400;
			dataTableModel.resetSort();
			// fall through
			//$FALL-THROUGH$
		case MODE_CREATE: // 0x01;
		case MODE_CLEAR: // 0x02;
		case MODE_MODEL: // 0x03;
		case MODE_TAB: // 0x04;
			mask = MODE_MASK_NEW;
			columnsChanged = true;
		break;
		case MODE_TRACK_REFRESH: // 0x1100;
		case MODE_TRACK_STATE: // 0x1200;
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
		case MODE_TRACK_STEP: // 0x1300;
		case MODE_TRACK_STEPS: // 0x1500;
		// DB: MODE_TRACK_DATA must trigger row update for step size changes 
		case MODE_TRACK_DATA: // 0x1C00;
			rowsChanged = true;
			//$FALL-THROUGH$
		case MODE_COL_SETVISIBLE: // 0x1700;
		case MODE_TRACK_SELECTEDPOINT: // 0x1400;
		case MODE_TRACK_TRANSFORM: // 0x1B00;
		case MODE_TRACK_FUNCTION: // 0x1D00;
		case MODE_TRACK_CHOOSE: // 0x1900;
		case MODE_TRACK_SELECT: // 0x1A00;
		case MODE_TRACK_NEW: // 0x1A00;
			mask = MODE_MASK_TRACK; // 0x1000;
			columnsChanged = false;
			break;
			
		case MODE_INSERT_ROW: // 0x2100;
		case MODE_DELETE_ROW: // 0x2200;
		case MODE_APPEND_ROW: // 0x2300;
			mask = MODE_MASK_ROW;
			rowsChanged = true;
			columnsChanged = false;
			break;
		case MODE_PATTERN: // 0x810000;
		case MODE_FUNCTION: // 0x820000;
		case MODE_FORMAT: // 0x830000;
		case MODE_HIGHLIGHT: // 0x840000;
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
		if (tainted) {
			columnsChanged = rowsChanged = true;
			tainted = false;
		}
		//OSPLog.debug(">>>>DataTable.refreshTableNow:  mode" + Integer.toHexString(mode) + " cols changed?" + columnsChanged +" rows changed?"+rowsChanged);

		dataTableModel.refresh(mask);
		if (columnsChanged) {
			dataTableModel.fireTableStructureChanged();
		} else if (rowsChanged) {
			// BH 2020.10.04 this isn't necessarily an inserted row. It's just
			// a way to trigger the revalidate
//			dataTableModel.fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
			dataTableModel.fireTableRowsInserted(getRowCount()==0? -1: 0, getRowCount() - 1);
		} else {
			if (mode == MODE_TRACK_NEW)
				revalidate();
			repaint();
		}
		//OSPLog.debug(Performance.timeCheckStr("DataTable.refreshTable1 " + Integer.toHexString(mode),
		//Performance.TIME_MARK));
	}

	/**
	 * Add a TableModel object to the table model list.
	 *
	 * @param tableModel
	 */
	public void add(TableModel tableModel) {
		dataTableModel.add((OSPTableModel) tableModel);
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
	 * A wrapper for a TableModel, allowing skipping of rows
	 * ("stride") 
	 *
	 */
	private static class DataTableElement implements TableModelListener {

		protected final OSPTableModel tableModel;

		/**
		 * the column "return" value from find
		 */
		protected int foundColumn = -1;

		/**
		 * indicating if a column is visible
		 */
		protected final BitSet bsColVis = new BitSet();

		/**
		 * Constructor DataTableElement
		 *
		 * @param t
		 */
		protected DataTableElement(OSPTableModel t) {
			tableModel = t;
			bsColVis.set(0, t.getColumnCount());
			if (t instanceof DatasetManager.Model) {
				t.addTableModelListener(this);
			}
		}

		/**
		 * Method setColumnVisible
		 *
		 * @param columnIndex
		 * @param visible
		 */
		protected void setColumnVisible(int columnIndex, boolean visible) {
			bsColVis.set(columnIndex, visible);
		}

		/**
		 * Method getStride
		 *
		 * @return
		 */
		protected int getStride() {
			return 1;//tableModel.getStride();
		}

		/**
		 * Method getColumnCount
		 *
		 * @return
		 */
		protected int getVisibleColumnCount() {
			return bsColVis.cardinality();
		}
		
		@Override
		public String toString() {
			return "DataTableElement " + tableModel.getRowCount() + "x" + tableModel.getColumnCount() + " vis=" + bsColVis;
		}

		@Override
		public void tableChanged(TableModelEvent e) {
			refresh();
		}

		public void refresh() {
			bsColVis.clear();
			int n = tableModel.getColumnCount();
			bsColVis.set(0, n);
		}

	
	}

	/*
	 * OSPDataTableModel acts on behalf of the OSPTableModels that the DataTable
	 * contains. It combines data from these multiple sources and allows the
	 * DataTable to display data is if the data were from a single source.
	 *
	 * @author jgould
	 * 
	 * @created February 21, 2002
	 */
	protected class OSPDataTableModel extends AbstractTableModel implements TableModelListener {

		final private ArrayList<DataTableElement> dataTableElements;
		boolean rowNumberVisible;
		private SortDecorator decorator;
		protected TableModelEvent lastModelEvent;

		protected int columnCount;
		private int rowCount;
		protected boolean haveColumnClasses = true;

		private BitSet selectedModelRows = new BitSet();
		private BitSet selectedModelCols = new BitSet();
		private TableModel foundModel;

		protected OSPDataTableModel() {
			dataTableElements = new ArrayList<DataTableElement>();
			decorator = new SortDecorator();
			addTableModelListener(new TableModelListener() {
				@Override
				public void tableChanged(TableModelEvent e) {
					setTainted();
					decorator.reset();
				}

			});
		}

		/**
		 * The TableModelListener will clue us in
		 */
		protected void setTainted() {
			columnCount = rowCount = -1;
			tainted = true;
		}

		public void setColumnSelectionFromJTable() {
			selectedModelCols.clear();
			int[] selected = getSelectedColumns(); // selected view columns
			int labelCol = convertColumnIndexToView(0);
//			OSPLog.debug("DataTable.setColumnSelectionFromJTable " + Arrays.toString(selected));
//			
			for (int i = 0; i < selected.length; i++) {
				if (selected[i] == labelCol) {
					continue;
				}
				selectedModelCols.set(convertColumnIndexToModel(selected[i]));
			}
			if (selectedModelCols.isEmpty() || selectedModelRows.isEmpty()) {
				clearSelection();
			}
		}

		public void setSelectedRowsFromJTable() {
			selectedModelRows.clear();
			BitSet bs = getSelectedTableRowsBS();
			for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
				selectedModelRows.set(getModelRow(i));
			}
		}
		

		/**
		 * Converts a table row index to the corresponding model row number (i.e.,
		 * displayed in the "row" column).
		 *
		 * @param row the table row
		 * @return the model row
		 */
		protected int getModelRow(int row) {
			return decorator.getModelRow(row);
		}


		protected void addColumnSelectionInterval(int coli, int colj) {
	        columnModel.getSelectionModel().addSelectionInterval(coli, colj);
			int labelCol = convertColumnIndexToView(0);
			selectedModelCols.clear();
			int[] selected = getSelectedColumns(); // selected view columns
			for (int i = 0; i < selected.length; i++) {
				if (selected[i] == labelCol) {
					continue;
				}
				int modelCol = convertColumnIndexToModel(selected[i]);
				selectedModelCols.set(modelCol);
			}
			if (selectedModelCols.isEmpty()) {
				clearSelection();
			}
	    }

		/**
		 * Find the dataTableElement associated with this column.
		 *
		 * @param rowNumberVisible
		 * @param dataTableElements
		 * @param icol
		 * @return
		 */
		protected DataTableElement find(int icol) {
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
//						if (!bs.get(j)) {
						if (!bs.get(j-ncol)) {
							i++;
						}
					}
					dte.foundColumn = i;
					foundModel = dte.tableModel;
					return dte;
				}
				ncol += nvis;
			}
			foundModel = null;
			return null; // this shouldn't happen
		}

		/**
		 * Sorts the table rows using the given column.
		 * 
		 * @param col int
		 */
		protected void sort(int col) {
			DataTableElement dte;
			if (col < 0 || dataTableElements.size() == 0
					|| (dte = find(col)) == null)
				return;
			// secure the data set column
			dte.tableModel.getValueAt(0, dte.foundColumn);
			decorator.sort(dte, col);
		}

		/**
		 * Gets the sorted column. Added by D Brown 2010-10-24.
		 * 
		 * @return the
		 */
		public int getSortedColumn() {
			return decorator.getSortedColumn();
		}

		protected void resetSort() {
			decorator.reset();
		}

		protected int getSortedRow(int j) {
			return decorator.getSortedRow(j);
		}

		/**
		 * Method setColumnVisible
		 *
		 * @param model
		 * @param columnIndex 
		 * @param b
		 */
		protected void setColumnVisible(TableModel model, int columnIndex, boolean b) {
			DataTableElement dte = findElementContaining(model);
			dte.setColumnVisible(columnIndex, b);
		}

		protected void refreshColumnModel() {
			for (int i = dataTableElements.size(); --i >= 0;) {
				dataTableElements.get(i).refresh();
			}
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
			if (rowIndex >= dte.tableModel.getRowCount()) {
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
			return dte.tableModel.getColumnName(dte.foundColumn);
		}

		/**
		 * Method getRowCount
		 *
		 * @return
		 */
		@Override
		public synchronized int getRowCount() {
			if (rowCount > 0)
				return rowCount;
			int n = 0;
			for (int i = dataTableElements.size(); --i >= 0;) {
				DataTableElement dte = dataTableElements.get(i);
				int stride = dte.getStride();
				n = Math.max(n, (dte.tableModel.getRowCount() + stride - 1) / stride);
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

		protected Object getElementValue(int rowIndex, int columnIndex) {
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
			if (rowIndex >= dte.tableModel.getRowCount()) {
				return null;
			}
			return dte.tableModel.getValueAt(rowIndex, dte.foundColumn);
		}
		

		/**
		 * Get an array of column values ready for sorting.
		 * 
		 * @param columnIndex
		 * @param objects
		 * @return
		 */
		protected Object[] getElementValues(DataTableElement dte, int columnIndex, Object[] objects) {
			boolean asRow = (rowNumberVisible && columnIndex == 0);
			int stride = dte.getStride();
			for (int i = 0, rowIndex = 0, n = objects.length, nr = dte.tableModel.getRowCount(); i < n
					&& rowIndex < nr; i++, rowIndex += stride) {
				// BH note: was getValueAt(rowIndex), but datasets will multiply this by their
				// stride
				objects[i] = (asRow ? Integer.valueOf(rowIndex) : dte.tableModel.getValueAt(i, dte.foundColumn));
			}
			return objects;
		}

//
//		/**
//		 * Method setStride
//		 *
//		 * @param tableModel
//		 * @param stride
//		 */
////		@Override
//		public void setStride(TableModel tableModel, int stride) {
//			findElementContaining(tableModel).setStride(stride);
//		}


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

			if (!haveColumnClasses)
			{
				return super.getColumnClass(columnIndex);
			}
			if (rowNumberVisible) {
				if (columnIndex == 0) {
					return Integer.class;
				}
			}			
			
			DataTableElement dte = find(columnIndex);
			return dte.tableModel.getColumnClass(dte.foundColumn);
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

		protected boolean isElementEditable(int row, int col) {
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
		 * Method to add an instance of OSPTableModel, 
		 * including ComplexDataSet, DataSet, DataSetManager, 
		 * Histogram, or TextColumnTableModel.
		 * 
		 *
		 * @param tableModel
		 */
		protected void add(OSPTableModel tableModel) {
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
		protected DataTableElement findElementContaining(TableModel tableModel) {
			for (int i = dataTableElements.size(); --i >= 0;) {
				DataTableElement dte = dataTableElements.get(i);
				if (dte.tableModel == tableModel) {
					return dte;
				}
			}
			return null;
		}

		private class SortDecorator {
			private int[] viewRowToModel = new int[0];
			private int[] modelToViewRow = new int[0];
			private int sortedColumn; // added by D Brown 2010-10-24

			/**
			 * Constructor SortDecorator
			 * 
			 * @param model
			 */
			private SortDecorator() {
			}

			protected int getModelRow(int row) {
				return viewRowToModel[row];
			}

			/**
			 * Gets the sorted row number for a given real model row Added by D Brown Dec
			 * 2017
			 * 
			 * @param realModelRow the unsorted row number
			 * @return the sorted row number
			 */
			protected int getSortedRow(int realModelRow) {
				if (realModelRow >= rowCount)
					setTainted();
				allocate(realModelRow);
				return modelToViewRow[realModelRow];
			}

			protected Object getValueAt(int viewRow, int viewCol) {
				if (viewCol >= getColumnCount()) {
					return null;
				}
				allocate(viewRow);
				return getElementValue(viewRowToModel[viewRow], viewCol);
			}

			protected void setValueAt(Object aValue, int viewRow, int viewCol) {
				allocate(viewRow);
				setElementValue(aValue, viewRowToModel[viewRow], viewCol);
			}

			protected void sort(DataTableElement dte, int column) {
				if (dte.tableModel.isFoundOrdered()) {
					allocate(Integer.MAX_VALUE);
					sortedColumn = column;
					return;
				}
				sortedColumn = column;
				allocate(Integer.MAX_VALUE);
				
				// new faster sort method added by D Brown 2015-05-16
				try {
					Object[] data = getElementValues(dte, column, new Object[rowCount]);
					Object[][] sortArray = new Object[rowCount][2];
					for (int i = 0; i < rowCount; i++) {
						sortArray[i][0] = data[i];
						sortArray[i][1] = Integer.valueOf(viewRowToModel[i]);
					}
					// Comparators can be static; either Number or String, depending upon the column type
					Class<?> c = getColumnClass(column);
					if (Number.class.isAssignableFrom(c)) {
						Arrays.sort(sortArray, nCompare);
					} else { 
						Arrays.sort(sortArray, sCompare);					
					}
					for (int i = 0; i < rowCount; i++) {
						viewRowToModel[i] = ((Integer)sortArray[i][1]).intValue();
					}
					return;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// added by D Brown 2010-10-24
			protected int getSortedColumn() {
				return sortedColumn;
			}

//			private void swap(int i, int j) {
//				int tmp = indexes[i];
//				indexes[i] = indexes[j];
//				indexes[j] = tmp;
//			}
//
//			private int compare(int i, int j, int column) {
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
			private void allocate(int min) {
				if (viewRowToModel.length <= min) {
					int n = getRowCount();
					viewRowToModel = new int[n];
					modelToViewRow = new int[n];
					for (int i = 0; i < viewRowToModel.length; ++i) {
						viewRowToModel[i] = modelToViewRow[i] = i;
					}
				}
			}

			protected void reset() {
				allocate(Integer.MAX_VALUE);
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

		public boolean isFoundOrdered() {
			return (foundModel != null && foundModel 
					instanceof Dataset && 
					((Dataset) foundModel).model.isFoundOrdered());
		}

		protected void refresh(int mask) {
			@SuppressWarnings("unused")
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
			updateColumnModel();
//			OSPLog.debug("OSPDataTableModel rebuild " + type);
		}

		protected int[] getSelectedModelRows() {
			int[] rows = new int[selectedModelRows.cardinality()];
			for (int pt = 0, i = selectedModelRows.nextSetBit(0); i >= 0; i = selectedModelRows.nextSetBit(i + 1)) {
				rows[pt++] = i;
			}
			return rows;
		}

	}
	
	public class DataTableColumnModel extends DefaultTableColumnModel {

		private DataTableColumnModel() {
			super();
		}
		
//		public void removeColumn(TableColumn col) {
//	    	super.removeColumn(col);
//	    	updateColumnModel();
//	    }
//
		public class DataTableColumn extends TableColumn {

			private boolean isSizeSet;

			private DataTableColumn(int modelIndex) {
				super(modelIndex);
				setHeaderValue(getModel().getColumnName(modelIndex));
			}
			
			@Override
			public String toString() {
				return "[DT column " + headerValue + " " + modelIndex + "]";
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

			int nTableCols = tableColumns.size();
			int nModelCols = dataTableModel.getColumnCount();

			// check that headers show correct columns names
			// DB! added May 2022 for LineProfile multiple frame data
			for (int i = nTableCols; --i >= 0;) {
				DataTableColumn tc = getTableColumn(i);
				String colName = getModel().getColumnName(tc.getModelIndex());
				if (!colName.equals(tc.getHeaderValue())) {
					tc.setHeaderValue(colName);
				}
			}

			// we do not have to continue unless there have been additions or deletions,
			// as moving columns around does not affect their model references, and after
			// loading, this method might be run unnecessarily as part of a refresh().

			if (nModelCols == nTableCols)
				return;

			// Create a map of the current column set (actually displayed),
			// updating all model indexes by name and removing ones that are gone.
			// We remove any deleted columns here as well.
			// We create a name list for reference only in the case of addition,
			// so that we can match the missing name with a dataset.

			StringBuffer names = (nModelCols > nTableCols ? new StringBuffer(",") : null);
			for (int i = nTableCols; --i >= 0;) {
				DataTableColumn tc = getTableColumn(i);
				String name = (String) tc.getHeaderValue();
				if (names != null)
					names.append(name).append(",");
				int modelIndex = dataTableModel.findColumn(name);
				if (modelIndex >= 0) {
					tc.setModelIndex(modelIndex);
				} else {
					tc.removePropertyChangeListener(this);
					tableColumns.remove(i);
					nTableCols--;
				}
			}

			// For a new column, find or assign a modelIndex and add it to tableColumns

			for (int i = nTableCols; i < nModelCols; i++) {
				// Find or assign the model number. If it is not found, then just assign
				// it sequentially. The findLast method works only when only one column
				// has been added.
				int modelIndex = findLastAddedModelIndex(names);
				DataTableColumn tc = new DataTableColumn(modelIndex < 0 ? i : modelIndex);
				tc.addPropertyChangeListener(this);
				tableColumns.add(tc);
			}
			
			// finally, clear all selections and invalidate widths

			totalColumnWidth = -1;
			selectionModel.clearSelection();
			invalidateWidths();
		}

		/**
		 * 
		 * @param viewIndex
		 * @return
		 */
		public int convertColumnIndexToModel(int viewIndex) {
			if (dataTableModel.getColumnCount() != tableColumns.size())
				updateColumnModel();
			return getTableColumn(viewIndex).getModelIndex();
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
		 * Return a DataTableColumn, finalizing its size calculation and creating a new
		 * tableColumn, if necessary due to an out-of-bounds calculation.
		 *
		 * @param columnIndex
		 * @return
		 */
		@Override
		public TableColumn getColumn(int columnIndex) {
			return (columnIndex >= 0 && columnIndex < tableColumns.size() ? updateTableColumnSize(columnIndex)
					: new TableColumn(0));
		}

		private TableColumn updateTableColumnSize(int index) {
			DataTableColumn tableColumn = getTableColumn(index);
			if (!tableColumn.isSizeSet) {
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
			}
			return tableColumn;
		}

		/**
		 * Do not add a new column and do not check for sizing.
		 * 
		 * @param columnIndex
		 * @return the indicated column
		 */
		public DataTableColumn getTableColumn(int columnIndex) {
			return (DataTableColumn) super.getColumn(columnIndex);
		}

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
			BitSet mapped = new BitSet(max);
			for (int i = 0, n = modelColumns.length; i < n; i++) {
				int mi = modelColumns[i];
				int j = (mi > max ? -1 : map[mi] - 1);
				if (j >= 0) {
					DataTableColumn tc = (DataTableColumn) tableColumns.get(j);
					tc.setModelIndex(mi);
					newCols.add(tc);
					mapped.set(j);
				}
			}
			// Now remove the property listener for any removed columns.
			// Current DataTableColumnModel subclasses and superclasses do not implement any
			// listeners, actually.
			for (int i = mapped.nextClearBit(0), n = tableColumns.size(); i < n; i = mapped.nextClearBit(i + 1)) {
				tableColumns.get(i).removePropertyChangeListener(this);

			}
			tableColumns = newCols;
		}

		/**
		 * Get modelIndex array for currently visible columns.
		 * 
		 * @return
		 */
		private int[] getModelColumnOrder() {
			int n = getModel().getColumnCount();
			int[] modelColumns = new int[n];
			for (int i = 0; i < n; i++) {
				int d = getTableColumn(i).getModelIndex();
				modelColumns[i] = (d == 0 ? i : d);
			}
			return modelColumns;
		}

		@Override
		public String toString() {
			String s = "[DT columnModel ";
			for (int i = 0; i < tableColumns.size(); i++)
				s += getTableColumn(i).toString() + " " + "]";
			return s;
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
			if (value == null || Double.isNaN((Double) value)) {
				setText(""); //$NON-NLS-1$
				return;
			}
			numberField.setValue((Double) value);
			setText(numberField.getText());
		}
//
//		/**
//		 * Gets the number format
//		 */
//		DecimalFormat getFormat() {
//			return numberField.getFormat();
//		}

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
			setText((value == null || value.toString().equals("NaN")) ? "" : numberFormat.format(value)); //$NON-NLS-1$
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

	static int test = 0;
	
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
				String s = label.getText();
				if (s != null && !s.equals("")) //$NON-NLS-1$
					label.setText(s + units);
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
					formatAction();
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
					formatAction();
				}
			});
			patternField.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					// was keyPressed
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						patternField.setBackground(Color.white);
					} else {
						patternField.setBackground(Color.yellow);
						// refresh sample format after text changes
						SwingUtilities.invokeLater(() -> {
							// in SwingJS this needs to be after keyReleased, not key pressed
							updatePatternAction();
						});
					}
				}

			});
			patternField.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					patternField.setBackground(Color.white);
					formatAction();
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

		protected void updatePatternAction() {
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

		protected void formatAction() {
			
			String pattern = patternField.getText();
			if (pattern.indexOf(NO_PATTERN) > -1)
				pattern = ""; //$NON-NLS-1$
			// substitute 0 for other digits
			for (int i = 1; i < 10; i++) {
				pattern = pattern.replaceAll("" + i, "0"); //$NON-NLS-1$
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
			}
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
			boolean isSortedCol = sortCol == convertColumnIndexToModel(col);
			comp.setFont(!isSortedCol? font.deriveFont(Font.PLAIN)
					: font.deriveFont(Font.BOLD));
			if (comp instanceof JLabel) {
				JLabel label = (JLabel) comp;
				label.setHorizontalAlignment(SwingConstants.CENTER);
				if (label.getText().indexOf("{") >= 0) {
					String s = TeXParser.toHTML(label.getText());
					label.setText(s);
				}
				if (isSortedCol) {
					font = label.getFont();
					Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
					attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
					label.setFont(font.deriveFont(attributes));					
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
			//unnecessary defaultDoubleRenderer.getFormat().setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
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
    	//System.out.println("DataTable.refreshTable " + mode);
		// model for this table assumed to be a SortDecorator
		// always reset the decorator before changing table structure
		int col = dataTableModel.getSortedColumn();
		// save selected rows and columns
		BitSet rows = getSelectedTableRowsBS();
		BitSet cols = getSelectedTableColumnsBS();
		// refresh table and sort if needed
		refreshTableNow(mode);
		if (col >= 0) {
			switch (mode) {
			// TODO more here
			case MODE_TRACK_STEP:
				break;
			default:
				dataTableModel.resetSort();
				sort(col);
				// restore selected rows and columns
				// BitSet method uses block addition
				selectTableRowsBS(rows, 0);
				selectTableColsBS(cols);
				break;
			}
		}
	}

    /**
     * A standard method using BitSet blocks; reduces the number of 
     * calls to addRowSelectionInterval.
     * 
     * @param rows
     * @param nRows
     */
	public void selectTableRowsBS(BitSet rows, int nRows) {
		if (nRows > 0)
			removeRowSelectionInterval(0, nRows - 1);
		for (int i = rows.nextSetBit(0), j = 0, n = getRowCount(); 
				i >= 0 && i < n; i = rows.nextSetBit(j + 1)) {
			j = Math.min(n, rows.nextClearBit(i + 1));
			addRowSelectionInterval(i, j - 1);
		}
	}

	/**
	 * A standard method using BitSet blocks; reduces the number of calls to
	 * addColumnSelectionInterval.
	 * 
	 * @param cols
	 */
	public void selectTableColsBS(BitSet cols) {
		for (int i = cols.nextSetBit(0), j = 0, n = getColumnCount(); 
				i >= 0 && i < n; i = cols.nextSetBit(j + 1)) {
			j = Math.min(n, cols.nextClearBit(i + 1));
			addColumnSelectionInterval(i, j - 1);
		}
	}

	/**
	 * 
	 */
	public void setSelectedColumnsFromModelBS() {
		BitSet bs = dataTableModel.selectedModelCols;
		// we can use i and j to pick up a block of columns at a time
		for (int i = bs.nextSetBit(0), j = 0; i >= 0; i = bs.nextSetBit(j + 1)) {
			j = bs.nextClearBit(i + 1);
			addColumnSelectionInterval(convertColumnIndexToView(i), convertColumnIndexToView(j));
		}
	}

	public void updateColumnModel() {
		// modelColumns is always null here
		dataTableModel.setTainted();
		((DataTableColumnModel) getColumnModel()).updateColumnModel();
	}

	private static Comparator<Object[]> nCompare = new Comparator<Object[]>() {
		@Override
		public int compare(Object[] a, Object[] b) {
			return (a[0] != null && b[0] != null
					? Double.compare(((Number)a[0]).doubleValue(), ((Number)b[0]).doubleValue())
					: a[0] != null ? -1 : b[0] != null ? 1 : 0);
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

	/**
	 * Gets the selected model rows in ascending order.
	 *
	 * @return the selected rows
	 */
	protected int[] getSelectedModelRows() {
		return dataTableModel.getSelectedModelRows();
	}


	/**
	 * Converts a model row index (i.e., displayed in the "row" column) to the
	 * corresponding table row number.
	 *
	 * @param modelRow the table row
	 * @return the model row
	 */
	protected int getViewRow(int modelRow) {
		int col = convertColumnIndexToView(0);
		for (int i = 0, n = getRowCount(); i < n; i++) {
			if (modelRow == (Integer) getValueAt(i, col)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Sets the selected model rows.
	 *
	 * @param modelRows the model rows to select
	 */
	public void selectModelRows(int[] modelRows) {
		int n = getRowCount();
		if (n == 0) {
			return;
		}
		if (modelRows.length == n) {
			addRowSelectionInterval(0, n - 1);
			return;
		}
		BitSet bs = new BitSet();
		for (int i = 0; i < modelRows.length; i++) {
			int row =  modelRows[i];
			// DB edit 4 Dec 2022
			if (row >= n)
				break;
			for (int j = 0; j <= n; j++) {
				if (row == ((Integer) dataTableModel.getValueAt(convertRowIndexToModel(j),0)).intValue()) {
					bs.set(j);
					break;
				}
			}
		}
		selectTableRowsBS(bs, n);
	}

	public void selectModelRowsBS(BitSet rows) {
		int n = getRowCount();
		if (n == 0) {
			return;
		}
		if (rows.cardinality() == n) {
			addRowSelectionInterval(0, n - 1);
			return;
		}
		BitSet bs = new BitSet();
		for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
			for (int j = 0; j <= n; j++) {
				if (i == ((Integer) dataTableModel.getValueAt(convertRowIndexToModel(j),0)).intValue()) {
					bs.set(j);
					break;
				}
			}
		}
		selectTableRowsBS(bs, n);
	}

	private BitSet getSelectedTableRowsBS() {
		return getSelectedTableBS(getSelectionModel());
	}

	private BitSet getSelectedTableColumnsBS() {
		return getSelectedTableBS(columnModel.getSelectionModel());
	}

	private static BitSet getSelectedTableBS(ListSelectionModel sm) {
		BitSet bs = new BitSet();
		int min = sm.getMinSelectionIndex();
		int max = sm.getMaxSelectionIndex();
		if (min < 0 || max < 0)
			return bs;
		for (int i = min; i <= max; i++) {
			if (sm.isSelectedIndex(i))
				bs.set(i);
		}
		return bs;
	}

	protected boolean haveSelectedRows() {
		return !dataTableModel.selectedModelRows.isEmpty();
	}

	public BitSet getSelectedModelRowsBS() {
		return dataTableModel.selectedModelRows;
	}

	public void setSelectedModelRowsBS(BitSet rows) {
		dataTableModel.selectedModelRows = rows;
	}
	
	protected int getModelRow(int i) {
		return dataTableModel.getModelRow(i);
	}

	public void scrollRowToVisible(int row) {
		scrollRectToVisible(getCellRect(row, 0, true));
	}

	public void scrollColumnToVisible(int col) {
		scrollRectToVisible(getCellRect(0, col, true));
	}

	/**
	 * Gets the data selected by the user in this datatable. This method is modified
	 * from the org.opensourcephysics.display.DataTableFrame getSelectedData method.
	 *
	 * @param asFormatted true to retain table formatting
	 * @return a StringBuffer containing the data.
	 */
	public StringBuffer getData(boolean asFormatted) {
		StringBuffer buf = new StringBuffer();
		// get selected data
		int[] selectedRows = getSelectedRows();
		int[] selectedColumns = getSelectedColumns();
		// if no data is selected, select all
		int[] restoreRows = null;
		int[] restoreColumns = null;
		if (selectedRows.length == 0) {
			selectAll();
			restoreRows = selectedRows;
			restoreColumns = selectedColumns;
			selectedRows = getSelectedRows();
			selectedColumns = getSelectedColumns();
		}
		if (includeHeadersInCopiedData) {
			// copy column headings
			for (int j = 0; j < selectedColumns.length; j++) {
				// ignore row heading
				if (isRowNumberVisible() && selectedColumns[j] == 0)
					continue;
				String name = getColumnName(selectedColumns[j]);
				name = TeXParser.removeSubscripting(name);
	//			if (name.startsWith(FunctionEditor.THETA)) {
	//				for (int i = 0; i < selectedRows.length; i++) {
	//					Object val = getFormattedValueAt(selectedRows[i], selectedColumns[j]);
	//					if (val != null && val.toString().contains(FunctionEditor.DEGREES)) {
		//						name += "(" + FunctionEditor.DEGREES + ")";
		//						break;
		//					}				
		//				}
	//			}
				buf.append(name);
				if (j < selectedColumns.length - 1)
					buf.append(VideoIO.getDelimiter()); // add delimiter after each column except the last
			}
			buf.append(XML.NEW_LINE);
		}
		java.text.DecimalFormat nf = (DecimalFormat) NumberFormat.getInstance();
		nf.applyPattern("0.000000E0"); //$NON-NLS-1$
		nf.setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
		java.text.DateFormat df = java.text.DateFormat.getInstance();
		for (int i = 0; i < selectedRows.length; i++) {
			for (int j = 0; j < selectedColumns.length; j++) {
				int temp = convertColumnIndexToModel(selectedColumns[j]);
				if (isRowNumberVisible()) {
					if (temp == 0) { // don't copy row numbers
						continue;
					}
				}
				Object value = null;
				if (asFormatted) {
					value = getFormattedValueAt(selectedRows[i], selectedColumns[j]);
				} else {
					value = getValueAt(selectedRows[i], selectedColumns[j]);
					if (value != null) {
						if (value instanceof Number) {
							value = nf.format(value);
						} else if (value instanceof java.util.Date) {
							value = df.format(value);
						}
					}
					if ("NaN".equals(value))
						value = "";
				}
				if (value != null) {
					// remove degrees sign, if any
					int n = value.toString().indexOf(FunctionEditor.DEGREES);
					if (n > 0)
						value = value.toString().substring(0, n);
					buf.append(value);
				}
				if (j < selectedColumns.length - 1)
					buf.append(VideoIO.getDelimiter()); // add delimiter after each column except the last
			}
			buf.append(XML.NEW_LINE); // new line after each row
		}
		if (restoreRows != null && restoreColumns != null) {
			// restore previous selection state
			clearSelection();
			for (int row : restoreRows)
				addRowSelectionInterval(row, row);
			for (int col : restoreColumns)
				addColumnSelectionInterval(col, col);
		}
		return buf;
	}

	/**
	 * Copies data from this table to the system clipboard.
	 *
	 * @param asFormatted true to retain table formatting
	 * @param header      the table header
	 */
	public void copyTable(boolean asFormatted, String header) {
		StringBuffer buf = getData(asFormatted);
		// replace spaces with underscores in header (must be single string)
		header = header.replace(' ', '_');
		if (!header.endsWith(XML.NEW_LINE))
			header += XML.NEW_LINE;
		if (includeHeadersInCopiedData)
			OSPRuntime.copy(header + buf, null);
		else
			OSPRuntime.copy(buf.toString(), null);
	}

	/**
	 * subclass should return the modelIndex of the first column name (comma-quoted
	 * key) not found in names and, if that exists, add that key to name in case this needs to 
	 * be run more than once. 
	 * 
	 * @param names a simple comma-quoted listing of known column names 
	 * @return the modelIndex and append to names the new comma-quoted name found
	 */
	protected int findLastAddedModelIndex(StringBuffer names) {
		return -1;
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
 * Copyright (c) 2024 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
