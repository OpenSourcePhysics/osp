package org.opensourcephysics.media.mov;

import java.io.IOException;

public interface MovieVideoI {

	boolean isSmoothPlay();

	void setSmoothPlay(boolean b);

	void init(String name) throws IOException;

}
