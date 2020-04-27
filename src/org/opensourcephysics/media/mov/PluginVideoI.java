package org.opensourcephysics.media.mov;

import java.io.IOException;

/**
 * Interface methods specific to Plugins such as XuggleVideo
 * 
 * @author hansonr
 *
 */

public interface PluginVideoI extends MovieVideoI {

	boolean isSmoothPlay();

	void setSmoothPlay(boolean b);

	void init(String name) throws IOException;
	
	
}
