/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.controls;
import java.util.Locale;
import java.util.MissingResourceException;

import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * ControlsRes provides access to internationalized string resources for OSPControls.
 *
 * @author Wolfgang Christian
 * @version 1.0
 */
public class ControlsRes {
  // static constants for speed
  public static String ANIMATION_NEW;
  public static String ANIMATION_INIT;
  public static String ANIMATION_STEP;
  public static String ANIMATION_RESET;
  public static String ANIMATION_START;
  public static String ANIMATION_STOP;
  public static String ANIMATION_RESET_TIP;
  public static String ANIMATION_INIT_TIP;
  public static String ANIMATION_START_TIP;
  public static String ANIMATION_STOP_TIP;
  public static String ANIMATION_NEW_TIP;
  public static String ANIMATION_STEP_TIP;
  public static String CALCULATION_CALC;
  public static String CALCULATION_RESET;
  public static String CALCULATION_CALC_TIP;
  public static String CALCULATION_RESET_TIP;
  public static String XML_NAME;
  public static String XML_VALUE;
  static final String BUNDLE_NAME = "org.opensourcephysics.resources.controls.controls_res"; //$NON-NLS-1$
  static org.opensourcephysics.tools.ResourceLoader.Bundle res;

  // private constructor because all methods are static
  private ControlsRes() {}

  static {
    String language = Locale.getDefault().getLanguage();
    Locale resourceLocale = Locale.ENGLISH;
    for(Locale locale : OSPRuntime.getInstalledLocales()) {
      if(locale.getLanguage().equals(language)) {
        resourceLocale = locale;
        break;
      }
    }
    res = ResourceLoader.getBundle(BUNDLE_NAME, resourceLocale);
    setLocalStrings();
  }

//  private static String getString(final ResourceBundle bundle, final String key) {
//    try {
//      return bundle.getString(key);
//    } catch(final MissingResourceException ex) {
//      return '|'+key+'|';
//    }
//  }

  public static void setLocale(Locale locale) {
	if(OSPRuntime.isJS) return;
    res = ResourceLoader.getBundle(BUNDLE_NAME, locale);
    setLocalStrings();
  }

  /**
   * Gets the localized value of a string. If no localized value is found, the
   * key is returned surrounded by exclamation points.
   *
   * @param key the string to localize
   * @return the localized string
   */
  static public String getString(String key) {
    try {
      return res.getString(key);
    } catch(MissingResourceException ex) {
      return "!"+key+"!"; //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  /**
  * Gets the local strings.  Static strings are used for speed to avoid having to call the resource object.
  */
  private static void setLocalStrings() {
    ANIMATION_NEW = res.getString("ANIMATION_NEW");                 //$NON-NLS-1$
    ANIMATION_INIT = res.getString("ANIMATION_INIT");               //$NON-NLS-1$
    ANIMATION_STEP = res.getString("ANIMATION_STEP");               //$NON-NLS-1$
    ANIMATION_RESET = res.getString("ANIMATION_RESET");             //$NON-NLS-1$
    ANIMATION_START = res.getString("ANIMATION_START");             //$NON-NLS-1$
    ANIMATION_STOP = res.getString("ANIMATION_STOP");               //$NON-NLS-1$
    ANIMATION_RESET_TIP = res.getString("ANIMATION_RESET_TIP");     //$NON-NLS-1$
    ANIMATION_INIT_TIP = res.getString("ANIMATION_INIT_TIP");       //$NON-NLS-1$
    ANIMATION_START_TIP = res.getString("ANIMATION_START_TIP");     //$NON-NLS-1$
    ANIMATION_STOP_TIP = res.getString("ANIMATION_STOP_TIP");       //$NON-NLS-1$
    ANIMATION_NEW_TIP = res.getString("ANIMATION_NEW_TIP");         //$NON-NLS-1$
    ANIMATION_STEP_TIP = res.getString("ANIMATION_STEP_TIP");       //$NON-NLS-1$
    CALCULATION_CALC = res.getString("CALCULATION_CALC");           //$NON-NLS-1$
    CALCULATION_RESET = res.getString("CALCULATION_RESET");         //$NON-NLS-1$
    CALCULATION_CALC_TIP = res.getString("CALCULATION_CALC_TIP");   //$NON-NLS-1$
    CALCULATION_RESET_TIP = res.getString("CALCULATION_RESET_TIP"); //$NON-NLS-1$
    XML_NAME = res.getString("XML_NAME");                           //$NON-NLS-1$
    XML_VALUE = res.getString("XML_VALUE");                         //$NON-NLS-1$
  }

}

/*
 * Open Source Physics software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.
 *
 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be released
 * under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2024  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
