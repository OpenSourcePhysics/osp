package davidson.qm;
public interface QMWavefunction {

   /**
    * Gets the array containing the real component of the wavefunction;
    * @return double[]
    */
   double[] getRePsi();

   /**
    * Gets the array containing the imaginary component of the wavefunction;
    * @return double[]
    */
   double[] getImPsi();

   /**
    * Gets the array containing the x values of the wavefunction;
    * @return double[]
    */
   double[] getX();

   /**
    * Gets the number of points used to approximate the wave function.
    * @return int
    */
   int getNumpts();

   /**
    * Gets the value of x at the first data point.
    * @return double xmin
    */
   double getXMin();

   /**
    * Gets the value of x at the last data point.
    * @return double xmax
    */
   double getXMax();

   /**
    * Sets the energy scale.
    *
    * @param scale double
    */
   void setEnergyScale(double scale);

   /**
    * Gets the energy scale.
    *
    * @return the scale
    */
   double getEnergyScale();
}
