/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.controls;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * This modal dialog lets the user choose any number of items
 * from a supplied list. Items not selected are removed from the list.
 */
public class ListChooser extends JDialog {
  // instance fields
  private JPanel checkPane = new JPanel();
  private Object[] objects;
  private boolean[] selections;
  private JCheckBox[] checkBoxes;
  private JLabel instructions;
  private boolean applyChanges = false;

  /**
   * Constructs a dialog with the specified title and text.
   *
   * @param title the title of the dialog
   * @param text the label text
   */
  public ListChooser(String title, String text) {
    this(title, text, null);
  }

  /**
   * Constructs a dialog with the specified title, text and owner.
   *
   * @param title the title of the dialog
   * @param text the label text
   * @param owner the component that owns the dialog (may be null)
   */
  public ListChooser(String title, String text, Component owner) {
    super(JOptionPane.getFrameForComponent(owner), true);
    setTitle(title);
    instructions = new JLabel(" "+text);  //$NON-NLS-1$
    instructions.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
    createGUI();
  }

  /**
   * Constructs a dialog with the specified title, text and owner.
   *
   * @param title the title of the dialog
   * @param text the label text
   * @param owner the component that owns the dialog (may be null)
   */
  public ListChooser(String title, String text, JDialog owner) {
    super(owner, true);
    setTitle(title);
    instructions = new JLabel(" "+text); //$NON-NLS-1$
    instructions.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
    createGUI();
  }

  /**
   * Constructs a dialog with the specified title, text and owner.
   *
   * @param title the title of the dialog
   * @param text the label text
   * @param owner the component that owns the dialog (may be null)
   */
  public void createGUI() {
    // create the buttons
    JButton cancelButton = new JButton(ControlsRes.getString("Chooser.Button.Cancel"));       //$NON-NLS-1$
    JButton okButton = new JButton(ControlsRes.getString("Chooser.Button.OK"));               //$NON-NLS-1$
    JButton selectAllButton = new JButton(ControlsRes.getString("Chooser.Button.SelectAll")); //$NON-NLS-1$
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setVisible(false);
      }

    });
    okButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        for(int i = 0; i<checkBoxes.length; i++) {
          selections[i] = checkBoxes[i].isSelected();
        }
        applyChanges = true;
        setVisible(false);
      }

    });
    selectAllButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        for(int i = 0; i<checkBoxes.length; i++) {
          checkBoxes[i].setSelected(true);
        }
      }

    });
    getRootPane().setDefaultButton(okButton);
    // lay out the header pane
    JPanel headerPane = new JPanel();
    headerPane.setLayout(new BoxLayout(headerPane, BoxLayout.X_AXIS));
    headerPane.add(instructions);
    headerPane.add(Box.createHorizontalGlue());
    headerPane.add(selectAllButton);
    headerPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
    // lay out the scroll pane
    checkPane.setLayout(new BoxLayout(checkPane, BoxLayout.Y_AXIS));
    checkPane.setBackground(Color.white);
    JScrollPane scroller = new JScrollPane(checkPane);
    scroller.setPreferredSize(new Dimension(250, 180));
    JPanel scrollPane = new JPanel(new BorderLayout());
    scrollPane.add(scroller, BorderLayout.CENTER);
    scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    // lay out the button pane
    JPanel buttonPane = new JPanel();
    buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
    buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
    buttonPane.add(Box.createHorizontalGlue());
    buttonPane.add(okButton);
    buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
    buttonPane.add(cancelButton);
    // add everything to the content pane
    Container contentPane = getContentPane();
    contentPane.add(headerPane, BorderLayout.NORTH);
    contentPane.add(scrollPane, BorderLayout.CENTER);
    contentPane.add(buttonPane, BorderLayout.SOUTH);
    pack();
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    Rectangle rect = new Rectangle(dim);
    if (getOwner()!=null) {
	    rect = getOwner().getBounds();
    }
    int x = rect.x+(rect.width-this.getBounds().width)/2;
    int y = rect.y+(rect.height-this.getBounds().height)/2;
    setLocation(x, y);    	
  }

  /**
   * Allows the user to choose from the supplied list. Items not selected are
   * removed from the list.
   *
   * @param choices a collection of objects to choose from
   * @param names an optional collection of descriptive names
   * @return <code>true</code> if OK button was clicked
   */
  public boolean choose(Collection<?> choices, Collection<String> names) {
    return choose(choices, names, null);
  }

  /**
   * Allows the user to choose from the supplied list. Items not selected are
   * removed from the list.
   *
   * @param choices a collection of objects to choose from
   * @param names an optional collection of descriptive names
   * @param values an optional collection of values
   * @return <code>true</code> if OK button was clicked
   */
  public boolean choose(Collection<?> choices, Collection<String> names, Collection<?> values) {
    boolean[] selected = new boolean[choices.size()]; // all false by default
    return choose(choices, names, values, selected);
  }

  /**
   * Allows the user to choose from the supplied list. Items not selected are
   * removed from the list.
   *
   * @param choices a collection of objects to choose from
   * @param names an optional collection of descriptive names
   * @param values an optional collection of values
   * @param selected an array of initially selected states
   * @return <code>true</code> if OK button was clicked
   */
  public boolean choose(Collection<?> choices, Collection<String> names, Collection<?> values, boolean[] selected) {
    boolean[] disabled = new boolean[choices.size()]; // all false by default
    return choose(choices, names, values, selected, disabled);
  }

  /**
   * Allows the user to choose from the supplied list. Items not selected are
   * removed from the list.
   *
   * @param choices a collection of objects to choose from
   * @param names an optional collection of descriptive names
   * @param values an optional collection of values
   * @param selected an array of initially selected states
   * @param disabled an array of disabled states (true = disabled)
   * @return <code>true</code> if OK button was clicked
   */
  public boolean choose(Collection<?> choices, Collection<String> names, Collection<?> values, boolean[] selected, boolean[] disabled) {
    checkPane.removeAll();
    checkBoxes = new JCheckBox[choices.size()];
    selections = new boolean[choices.size()];
    objects = new Object[choices.size()];
    ArrayList<String> nameList = new ArrayList<String>();
    if(names!=null) {
      nameList.addAll(names);
    }
    ArrayList<Object> valueList = new ArrayList<Object>();
    if(values!=null) {
      valueList.addAll(values);
    }
    Iterator<?> it = choices.iterator();
    int i = 0;
    while(it.hasNext()) {
      objects[i] = it.next();
      selections[i] = false;
      if((nameList.size()<=i)||(nameList.get(i)==null)) {
        checkBoxes[i] = new JCheckBox(objects[i].toString());
      } else {
        String text = nameList.get(i);
        if((valueList.size()>i)&&(valueList.get(i)!=null)) {
          text += ":  "+valueList.get(i); //$NON-NLS-1$
        }
        checkBoxes[i] = new JCheckBox(text);
      }
      checkBoxes[i].setSelected(selected[i]);
      checkBoxes[i].setEnabled(!disabled[i]);
      checkBoxes[i].setBackground(Color.white);
      checkPane.add(checkBoxes[i]);
      i++;
    }
    setVisible(true);
    if(!applyChanges) {
      return false;
    }
    for(i = 0; i<objects.length; i++) {
      if(!selections[i]) {
        choices.remove(objects[i]);
      }
    }
    return true;
  }

}

/*
 * Open Source Physics software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.

 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be released
 * under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2007  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
