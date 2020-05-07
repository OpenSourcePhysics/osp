/*
 * The org.opensourcephysics.davidson.applets package contains the framework for
 * embedding Open Source Physics programs into html pages.
 * Copyright (c) 2001 W. Christian.
 *
 */
package debugging.applets;
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.awt.Container;

/**
 * ObjectManager stores objects for use in embeddable applets.
 *
 * Views are container objects such as JPanels and JFrames that provide
 * views of a program's data.
 *
 * @author       Wolfgang Christian
 * @version 1.0
 */
public class ObjectManager {
    Map myObjects = new HashMap();
    Map myViews = new HashMap();

   /**
    * Stores a reference to an object.
    *
    * @param key the key that identifies the object
    * @param obj the object
    */
   public void addObject(String key, Object obj) {
     myObjects.put(key, obj);
   }

   /**
    * Gets an object.
    *
    * @param key the key that identifies the object
    * @return the object
    */
   public Object getObject(String key) {
     Object obj=myObjects.get(key);
     if(obj==null)obj=myViews.get(key);
     return obj;
 }


  /**
   * Stores a reference to a view.
   *
   * @param key the key that identifies the object
   * @param view the graphical object
   */
   public void addView (String key, Container view){
     myViews.put(key, view);
   }


  /**
   * Clears all objects from the manager.
   */
   public void clearAll(){
     myObjects.clear();
     myViews.clear();
   }

   /**
 * Clears objects from the manager.
 */
 public void clearObjects(){
   myObjects.clear();
 }


 /**
 * Clears views from the manager.
 */
 public void clearViews (){
   myViews.clear();
 }


  /**
   * Determines if the view manager contains a given view.
   *
   * @return true if the manager contains the view
   */
   public boolean containsView (Container view){
     return myViews.containsValue(view);
   }

  /**
   * Gets a view.
   *
   * @param key the key that identifies the view
   * @return the view
   */
   public Container getView (String key){
     return (Container) myViews.get(key);
   }

  /**
   * Gets the views.
   *
   * @return the views
   */
   public Collection getViews (){
     return myViews.values();
   }

   /**
    * Gets objects of an assignable type. The collection contains
    * objects that are assignable from the class or interface.
    *
    * Returns a shallow clone.
    *
    * @param c the type of object
    * @return the collection
    */
   public Collection getObjects(Class c) {
     HashMap map = new HashMap(myObjects); // clone the object map
     Iterator it = map.values().iterator();
     while (it.hasNext()) { // copy only the objects of the correct type
       Object obj = it.next();
       if (!c.isInstance(obj)) {
         it.remove();
       }
     }
     return map.values();
  }

  /**
 * Gets the objects.
 *
 * @return the collection
 */
public Collection getObjects() {
  return myObjects.values();
}


  /**
   * Removes objects of an assignable type.
   *
   * @param c the type of object
   */
  public void removeObjects(Class c) {
    HashMap map = new HashMap(myObjects); // clone the object map
    Iterator it = map.values().iterator();
    while (it.hasNext()) { // copy only the obejcts of the correct type
      Object obj = it.next();
      if (!c.isInstance(obj)) {
        it.remove();
      }
    }
  }

  /**
   * Removes an object from the manager.
   * @param obj the object
   */
  public synchronized void removeObject(Object obj) {
    myObjects.remove(obj);
  }

  /**
   * Gets the objects.
   *
   * @return the collection
   */
  public void printObjectsAndViews() {
    Iterator it = myObjects.keySet().iterator();
    System.out.println("Objects");
    while (it.hasNext()) { // copy only the obejcts of the correct type
      System.out.println(it.next().toString());
    }
    it = myViews.keySet().iterator();
    System.out.println("Views");
    while (it.hasNext()) { // copy only the obejcts of the correct type
      System.out.println(it.next().toString());
    }

  }


}



