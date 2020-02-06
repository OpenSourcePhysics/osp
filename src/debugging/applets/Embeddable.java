/*
 * The org.opensourcephysics.davidson.applets package contains the framework for
 * embedding Open Source Physics programs into html pages.
 * Copyright (c) 2001 W. Christian.
 *
 */
package debugging.applets;

/**
 * Title:        Embeddable
 *
 * Embeddable programs can run inside an html page using the Davidson WrapperApplet and
 * the Davidson ViewerApplet.
 *
 * @author       Wolfgang Christian
 * @version 1.0
 */
public interface Embeddable extends Controllable {
  public ObjectManager getManager();
}
