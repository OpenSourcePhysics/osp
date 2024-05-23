package org.opensourcephysics.display;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;

public class OSPButton extends JButton {
	
	private JComponent heightComponent;

	/**
	 * Constructs an OSPButton.
	 */
	public OSPButton() {
		setOpaque(false);
		setBorderPainted(false);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				setBorderPainted(true);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				setBorderPainted(false);
			}

		});
	}

	/**
	 * Constructs an OSPButton with an Action.
	 *
	 * @param action the Action
	 */
	public OSPButton(Action action) {
		this();
		setAction(action);
	}

	/**
	 * Constructs an OSPButton with an icon.
	 *
	 * @param icon the icon
	 */
	public OSPButton(Icon icon) {
		this();
		setIcon(icon);
	}

	/**
	 * Constructs an OSPButton with icons for selected and unselected states.
	 *
	 * @param off the unselected state icon
	 * @param on  the selected state icon
	 */
	public OSPButton(Icon off, Icon on) {
		this();
		setIcon(off);
		setSelectedIcon(on);
	}

	/**
	 * Sets the component that determines the height of this button.
	 *
	 * @param comp the height component. May be null.
	 */
	public void setHeightComponent(JComponent comp) {
		heightComponent = comp;
	}

	// override size methods so has same height as heightComponent, if any
	@Override
	public Dimension getPreferredSize() {
		Dimension dim = super.getPreferredSize();
		if (heightComponent != null)
			dim.height = heightComponent.getPreferredSize().height;
		return dim;
	}

	@Override
	public Dimension getMinimumSize() {
		Dimension dim = super.getMinimumSize();
		if (heightComponent != null)
			dim.height = heightComponent.getPreferredSize().height;
		return dim;
	}

	@Override
	public Dimension getMaximumSize() {
		Dimension dim = super.getMaximumSize();
		if (heightComponent != null)
			dim.height = heightComponent.getPreferredSize().height;
		return dim;
	}

}
