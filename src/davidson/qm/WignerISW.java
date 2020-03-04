package davidson.qm;
import org.opensourcephysics.analysis.FourierAnalysis;
import org.opensourcephysics.frames.Scalar2DFrame;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.display2d.ColorMapper;
import java.text.DecimalFormat;

/**
 * WignerISW displays the Wigner function for a wavefunction in an infinite square well.
 */
public class WignerISW {

   Scalar2DFrame wignerFrame = new Scalar2DFrame("x", "p", "Wigner Distribution");
   double[] fvec, fftvec, zerovec;
   double[][] fieldData;
   int gutterpts = 0;
   double zmax = 0;
   double time = 0;
   double prange = 4;
   FourierAnalysis analysis = new FourierAnalysis();
   DecimalFormat decimalFormat = new DecimalFormat("0.00"); // default numeric format for messages
   DrawingPanel wignerPanel;
   boolean showTime=false;
   
   public WignerISW() {
      wignerFrame.setPaletteType(ColorMapper.DUALSHADE);
      wignerFrame.setExpandedZ(true, 1.5);
      wignerPanel=wignerFrame.getDrawingPanel();
   }

   void initialize(QMWavefunction superposition, int gutterpts) {
      double[] rePsi = superposition.getRePsi();
      double[] imPsi = superposition.getImPsi();
      double[] xvec = superposition.getX();
      int numpts = rePsi.length;
      this.gutterpts = gutterpts;
      fvec = new double[2*numpts]; // function;  real and imaginary values alternate
      zerovec = new double[2*numpts];
      fieldData = new double[xvec.length][xvec.length];
      double xmin = superposition.getXMin();
      double xmax = superposition.getXMax();
      fftvec = analysis.doAnalysis(xvec, fvec, gutterpts);
      double[] omega = analysis.getNaturalOmega();
      if(numpts%2==0) {
         double shift = (omega[gutterpts+1]-omega[gutterpts])/4;
         wignerFrame.setAll(fieldData, xmin, xmax, omega[gutterpts]/2+shift, -omega[gutterpts]/2+shift);
      } else {
         wignerFrame.setAll(fieldData, xmin, xmax, omega[gutterpts]/2, -omega[gutterpts]/2);
      }
      for(int i = 0, nx = fieldData.length; i<nx; i++) {
         getWignerSlice(rePsi, imPsi, fvec, i-nx/2);
         double[] fftData = analysis.repeatAnalysis(fvec);
         for(int j = 0, ny = fieldData[0].length; j<ny; j++) {
            fieldData[i][j] = fftData[2*(j+gutterpts)]; // real component
            if(zmax<fieldData[i][j]) {
               zmax = fieldData[i][j];
            }
         }
      }
      wignerFrame.setAutoscaleX(false);
      wignerFrame.setPreferredMinMaxX(xmin, xmax);
      wignerFrame.setAutoscaleY(false);
      wignerFrame.setPreferredMinMaxY(-prange, prange);
      wignerFrame.setZRange(false, -zmax, zmax);
      wignerFrame.setShowGrid(false);
      wignerFrame.setAll(fieldData);
      if(showTime){
    	  wignerFrame.setMessage("t="+decimalFormat.format(time));
      }else{
    	  wignerFrame.setMessage(null);
      }
      wignerFrame.render();
   }

   void doStep(QMWavefunction superposition) {
      double[] rePsi = superposition.getRePsi();
      double[] imPsi = superposition.getImPsi();
      for(int i = 0, nx = fieldData.length; i<nx; i++) {
         getWignerSlice(rePsi, imPsi, fvec, i-nx/2);       // kernal is in fvec array
         double[] fftData = analysis.repeatAnalysis(fvec); // transforms the kernal
         for(int j = 0, ny = fieldData[0].length; j<ny; j++) {
            fieldData[i][j] = fftData[2*(j+gutterpts)];    // real component
            if(zmax<fieldData[i][j]) {
               zmax = fieldData[i][j];
            }
         }
      }
      wignerFrame.setZRange(false, -zmax, zmax);
      wignerFrame.setAll(fieldData);
      if(showTime){
    	  wignerFrame.setMessage("t="+decimalFormat.format(time));
      }else{
    	  wignerFrame.setMessage(null);
      }
      wignerPanel.repaint();
   }

   /**
    * Gets the kernel for the FFT integral.
    *
    * @param rePsi double[]
    * @param imPsi double[]
    * @param fvec double[]
    * @param shift int
    */
   void getWignerSlice(double[] rePsi, double[] imPsi, double[] fvec, int shift) {
      System.arraycopy(zerovec, 0, fvec, 0, zerovec.length);
      for(int i = 0, n = rePsi.length; i<n; i++) {
         int ip = shift+i;
         int im = shift+n-1-i;
         if((ip<0)||(im<0)||(ip>=n)||(im>=n)) {
            continue;
         }
         double re1 = rePsi[ip];
         double im1 = imPsi[ip];
         double re2 = rePsi[im];
         double im2 = imPsi[im];
         fvec[2*i] = re1*re2+im1*im2;
         fvec[2*i+1] = -re2*im1+re1*im2;
      }
   }
}
