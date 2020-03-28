package davidson.qm;

import org.opensourcephysics.numerics.*;

/**
 * EigenstateShooting calculates energy eigenstates for an arbitrary potential using the shooting method.
 *
 * Copyright: Copyright (c) 2005
 * @author W. Christian
 * @version 1.0
 */
public class EigenstateShooting implements ODE {
  double bisectionTolerance=1.0e-2;    // tolerance
  double energy;
  double[] psi;         // wavefuction
  double[] x;           // positions
  double maxAmp;        // maximum amplitude of psi
  double xmin, xmax;    // min and max x values
  int istart,istop;
  double dx;
  double[] state=new double[3];  // state= psi, d psi/dx, x
  //ODEAdaptiveSolver solver= org.opensourcephysics.ode.MultistepSolvers.Dopri853(this);
  ODEAdaptiveSolver solver = new RK45MultiStep(this);
  Function pot;

  /**
   * Eigenstate calculates eigenstates on a grid using the given potential function.
   *
   */
  public EigenstateShooting(Function potential, int numpts, double _xmin, double _xmax){
    pot=potential;
    psi= new double[numpts];
    x= new double[numpts];
    dx=(_xmax-_xmin)/(numpts-1);
    solver.setStepSize(dx);
    xmin=_xmin;
    xmax=_xmax;
    istart=0;
    istop=numpts;
  }

  public void setBisectionTolerance(double t){
    bisectionTolerance=t;
  }

  public void setSolverTolerance(double t){
    solver.setTolerance(t);
    solver.initialize(dx);
  }

  /**
 * Estimates region where the wave function is non-zero.
 *
 * @param en double
 */
private void estimateStartStopIndex(double en) {
  double x = xmin;
  // find the first point where E>V
  for (int i = 0, n = psi.length; i < n; i++) {
    if ( (pot.evaluate(x) - en) < 0) {
      istart = i;
      break;
    }
    x += dx;
  }
  x = xmax;
  // find the last point where E>V
  for (int i = psi.length; i > istart; i--) {
    if ( (pot.evaluate(x) - en) < 0) {
      istop = i;
      break;
    }
    x -= dx;
  }
  double a = 1;
  x = xmin + istart * dx;
  // assume exponential decay on left hand side
  while (a > bisectionTolerance && istart > 0) {
    istart--;
    x -= dx;
    double k = Math.sqrt(2 * (pot.evaluate(x) - en));
    a *= Math.exp( -k * dx);
  }
  a = 1;
  x = xmin + istop * dx;
  // assume exponential decay on right hand side
  while (a > bisectionTolerance && istop < psi.length) {
    istop++;
    x += dx;
    double k = Math.sqrt(2 * (pot.evaluate(x) - en));
    a *= Math.exp( -k * dx);
  }
  //System.out.println("istart= " + istart + "  istop=" + istop);
}




  /**
   * Solves for the wavefunction with the given energy.
   * @param e double
   * @return int the number of crossings
   */
  int solve(double e){
    estimateStartStopIndex(e);
    if(istop-istart<3){
      return 0;
    }
    energy=e;
    state[0]=0;                                           // initial psi
    state[1]=1.0;                                         // nonzero initial d psi/dx
    state[2]=xmin+istart*dx;                              // initial x
    int crossing=0;                                       // count the number of zero crossings.
    boolean slopeChanged=false;
    maxAmp=0;
    double norm=0;
    double[] lastState=new double[3];
    for(int i=0; i<istart; i++){
      psi[i]=0;                                           // wavefuntion = 0 to start
      x[i]=xmin+i*dx;
    }
    solver.initialize(dx);
    for(int i=istart; i<istop; i++){
      psi[i]=state[0];                                    // store the wavefunction value
      x[i]=state[2];
      norm+=state[0]*state[0];
      System.arraycopy(state,0,lastState,0,3);            // save current state
      solver.step();                                      // step time independent Schroedinger eqn
      if(maxAmp<Math.abs(state[0]))
        maxAmp=Math.abs(state[0]);
      if((state[1]<=0 && lastState[1]>0) || (state[1]>=0 && lastState[1]<0)){
        slopeChanged=true;
      }
      if(state[0]<=0 && lastState[0]>0){                  // positive to negative transition
        crossing++;
        slopeChanged=false;
      }
      else if(state[0]>=0 && lastState[0]<0){             // negative to positive transition
        crossing++;
        slopeChanged=false;
      }
      if (Math.abs(state[0]) > 1.0e9) {                  // break if solution is diverging
        for (int j = i+1; j < istop; j++){                    // fill remaining values
          psi[j] = state[0];
          x[i] += solver.getStepSize();
          norm+=state[0]*state[0];
        }
        break;
      }
    }
    for (int i = istop, n = psi.length; i < n; i++) {
      psi[i] = psi[istop-1];
      x[i] = xmin+i * dx;
      norm+=psi[i]*psi[i];
    }
    // check to see if last value is close enough for it to be a crossing
    if (slopeChanged && Math.abs(psi[psi.length - 1]) <= bisectionTolerance*maxAmp)crossing++;
    rescale(Math.sqrt(norm*dx));
    return crossing;
  }


  /**
   * Calculates the energy eigenfuntion for the given quantum number.
   *
   * @param qnumber  quantum number
   * @param enmin minimum energy estimate
   * @param enmax maximum energy estimate
   */
  boolean calcEigenfunction(int qnumber, double enmin, double enmax) {
    int counter = 0;
    double energyErr=0;
    do {
      energyErr=(Math.abs(enmax)+Math.abs(enmin)<bisectionTolerance)?(enmax - enmin):(enmax - enmin)/(Math.abs(enmax)+Math.abs(enmin));
      double en = (enmax + enmin) / 2;  // estimate the energy
      int crossing = solve(en);
      if (crossing == qnumber && Math.abs(psi[psi.length - 1])<= bisectionTolerance * maxAmp ) {
        return true;
      }
      if(crossing == qnumber && energyErr<1.e-6){ // one part per million energy error
        return true;
      }
      if (crossing > qnumber || (crossing == qnumber && parity(crossing) * psi[psi.length - 1] > 0 )) {
        enmax = en;
      } else {
        enmin = en;
      }
      counter++;
    }
    while (counter < 32 && energyErr > 1.0e-9);  // too many computations or too small a change in energy
    return false;  // did not converge to eigenfunction
  }

  private int parity(int n){
    if(n%2==0) return 1;
    else return -1;
  }

  private void rescale(double scale){
  if(scale==0) return;
  for(int i=0, n=psi.length; i<n; i++){
    psi[i]/=scale;
    state[0]/=scale;
    state[1]/=scale;
  }
  maxAmp/=scale;
}


  /**
   * Gets the state.
   * The state for the ode solver is: psi, d psi/dx, x.
   * @return
   */
  public double[] getState(){
    return state;
  }

  public void getRate(double[] state, double[] rate ){
     rate[0]=state[1];
     rate[1]=(-energy+pot.evaluate(state[2]))*state[0];
     rate[2]=1;
  }

}
