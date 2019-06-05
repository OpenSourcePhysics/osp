package org.opensourcephysics.display;

import javax.swing.*;
import java.awt.*;

/**
 * A DrawingPanel that mimics the look of a JLabel but can display subscripts.
 */
public class TextLineLabel extends DrawingPanel {
	DrawableTextLine textLine;
	JLabel label;
	int w;

	/**
	 * Constructor
	 */
	public TextLineLabel() {
		textLine = new DrawableTextLine("", 0, -4.5); //$NON-NLS-1$
		textLine.setJustification(TextLine.CENTER);
		addDrawable(textLine);
		label = new JLabel();
		textLine.setFont(label.getFont());
		textLine.setColor(label.getForeground());
	}

	/**
	 * Constructor with initial text
	 */
	public TextLineLabel(String text) {
		this();
		setText(text);
	}

	/**
	 * Sets the text to be displayed. Accepts subscript notation eg v_{x}.
	 *
	 * @param text the text
	 */
	public void setText(String text) {
		if (text == null) text = ""; //$NON-NLS-1$
		if (text.equals(textLine.getText())) return;
		w = -1;
		textLine.setText(text);
		if (text.contains("_{")) { //$NON-NLS-1$
			text = TeXParser.removeSubscripting(text);
		}
		// use label to set initial preferred size
		label.setText(text);
		java.awt.Dimension dim = label.getPreferredSize();
		dim.width += 4;
		setPreferredSize(dim);
	}

	@Override
	public Font getFont() {
		if (textLine != null) return textLine.getFont();
		return super.getFont();
	}

	@Override
	public void setFont(Font font) {
		if (textLine != null) {
			textLine.setFont(font);
			w = -1;
		} else super.setFont(font);
	}

	@Override
	public void paintComponent(Graphics g) {
		setPixelScale(); // sets the pixel scale and the world-to-pixel AffineTransform
		((Graphics2D) g).setRenderingHint(
				RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		textLine.draw(this, g);
		if (w == -1) {
			// check preferred size and adjust if needed
			w = textLine.getWidth(g);
			Dimension dim = getPreferredSize();
			if (dim.width > w + 4 || dim.width < w + 4) {
				dim.width = w + 4;
				setPreferredSize(dim);
				processParent(getParent());
			}
		}
	}

	public void processParent(Container c){
		/*To be overridden*/
	}

}
