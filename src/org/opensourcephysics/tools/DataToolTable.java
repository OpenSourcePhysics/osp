/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.display.DataFunction;
import org.opensourcephysics.display.DataTable;
import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.display.DatasetManager;
import org.opensourcephysics.display.DrawableTextLine;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.display.HighlightableDataset;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.display.TeXParser;
import org.opensourcephysics.display.TextLine;
import org.opensourcephysics.js.JSUtil;

/**
 * This is a DataTable that displays DataColumns and constructs
 * HighlightableDatasets for a plot.
 *
 * @author Douglas Brown
 * @version 1.0
 */
@SuppressWarnings("serial")
public class DataToolTable extends DataTable {
	// static fields and constants
	protected final static int RENAME_COLUMN_EDIT = 0;
	protected final static int INSERT_COLUMN_EDIT = 1;
	protected final static int DELETE_COLUMN_EDIT = 2;
	protected final static int INSERT_CELLS_EDIT = 3;
	protected final static int DELETE_CELLS_EDIT = 4;
	protected final static int REPLACE_CELLS_EDIT = 5;
	protected final static int INSERT_ROWS_EDIT = 6;
	protected final static int DELETE_ROWS_EDIT = 7;

	protected static String[] editTypes = { "rename column", "insert column", //$NON-NLS-1$ //$NON-NLS-2$
			"delete column", "insert cells", "delete cells", "replace cells", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"insert rows", "delete rows" }; //$NON-NLS-1$ //$NON-NLS-2$
	protected static Color xAxisColor = new Color(255, 255, 153); // yellow
	protected static Color yAxisColor = new Color(204, 255, 204); // light green

	// instance fields
	/**
	 * tab that displays this table
	 */
	DataToolTab dataToolTab;
	/**
	 * manages datasets for table
	 */
	DatasetManager dataManager;
	/**
	 * first two table columns in x-y order
	 */
	WorkingDataset workingData;
	/**
	 * maps column name to working dataset
	 */
	HashMap<String, WorkingDataset> workingMap = new HashMap<String, WorkingDataset>(); 
	/**
	 *  selected rows of working data
	 */
	HighlightableDataset selectedData = new HighlightableDataset();
	
	HeaderRenderer headerRenderer;
	LabelRenderer labelRenderer = new LabelRenderer();
	DataCellRenderer dataRenderer = new DataCellRenderer();
	DataEditor editor = new DataEditor();
	JPopupMenu popup;

	JPopupMenu getPopup() {
		return (popup == null ? (popup = new JPopupMenu()) : popup);	
	}
	
	JMenuItem renameColumnItem, copyColumnsItem, cutColumnsItem, pasteColumnsItem, cloneColumnsItem, numberFormatItem;
	JMenuItem insertRowItem, pasteRowsItem, copyRowsItem, cutRowsItem;
	JMenuItem insertCellsItem, deleteCellsItem, copyCellsItem, cutCellsItem, pasteInsertCellsItem, pasteCellsItem;
	JMenuItem addEndRowItem, trimRowsItem;
	JMenuItem selectAllItem, selectNoneItem, clearContentsItem;
	Action clearCellsAction, pasteCellsAction, pasteInsertCellsAction, cantPasteCellsAction, cantPasteRowsAction,
			getPasteDataAction;
	MouseAdapter tableMouseListener;
	Color selectedBG, selectedFG, unselectedBG, selectedHeaderFG, selectedHeaderBG, rowBG;
	int focusRow, focusCol, mouseRow, mouseCol;
	int leadCol = 0, leadRow = 0, prevSortedColumn; // used for shift-click selections
	int pasteW, pasteH;
	HashMap<String, double[]> pasteValues = new HashMap<String, double[]>();
	DatasetManager pasteData = null;
	HashMap<Integer, Integer> workingRowToModelRow = new HashMap<Integer, Integer>(); // maps working row to model row
    @Override
	protected OSPDataTableModel createTableModel() {
    	return new DataToolTableModel();
	}
    
	/**
	 * Constructs a DataToolTable for the specified DataTooltab.
	 *
	 * @param tab
	 */
	public DataToolTable(DataToolTab tab) {
		super();
		init();
		((DataToolTableModel) getModel()).tab = tab;
		dataToolTab = tab;
		dataManager = tab.dataManager;
		add(dataManager);
		setRowNumberVisible(true);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		headerRenderer = new HeaderRenderer(getTableHeader().getDefaultRenderer());
		getTableHeader().setDefaultRenderer(headerRenderer);
		// selection listener for table
		ListSelectionModel selectionModel = getSelectionModel();
		selectionModel.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getFirstIndex() == -1) {
					return;
				}
				dataTableModel.setSelectedRowsFromJTable();
				if (!e.getValueIsAdjusting()) {
					int labelCol = convertColumnIndexToView(0);
					addColumnSelectionInterval(labelCol, labelCol);
					dataToolTab.setSelectedData(getSelectedData(), true);
					
				}
			}
		});
		// selection listener for column header
//		selectionModel = getTableHeader().getColumnModel().getSelectionModel();
//		selectionModel.addListSelectionListener(new ListSelectionListener() {
//			@Override
//			public void valueChanged(ListSelectionEvent e) {
//				getTableHeader().repaint();
//				//dataToolTab.refreshPlot(false);
//			}
//
//		});
		
		installActions();
	}

	private void installActions() {
		// create actions
		clearCellsAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// clear selected cells by replacing with NaN
				HashMap<String, double[]> values = new HashMap<String, double[]>();
				Iterator<String> it = getSelectedColumnNames().iterator();
				while (it.hasNext()) {
					values.put(it.next(), null);
				}
				int[] rows = getSelectedModelRows();
				HashMap<String, double[]> prev = replaceCells(rows, values);
				// post edit: target is rows, value is HashMap[] {undo, redo}
				TableEdit edit = new TableEdit(REPLACE_CELLS_EDIT, null, rows, new HashMap[] { prev, values });
				dataToolTab.undoSupport.postEdit(edit);
				refreshUndoItems();
			}

		};
		pasteCellsAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int[] rows = getSelectedModelRows();
				if (!pasteValues.isEmpty() && ((rows.length == 1) || (pasteH == rows.length))) {
					int[] pasteRows = new int[pasteH];
					if (pasteH == rows.length) {
						pasteRows = rows;
					} else {
						pasteRows[0] = rows[0];
						int vRow = getViewRow(rows[0]);
						for (int i = 1; i < pasteH; i++) {
							while (vRow + i >= getRowCount()) {
								int[] row = new int[] { getRowCount() };
								insertRows(row, null);
							}
							pasteRows[i] = getModelRow(vRow + i);
						}
					}
					HashMap<String, double[]> prev = replaceCells(pasteRows, pasteValues);
					// post edit: target is rows, value is HashMap[] {undo, redo}
					TableEdit edit = new TableEdit(REPLACE_CELLS_EDIT, null, pasteRows,
							new HashMap[] { prev, pasteValues });
					dataToolTab.undoSupport.postEdit(edit);
					refreshUndoItems();
				} else {
					cantPasteCellsAction.actionPerformed(e);
				}
			}

		};
		pasteInsertCellsAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int[] rows = getSelectedModelRows();
				if (!pasteValues.isEmpty() && ((rows.length == 1) || (pasteH == rows.length))) {
					int[] pasteRows = new int[pasteH];
					if (pasteH == rows.length) {
						pasteRows = rows;
					} else {
						pasteRows[0] = rows[0];
						int vRow = getViewRow(rows[0]);
						for (int i = 1; i < pasteH; i++) {
							while (vRow + i >= getRowCount()) {
								int[] row = new int[] { getRowCount() };
								insertRows(row, null);
							}
							pasteRows[i] = getModelRow(vRow + i);
						}
					}
					insertCells(pasteRows, pasteValues);
					// post edit: target is rows, value is HashMap
					TableEdit edit = new TableEdit(INSERT_CELLS_EDIT, null, pasteRows, pasteValues);
					dataToolTab.undoSupport.postEdit(edit);
					refreshUndoItems();
				} else {
					cantPasteCellsAction.actionPerformed(e);
				}
			}

		};
		cantPasteCellsAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(dataToolTab,
						ToolsRes.getString("DataToolTable.Dialog.CantPasteCells.Message1") //$NON-NLS-1$
								+ " " + pasteW + " x " + pasteH + "\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								+ ToolsRes.getString("DataToolTable.Dialog.CantPasteCells.Message2"), //$NON-NLS-1$
						ToolsRes.getString("DataToolTable.Dialog.CantPaste.Title"), //$NON-NLS-1$
						JOptionPane.WARNING_MESSAGE);
			}

		};
		cantPasteRowsAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(dataToolTab,
						ToolsRes.getString("DataToolTable.Dialog.CantPasteRows.Message1") //$NON-NLS-1$
								+ " " + pasteH + "\n" //$NON-NLS-1$ //$NON-NLS-2$
								+ ToolsRes.getString("DataToolTable.Dialog.CantPasteRows.Message2"), //$NON-NLS-1$
						ToolsRes.getString("DataToolTable.Dialog.CantPaste.Title"), //$NON-NLS-1$
						JOptionPane.WARNING_MESSAGE);
			}

		};
		getPasteDataAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pasteValues.clear();
				pasteData = null;
				// put clipboard data into pasteValues map
				String dataString = DataTool.paste();
				ArrayList<String> colNames = getSelectedColumnNames();
				if (dataString != null) {
					pasteData = DataTool.parseData(dataString, null);
					if (pasteData != null) {
						pasteW = pasteData.getDatasets().size();
						if ((pasteW > 0) && (pasteW == colNames.size())) {
							pasteH = pasteData.getDataset(0).getIndex();
							if (pasteH > 0) {
								for (int i = 0; i < pasteW; i++) {
									double[] vals = pasteData.getDataset(i).getYPoints();
									pasteValues.put(colNames.get(i), vals);
								}
							}
						}
					}
				}
			}

		};
		// mouse motion listener for table header
		getTableHeader().addMouseMotionListener(new MouseInputAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				int n = getTableHeader().columnAtPoint(e.getPoint());
				n = convertColumnIndexToModel(n);
				if (n == 0) {
					getTableHeader().setToolTipText(ToolsRes.getString("DataToolTable.Header.Deselect.Tooltip")); //$NON-NLS-1$
				} else {
					getTableHeader().setToolTipText(ToolsRes.getString("DataToolTable.Header.Tooltip")); //$NON-NLS-1$
				}
			}

		});
		// mouse listener for table header
		getTableHeader().addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				doHeaderMouseClicked(e);
			}

		});
		
		// mouse motion listener for table
		addMouseMotionListener(new MouseInputAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if (popup == null || !popup.isVisible()) {
					int row = rowAtPoint(e.getPoint());
					int col = columnAtPoint(e.getPoint());
					int labelCol = convertColumnIndexToView(0);
					mouseRow = row;
					mouseCol = col;
					dataRenderer.showFocus = (col == labelCol);
					dorepaint(2);
					if (col == labelCol) {
						dataRenderer.showFocus = true;
						setToolTipText(ToolsRes.getString("DataToolTable.Deselect.Tooltip")); //$NON-NLS-1$
					} else {
						Object obj = getValueAt(row, col);
						String name = getColumnName(col);
						setToolTipText(name + " = " + obj); //$NON-NLS-1$
					}
				}
				requestFocusInWindow();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				int col = columnAtPoint(e.getPoint());
				int row = rowAtPoint(e.getPoint());
				mouseRow = row;
				mouseCol = col;
				int labelCol = convertColumnIndexToView(0);
				if (col == labelCol) {
					if (leadRow < getRowCount()) {
						setRowSelectionInterval(leadRow, row);
					}
					setColumnSelectionInterval(getColumnCount() - 1, 0);
				}
				dataRenderer.showFocus = false;
				// update selected data in curve fitter and plot
				dataToolTab.setSelectedData(getSelectedData(), false);
				dataToolTab.refreshFit();
				dataToolTab.plot.repaint();
			}

		});
		// mouse listener for table
		tableMouseListener = new MouseAdapter() {
			@Override
			public void mouseExited(MouseEvent e) {
				if (popup == null || !popup.isVisible()) {
					mouseRow = -1;
					dataRenderer.showFocus = true;
					dorepaint(1);
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				doTableMousePressed(e);
			}

		};
		addMouseListener(tableMouseListener);
		// override default enter action
		InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		Action enterAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// start editing focused cell
				editCellAt(focusRow, focusCol, e);
				editor.field.requestFocus();
			}

		};
		KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		getActionMap().put(im.get(enter), enterAction);
		// override default copy action
		Action copyAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dataToolTab.copyTableDataToClipboard();
			}

		};
		int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, mask);
		getActionMap().put(im.get(copy), copyAction);
		// override default paste action
		Action pasteAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getPasteDataAction.actionPerformed(e);
				pasteCellsAction.actionPerformed(e);
			}

		};
		KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V, mask);
		getActionMap().put(im.get(paste), pasteAction);
		// associate clear cells action with delete key
		KeyStroke delete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
		im.put(delete, clearCellsAction);
		getActionMap().put(im.get(delete), clearCellsAction);
	}

	@Override
	protected void updateRowSelection(int firstIndex, boolean isAdjusting) {
		if (firstIndex == -1) {
			return;
		}
		dataTableModel.setSelectedRowsFromJTable();
		if (!isAdjusting) {
			int labelCol = convertColumnIndexToView(0);
			addColumnSelectionInterval(labelCol, labelCol);
			dataToolTab.setSelectedData(getSelectedData(), true);
			
		}
	}

	protected void doTableMousePressed(MouseEvent e) {
		final int col = columnAtPoint(e.getPoint());
		final int row = rowAtPoint(e.getPoint());
		int labelCol = convertColumnIndexToView(0);
		// right-click: show popup menu
		if (OSPRuntime.isPopupTrigger(e)) {
			getTablePopup(row, col, labelCol);
			popup.show(DataToolTable.this, e.getX(), e.getY() + 8);
			return;
		}
		// single click: show focus
		dataRenderer.showFocus = true;
		// label column clicked: clear selection or select row
		if (col == labelCol) {
			// double-click: select only that row
			if (e.getClickCount() == 2) {
				leadRow = row;
				setRowSelectionInterval(row, row);
				setColumnSelectionInterval(0, getColumnCount() - 1);
			}
			// shift-click: extend row selection
			else if (e.isShiftDown() && (leadRow < getRowCount())) {
				setRowSelectionInterval(leadRow, row);
				setColumnSelectionInterval(0, getColumnCount() - 1);
//				setSelectedColumnsFromBitSet();
			}
			// control-click: select/deselect rows
			else if (e.isControlDown() || e.isShiftDown()) {
//    leadRow = row;
//    if(getSelectedRows().length==0) {
//      clearSelection();
//    } else {
//      setColumnSelectionInterval(0, getColumnCount()-1);
//    }
			}
			// single click: clear selection
			else {
				leadRow = row;
				leadCol = 1;
			}
		} else if (!e.isControlDown() && !e.isShiftDown()) {
			leadRow = row;
			leadCol = col;
		}
		// save selected columns
		
		addColumnSelectionInterval(labelCol, labelCol);
		dataTableModel.setColumnSelectionFromJTable();
		getSelectedData();
		dataToolTab.plot.repaint();
	}

	private void getTablePopup(int row, int col, int labelCol) {
		getPopup();
		editor.stopCellEditing();
		// select appropriate cells
		if (col == labelCol) {
			if (!isRowSelected(row)) {
				setRowSelectionInterval(row, row);
			}
			setColumnSelectionInterval(0, getColumnCount() - 1);
		} else if (!isCellSelected(row, col)) {
			setRowSelectionInterval(row, row);
			setColumnSelectionInterval(col, col);
			leadCol = col;
			leadRow = row;
		}
		dorepaint(3);
		getPasteDataAction.actionPerformed(null);
		final int[] rows = getSelectedModelRows();
		// determine if selection is all empty cells
		boolean isEmptyCells = true;
		int[] selectedRows = getSelectedRows();
		ArrayList<String> selectedColumns = getSelectedColumnNames();
		for (int i = 0; i < selectedRows.length; i++) {
			if (!isEmptyCells(selectedRows[i], selectedColumns)) {
				isEmptyCells = false;
				break;
			}
		}
		popup.removeAll();
		String text;
		// data cell clicked: show cells popup
		if (col != labelCol) {
			int index = convertColumnIndexToModel(col) - 1;
			final Dataset data = dataManager.getDataset(index);
			mouseRow = row;
			mouseCol = col;
			dorepaint(4);

			text = ToolsRes.getString("DataToolTable.Popup.MenuItem.SelectAll"); //$NON-NLS-1$
			selectAllItem = new JMenuItem(text);
			selectAllItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					selectAllCells();
				}
			});
			popup.add(selectAllItem);
			text = ToolsRes.getString("DataToolTable.Popup.MenuItem.SelectNone"); //$NON-NLS-1$
			selectNoneItem = new JMenuItem(text);
			selectNoneItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					clearSelection();
				}
			});
			popup.add(selectNoneItem);
			popup.addSeparator();

			if (dataToolTab.isUserEditable() && !(data instanceof DataFunction)) {
				// insert cells item
				text = ToolsRes.getString("DataToolTable.Popup.MenuItem.InsertCells"); //$NON-NLS-1$
				insertCellsItem = new JMenuItem(text);
				insertCellsItem.setActionCommand(String.valueOf(col));
				insertCellsItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						HashMap<String, double[]> emptyRow = new HashMap<String, double[]>();
						Iterator<String> it = getSelectedColumnNames().iterator();
						while (it.hasNext()) {
							emptyRow.put(it.next(), null);
						}
						insertCells(rows, emptyRow);
						// post edit: target is rows, value is HashMap
						TableEdit edit = new TableEdit(INSERT_CELLS_EDIT, null, rows, emptyRow);
						dataToolTab.undoSupport.postEdit(edit);
						refreshUndoItems();
					}

				});
				popup.add(insertCellsItem);
				// paste insert cells item
				if (pasteData != null) {
					text = ToolsRes.getString("DataToolTable.Popup.MenuItem.PasteInsertCells"); //$NON-NLS-1$
					pasteInsertCellsItem = new JMenuItem(text);
					pasteInsertCellsItem.setActionCommand(String.valueOf(col));
					pasteInsertCellsItem.addActionListener(pasteInsertCellsAction);
					popup.add(pasteInsertCellsItem);
				}
				// delete cells item
				text = ToolsRes.getString("DataToolTable.Popup.MenuItem.DeleteCells"); //$NON-NLS-1$
				deleteCellsItem = new JMenuItem(text);
				deleteCellsItem.setActionCommand(String.valueOf(col));
				deleteCellsItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						Iterator<String> it = getSelectedColumnNames().iterator();
						while (it.hasNext()) {
							pasteValues.put(it.next(), null);
						}
						HashMap<String, double[]> prev = deleteCells(rows, pasteValues);
						// post edit: target is rows, value is HashMap
						TableEdit edit = new TableEdit(DELETE_CELLS_EDIT, null, rows, prev);
						dataToolTab.undoSupport.postEdit(edit);
						refreshUndoItems();
					}

				});
				popup.add(deleteCellsItem);
			}
			if (!isEmptyCells || (pasteData != null)) {
				if (popup.getComponentCount() > 0 && !dataToolTab.originShiftEnabled) {
					popup.addSeparator();
				}
				if (!isEmptyCells) {
					// copy cells item
					text = ToolsRes.getString("DataToolTable.Popup.MenuItem.CopyCells"); //$NON-NLS-1$
					copyCellsItem = new JMenuItem(text);
					copyCellsItem.setActionCommand(String.valueOf(col));
					copyCellsItem.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							dataToolTab.copyTableDataToClipboard();
						}

					});
					popup.add(copyCellsItem);
					if (dataToolTab.isUserEditable() && !(data instanceof DataFunction)) {
						// cut cells item
						text = ToolsRes.getString("DataToolTable.Popup.MenuItem.CutCells"); //$NON-NLS-1$
						cutCellsItem = new JMenuItem(text);
						cutCellsItem.setActionCommand(String.valueOf(col));
						cutCellsItem.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								copyCellsItem.doClick();
								clearCellsAction.actionPerformed(e);
							}

						});
						popup.add(cutCellsItem);
						text = ToolsRes.getString("DataToolTable.Popup.MenuItem.DeleteContents"); //$NON-NLS-1$
						clearContentsItem = new JMenuItem(text);
						clearContentsItem.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								clearCellsAction.actionPerformed(null);
							}
						});
						popup.add(clearContentsItem);
					}
				}
				if (dataToolTab.isUserEditable() && pasteData != null) {
					// paste cells item
					text = ToolsRes.getString("DataToolTable.Popup.MenuItem.PasteCells"); //$NON-NLS-1$
					pasteCellsItem = new JMenuItem(text);
					pasteCellsItem.setActionCommand(String.valueOf(col));
					pasteCellsItem.addActionListener(pasteCellsAction);
					popup.add(pasteCellsItem);
				}
			}

		}
		// label cell clicked: set lead row and show row popup
		else {
			leadRow = row;
			if (dataToolTab.isUserEditable()) {
				// insert row item
				text = ToolsRes.getString("DataToolTable.Popup.MenuItem.InsertRows"); //$NON-NLS-1$
				insertRowItem = new JMenuItem(text);
				insertRowItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						HashMap<String, double[]> prev = insertRows(rows, null);
						// post edit: target is rows, value is map
						TableEdit edit = new TableEdit(INSERT_ROWS_EDIT, null, rows, prev);
						dataToolTab.undoSupport.postEdit(edit);
						refreshUndoItems();
					}

				});
				popup.add(insertRowItem);
				// determine if clipboard contains row data
				boolean hasRows = !pasteValues.isEmpty();
				if (hasRows) {
					Iterator<String> it = pasteValues.keySet().iterator();
					while (it.hasNext()) {
						String next = it.next();
						hasRows = hasRows && (pasteData.getDatasetIndex(next) > -1);
					}
				}
				// paste rows item
				if (hasRows) {
					text = ToolsRes.getString("DataToolTable.Popup.MenuItem.PasteInsertRows"); //$NON-NLS-1$
					pasteRowsItem = new JMenuItem(text);
					pasteRowsItem.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							if ((rows.length != 1) && (pasteH != rows.length)) {
								cantPasteRowsAction.actionPerformed(e);
								return;
							}
							int[] pasteRows = new int[pasteH];
							if (pasteH == rows.length) {
								pasteRows = rows;
							} else if (rows.length == 1) {
								pasteRows[0] = rows[0];
								for (int i = 1; i < pasteH; i++) {
									pasteRows[i] = rows[0] + i;
								}
							}
							insertRows(pasteRows, pasteValues);
							// post edit: target is rows, value is map
							TableEdit edit = new TableEdit(INSERT_ROWS_EDIT, null, pasteRows, pasteValues);
							dataToolTab.undoSupport.postEdit(edit);
							refreshUndoItems();
						}

					});
					popup.add(pasteRowsItem);
				}
				popup.addSeparator();
			}
			// copy row item
			text = ToolsRes.getString("DataToolTable.Popup.MenuItem.CopyRows"); //$NON-NLS-1$
			copyRowsItem = new JMenuItem(text);
			copyRowsItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					OSPLog.finest("copying rows"); //$NON-NLS-1$
					String data = dataToolTab.getSelectedTableData();
					DataTool.copy(data);
				}

			});
			popup.add(copyRowsItem);
			if (dataToolTab.isUserEditable()) {
				// cut row item
				text = ToolsRes.getString("DataToolTable.Popup.MenuItem.CutRows"); //$NON-NLS-1$
				cutRowsItem = new JMenuItem(text);
				cutRowsItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						copyRowsItem.doClick();
						int[] rows = getSelectedModelRows();
						HashMap<String, double[]> removed = deleteRows(rows);
						// post edit: target is rows, value is map
						TableEdit edit = new TableEdit(DELETE_ROWS_EDIT, null, rows, removed);
						dataToolTab.undoSupport.postEdit(edit);
						refreshUndoItems();
					}

				});
				popup.add(cutRowsItem);
				popup.addSeparator();
				// addEndRow item
				text = ToolsRes.getString("DataToolTable.Popup.MenuItem.AddEndRow"); //$NON-NLS-1$
				addEndRowItem = new JMenuItem(text);
				addEndRowItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						insertRows(new int[] { getRowCount() }, null);
					}

				});
				popup.add(addEndRowItem);
				if (isEmptyRow(getRowCount() - 1)) {
					// trimRows item
					text = ToolsRes.getString("DataToolTable.Popup.MenuItem.TrimRows"); //$NON-NLS-1$
					trimRowsItem = new JMenuItem(text);
					trimRowsItem.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							trimEmptyRows(0);
						}

					});
					popup.add(trimRowsItem);
				}
			}
		}
		FontSizer.setFonts(popup, FontSizer.getLevel());
	}

	protected void doHeaderMouseClicked(MouseEvent e) {
		if (getRowCount() == 0)
			return;
		final java.awt.Point mousePt = e.getPoint();
		final int col = columnAtPoint(mousePt);
		if (col == -1) {
			return;
		}
		int labelCol = convertColumnIndexToView(0);
		// save selected columns and rows
		ArrayList<String> cols = getSelectedColumnNames();
		// right-click: set lead column and show popup menu
		if (OSPRuntime.isPopupTrigger(e)) {
			if (col == labelCol) {
				return;
			}
			String colName = getColumnName(col);
			if (!cols.contains(colName)) {
				setColumnSelectionInterval(col, col);
				leadCol = col;
			}
			createHeaderPopup(col);
			popup.show(getTableHeader(), e.getX(), e.getY() + 8);
		}
		// double-click: select column and all rows (or select table if label column)
		else if (e.getClickCount() == 2) {
			if (col == labelCol) {
				selectAllCells();
			} else {
				setRowSelectionInterval(0, getRowCount() - 1); // all rows
				setColumnSelectionInterval(col, col);
				leadCol = col;
			}
			// sort by row number
			sort(0);
		}
		// single click label column: refresh selected rows
		else if (col == labelCol && dataTableModel.getSortedColumn() == col) {
			if (col != prevSortedColumn) {
				int[] rows = getSelectedModelRows();
				selectModelRows(rows);
				prevSortedColumn = col;
			}
		}
		// control-click: add/remove columns to selection
		else if (e.isControlDown()) {
			if (col != labelCol && isColumnSelected(col)) {
				removeColumnSelectionInterval(col, col);
			} else {
				if (haveSelectedRows())
					addColumnSelectionInterval(col, col);
				if (getSelectedColumns().length == 1) {
					leadCol = col;
				}
			}
		}
		// shift-click: extend selection
		else if (e.isShiftDown() && haveSelectedRows()) {
			if (leadCol < getColumnCount()) {
				setColumnSelectionInterval(col, leadCol);
			}
		}
		// single click: refresh selected rows
		else {
			if (col != prevSortedColumn) {
				int[] rows = getSelectedModelRows();
				selectModelRows(rows);
				prevSortedColumn = col;
			}
		}
		getSelectedData();
		// save selected columns
		addColumnSelectionInterval(labelCol, labelCol);
	}

	protected void createHeaderPopup(int col) {
		getPopup();
		setRowSelectionInterval(0, getRowCount() - 1);
		popup.removeAll();
		String text;
		// get newly selected columns
		ArrayList<String> cols = getSelectedColumnNames();
		// rename column item
		if ((cols.size() == 1) && dataToolTab.isUserEditable()) {
			int index = convertColumnIndexToModel(col) - 1;
			final Dataset data = dataManager.getDataset(index);
			text = ToolsRes.getString("DataToolTable.Popup.MenuItem.RenameColumn"); //$NON-NLS-1$
			renameColumnItem = new JMenuItem(text);
			renameColumnItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (data instanceof DataFunction) {
						showDataBuilder();
						return;
					}
					String prevName = data.getYColumnName();
					Object input = JOptionPane.showInputDialog(dataToolTab,
							ToolsRes.getString("DataToolTable.Dialog.NameColumn.Message"), //$NON-NLS-1$
							ToolsRes.getString("DataToolTable.Dialog.NameColumn.Title"), //$NON-NLS-1$
							JOptionPane.QUESTION_MESSAGE, null, null, prevName);
					if ((input == null) || input.equals("")) { //$NON-NLS-1$
						return;
					}
					String newName = dataToolTab.getUniqueYColumnName(data, input.toString(), true);
					if (newName == null) {
						return;
					}
					// remove any characters after a closing brace
					int n = newName.indexOf("}"); //$NON-NLS-1$
					if (n == 0)
						return;
					if (n > -1) {
						newName = newName.substring(0, n + 1);
					}
					renameColumn(prevName, newName);
					// post edit: target is null, value is previous name
					TableEdit edit = new TableEdit(RENAME_COLUMN_EDIT, newName, null, prevName);
					dataToolTab.undoSupport.postEdit(edit);
				}

			});
			popup.add(renameColumnItem);
			popup.addSeparator();
		}
		// copy column item
		text = ToolsRes.getString("DataToolTable.Popup.MenuItem.CopyColumns"); //$NON-NLS-1$
		copyColumnsItem = new JMenuItem(text);
		copyColumnsItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dataToolTab.copyTableDataToClipboard();
			}

		});
		popup.add(copyColumnsItem);
		// cut columns item: only if all selected columns are deletable
		boolean addCutItem = true;
		for (String name : cols) {
			if (!dataToolTab.isDeletable(getDataset(name))) {
				addCutItem = false;
			}
		}
		if (addCutItem) {
			text = ToolsRes.getString("DataToolTable.Popup.MenuItem.CutColumns"); //$NON-NLS-1$
			cutColumnsItem = new JMenuItem(text);
			cutColumnsItem.setActionCommand(String.valueOf(col));
			cutColumnsItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					copyColumnsItem.doClick();
					deleteSelectedColumns(); // also posts undoable edits
				}

			});
			popup.add(cutColumnsItem);
		}
		// paste columns item
		if ((dataToolTab != null) && (dataToolTab.dataTool != null)
				&& dataToolTab.dataTool.hasPastableData()
				&& dataToolTab.dataTool.hasPastableColumns(dataToolTab)) {
			text = ToolsRes.getString("DataToolTable.Popup.MenuItem.PasteColumns"); //$NON-NLS-1$
			pasteColumnsItem = new JMenuItem(text);
			pasteColumnsItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					dataToolTab.dataTool.pasteColumnsItem.doClick(0);
				}

			});
			popup.add(pasteColumnsItem);
		}
		// clone column item
		popup.addSeparator();
		text = ToolsRes.getString("DataToolTable.Popup.MenuItem.CloneColumns"); //$NON-NLS-1$
		cloneColumnsItem = new JMenuItem(text);
		cloneColumnsItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ArrayList<String> colNames = getSelectedColumnNames();
				for (int i = 0; i < colNames.size(); i++) {
					Dataset data = getDataset(colNames.get(i));
					if (data == null) {
						continue;
					}
					Dataset clone = DataTool.copyDataset(data, null, false);
					double[] x = data.getXPoints();
					double[] y = data.getYPoints();
					clone.append(x, y);
					String name = data.getYColumnName();
					String postfix = "_" + ToolsRes.getString("DataTool.Clone.Subscript"); //$NON-NLS-1$ //$NON-NLS-2$
					int n = name.indexOf(postfix);
					if (n > -1) {
						name = name.substring(0, n);
					}
					name = name + postfix;
					name = dataToolTab.getUniqueYColumnName(clone, name, false);
					clone.setXYColumnNames(data.getXColumnName(), name);
					ArrayList<DataColumn> loadedColumns = dataToolTab.loadData(clone, false);
					if (!loadedColumns.isEmpty()) {
						for (DataColumn next : loadedColumns) {
							next.deletable = true;
						}
					}
				}
			}

		});
		popup.add(cloneColumnsItem);
		// number format item
		popup.addSeparator();
		text = ToolsRes.getString("DataToolTable.Popup.MenuItem.NumberFormat"); //$NON-NLS-1$
		numberFormatItem = new JMenuItem(text);
		numberFormatItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				numberFormatAction();
			}

		});
		popup.add(numberFormatItem);
		FontSizer.setFonts(popup, FontSizer.getLevel());
	}

	private void numberFormatAction() {
		// get list of all column names
		int colCount = getColumnCount();
		String[] names = new String[colCount - 1];
		int labelCol = convertColumnIndexToView(0);
		int index = 0;
		for (int i = 0; i < colCount; i++) {
			if (i == labelCol)
				continue;
			String name = getColumnName(i);
			names[index] = name;
			index++;
		}
		ArrayList<String> selected = getSelectedColumnNames();
		String[] selectedNames = new String[selected.size()];
		for (int i = 0; i < selectedNames.length; i++) {
			selectedNames[i] = selected.get(i);
		}
		NumberFormatDialog dialog = getFormatDialog(names, selectedNames);
		dialog.setVisible(true);
		dataToolTab.refreshPlot();
	}

	protected void dorepaint(int i) {
		switch (i) {
		case 1:
		case 2:
		case 3:
		case 4:
		default:
			if (!JSUtil.isJS)
				repaint(); // BH 2020.02.14 seeing if we can avoid this

		}
	}

	/**
	 * Gets the working data for a specified column name. The working y-data is the
	 * named table column The working x-data is the x (yellow) table column
	 *
	 * @param colName the name of the data column
	 * @return the working dataset
	 */
	protected WorkingDataset getWorkingData(String colName) {
		if (colName == null) {
			return null;
		}
		// find or create working data
		WorkingDataset working = workingMap.get(colName);
		if (working == null && dataToolTab.originShiftEnabled) {
			working = workingMap.get(colName.substring(0, colName.length() - DataToolTab.SHIFTED.length()));
		}
		if (working == null) {
			Dataset ySource = getDataset(colName);
			if (ySource == null) {
				return null;
			}
			working = new WorkingDataset(ySource);
			if (ySource.getMarkerShape() == Dataset.NO_MARKER) {
				ySource.setMarkerShape(Dataset.SQUARE);
				working.setMarkersVisible(false);
			}
			workingMap.put(colName, working);
		}
		// set x-column source of working data
		int labelCol = convertColumnIndexToView(0);
		String xName = getColumnName((labelCol == 0) ? 1 : 0);
		Dataset xSource = getDataset(xName);
		if (xSource == null) {
			return null;
		}
		working.setXSource(xSource);
		// set marker and line properties of working data
		Dataset ySource = working.getYSource();
		working.setMarkerColor(ySource.getFillColor(), ySource.getEdgeColor());
		working.setMarkerSize(ySource.getMarkerSize());
		working.markerType = ySource.getMarkerShape();
		working.setLineColor(ySource.getLineColor());
		working.setConnected(ySource.isConnected());
		return working;
	}

	/**
	 * Gets the working data: first two data columns in x-y order
	 *
	 * @return the working dataset
	 */
	protected WorkingDataset getWorkingData() {
		int n = dataManager.getDatasets().size();
		if (n < 2) {
			workingData = null;
		} else {
			int labelCol = convertColumnIndexToView(0);
			int yCol = (labelCol < 2) ? 2 : 1;
			String yName = getColumnName(yCol);
			workingData = getWorkingData(yName);
		}
		return workingData;
	}

	/**
	 * Removes the working data for a specified column name.
	 *
	 * @param colName the name of the data column
	 */
	protected void removeWorkingData(String colName) {
		if (colName == null) {
			return;
		}
		workingMap.remove(colName);
		setFormatPattern(colName, null);
		refreshTable(DataTable.MODE_COLUMN);
	}

	protected void deleteSelectedColumns() {
		ArrayList<String> colNames = getSelectedColumnNames();
		int[] cols = getSelectedColumns();
		for (int i = colNames.size() - 1; i > -1; i--) {
			String name = colNames.get(i);
			Dataset target = getDataset(name);
			if (!dataToolTab.isDeletable(target)) {
				continue;
			}
			Dataset deleted = deleteColumn(name);
			if (deleted == null) {
				continue;
			}
			// post edit: target is column, value is dataset
			Integer colInt = Integer.valueOf(cols[i]);
			TableEdit edit = new TableEdit(DELETE_COLUMN_EDIT, name, colInt, deleted);
			dataToolTab.undoSupport.postEdit(edit);
		}
		refreshUndoItems();
	}

	/**
	 * Clears the working data.
	 */
	protected void clearWorkingData() {
		for (Iterator<String> it = workingMap.keySet().iterator(); it.hasNext();) {
			String colName = it.next().toString();
			setFormatPattern(colName, null);
		}
		workingMap.clear();
		refreshTable(DataTable.MODE_CLEAR);
	}

	/**
	 * Gets the source dataset associated with table column name.
	 * 
	 * @param colName the column name
	 * @return the dataset
	 */
	protected Dataset getDataset(String colName) {
		if (colName == null)
			return null;
		int i = dataManager.getDatasetIndex(colName);
		if (i == -1 && colName.endsWith(DataToolTab.SHIFTED)) {
			i = dataManager.getDatasetIndex(colName.substring(0, colName.length() - DataToolTab.SHIFTED.length()));
		}
		if (i > -1) {
			return dataManager.getDataset(i);
		}
		// check all datasets in dataManager to see if subscripting removed
		java.util.Iterator<Dataset> it = dataManager.getDatasets().iterator();
		while (it.hasNext()) {
			Dataset next = it.next();
			if (next.getYColumnName().equals(colName)) {
				return next;
			}
			if (colName.endsWith(DataToolTab.SHIFTED) && next.getYColumnName()
					.equals(colName.substring(0, colName.length() - DataToolTab.SHIFTED.length()))) {
				return next;
			}
		}
		return null;
	}

	/**
	 * Gets the selected data. The returned dataset consists of the selected rows in
	 * the first two columns of the table in x-y order. This also sets the
	 * highlights of the working data and populates the workingRows map.
	 *
	 * @return the data in the selected rows, or all data if no rows are selected
	 */
	protected HighlightableDataset getSelectedData() {
		if (getWorkingData() == null) {
			return null;
		}
		double[] xValues, yValues; // selected data values
		double[] x = workingData.getXSource().getYPoints();
		double[] y = workingData.getYSource().getYPoints();
		// map working index to row index
		int workingIndex = 0; // index in working data
		workingData.clearHighlights();
		for (int i = 0; i < x.length; i++) {
			if (Double.isNaN(x[i])) {
				continue;
			}
			workingRowToModelRow.put(Integer.valueOf(workingIndex++), Integer.valueOf(i));
		}
		// is x- or y-source column selected?
		int labelCol = convertColumnIndexToView(0);
		int xCol = (labelCol == 0) ? 1 : 0;
		int yCol = (labelCol < 2) ? 2 : 1;
		int[] cols = getSelectedColumns();
		boolean colSelected = false;
		for (int k = 0; k < cols.length; k++) {
			colSelected = colSelected || (cols[k] == xCol) || (cols[k] == yCol);
		}
		BitSet bs = (BitSet) getSelectedModelRowsBS().clone();
		int nsel = bs.cardinality();
		if (!colSelected || nsel == 0) { // nothing selected
			xValues = x;
			yValues = new double[x.length];
			for (int i = 0; i < yValues.length; i++) {
				yValues[i] = (i < y.length ? y[i] : Double.NaN);
			}
			bs.clear();
		} else {
			xValues = new double[nsel];
			yValues = new double[nsel];
			BitSet bsNew = new BitSet();
			bsNew.set(0, nsel);
			for (int i = 0, row = bs.nextSetBit(0); row >= 0;
					row = bs.nextSetBit(row + 1), i++) {
				xValues[i] = (row >= x.length) ? Double.NaN : x[row];
				yValues[i] = (row >= y.length) ? Double.NaN : y[row];
				boolean ignore = Double.isNaN(xValues[i]);
				if (ignore) {
					bsNew.clear(i);
				} else {
					workingData.setHighlighted(row, true);
					if (Double.isNaN(yValues[i]))
					bsNew.clear(i);
				}
			}
			bs = bsNew;
		}
		DataTool.copyDataset(workingData, selectedData, false);
		selectedData.clear();
		selectedData.setHighlights(bs);
		selectedData.append(xValues, yValues);
		return selectedData;
	}

	/**
	 * Gets the selected column names.
	 *
	 * @return ArrayList of selected column names
	 */
	protected ArrayList<String> getSelectedColumnNames() {
		int[] columns = getSelectedColumns();
		ArrayList<String> names = new ArrayList<String>();
		for (int i = 0; i < columns.length; i++) {
			int index = convertColumnIndexToModel(columns[i]) - 1;
			if (index < 0) {
				continue;
			}
			String name = dataManager.getDataset(index).getYColumnName();
			names.add(name);
		}
		return names;
	}

	/**
	 * Sets the selected column names.
	 *
	 * @param names Collection of column names to select
	 */
	protected void setSelectedColumnNames(Collection<String> names) {
		if (getColumnCount() < 1) {
			return;
		}
		removeColumnSelectionInterval(0, getColumnCount() - 1);
		Iterator<String> it = names.iterator();
		while (it.hasNext()) {
			String colName = it.next();
			int index = dataManager.getDatasetIndex(colName);
			if (index == -1) {
				continue;
			}
			int col = convertColumnIndexToView(index + 1);
			addColumnSelectionInterval(col, col);
		}
	}

	/**
	 * Inserts a column dataset.
	 *
	 * @param data the dataset to insert
	 * @param col  the insertion view column number
	 */
	protected void insertColumn(Dataset data, int col) {
		// save selected rows and columns
		BitSet rows = getSelectedModelRowsBS();
		ArrayList<String> cols = getSelectedColumnNames();
		// clear selection
		clearSelection();
		data.setXColumnVisible(false);
		ArrayList<Dataset> datasets = dataManager.getDatasets();
		// data, if added, will be last dataset
		int index = datasets.size();
		if (index == 0) {
			dataToolTab.originatorID = data.getID();
		}
		// determine model and view columns of data
//		int dataModelCol = index + 1; // default if not yet added
//		int dataViewCol = index + 1; // default if not yet added
		// save desired model column order
//		OSPDataTableModel model = (OSPDataTableModel) getModel();
//		int len = model.getColumnCount();
//		int[] modelColumns = new int[len + 1];
//		modelColumns[col] = dataModelCol;
//		// modelColumns index is view column and value is model column
//		for (int j = 0; j < len; j++) {
//			// j is current view column number
//			// modelCol is current model column number
//			int modelCol = convertColumnIndexToModel(j);
//			if (modelCol == dataModelCol) {
//				continue;
//			}
//			// viewCol is desired view column number
//			int viewCol = j;
//			if (j < dataViewCol) {
//				if (j >= col) {
//					viewCol++;
//				}
//			} else {
//				if (j <= col) {
//					viewCol--;
//				}
//			}
//			modelColumns[viewCol] = modelCol;
//		}
		// add data if not present
		if (data instanceof DataFunction) {
			FunctionTool tool = dataToolTab.getDataBuilder();
			FunctionPanel panel = tool.getPanel(dataToolTab.getName());
			String presentation = panel.undoManager.getPresentationName();
			if (panel.undoManager.canUndo() && presentation.equals("Deletion")) { //$NON-NLS-1$
				panel.undoManager.undo();
			}
		} else {
			dataManager.addDataset(data);
			getWorkingData(data.getYColumnName());
		}
		// refresh table and set column order
		
		updateColumnModel(null);
		// restore selected rows or select all rows if none selected
		if (rows.cardinality() == 0) {
			setRowSelectionInterval(0, getRowCount() - 1);
		} else {
			setSelectedModelRowsBS(rows);
		}
		// restore selected columns but include inserted column
		cols.add(data.getYColumnName());
		setSelectedColumnNames(cols);
		refreshTable(DataTable.MODE_COLUMN);
		refreshDataFunctions();
		dataToolTab.statsTable.refreshStatistics();
		dataToolTab.propsTable.refreshTable();
		dataToolTab.refreshGUI();
		dataToolTab.refreshPlot();
		dataToolTab.tabChanged(true);
		refreshUndoItems();
	}
	/**
	 * Deletes a column.
	 *
	 * @param colName the column name to delete
	 * @return the deleted dataset
	 */
	protected Dataset deleteColumn(String colName) {
		int index = dataManager.getDatasetIndex(colName);
		int deletedCol = convertColumnIndexToView(index + 1);
		Dataset data = dataManager.getDataset(index);
		// determine if sort column is to be deleted
		boolean sortColDeleted = dataTableModel.getSortedColumn() == index + 1;
		if (sortColDeleted) {
			sort(0);
		}
		// save selected rows and columns
		BitSet rows = getSelectedModelRowsBS();
		ArrayList<String> cols = getSelectedColumnNames();
		// clear selection
		clearSelection();
		// save desired model column order
		TableModel model = getModel();
		// modelColumns index is view column and value is model column
		int[] modelColumns = new int[model.getColumnCount() - 1];
		int viewCol = -1;
		for (int j = 0; j < model.getColumnCount(); j++) {
			// j is current view column number
			// viewCol is desired view column number
			if (j == deletedCol) {
				continue;
			}
			viewCol++;
			// modelCol is current model column number
			int modelCol = convertColumnIndexToModel(j);
			// decrement modelCol if greater than deleted model column
			if (modelCol > index + 1) {
				modelCol--;
			}
			modelColumns[viewCol] = modelCol;
		}
		if (data instanceof DataFunction) {
			FunctionTool tool = dataToolTab.getDataBuilder();
			FunctionPanel panel = tool.getPanel(dataToolTab.getName());
			// next line posts undo edit to FunctionPanel
			panel.functionEditor.removeObject((DataFunction) data, true);
		} else {
			dataManager.removeDataset(index);
			workingMap.remove(colName);
		}
		if (dataManager.getDatasets().isEmpty()) {
			dataToolTab.originatorID = 0;
			tableChanged(new TableModelEvent(getModel(), TableModelEvent.HEADER_ROW));
			dataToolTab.refreshGUI();
		} else {
			// refresh table and set column order
			updateColumnModel(null);
			// restore selection unless deleted column was only one selected
			if (!((cols.size() == 1) && cols.contains(colName))) {
				setSelectedModelRowsBS(rows);
				setSelectedColumnNames(cols);
			}
		}
		refreshTable(DataTable.MODE_COLUMN);
		refreshDataFunctions();
		dataToolTab.refreshPlot();
		dataToolTab.propsTable.refreshTable();
		dataToolTab.refreshGUI();
		dataToolTab.tabChanged(true);
		refreshUndoItems();
		dataToolTab.varPopup = null;
		return data;
	}

	/**
	 * Inserts cells with values specified by column name. Existing cells are
	 * shifted down and other columns are padded with NaN at the end if needed.
	 *
	 * @param rows   the model rows to insert
	 * @param values HashMap of column name to double[] values
	 * @return HashMap of column name to double[] inserted values
	 */
	protected HashMap<String, double[]> insertCells(int[] rows, HashMap<String, double[]> values) {
		int count = getRowCount();
		int[] fillRows = new int[rows.length];
		for (int i = 0; i < rows.length; i++) {
			fillRows[i] = count + i;
		}
		int[] cols = new int[values.keySet().size()];
		int k = 0;
		HashMap<String, double[]> inserted = new HashMap<String, double[]>();
		Iterator<Dataset> it = dataManager.getDatasets().iterator();
		while (it.hasNext()) {
			Dataset next = it.next();
			String colName = next.getYColumnName();
			if (values.keySet().contains(colName)) {
				// insert cells in columns with values
				double[] vals = values.get(colName);
				vals = insertPoints(next, rows, vals);
				inserted.put(colName, vals);
				int index = dataManager.getDatasetIndex(colName);
				cols[k++] = convertColumnIndexToView(index + 1);
			}
			// insert NaN cells at end of other columns
			else {
				insertPoints(next, fillRows, null);
			}
		}
		refreshDataFunctions();
		refreshTable(DataTable.MODE_CELLS);
		selectModelRows(rows);
		setSelectedColumnNames(values.keySet());
		dataToolTab.refreshPlot();
		refreshUndoItems();
		return inserted;
	}

	/**
	 * Deletes cells in a column. Remaining cells are shifted up.
	 *
	 * @param rows   the model rows to delete
	 * @param values HashMap of column name to (ignored) double[] values
	 * @return HashMap of column name to double[] deleted values
	 */
	protected HashMap<String, double[]> deleteCells(int[] rows, HashMap<String, double[]> values) {
		int startFillRow = getRowCount() - rows.length;
		HashMap<String, double[]> deleted = new HashMap<String, double[]>();
		// for each column in map keys, delete specified rows
		Iterator<String> it = values.keySet().iterator();
		while (it.hasNext()) {
			String colName = it.next();
			int index = dataManager.getDatasetIndex(colName);
			// identify the dataset
			Dataset dataset = dataManager.getDataset(index);
			// remove points at specified rows
			double[] removed = deletePoints(dataset, rows);
			deleted.put(colName, removed);
			int[] fillRows = new int[rows.length];
			for (int i = 0; i < rows.length; i++) {
				fillRows[i] = startFillRow + i;
			}
			// insert NaN points at end
			insertPoints(dataset, fillRows, null);
		}
		// trim empty rows
		trimEmptyRows(startFillRow - 1);
		refreshDataFunctions();
		refreshTable(DataTable.MODE_CELLS);
		setSelectedColumnNames(values.keySet());
		selectModelRows(rows);
		dataToolTab.refreshPlot();
		refreshUndoItems();
		return deleted;
	}

	/**
	 * Replaces cells.
	 *
	 * @param rows   the model rows to replace in ascending order
	 * @param values HashMap of column name to double[] new values
	 * @return HashMap of column name to double[] old values
	 */
	protected HashMap<String, double[]> replaceCells(int[] rows, HashMap<String, double[]> values) {
		int[] cols = new int[values.keySet().size()];
		HashMap<String, double[]> replaced = new HashMap<String, double[]>();
		Iterator<String> it = values.keySet().iterator();
		int i = 0;
		while (it.hasNext()) {
			String colName = it.next();
			double[] vals = values.get(colName);
			int index = dataManager.getDatasetIndex(colName);
			Dataset data = dataManager.getDataset(index);
			double[] pts = replacePoints(data, rows, vals);
			replaced.put(colName, pts);
			cols[i++] = convertColumnIndexToView(index + 1);
		}
		refreshDataFunctions();
		refreshTable(DataTable.MODE_VALUES);
		selectModelRows(rows);
		setSelectedColumnNames(values.keySet());
		refreshUndoItems();
		dataToolTab.refreshPlot();
		return replaced;
	}

	/**
	 * Inserts rows with values specified by column name. Unspecified values are set
	 * to NaN.
	 *
	 * @param rows   the model rows to insert
	 * @param values HashMap of column name to double[] values
	 * @return HashMap of column name to double[] inserted values
	 */
	protected HashMap<String, double[]> insertRows(int[] rows, HashMap<String, double[]> values) {
		if (values == null) {
			values = new HashMap<String, double[]>();
		}
		// ensure every column is included
		ArrayList<Dataset> datasets = dataManager.getDatasets();
		for (int i = 0; i < datasets.size(); i++) {
			Dataset next = datasets.get(i);
			String name = next.getYColumnName();
			if (!values.keySet().contains(name)) {
				values.put(name, null);
			}
		}
		// insert cells into all columns
		values = insertCells(rows, values);
		// scroll table to end if end row is newly inserted
		int endRow = getModelRow(getRowCount() - 1);
		for (int i = 0; i < rows.length; i++) {
			if (endRow == rows[i]) {
				computeVisibleRect(rect);
				rect.y = getSize().height - rect.height + getRowHeight();
				scrollRectToVisible(rect);
				break;
			}
		}
		return values;
	}

	private Rectangle rect = new Rectangle();

	/**
	 * Deletes rows.
	 *
	 * @param rows the model rows to delete
	 * @return the deleted values
	 */
	protected HashMap<String, double[]> deleteRows(int[] rows) {
		HashMap<String, double[]> removed = new HashMap<String, double[]>();
		// remove points from every dataset
		ArrayList<Dataset> dataSets = dataManager.getDatasets();
		for (int i = dataSets.size(); --i >= 0;) {
			Dataset ds = dataSets.get(i);
			removed.put(ds.getYColumnName(), deletePoints(ds, rows));
		}
		refreshTable(DataTable.MODE_DELETE_ROW);
		refreshDataFunctions();
		clearSelection();
		setSelectedColumnNames(removed.keySet());
		selectModelRows(rows);
		refreshUndoItems();
		dataToolTab.refreshPlot();
		return removed;
	}

	/**
	 * Determines if a row is empty.
	 *
	 * @param row the model row number
	 * @return true if all datasets are NaN at row index
	 */
	protected boolean isEmptyRow(int row) {
		boolean empty = true;
		Iterator<Dataset> it = dataManager.getDatasets().iterator();
		while (it.hasNext()) {
			Dataset data = it.next();
			if (data instanceof DataFunction) {
				continue;
			}
			double[] y = data.getYPoints();
			if (row >= y.length) {
				return false;
			}
			empty = empty && Double.isNaN(y[row]);
		}
		return empty;
	}

	/**
	 * Determines if a row is empty.
	 *
	 * @param row         the model row number
	 * @param columnNames a list of column names
	 * @return true if all named columns are NaN at row index
	 */
	protected boolean isEmptyCells(int row, ArrayList<String> columnNames) {
		boolean empty = true;
		Iterator<Dataset> it = dataManager.getDatasets().iterator();
		while (it.hasNext()) {
			Dataset data = it.next();
			String name = data.getYColumnName();
			if ((data instanceof DataFunction) || !columnNames.contains(name)) {
				continue;
			}
			double[] y = data.getYPoints();
			if (row >= y.length) {
				return false;
			}
			empty = empty && Double.isNaN(y[row]);
		}
		return empty;
	}

	/**
	 * Gets the x-axis view column.
	 *
	 * @return col the view column number
	 */
	protected int getXColumn() {
		if (getColumnCount() < 2) {
			return -1;
		}
		int labelCol = convertColumnIndexToView(0);
		return (labelCol == 0) ? 1 : 0;
	}

	/**
	 * Gets the y-axis view column.
	 *
	 * @return col the view column number
	 */
	protected int getYColumn() {
		if (getColumnCount() < 3) {
			return -1;
		}
		int labelCol = convertColumnIndexToView(0);
		return (labelCol < 2) ? 2 : 1;
	}

	/**
	 * Replaces points in a dataset.
	 *
	 * @param dataset the dataset
	 * @param rows    the rows to replace in ascending order
	 * @param vals    array of new y-values
	 * @return array of values replaced
	 */
	protected double[] replacePoints(Dataset dataset, int[] rows, double[] vals) {
		double[] replaced = new double[rows.length];
		DataColumn column = null;
		boolean shifted = false;
		if (dataset instanceof DataColumn) {
			column = (DataColumn) dataset;
			shifted = column.isShifted();
			column.setShifted(false);
		}
		// determine required row count
		int count = dataset.getIndex();
		for (int i = 0; i < rows.length; i++) {
			count = Math.max(count, rows[i] + 1);
		}
		// add rows if required
		for (int len = dataset.getIndex(); count > len; len = dataset.getIndex()) {
			int[] row = new int[] { len };
			insertRows(row, null);
		}
		double[] x = dataset.getXPoints();
		double[] y = dataset.getYPoints();
		for (int i = 0; i < rows.length; i++) {
			replaced[i] = y[rows[i]];
			y[rows[i]] = (vals == null) ? Double.NaN : vals[i];
		}
		dataset.clear();
		dataset.append(x, y);
		if (column != null) {
			column.setShifted(shifted);
		}
		dataToolTab.tabChanged(true);
		return replaced;
	}

	/**
	 * Inserts points into a dataset.
	 *
	 * @param dataset the dataset
	 * @param rows    the rows to insert in ascending order
	 * @param vals    the corresponding y-values to insert
	 * @return array of values inserted
	 */
	protected double[] insertPoints(Dataset dataset, int[] rows, double[] vals) {
		if (vals == null) {
			vals = new double[rows.length];
			for (int i = 0; i < vals.length; i++) {
				vals[i] = Double.NaN;
			}
		}
		if (dataset instanceof DataFunction) {
			return vals;
		}
		// insert values starting with lowest row
		double[] y = dataset.getYPoints();
		for (int i = 0; i < rows.length; i++) {
			int n = y.length;
			double[] newy = new double[n + 1];
			System.arraycopy(y, 0, newy, 0, rows[i]);
			System.arraycopy(y, rows[i], newy, rows[i] + 1, n - rows[i]);
			newy[rows[i]] = vals[i];
			y = newy;
		}
		double[] x = DataTool.getRowArray(y.length);
		dataset.clear();
		dataset.append(x, y);
		dataToolTab.tabChanged(true);
		return vals;
	}

	/**
	 * Deletes points from a dataset.
	 *
	 * @param dataset the dataset
	 * @param rows    the rows to remove in ascending order
	 * @return the removed y-values
	 */
	protected double[] deletePoints(Dataset dataset, int[] rows) {
		double[] removed = new double[rows.length];
		if (dataset instanceof DataFunction) {
			return removed;
		}
		// remove y-values starting with highest row
		double[] y = dataset.getYPoints();
		for (int i = rows.length - 1; i > -1; i--) {
			int n = y.length;
			double[] newy = new double[n - 1];
			System.arraycopy(y, rows[i], removed, i, 1);
			if (rows[i] > 0) {
				System.arraycopy(y, 0, newy, 0, rows[i]);
			}
			if (rows[i] < n - 1) {
				System.arraycopy(y, rows[i] + 1, newy, rows[i], n - rows[i] - 1);
			}
			y = newy;
		}
		double[] x = DataTool.getRowArray(y.length);
		dataset.clear();
		dataset.append(x, y);
		dataToolTab.tabChanged(true);
		return removed;
	}

	/**
	 * Trims empty rows from bottom of table up to a specified minimum.
	 *
	 * @param minSize the minimum row count to keep
	 */
	protected void trimEmptyRows(int minSize) {
		clearSelection();
		int endRow = getRowCount() - 1;
		boolean empty = true;
		int[] rows = new int[1];
		while (empty && (endRow > minSize)) {
			empty = isEmptyRow(endRow);
			if (empty) {
				rows[0] = endRow;
				deleteRows(rows);
				endRow--;
			}
		}
		if (getSelectedRows().length == 0) {
			removeColumnSelectionInterval(0, getColumnCount() - 1);
		}
	}

	/**
	 * Clears the selection if it consists of only an empty end row.
	 */
	protected void clearSelectionIfEmptyEndRow() {
		int n = getRowCount();
		if (n < 2) {
			return;
		}
		int[] selectedRows = getSelectedRows();
		if ((selectedRows.length == 1) && (selectedRows[0] == n - 1) && isEmptyRow(n - 1)) {
			clearSelection();
		}
	}

	/**
	 * Displays the data builder.
	 */
	protected void showDataBuilder() {
		FunctionTool tool = dataToolTab.getDataBuilder();
		tool.setSelectedPanel(dataToolTab.getName());
		tool.setVisible(true);
	}

	/**
	 * Renames a column.
	 *
	 * @param oldName the old name
	 * @param newName the new name
	 */
	protected void renameColumn(String oldName, String newName) {
		int index = dataManager.getDatasetIndex(oldName);
		Dataset data = dataManager.getDataset(index);
		data.setXYColumnNames(data.getXColumnName(), newName);
		refreshDataFunctions();
		dataToolTab.columnNameChanged(oldName, newName);
		refreshTable(DataTable.MODE_HEADER);
		refreshUndoItems();
	}

	/**
	 * Refreshes the undo and redo menu items.
	 */
	protected void refreshUndoItems() {
		if (dataToolTab != null) {
			dataToolTab.refreshUndoItems();
		}
	}

	/**
	 * Refreshes the data functions.
	 */
	public void refreshDataFunctions() {
		java.util.Iterator<Dataset> it = dataManager.getDatasets().iterator();
		while (it.hasNext()) {
			Dataset next = it.next();
			if (next instanceof DataFunction) {
				((DataFunction) next).refreshFunctionData();
			}
		}
	}

	/**
	 * Selects all cells in the table.
	 */
	public void selectAllCells() {
		selectAll();
		requestFocusInWindow();
	}

	boolean clearing = false;
	/**
	 * Deselects all selected columns and rows. Overrides JTable method.
	 */
	@Override
	public void clearSelection() {
		if (clearing) {
			return;
		}
		clearing = true;
		if (workingData != null) {
			workingData.clearHighlights();
		}
		if (selectedData != null) {
			selectedData.clearHighlights();
		}
		super.clearSelection();
//		// select only the focus cell so it has focus
//		if ((focusRow > -1) && (focusRow < getRowCount()) && (focusCol > 0) && (focusCol < getColumnCount())) {
//			setRowSelectionInterval(focusRow, focusRow);
//			setColumnSelectionInterval(focusCol, focusCol);
//		}
		leadCol = 0;
//		leadRow = 0;
		repaint();
		clearing = false;
		
	}

	/**
	 * Refreshes the data in the table. Overrides DataTable method.
	 */
	@Override
	public void refreshTable(int mode) {
//		if (mode == DataTable.MODE_SET_TAINTED) {
//			super.refreshTable(mode);
//			updateColumnModel(null);
//			return;
//		}
		boolean noView = convertColumnIndexToView(0) == -1;
		if (noView) {
			updateColumnModel(null);
			return;
		}
		//		// save model column order
//		int[] modelColumns = getModelColumnOrder();
		// save selected rows and columns
		int[] rows = getSelectedModelRows();
		ArrayList<String> cols = getSelectedColumnNames();
		updateColumnModel(null);
	// restore column order--but keep "tabChanged" unchanged
		boolean changed = dataToolTab.tabChanged;
//		setModelColumnOrder(modelColumns);
		dataToolTab.tabChanged(changed);
		// re-sort to restore row order
		sort(dataTableModel.getSortedColumn());
		// restore selected rows and columns
		// important to select columns first!
		if (!cols.isEmpty()) {
			setSelectedColumnNames(cols);
		}
		if (rows.length > 0) {
			selectModelRows(rows);
		}
	}

	@Override
	public NumberFormatDialog getFormatDialog(String[] names, String[] selected) {
		for (int i = 0; i < names.length; i++) {
			if (names[i].endsWith(DataToolTab.SHIFTED)) {
				names[i] = names[i].substring(0, names[i].length() - DataToolTab.SHIFTED.length());
			}
		}
		return super.getFormatDialog(names, selected);
	}

	/**
	 * Gets the names of columns with visible markers.
	 *
	 * @return array of column names
	 */
	public String[] getHiddenMarkers() {
		ArrayList<String> list = new ArrayList<String>();
		for (int i = 0; i < getColumnCount(); i++) {
			String name = getColumnName(i);
			WorkingDataset next = getWorkingData(name);
			if ((next != null) && !next.isMarkersVisible()) {
				list.add(name);
			}
		}
		return list.toArray(new String[list.size()]);
	}

	/**
	 * Hides markers of named columns.
	 *
	 * @param hiddenColumns names of columns with hidden markers
	 */
	public void hideMarkers(String[] hiddenColumns) {
		if (hiddenColumns == null) {
			return;
		}
		for (int i = 0; i < hiddenColumns.length; i++) {
			String name = hiddenColumns[i];
			WorkingDataset next = getWorkingData(name);
			if (next != null) {
				next.setMarkersVisible(false);
			}
		}
	}

	/**
	 * Sets the working columns by name.
	 *
	 * @param xColName the name of the horizontal axis variable
	 * @param yColName the name of the vertical axis variable
	 */
	public void setWorkingColumns(String xColName, String yColName) {
		// move labels to column 0
		int labelCol = convertColumnIndexToView(0);
		getColumnModel().moveColumn(labelCol, 0);
		// find xCol and move to column 1
		TableModel model = getModel();
		for (int i = 1; i < model.getColumnCount(); i++) {
			if (xColName.equals(getColumnName(i))) {
				getColumnModel().moveColumn(i, 1);
				break;
			}
		}
		// find y and move to column 2
		for (int i = 2; i < model.getColumnCount(); i++) {
			if (yColName.equals(getColumnName(i))) {
				getColumnModel().moveColumn(i, 2);
				break;
			}
		}
	}

	@Override
	public void setFont(Font font) {
		super.setFont(font);
		if (labelRenderer != null) {
			Font labelFont = labelRenderer.getFont();
			labelFont = labelFont.deriveFont(font.getSize2D());
			labelRenderer.setFont(labelFont);
			headerRenderer.headerFont = labelFont;
			rowNumberRenderer.setFont(labelFont);
		}
		setRowHeight(font.getSize() + 4);
	}

	/**
	 * Overrides DataTable getCellRenderer() method.
	 *
	 * @param row the row number
	 * @param col the column number
	 * @return the cell editor
	 */
	@Override
	public TableCellRenderer getCellRenderer(int row, int col) {
		TableCellRenderer renderer = super.getCellRenderer(row, col);
		if (renderer == rowNumberRenderer) {
			return labelRenderer;
		}
		dataRenderer.renderer = renderer;
		return dataRenderer;
	}

	/**
	 * Returns the editor for a cell specified by row and column.
	 *
	 * @param row the row number
	 * @param col the column number
	 * @return the cell editor
	 */
	@Override
	public TableCellEditor getCellEditor(int row, int col) {
		editor.setColumn(col);
		return editor;
	}


	/**
	 * A header cell renderer that identifies sorted and selected columns.
	 */
	class HeaderRenderer implements TableCellRenderer {
		TableCellRenderer renderer;
		Font headerFont;
		DrawingPanel panel = new DrawingPanel();
		DrawableTextLine textLine = new DrawableTextLine("", 0, -6); //$NON-NLS-1$

		/**
		 * Constructor HeaderRenderer
		 * 
		 * @param renderer
		 */
		public HeaderRenderer(TableCellRenderer renderer) {
			this.renderer = renderer;
			textLine.setJustification(TextLine.CENTER);
			panel.addDrawable(textLine);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int col) {
			// value is data column name
			String realname = (value == null) ? "" : value.toString(); //$NON-NLS-1$
			String name = realname;
			if (OSPRuntime.isMac()) {
				name = TeXParser.removeSubscripting(name);
			}
			Component c = renderer.getTableCellRendererComponent(table, name, isSelected, hasFocus, row, col);
			if (headerFont == null)
				headerFont = c.getFont();
			int labelCol = convertColumnIndexToView(0);
			int xCol = (labelCol == 0) ? 1 : 0;
			int yCol = (labelCol < 2) ? 2 : 1;
			if (unselectedBG == null) {
				unselectedBG = c.getBackground();
			}
			// backup color in case c has none
			if (unselectedBG == null) {
				unselectedBG = javax.swing.UIManager.getColor("Panel.background"); //$NON-NLS-1$
			}
			rowBG = dataToolTab.plot.getBackground();
			Color bgColor = (col == xCol) ? DataToolTable.xAxisColor : (col == yCol) ? DataToolTable.yAxisColor : rowBG;
			if (!(c instanceof JComponent)) {
				return c;
			}
			JComponent comp = (JComponent) c;

			java.awt.Dimension dim = comp.getPreferredSize();
			dim.height += 1;
			dim.height = Math.max(getRowHeight() + 2, dim.height);
			panel.setPreferredSize(dim);
			javax.swing.border.Border border = comp.getBorder();
			if (border instanceof javax.swing.border.EmptyBorder) {
				border = BorderFactory.createLineBorder(Color.LIGHT_GRAY);
			}
			panel.setBorder(border);

			// determine font: italics if undeletable, bold if sorted column
			Font font;
			Dataset data = getDataset(realname);
			if (!dataToolTab.isDeletable(data)) {
				font = dataTableModel.getSortedColumn() != convertColumnIndexToModel(col)
						? headerFont.deriveFont(Font.PLAIN + Font.ITALIC)
						: headerFont.deriveFont(Font.BOLD + Font.ITALIC);
			} else {
				font = dataTableModel.getSortedColumn() != convertColumnIndexToModel(col) ? headerFont.deriveFont(Font.PLAIN)
						: headerFont.deriveFont(Font.BOLD);
			}
			int[] cols = getSelectedColumns();
			boolean selected = false;
			for (int i = 0; i < cols.length; i++) {
				selected = selected || (cols[i] == col);
			}
			selected = selected && (convertColumnIndexToModel(col) > 0);
			bgColor = selected ? selectedHeaderBG : bgColor;

			// special case: textline doesn't work on OSX
			if (OSPRuntime.isMac()) {
				comp.setFont(font);
				comp.setBackground(bgColor);
				comp.setForeground(selected ? selectedHeaderFG : comp.getForeground());
				if (comp instanceof JLabel) {
					((JLabel) comp).setHorizontalAlignment(SwingConstants.CENTER);
				}
				return comp;
			}

			textLine.setText(name);
			textLine.setFont(font);
			textLine.setColor(selected ? selectedHeaderFG : comp.getForeground());
			textLine.setBackground(bgColor);
			panel.setBackground(bgColor);
			return panel;
		}

	}

	/**
	 * A dataset whose y values and display properties depend on a source dataset.
	 */
	class WorkingDataset extends HighlightableDataset {
		final private Dataset yData;
		private Dataset xData;
		boolean markersVisible;
		int markerType;
		boolean isWorkingYColumn;

		/**
		 * Constructor WorkingDataset
		 * 
		 * @param yDataset
		 */
		public WorkingDataset(Dataset yDataset) {
			yData = yDataset;
			setColor(yData.getFillColor(), yData.getLineColor());
			markerType = yData.getMarkerShape();
			setMarkerShape(markerType);
			markersVisible = (markerType != Dataset.NO_MARKER);
			if (markerType == Dataset.NO_MARKER) {
				markerType = Dataset.CIRCLE;
			}
			setMarkerSize(yData.getMarkerSize());
			setConnected(yData.isConnected());
		}

		@Override
		public void draw(DrawingPanel drawingPanel, java.awt.Graphics g) {
			if (isWorkingYColumn) {
				drawSurrounds(drawingPanel, (Graphics2D) g);
			}
			boolean vis = markersVisible;
			if (isWorkingYColumn && !vis) {
				setMarkersVisible(true);
			}
			super.draw(drawingPanel, g);
			if (isWorkingYColumn && !vis) {
				setMarkersVisible(false);
			}
		}

		/**
		 * Draw green shapes surrounding the working data points.
		 *
		 * @param drawingPanel
		 * @param g2
		 */
		protected void drawSurrounds(DrawingPanel drawingPanel, Graphics2D g2) {
			// set up graphics
			Color c = g2.getColor();
			Stroke s = g2.getStroke();
			g2.setColor(new Color(51, 255, 51, 153));
			g2.setStroke(new BasicStroke(2));

			// set up shape size
			Shape shape = null;
			int radius = getMarkerSize() + 2;
			int marker = getMarkerShape();
			if (marker == NO_MARKER || marker == PIXEL) {
				radius = 3;
			}
			int size = radius * 2 + 1;

			// get data points
			double[] xpoints = getXPointsRaw();
			double[] ypoints = getYPointsRaw();

			// draw surrounds
			for (int i = 0; i < index; i++) {
				double yp = ypoints[i];
				if (Double.isNaN(yp)) {
					continue;
				}
				double xp = xpoints[i];
				if (drawingPanel.isLogScaleX() && (xp <= 0)) {
					continue;
				}
				if (drawingPanel.isLogScaleY() && (yp <= 0)) {
					continue;
				}
				xp = drawingPanel.xToPix(xp);
				yp = drawingPanel.yToPix(yp);
				if (marker == SQUARE || marker == POST || marker == BAR) {
					shape = new Rectangle2D.Double(xp - radius, yp - radius, size, size);
				} else
					shape = new Ellipse2D.Double(xp - radius, yp - radius, size, size);
				g2.draw(shape);
			}

			// restore graphics
			g2.setColor(c);
			g2.setStroke(s);
		}

		public boolean isMarkersVisible() {
			return markersVisible || isWorkingYColumn;
		}

		public void setMarkersVisible(boolean visible) {
			if (!visible && markersVisible) {
				markerType = getMarkerShape();
				setMarkerShape(Dataset.NO_MARKER);
			} else if (visible) {
				setMarkerShape(markerType);
			}
			markersVisible = visible;
		}

		public void setColor(Color edgeColor, Color lineColor) {
			Color fill = new Color(edgeColor.getRed(), edgeColor.getGreen(), edgeColor.getBlue(), 100);
			setMarkerColor(fill, edgeColor);
			setLineColor(lineColor);
			yData.setMarkerColor(fill, edgeColor);
			yData.setLineColor(lineColor);
		}

		@Override
		public void setConnected(boolean connected) {
			super.setConnected(connected);
			yData.setConnected(connected);
		}

		@Override
		public void setMarkerSize(int size) {
			super.setMarkerSize(size);
			yData.setMarkerSize(size);
		}

		@Override
		public void setMarkerShape(int shape) {
			super.setMarkerShape(shape);
			if (shape != Dataset.NO_MARKER) {
				yData.setMarkerShape(shape);
				markerType = shape;
			}
		}

		Dataset getYSource() {
			return yData;
		}

		Dataset getXSource() {
			return xData;
		}

		void setXSource(Dataset xDataset) {
			xData = xDataset;
			clear();
			double[] x = xData.getYPoints();
			double[] y = yData.getYPoints();
			if (x.length != y.length) {
				int n = Math.min(x.length, y.length);
				double[] nx = new double[n];
				System.arraycopy(x, 0, nx, 0, n);
				double[] ny = new double[n];
				System.arraycopy(y, 0, ny, 0, n);
				append(nx, ny);
			} else {
				append(x, y);
			}
			setXYColumnNames(xData.getYColumnName(), yData.getYColumnName());
		}

	}

	/**
	 * A class to render data cells.
	 */
	class DataCellRenderer implements TableCellRenderer {
		TableCellRenderer renderer;
		boolean showFocus = false;
		Color unlockedBG = Color.WHITE, lockedBG = new Color(255, 220, 0, 30);

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int col) {
			int modelCol = convertColumnIndexToModel(col);
			if ((hasFocus || isSelected) && modelCol > 0) {
				focusRow = row;
				focusCol = col;
			}
			if (selectedBG == null) {
				Component c = renderer.getTableCellRendererComponent(table, value, true, false, row, col);
				selectedBG = c.getBackground();
				selectedFG = c.getForeground();
				selectedHeaderFG = selectedFG.darker();
				float[] hsb = Color.RGBtoHSB(selectedBG.getRed(), selectedBG.getGreen(), selectedBG.getBlue(), null);
				hsb[2] *= 0.85f;
				int darker = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
				selectedHeaderBG = new Color(darker);
			}
			if (!showFocus) {
				hasFocus = ((col == mouseCol) && (row == mouseRow));
			}
			Component c = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
			Dataset data = dataManager.getDataset(modelCol - 1);
			if (!isSelected) {
				c.setBackground(dataToolTab.isDeletable(data) ? unlockedBG : lockedBG);
			}
			return c;
		}

	}

	/**
	 * A class to render labels such as row number. Also used by DataToolPropsTable
	 * and DataToolStatsTable.
	 */
	class LabelRenderer extends JLabel implements TableCellRenderer {
		/**
		 * Constructor LabelRenderer
		 */
		public LabelRenderer() {
			setOpaque(true); // make background visible.
			setHorizontalAlignment(SwingConstants.RIGHT);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int col) {
			setText((value == null) ? null : value.toString());
			setEnabled(true);
			boolean selected = false;
			if (table == DataToolTable.this) {
				setEnabled((row < getRowCount() - 1) || !isEmptyRow(row));
				int[] rows = getSelectedRows();
				for (int i = 0; i < rows.length; i++) {
					selected = selected || (rows[i] == row);
				}
			}
//      setForeground(selected ? selectedFG : Color.black);
//      setBackground(selected ? selectedBG : unselectedBG);
			setForeground(selected ? selectedHeaderFG : Color.black);
			setBackground(selected ? selectedHeaderBG : unselectedBG);
			return this;
		}

	}

	/**
	 * A table model for this table.
	 */
	protected class DataToolTableModel extends DataTable.OSPDataTableModel {
		DataToolTab tab;

		DataToolTableModel() {
		//	haveColumnClasses = false;
		}
//
//		@Override
//		protected int getModelRow(int row) {
//			return super.getModelRow(row);
//		}
		
		@Override
		public synchronized int getColumnCount() {
			return (columnCount >= 0 ? columnCount
					: (columnCount = 1 + dataManager.getColumnCount()));
		}
		
		@Override
		public Class<?> getColumnClass(int col) {
			return (col == 0 ? Integer.class : Double.class);
		}

		/**
		 * @param row    - an actual table row, zero-based
		 * @param column - a logical column, zero-based, always the same number for a
		 *               column no matter how the columns are sorted.
		 */
		@Override
		public Object getValueAt(int row, int column) {
			return (column >= getColumnCount() ? null 
					: column == 0 ? Integer.valueOf(getModelRow(row))
							: super.getValueAt(row,  column));
		}

		@Override
		public String getColumnName(int col) {
			if (col >= getColumnCount()) {
				return "unknown";
			}
			if (col == 0) {
				return rowName;
			}
			String name = tab.dataManager.getColumnName(col - 1);
			if (tab.originShiftEnabled && tab.plot != null) {
				name = name + DataToolTab.SHIFTED;
			}
			return name;
		}

//    @Override
//    public Object getValueAt(int row, int col) {
//    	if (tab.offsetOriginVisible && col>0) {
//		    double val = (Double)super.getValueAt(row, col);		    	
// 		    DataColumn dataCol = (DataColumn)getDataset(getColumnName(col));
//    		if (dataCol!=null) {
//    			val -= dataCol.getShift();
//    		}
//    		return val;
//    	}
//    	return super.getValueAt(row, col);
//    }
//
		@Override
		public void setValueAt(Object value, int row, int col) {
			if (value == null) {
				return;
			}
			Dataset data = dataManager.getDataset(col - 1);
			double[] y = data.getYPoints();
			double val = Double.NaN;
			try {
				val = Double.parseDouble(value.toString());
				if (y[row] == val) {
					return; // no change
				}
			} catch (NumberFormatException e) {
				if (Double.isNaN(y[row])) {
					return; // no change
				}
			}
			String name = data.getYColumnName();
			int[] rows = new int[] { row };
			HashMap<String, double[]> map = new HashMap<String, double[]>();
			map.put(name, new double[] { val });
			HashMap<String, double[]> old = replaceCells(rows, map);
			// post edit: target is rows, value is HashMap[] {undo, redo}
			TableEdit edit = new TableEdit(REPLACE_CELLS_EDIT, name, rows, new HashMap[] { old, map });
			tab.undoSupport.postEdit(edit);
		}

		@Override
		public boolean isElementEditable(int row, int col) {
			return (col > 0 && tab.isUserEditable());
		}

	}

	/**
	 * A cell editor for this table.
	 */
	class DataEditor extends AbstractCellEditor implements TableCellEditor {
		JTextField field = new JTextField();
		int column;
		boolean isFunction;

		// Constructor.
		DataEditor() {
			field.setHorizontalAlignment(SwingConstants.RIGHT);
			field.setBorder(BorderFactory.createEmptyBorder(0, 1, 1, 0));
			field.setSelectionColor(new Color(204, 255, 255));
			field.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(final KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						stopCellEditing();
						Runnable runner = new Runnable() {
							@Override
							public synchronized void run() {
								int row = getModelRow(focusRow) + 1;
								// add empty row if needed
								if (row == getRowCount()) {
									insertRows(new int[] { row }, null);
								}
								row = getViewRow(row);
								changeSelection(row, column, false, false);
								editCellAt(row, column, e);
								field.requestFocus();
							}

						};
						SwingUtilities.invokeLater(runner);
					} else if (field.isEnabled()) {
						field.setBackground(Color.yellow);
					}
				}

			});
			field.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					if (field.getBackground() != Color.white) {
						stopCellEditing();
					}
				}

				@Override
				public void focusGained(FocusEvent e) {
					field.selectAll();
				}

			});
			field.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					// right-click: pass e to the table mouse listener
					if (OSPRuntime.isPopupTrigger(e)) {
						stopCellEditing();
						tableMouseListener.mousePressed(e);
					}
				}

			});
		}

		void setColumn(int col) {
			column = col;
			int modelCol = convertColumnIndexToModel(col);
			Dataset data = dataManager.getDataset(modelCol - 1);
			isFunction = (data instanceof DataFunction);
		}

		// Gets the component to be displayed while editing.
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
			// if editing a function, simply show function builder
			if (isFunction) {
				showDataBuilder();
				return null;
			}
			field.setText((value == null) ? "" : String.valueOf(value)); //$NON-NLS-1$
			field.setFont(DataToolTable.this.getFont());
			return field;
		}

		// Determines when editing starts.
		@Override
		public boolean isCellEditable(EventObject e) {
			if ((e instanceof MouseEvent) && ((MouseEvent) e).getClickCount() == 2) {
				return true;
			}
			if (e instanceof ActionEvent) {
				return true;
			}
			if (e instanceof KeyEvent) {
				return true;
			}
			return false;
		}

		// Called when editing is completed.
		@Override
		public Object getCellEditorValue() {
			DataToolTable.this.requestFocusInWindow();
			field.setBackground(Color.white);
			return field.getText();
		}

	}

	/**
	 * A class to undo/redo datatable edits.
	 */
	protected class TableEdit extends AbstractUndoableEdit {
		Object target, value;
		int editType;
		String columnName;
		HashMap<String, double[]> map;

		/**
		 * Contructor.
		 *
		 * @param type    may be RENAME_COLUMN_EDIT, DELETE_COLUMN_EDIT, ADD_CELLS_EDIT,
		 *                DELETE_CELLS_EDIT, ADD_ROWS_EDIT, DELETE_ROWS_EDIT, VALUE_EDIT
		 * @param colName the column name
		 * @param target  the target rows or column
		 * @param value   the value
		 */
		public TableEdit(int type, String colName, Object target, Object value) {
			editType = type;
			columnName = colName;
			this.target = target;
			this.value = value;
			String name = (colName == null) ? null : ": column \"" + colName + "\""; //$NON-NLS-1$ //$NON-NLS-2$
			OSPLog.finer(editTypes[type] + name);
		}

		// undoes the change
		@Override
		@SuppressWarnings("unchecked")
		public void undo() throws CannotUndoException {
			super.undo();
			OSPLog.finer("undoing " + editTypes[editType]); //$NON-NLS-1$
			switch (editType) {
			case RENAME_COLUMN_EDIT: {
				// columnName is new name, value is undo name
				// String newName = value.toString();
				renameColumn(columnName, value.toString());
				break;
			}
			case INSERT_COLUMN_EDIT: {
				// target is column, value is dataset
				deleteColumn(columnName);
				break;
			}
			case DELETE_COLUMN_EDIT: {
				// target is column, value is dataset
				Dataset data = (Dataset) value;
				int col = ((Integer) target).intValue();
				insertColumn(data, col);
				break;
			}
			case INSERT_CELLS_EDIT: {
				// target is rows, value is HashMap
				int[] rows = (int[]) target;
				HashMap<String, double[]> values = (HashMap<String, double[]>) value;
				deleteCells(rows, values);
				break;
			}
			case DELETE_CELLS_EDIT: {
				// target is rows, value is HashMap
				int[] rows = (int[]) target;
				HashMap<String, double[]> values = (HashMap<String, double[]>) value;
				insertCells(rows, values);
				break;
			}
			case INSERT_ROWS_EDIT: {
				// target is rows, value is HashMap
				int[] rows = (int[]) target;
				deleteRows(rows);
				break;
			}
			case DELETE_ROWS_EDIT: {
				// target is rows, value is HashMap
				int[] rows = (int[]) target;
				HashMap<String, double[]> values = (HashMap<String, double[]>) value;
				insertRows(rows, values);
				break;
			}
			case REPLACE_CELLS_EDIT: {
				// target is rows, value is HashMap[] {undo, redo}
				int[] rows = (int[]) target;
				@SuppressWarnings("rawtypes")
				HashMap[] values = (HashMap[]) value;
				replaceCells(rows, values[0]);
				break;
			}
			}
		}

		// redoes the change
		@Override
		@SuppressWarnings("unchecked")
		public void redo() throws CannotUndoException {
			super.redo();
			OSPLog.finer("redoing " + editTypes[editType]); //$NON-NLS-1$
			switch (editType) {
			case RENAME_COLUMN_EDIT: {
				// columnName is new name, value is undo name
				renameColumn(value.toString(), columnName);
				break;
			}
			case INSERT_COLUMN_EDIT: {
				// target is column, value is dataset
				Dataset data = (Dataset) value;
				int col = ((Integer) target).intValue();
				insertColumn(data, col);
				break;
			}
			case DELETE_COLUMN_EDIT: {
				// target is column, value is dataset
				deleteColumn(columnName);
				break;
			}
			case INSERT_CELLS_EDIT: {
				// target is rows, value is HashMap
				int[] rows = (int[]) target;
				HashMap<String, double[]> values = (HashMap<String, double[]>) value;
				insertCells(rows, values);
				break;
			}
			case DELETE_CELLS_EDIT: {
				// target is rows, value is HashMap
				int[] rows = (int[]) target;
				HashMap<String, double[]> values = (HashMap<String, double[]>) value;
				deleteCells(rows, values);
				break;
			}
			case INSERT_ROWS_EDIT: {
				// target is rows, value is HashMap
				int[] rows = (int[]) target;
				HashMap<String, double[]> values = (HashMap<String, double[]>) value;
				insertRows(rows, values);
				break;
			}
			case DELETE_ROWS_EDIT: {
				// target is rows, value is HashMap
				int[] rows = (int[]) target;
				deleteRows(rows);
				break;
			}
			case REPLACE_CELLS_EDIT: {
				// target is rows, value is HashMap[] {undo, redo}
				int[] rows = (int[]) target;
				@SuppressWarnings("rawtypes")
				HashMap[] values = (HashMap[]) value;
				replaceCells(rows, values[1]);
				break;
			}
			}
		}

		// returns the presentation name
		@Override
		public String getPresentationName() {
			return "Edit"; //$NON-NLS-1$
		}

	}

	public boolean isFitDrawable(KnownFunction fit) {
		int n = selectedData.getValidXPoints().length;
		int nparam = fit.getParameterCount();
		return (n >= nparam);
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
