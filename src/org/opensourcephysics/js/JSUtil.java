package org.opensourcephysics.js;

<<<<<<< HEAD
public class JSUtil {
	
	static public boolean isJS = /** @j2sNative true || */ false;

=======
import org.opensourcephysics.display.OSPRuntime;

public class JSUtil {
	@Deprecated
	public static boolean isJS = OSPRuntime.isJS;
>>>>>>> refs/remotes/origin/swingJS
}
