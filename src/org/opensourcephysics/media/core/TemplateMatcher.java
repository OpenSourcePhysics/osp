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

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.BitSet;
import java.util.TreeMap;

import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.tools.DatasetCurveFitter;
import org.opensourcephysics.tools.FitBuilder;
import org.opensourcephysics.tools.UserFunction;

/**
 * A class to find the best match of a template image in a target image. The
 * match location is estimated to sub-pixel accuracy by assuming the
 * distribution of match scores near a peak is Gaussian.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class TemplateMatcher {

	// static constants
	private static final double LARGE_NUMBER = 1.0E10;

	// instance fields
	private BufferedImage original, template, working, match;
	private Shape mask;
	private int[] pixels, templateR, templateG, templateB;
	private boolean[] isPixelTransparent;
	private int[] targetPixels;
	private int wTemplate, hTemplate; // width and height of the template image
	private int wTarget, hTarget; // width and height of the target image
	private int wTest, hTest; // width and height of the tested image (in search rect)
	private double largeNumber = 1.0E20; // bigger than any expected difference
	private DatasetCurveFitter fitter; // used for Gaussian fit
	private Dataset dataset; // used for Gaussian fit
	private UserFunction f; // used for Gaussian fit
	private double[] pixelOffsets = { -1, 0, 1 }; // used for Gaussian fit
	private double[] xValues = new double[3]; // used for Gaussian fit
	private double[] yValues = new double[3]; // used for Gaussian fit
	private double peakHeight, peakWidth; // peak height and width of most recent match
	private int trimLeft, trimTop;
	private int[] alphas = new int[2]; // most recent alphas {input, original}
	private int index; // for AutoTracker--not used internally

	/**
	 * Constructs a TemplateMatcher object. If a mask shape is specified, then only
	 * pixels that are entirely inside the mask are included in the template.
	 * 
	 * @param image     the image to match
	 * @param maskShape a shape to define inside pixels (may be null)
	 */
	public TemplateMatcher(BufferedImage image, Shape maskShape) {
		mask = maskShape;
		setTemplate(image);
		// set up the Gaussian curve fitter
		dataset = new Dataset();
		fitter = new DatasetCurveFitter(dataset, new FitBuilder(null));
		fitter.setActiveNoFit(true);
		f = new UserFunction("gaussian"); //$NON-NLS-1$
		f.setParameters(new String[] { "a", "b", "c" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				new double[] { 1, 0, 1 });
		f.setExpression("a*exp(-(x-b)^2/c)", new String[] { "x" }); //$NON-NLS-1$ //$NON-NLS-2$
		fitter.setAutofit(true); //
	}

	/**
	 * Sets the template to be used for the next search. The new template dimensions
	 * must match those of the previous template.
	 *
	 * @param image the template image
	 */
	public void setTemplate(BufferedImage image) {
		boolean isARGB = (image.getType() == BufferedImage.TYPE_INT_ARGB);
		boolean isOK = (template != null && wTemplate == image.getWidth() && hTemplate == image.getHeight());
		if (!isARGB || !isOK) {
			original = ensureType(image, image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
			image = buildTemplate(original, 255, 0); // builds from scratch
		}
		template = image;
		pixels = getPixels(template);
		// set up rgb and transparency arrays for fast matching
		for (int i = pixels.length; --i >= 0;) {
			int val = pixels[i];
			templateR[i] = getRed(val); // red
			templateG[i] = getGreen(val); // green
			templateB[i] = getBlue(val); // blue
			isPixelTransparent[i] = getAlpha(val) == 0; // alpha
		}
	}

	/**
	 * Gets the working image pixels used to generate the template.
	 *
	 * @param pixels int[] of pixels. If null, it will be created
	 * @return the filled pixels array
	 */
	public int[] getWorkingPixels() {
		return getPixels(working);
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
	 * Builds the template from an input image. The input image dimensions must
	 * match the original. The input and original are overlaid onto the working
	 * image, from which the template is generated. Pixels that fall outside the
	 * mask are ignored in the f template.
	 * 
	 * @param image         the input image
	 * @param alphaInput    the opacity with which the input image is overlaid
	 *                      (0-255)
	 * @param alphaOriginal the opacity with which the original image is overlaid
	 *                      (0-255)
	 * @return the template
	 */
	public BufferedImage buildTemplate(BufferedImage image, int alphaInput, int alphaOriginal) {
		int w = image.getWidth();
		int h = image.getHeight();
		int nPixels = w * h;
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
		BufferedImage input = ensureType(image, w, h, BufferedImage.TYPE_INT_ARGB);
		// create working image if needed
		working = ensureType(working, w, h, BufferedImage.TYPE_INT_ARGB);
		
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

		pixels = getWorkingPixels();

		// ensure template matches working
		if (template == null || w != wTemplate || h != hTemplate) {
			wTemplate = w;
			hTemplate = h;
			template = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			templateR = new int[nPixels];
			templateG = new int[nPixels];
			templateB = new int[nPixels];
			isPixelTransparent = new boolean[nPixels];
		}

		if (mask != null) {
			// set pixels outside mask to transparent
			for (int i = nPixels; --i >= 0;) {
				boolean inside = true;
				// pixel is inside only if all corners are inside
				int x = i % wTemplate, y = i / wTemplate;
				for (int j = 0; j < 2; j++) {
					for (int k = 0; k < 2; k++) {
						inside = inside && mask.contains(x + j, y + k);
					}
				}
				if (!inside)
					pixels[i] = 0; // set alpha to zero (transparent)
			}
		}
		// trim transparent edges from template
		BitSet bsOpaque = getOpaqueBS(pixels, wTemplate, hTemplate);
		trimTop = bsOpaque.nextSetBit(0) / wTemplate;
		int trimBottom = hTemplate - 1 - (bsOpaque.length() - 1) / wTemplate;
		int trimRight = trimLeft = 0;
		// left edge
		outl: while (trimLeft < wTemplate) {
			for (int line = trimTop, wT = wTemplate, i = trimTop * wT + trimLeft; line < hTemplate; line++, i += wT) {
				if (bsOpaque.get(i))
					break outl;
			}
			trimLeft++;
		}
		// right edge
		outr: while ((trimLeft + trimRight) < wTemplate) {
			for (int line = 0, wT = wTemplate, i = wT - 1 - trimRight; line < hTemplate; line++, i += wT) {
				if (bsOpaque.get(i))
					break outr;
			}
			trimRight++;
		}
		// reduce size of template if needed
		if (trimLeft + trimRight + trimTop + trimBottom == 0) {
			System.arraycopy(pixels, 0, getPixels(template), 0, nPixels);
		} else {
			int w0 = wTemplate;
			wTemplate -= (trimLeft + trimRight);
			hTemplate -= (trimTop + trimBottom);
			wTemplate = Math.max(wTemplate, 1);
			hTemplate = Math.max(hTemplate, 1);
			int len = wTemplate * hTemplate;
			templateR = new int[len];
			templateG = new int[len];
			templateB = new int[len];
			isPixelTransparent = new boolean[len];
			template = new BufferedImage(wTemplate, hTemplate, BufferedImage.TYPE_INT_ARGB);
			transferPixels(pixels, trimLeft, trimTop, w0, getPixels(template), wTemplate);
			pixels = new int[len];
		}
		return template;
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
	 * Gets the index. Note: index is not set internally, but only with calls to
	 * setIndex().
	 *
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Sets the working image pixels used to generate the template.
	 *
	 * @param pixels int[] of pixels
	 */
	public void setWorkingPixels(int[] pixels) {
		if (pixels != null && pixels.length == wTemplate * hTemplate) {
			int[] p = getWorkingPixels();
			if (p != pixels)
				System.arraycopy(pixels, 0, p, 0, p.length);
		}
	}

	/**
	 * Gets the template location at which the best match occurs in a rectangle. May
	 * return null.
	 * 
	 * Template matching process:
	 * 
	 * 1. At each test position in the search area, find the RGB square deviation
	 * ("RGBSqD": sum of squares of rgb differences of all pixels) between the
	 * template and video image. Note that the RGBSqD is zero for a perfect match
	 * and larger for poorer matches.
	 * 
	 * 2. Determine the average RGBSqD for all test positions.
	 * 
	 * 3. Define the position for which the RGBSqD is minimum as the "working" best
	 * match. Define the peak height ("PH") of this match to be PH =
	 * (avgRGBSqD/matchRGBSqD)-1. Note that the PH may vary from zero to infinity.
	 * 
	 * 4. If the PH exceeds the "Automark" setting, the match is deemed to be a good
	 * one (i.e., significantly better than average).
	 * 
	 * 5. For sub-pixel accuracy, fit a Gaussian curve to the PHs of the working
	 * best match and its immediate vertical and horizontal neighbors. Note that the
	 * 3-point Gaussian fits should be exact.
	 * 
	 * 6. The final best match (sub-pixel) is the position of the peak of the
	 * Gaussian fit.
	 * 
	 * 7. Note that the width of the Gaussian fit is probably correlated with the
	 * uncertainty of the match position, but it is not used to explicitly estimate
	 * this uncertainty except that if the width > 1 pixel then the peak height is
	 * divided by that width. This assures that very wide fits are not treated as
	 * good fits.
	 *
	 * @param target     the image to search
	 * @param searchRect the rectangle to search within the target image
	 * @return the optimized template location at which the best match, if any, is
	 *         found
	 */
	public TPoint getMatchLocation(BufferedImage target, Rectangle searchRect) {
		wTarget = target.getWidth();
		hTarget = target.getHeight();
		// determine insets needed to accommodate template
		int left = wTemplate / 2;
		int top = hTemplate / 2;
		int right = left + (wTemplate % 2);
		int bottom = top + (hTemplate % 2);
		// trim search rectangle if necessary
		int sx = searchRect.x = Math.max(left, Math.min(wTarget - right, searchRect.x));
		int sy = searchRect.y = Math.max(top, Math.min(hTarget - bottom, searchRect.y));
		int sw = searchRect.width = Math.min(wTarget - sx - right, searchRect.width);
		int sh = searchRect.height = Math.min(hTarget - sy - bottom, searchRect.height);
		if (sw <= 0 || sh <= 0) {
			peakHeight = Double.NaN;
			peakWidth = Double.NaN;
			return null;
		}
		// set up test pixels to search (rectangle plus template)
		int xMin = Math.max(0, sx - left);
		int xMax = Math.min(wTarget, sx + sw + right);
		int yMin = Math.max(0, sy - top);
		int yMax = Math.min(hTarget, sy + sh + bottom);
		wTest = xMax - xMin;
		hTest = yMax - yMin;
		target = ensureType(target, wTarget, hTarget, BufferedImage.TYPE_INT_RGB);
		targetPixels = new int[wTest * hTest];
		// target.getRaster().getDataElements(xMin, yMin, wTest, hTest, targetPixels);
		transferPixels(getPixels(target), xMin, yMin, wTarget, targetPixels, wTest);
		// find the rectangle point with the minimum difference
		double matchDiff = largeNumber; // larger than typical differences
		int xMatch = 0, yMatch = 0;
		double avgDiff = 0;
		for (int x = 0; x < sw; x++) { // BH this was <=, but then we are checking past the edge
			for (int y = 0; y < sh; y++) { // BH same here.

				double diff = getDifferenceAtTestPoint(x, y);
				avgDiff += diff;
				if (diff < matchDiff) {
					matchDiff = diff;
					xMatch = x;
					yMatch = y;
				}
			}
		}
		avgDiff /= (sw * sh);
		peakHeight = avgDiff / matchDiff - 1;
		peakWidth = Double.NaN;
		double dx = 0, dy = 0;
		// if match is not exact, fit a Gaussian and find peak
		if (!Double.isInfinite(peakHeight)) {
			// fill data arrays
			xValues[1] = yValues[1] = peakHeight;
			for (int i = -1; i < 2; i++) {
				if (i == 0)
					continue;
				double diff = getDifferenceAtTestPoint(xMatch + i, yMatch);
				xValues[i + 1] = avgDiff / diff - 1;
				diff = getDifferenceAtTestPoint(xMatch, yMatch + i);
				yValues[i + 1] = avgDiff / diff - 1;
			}
			// estimate peakHeight = peak of gaussian
			// estimate offset dx of gaussian
			double pull = 1 / (xValues[1] - xValues[0]);
			double push = 1 / (xValues[1] - xValues[2]);
			if (Double.isNaN(pull))
				pull = LARGE_NUMBER;
			if (Double.isNaN(push))
				push = LARGE_NUMBER;
			dx = 0.6 * (push - pull) / (push + pull);
			// estimate width wx of gaussian
			double ratio = dx > 0 ? peakHeight / xValues[0] : peakHeight / xValues[2];
			double wx = dx > 0 ? dx + 1 : dx - 1;
			wx = wx * wx / Math.log(ratio);
			// estimate offset dy of gaussian
			pull = 1 / (yValues[1] - yValues[0]);
			push = 1 / (yValues[1] - yValues[2]);
			if (Double.isNaN(pull))
				pull = LARGE_NUMBER;
			if (Double.isNaN(push))
				push = LARGE_NUMBER;
			dy = 0.6 * (push - pull) / (push + pull);
			// estimate width wy of gaussian
			ratio = dy > 0 ? peakHeight / yValues[0] : peakHeight / yValues[2];
			double wy = dy > 0 ? dy + 1 : dy - 1;
			wy = wy * wy / Math.log(ratio);

			// set x parameters and fit to x data
			dataset.clear();
			dataset.append(pixelOffsets, xValues);
			double rmsDev = 1;
			for (int k = 0; k < 3; k++) {
				double c = k == 0 ? wx : k == 1 ? wx / 3 : wx * 3;
				f.setParameterValue(0, peakHeight);
				f.setParameterValue(1, dx);
				f.setParameterValue(2, c);
				rmsDev = fitter.fit(f);
				if (rmsDev < 0.01) { // fitter succeeded (3-point fit should be exact)
					dx = f.getParameterValue(1);
					peakWidth = f.getParameterValue(2);
					break;
				}
			}
			if (!Double.isNaN(peakWidth)) {
				// set y parameters and fit to y data
				dataset.clear();
				dataset.append(pixelOffsets, yValues);
				for (int k = 0; k < 3; k++) {
					double c = k == 0 ? wy : k == 1 ? wy / 3 : wy * 3;
					f.setParameterValue(0, peakHeight);
					f.setParameterValue(1, dy);
					f.setParameterValue(2, c);
					rmsDev = fitter.fit(f);
					if (rmsDev < 0.01) { // fitter succeeded (3-point fit should be exact)
						dy = f.getParameterValue(1);
						peakWidth = (peakWidth + f.getParameterValue(2)) / 2;
						break;
					}
				}
				if (rmsDev > 0.01)
					peakWidth = Double.NaN;
			}
			if (!Double.isNaN(peakWidth) && peakWidth > 1) {
				peakHeight /= peakWidth;
			}
		}

		xMatch += sx - left - trimLeft;
		yMatch += sy - top - trimTop;
		refreshMatchImage(target, xMatch, yMatch);
		return new TPoint(xMatch + dx, yMatch + dy);
	}

	/**
	 * Gets the template location at which the best match occurs in a rectangle and
	 * along a line. May return null.
	 *
	 * @param target     the image to search
	 * @param searchRect the rectangle to search within the target image
	 * @param x0         the x-component of a point on the line
	 * @param y0         the y-component of a point on the line
	 * @param theta      the angle of the line
	 * @param spread     the spread of the line (line width = 1+2*spread)
	 * @return the optimized template location of the best match, if any
	 */
	public TPoint getMatchLocation(BufferedImage target, Rectangle searchRect, double x0, double y0, double theta,
			int spread) {
		wTarget = target.getWidth();
		hTarget = target.getHeight();
		// determine insets needed to accommodate template
		int left = wTemplate / 2;
		int right = left + (wTemplate % 2);
		int top = hTemplate / 2;
		int bottom = top + (hTemplate % 2);

		// trim search rectangle if necessary
		int sx = searchRect.x = Math.max(left, Math.min(wTarget - right, searchRect.x));
		int sy = searchRect.y = Math.max(top, Math.min(hTarget - bottom, searchRect.y));
		int sw = searchRect.width = Math.min(wTarget - sx - right, searchRect.width);
		int sh = searchRect.height = Math.min(hTarget - sy - bottom, searchRect.height);
		if (sw <= 0 || sh <= 0) { // not able to search
			peakHeight = Double.NaN;
			peakWidth = Double.NaN;
			return null;
		}
		// set up test pixels to search (rectangle plus template)
		int xMin = Math.max(0, sx - left);
		int xMax = Math.min(wTarget, sx + sw + right);
		int yMin = Math.max(0, sy - top);
		int yMax = Math.min(hTarget, sy + sh + bottom);
		wTest = xMax - xMin;
		hTest = yMax - yMin;
		target = ensureType(target, wTarget, hTarget, BufferedImage.TYPE_INT_RGB);
		targetPixels = getPixels(target);
		//new int[wTest * hTest];
		//target.getRaster().getDataElements(xMin, yMin, wTest, hTest, targetPixels);
		// get the points to search along the line
		int[][] searchPts = getSearchPoints(searchRect, x0, y0, theta);
		if (searchPts == null) { // not able to search
			peakHeight = Double.NaN;
			peakWidth = -1;
			return null;
		}
		// collect differences in a map as they are measured
		double[] diffs = new double[searchPts.length];
		// find the point with the minimum difference from template
		double matchDiff = largeNumber; // larger than typical differences
		int xMatch = 0, yMatch = 0;
		double avgDiff = 0;
		int[] matchPt = null;
		int matchIndex = -1;
		for (int i = searchPts.length; --i >= 0;) {
			int[] pt = searchPts[i];
			int x = pt[0];
			int y = pt[1];
			double diff = getDifferenceAtTestPoint(x, y);
			diffs[i] = diff;
			avgDiff += diff;
			if (diff < matchDiff) {
				matchDiff = diff;
				xMatch = x;
				yMatch = y;
				matchPt = pt;
				matchIndex = i;
			}
		}
		avgDiff /= searchPts.length;
		peakHeight = avgDiff / matchDiff - 1;
		peakWidth = Double.NaN;
		double dl = 0;

		// if match is not exact, fit a Gaussian and find peak
		if (matchPt != null && !Double.isInfinite(peakHeight) && matchIndex > 0 && matchIndex < searchPts.length - 1) {
			// fill data arrays
			int[] pt = searchPts[matchIndex - 1];
			double diff = diffs[matchIndex - 1];
			int dx;
			xValues[0] = -Math.sqrt((dx = pt[0] - matchPt[0]) * dx + (dx = pt[1] - matchPt[1]) * dx);
			yValues[0] = avgDiff / diff - 1;
			xValues[1] = 0;
			yValues[1] = peakHeight;
			pt = searchPts[matchIndex + 1];
			diff = diffs[matchIndex + 1];
			xValues[2] = Math.sqrt((dx = pt[0] - matchPt[0]) * dx + (dx = pt[1] - matchPt[1]) * dx);
			yValues[2] = avgDiff / diff - 1;

			// determine approximate offset (dl) and width (w) values
			double pull = -xValues[0] / (yValues[1] - yValues[0]);
			double push = xValues[2] / (yValues[1] - yValues[2]);
			if (Double.isNaN(pull))
				pull = LARGE_NUMBER;
			if (Double.isNaN(push))
				push = LARGE_NUMBER;
			dl = 0.3 * (xValues[2] - xValues[0]) * (push - pull) / (push + pull);
			double ratio = dl > 0 ? peakHeight / yValues[0] : peakHeight / yValues[2];
			double w = dl > 0 ? dl - xValues[0] : dl - xValues[2];
			w = w * w / Math.log(ratio);

			// set parameters and fit to x data
			dataset.clear();
			dataset.append(xValues, yValues);
			double rmsDev = 1;
			for (int k = 0; k < 3; k++) {
				double c = k == 0 ? w : k == 1 ? w / 3 : w * 3;
				f.setParameterValue(0, peakHeight);
				f.setParameterValue(1, dl);
				f.setParameterValue(2, c);
				rmsDev = fitter.fit(f);
				if (rmsDev < 0.01) { // fitter succeeded (3-point fit should be exact)
					dl = f.getParameterValue(1);
					peakWidth = f.getParameterValue(2);
					break;
				}
			}
		}
		double dx = dl * Math.cos(theta);
		double dy = dl * Math.sin(theta);
		xMatch = xMatch + sx - left - trimLeft;
		yMatch = yMatch + sy - top - trimTop;
		refreshMatchImage(target, xMatch, yMatch);
		return new TPoint(xMatch + dx, yMatch + dy);
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
	 * Returns the width and height of the peak for the most recent match. The peak
	 * height is the ratio meanSqPixelDiff/matchSqPixelDiff. The peak width is the
	 * mean of the vertical and horizontal Gaussian fit widths. This data can be
	 * used to determine whether a match is acceptable. A peak height greater than 5
	 * is a reasonable standard for acceptability.
	 * 
	 * Special cases:
	 * 
	 * 1. If the match is perfect, then the height is infinite and the width NaN.
	 * 
	 * 2. If the searchRect fell outside the target image, then no match was
	 * possible and both the width and height are NaN.
	 * 
	 * 3. If there were no points to search along the 1D x-axis path, then the
	 * height is NaN and the width is negative.
	 * 
	 * 4. If the Gaussian fit optimization was not successful (either horizontally
	 * or vertically) then the height is finite and the width is NaN.
	 *
	 * @return double[2] {mean Gaussian width, height}
	 */
	public double[] getMatchWidthAndHeight() {
		return new double[] { peakWidth, peakHeight };
	}

	/**
	 * Method to get the color value
	 * 
	 * @param a    0-255 alpha
	 * @param argb current color value
	 * @return the integer value
	 */
	public static int getValue(int a, int argb) {
		return argb & 0xFFFFFF | (a << 24);
//		int r = getRed(argb);
//		int g = getGreen(argb);
//		int b = getBlue(argb);
//		return getValue(a, r, g, b);
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
		return (a << 24) + (r << 16) + (g << 8) + b;
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
	 * Refreshes the match image.
	 *
	 * @param target the matched image
	 * @param x      the match x-position
	 * @param y      the match y-position
	 */
	private void refreshMatchImage(BufferedImage target, int x, int y) {
		if (match == null || match.getWidth() != wTemplate || match.getHeight() != hTemplate) {
			match = new BufferedImage(wTemplate, hTemplate, BufferedImage.TYPE_INT_ARGB);
		}
		int[] matchPixels = getPixels(match);
		transferPixels(getPixels(target), x + 1, y + 1, wTarget, matchPixels, wTemplate);
		for (int i = matchPixels.length; --i >= 0;) {
			matchPixels[i] = getValue(isPixelTransparent[i] ? 0 : 255, matchPixels[i]);
		}
	}

	/**
	 * Gets the total difference between the template and test pixels at a specified
	 * test point. The test point is the point on the test image where the top left
	 * corner of the template is located.
	 * 
	 * @param x the test point x-component
	 * @param y the test point y-component
	 */
	private double getDifferenceAtTestPoint(int x, int y) {
		// for each pixel in template, get difference from corresponding test pixel
		// return sum of these differences
		double diff = 0;
		int xyoff = y * wTest + x;
		// last point on target is the first point plus the target width times one less
		// than the number of lines in the template plus the width of the template.
		int pmax = xyoff + (hTemplate - 1) * wTest + wTemplate;
		if (xyoff < 0 || pmax >= targetPixels.length)
			return Double.NaN;
		for (int j = 0, tpt = 0, targetIndex = xyoff, dw = wTest - wTemplate; j < hTemplate; j++, targetIndex += dw) {
			for (int i = 0; i < wTemplate; i++, tpt++, targetIndex++) {
				if (targetIndex >= targetPixels.length)
					return Double.NaN; // may occur when doing Gaussian fit
				if (!isPixelTransparent[tpt]) { // include only non-transparent pixels
					int pixel = targetPixels[targetIndex];
					diff += getRGBDiffSquared(pixel, templateR[tpt], templateG[tpt], templateB[tpt]);
				}
			}
		}
		return diff;
	}

	/**
	 * Gets a list of Point2D objects that lie within pixels in a rectangle and
	 * along a line.
	 * 
	 * @param searchRect the rectangle
	 * @param x0         the x-component of a point on the line
	 * @param y0         the y-component of a point on the line
	 * @param theta      the angle of the line
	 * @return a list of Point2D
	 */
	private int[][] getSearchPoints(Rectangle searchRect, double x0, double y0, double theta) {
		int sx = searchRect.x;
		int sy = searchRect.y;
		int sh = searchRect.height;
		int sw = searchRect.width;
		double slope = -Math.tan(theta);
		// create line to search along
		Line2D.Double line = new Line2D.Double();
		if (Math.abs(slope) > LARGE_NUMBER) { // vertical axis
			line.setLine(x0, y0, x0, y0 + 1);
		} else if (Math.abs(slope) < 1 / LARGE_NUMBER) { // horizontal axis
			line.setLine(x0, y0, x0 + 1, y0);
		} else {
			line.setLine(x0, y0, x0 + 1, y0 + slope);
		}
		double[] uxy = new double[3];
		// create intersection points (to set line ends)
		double[] p1 = new double[2];
		double[] p2 = new double[] { Double.NaN, Double.NaN };
		double[] p = p1;
//		Point2D.Double p1 = new Point2D.Double();
//		Point2D.Double p2 = new Point2D.Double(Double.NaN, Double.NaN);
//		Point2D.Double p = p1;
		if (getDistanceAndPointAtX(line, sx, uxy)) {
			p[0] = uxy[1];
			p[1] = uxy[2];// .setLocation(uxy[1], uxy[2]);
			if (p[1] >= sy && p[1] <= sy + sh) {
				// line end is left edge
				// first found is p1, set for p2
				p = p2;
			}
		}
		if (getDistanceAndPointAtX(line, sx + sw, uxy)) {
			p[0] = uxy[1];
			p[1] = uxy[2];// .setLocation(uxy[1], uxy[2]);
			if (p[1] >= sy && p[1] <= sy + sh) {
				if (p == p1) {
					// nothing was found in the first round, above
					// line end is right edge
					p = p2;
				} else {
					p = null;
				}
			}
		}
		// p now could be p1 (none found) or p2 (one found) or null (both found)
		if (p != null) {
			if (getDistanceAndPointAtY(line, sy, uxy)) {
				p[0] = uxy[1];
				p[1] = uxy[2];// .setLocation(uxy[1], uxy[2]);
				if (p[0] >= sx && p[0] <= sx + sw) {
					// line end is top edge
					if (p == p1) {
						p = p2;
					} else if (p1[0] != p2[0] || p1[1] != p2[1]) { // !p1.equals(p2)
						p = null; // p1 and p2 are now both defined.
					}
				}
			}
			if (p == p2) {
				if (getDistanceAndPointAtY(line, sy + sh, uxy)) {
					p[0] = uxy[1];
					p[1] = uxy[2];// .setLocation(uxy[1], uxy[2]);
					if (p[0] >= sx && p[0] <= sx + sw) {
						// line end is bottom edge
						if (p == p2 && (p1[0] != p2[0] || p1[1] != p2[1])) { // !p1.equals(p2))
							p = null;
						}
					}
				}
			}
		}
		if (p != null)
			return null;
		// if both line ends have been found, use line to find pixels to search
		// set line ends to intersections
		if (p1[0] <= p2[0]) {
			line.setLine(p1[0], p1[1], p2[0], p2[1]);
		} else {
			line.setLine(p2[0], p2[1], p1[0], p1[1]);
		}
		// find pixel intersections that fall along the line
		int xMin = (int) Math.ceil(Math.min(p1[0], p2[0]));
		int xMax = (int) Math.floor(Math.max(p1[0], p2[0]));
		int yMin = (int) Math.ceil(Math.min(p1[1], p2[1]));
		int yMax = (int) Math.floor(Math.max(p1[1], p2[1]));
		// collect intersections in TreeMap sorted by position along line
		TreeMap<Double, double[]> intersections = new TreeMap<>();
		for (int x = xMin; x <= xMax; x++) {
			if (getDistanceAndPointAtX(line, x, uxy))
				intersections.put(uxy[0], new double[] { uxy[1], uxy[2] });
		}
		for (int y = yMin; y <= yMax; y++) {
			if (getDistanceAndPointAtY(line, y, uxy))
				intersections.put(uxy[0], new double[] { uxy[1], uxy[2] });
		}
		// create array of search points that are midway between intersections

		int[][] pts = new int[intersections.size() - 1][2];
		int i = -1;
		double[] pxy = null;
		for (double[] next : intersections.values()) {
			if (pxy != null) {
				pts[i][0] = (int) ((pxy[0] + next[0]) / 2) - sx;
				pts[i][1] = (int) ((pxy[1] + next[1]) / 2) - sy;
			}
			pxy = next;
			i++;
		}
		return pts;
	}

	/**
	 * Gets the distance and point along a Line2D at a specified x. If the Line2D is
	 * vertical this returns null.
	 * 
	 * Based on a simplification of algorithm described by Paul Burke at
	 * http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline2d/ (April 1986)
	 * 
	 * @param line the line
	 * @param x    the value of x
	 * @return Object[] {fractional distance from line end, Point2D}
	 */
	private static boolean getDistanceAndPointAtX(Line2D.Double line, double x, double[] uxy) {
		double dx = line.x2 - line.x1;
		// if line is vertical, return null
		if (dx == 0)
			return false;
		// parametric eqn of line: P = P1 + u(P2 - P1)
		uxy[0] = (x - line.x1) / dx;
		uxy[1] = x;
		uxy[2] = line.y1 + uxy[0] * (line.y2 - line.y1);
		return true;
	}

	/**
	 * Gets the distance and point along a Line2D at a specified y. If the Line2D is
	 * horizontal this returns null.
	 * 
	 * Based on a simplification of algorithm described by Paul Burke at
	 * http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline2d/ (April 1986)
	 * 
	 * @param line the line
	 * @param y    the value of y
	 * @return Object[] {fractional distance from line end, Point2D}
	 */
	private static boolean getDistanceAndPointAtY(Line2D.Double line, double y, double[] uxy) {
		double dy = line.y2 - line.y1;
		// if line is horizontal, return null
		if (dy == 0)
			return false;
		// parametric eqn of line: P = P1 + u(P2 - P1)
		uxy[0] = (y - line.y1) / dy;
		uxy[1] = line.x1 + uxy[0] * (line.x2 - line.x1);
		uxy[2] = y;
		return true;
	}

	/**
	 * Gets the difference between a pixel and a comparison set of rgb components.
	 * @return sum of squares of rgb differences
	 */
	private static double getRGBDiffSquared(int pixel, int r, int g, int b) {
		int dr = r - ((pixel >> 16) & 0xff);
		int dg = g - ((pixel >> 8) & 0xff);
		int db = b - ((pixel) & 0xff);
		return dr * dr + dg * dg + db * db; 
	}

	/**
	 * Gets an AlphaComposite object with a specified alpha value.
	 */
	private static AlphaComposite getComposite(int alpha) {
		float a = 1.0f * alpha / 255;
		return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a);
	}

	/**
	 * Ensures that an image is of a given type, particularly ARGB or RGB.
	 * 
	 * @param image
	 * @param w
	 * @param h
	 * @param type
	 * @return
	 */
	private static BufferedImage ensureType(BufferedImage image, int w, int h, int type) {
		if (image != null && image.getType() == type)
			return image;
		BufferedImage bi = new BufferedImage(w, h, type);
		if (image != null) {
			Graphics2D g = bi.createGraphics();
			g.drawImage(image, 0, 0, null);
			g.dispose();
		}
		return bi;
	}

	private static int[] getPixels(BufferedImage img) {
		return ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
	}

	private static void transferPixels(int[] p0, int x, int y, int w0, int[] p1, int w1) {
		for (int tpt = 0, len = p1.length, dw = w0 - w1, pt = y * w0 + x; tpt < len; pt += dw) {
			for (int j = 0; j < w1; j++, pt++, tpt++)
				p1[tpt] = p0[pt];
		}
	}

	private static BitSet getOpaqueBS(int[] pixels, int w, int h) {
		int nPixels = w * h;
		BitSet bs = new BitSet(nPixels);
		for (int i = nPixels; --i >= 0;) {
			if ((pixels[i] & 0xFF000000) == 0xFF000000)
				bs.set(i);
		}
//		for (int i = 0, pt = 0; i < h; i++) {
//			for (int j = 0; j < w; j++, pt++) {
//				System.out.print(bs.get(pt) ? "1" : "0");
//			}
//			System.out.println( " " + pt);
//		}
//		System.out.println(" ");
//		System.out.println(bs.nextSetBit(0));
//		System.out.println(bs.length() - 1);
		return bs;
	}



}
