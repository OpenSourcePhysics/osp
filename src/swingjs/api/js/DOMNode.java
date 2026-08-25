package swingjs.api.js;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D.Float;
import java.awt.image.BufferedImage;

/**
 * A mix of direct DOM calls on DOM nodes and convenience methods to do that.
 * 
 * NOTE: DO NOT OVERLOAD THESE METHODS, as this package will not be qualified.
 * 
 * @author hansonr
 *
 */
public interface DOMNode {
	public static JQuery jQuery = /** @j2sNative jQuery.$ || (jQuery.$ = jQuery) || */null;

	// "abstract" in the sense that these are the exact calls to JavaScript
	

	public void addEventListener(String event, Object listener);
	public void removeEventListener(String event);
	public void removeEventListener(String event, Object listener);



	public String[] getAttributeNames();

	public String getAttribute(String name);

	public void setAttribute(String attr, String val);

	public void appendChild(DOMNode node);
	
	public void prepend(DOMNode node);
	
	public void insertBefore(DOMNode node, DOMNode refNode);
	
	public DOMNode removeChild(DOMNode node);

	public void focus();
	public boolean hasFocus();
	public void blur();

	public DOMNode removeAttribute(String attr);
	
	public void setSelectionRange(int start, int end, String direction);

	public Rectangle getBoundingClientRect();
	
	// static convenience methods

	public static DOMNode createElement(String key, String id) {
		DOMNode node = null;
		/**
		 * @j2sNative
		 * 					node = document.createElement(key);
		 * 					id && (node.id = id);
		 */
		return node;
	}

	public static DOMNode getElement(String id) {
		return (/**  @j2sNative  document.getElementById(id) ||*/ null);
	}

	public static DOMNode createTextNode(String text) {
		return (/** @j2sNative document.createTextNode(text) || */ null); 
	}

	public static DOMNode getParent(DOMNode node) {
		return (/**  @j2sNative  node.parentNode ||*/ null);
	}
	
	public static DOMNode getPreviousSibling(DOMNode node) {
		return (/**  @j2sNative  node.previousSibling ||*/ null);
	}
	
	public static DOMNode firstChild(DOMNode node) {
		return 	(/**  @j2sNative node.firstChild ||*/ null);
	}

	public static DOMNode lastChild(DOMNode node) {
		return 	(/**  @j2sNative node.lastChild ||*/ null);
	}

	public static DOMNode setZ(DOMNode node, int z) {
		return setStyle(node, "z-index", "" + z);
	}

	public static Object getAttr(Object node, String attr) {
		/**
		 * @j2sNative
		 * 
		 * if (!node)
		 *   return null;
		 * var a = node[attr];
		 * return (typeof a == "undefined" ? null : a); 
		 */
		{
		return null;
		}
	}

	public static int getAttrInt(DOMNode node, String attr) {
		return 	(/**  @j2sNative node && node[attr] ||*/ 0);
	}

	public static String getStyle(DOMNode node, String style) {
		return 	(/**  @j2sNative node && node.style[style] ||*/ null);
	}

	public static void getCSSRectangle(DOMNode node, Rectangle r) {
		/**
		 * @j2sNative
		 * 
		 *       r.x = parseInt(node.style.left.split("p")[0]);
		 *       r.y = parseInt(node.style.top.split("p")[0]);
		 *       r.width = parseInt(node.style.width.split("p")[0]);
		 *       r.height = parseInt(node.style.height.split("p")[0]);
		 * 
		 */
	}

	public static DOMNode setAttr(DOMNode node, String attr, Object val) {
		/**
		 * @j2sNative
		 * 
		 * 			attr && (node[attr] = (val == "秘TRUE" ? true : val == "秘FALSE" ? false : val));
		 * 
		 */
		return node;
	}


	public static void setAttrInt(DOMNode node, String attr, int val) {
		/**
		 * @j2sNative
		 * 
		 * 			node[attr] = val;
		 * 
		 */
	}


	/**
	 * allows for null key to be skipped (used in audio)
	 * 
	 * @param node
	 * @param attr
	 * @return
	 */
	public static DOMNode setAttrs(DOMNode node, Object... attr) {
		/**
		 * @j2sNative
		 * 
		 *            for (var i = 0; i < attr.length;) { 
		 *              C$.setAttr(node, attr[i++],attr[i++]);
		 *            }
		 */
		return node;
	}

	public static DOMNode setStyle(DOMNode node, String attr, String val) {
		/**
		 * @j2sNative
		 * 
		 *            node && (node.style[attr] = val);
		 * 
		 */
		return node;
	}

	public static DOMNode setStyles(DOMNode node, String... av) {
		/**
		 * @j2sNative
		 * 
if (node)for (var i = 0, n = av.length; i < n;) {
	var k = av[i++], v = av[i++];
	node.style[k] != v && (node.style[k] = v);
}
		 * 
		 */
		return node;
	}

	public static DOMNode setSize(DOMNode node, int width, int height) {
		return setStyles(node, "width", width + "px", "height", height + "px");
	}

	public static DOMNode setPositionAbsolute(DOMNode node) {
		return DOMNode.setStyle(node, "position", "absolute");
	}

	public static void setVisible(DOMNode node, boolean visible) {
		setStyle(node, "display", visible ? "block" : "none");
	}

	public static DOMNode setTopLeftAbsolute(DOMNode node, int top, int left) {
		return DOMNode.setStyles(node, "top", top + "px", "left", left + "px", "position", "absolute");
	}

	public static void addHorizontalGap(DOMNode domNode, int gap) {
		DOMNode label = DOMNode.setStyles(DOMNode.createElement("label", null), 
				"letter-spacing", gap + "px", "font-size", "0pt");
		label.appendChild(DOMNode.createTextNode("."));
		domNode.appendChild(label);
	}

	public static void appendChildSafely(DOMNode parent, DOMNode node) {
		/**
		 * @j2sNative
		 * if (!parent || node.parentElement == parent)
		 *   return;
		 */
		parent.appendChild(node);
	}
	
	// static jQuery calls
	
	/**
	 * jQuery height()
	 * 
	 * @param node
	 * @return height
	 */
	public static int getHeight(DOMNode node) {
		return jQuery.$(node).height();
	}

	/**
	 * jQuery width()
	 * 
	 * @param node
	 * @return width
	 */
	public static int getWidth(DOMNode node) {
		return jQuery.$(node).width();
	}

	/**
	 * jQuery remove()
	 * 
	 * Remove this node and return its parent. Automatically removing all events
	 * attached to it.
	 * 
	 * @param node
	 * @return parent or null
	 */
	public static void dispose(DOMNode node) {
		if (node != null)		
			jQuery.$(node).remove();
	}

	/**
	 * Just remove the node, keeping its events and data 
	 * @param node
	 */
	public static void remove(DOMNode node) {
		
		// NOTE: IE does not have node.remove()
		
		DOMNode p = getParent(node);
		if (p != null)
			p.removeChild(node);
	}

	/**
	 * just detaches all the nodes; doesn't remove their listeners
	 * @param node
	 */
	public static void detachAll(DOMNode node) {
		/**
		 * @j2sNative
		 *  if(node)
		 *    while(node.lastChild)
		 *  	node.removeChild(node.lastChild);
		 */
	}
	
	/**
	 * jQuery detach() + append()
	 * 
	 * @param node
	 * @param container
	 * @return parent if container is null, or container if it is not null
	 */
	public static DOMNode transferTo(DOMNode node, DOMNode container) {
		if (node == null)
			return null;
		DOMNode p = getParent(node);
		try {
			if (p != null)
				jQuery.$(node).detach();
		} catch (Throwable e) {
			// ignore
		}
		 if (container == null)
		 	return p; 
		 jQuery.$(container).append(node);
		return container;
	}

	public static Object getEmbedded(String name, String type) {
		DOMNode node = DOMNode.getElement(name + "-div");
		if (node == null)
			return null;
		switch (type) {
		case "node":
			return node;
		case "dim":
			return new Dimension(DOMNode.getWidth(node), DOMNode.getHeight(node));
		default:
			return DOMNode.getAttr(node, type);
		}
	}

	public interface HTML5Canvas extends DOMNode {

		Context2D getContext(String str2d);

		/*
		 * Retrieves the byte[] data buffer from an HTML5 CANVAS element, optionally
		 * first setting its contents to a source IMG, CANVAS, or VIDEO element.
		 * 
		 */
		static byte[] getDataBufferBytes(HTML5Canvas canvas, DOMNode sourceNode, int w, int h) {
			if (sourceNode != null) {
				DOMNode.setAttrInt(canvas, "width", w);
				DOMNode.setAttrInt(canvas, "height", h);
			}
			Context2D ctx = canvas.getContext("2d");
			if (sourceNode != null) {
				ctx.drawImage(sourceNode, 0, 0, w, h);
			}
			// Coerse int[] to byte[]
			return (byte[]) (Object) ctx.getImageData(0, 0, w, h).data;
		}

		/**
		 * Install a source image (img, video, or canvas) into a matching BufferedImage 
		 * 
		 * @param sourceNode
		 * @param image
		 */
		static void setImageNode(DOMNode sourceNode, BufferedImage image) {
			/**
			 * @j2sNative
			 * 
			 * 			image._setImageNode$O$Z(sourceNode, false);
			 * 
			 */		{
				// can't expose this image._setImageNode(sourceNode, false);
			 }
		}
		
		

		static HTML5Canvas createCanvas(int width, int height, String id) {
			HTML5Canvas canvas = (HTML5Canvas) DOMNode.createElement("canvas", (id == null ? "img" + Math.random() : id + ""));
			DOMNode.setStyles(canvas, "width", width + "px", "height", height + "px");
			/**
			 * @j2sNative
			 * 
			 * canvas.width = width;
			 * canvas.height = height;
			 * 
			 */
			return canvas;
		}

		abstract class Context2D {

			public class ImageData {
				public int[] data; 
			}

			public ImageData imageData;
			
			public Object[][] _aSaved;
			
			public double lineWidth;

			public String font, fillStyle, strokeStyle;

			public float globalAlpha;

			public abstract void drawImage(DOMNode img, double sx,
					double sy, double swidth, double sheight, double dx, double dy, double width, double height);

			public abstract ImageData getImageData(int x, int y, int width, int height);

			public abstract void beginPath();

			public abstract void moveTo(double x0, double y0);

			public abstract void lineTo(double x1, double y1);

			public abstract void stroke();

			public abstract void save();

			public abstract void scale(double f, double g);

			public abstract void arc(double centerX, double centerY, double radius, double startAngle, double  endAngle, boolean counterclockwise);

			public abstract void closePath();

			public abstract void restore();

			public abstract void translate(double x, double y);
			
			public abstract void rotate(double radians);

			public abstract void fill();


			public abstract void fill(String winding);

			public abstract void rect(double x, double y, double width, double height);

			public abstract void fillText(String s, double x, double y);

			public abstract void fillRect(double x, double y, double width, double height);

			public abstract void clearRect(double i, double j, double windowWidth, double windowHeight);

			public abstract void setLineDash(int[] dash);

			public abstract void clip();

			public abstract void quadraticCurveTo(double d, double e, double f, double g);

			public abstract void bezierCurveTo(double d, double e, double f, double g, double h, double i);

			public abstract void drawImage(DOMNode img, double x, double y, double width, double height);

			public abstract void putImageData(Object imageData, double x, double y);

			public abstract void transform(double d, double shx, double e, double shy, double f, double g);


			/**
			 * pull one save structure onto the stack array ctx._aSaved
			 * 
			 * @param ctx
			 * @return the length of the stack array after the push
			 */
			public static int push(Context2D ctx, Object[] map) {
				/**
				 * @j2sNative
				 * 
				 * (ctx._aSaved || (ctx._aSaved = [])).push(map); 
				 * return ctx._aSaved.length;
				 */
				{
					return 0;
				}
			}

			/**
			 * pull one save structure off the stack array ctx._aSaved
			 * 
			 * @param ctx
			 * @return
			 */
			public static Object[] pop(Context2D ctx) {
				/**
				 * @j2sNative
				 * 
				 * return (ctx._aSaved && ctx._aSaved.length > 0 ? ctx._aSaved.pop() : null); 
				 */
				{
					return null;
				}
			}

			public static int getSavedLevel(Context2D ctx) {
				/**
				 * @j2sNative
				 * 
				 * return (ctx._aSaved ? ctx._aSaved.length : 0); 
				 */
				{
					return 0;
				}
			}
			
			public static Object[][] getSavedStack(Context2D ctx) {
			   /**
			    * @j2sNative
			    * 
			    * return (ctx._aSaved || []);
			    */
				{
					return null;
				}
				
			}

			@SuppressWarnings("null")
			public static double[] setMatrix(Context2D ctx, AffineTransform transform) {
				double[] m = /**  @j2sNative ctx._m || */ null;
				if (transform == null) {
					/** @j2sNative ctx._m = null; */
					return null;			
				}
				if (m == null) {
					/**
					 * @j2sNative
					 * ctx._m = m = new Array(6);
					 */
					transform.getMatrix(m);
				}
				return m;
			}

			public static void createLinearGradient(Context2D ctx, Float p1, Float p2, String css1, String css2) {
				/**
				 * @j2sNative
				 * 
				 *   var grd = ctx.createLinearGradient(p1.x, p1.y, p2.x, p2.y);
				 *   grd.addColorStop(0,css1);
				 *   grd.addColorStop(1,css2);
				 *   ctx.fillStyle = grd;
				 */
				}

			abstract public void drawImage(DOMNode domNode, int x, int y);

		}

	}

	public interface JQuery {

		  JQueryObject $(Object selector);

		  DOMNode parseXML(String xmlData);
		  
		  boolean contains(Object outer, Object inner);

		  Object parseJSON(String json);

		  Object data(Object node, String attr);

			public interface JQueryObject {

				public interface JQEvent {

				}

				public abstract void appendTo(Object obj);

				public abstract JQueryObject append(Object span);

				public abstract void bind(String actions, Object f);

				public abstract void unbind(String actions);

				public abstract void on(String eventName, Object f);

				public abstract JQueryObject focus();

				public abstract JQueryObject select();

				public abstract int width();

				public abstract int height();

				public abstract Insets offset();

				public abstract void html(String html);

				public abstract DOMNode get(int i);

				public abstract String attr(String key);

				public abstract JQueryObject attr(String key, String value);

				public abstract JQueryObject css(String key, String value);

				public abstract JQueryObject addClass(String name);

				public abstract JQueryObject removeClass(String name);

				public abstract JQueryObject show();

				public abstract JQueryObject hide();

				public abstract JQueryObject resize(Object fHandleResize);

				/**
				 * closest ancestor
				 * 
				 * @param selector
				 * @return
				 */
				public abstract JQueryObject closest(String selector);

				/**
				 * find all descendants
				 * 
				 * @param selector
				 * @return
				 */
				public abstract JQueryObject find(String selector);

				public abstract JQueryObject parent();

				public abstract JQueryObject before(Object obj);

				public abstract JQueryObject after(Object div);

				public abstract JQueryObject scrollTop(int top);

				/**
				 * remove from tree, but do not clear events
				 */
				public abstract JQueryObject detach(); // like remove(), but does not change event settings

				/**
				 * remove from tree and clear all events -- for disposal only
				 */
				public abstract JQueryObject remove();

				/**
				 * fully remove all children, clearing all events
				 */
				public abstract JQueryObject empty();

				public abstract DOMNode getElement();

				public static DOMNode getDOMNode(JQueryObject jnode) {
					return (jnode == null ? null : ((DOMNode[]) (Object) jnode)[0]);
				}
			}		

		}

}
