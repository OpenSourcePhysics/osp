package davidson.qm;

import org.opensourcephysics.numerics.*;
import org.opensourcephysics.numerics.specialfunctions.*;

public class EigenstateSHO implements Function{
  static final double PISQRT=Math.sqrt(Math.PI);
  int n;// the quantum number for this state
  Polynomial hermite;

  public EigenstateSHO(int n) {
    hermite= Hermite.getPolynomial(n);
    this.n=n;
    double norm=Math.sqrt(Math.pow(2,n)*Factorials.factorial(n)*PISQRT);
    hermite=hermite.divide(norm);
  }

  /**
   * Evaluates the SHO Eigenfunction at the given x
   *
   * @param x double
   * @return double
   */
  public double evaluate(double x) {
    return Math.exp(-x*x/2)*hermite.evaluate(x);
  }

  public String toString(){
    return "exp(-x*x)*("+hermite.toString()+")";
  }

  /* --------------- test code  --------------- */

 public static void main(String args[]) {
	 for(int i=1; i<75; i++) {
     EigenstateSHO phi= new EigenstateSHO(i);
     System.err.println("i="+i+" eigenstate= "+phi+ "\nphi(1)="+phi.evaluate(1));
	 }
   
   System.err.println("\nHermite polynomials");
   for(int i=1; i<20; i++) {
     //System.err.println("i="+i+" H(i)"+Hermite.getPolynomial(i));
   }
 }


}
