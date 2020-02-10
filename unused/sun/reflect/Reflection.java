/*
 * @(#)Reflection.java  1.13 03/01/23
    *
    * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
    * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
    */
   
  package sun.reflect;
  
  import java.lang.reflect.Modifier;
  
  /** Common utility routines used by both java.lang and
      java.lang.reflect */
  
  public class Reflection {
 
      /** Returns the class of the method <code>realFramesToSkip</code>
          frames up the stack (zero-based), ignoring frames associated
          with java.lang.reflect.Method.invoke() and its implementation.
          The first frame is that associated with this method, so
          <code>getCallerClass(0)</code> returns the Class object for
          sun.reflect.Reflection. Frames associated with
          java.lang.reflect.Method.invoke() and its implementation are
          completely ignored and do not count toward the number of "real"
          frames skipped. */
      public static native Class getCallerClass(int realFramesToSkip);
  
      /** Retrieves the access flags written to the class file. For
          inner classes these flags may differ from those returned by
          Class.getModifiers(), which searches the InnerClasses
          attribute to find the source-level access flags. This is used
          instead of Class.getModifiers() for run-time access checks due
          to compatibility reasons; see 4471811. Only the values of the
          low 13 bits (i.e., a mask of 0x1FFF) are guaranteed to be
          valid. */
      private static native int getClassAccessFlags(Class c);
  
      /** A quick "fast-path" check to try to avoid getCallerClass()
          calls. */
      public static boolean quickCheckMemberAccess(Class memberClass,
                                                   int modifiers)
      {
          return Modifier.isPublic(getClassAccessFlags(memberClass) & modifiers);
      }
  
      public static void ensureMemberAccess(Class currentClass,
                                            Class memberClass,
                                            Object target,
                                            int modifiers)
          throws IllegalAccessException
      {
          if (currentClass == null || memberClass == null) {
              throw new InternalError();
          }
  
          if (!verifyMemberAccess(currentClass, memberClass, target, modifiers)) {
              throw new IllegalAccessException("Class " + currentClass.getName() +
                                               " can not access a member of class " +
                                               memberClass.getName() +
                                               " with modifiers \"" +
                                               Modifier.toString(modifiers) +
                                               "\"");
          }
      }
  
      public static boolean verifyMemberAccess(Class currentClass,
                                               // Declaring class of field
                                               // or method
                                               Class  memberClass,
                                               // May be NULL in case of statics
                                               Object target,
                                               int    modifiers)
      {
          // Verify that currentClass can access a field, method, or
          // constructor of memberClass, where that member's access bits are
          // "modifiers".
  
          boolean gotIsSameClassPackage = false;
          boolean isSameClassPackage = false;
  
          if (currentClass == memberClass) {
              // Always succeeds
              return true;
          }
  
          if (!Modifier.isPublic(getClassAccessFlags(memberClass))) {
              isSameClassPackage = isSameClassPackage(currentClass, memberClass);
              gotIsSameClassPackage = true;
              if (!isSameClassPackage) {
                  return false;
              }
          }
  
          // At this point we know that currentClass can access memberClass.
  
          if (Modifier.isPublic(modifiers)) {
              return true;
          }
  
         boolean successSoFar = false;
 
         if (Modifier.isProtected(modifiers)) {
             // See if currentClass is a subclass of memberClass
             if (isSubclassOf(currentClass, memberClass)) {
                 successSoFar = true;
             }
         }
 
         if (!successSoFar && !Modifier.isPrivate(modifiers)) {
             if (!gotIsSameClassPackage) {
                 isSameClassPackage = isSameClassPackage(currentClass,
                                                         memberClass);
                 gotIsSameClassPackage = true;
             }
 
             if (isSameClassPackage) {
                 successSoFar = true;
             }
         }
 
         if (!successSoFar) {
             return false;
         }
 
         if (Modifier.isProtected(modifiers)) {
             // Additional test for protected members: JLS 6.6.2
             Class targetClass = (target == null ? memberClass : target.getClass());
             if (targetClass != currentClass) {
                 if (!gotIsSameClassPackage) {
                     isSameClassPackage = isSameClassPackage(currentClass, memberClass);
                     gotIsSameClassPackage = true;
                 }
                 if (!isSameClassPackage) {
                     if (!isSubclassOf(targetClass, currentClass)) {
                         return false;
                     }
                 }
             }
         }
 
         return true;
     }
                                     
     private static boolean isSameClassPackage(Class c1, Class c2) {
         return isSameClassPackage(c1.getClassLoader(), c1.getName(),
                                   c2.getClassLoader(), c2.getName());
     }
 
     /** Returns true if two classes are in the same package; classloader
         and classname information is enough to determine a class's package */
     private static boolean isSameClassPackage(ClassLoader loader1, String name1,
                                               ClassLoader loader2, String name2)
     {
         if (loader1 != loader2) {
             return false;
         } else {
             int lastDot1 = name1.lastIndexOf('.');
             int lastDot2 = name2.lastIndexOf('.');
             if ((lastDot1 == -1) || (lastDot2 == -1)) {
                 // One of the two doesn't have a package.  Only return true
                 // if the other one also doesn't have a package.
                 return (lastDot1 == lastDot2);
             } else {
                 int idx1 = 0;
                 int idx2 = 0;
 
                 // Skip over '['s
                 if (name1.charAt(idx1) == '[') {
                     do {
                         idx1++;
                     } while (name1.charAt(idx1) == '[');
                     if (name1.charAt(idx1) != 'L') {
                         // Something is terribly wrong.  Shouldn't be here.
                         throw new InternalError("Illegal class name " + name1);
                     }
                 }
                 if (name2.charAt(idx2) == '[') {
                     do {
                         idx2++;
                     } while (name2.charAt(idx2) == '[');
                     if (name2.charAt(idx2) != 'L') {
                         // Something is terribly wrong.  Shouldn't be here.
                         throw new InternalError("Illegal class name " + name2);
                     }
                 }
 
                 // Check that package part is identical
                 int length1 = lastDot1 - idx1;
                 int length2 = lastDot2 - idx2;
 
                 if (length1 != length2) {
                     return false;
                 }
                 return name1.regionMatches(false, idx1, name2, idx2, length1);
             }
         }
     }
 
     static boolean isSubclassOf(Class queryClass,
                                 Class ofClass)
     {
         while (queryClass != null) {
             if (queryClass == ofClass) {
                 return true;
             }
             queryClass = queryClass.getSuperclass();
         }
         return false;
     }
}