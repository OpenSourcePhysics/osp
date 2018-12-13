package org.opensourcephysics.media.core;

import java.awt.*;
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
}
