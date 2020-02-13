package debugging;

import javax.swing.border.EtchedBorder;

import org.opensourcephysics.ejs.control.EjsControlFrame;

public class EJSSliderTest {

	EjsControlFrame frame;
	
	EJSSliderTest(){
		frame = new EjsControlFrame(this,  "name=controlFrame;title=Flow Lines;location=100,100;size=300,300;"
				+ "layout=border;exit=true; visible=true");
		frame.add ("Panel", "name=contentPanel; parent=controlFrame; layout=border; position=center");
		
    frame.add ("Panel", "name=controlPanel; layout=vbox; parent=contentPanel;position=south");
    ((javax.swing.JPanel) frame.getElement("controlPanel").getComponent()).setBorder(new EtchedBorder() );
    frame.add ("Slider",
        "position=center;parent=controlPanel;variable=size;minimum=2;maximum=64;ticks=0;action=sliderMoved; format=grid size=0");

	}
	
  public void sliderMoved () {
    System.out.println("Slider moved.");
  }
	
	public static void main(String[] args) {
		new EJSSliderTest();

	}

}
