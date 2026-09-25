package org.opensourcephysics.js;

import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;
import javax.swing.plaf.TextUI;

import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.media.core.NumberField;

import swingjs.api.js.DOMNode;

/**
 * A static class for collecting AI-generated j2sNative code
 * @author hanso
 *
 */
public class AIPatch {

	
	private static final String RESIZE_HANDLER_KEY = "TWindowResizeHandler_Installed"; //$NON-NLS-1$

	/**
	 * Installs the window resize handler on the specified window.
	 *
	 * @param window the top-level Window (TFrame or JDialog)
	 */
	public static void installResizeHandler(Window window) {
		if (window == null) return;

		if (window instanceof RootPaneContainer) {
			JRootPane rp = ((RootPaneContainer) window).getRootPane();
			if (rp != null) {
				if (rp.getClientProperty(RESIZE_HANDLER_KEY) != null) {
					setupResizer(window);
					isolateWindowGestures(window);
					return; // already installed
				}
				rp.putClientProperty(RESIZE_HANDLER_KEY, Boolean.TRUE);
			}
		}

		window.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				setupResizer(window);
				isolateWindowGestures(window);
			}

			@Override
			public void componentResized(ComponentEvent e) {
				setupResizer(window);
				isolateWindowGestures(window);
			}
		});

		setupResizer(window);
		isolateWindowGestures(window);

		// In SwingJS, Resizer DOM node may be created asynchronously when window peer finishes initializing
		if (OSPRuntime.isJS) {
			OSPRuntime.trigger(100, (e) -> {
				setupResizer(window);
				isolateWindowGestures(window);
			});
			OSPRuntime.trigger(500, (e) -> {
				setupResizer(window);
				isolateWindowGestures(window);
			});
			OSPRuntime.trigger(1500, (e) -> {
				setupResizer(window);
				isolateWindowGestures(window);
			});
		}
	}

	
	/**
	 * Isolates the window DOM node from propagating gestures to the host HTML page in SwingJS.
	 * Prevents pinch-to-zoom, two-finger pan, overscroll rubber-banding, and touch drag bubbling
	 * from escaping the Tracker window to the surrounding webpage.
	 *
	 * @param window the top-level Window (TFrame or JDialog)
	 */
	public static void isolateWindowGestures(final Window window) {
		if (window == null) return;
		if (!OSPRuntime.isJS) return;

		/**
		 * @j2sNative
		 * try {
		 *   var frame = window;
		 *   var viewer = (frame.getFrameViewer$ ? frame.getFrameViewer$() : (frame.秘frameViewer || null));
		 *   if (!viewer && window.getRootPane$) {
		 *     var rp = window.getRootPane$();
		 *     viewer = (rp && rp.getFrameViewer$ ? rp.getFrameViewer$() : (rp ? rp.秘frameViewer : null));
		 *   }
		 *   
		 *   var frameNode = (frame.ui && frame.ui.frameNode ? frame.ui.frameNode : (frame.ui && frame.ui.domNode ? frame.ui.domNode : null));
		 *   if (!frameNode && frame.秘htmlName) {
		 *     frameNode = document.getElementById(frame.秘htmlName + "_frame") || document.getElementById(frame.秘htmlName);
		 *   }
		 *   if (!frameNode && window.getRootPane$) {
		 *     var rp = window.getRootPane$();
		 *     frameNode = (rp && rp.ui && rp.ui.domNode ? rp.ui.domNode : (rp && rp.秘htmlName ? document.getElementById(rp.秘htmlName) : null));
		 *   }
		 *   
		 *   var rp = (window.getRootPane$ ? window.getRootPane$() : null);
		 *   var rpNode = (rp ? (rp.ui && rp.ui.domNode ? rp.ui.domNode : (rp.秘htmlName ? document.getElementById(rp.秘htmlName) : null)) : null);
		 *   
		 *   var isolate = function(node) {
		 *     if (!node || node._tGestureIsolated) return;
		 *     node._tGestureIsolated = true;
		 *     
		 *     // 1. CSS Touch & Overscroll Containment
		 *     node.style.touchAction = "none";
		 *     node.style.overscrollBehavior = "none";
		 *     node.style.webkitUserSelect = "none";
		 *     node.style.userSelect = "none";
		 *     node.style.webkitTouchCallout = "none";
		 *     
		 *     // 2. Suppress iOS Safari gesture events (pinch zoom & rotate)
		 *     var killGesture = function(e) {
		 *       e.preventDefault();
		 *       e.stopPropagation();
		 *     };
		 *     node.addEventListener("gesturestart", killGesture, { passive: false });
		 *     node.addEventListener("gesturechange", killGesture, { passive: false });
		 *     node.addEventListener("gestureend", killGesture, { passive: false });
		 *     
		 *     // 3. Prevent multi-touch pinch/pan & stop bubbling to host HTML page
		 *     node.addEventListener("touchmove", function(e) {
		 *       if (e.touches && e.touches.length > 1) {
		 *         e.preventDefault();
		 *       }
		 *       e.stopPropagation();
		 *     }, { passive: false });
		 *     
		 *     node.addEventListener("touchstart", function(e) {
		 *       e.stopPropagation();
		 *     }, { passive: false });
		 *     
		 *     node.addEventListener("touchend", function(e) {
		 *       e.stopPropagation();
		 *     }, { passive: false });
		 *     
		 *     // 4. Suppress trackpad pinch-to-zoom (ctrl + wheel)
		 *     node.addEventListener("wheel", function(e) {
		 *       if (e.ctrlKey) {
		 *         e.preventDefault();
		 *         e.stopPropagation();
		 *       }
		 *     }, { passive: false });
		 *   };
		 *   
		 *   isolate(frameNode);
		 *   if (rpNode && rpNode !== frameNode) {
		 *     isolate(rpNode);
		 *   }
		 *   
		 *   var appletViewer = (viewer && viewer.appletViewer ? viewer.appletViewer : null);
		 *   if (appletViewer && appletViewer.fullName) {
		 *     var appletNode = document.getElementById(appletViewer.fullName + "_appletdiv");
		 *     if (appletNode && !appletNode._tGestureIsolated) {
		 *       appletNode._tGestureIsolated = true;
		 *       appletNode.style.touchAction = "none";
		 *       appletNode.style.overscrollBehavior = "none";
		 *       var killGesture = function(e) {
		 *         e.preventDefault();
		 *         e.stopPropagation();
		 *       };
		 *       appletNode.addEventListener("gesturestart", killGesture, { passive: false });
		 *       appletNode.addEventListener("gesturechange", killGesture, { passive: false });
		 *       appletNode.addEventListener("gestureend", killGesture, { passive: false });
		 *       appletNode.addEventListener("touchmove", function(e) {
		 *         if (e.touches && e.touches.length > 1) {
		 *           e.preventDefault();
		 *         }
		 *         e.stopPropagation();
		 *       }, { passive: false });
		 *     }
		 *   }
		 * } catch (ex) {}
		 */
		{
		}
	}

	private static final int MIN_WIDTH = 320;
	private static final int MIN_HEIGHT = 220;

	/**
	 * Configures the SwingJS window resizer DOM element for touch responsiveness and visual affordance.
	 *
	 * @param window the window whose resizer should be configured
	 */
	@SuppressWarnings("unused")
	public static void setupResizer(Window window) {
		if (window == null || !OSPRuntime.isJS) 
			return;
		isolateWindowGestures(window);
		int minw = MIN_WIDTH;
		int minh = MIN_HEIGHT;
		/**
		 * @j2sNative
		 * try {
		 *   var frame = window;
		 *   var viewer = (frame.getFrameViewer$ ? frame.getFrameViewer$() : (frame.秘frameViewer || null));
		 *   if (!viewer && window.getRootPane$) {
		 *     var rp = window.getRootPane$();
		 *     viewer = (rp && rp.getFrameViewer$ ? rp.getFrameViewer$() : (rp ? rp.秘frameViewer : null));
		 *   }
		 *   var resizer = (viewer && viewer.getResizer$ ? viewer.getResizer$() : null);
		 *   if (!resizer && viewer && viewer.newResizer) {
		 *     resizer = viewer.newResizer();
		 *   }
		 *   if (resizer && resizer.show$) {
		 *     resizer.show$();
		 *   }
		 *   if (!resizer) return;
		 *   
		 *   var resizerNode = (resizer.getDOMNode$ ? resizer.getDOMNode$() : (resizer.resizer || null));
		 *   if (!resizerNode && resizer.rootPane) {
		 *     var id = resizer.rootPane.秘htmlName + "_resizer";
		 *     resizerNode = document.getElementById(id);
		 *   }
		 *   if (!resizerNode) return;
		 *   
		 *   var rubberBandNode = resizer.rubberBand;
		 *   if (!rubberBandNode && resizer.rootPane) {
		 *     var rbid = resizer.rootPane.秘htmlName + "_resizer_rb";
		 *     rubberBandNode = document.getElementById(rbid);
		 *   }
		 *   
		 *   // Ensure rubberBand does not block touch / pointer interactions
		 *   if (rubberBandNode) {
		 *     rubberBandNode.style.pointerEvents = "none";
		 *   }
		 *   
		 *   // 1. Keep the 20x20 hotspot inside the frame to avoid page overflow
		 *   resizerNode.style.width = "20px";
		 *   resizerNode.style.height = "20px";
		 *   resizerNode.style.marginLeft = "-20px";
		 *   resizerNode.style.marginTop = "-20px";
		 *   resizerNode.style.touchAction = "none";
		 *   resizerNode.style.zIndex = "100002";
		 *   resizerNode.style.userSelect = "none";
		 *   resizerNode.style.webkitUserSelect = "none";
		 *   resizerNode.style.cursor = "nwse-resize";
		 *   
		 *   // 2. Visible diagonal corner grip lines for clear visual feedback on touch screens
		 *   resizerNode.style.opacity = "0.75";
		 *   resizerNode.style.backgroundImage = "linear-gradient(135deg, transparent 0%, transparent 50%, #888888 50%, #888888 56%, transparent 56%, transparent 68%, #888888 68%, #888888 74%, transparent 74%, transparent 86%, #888888 86%, #888888 92%, transparent 92%)";
		 *   resizerNode.style.backgroundRepeat = "no-repeat";
		 *   resizerNode.style.backgroundPosition = "right bottom";
		 *   resizerNode.style.backgroundSize = "10px 10px";
		 *   
		 *   // 3. Attach dedicated touch listeners with passive: false to prevent iOS Safari scrolling
		 *   if (!resizerNode._tTouchAttached) {
		 *     resizerNode._tTouchAttached = true;
		 *     
		 *     resizerNode.addEventListener("touchstart", function(e) {
		 *       if (!e.touches || e.touches.length === 0) return;
		 *       e.preventDefault();
		 *       e.stopPropagation();
		 *       
		 *       var touch0 = e.touches[0];
		 *       var startX = touch0.pageX;
		 *       var startY = touch0.pageY;
		 *       var startW = frame.getWidth$ ? frame.getWidth$() : (frame.width || 800);
		 *       var startH = frame.getHeight$ ? frame.getHeight$() : (frame.height || 600);
		 *       var startLoc = frame.getLocation$ ? frame.getLocation$() : { x: 0, y: 0 };
		 *       
		 *       resizerNode.style.opacity = "1.0";
		 *       if (rubberBandNode) {
		 *         rubberBandNode.style.width = startW + "px";
		 *         rubberBandNode.style.height = startH + "px";
		 *         rubberBandNode.style.display = "block";
		 *         rubberBandNode.style.pointerEvents = "none";
		 *       }
		 *       
		 *       var onTouchMove = function(me) {
		 *         if (!me.touches || me.touches.length === 0) return;
		 *         me.preventDefault();
		 *         me.stopPropagation();
		 *         var t = me.touches[0];
		 *         var dx = t.pageX - startX;
		 *         var dy = t.pageY - startY;
		 *         var curW = Math.max(minw, Math.round(startW + dx));
		 *         var curH = Math.max(minh, Math.round(startH + dy));
		 *         if (rubberBandNode) {
		 *           rubberBandNode.style.width = curW + "px";
		 *           rubberBandNode.style.height = curH + "px";
		 *         }
		 *         resizerNode.style.left = (curW - 4) + "px";
		 *         resizerNode.style.top = (curH - 4) + "px";
		 *       };
		 *       
		 *       var onTouchEnd = function(ue) {
		 *         ue.preventDefault();
		 *         ue.stopPropagation();
		 *         window.removeEventListener("touchmove", onTouchMove, { passive: false, capture: true });
		 *         window.removeEventListener("touchend", onTouchEnd, { passive: false, capture: true });
		 *         window.removeEventListener("touchcancel", onTouchEnd, { passive: false, capture: true });
		 *         
		 *         resizerNode.style.opacity = "0.75";
		 *         if (rubberBandNode) {
		 *           rubberBandNode.style.display = "none";
		 *         }
		 *         
		 *         var endTouch = (ue.changedTouches && ue.changedTouches.length > 0 ? ue.changedTouches[0] : touch0);
		 *         var dx = endTouch.pageX - startX;
		 *         var dy = endTouch.pageY - startY;
		 *         var finalW = Math.max(minw, Math.round(startW + dx));
		 *         var finalH = Math.max(minh, Math.round(startH + dy));
		 *         
		 *         if (frame.setSize$I$I) {
		 *           frame.setSize$I$I(finalW, finalH);
		 *         } else if (frame.setBounds$I$I$I$I) {
		 *           frame.setBounds$I$I$I$I(startLoc.x, startLoc.y, finalW, finalH);
		 *         }
		 *         if (frame.setPreferredSize$java_awt_Dimension) {
		 *           frame.setPreferredSize$java_awt_Dimension(Clazz.new_(java.awt.Dimension.c$$I$I, [finalW, finalH]));
		 *         }
		 *         if (frame.validate$) {
		 *           frame.validate$();
		 *         }
		 *         if (frame.repaint$) {
		 *           frame.repaint$();
		 *         }
		 *         if (resizer && resizer.setPosition$I$I) {
		 *           resizer.setPosition$I$I(0, 0);
		 *         } else {
		 *           resizerNode.style.left = (finalW - 4) + "px";
		 *           resizerNode.style.top = (finalH - 4) + "px";
		 *         }
		 *         if (frame.frameResized$) {
		 *           frame.frameResized$();
		 *         }
		 *         if (frame.getSelectedPanel$) {
		 *           var tp = frame.getSelectedPanel$();
		 *           if (tp && org.opensourcephysics.cabrillo.tracker.TFrame && org.opensourcephysics.cabrillo.tracker.TFrame.repaintT) {
		 *             org.opensourcephysics.cabrillo.tracker.TFrame.repaintT(tp);
		 *           }
		 *         }
		 *       };
		 *       
		 *       window.addEventListener("touchmove", onTouchMove, { passive: false, capture: true });
		 *       window.addEventListener("touchend", onTouchEnd, { passive: false, capture: true });
		 *       window.addEventListener("touchcancel", onTouchEnd, { passive: false, capture: true });
		 *     }, { passive: false });
		 *   }
		 * } catch (ex) {}
		 */
		{
		}
	}


	public static void closeAllMenus() {
		// BH NOT ACCEPTABLE?
		/**
		 * @j2sNative
		 * try {
		 *   if (swingjs && swingjs.plaf && swingjs.plaf.JSPopupMenuUI) {
		 *     swingjs.plaf.JSPopupMenuUI.closeAllMenus$();
		 *   }
		 *   if (swingjs && swingjs.plaf && swingjs.plaf.JSComponentUI) {
		 *     swingjs.plaf.JSComponentUI.hideMenusAndToolTip$();
		 *   }
		 *   var applet = this.getFrameViewer$ ? (this.getFrameViewer$() && this.getFrameViewer$().applet) : null;
		 *   if (!applet && window.J2S && J2S._applets) {
		 *     for (var a in J2S._applets) {
		 *       applet = J2S._applets[a];
		 *       if (applet && applet._menus) break;
		 *     }
		 *   }
		 *   if (applet && applet._menus && window.J2S && J2S.Swing && J2S.Swing.hideMenu) {
		 *     for (var i in applet._menus) {
		 *       J2S.Swing.hideMenu(applet._menus[i], true);
		 *     }
		 *   }
		 *   if (window.$) {
		 *     $(".ui-j2smenu:visible, .swingjsPopupMenu:visible").hide().attr("aria-hidden", "true").attr("aria-expanded", "false");
		 *     $(".ui-j2smenu-node").removeClass("ui-state-active").removeClass("ui-state-focus");
		 *   }
		 * } catch (e) {}
		 */
	}


	public static boolean haveAnyVisibleMenusInAnyApplication() {
		// BH NOT ACCEPTABLE?
		/**
		 * @j2sNative
		 * return (window.$ && $(".ui-j2smenu:visible, .swingjsPopupMenu:visible").length > 0);
		 */
		return false;
	}


	public static void hackUIDOMNodeStyle(JComponent jc, String... styles) {
		DOMNode node = /** @j2sNative jc.ui.domNode || */null;
		DOMNode.setStyles(node, styles);
	}


	public static void disposeElement(String id) {
		DOMNode.dispose(DOMNode.getElement(id));
//		/**
//		 * @-j2sNative
//		 * var splash = document.getElementById();
//		 * if (splash && splash.parentNode) splash.parentNode.removeChild(splash);
//		 */
//		{}
	}


	public static void addWindowOrientationChangeListener(Runnable onOrient) {
		/**
		 * @j2sNative window.addEventListener(window.onorientationchange ?
		 *            "orientationchange" : "resize", function() {
		 *              console.log("Orientation changed"); 
		 *              onOrient.run$(); 
		 *            }, false);
		 */
	}

	public static void hackWelcomePane(JEditorPane ep, String rawHTML) {
		// BH NOT ACCEPTABLE?
		// In SwingJS on iPad / Safari: directly inject and style the DOM elements
		JEditorPane thePane = ep;
		@SuppressWarnings("unused")
		TextUI ui = thePane.getUI();
		@SuppressWarnings("unused")
		DOMNode domNode = (/** @j2sNative ui.domNode || */
		null);
		Runnable injectDOM = () -> {
			System.err.println("!!!AIP inject");
			// make explicitly final in JavaScript as well
			// we do not want to use Java's this.$finals$ here.
			DOMNode dnode = (/** @j2sNative domNode || */ null);
			DOMNode rHTML = (/** @j2sNative rawHTML || */ null);
			/**
			 * @j2sNative ui.mytext = null; ui.rawHTML = null; ui.currentHTML = null;
			 *            ui.setText$S(rHTML);
			 */
			DOMNode target = (/** @j2sNative ui.bodyNode ||*/ dnode);
			String html = (String) DOMNode.getAttr(target, "innerHTML");
			if (html.indexOf("Open Source Physics") < 0) {
				DOMNode.setAttr(target, "innerHTML", rHTML);
			}
			DOMNode.setStyles(dnode, //
					"width", "100%", //
					"height", "100%", //
					"minHeight", "350px", //
					"display", "block", //
					"backgroundColor", "#ffffff", //
					"webkitOverflowScrolling", "touch", //
					"boxSizing", "border-box");
			DOMNode.setStyles(target, //
					"width", "100%", //
					"display", "block", //
					"color", "#000000", //
					"backgroundColor", "#ffffff", //
					"padding", "20px", //
					"fontFamily", "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif", //
					"boxSizing", "border-box");
			DOMNode parentElement = (DOMNode) DOMNode.getAttr(dnode, "parentElement");
			if (parentElement != null) {
				DOMNode.setStyles(parentElement, //
						"width", "100%", //
						"overflow", "auto", //
						"backgroundColor", "#ffffff");
				parentElement = (DOMNode) DOMNode.getAttr(parentElement, "parentElement");
				if (parentElement != null) {
					DOMNode.setStyles(parentElement, //
							"width", "100%", //
							"height", "100%", //
							"backgroundColor", "#ffffff");
				}
			}

		};
		injectDOM.run();
		/**
		 * @j2sNative
		 * 
		 * 			if (window.requestAnimationFrame) {
		 *            window.requestAnimationFrame(injectDOM.run$); }
		*/
		setTimeout(injectDOM, 30);
		setTimeout(injectDOM, 100);
		setTimeout(injectDOM, 250);
		setTimeout(injectDOM, 500);
		setTimeout(injectDOM, 1000);
	}


	private static void setTimeout(Runnable r, int ms) {
		/**
		 * @j2sNative
		 * 
		 * setTimeout(r.run$, ms); 
		 */
	}


	/**
	 * BH this does not look right to me
	 * 
	 * Gets the screen or page location of a mouse event, compatible with desktop Java
	 * and touch events in SwingJS on iPad.
	 */
	public static Point getScreenLocation(MouseEvent e, Component comp, String from) {
		Point p = e.getLocationOnScreen();
		System.err.println("\nAIP getScLoc simple: " + p + " from " + from);
		int[] pt = null;
		/**
		 * @j2sNative
		 * try {
		 *   var je = (e && e.bdata ? e.bdata.jqevent : null);
		 *   if (je) {
		 *     var oe = je.originalEvent || je;
		 *     var t = (oe.targetTouches && oe.targetTouches.length > 0 ? oe.targetTouches[0] : null);
		 *     var px = (t ? t.pageX : (je.pageX != null ? je.pageX : null));
		 *     var py = (t ? t.pageY : (je.pageY != null ? je.pageY : null));
		 *     if (px == null && window.J2S && J2S._mousePageX != null) {
		
			System.err.println("AIP getScLoc using J2S._mousePageX/Y ");
		 
		 *       px = J2S._mousePageX;
		 *       py = J2S._mousePageY;
		 *     }
		 *     if (px != null && isFinite(px) && py != null && isFinite(py)) {
		 *       pt = [Math.round(px), Math.round(py)];
		 *     }
		 *   }
		 * } catch (ex) {}
		 */
		if (pt != null) {
			p = new Point(pt[0], pt[1]);
			System.err.println("AIP getScLoc AI calc " + p);
			return p;
		}
		if (p != null && (p.x != 0 || p.y != 0)) {
			return p;
		}
		if (comp != null && comp.isShowing()) {
			try {
				p = comp.getLocationOnScreen();
				p = new Point(p.x + e.getX(), p.y + e.getY());
				System.err.println("AIP getScLoc returning component offset " + p);
			} catch (Throwable t) {
			}
		}
		
		p = e.getPoint();
		System.err.println("AIP getScLoc returning e.getPoint() " + p);
		return p;
	}

	public static void virtualNumberPad(NumberField nf, String action) {
		switch (action) {
		case "add":
		/**
		 * @j2sNative
		 * 
		 * 			J2S.Mobile.addNumberPad(nf);
		 */
		{
		}
		//$FALL-THROUGH$
		case "show":
		/**
		 * @j2sNative
		 * 
		 * 			J2S.Mobile.showNumberPad(nf);
		 */
		{
		}
		}
	}
}