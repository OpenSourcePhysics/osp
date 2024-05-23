package davidson.qm;
import org.opensourcephysics.display.*;

public interface QMSuperposition extends QMWavefunction {

   /**
    * Gets the Eigenstates.
    * @return double[][]
    */
   double[][] getEigenstates();

   /**
    * Gets the energy eigenvalue for the i-th eigenstate.
    * @return double[][]
    */
   double getEigenValue(int i);

   /**
    * Sets the superposition coefficients.
    * @param re double[]
    * @param im double[]
    * @return boolean
    */
   boolean setCoef(double[] re, double[] im);

   /**
    * Gets the real coefficients.
    * @return the coefficients
    */
   double[] getReCoef();

   /**
    * Gets the imaginary coefficients.
    * @return the coefficients
    */
   double[] getImCoef();

   /**
    * Updates the internal state to the new time.
    *
    * @param time double
    */
   void update(double time);

   /**
    * Gets the wave function by filling the dataset with complex numbers.
    *
    * @param dataset ComplexDataset
    * @return ComplexDataset
    */
   ComplexDataset getPsi(ComplexDataset dataset);

   /**
    * Gets the probability by filling the dataset.
    *
    * @param dataset Dataset
    * @return Dataset
    */
   Dataset getRho(Dataset dataset);
}
