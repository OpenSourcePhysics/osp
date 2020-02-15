package org.opensourcephysics.js;

public class JSUtil {
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
