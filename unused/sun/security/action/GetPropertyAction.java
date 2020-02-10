
/*
* @(#)GetPropertyAction.java   1.8 03/01/23
*
* Copyright 2003 Sun Microsystems, Inc. All rights reserved.
* SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
*/

package sun.security.action;

/**
* A convenience class for retrieving the string value of a system
* property as a privileged action.
*
* <p>An instance of this class can be used as the argument of
* <code>AccessController.doPrivileged</code>.

*/

public class GetPropertyAction implements java.security.PrivilegedAction {
private String theProp;
private String defaultVal;
 /**
37       * Constructor that takes the name of the system property whose
38       * string value needs to be determined.
39       *
40       * @param theProp the name of the system property.
*/
public GetPropertyAction(String theProp) {
 this.theProp = theProp;
}

/**
* Constructor that takes the name of the system property and the default
48       * value of that property.
49       *
50       * @param theProp the name of the system property.
51       * @param defaulVal the default value.
*/
public GetPropertyAction(String theProp, String defaultVal) {
 this.theProp = theProp;
this.defaultVal = defaultVal;
}

 /**
59       * Determines the string value of the system property whose
60       * name was specified in the constructor.
61       *
62       * @return the string value of the system property,
63       *         or the default value if there is no property with that key.
*/
public Object run() {
String value = System.getProperty(theProp);
return (value == null) ? defaultVal : value;
}
}
