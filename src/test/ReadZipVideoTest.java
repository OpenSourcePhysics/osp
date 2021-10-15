package test;

import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.ResourceLoader;

public class ReadZipVideoTest {
	
	static String webpath = "https://physlets.org/tracker/library/JS/basketball.zip";
	static String localpath = "C:/Users/Doug/Website/tracker-davidson/library/JS/basketball.zip";

	public static void main(String[] args) {
		Resource res = ResourceLoader.getResource(webpath);
		if (res != null) {
			String[] paths = VideoIO.getZippedImagePaths(webpath);
			System.out.println("Found in zip file: " + (paths == null? null: paths.length+" images "+paths[0]));
		}
//		System.exit(0);
	}

}
