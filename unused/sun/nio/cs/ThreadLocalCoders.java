/*
2    * @(#)ThreadLocalCoders.java   1.6 03/01/23
3    *
4    * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
5    * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
*/

package sun.nio.cs;

import java.nio.*;
import java.nio.charset.*;
/**
16   * Utility class for caching per-thread decoders and encoders.
*/

public class ThreadLocalCoders {

     private static final int CACHE_SIZE = 3;

      private static abstract class Cache {

    // Thread-local reference to array of cached objects, in LRU order
    private ThreadLocal cache = new ThreadLocal();
    private final int size;

      Cache(int size) {
          this.size = size;
      }

      abstract Object create(Object name);

      private void moveToFront(Object[] oa, int i) {
          Object ob = oa[i];
          for (int j = i; j > 0; j--)
          oa[j] = oa[j - 1];
          oa[0] = ob;
      }

      abstract boolean hasName(Object ob, Object name);

     Object forName(Object name) {
          Object[] oa = (Object[])cache.get();
          if (oa == null) {
          oa = new Object[size];
          cache.set(oa);
          } else {
          for (int i = 0; i < oa.length; i++) {
              Object ob = oa[i];
              if (ob == null)
              continue;
              if (hasName(ob, name)) {
              if (i > 0)
                  moveToFront(oa, i);
              return ob;
              }
          }
          }

          // Create a new object
          Object ob = create(name);
          oa[oa.length - 1] = ob;
          moveToFront(oa, oa.length - 1);
          return ob;
      }

      }

      private static Cache decoderCache = new Cache(CACHE_SIZE) {
          boolean hasName(Object ob, Object name) {
          if (name instanceof String)
              return (((CharsetDecoder)ob).charset().name().equals(name));
          if (name instanceof Charset)
              return ((CharsetDecoder)ob).charset().equals(name);
          return false;
          }
          Object create(Object name) {
          if (name instanceof String)
              return Charset.forName((String)name).newDecoder();
          if (name instanceof Charset)
              return ((Charset)name).newDecoder();
          assert false;
          return null;
          }
      };

      public static CharsetDecoder decoderFor(Object name) {
      CharsetDecoder cd = (CharsetDecoder)decoderCache.forName(name);
      cd.reset();
      return cd;
      }

      private static Cache encoderCache = new Cache(CACHE_SIZE) {
          boolean hasName(Object ob, Object name) {
          if (name instanceof String)
              return (((CharsetEncoder)ob).charset().name().equals(name));
          if (name instanceof Charset)
             return ((CharsetEncoder)ob).charset().equals(name);
         return false;
         }
         Object create(Object name) {
         if (name instanceof String)
             return Charset.forName((String)name).newEncoder();
         if (name instanceof Charset)
             return ((Charset)name).newEncoder();
         assert false;
         return null;
         }
     };

     public static CharsetEncoder encoderFor(Object name) {
     CharsetEncoder ce = (CharsetEncoder)encoderCache.forName(name);
     ce.reset();
     return ce;
     }
 }
