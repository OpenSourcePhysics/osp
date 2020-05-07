package davidson.qm;

import org.opensourcephysics.display.ComplexDataset;
import org.opensourcephysics.display.Dataset;

/**
 * A superposition of particle in a box eigenstates.
 * @author W. Christian
 * @version 1.0
 */
public class EigenstateRingSuperposition implements QMSuperposition{
  static final double PISQR=Math.PI*Math.PI;
  double[] recoef=new double[0];
  double[] imcoef=new double[0];
  double[][] eigenstates;
  double L;
  double[] x,rePsi,imPsi,rho,zeroArray;
  double energyScale=1;

  public EigenstateRingSuperposition(int numpts, double xmin, double xmax) {
    L=Math.abs(xmax-xmin);
    rePsi = new double[numpts];
    imPsi = new double[numpts];
    rho = new double[numpts];
    x = new double[numpts];
    zeroArray = new double[numpts];
    double xo = xmin;
    double dx=(xmax-xmin)/(numpts-1);
    for (int j = 0, n = numpts; j < n; j++) {
      x[j] = xo;
      xo += dx;
    }
    eigenstates= new double[0][numpts];
    setCoef(new double[0], new double[0]);
  }

  @Override
public Dataset getRho(Dataset dataset){
    if(dataset==null) dataset=new Dataset();
    else dataset.clear();
    for(int j=0, n=x.length; j<n; j++){
     rho[j]=rePsi[j]*rePsi[j]+imPsi[j]*imPsi[j];
    }
    dataset.append(x[0],0);
    dataset.append(x,rho);
    dataset.append(x[x.length-1],0);
    return dataset;
  }

  /**
   * Gets the Eigenstates.
   * @return double[][]
   */
  @Override
public double[][] getEigenstates() {
    return eigenstates;
  }

  @Override
public ComplexDataset getPsi(ComplexDataset dataset){
    if(dataset==null) dataset=new ComplexDataset();
    else dataset.clear();
    dataset.append(x,rePsi,imPsi);
    return dataset;
  }

  /**
   * Gets the number of points used to approximate the wave function.
   * @return int
   */
  @Override
public int getNumpts(){
    return x.length;
  }

  /**
   * Gets the value of x at the first data point.
   * @return double xmin
   */
  @Override
public double getXMin() {
    return x[0];
  }

  /**
   * Gets the value of x at the last data point.
   * @return double xmax
   */
  @Override
public double getXMax() {
    return x[x.length - 1];
  }

  /**
   * Gets the array containing the real component of the wavefunction;
   * @return double[]
   */
  @Override
public double[] getRePsi() {
    return rePsi;
  }

  /**
   * Gets the array containing the imaginary component of the wavefunction;
   * @return double[]
   */
  @Override
public double[] getImPsi() {
    return imPsi;
  }

  /**
   * Gets the array containing the x values of the wavefunction;
   * @return double[]
   */
  @Override
public double[] getX() {
    return x;
  }


  /**
   * Sets the energy scale.
   *
   * @param scale double
   */
  @Override
public void setEnergyScale(double scale) {
    energyScale=scale;
  }

  /**
   * Gets the energy scale.
   *
   * @return the scale
   */
  @Override
public double getEnergyScale() {
    return energyScale;
  }


  /**
   * Gets the real coefficients.
   * @return the coefficients
   */
  @Override
public double[] getReCoef() {
    return recoef;
  }

  /**
   * Gets the imaginary coefficients.
   * @return the coefficients
   */
  @Override
public double[] getImCoef() {
    return imcoef;
  }

  /**
   * Sets the coefficients.
   *
   * @param re double[]
   * @param im double[]
   * @return boolean
   */
  @Override
public boolean setCoef(double[] re, double[] im) {
    if (re != null && im == null)
      im = new double[re.length];
    if (im != null && re == null)
      re = new double[im.length];
    if (re == null && im == null) {
      re = new double[1];
      im = new double[1];
    }
    if (re.length < im.length) {
      double[] temp = re;
      re = new double[im.length];
      System.arraycopy(temp, 0, re, 0, temp.length);
    }
    if (im.length < re.length) {
      double[] temp = im;
      im = new double[re.length];
      System.arraycopy(temp, 0, im, 0, temp.length);
    }
    int n=re.length+1;  // add an extra coefficient so that there are two constant terms
    n += n%2;  // make sure that n is even
    int numpts=x.length;
    recoef= new double[n];
    imcoef= new double[n];
    System.arraycopy(re,0,recoef,1,re.length);
    System.arraycopy(im,0,imcoef,1,im.length);
    eigenstates= new double[2*n][numpts];
    double norm=Math.sqrt(1.0/L);
    for(int i=0; i<n; i+=2){
      double[] rePlus = eigenstates[2*i];
      double[] imPlus = eigenstates[2*i+1];
      double[] reMinus = eigenstates[2*i+2];
      double[] imMinus = eigenstates[2*i+3];
      double k=(i/2)*2*Math.PI/L;
      for(int j=0; j<numpts; j++){
        double phase=k*x[j];
        rePlus[j]=norm*Math.cos(phase);
        imPlus[j]=norm*Math.sin(phase);
        reMinus[j]=(i==0)?0:norm*Math.cos(-phase);
        imMinus[j]=(i==0)?0:norm*Math.sin(-phase);
      }
    }
    recoef[0]+=recoef[1];
    imcoef[0]+=imcoef[1];
    recoef[1]=0;
    imcoef[1]=0;
    return true;
  }

  /**
 * Gets the energy eigenvalue for the i-th eigenstate.
 * @return double[][]
 */
@Override
public double getEigenValue(int i) {
  return (i/2)*(i/2)*4*energyScale*PISQR/L/L; // use integer arithmetic; + and - momentum states have same energy
}



  @Override
public void update(double time){
    System.arraycopy(zeroArray,0,rePsi,0,rePsi.length);
    System.arraycopy(zeroArray,0,imPsi,0,imPsi.length);
    if(eigenstates.length==0) return;
    double kk=-4*time*energyScale*PISQR/L/L;
    for(int i=0, ns=recoef.length; i<ns; i++){
      double re=recoef[i];
      double im=imcoef[i];
      double phase=kk*(i/2)*(i/2);  // use integer arithmetic; + and - momentum states have same energy
      double sin=Math.sin(phase);
      double cos=Math.cos(phase);
      double[] reArray = eigenstates[2*i];
      double[] imArray = eigenstates[2*i+1];
      for(int j=0, n=x.length; j<n; j++){
        double a=(re*cos-im*sin);
        double b=(im*cos+re*sin);
        rePsi[j]+=(a*reArray[j]-b*imArray[j]);
        imPsi[j]+=(b*reArray[j]+a*imArray[j]);
      }
    }
  }

}
