/*
2    * @(#)AbstractCharsetProvider.java 1.11 03/01/23
3    *
4    * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
5    * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
*/
package sun.nio.cs;
   
  import java.lang.ref.SoftReference;
  import java.nio.charset.Charset;
  import java.nio.charset.spi.CharsetProvider;
  import java.util.ArrayList;
  import java.util.TreeMap;
  import java.util.Iterator;
  import java.util.Locale;
  import java.util.Map;
  import sun.misc.ASCIICaseInsensitiveComparator;
  
  
  /**
   * Abstract base class for charset providers.
   *
   * @author Mark Reinhold
   * @version 1.11, 03/01/23
   */
  
  public class AbstractCharsetProvider
      extends CharsetProvider
  {
  
      /* Maps canonical names to class names
       */
      private Map classMap
      = new TreeMap(ASCIICaseInsensitiveComparator.CASE_INSENSITIVE_ORDER);
  
      /* Maps alias names to canonical names
       */
      private Map aliasMap
      = new TreeMap(ASCIICaseInsensitiveComparator.CASE_INSENSITIVE_ORDER);
  
      /* Maps canonical names to alias-name arrays
       */
      private Map aliasNameMap
      = new TreeMap(ASCIICaseInsensitiveComparator.CASE_INSENSITIVE_ORDER);
  
      /* Maps canonical names to soft references that hold cached instances
       */
      private Map cache
      = new TreeMap(ASCIICaseInsensitiveComparator.CASE_INSENSITIVE_ORDER);
  
      private String packagePrefix;
  
      protected AbstractCharsetProvider() {
      packagePrefix = "sun.nio.cs";
      }
  
      protected AbstractCharsetProvider(String pkgPrefixName) {
      packagePrefix = pkgPrefixName;
      }
  
      /* Add an entry to the given map, but only if no mapping yet exists
       * for the given name.
       */
      private static void put(Map m, String name, Object value) {
      if (!m.containsKey(name))
          m.put(name, value);
      }
  
      /* Declare support for the given charset
       */
      protected void charset(String name, String className, String[] aliases) {
      synchronized (this) {
          put(classMap, name, className);
          for (int i = 0; i < aliases.length; i++)
          put(aliasMap, aliases[i], name);
          put(aliasNameMap, name, aliases);
      }
      }
  
      private String canonicalize(String charsetName) {
      String acn = (String)aliasMap.get(charsetName);
      return (acn != null) ? acn : charsetName;
      }
  
      private Charset lookup(String csn) {
  
      // Check cache first
      SoftReference sr = (SoftReference)cache.get(csn);
      if (sr != null) {
          Charset cs = (Charset)sr.get();
          if (cs != null)
          return cs;
      }
  
      // Do we even support this charset?
      String cln = (String)classMap.get(csn);
      
      if (cln == null)
         return null;
 
     // Instantiate the charset and cache it
     try {
 
         Class c = Class.forName(packagePrefix + "." + cln,
                     true,
                     this.getClass().getClassLoader());
 
         Charset cs = (Charset)c.newInstance();
         cache.put(csn, new SoftReference(cs));
         return cs;
     } catch (ClassNotFoundException x) {
         return null;
     } catch (IllegalAccessException x) {
         return null;
     } catch (InstantiationException x) {
         return null;
     }
     }
 
     public final Charset charsetForName(String charsetName) {
     synchronized (this) {
         return lookup(canonicalize(charsetName));
     }
     }
 
     public final Iterator charsets() {
 
     final ArrayList ks;
     synchronized (this) {
         ks = new ArrayList(classMap.keySet());
     }
 
     return new Iterator() {
         Iterator i = ks.iterator();
 
         public boolean hasNext() {
             return i.hasNext();
         }
 
         public Object next() {
             String csn = (String)i.next();
             return lookup(csn);
         }
 
         public void remove() {
             throw new UnsupportedOperationException();
         }
         };
     }
 
     public final String[] aliases(String charsetName) {
     synchronized (this) {
         return (String[])aliasNameMap.get(charsetName);
     }
     }
 
 }