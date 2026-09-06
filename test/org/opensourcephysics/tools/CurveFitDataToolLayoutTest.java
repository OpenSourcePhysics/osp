package org.opensourcephysics.tools;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import javax.swing.*;
import org.opensourcephysics.display.Dataset;

/** Integration test: real Data Tool, plot/fitter split, font and window changes. */
public class CurveFitDataToolLayoutTest {
 static int passed;
 static boolean success;
 public static void main(String[] args)throws Exception {
  SwingUtilities.invokeAndWait(()->{try{
   Dataset data=new Dataset(); data.setXYColumnNames("t","y");
   for(int i=0;i<16;i++){double t=i/30.0;data.append(t,-1980*t*t+1067*t-3.9+0.4*Math.sin(i));}
   DataTool tool=new DataTool(data); tool.addNotify();
   DataToolTab tab=tool.getTab(0); tab.checkGUI();
   DatasetCurveFitter fitter=tab.getCurveFitter();
   tab.showFitterAction.actionPerformed(new ActionEvent(tab,0,fitter.getPolyFitNameOfDegree(2)));
   java.lang.reflect.Field sf=DatasetCurveFitter.class.getDeclaredField("statisticsPanel");sf.setAccessible(true);JPanel stats=(JPanel)sf.get(fitter);
   JScrollPane scroll=(JScrollPane)fitter.splitPane.getRightComponent();JTable table=(JTable)scroll.getViewport().getView();
   for(int level:new int[]{0,2}){
    tool.setFontLevel(level);
    for(int[] size:new int[][]{{950,700},{1600,1000},{950,700}}){
     tool.setSize(size[0],size[1]); tool.validate();
     tab.splitPanes[0].setDividerLocation(0.7);
     for(int i=0;i<5;i++){tool.validate();layout(tool.getContentPane());}
     int stableDivider=tab.splitPanes[1].getDividerLocation();
     int stableOrientation=fitter.splitPane.getOrientation();
     for(int count:new int[]{1,2,3,8,15,16,1,16}){
      Dataset selected=new Dataset();selected.setXYColumnNames("t","y");
      for(int j=0;j<count;j++){double t=j/30.0;selected.append(t,-1980*t*t+1067*t-3.9+0.4*Math.sin(j));}
      tab.setSelectedData(selected,true);
      for(int pass=0;pass<5;pass++){tool.validate();layout(tool.getContentPane());}
      check(tab.splitPanes[1].getDividerLocation()==stableDivider,"point selection preserves plot height: "+count);
      check(fitter.splitPane.getOrientation()==stableOrientation,"point selection preserves parameter placement: "+count);
     }
     check(table.getRowCount()==3,"parabola parameter rows present");
     for(int r=0;r<3;r++){
      Rectangle cell=table.getCellRect(r,2,true);
      int w=table.prepareRenderer(table.getCellRenderer(r,2),r,2).getPreferredSize().width;
      Rectangle text=new Rectangle(cell.x,cell.y,w,cell.height);
      check(table.getVisibleRect().contains(text),"parameter visible in nested viewport, font="+level+", width="+size[0]+", row="+r);
      Rectangle inFitter=SwingUtilities.convertRectangle(table,text,fitter);
      check(fitter.getVisibleRect().contains(inFitter),"parameter visible in parent fit area");
     }
     Rectangle parameterBottom=SwingUtilities.convertRectangle(table,table.getCellRect(2,2,true),fitter);
     Rectangle statsBounds=SwingUtilities.convertRectangle(stats,new Rectangle(0,0,stats.getWidth(),stats.getHeight()),fitter);
     check(statsBounds.y>=parameterBottom.y+parameterBottom.height,"parameter information above statistics");
     check(fitter.getVisibleRect().contains(statsBounds),"all statistics visible in parent fit area");
     check(stats.getHeight()==stats.getPreferredSize().height,"statistics remain compact");
     java.lang.reflect.Field uf=DatasetCurveFitter.class.getDeclaredField("uncertaintyPanel");uf.setAccessible(true);JPanel uncertainty=(JPanel)uf.get(fitter);
     Rectangle uncertaintyBounds=SwingUtilities.convertRectangle(uncertainty,new Rectangle(0,0,uncertainty.getWidth(),uncertainty.getHeight()),fitter);
     check(fitter.getVisibleRect().contains(uncertaintyBounds),"uncertainty controls visible in fit area");
     check(tab.splitPanes[1].getTopComponent().getHeight()>100,"plot retains usable height");
     check(Math.abs(fitter.getHeight()-fitter.prepareFitLayout(fitter.getWidth()))<=4,"fit area stays at required height");
     if(args.length>0){BufferedImage image=new BufferedImage(tool.getContentPane().getWidth(),tool.getContentPane().getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();tool.getContentPane().printAll(g);g.dispose();javax.imageio.ImageIO.write(image,"png",new java.io.File(args[0]+"/data-tool-"+size[0]+"-font"+level+".png"));}
    }
   }
   System.out.println("Passed: "+passed); success=true;
  }catch(Exception e){throw new RuntimeException(e);}finally{System.exit(success ? 0 : 1);}});
  System.exit(0);
 }
 static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child);}
 static void check(boolean b,String message){if(!b){System.err.println("FAIL: "+message);System.exit(1);}passed++;System.out.println("PASS: "+message);}
}
