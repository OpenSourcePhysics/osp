package test;

import org.opensourcephysics.media.core.*;
import org.opensourcephysics.media.mov.MovieFactory;


public class PlayVideoTest {
	
	
	public static void main(String[] args) {
		// register JSVideoTypes with VideoIO here
		MovieFactory.registerWithViewoIO();
		VideoPanel panel = new VideoPanel();
		VideoFrame frame = new VideoFrame(panel);
		frame.setVisible(true);
	}
	
}
