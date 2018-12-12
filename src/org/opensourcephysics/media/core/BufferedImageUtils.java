package org.opensourcephysics.media.core;

import java.awt.image.BufferedImage;

public class BufferedImageUtils {
	public static BufferedImage convertIfNeeded(BufferedImage source, int type){
		if (source.getType() != type) {
			BufferedImage bi = new BufferedImage(source.getWidth(), source.getHeight(), type);
			bi.createGraphics().drawImage(source, 0, 0, null);
			return bi;
		}
		return source;
	}
}
