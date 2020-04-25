package test;

import org.opensourcephysics.media.core.*;


public class PlayVideoTest {
	
	
	public static void main(String[] args) {
		// register JSVideoTypes with VideoIO here
		VideoPanel panel = new VideoPanel();
		VideoFrame frame = new VideoFrame(panel);
		frame.setVisible(true);
	}
	
}
