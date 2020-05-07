package davidson.qm;

import org.opensourcephysics.display.*;
import org.opensourcephysics.numerics.*;
import javax.swing.JOptionPane;

/**
 * A superposition of quantum eigenstates.
 *
 * @author W. Christian
 * @version 1.0
 */
public class EigenstateShootingSuperposition implements QMSuperposition {
  static final int MAX_STATES=120;
  double[][] states = new double[MAX_STATES][];   // eigenfunctions
  double[] vals= new double[MAX_STATES];          // eigenvalues
  double[] recoef= new double[MAX_STATES];
  double[] imcoef= new double[MAX_STATES];
  int numstates;
  double energyScale=1;
  EigenstateShooting qmsystem;
  double[] x,rePsi,imPsi,rho;
  double[] zeroArray;

  public EigenstateShootingSuperposition(Function potential, int numpts, double xmin, double xmax) {
    this( potential, numpts, xmin, xmax, 1.0E-6, 1.0E-2);
  }

  public EigenstateShootingSuperposition(Function potential, int numpts, double xmin, double xmax, double odeTol, double bisectionTol) {
    rePsi = new double[numpts];
    imPsi = new double[numpts];
    rho = new double[numpts];
    zeroArray = new double[numpts];
    x = new double[numpts];
    qmsystem = new EigenstateShooting(potential, numpts, xmin, xmax);
    qmsystem.setSolverTolerance(odeTol);
    qmsystem.setBisectionTolerance(bisectionTol);
    double xo = xmin;
    for (int j = 0, n = rePsi.length; j < n; j++) {
      x[j] = xo;
      xo += qmsystem.dx;
    }
    setCoef(null, null);

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
   * Sets the coefficients
   * @param re double[]
   * @param im double[]
   * @return boolean
   */
  @Override
public boolean setCoef(double[] re, double[] im) {
    if((re!=null && re.length>MAX_STATES)  || (im!=null && im.length>MAX_STATES) )
    JOptionPane.showMessageDialog(null,
        "The number of engenstates cannot be larger than "+MAX_STATES+". You are obviously a theorist who needs help.",
                                  "Input Error.",
                                      JOptionPane.ERROR_MESSAGE);
    if (re != null && im == null)
      im = new double[re.length];
    if (im != null && re == null)
      re = new double[im.length];
    if (re == null && im == null) {
      re = new double[1];
      im = new double[1];
    }
    if(re.length<im.length){
      double[] temp = re;
      re=new double[im.length];
      System.arraycopy(temp,0,re,0,temp.length);
    }
    if (im.length < re.length) {
      double[] temp = im;
      im = new double[re.length];
      System.arraycopy(temp, 0, im, 0, temp.length);
    }

    boolean noerror=true;
    double enmin=Double.MAX_VALUE;
    for (int j = 0, n = x.length; j < n; j++) {
      enmin=Math.min(enmin,qmsystem.pot.evaluate(x[j]));
    }
    numstates = Math.min(re.length, recoef.length);
    System.arraycopy(re,0,recoef,0,numstates);
    System.arraycopy(im,0,imcoef,0,numstates);
    for(int i=0; i<numstates; i++){
      if(states[i]!=null) continue;  // continue if state has already been calculated
      boolean converge;
      if(i>0){
        double enmax = vals[i-1] + i;
        for (int count = 0; count < 32 && qmsystem.solve(enmax) < i+1 ; count++) {
          enmax += enmax - vals[i-1];
        }
        converge=qmsystem.calcEigenfunction(i+1,vals[i-1],enmax);
      }else{ // ground state
        double enmax = enmin + 1;
        for (int count = 0; count < 32 && qmsystem.solve(enmax) <1; count++) {
          enmax += enmax - enmin;
        }
        converge=qmsystem.calcEigenfunction(i+1,enmin,enmax);
      }
      if(converge){
        vals[i]=qmsystem.energy;
        states[i]=qmsystem.psi.clone();
        //System.out.println("state n="+i+" energy="+vals[i]);
      }else{
        System.out.println("state did not converge. n="+i);
        noerror=false;
      }
    }
    return noerror;
  }

  /**
   * Gets the energy eigenvalue for the i-th eigenstate.
   * @return double[][]
   */
  @Override
public double getEigenValue(int i) {
     return vals[i]*energyScale;
  }


  @Override
public void update(double time){
    System.arraycopy(zeroArray,0,rePsi,0,rePsi.length);
    System.arraycopy(zeroArray,0,imPsi,0,imPsi.length);
    for(int i=0; i<numstates; i++){
      double[] state=states[i];
      if(state==null) continue;
      double re=recoef[i];
      double im=imcoef[i];
      double phase=-time*vals[i]*energyScale;
      double sin=Math.sin(phase);
      double cos=Math.cos(phase);
      for(int j=0, n=rePsi.length; j<n; j++){
        rePsi[j]+=(re*cos-im*sin)*state[j];
        imPsi[j]+=(im*cos+re*sin)*state[j];
      }
    }
    rePsi[0]=rePsi[rePsi.length-1]=0;  // end points are always zero
    imPsi[0]=imPsi[imPsi.length-1]=0;
  }


  @Override
public Dataset getRho(Dataset dataset){
    if(dataset==null) dataset=new Dataset();
    else dataset.clear();
    for(int j=0, n=rePsi.length; j<n; j++){
     rho[j]=rePsi[j]*rePsi[j]+imPsi[j]*imPsi[j];
    }
    dataset.append(x,rho);
    return dataset;
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
    return states;
  }




  @Override
public ComplexDataset getPsi(ComplexDataset dataset){
    if(dataset==null) dataset=new ComplexDataset();
    else dataset.clear();
    dataset.append(x,rePsi,imPsi);
    return dataset;
  }


}
