package org.opensourcephysics.tools;

/** Session-only common data uncertainty. Invalid supplied input never becomes estimated input. */
public final class FitUncertainty {
    public static final int ESTIMATED=0, CONSTANT=1, PIXELS=2;
    public final int mode;
    public final double value;
    public FitUncertainty(int mode, double value) {
        if(mode<0 || mode>2)throw new IllegalArgumentException("Unknown uncertainty model");
        this.mode=mode;this.value=value;
    }
    public static FitUncertainty estimated() { return new FitUncertainty(ESTIMATED,Double.NaN); }
    public double sigma(double residualSigma, double unitsPerPixel) {
        if(mode==ESTIMATED)return residualSigma;
        if(!positive(value))return Double.NaN;
        return mode==CONSTANT ? value : positive(unitsPerPixel) && positive(value*unitsPerPixel) ? value*unitsPerPixel : Double.NaN;
    }
    static boolean positive(double x) { return CurveFitReport.isFinite(x) && x>0; }
    /** sigma, chi-square, reduced chi-square, chi-square survival probability. */
    double[] statistics(double[] fitStats, boolean autofit, double unitsPerPixel) {
        double df=fitStats[2], sse=fitStats[3], sigma=sigma(fitStats[6],unitsPerPixel);
        double chi=Double.NaN, reduced=Double.NaN, q=Double.NaN;
        if(autofit && df>0 && CurveFitReport.isFinite(sse) && sse>=0 && positive(sigma)) {
            chi=mode==ESTIMATED?df:sse/sigma/sigma;
            reduced=chi/df;
            if(mode!=ESTIMATED)q=survival(chi,df);
        }
        return new double[]{sigma,chi,reduced,q};
    }
    /** Q(df/2, chi-square/2), the regularized upper incomplete gamma.
     * NIST DLMF 8.2.4; lower series for x<a+1, upper continued fraction otherwise.
     */
    static double survival(double chi, double df) {
        if(!positive(df) || Double.isNaN(chi) || chi<0)return Double.NaN;
        if(chi==0)return 1;
        if(Double.isInfinite(chi))return 0;
        double a=df/2,x=chi/2, pref=Math.exp(a*Math.log(x)-x-logGamma(a));
        if(x<a+1) {
            double term=1/a,sum=term;
            for(int n=1;n<10000;n++) { term*=x/(a+n);sum+=term;
                if(Math.abs(term)<Math.abs(sum)*1e-14)return Math.max(0,Math.min(1,1-pref*sum)); }
        } else {
            double tiny=1e-300,b=x+1-a,c=1/tiny,d=1/b,h=d;
            for(int n=1;n<10000;n++) {
                double an=-n*(n-a);b+=2;d=an*d+b;if(Math.abs(d)<tiny)d=tiny;
                c=b+an/c;if(Math.abs(c)<tiny)c=tiny;d=1/d;double delta=d*c;h*=delta;
                if(Math.abs(delta-1)<1e-14)return Math.max(0,Math.min(1,pref*h));
            }
        }
        return Double.NaN;
    }
    private static double logGamma(double z) {
        double[] c={676.5203681218851,-1259.1392167224028,771.32342877765313,
            -176.61502916214059,12.507343278686905,-.13857109526572012,9.9843695780195716e-6,1.5056327351493116e-7};
        if(z<.5)return Math.log(Math.PI)-Math.log(Math.sin(Math.PI*z))-logGamma(1-z);
        z-=1;double x=.99999999999980993;for(int i=0;i<c.length;i++)x+=c[i]/(z+i+1);
        double t=z+7.5;return .9189385332046727+(z+.5)*Math.log(t)-t+Math.log(x);
    }
}
