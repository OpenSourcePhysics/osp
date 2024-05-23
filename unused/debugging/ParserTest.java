package debugging;

import org.opensourcephysics.numerics.ParserException;
import org.opensourcephysics.numerics.SuryonoParser;
import org.opensourcephysics.numerics.Function;

public class ParserTest {

	SuryonoParser fxParser;
	String fxStr="sin(x)";

	ParserTest() {
		try {
			fxParser = new SuryonoParser("0", "x");
			fxParser.setFunction (fxStr);
		} catch (ParserException ex) {
			System.err.println(ex.getMessage());
		}
		
		showValues(fxParser);
	}
	
	//void showValues(SuryonoParser fun){   // WC: this signature works
	void showValues(Function fun){          // WC: this signature fails
	    for (int i=0; i<=10; i++) {
	    	double x=2*Math.PI*i*0.1;
	        double val = fun.evaluate (x);  // parse the function
	        System.out.println("f(x)="+val);
	     }
	}

	public static void main(String[] args) {
		new ParserTest();
	}

}
