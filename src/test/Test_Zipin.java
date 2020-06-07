package test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

public class Test_Zipin extends Test_ {

	@SuppressWarnings("unused")
	public static void main(String[] args) {

		try {
			

			InputStream is = Test_Zipin.class.getResourceAsStream("3c9k.xml.gz");
			GZIPInputStream  gzis = new GZIPInputStream(is);
			gzis = new GZIPInputStream(gzis);
			byte[] buf = new byte[100];
			gzis.read(buf, 0, 100);
			gzis.close();
			System.out.println("Test_Zipin OK");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public static Object getStreamAsBytes(BufferedInputStream bis) throws IOException {
		byte[] buf = new byte[1024];
		byte[] bytes = new byte[4096];
		int len = 0;
		int totalLen = 0;
		while ((len = bis.read(buf, 0, 1024)) > 0) {
			totalLen += len;
				if (totalLen >= bytes.length)
					bytes = Arrays.copyOf(bytes, totalLen * 2);
				System.arraycopy(buf, 0, bytes, totalLen - len, len);
		}
		bis.close();
		return 	Arrays.copyOf(bytes, totalLen);
	}

}