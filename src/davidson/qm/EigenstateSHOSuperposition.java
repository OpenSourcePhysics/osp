package davidson.qm;

import org.opensourcephysics.display.ComplexDataset;
import org.opensourcephysics.display.Dataset;

/**
 * A superposition of particle in a box eigenstates.
 * @author W. Christian
 * @version 1.0
 */
public class EigenstateSHOSuperposition implements QMSuperposition{
  static final double PISQR=Math.PI*Math.PI;
  double[] recoef=new double[0];
  double[] imcoef=new double[0];
  double[][] eigenstates;
  double[] x,rePsi,imPsi,rho,zeroArray;
  double energyScale=1;

  public EigenstateSHOSuperposition(int numpts, double xmin, double xmax) {
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
	public Dataset getRho(Dataset dataset) {
		if (dataset == null)
			dataset = new Dataset();
		for (int j = 0, n = rePsi.length; j < n; j++) {
			rho[j] = rePsi[j] * rePsi[j] + imPsi[j] * imPsi[j];
		}
		return dataset.set(x, rho);
	}

  /**
   * Gets the number of points used to approximate the wave function.
   * @return int
   */
  @Override
public int getNumpts() {
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
   * Sets the energy scale.
   *
   * @param scale double
   */
  @Override
public void setEnergyScale(double scale) {
    energyScale = scale;
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
    int n=re.length;
    int numpts=x.length;
    recoef= new double[n];
    imcoef= new double[n];
    System.arraycopy(re,0,recoef,0,n);
    System.arraycopy(im,0,imcoef,0,n);
    eigenstates= new double[n][numpts];
    for(int i=0; i<n; i++){
      double[] state = eigenstates[i];
      EigenstateSHO sho= new EigenstateSHO(i);
      for(int j=0; j<numpts; j++){
        state[j]=sho.evaluate(x[j]);
      }
    }
    return true;
  }

  /**
   * Gets the energy eigenvalue for the i-th eigenstate.
   * @return double[][]
   */
  @Override
public double getEigenValue(int i) {
    return energyScale*(0.5+i);
  }


  @Override
public void update(double time){
    System.arraycopy(zeroArray,0,rePsi,0,rePsi.length);
    System.arraycopy(zeroArray,0,imPsi,0,imPsi.length);
    if(eigenstates.length==0) return;
    double kk=-time*energyScale;
    for(int i=0, ns=recoef.length; i<ns; i++){
      double re=recoef[i];
      double im=imcoef[i];
      double phase=kk*(0.5+i);
      double sin=Math.sin(phase);
      double cos=Math.cos(phase);
      double[] state = eigenstates[i];
      for(int j=0, n=x.length; j<n; j++){
        rePsi[j]+=(re*cos-im*sin)*state[j];
        imPsi[j]+=(im*cos+re*sin)*state[j];
      }
    }
  }

}
