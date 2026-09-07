package org.opensourcephysics.tools;

import org.opensourcephysics.display.*;
import org.opensourcephysics.media.core.VideoIO;
import javax.swing.SwingUtilities;

/** Scientific and clipboard regression checks, also transpiled for SwingJS. */
public class CurveFitPhysicsTest {
    static int passed;
    static void check(boolean ok,String name) { if(!ok)throw new AssertionError(name);passed++; }
    static void near(double a,double b,String name) { check(Math.abs(a-b)<1e-7*Math.max(1,Math.abs(b)),name+": "+a+" != "+b); }
    public static void main(String[] args) throws Exception {
        try { if(OSPRuntime.isJS)run();else SwingUtilities.invokeAndWait(()->run()); }
        catch(Throwable t) { t.printStackTrace();if(!OSPRuntime.isJS)System.exit(1);throw new RuntimeException(t); }
        System.out.println("Passed: "+passed);
        if(!OSPRuntime.isJS)System.exit(0);
    }
    static void run() {
        OSPRuntime.setPreferredDecimalSeparator(".");
        check(DatasetCurveFitter.formatDataUncertainty(.002518020744556).equals("0.0025"),"data uncertainty two significant digits");
        check(DatasetCurveFitter.formatDataUncertainty(1).equals("1.0"),"pixel trailing zero");
        check(DatasetCurveFitter.formatDataUncertainty(.000001234).equals("1.2E-6"),"small uncertainty compact scientific notation");
        check(DatasetCurveFitter.formatDataUncertainty(.00999).equals("0.010"),"uncertainty rounding carry");
        OSPRuntime.setPreferredDecimalSeparator(",");
        check(DatasetCurveFitter.formatDataUncertainty(.002518).equals("0,0025"),"localized uncertainty display");
        OSPRuntime.setPreferredDecimalSeparator(".");
        DatasetManager manager=new DatasetManager();manager.setXPointsLinked(true);
        manager.setXYColumnNames(0,"t","x");manager.append(0,new double[]{0,1,2,3},new double[]{1.1,2.9,4.9,7.1});
        manager.setXYColumnNames(1,"t","v_{x}");manager.append(1,new double[]{0,1,2,3},new double[]{2,2,2,2});
        DataTable table=new DataTable();table.add(manager.model);
        table.refreshTable(DataTable.MODE_CREATE);
        String delimiter=VideoIO.getDelimiter();
        for(String delim:new String[]{"\t",","}) {
            VideoIO.setDelimiter(delim);
            for(boolean formatted:new boolean[]{false,true}) {
                table.setUnits("t",null,null);table.setUnits("x",null,null);table.setUnits("v_{x}",null,null);
                String bare=table.getData(formatted).toString();
                check(!bare.substring(0,bare.indexOf('\n')).contains("("),"no units");
                check(bare.startsWith("t"+delim+"x"+delim+"vx"),"unitless headers preserve legacy subscript normalization");
                table.setUnits("t"," s",null);table.setUnits("x"," m",null);table.setUnits("v_{x}"," m/s²",null);
                String full=table.getData(formatted).toString();
                check(full.startsWith("t (s)"+delim+"x (m)"+delim+"v_x (m/s^2)"),"all unit headers "+delim+formatted);
                check(full.substring(full.indexOf('\n')).equals(bare.substring(bare.indexOf('\n'))),"numeric cells unchanged "+formatted);
                table.setUnits("x",null,null);
                check(table.getData(formatted).toString().startsWith("t (s)"+delim+"x"+delim),"mixed headers");
                DatasetManager[] pasted=DataTool.parseData(full,null);
                check(pasted!=null,"export parses");
                DataTable again=new DataTable();again.add(pasted[0].model);again.refreshTable(DataTable.MODE_CREATE);
                String twice=again.getData(false).toString();
                check(!twice.contains("(m) (m)") && twice.contains("x (m)"),"round trip no duplicate");
            }
        }
        VideoIO.setDelimiter(delimiter);
        check(ExportText.header("x (m)","m").equals("x (m)"),"known suffix not doubled");
        check(ExportText.ascii("A*t^{2} + B*t + C").equals("A*t^2 + B*t + C"),"plain equation");
        for(int degree=0;degree<=3;degree++) {
            KnownPolynomial polynomial=new KnownPolynomial(new double[degree+1]);
            for(int i=0;i<=degree;i++) {
                int power=degree-i;polynomial.setParameterValue(i,7);
                String expected=power==0?"m":power==1?"m/s":"m/s^"+power;
                check(CurveFitReport.coefficientUnits(polynomial,i,"s","m").equals(expected),"polynomial degree "+degree+" term "+power);
                check(CurveFitReport.coefficientUnits(polynomial,i,null,"m").equals(power==0?"m":""),"unknown x unit");
                check(CurveFitReport.coefficientUnits(polynomial,i,"s",null).isEmpty(),"unknown y unit");
            }
        }
        UserFunction custom=new UserFunction("custom");custom.setParameters(new String[]{"A"},new double[]{1},null);custom.setExpression("A*x",new String[]{"x"});
        check(CurveFitReport.coefficientUnits(custom,0,"s","m").isEmpty(),"user function units not guessed");
        Dataset data=CurveFitConstraintTest.data(new double[]{0,1,2,3},new double[]{1.1,2.9,4.9,7.1});
        DatasetCurveFitter fitter=CurveFitConstraintTest.line(data);
        near(fitter.fit.getParameterValue(0),2,"default slope");near(fitter.fit.getParameterValue(1),1,"default intercept");
        double[] stats=CurveFitReport.statistics(fitter.fit,data,null,true);
        FitUncertainty estimated=FitUncertainty.estimated();double[] chi=estimated.statistics(stats,true,Double.NaN);
        near(chi[0],Math.sqrt(.02),"estimated sigma");near(chi[1],2,"default chi df");near(chi[2],1,"default reduced chi");check(Double.isNaN(chi[3]),"default Q unavailable");
        double oldError=fitter.getUncertainty(0),slope=fitter.fit.getParameterValue(0),intercept=fitter.fit.getParameterValue(1);
        fitter.setUncertaintyModel(FitUncertainty.CONSTANT,.2);
        check(fitter.fit.getParameterValue(0)==slope && fitter.fit.getParameterValue(1)==intercept,"constant sigma leaves coefficients bitwise unchanged");
        near(fitter.getUncertainty(0),oldError*.2/Math.sqrt(.02),"profile uncertainty uses supplied scale");
        chi=fitter.getUncertaintyModel().statistics(stats,true,Double.NaN);
        near(chi[0],.2,"supplied sigma not rescaled");near(chi[1],1,"specified chi");near(chi[2],.5,"specified reduced chi");near(chi[3],Math.exp(-.5),"Q df2");
        near(FitUncertainty.survival(3.841458820694124,1),.05,"Q df1");
        near(FitUncertainty.survival(18.307038053275146,10),.05,"Q df10");
        near(FitUncertainty.survival(0,3),1,"Q zero");near(FitUncertainty.survival(1000,2),Math.exp(-500),"Q extreme");
        fitter.setUncertaintyModel(FitUncertainty.CONSTANT,Double.NaN);
        check(Double.isNaN(fitter.getUncertainty(0)),"invalid supplied sigma no estimated fallback");
        chi=fitter.getUncertaintyModel().statistics(stats,true,Double.NaN);check(Double.isNaN(chi[1]),"invalid chi unavailable");
        fitter.setAutoFit(false);fitter.setUncertaintyModel(FitUncertainty.CONSTANT,.2);
        check(!fitter.isAutoFit() && fitter.fit.getParameterValue(0)==slope && Double.isNaN(fitter.getUncertainty(0)),"manual behavior retained");
        for(double pixels:new double[]{.5,1,1.5,2,2.5,3,.73}) {
            FitUncertainty model=new FitUncertainty(FitUncertainty.PIXELS,pixels);
            near(model.sigma(Double.NaN,.01),pixels*.01,"fractional pixel conversion");
            double[] result=model.statistics(stats,true,.01);
            near(result[1],stats[3]/Math.pow(pixels*.01,2),"pixel chi");
            check(Double.isNaN(model.sigma(1,Double.NaN)),"derived quantity has no pixel conversion");
        }
        UserFunction redundant=new UserFunction("RedundantPhysics");redundant.setParameters(new String[]{"A","B"},new double[]{1,1},null);redundant.setExpression("(A+B)*x",new String[]{"x"});
        stats=CurveFitReport.statistics(redundant,data,null,true);near(stats[1],2,"nominal free count");near(stats[8],1,"rank one");near(stats[2],3,"rank df");
        chi=estimated.statistics(stats,true,Double.NaN);near(chi[1],3,"rank default chi");
        stats=CurveFitReport.statistics(fitter.fit,data,new boolean[]{true,false},true);near(stats[8],1,"fixed rank");near(stats[2],3,"fixed df");
        Dataset exact=CurveFitConstraintTest.data(new double[]{0,1,2,3},new double[]{1,3,5,7});
        fitter=CurveFitConstraintTest.line(exact);stats=CurveFitReport.statistics(fitter.fit,exact,null,true);
        chi=estimated.statistics(stats,true,Double.NaN);near(chi[0],0,"perfect residual sigma zero");check(Double.isNaN(chi[1]),"perfect estimated chi undefined");
        fitter.setUncertaintyModel(FitUncertainty.CONSTANT,.2);check(fitter.getUncertainty(0)>0,"perfect fit known noise positive profile error");
        chi=fitter.getUncertaintyModel().statistics(stats,true,Double.NaN);near(chi[1],0,"perfect specified chi zero");near(chi[3],1,"perfect specified Q one");
        // Existing UserFunction profile-curvature path, including its scale.
        fitter=CurveFitConstraintTest.line(data);
        UserFunction exponential=new UserFunction("PhysicsExp");
        exponential.setParameters(new String[]{"A"},new double[]{.4},null);
        exponential.setExpression("exp(A*x)",new String[]{"x"});
        fitter.fit=exponential;fitter.fit(exponential,true);
        double originalA=fitter.fit.getParameterValue(0),profile=fitter.getUncertainty(0);
        double residualSigma=CurveFitReport.statistics(fitter.fit,data,null,true)[6];
        fitter.setUncertaintyModel(FitUncertainty.CONSTANT,residualSigma*2);
        check(fitter.fit.getParameterValue(0)==originalA,"user function coefficient unchanged by scale");
        check(profile>0 && Math.abs(fitter.getUncertainty(0)/profile-2)<.02,"user function profile errors scale without covariance replacement");
        fitter.fit=redundant;fitter.fit(redundant,true);
        check(Double.isNaN(fitter.getUncertainty(0)) && Double.isNaN(fitter.getUncertainty(1)),"redundant supplied-sigma profile errors remain unavailable");

        DataTool tool=new DataTool(manager);DataToolTab tab=tool.getTab(0);tab.checkGUI();
        final double[] scale={.01};
        tab.setFitMetadataProvider(new FitMetadataProvider() {
            public String getUnits(String column) { return column.equals("t")?"s":column.equals("x")?"m":null; }
            public double getYUnitsPerPixel(String column) { return column.equals("x")?scale[0]:Double.NaN; }
        });
        tab.setWorkingColumns("t","x");
        fitter=tab.getCurveFitter();fitter.selectFit(fitter.getPolyFitNameOfDegree(1));fitter.setActiveAndFit(true);
        double measuredSlope=fitter.fit.getParameterValue(0);
        fitter.selectUncertaintyMode(FitUncertainty.CONSTANT);
        near(fitter.getUncertaintyModel().value,.01,"physical default is one calibrated pixel");
        fitter.setUncertaintyModel(FitUncertainty.CONSTANT,.001);
        fitter.selectUncertaintyMode(FitUncertainty.PIXELS);
        near(fitter.getUncertaintyModel().value,.1,"physical to pixel switch preserves scale");
        fitter.selectUncertaintyMode(FitUncertainty.CONSTANT);
        near(fitter.getUncertaintyModel().value,.001,"pixel to physical switch preserves scale");
        fitter.selectUncertaintyMode(FitUncertainty.ESTIMATED);
        fitter.selectUncertaintyMode(FitUncertainty.PIXELS);
        near(fitter.getUncertaintyModel().value,1,"pixel default is one pixel");
        check(fitter.fit.getParameterValue(0)==measuredSlope,"mode switches preserve coefficient");
        near(fitter.getYUnitsPerPixel(),.01,"host position calibration reaches fitter: "+fitter.getData().getYColumnName());
        for(double pixels:new double[]{.5,1,1.5,2,2.5,3,.73}) {
            fitter.setUncertaintyModel(FitUncertainty.PIXELS,pixels);
            near(fitter.getUncertaintyModel().sigma(Double.NaN,fitter.getYUnitsPerPixel()),pixels*.01,"host fractional pixel scale");
            check(fitter.fit.getParameterValue(0)==measuredSlope,"pixel choices preserve fitted coefficient");
        }
        scale[0]=.02;fitter.refreshUncertaintyModel();near(fitter.getYUnitsPerPixel(),.02,"live calibration refresh");
        tab.setWorkingColumns("t","v_{x}");
        check(Double.isNaN(fitter.getYUnitsPerPixel()),"derived variable does not inherit position calibration");
        fitter.setUncertaintyModel(FitUncertainty.PIXELS,1);
        check(Double.isNaN(fitter.getUncertainty(0)),"ineligible pixel sigma not applied");
        // This fixture is disposable; do not open a modal save prompt during cleanup.
        tab.tabChanged(false);
        tool.dispose();
        fitter=CurveFitConstraintTest.line(exact);
        String report=CurveFitReport.create(fitter.fit,exact,null,new double[]{.1,.1},true,true,"s","m",estimated,Double.NaN);
        check(report.contains("X variable\tt (s)") && report.contains("Y variable\ty (m)"),"fit variable units");
        check(report.contains("Parameter\tCoefficients\tStandard Error\tUnits\tFixed"),"coefficient units column");
        check(report.contains("R Square (centered)"),"centered R2 retained");
        report=CurveFitReport.create(redundant,data,null,null,true,true,"s","m",estimated,Double.NaN);
        check(report.contains("not independently identifiable"),"rank warning exposed");
    }
}
