package org.opensourcephysics.media.core;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class BufferedImageUtils {
	/**
	 * If source image has the desired type, does nothing and returns source.
	 * Othrwise, creates a copy of source imagewith desired type and returns the copy.
	 * @param source Image to be converted
	 * @param type Type to convert to
	 * @return Original or converted image
	 */
	public static BufferedImage convertIfNeeded(BufferedImage source, int type){
		if (source.getType() != type) {
			BufferedImage bi = new BufferedImage(source.getWidth(), source.getHeight(), type);
			bi.createGraphics().drawImage(source, 0, 0, null);
			return bi;
		}
		return source;
	}

	/**
	 * Get the average RGB color of given rectangle rect of image.
	 * @param image The image to be processed.
	 * @param rect The rectangle to take pixels from. It is truncated if too big.
	 * @return Array of average values R, G, B
	 */
	public static double[] getAverageColorInRect(BufferedImage image, Rectangle rect){
		int x = Math.min(image.getWidth()  - 1, rect.x);
		int y = Math.min(image.getHeight() - 1, rect.y);
		int width  = Math.min(image.getWidth()  - rect.x, rect.width );
		int height = Math.min(image.getHeight() - rect.y, rect.height);
		int[] rgbData = image.getRGB(
				x, y,
				width, height,
				null,
				0,
				width
		);
		long sumR = 0, sumG = 0, sumB = 0;
		for(int i = 0; i < rgbData.length; i++){
			sumB += rgbData[i] & 0xff;
			sumG += (rgbData[i] >>  8) & 0xff;
			sumR += (rgbData[i] >> 16) & 0xff;
		}
		return new double[]{
				sumR / (double)(width*height),
				sumG / (double)(width*height),
				sumB / (double)(width*height),
		};
	}

	/**
	 * Shifts the color of each pixel of existing image
	 * with the given RGB values.
	 * Tries to preserve alpha value.
	 * @param image The image to be edited
	 * @param shift Array of three elements: R, G and B values of the shift. May be negative.
	 */
	public static void shiftColors(BufferedImage image, int[] shift){
		int[] rgbData = image.getRGB(
				0, 0,
				image.getWidth(), image.getHeight(),
				null,
				0,
				image.getWidth()
		);
		for(int i = 0; i < rgbData.length; i++){
			int pixel = rgbData[i];
			int r = Math.max(Math.min(((pixel >>> 16) & 0xff) + shift[0], 255),0);
			int g = Math.max(Math.min(((pixel >>>  8) & 0xff) + shift[1], 255),0);
			int b = Math.max(Math.min(((pixel       ) & 0xff) + shift[2], 255),0);
			int a = pixel & 0xff000000;
			rgbData[i] = a | (r << 16) | (g << 8) | b;

		}
		image.setRGB(
				0, 0,
				image.getWidth(), image.getHeight(),
				rgbData,
				0,
				image.getWidth()
		);
	}

	/**
	 * https://stackoverflow.com/questions/3514158/how-do-you-clone-a-bufferedimage
	 * @param source Image to be copied
	 * @return Copy of the image
	 */
	public static BufferedImage copyImage(BufferedImage source){
		BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
		Graphics g = b.createGraphics();
		g.drawImage(source, 0, 0, null);
		g.dispose();
		return b;
	}

	/**
	 * Modifies the rectangle rect to be into image.
	 * Preferres to move, if it is impossible - shrinks.
	 * @param searchRect
	 * @param image
	 * @return true if the rectangle has been changed
	 */

	public static boolean moveRectIntoImage(Rectangle2D searchRect, BufferedImage image) {
		int w = image.getWidth();
		int h = image.getHeight();
		Point2D corner = new Point2D.Double(searchRect.getX(), searchRect.getY());
		Dimension dim = new Dimension((int) searchRect.getWidth(), (int) searchRect.getHeight());

		boolean changed = false;
		// reduce size if needed
		if (w < dim.width || h < dim.height) {
			changed = true;
			dim.setSize(Math.min(w, dim.width), Math.min(h, dim.height));
			searchRect.setFrame(corner, dim);
		}

		// move corner point if needed
		double x = Math.max(0, corner.getX());
		x = Math.min(x, w - dim.width);
		double y = Math.max(0, corner.getY());
		y = Math.min(y, h - dim.height);
		if (x != corner.getX() || y != corner.getY()) {
			changed = true;
			corner.setLocation(x, y);
			searchRect.setFrame(corner, dim);
		}

		return changed;
	}

	public static BufferedImage createMagnifiedImage(BufferedImage source, int magFactor, int type) {
		BufferedImage image = new BufferedImage(
				magFactor*source.getWidth(),
				magFactor*source.getHeight(),
				type);
		image.createGraphics().drawImage(source, 0, 0, image.getWidth(), image.getHeight(), null);
		return image;
	}

}
