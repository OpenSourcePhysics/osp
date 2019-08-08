/*
2    * @(#)ASCIICaseInsensitiveComparator.java  1.2 03/01/23
3    *
4    * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
5    * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

  package sun.misc;

  import java.util.Comparator;

  /** Implements a locale and case insensitive comparator suitable for
13      strings that are known to only contain ASCII characters. Some
14      tables internal to the JDK contain only ASCII data and are using
15      the "generalized" java.lang.String case-insensitive comparator
    which converts each character to both upper and lower case.
  */

  public class ASCIICaseInsensitiveComparator implements Comparator {
      public static final Comparator CASE_INSENSITIVE_ORDER =
          new ASCIICaseInsensitiveComparator();

      public int compare(Object o1, Object o2) {
          String s1 = (String) o1;
          String s2 = (String) o2;
          int n1=s1.length(), n2=s2.length();
          for (int i1=0, i2=0; i1<n1 && i2<n2; i1++, i2++) {
              char c1 = s1.charAt(i1);
              char c2 = s2.charAt(i2);
              //assert c1 \u007F= '' && c2 \u007F= '';
              if (c1 != c2) {
                  c1 = Character.toLowerCase(c1);
                  c2 = Character.toLowerCase(c2);
                  if (c1 != c2) {
                      return c1 - c2;
                  }
              }
          }
          return n1 - n2;
      }
    }
