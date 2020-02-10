/*
* @(#)StandardCharsets.java    1.18 03/01/23
*
* Copyright 2003 Sun Microsystems, Inc. All rights reserved.
* SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
*/

package sun.nio.cs;

import java.lang.ref.SoftReference;
import java.nio.charset.Charset;
import java.nio.charset.spi.CharsetProvider;
 /**
16   * Provider for platform-standard charsets.
17   *
18   * @author Mark Reinhold
19   * @version 1.18, 03/01/23
*/

public class StandardCharsets
      extends sun.nio.cs.AbstractCharsetProvider{

      static volatile SoftReference instance = null;

      public StandardCharsets() {

      charset("US-ASCII", "US_ASCII",
          new String[] {

              // IANA aliases
              "iso-ir-6",
              "ANSI_X3.4-1986",
              "ISO_646.irv:1991",
              "ASCII",
              "ISO646-US",
              "us",
              "IBM367",
              "cp367",
              "csASCII",

              // Other aliases
              "646",      // Solaris POSIX locale
              "iso_646.irv:1983", // Linux POSIX locale
              "ANSI_X3.4-1968",   // Caldera Linux POSIX locale
              "ascii7"        // Compatibility with old "default"
                          // converters.
          });

      charset("UTF-8", "UTF_8",
          new String[] {
              "UTF8"      // JDK historical
          });

      charset("UTF-16", "UTF_16",
          new String[] {
              "UTF_16"        // JDK historical
          });

      charset("UTF-16BE", "UTF_16BE",
          new String[] {
              // JDK historical aliases
              "UTF_16BE",
              "ISO-10646-UCS-2",
              "X-UTF-16BE"
          });

      charset("UTF-16LE", "UTF_16LE",
          new String[] {
              // JDK historical aliases
              "UTF_16LE",
              "X-UTF-16LE"
          });

      charset("ISO-8859-1", "ISO_8859_1",
          new String[] {

              // IANA aliases
              "iso-ir-100",
              "ISO_8859-1",
              "latin1",
              "l1",
              "IBM819",
              "cp819",
              "csISOLatin1",

              // JDK historical aliases
              "819",
              "IBM-819",
              "ISO8859_1",
              "ISO_8859-1:1987",
              "ISO_8859_1",
                      "8859_1",
                      "ISO8859-1",

          });

         charset("ISO-8859-2", "ISO_8859_2",
                 new String[] {
                     "iso8859_2", // JDK historical
                     "iso-ir-101",
                     "ISO_8859-2",
                     "ISO_8859-2:1987",
                     "latin2",
                     "l2",
                     "csISOLatin2"
                 });

         charset("ISO-8859-4", "ISO_8859_4",
                 new String[] {
                     "iso8859_4", // JDK historical
                     "iso-ir-110",
                     "ISO_8859-4",
                     "ISO_8859-4:1988",
                     "latin4",
                     "l4",
                     "csISOLatin4"
                 });

         charset("ISO-8859-5", "ISO_8859_5",
                 new String[] {
                     "iso8859_5", // JDK historical
                     "iso-ir-144",
                     "ISO_8859-5",
                     "cyrillic",
                     "csISOLatinCyrillic"
                 });

         charset("ISO-8859-7", "ISO_8859_7",
                 new String[] {
                     "iso8859_7", // JDK historical
                     "iso-ir-126",
                     "ISO_8859-7",
                     "ISO_8859-7:1987",
                     "ELOT_928",
                     "ECMA-118",
                     "greek",
                     "greek8",
                     "csISOLatinGreek",
                     "sun_eu_greek" // required for Solaris 7 compatibility
                 });

         charset("ISO-8859-9", "ISO_8859_9",
                 new String[] {
                     "iso8859_9", // JDK historical
                     "iso-ir-148",
                     "ISO_8859-9",
                     "ISO_8859-9:1989",
                     "latin5",
                     "l5",
                   "csISOLatin5"
                 });

         charset("ISO-8859-13", "ISO_8859_13",
                 new String[] {
                     "iso8859_13" // JDK historical
                 });

     charset("ISO-8859-15", "ISO_8859_15",
         new String[] {

             // IANA alias
             "ISO_8859-15",

             // JDK historical aliases
             "8859_15",
             "ISO-8859-15",
             "ISO_8859-15",
             "ISO8859-15",
             "IBM923",
             "IBM-923",
             "cp923",
             "923",
             "LATIN0",
             "LATIN9",
             "L9",
             "csISOlatin0",
             "csISOlatin9",
             "ISO8859_15_FDIS"

         });

         charset("KOI8-R", "KOI8_R",
                 new String[] {
                     "koi8",
                     "cskoi8r"
                 });

         charset("windows-1250", "MS1250",
                 new String[] {
                     "cp1250" // JDK historical
                 });

         charset("windows-1251", "MS1251",
                 new String[] {
                     "cp1251" // JDK historical
                 });

         charset("windows-1252", "MS1252",
         new String[] {
             "cp1252"        // JDK historical
         });

         charset("windows-1253", "MS1253",
                 new String[] {
                     "cp1253" // JDK historical
                 });

         charset("windows-1254", "MS1254",
                 new String[] {
                     "cp1254" // JDK historical
                 });

         charset("windows-1257", "MS1257",
                 new String[] {
                     "cp1257" // JDK historical
                 });

     instance = new SoftReference(this);

     }

     public static String[] aliasesFor(String charsetName) {
     SoftReference sr = instance;
     StandardCharsets sc = null;
     if (sr != null)
         sc = (StandardCharsets)sr.get();
     if (sc == null) {
         sc = new StandardCharsets();
         instance = new SoftReference(sc);
     }
     return sc.aliases(charsetName);
     }
}
