package org.opensourcephysics.media.core;

/**
 * A VideoAdapter that must be accessed asynchronously (JSMovieVideo)
 */
public interface AsyncVideoI {

	String PROPERTY_VIDEO_ASYNC_READY = "asyncVideoReady";
	String PROPERTY_VIDEO_IMAGE_READY = "asyncImageReady";

}
