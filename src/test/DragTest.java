package test;
import org.opensourcephysics.display.*;
import org.opensourcephysics.frames.DisplayFrame;

public class DragTest
{
  public static void main(String[] args) {
    DisplayFrame frame = new DisplayFrame("x", "y", "Drag Shapes");
    frame.addDrawable(InteractiveShape.createCircle(-5.0, -5.0, 4));
    frame.setVisible(true);
    frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
  }
}

