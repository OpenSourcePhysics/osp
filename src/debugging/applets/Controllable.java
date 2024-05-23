/*
 * The org.opensourcephysics.davidson.applets package contains the framework for
 * embedding Open Source Physics programs into html pages.
 * Copyright (c) 2001 W. Christian.
 *
 */
package debugging.applets;
import org.opensourcephysics.controls.*;

/**
 * Controllable allows another program (such as an applet) to change the
 * control object.
 *
 * @author       Wolfgang Christian
 * @version 1.0
 */
public interface Controllable {
  public void setControl(Control control);
  public Control getControl();
}