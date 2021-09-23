package test;

import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.ResourceLoader;

public class ReadZipHTMLTest {
	
	static String path = "https://physlets.org/tracker/library/JS/basketballGIF.trz!/html/basketballGIF_info.html";

	public static void main(String[] args) {
		Resource res = ResourceLoader.getResource(path);
		if (res != null) {
			String htmlCode = res.getString();
		}
		System.exit(0);
	}

}
