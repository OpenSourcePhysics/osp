package org.opensourcephysics.media.xuggle;

import java.io.IOException;

import org.opensourcephysics.media.mov.MovieVideoI;

/**
 * Interface methods specific to XuggleVideo
 * 
 * @author hansonr
 *
 */

public interface XuggleVideoI extends MovieVideoI {

	boolean isSmoothPlay();

	void setSmoothPlay(boolean b);

	void init(String name) throws IOException;

}
