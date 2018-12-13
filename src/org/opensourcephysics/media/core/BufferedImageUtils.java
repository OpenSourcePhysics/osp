package org.opensourcephysics.media.core;

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
}
