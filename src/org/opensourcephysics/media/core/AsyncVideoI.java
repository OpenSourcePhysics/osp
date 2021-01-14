package org.opensourcephysics.media.core;

/**
 * A VideoAdapter that must be accessed asynchronously (JSMovieVideo)
 */
public interface AsyncVideoI {

	String PROPERTY_ASYNCVIDEOI_HAVEFRAMES = "asyncVideoHaveFrames";
	String PROPERTY_ASYNCVIDEOI_READY = "asyncVideoReady";
	String PROPERTY_ASYNCVIDEOI_IMAGEREADY = "asyncImageReady";

}
