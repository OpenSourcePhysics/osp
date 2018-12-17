/*
 * The org.opensourcephysics.media.core package defines the Open Source Physics
 * media framework for working with video and other media.
 *
 * Copyright (c) 2017  Douglas Brown and Wolfgang Christian.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * For additional information and documentation on Open Source Physics,
 * please see <http://www.opensourcephysics.org/>.
 */
package org.opensourcephysics.media.core;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;

import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.tools.DatasetCurveFitterNoGUI;
import org.opensourcephysics.tools.UserFunction;

/**
 * A class to find the best match of a template image in a target image.
 * The match location is estimated to sub-pixel accuracy by assuming the
 * distribution of match scores near a peak is Gaussian.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class TemplateMatcher {

	// static constants
	private static final double LARGE_NUMBER = 1.0E10;


	// static fields for Gaussian fit
	// It takes too much time to re-create them
	private static DatasetCurveFitterNoGUI fitter; // used for Gaussian fit
	private static Dataset dataset; // used for Gaussian fit
	private static UserFunction fGaussian; // used for Gaussian fit

	static {
		long startTime2 = System.currentTimeMillis();
		// set up the Gaussian curve fitter
		dataset = new Dataset();

		long endTime2 = System.currentTimeMillis();
		System.out.println("Creating new dataset, ms:");
		System.out.println(endTime2 - startTime2);
		startTime2 = System.currentTimeMillis();


		fitter = new DatasetCurveFitterNoGUI(dataset, null);

		endTime2 = System.currentTimeMillis();
		System.out.println("Building fitter, ms:");
		System.out.println(endTime2 - startTime2);

		long startTime = System.currentTimeMillis();
		fGaussian = new UserFunction("gaussian"); //$NON-NLS-1$
		fGaussian.setParameters(new String[]{"a", "b", "c"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				new double[]{1, 0, 1});
		fGaussian.setExpression("a*exp(-(x-b)^2/c)", new String[]{"x"}); //$NON-NLS-1$ //$NON-NLS-2$
		long endTime = System.currentTimeMillis();
		System.out.println("Building Gaussian filter, ms:");
		System.out.println(endTime - startTime);

	}


  // instance fields
  private BufferedImage original, template, working, match;
  private Shape mask;
  private int[] pixels, templateR, templateG, templateB;
  private boolean[] isPixelTransparent;
  private int[] targetPixels, matchPixels;
  private int wTemplate, hTemplate; // width and height of the template image
  private int wTarget, hTarget; // width and height of the target image
  private int wTest, hTest; // width and height of the tested image (in search rect)
  private TPoint p = new TPoint(); // for general use in methods
  private double largeNumber = 1.0E20; // bigger than any expected difference
  private long largeLong = 9223372036854775807L; // bigger than any expected difference
  private double[] pixelOffsets = {-1, 0, 1}; // used for Gaussian fit
  private double[] xValues = new double[3]; // used for Gaussian fit
  private double[] yValues = new double[3]; // used for Gaussian fit
	private boolean convexMask = false; // is the mask guaranteed to be convex?
	private int[] convexMaskStart = null;
	private int[] convexMaskEnd   = null;
	private int[] quantityOfTransparentPixels = null;
	private double[] columnExtrapolationCoefficients = null;
  private double peakHeight, peakWidth; // peak height and width of most recent match
  private int trimLeft, trimTop;
  private int[] alphas = new int[2]; // most recent alphas {input, original}
  private int index; // for AutoTracker--not used internally
	public long totalOpaquePixels;

	/**
   * Constructs a TemplateMatcher object. If a mask shape is specified, then
   * only pixels that are entirely inside the mask are included in the template.
   *
   * @param templateImage the image to match
   * @param maskShape a shape to define inside pixels (may be null)
   */
  public TemplateMatcher(BufferedImage templateImage, Shape maskShape) {
	  mask = maskShape;
	  setTemplate(templateImage);
	  if(mask instanceof Ellipse2D || mask instanceof RoundRectangle2D || mask instanceof Rectangle2D){
		declareMaskAsConvex();
	  }

  }

  /**
   * Sets the template to be used for the next search.
   * The new template dimensions must match those of the previous template.
   *
   * @param image the template image
   */
  public void setTemplate(BufferedImage image) {
  	if (template!=null && image.getType()==BufferedImage.TYPE_INT_ARGB
  			&& wTemplate==image.getWidth() && hTemplate==image.getHeight())
  	{
	  	template = image;
	    template.getRaster().getDataElements(0, 0, wTemplate, hTemplate, pixels);

		// set up rgb and transparency arrays for fast matching
		for (int i = 0; i < pixels.length; i++) {
			int val = pixels[i];
			isPixelTransparent[i] = getAlpha(val)==0;		// alpha
			//if (!isPixelTransparent[i]) {
			templateR[i] = getRed(val);        // red
			templateG[i] = getGreen(val);        // green
			templateB[i] = getBlue(val);        // blue
			//}
		}
  	}
  	else {
  		original = BufferedImageUtils.convertIfNeeded(image, BufferedImage.TYPE_INT_ARGB);
    	template = buildTemplate(original, 255, 0); // builds from scratch
    	setTemplate(template);
  	}
  }

  /**
   * Gets the template. Includes only pixels inside the mask.
   *
   * @return the template
   */
  public BufferedImage getTemplate() {
  	if (template == null) {
    	template = buildTemplate(original, 255, 0); // builds from scratch
    	setTemplate(template);
  	}
  	return template;
  }

  /**
   * Builds the template from an input image.
   * The input image dimensions must match the original.
   * The input and original are overlaid onto the working image, from which
   * the template is generated. Pixels that fall outside the mask are ignored
   * in the f (?) template.
   *
   * @param image the input image
   * @param alphaInput the opacity with which the input image is overlaid (0-255)
   * @param alphaOriginal the opacity with which the original image is overlaid (0-255)
   * @return the template
   */
  public BufferedImage buildTemplate(BufferedImage image, int alphaInput, int alphaOriginal) {
	  //System.out.println("Building a template...");
	  int w = image.getWidth();
	  int h = image.getHeight();
	  // return if image dimensions do not match original image
	  if (original.getWidth() != w || original.getHeight() != h)
		  return null;
	  // return existing if both alphas are zero
	  if (alphaInput == 0 && alphaOriginal == 0)
		  return template != null ? template : original;
	  // save alphas
	  alphas[0] = alphaInput;
	  alphas[1] = alphaOriginal;
	  // set up argb input image
	  BufferedImage input = BufferedImageUtils.convertIfNeeded(image, BufferedImage.TYPE_INT_ARGB);
	  // create working image if needed
	  if (working == null) {
		  working = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	  }
	  // reset template dimensions and create new template if needed
	  if (template == null || w != wTemplate || h != hTemplate) {
		  wTemplate = w;
		  hTemplate = h;
		  int len = w * h;
		  template = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		  pixels = new int[len];
	  }
	  // set alpha of input and draw onto working
	  Graphics2D gWorking = working.createGraphics();
	  alphaInput = Math.max(0, Math.min(255, alphaInput));
	  if (alphaInput > 0) { // overlay only if not transparent
		  gWorking.setComposite(getComposite(alphaInput));
		  gWorking.drawImage(input, 0, 0, null);
	  }
	  // set alpha of original and draw onto working
	  alphaOriginal = Math.max(0, Math.min(255, alphaOriginal));
	  if (alphaOriginal > 0) { // overlay only if not transparent
		  gWorking.setComposite(getComposite(alphaOriginal));
		  gWorking.drawImage(original, 0, 0, null);
	  }
	  // read pixels from working raster
	  working.getRaster().getDataElements(0, 0, wTemplate, hTemplate, pixels);

		makeOuterPixelsTransparent();

	  // write pixels to template raster
	  template.getRaster().setDataElements(0, 0, wTemplate, hTemplate, pixels);

	  trimTransparentEdgesFromTemplate();
	  countColumnExtrapolationCoefficients();
	  initializeArrays();

	  return template;
  }

	/**
	 * Create auxiliary arrays with proper length
	 */
  	private void initializeArrays(){
		int len = wTemplate * hTemplate;
		templateR = new int[len];
		templateG = new int[len];
		templateB = new int[len];
		isPixelTransparent = new boolean[len];
		matchPixels = new int[len];
	}

	/**
	 * Trims transparent edges from the template
	 */
	private void trimTransparentEdgesFromTemplate(){
		int trimRight = 0, trimBottom = 0;
		trimLeft = trimTop = 0;

		int trim;
		// left edge
		for(trim = 0; trim < wTemplate; trim++){
			if(quantityOfTransparentPixels[trim] != hTemplate){
				break;
			}
		}
		trimLeft = trim;

		// right edge
		for(trim = wTemplate - 1; trim >= 0; trim--){
			if(quantityOfTransparentPixels[trim] != hTemplate){
				break;
			}
		}
		trimRight = wTemplate - trim - 1;


		// TODO: probe a point in the middle of an edge
		// top edge
		boolean transparentEdge = true;
		while (transparentEdge && trimTop < hTemplate) {
			for (int col = 0; col < wTemplate  && transparentEdge; col++) {
				int i = trimTop * wTemplate + col;
				transparentEdge = transparentEdge && getAlpha(pixels[i]) == 0;
			}
			if (transparentEdge) trimTop++;
		}
		// bottom edge
		transparentEdge = true;
		while (transparentEdge && (trimTop + trimBottom) < hTemplate) {
			for (int col = 0; col < wTemplate && transparentEdge; col++) {
				int i = (hTemplate - 1 - trimBottom) * wTemplate + col;
				transparentEdge = transparentEdge && getAlpha(pixels[i]) == 0;
			}
			if (transparentEdge) trimBottom++;
		}
		// reduce size of template if needed
		if(trimLeft + trimRight != 0) {
			quantityOfTransparentPixels = Arrays.copyOfRange(quantityOfTransparentPixels, trimLeft,wTemplate - trimRight);
		}
		if(trimTop + trimBottom != 0) {
			for(int i = 0; i < quantityOfTransparentPixels.length; i++){
				quantityOfTransparentPixels[i] -= trimTop + trimBottom;
			}
		}
		if (trimLeft + trimRight + trimTop + trimBottom > 0) {
			wTemplate -= (trimLeft + trimRight);
			hTemplate -= (trimTop + trimBottom);
			wTemplate = Math.max(wTemplate, 1);
			hTemplate = Math.max(hTemplate, 1);
			pixels = new int[wTemplate * hTemplate];
			BufferedImage bi = new BufferedImage(wTemplate, hTemplate, BufferedImage.TYPE_INT_ARGB);
			bi.createGraphics().drawImage(template, -trimLeft, -trimTop, null);
			template = bi;
			/* TODO: avoid redrawing (which is rather slow operation), maybe when a flag is up.
				It should be enough to rebuild pixels and tune wTemplate and hTemplate
			 */
			template.getRaster().getDataElements(0, 0, wTemplate, hTemplate, pixels);
		}
	}

	/**
	 * For each column determines the number of pixels which are transparent.
	 * Used when cropping left and right edges and when extrapolating the difference.
	 */
	private void countColumnExtrapolationCoefficients(){
		columnExtrapolationCoefficients = new double[wTemplate];
		int sum = 0;
		for (int i = 0; i < wTemplate; i++){
			sum += hTemplate - quantityOfTransparentPixels[i];
			columnExtrapolationCoefficients[i] = sum;
		}
		totalOpaquePixels = sum;
		for(int i = 0; i < wTemplate; i++){
			columnExtrapolationCoefficients[i] = sum / columnExtrapolationCoefficients[i];
		}
	}

	/**
	 * Makes all the pixels outside mask transparent and black (because they do not matter)
	 */
	private void makeOuterPixelsTransparent(){
		//TODO: refactor this to respect convex mask declaration

		quantityOfTransparentPixels = new int [wTemplate];
		if (mask == null) {
			return;
		}
		for (int x = 0; x < wTemplate; x++){
			//TODO: is the following initialization necessary in Java?
			quantityOfTransparentPixels[x] = 0;
			for(int y = 0; y < hTemplate; y++){
				if (!(
						mask.contains(x  , y  ) &&
						mask.contains(x  , y+1) &&
						mask.contains(x+1, y  ) &&
						mask.contains(x+1, y+1)
				)) {
					pixels[y * wTemplate + x] = 0; // set alpha to zero (transparent)
					quantityOfTransparentPixels[x]++;
				}
			}
		}
	}

	/**
	 * Shifts the colors of the template in such way that the average color value for template
	 * is equal to average color value of given image (probably match)
	 */
	public void shiftTemplate(BufferedImage example, int[] maxShift){
		// Suppose pixels have been already built
		int len = wTemplate * hTemplate;
		int[] examplePixels = new int[len];
		example.getRaster().getDataElements(0, 0, wTemplate, hTemplate, examplePixels);

		long tR = 0, tG = 0, tB = 0, eR = 0, eB = 0, eG = 0;

		for(int i = 0; i < len; i++){
			if(!isPixelTransparent[i]){
				tB += (pixels[i]      ) & 0xff;
				tG += (pixels[i] >>  8) & 0xff;
				tR += (pixels[i] >> 16) & 0xff;
				eB += (examplePixels[i]      ) & 0xff;
				eG += (examplePixels[i] >>  8) & 0xff;
				eR += (examplePixels[i] >> 16) & 0xff;
			}
		}
		int[] shift = {
				(int)(1.0 * (eR - tR) / totalOpaquePixels),
				(int)(1.0 * (eG - tG) / totalOpaquePixels),
				(int)(1.0 * (eB - tB) / totalOpaquePixels),
		};
		shiftTemplate(shift, maxShift);
	}

	public void shiftTemplate(int[] shift, int[] maxShift){
		for(int j = 0; j < 3; j++) {
			maxShift[j] = Math.abs(maxShift[j]);
			if (shift[j] > maxShift[j]) {
				shift[j] = maxShift[j];
			} else if (shift[j] < -maxShift[j]) {
				shift[j] = -maxShift[j];
			}
		}
		shiftTemplate(shift);
	}

	public void shiftTemplate(int[] shift){
		if(shift[0] == 0 && shift[1] == 0 && shift[2] == 0){
			return;
		}

		int len = wTemplate * hTemplate;

		for(int i = 0; i < len; i++){
			if(!isPixelTransparent[i]){
				int pixel = pixels[i];
				int r = Math.max(Math.min(((pixel >>> 16) & 0xff) + shift[0], 255),0);
				int g = Math.max(Math.min(((pixel >>>  8) & 0xff) + shift[1], 255),0);
				int b = Math.max(Math.min(((pixel       ) & 0xff) + shift[2], 255),0);
				int a = pixel & 0xff000000;
				pixels[i] = a | (r << 16) | (g << 8) | b;
			}
		}
		template.getRaster().setDataElements(0, 0, wTemplate, hTemplate, pixels);
	}

	/**
	 * Guarantees that the mask is convex and builds the appropriate limits
	 */
  public void declareMaskAsConvex(){
  	convexMask = true;
  	convexMaskStart = new int[wTemplate];
  	convexMaskEnd   = new int[wTemplate];
  	for(int i = 0; i < wTemplate; i++){
  		int start = 0;
  		while(start < hTemplate && isPixelTransparent[start*wTemplate + i]){
  			start++;
		}
  		convexMaskStart[i] = start;

		int end = hTemplate - 1;
		while(end >= start && isPixelTransparent[end*wTemplate + i]){
			end--;
		}
		convexMaskEnd[i] = end + 1;
	}
  }

  /**
   * Gets the alphas used to build the most recent template.
   *
   * @return int[] {alphaInput, alphaOriginal}
   */
  public int[] getAlphas() {
  	return alphas;
  }

  /**
   * Sets the index.
   *
   * @param index the index
   */
  public void setIndex(int index) {
  	this.index = index;
  }

  /**
   * Gets the index.
   * Note: index is not set internally, but only with calls to setIndex().
   *
   * @return the index
   */
  public int getIndex() {
  	return index;
  }

  /**
   * Gets the working image pixels used to generate the template.
   *
   * @param pixels int[] of pixels. If null, it will be created
   * @return the filled pixels array
   */
  public int[] getWorkingPixels(int[] pixels) {
  	if (pixels==null || pixels.length!=wTemplate*hTemplate) {
  		pixels = new int[wTemplate*hTemplate];
  	}
  	working.getRaster().getDataElements(0, 0, wTemplate, hTemplate, pixels);
  	return pixels;
  }

  /**
   * Sets the working image pixels used to generate the template.
   *
   * @param pixels int[] of pixels
   */
  public void setWorkingPixels(int[] pixels) {
  	if (pixels!=null && pixels.length==wTemplate*hTemplate)
  		working.getRaster().setDataElements(0, 0, wTemplate, hTemplate, pixels);
  }

  /**
   * Gets the template location at which the best match occurs in a rectangle.
   * May return null.
   *
   *  Template matching process:
   *  1. At each test position in the search area, find the RGB square deviation ("RGBSqD": sum
   *  			of squares of rgb differences of all pixels) between the template and video image.
   *  			Note that the RGBSqD is zero for a perfect match and larger for poorer matches.
   * 	2. Determine the average RGBSqD for all test positions.
   *  3. Define the position for which the RGBSqD is minimum as the "working" best match.
   *  			Define the peak height ("PH") of this match to be PH = (avgRGBSqD/matchRGBSqD)-1.
   *  			Note that the PH may vary from zero to infinity.
   *  4. If the PH exceeds the "Automark" setting, the match is deemed to be a good one
   *  			(i.e., significantly better than average).
   *  5. For sub-pixel accuracy, fit a Gaussian curve to the PHs of the working best match
   *  			and its immediate vertical and horizontal neighbors. Note that the 3-point Gaussian
   *  			fits should be exact.
   *  6. The final best match (sub-pixel) is the position of the peak of the Gaussian fit.
   *  7. Note that the width of the Gaussian fit is probably correlated with the uncertainty of
   *  			the match position, but it is not used to explicitly estimate this uncertainty.
   *
   * @param target the image to search
   * @param searchRect the rectangle to search within the target image
   * @return the optimized template location at which the best match, if any, is found
   */
  public TPoint getMatchLocation(BufferedImage target, Rectangle searchRect) {

    wTarget = target.getWidth();
    hTarget = target.getHeight();
    // determine insets needed to accommodate template
    int left = wTemplate/2, right = left;
    if (wTemplate%2>0) right++;
    int top = hTemplate/2, bottom = top;
    if (hTemplate%2>0) bottom++;
    // trim search rectangle if necessary
  	searchRect.x = Math.max(left, Math.min(wTarget-right, searchRect.x));
  	searchRect.y = Math.max(top, Math.min(hTarget-bottom, searchRect.y));
  	searchRect.width = Math.min(wTarget-searchRect.x-right, searchRect.width);
  	searchRect.height = Math.min(hTarget-searchRect.y-bottom, searchRect.height);
  	if (searchRect.width <= 0 || searchRect.height <= 0) {
  		peakHeight = Double.NaN;
  		peakWidth = Double.NaN;
  		return null;
  	}
  	// set up test pixels to search (rectangle plus template)
  	int xMin = Math.max(0, searchRect.x-left);
  	int xMax = Math.min(wTarget, searchRect.x+searchRect.width+right);
  	int yMin = Math.max(0, searchRect.y-top);
  	int yMax = Math.min(hTarget, searchRect.y+searchRect.height+bottom);
  	wTest = xMax-xMin;
  	hTest = yMax-yMin;
  	target = BufferedImageUtils.convertIfNeeded(target, BufferedImage.TYPE_INT_RGB);
    targetPixels = new int[wTest * hTest];
    target.getRaster().getDataElements(xMin, yMin, wTest, hTest, targetPixels);
    // find the rectangle point with the minimum difference
    long matchDiff = largeLong; // larger than typical differences
    int xMatch=0, yMatch=0;
    double avgDiff = 0;


    for (int dx = 0; dx < 4; dx++){
    	for (int dy = 0; dy < 4; dy++){
			for (int x = dx; x <= searchRect.width; x += 4) {
				for (int y = dy; y <= searchRect.height; y += 4) {
					long diff = convexMask?
							getDifferenceAtTestPointConvex(x, y, matchDiff):
							getDifferenceAtTestPoint(x, y, matchDiff);
					avgDiff += diff;
					if (diff < matchDiff) {
						matchDiff = diff;
						xMatch = x;
						yMatch = y;
					}
				}
			}
		}
	}



	  avgDiff /= (searchRect.width * searchRect.height);
	  peakHeight = avgDiff / matchDiff - 1;
	  peakWidth = Double.NaN;
	  double dx = 0, dy = 0;
	  // if match is not exact, fit a Gaussian and find peak
	  if (!Double.isInfinite(peakHeight)) {
		  // fill data arrays
		  xValues[1] = yValues[1] = peakHeight;
		  for (int i: new int[]{-1,1}) {
			  double diff = getDifferenceAtTestPoint(xMatch + i, yMatch);
			  xValues[i + 1] = avgDiff / diff - 1;
			  diff = getDifferenceAtTestPoint(xMatch, yMatch + i);
			  yValues[i + 1] = avgDiff / diff - 1;
		  }
		  double[] est = estimateGaussian(xValues);
		  dx = est[0];
		  double wx = est[1];
		  est = estimateGaussian(yValues);
		  dy = est[0];
		  double wy = est[1];

		  // set x parameters and fit to x data
		  est = fitGaussian(pixelOffsets, xValues, dx, wx);
		  dx = est[0];
		  peakWidth = est[1];

		  if (!Double.isNaN(peakWidth)) {
		  	// set y parameters and fit to y data
			est = fitGaussian(pixelOffsets, yValues, dy, wy);
			dy = est[0];
			peakWidth = (peakWidth + est[1])/2;
			if (est[2] > 0.01) {
				peakWidth = Double.NaN;
			}
		  }
	  }
	  xMatch = xMatch + searchRect.x - left - trimLeft;
	  yMatch = yMatch + searchRect.y - top - trimTop;
	  refreshMatchImage(target, xMatch, yMatch);
	  return new TPoint(xMatch + dx, yMatch + dy);
  }

	/**
	 * Estimates offset dx and width wx of gaussian
	 * using three points
	 */
	private double[] estimateGaussian(double[] values){
		// estimate offset dx of gaussian
		double pull = 1 / (values[1] - values[0]);
		double push = 1 / (values[1] - values[2]);
		if (Double.isNaN(pull)) pull = LARGE_NUMBER;
		if (Double.isNaN(push)) push = LARGE_NUMBER;
		double dx = 0.6 * (push - pull) / (push + pull);
		// estimate width wx of gaussian
		double ratio = dx > 0 ? peakHeight / values[0] : peakHeight / values[2];
		double wx = dx > 0 ? dx + 1 : dx - 1;
		wx = wx * wx / Math.log(ratio);
		return new double[]{dx, wx};
	}

	/**
	 * Fits a gaussian
	 */
	private double[] fitGaussian(double[] offsets, double[] values, double dx, double wx){
		dataset.clear();
		dataset.append(offsets, values);
		double rmsDev = 1;
		for (int k = 0; k < 3; k++) {
			double c = k == 0 ? wx : k == 1 ? wx / 3 : wx * 3;
			fGaussian.setParameterValue(0, peakHeight);
			fGaussian.setParameterValue(1, dx);
			fGaussian.setParameterValue(2, c);
			rmsDev = fitter.fit(fGaussian);
			if (rmsDev < 0.01) { // fitter succeeded (3-point fit should be exact)
				return new double[]{fGaussian.getParameterValue(1),fGaussian.getParameterValue(2),rmsDev};
			}
		}
		return new double[]{dx,peakWidth,rmsDev};
	}


	/**
   * Refreshes the match image.
   *
   * @param target the matched image
   * @param x the match x-position
   * @param y the match y-position
   */
  private void refreshMatchImage(BufferedImage target, int x, int y) {
    target.getRaster().getDataElements(x+1, y+1, wTemplate, hTemplate, matchPixels);
    for (int i = 0; i< matchPixels.length; i++) {
  		matchPixels[i] = getValue(isPixelTransparent[i]? 0: 255, matchPixels[i]);
//    	if (!isPixelTransparent[i])
//    		matchPixels[i] = getValue(255, matchPixels[i]);
//    	else {
//    		matchPixels[i] = getValue(0, matchPixels[i]);
//    	}
    }
    if (match==null || match.getWidth()!=wTemplate || match.getHeight()!=hTemplate) {
    	match = new BufferedImage(wTemplate, hTemplate, BufferedImage.TYPE_INT_ARGB);
    }
    match.getRaster().setDataElements(0, 0, wTemplate, hTemplate, matchPixels);
  }

  /**
   * Gets the template location at which the best match occurs in a
   * rectangle and along a line. May return null.
   *
   * @param target the image to search
   * @param searchRect the rectangle to search within the target image
   * @param x0 the x-component of a point on the line
   * @param y0 the y-component of a point on the line
   * @param theta the angle of the line
   * @param spread the spread of the line (line width = 1+2*spread)
   * @return the optimized template location of the best match, if any
   */
  public TPoint getMatchLocation(BufferedImage target, Rectangle searchRect,
  		double x0, double y0, double theta, int spread) {

	//TODO: tests for this!

    wTarget = target.getWidth();
    hTarget = target.getHeight();
    // determine insets needed to accommodate template
    int left = wTemplate/2, right = left;
    if (wTemplate%2>0) right++;
    int top = hTemplate/2, bottom = top;
    if (hTemplate%2>0) bottom++;

    // trim search rectangle if necessary
  	searchRect.x = Math.max(left, Math.min(wTarget-right, searchRect.x));
  	searchRect.y = Math.max(top, Math.min(hTarget-bottom, searchRect.y));
  	searchRect.width = Math.min(wTarget-searchRect.x-right, searchRect.width);
  	searchRect.height = Math.min(hTarget-searchRect.y-bottom, searchRect.height);
  	if (searchRect.width <= 0 || searchRect.height <= 0) { // not able to search
  		peakHeight = Double.NaN;
  		peakWidth = Double.NaN;
  		return null;
  	}
  	// set up test pixels to search (rectangle plus template)
  	int xMin = Math.max(0, searchRect.x-left);
  	int xMax = Math.min(wTarget, searchRect.x+searchRect.width+right);
  	int yMin = Math.max(0, searchRect.y-top);
  	int yMax = Math.min(hTarget, searchRect.y+searchRect.height+bottom);
  	wTest = xMax-xMin;
  	hTest = yMax-yMin;
  	target = BufferedImageUtils.convertIfNeeded(target, BufferedImage.TYPE_INT_RGB);
    targetPixels = new int[wTest * hTest];
    target.getRaster().getDataElements(xMin, yMin, wTest, hTest, targetPixels);
    // get the points to search along the line
    ArrayList<Point2D> searchPts = getSearchPoints(searchRect, x0, y0, theta);
    if (searchPts==null) { // not able to search
  		peakHeight = Double.NaN;
  		peakWidth = -1;
  		return null;
    }
    // collect differences in a map as they are measured
    HashMap<Point2D, Double> diffs = new HashMap<Point2D, Double>();
    // find the point with the minimum difference from template
    double matchDiff = largeNumber; // larger than typical differences
    int xMatch=0, yMatch=0;
    double avgDiff = 0;
    Point2D matchPt = null;
  	for (Point2D pt: searchPts) {
  		int x = (int)pt.getX();
  		int y = (int)pt.getY();
  		double diff = getDifferenceAtTestPoint(x, y);
  		diffs.put(pt, diff);
  		avgDiff += diff;
  		if (diff < matchDiff) {
  			matchDiff = diff;
  			xMatch = x;
  			yMatch = y;
  			matchPt = pt;
  		}
  	}
  	avgDiff /= searchPts.size();
		peakHeight = avgDiff/matchDiff-1;
		peakWidth = Double.NaN;
		double dl = 0;
		int matchIndex = searchPts.indexOf(matchPt);

		// if match is not exact, fit a Gaussian and find peak
		if (!Double.isInfinite(peakHeight) && matchIndex>0 && matchIndex<searchPts.size()-1) {
			// fill data arrays
			Point2D pt = searchPts.get(matchIndex-1);
			double diff = diffs.get(pt);
	  	xValues[0] = -pt.distance(matchPt);
	  	yValues[0] = avgDiff/diff-1;
	  	xValues[1] = 0;
	  	yValues[1] = peakHeight;
			pt = searchPts.get(matchIndex+1);
			diff = diffs.get(pt);
	  	xValues[2] = pt.distance(matchPt);
	  	yValues[2] = avgDiff/diff-1;

  		// determine approximate offset (dl) and width (w) values
  		double pull = -xValues[0]/(yValues[1]-yValues[0]);
  		double push = xValues[2]/(yValues[1]-yValues[2]);
  		if (Double.isNaN(pull)) pull=LARGE_NUMBER;
  		if (Double.isNaN(push)) push=LARGE_NUMBER;
  		dl = 0.3*(xValues[2]-xValues[0])*(push-pull)/(push+pull);
			double ratio = dl>0? peakHeight/yValues[0]: peakHeight/yValues[2];
			double w = dl>0? dl-xValues[0]: dl-xValues[2];
			w = w*w/Math.log(ratio);

			// set parameters and fit to x data
			double[] est = fitGaussian(xValues, yValues, dl, w);
			dl = est[0];
			peakWidth = est[1];
		}
		double dx = dl*Math.cos(theta);
		double dy = dl*Math.sin(theta);
		xMatch = xMatch+searchRect.x-left-trimLeft;
		yMatch = yMatch+searchRect.y-top-trimTop;
		refreshMatchImage(target, xMatch, yMatch);
		return new TPoint(xMatch+dx, yMatch+dy);
  }

  /**
   * Gets the most recent match image.
   *
   * @return the best match image
   */
  public BufferedImage getMatchImage() {
  	return match;
  }

  /**
   * Returns the width and height of the peak for the most recent match.
   * The peak height is the ratio meanSqPixelDiff/matchSqPixelDiff.
   * The peak width is the mean of the vertical and horizontal Gaussian fit widths.
   * This data can be used to determine whether a match is acceptable.
   * A peak height greater than 5 is a reasonable standard for acceptability.
   *
   * Special cases:
   * 1. If the match is perfect, then the height is infinite and the width NaN.
   * 2. If the searchRect fell outside the target image, then no match was
   *    possible and both the width and height are NaN.
   * 3. If there were no points to search along the 1D x-axis path, then
   *    the height is NaN and the width is negative.
   * 4. If the Gaussian fit optimization was not successful (either horizontally
   *    or vertically) then the height is finite and the width is NaN.
   *
   * @return double[2] {mean Gaussian width, height}
   */
  public double[] getMatchWidthAndHeight() {
  	return new double[] {peakWidth, peakHeight};
  }

  /**
   * Method to get the color value
   *
   * @param a 0-255 alpha
   * @param argb current color value
   * @return the integer value
   */
  public static int getValue(int a, int argb) {
  	int r = getRed(argb);
  	int g = getGreen(argb);
   	int b = getBlue(argb);
   	return getValue(a, r, g, b);
  }

  /**
   * Method to get the color value
   *
   * @param a 0-255 alpha
   * @param r 0-255 red
   * @param g 0-255 green
   * @param b 0-255 blue
   * @return the integer value
   */
  public static int getValue(int a, int r, int g, int b) {
  	 int value = (a << 24) + (r << 16) + (g << 8) + b;
     return value;
  }

  /**
   * Method to get the alpha component from a color value
   *
   * @param value the color value
   * @return 0-255 alpha component
   */
  public static int getAlpha(int value) {
  	int alpha = (value >> 24) & 0xff;
    return alpha;
  }

  /**
   * Method to get the red component from a color value
   *
   * @param value the color value
   * @return 0-255 red component
   */
  public static int getRed(int value) {
    int red = (value >> 16) & 0xff;
    return red;
  }

  /**
   * Method to get the green component from a color value
   *
   * @param value the color value
   * @return 0-255 green component
   */
  public static int getGreen(int value) {
    int green = (value >> 8) & 0xff;
    return green;
  }

  /**
   * Method to get the blue component from a color value
   *
   * @param value the color value
   * @return 0-255 blue component
   */
  public static int getBlue(int value) {
    int blue = value & 0xff;
    return blue;
  }

//_____________________________ private methods _______________________

  /**
   * Gets a list of Point2D objects that lie within pixels in a rectangle
   * and along a line.
   *
   * @param searchRect the rectangle
   * @param x0 the x-component of a point on the line
   * @param y0 the y-component of a point on the line
   * @param theta the angle of the line
   * @return a list of Point2D
   */
  public ArrayList<Point2D> getSearchPoints(Rectangle searchRect,
  		double x0, double y0, double theta) {
  	double slope = -Math.tan(theta);
  	// create line to search along
  	Line2D line = new Line2D.Double();
  	if (Math.abs(slope)>LARGE_NUMBER) { // vertical axis
  		line.setLine(x0, y0, x0, y0+1);
  	}
  	else if (Math.abs(slope)<1/LARGE_NUMBER) { // horizontal axis
  		line.setLine(x0, y0, x0+1, y0);
  	}
  	else {
    	line.setLine(x0, y0, x0+1, y0+slope);
  	}
  	// create intersection points (to set line ends)
  	Point2D p1 = new Point2D.Double();
  	Point2D p2 = new Point2D.Double(Double.NaN, Double.NaN);
  	Point2D p = p1;
  	boolean foundBoth = false;
  	double d = searchRect.x;
  	Object[] data = getDistanceAndPointAtX(line, d);
  	if (data!=null) {
	  	p.setLocation((Point2D)data[1]);
			if (p.getY()>=searchRect.y && p.getY()<=searchRect.y+searchRect.height) {
				// line end is left edge
				p = p2;
			}
  	}
		d += searchRect.width;
  	data = getDistanceAndPointAtX(line, d);
  	if (data!=null) {
	  	p.setLocation((Point2D)data[1]);
			if (p.getY()>=searchRect.y && p.getY()<=searchRect.y+searchRect.height) {
				// line end is right edge
				if (p==p1) p = p2;
				else foundBoth = true;
			}
  	}
		if (!foundBoth) {
	  	d = searchRect.y;
	  	data = getDistanceAndPointAtY(line, d);
	  	if (data!=null) {
		  	p.setLocation((Point2D)data[1]);
				if (p.getX()>=searchRect.x && p.getX()<=searchRect.x+searchRect.width) {
					// line end is top edge
					if (p==p1) p = p2;
					else if (!p1.equals(p2)) foundBoth = true;
				}
	  	}
		}
		if (!foundBoth) {
	  	d += searchRect.height;
	  	data = getDistanceAndPointAtY(line, d);
	  	if (data!=null) {
		  	p.setLocation((Point2D)data[1]);
				if (p.getX()>=searchRect.x && p.getX()<=searchRect.x+searchRect.width) {
					// line end is bottom edge
					if (p==p2 && !p1.equals(p2)) foundBoth = true;
				}
	  	}
		}
		// if both line ends have been found, use line to find pixels to search
  	if (foundBoth) {
  		// set line ends to intersections
  		line.setLine(p1, p2);
	  	if (p1.getX()>p2.getX()) {
	  		line.setLine(p2, p1);
	  	}
	  	// find pixel intersections that fall along the line
	  	int xMin = (int)Math.ceil(Math.min(p1.getX(), p2.getX()));
	  	int xMax = (int)Math.floor(Math.max(p1.getX(), p2.getX()));
	  	int yMin = (int)Math.ceil(Math.min(p1.getY(), p2.getY()));
	  	int yMax = (int)Math.floor(Math.max(p1.getY(), p2.getY()));
	  	// collect intersections in TreeMap sorted by position along line
	  	TreeMap<Double, Point2D> intersections = new TreeMap<Double, Point2D>();
	  	for (int x = xMin; x <= xMax; x++) {
				Object[] next = getDistanceAndPointAtX(line, x);
				if (next==null) continue;
				intersections.put((Double)next[0], (Point2D)next[1]);
	  	}
	  	for (int y = yMin; y <= yMax; y++) {
				Object[] next = getDistanceAndPointAtY(line, y);
				if (next==null) continue;
				intersections.put((Double)next[0], (Point2D)next[1]);
	  	}
	  	p = null;
	  	// create array of search points that are midway between intersections
	  	ArrayList<Point2D> searchPts = new ArrayList<Point2D>();
	  	for (Double key: intersections.keySet()) {
	  		Point2D next = intersections.get(key);
	  		if (p!=null) {
	  			double x = (p.getX()+next.getX())/2 - searchRect.x;
	  			double y = (p.getY()+next.getY())/2 - searchRect.y;
		  		p.setLocation(x, y);
		  		searchPts.add(p);
	  		}
	  		p = next;
	  	}
	  	return searchPts;
  	}
  	return null;
  }

  /**
   * Gets the distance and point along a Line2D at a specified x.
   * If the Line2D is vertical this returns null.
   *
   * Based on a simplification of algorithm described by Paul Burke
   * at http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline2d/ (April 1986)
   *
   * @param line the line
   * @param x the value of x
   * @return Object[] {fractional distance from line end, Point2D}
   */
  private Object[] getDistanceAndPointAtX(Line2D line, double x) {
  	double dx = line.getX2()-line.getX1();
  	// if line is vertical, return null
  	if (dx==0) return null;
  	// parametric eqn of line: P = P1 + u(P2 - P1)
  	double u = (x-line.getX1())/dx;
		double y = line.getY1() + u*(line.getY2()-line.getY1());
		return new Object[] {u, new Point2D.Double(x, y)};
  }

  /**
   * Gets the distance and point along a Line2D at a specified y.
   * If the Line2D is horizontal this returns null.
   *
   * Based on a simplification of algorithm described by Paul Burke
   * at http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline2d/ (April 1986)
   *
   * @param line the line
   * @param y the value of y
   * @return Object[] {fractional distance from line end, Point2D}
   */
  private Object[] getDistanceAndPointAtY(Line2D line, double y) {
  	double dy = line.getY2()-line.getY1();
  	// if line is horizontal, return null
  	if (dy==0) return null;
  	// parametric eqn of line: P = P1 + u(P2 - P1)
  	double u = (y-line.getY1())/dy;
		double x = line.getX1() + u*(line.getX2()-line.getX1());
		return new Object[] {u, new Point2D.Double(x, y)};
  }

  /**
   * Gets the total difference between the template and test pixels
   * at a specified test point. The test point is the point on the test
   * image where the top left corner of the template is located.
   *
   * @param x the test point x-component
   * @param y the test point y-component
   */
  private double getDifferenceAtTestPoint(int x, int y) {
	  // for each pixel in template, get difference from corresponding test pixel
	  // return sum of these differences
	  if (y * wTest + x < 0 || (y + hTemplate - 1) * wTest + x + (wTemplate - 1) >= targetPixels.length)
		  return Double.NaN; // may occur when doing Gaussian fit
	  long diff = 0;
	  for (int i = 0; i < wTemplate; i++) {
		  for (int j = 0; j < hTemplate; j++) {
			  int templateIndex = j * wTemplate + i;
			  if (!isPixelTransparent[templateIndex]) { // include only non-transparent pixels
				  int testIndex = (y + j) * wTest + x + i;
				  int pixel = targetPixels[testIndex];
				  diff += getRGBDifference(pixel, templateR[templateIndex], templateG[templateIndex], templateB[templateIndex]);
			  }
		  }
	  }
	  return diff;
  }


	/**
	 * Gets the total difference between the template and test pixels
	 * at a specified test point. The test point is the point on the test
	 * image where the top left corner of the template is located.
	 * When the difference becomes big enough, it gets approximated
	 *  @param x the test point x-component
	 * @param y the test point y-component
	 * @param enough the value of rather big difference
	 */
	private long getDifferenceAtTestPoint(int x, int y, long enough) {
		// for each pixel in template, get difference from corresponding test pixel
		// return sum of these differences
		if (y * wTest + x < 0 || (y + hTemplate - 1) * wTest + x + (wTemplate - 1) >= targetPixels.length)
			return largeLong; // may occur when doing Gaussian fit
		long diff = 0;
		for (int i = 0; i < wTemplate; i++) {
			for (int j = 0; j < hTemplate; j++) {
				int templateIndex = j * wTemplate + i;
				if (!isPixelTransparent[templateIndex]) { // include only non-transparent pixels
					int testIndex = (y + j) * wTest + x + i;
					int pixel = targetPixels[testIndex];
					diff += getRGBDifference(pixel, templateR[templateIndex], templateG[templateIndex], templateB[templateIndex]);
				}
			}
			if(diff > enough){
				return (long)(((double)diff) * columnExtrapolationCoefficients[i]);
			}
		}
		return diff;
	}

	/**
	 * Gets the total difference between the template and test pixels
	 * at a specified test point. The test point is the point on the test
	 * image where the top left corner of the template is located.
	 * When the difference becomes big enough, it gets approximated
	 *  @param x the test point x-component
	 * @param y the test point y-component
	 * @param enough the value of rather big difference
	 */
	private long getDifferenceAtTestPointConvex(int x, int y, long enough) {
		// for each pixel in template, get difference from corresponding test pixel
		// return sum of these differences
		if (y * wTest + x < 0 || (y + hTemplate - 1) * wTest + x + (wTemplate - 1) >= targetPixels.length)
			return largeLong; // may occur when doing Gaussian fit
		long diff = 0;
		for (int i = 0; i < wTemplate; i++) {
			for (int j = convexMaskStart[i]; j < convexMaskEnd[i]; j++) {
				int templateIndex = j * wTemplate + i;
				//if (!isPixelTransparent[templateIndex]) { // include only non-transparent pixels
					int testIndex = (y + j) * wTest + x + i;
					int pixel = targetPixels[testIndex];
					diff += getRGBDifference(pixel, templateR[templateIndex], templateG[templateIndex], templateB[templateIndex]);
				//}
			}
			if(diff > enough){
				return (long)(((double)diff) * columnExtrapolationCoefficients[i]);
			}
		}
		return diff;
	}

	/**
   * Gets the difference between a pixel and a comparison set of rgb components.
   */
  private double getRGBDifference(int pixel, int r, int g, int b) {
    int rPix = (pixel >> 16) & 0xff;		// red
    int gPix = (pixel >>  8) & 0xff;		// green
    int bPix = (pixel      ) & 0xff;		// blue
    int dr = r-rPix;
	  int dg = g-gPix;
	  int db = b-bPix;
	  return dr*dr + dg*dg + db*db; // sum of squares of rgb differences
  }

  /**
   * Gets an AlphaComposite object with a specified alpha value.
   */
  private AlphaComposite getComposite(int alpha) {
  	float a = 1.0f*alpha/255;
    return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a);
  }

}
