package org.opensourcephysics.js;

public class JSUtil {
	
	/**
	 * The HTML5 canvas delivers [r g b a r g b a ...] which is not a Java option.
	 * The closest Java option is TYPE_4BYTE_ABGR, but that is not quite what we
	 * need. SwingJS decodes TYPE_4BYTE_HTML5 as TYPE_4BYTE_RGBA"
	 * 
	 * ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
	 * 
	 * int[] nBits = { 8, 8, 8, 8 };
	 * 
	 * int[] bOffs = { 0, 1, 2, 3 };
	 * 
	 * colorModel = new ComponentColorModel(cs, nBits, true, false,
	 * Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
	 * 
	 * raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height,
	 * width * 4, 4, bOffs, null);
	 * 
	 * Note, however, that this buffer type should only be used for direct buffer access
	 * using
	 * 
	 * 
	 * 
	 */
	public static final int TYPE_4BYTE_HTML5 = -6;

	static {
		String[] corsEnabled = new String[] {
				 "www.physlets.org", 
// TODO				 "www.opensourcephysics.org",
// TODO				 "www.compadre.org" 
				 };
				
		for (int i = corsEnabled.length; --i >= 0;) {
			String uri = corsEnabled[i];
		/**
		 * @j2sNative J2S.addDirectDatabaseCall(uri);
		 */
		}
	}
	
	static public boolean isJS = /** @j2sNative true || */ false;

}
