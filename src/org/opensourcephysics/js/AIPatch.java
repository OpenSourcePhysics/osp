package org.opensourcephysics.js;

import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;

import org.opensourcephysics.display.OSPRuntime;

/**
 * A static class for collecting AI-generated j2sNative code
 * 
 * Reviewed and revised by Bob Hanson
 * 
 * Many of these features are good general solutions. They have been ported to
 * SwingJS JSUtil.java
 * 
 * @author ChatGP+
 * @author Bob Hanson
 *
 */
public class AIPatch {

	@Deprecated
	public static void addButtonTouchListeners(JButton button, Runnable onBegin, Runnable onEnd, Runnable onCancel) {
		return;
		/**
		 * ???
		 * 
		 * @j2sNative
		 * var node = button.getUI$().getDOMNode$();
		 * if (!node || node._trackerViewTouch) 
		 *   return;
		 * 
		 * node._trackerViewTouch = true;
		 * node.style.touchAction = "none";
		 * node.style.webkitTouchCallout = "none";
		 * var pointer = null, startX, startY, suppressClickUntil = 0;
		 * function consume(event) {
		 *   event.preventDefault();
		 *   event.stopImmediatePropagation();
		 * }
		 * node.addEventListener("pointerdown", function(event) {
		 *   if (event.pointerType !== "touch" && event.pointerType !== "pen") {
		 *     suppressClickUntil = 0;
		 *     return;
		 *   }
		 *   if (!event.isPrimary || !button.isEnabled$()) return;
		 *   pointer = event.pointerId;
		 *   startX = event.clientX;
		 *   startY = event.clientY;
		 *   if (event.isTrusted) node.setPointerCapture(pointer);
		 *   consume(event);
		 *   onBegin.run$();
		 * }, true);
		 * node.addEventListener("pointermove", function(event) {
		 *   if (event.pointerId !== pointer) return;
		 *   var dx = event.clientX - startX, dy = event.clientY - startY;
		 *   if (dx * dx + dy * dy > 144) onCancel.run$();
		 *   consume(event);
		 * }, true);
		 * function end(event) {
		 *   if (event.pointerId !== pointer) return;
		 *   pointer = null;
		 *   suppressClickUntil = Date.now() + 500;
		 *   consume(event);
		 *   switch (event.type) {
		 *   case "pointerup":
		 *      onEnd.run$();
		 *      break;
		 *   case "pointercancel":
		 *      onCancel.run$();
		 *      break;
		 *   }
		 * }
		 * node.addEventListener("pointerup", end, true);
		 * node.addEventListener("pointercancel", end, true);
		 * ["touchstart", "touchmove", "touchend", "touchcancel", "contextmenu"].forEach(function(type) {
		 *   node.addEventListener(type, function(event) {
		 *     if (pointer !== null || Date.now() < suppressClickUntil) consume(event);
		 *   }, {capture: true, passive: false});
		 * });
		 * node.addEventListener("click", function(event) {
		 *   if (event.detail && Date.now() < suppressClickUntil) consume(event);
		 * }, true);
		 */
	}

	@SuppressWarnings("unused")
	@Deprecated
	private static void moveFromMenuBar(JFrame frame, int x, int y) {
		Rectangle viewport = OSPRuntime.jsutil.getMaximumViewport(2, 2);
		int minX = viewport.x + Math.min(48, frame.getWidth()) - frame.getWidth();
		int maxX = viewport.x + viewport.width - Math.min(48, frame.getWidth());
		int maxY = viewport.y + Math.max(0, viewport.height - 30);
		frame.setLocation(Math.max(minX, Math.min(x, maxX)), Math.max(viewport.y, Math.min(y, maxY)));
	}


	@Deprecated
	public static void installWindowDrag(JFrame frame, JComponent bar) {
		System.err.println("AIPatch.installWindowDrag disabled");
		return;
		/**
		 * 
		 * Huh?? Why?? Why JavaScript only??
		 * 
		 * @j2sNative
		 * var node = bar.getUI$().getDOMNode$();
		 * if (!node || node._trackerWindowDrag) return;
		 * node._trackerWindowDrag = true;
		 * node.style.touchAction = "none";
		 * var pointer = null, startX, startY, frameX, frameY;
		 * node.addEventListener("pointerdown", function(e) {
		 *   if (!e.isPrimary || e.button !== 0 || !bar.isEnabled$() || frame.maximized) return;
		 *   // SwingJS menu labels can be plain DOM nodes with no button role.
		 *   // Capture gestures only on the bar background.
		 *   if (e.target !== node) return;
		 *   pointer = e.pointerId;
		 *   startX = e.clientX;
		 *   startY = e.clientY;
		 *   frameX = frame.getX$();
		 *   frameY = frame.getY$();
		 *   node.setPointerCapture(pointer);
		 *   e.preventDefault();
		 *   e.stopPropagation();
		 * }, true);
		 * node.addEventListener("pointermove", function(e) {
		 *   if (e.pointerId !== pointer) return;
		 *   var dx = e.clientX - startX, dy = e.clientY - startY;
		 *   if (Math.abs(dx) + Math.abs(dy) >= 4)
		 *     C$.moveFromMenuBar$I$I(Math.round(frameX + dx), Math.round(frameY + dy));
		 *   e.stopPropagation();
		 * }, true);
		 * node.addEventListener("lostpointercapture", function(e) {
		 *   if (e.pointerId === pointer) pointer = null;
		 * });
		 */
	}


	/// unused; adapted

	// BH: Much more sensible to just use Java override and JavaScript UI
	// clientProperties to do this.
//  See TButton.TStickyMenuItem and TButton.TStickyPopupButton -- very simple there!
//	/**
//	 * Keeps browser context menus open through the release of their opening press.
//	 * Install on the actual menu, since marker menus may belong to a track rather
//	 * than this panel's background popup.
//	 */
//static public void installContextPopupGuard(JPanel panel, JPopupMenu menu) {
//		if (!OSPRuntime.isJS || menu == null)
//			return;
//
//		@SuppressWarnings("unused")
//		Runnable onShow = () -> keepContextPopupOpen(panel, menu);
//
//		return;
//
//		/** -j2sNative
//		 // A track can reuse its menu in another panel, so update the owner.
//		 menu._trackerContextPopupOnShow = onShow;
//		 if (menu._trackerContextPopupShowInstalled) return;
//		 menu._trackerContextPopupShowInstalled = true;
//		 var show = menu.show$java_awt_Component$I$I;
//		 menu.show$java_awt_Component$I$I = function(invoker, x, y) {
//		  var result = show.apply(this, arguments);
//		  // Arm after SwingJS has created and displayed the actual menu node.
//		  if (this.$ulTop && this.$ulTop[0].style.display !== "none")
//		   this._trackerContextPopupOnShow.run$();
//		  return result;
//		 };
//		 var setVisible = menu.setVisible$Z;
//		 menu.setVisible$Z = function(visible) {
//		  if (!visible && this._trackerContextPopupCleanup)
//		   this._trackerContextPopupCleanup();
//		  return setVisible.apply(this, arguments);
//		 };
//		 */
//	}
//
//	/**
//	 * Keeps browser context menus open through the release of their opening press.
//	 * Install on the actual menu, since marker menus may belong to a track rather
//	 * than this panel's background popup.
//	 */
//	static private void keepContextPopupOpen(JPanel panel, JPopupMenu popup) {
//		return;
//		/** =j2sNative
//		 if (popup._trackerContextPopupCleanup) popup._trackerContextPopupCleanup();
//		 var openingRelease = true;
//		 var touchReleaseTime = null;
//		 var observer;
//		 var cleanup = function() {
//		  document.removeEventListener("pointerdown", press, true);
//		  document.removeEventListener("mousedown", press, true);
//		  document.removeEventListener("touchstart", press, true);
//		  document.removeEventListener("pointerup", release, true);
//		  document.removeEventListener("pointercancel", release, true);
//		  document.removeEventListener("mouseup", release, true);
//		  document.removeEventListener("touchend", release, true);
//		  document.removeEventListener("touchcancel", release, true);
//		  document.removeEventListener("click", click, true);
//		  if (observer) observer.disconnect();
//		  delete popup._trackerContextPopupCleanup;
//		 };
//		 var press = function(e) {
//		  // Safari can synthesize a mouse press after the opening touch ends.
//		  // A genuine next touch/pointer press disarms this check first.
//		  if (openingRelease && e.type === "mousedown" && touchReleaseTime !== null
//		      && Date.now() - touchReleaseTime < 800) {
//		   if (e.cancelable) e.preventDefault();
//		   e.stopImmediatePropagation();
//		   return;
//		  }
//		  openingRelease = false;
//		  var node = popup.$ulTop && popup.$ulTop[0];
//		  if (!node) node = popup.秘getUI$().domNode;
//		  if (node && node.contains(e.target)) return;
//		  cleanup();
//		  panel.hidePopup$();
//		 };
//		 var release = function(e) {
//		  if (!openingRelease) return;
//		  if (e.type.indexOf("touch") === 0 || e.pointerType === "touch" || e.pointerType === "pen") {
//		   touchReleaseTime = Date.now();
//		  }
//		  // The context action already supplied its Java release. Consume the
//		  // physical release once, before duplicate SwingJS/document listeners.
//		  if (e.cancelable) e.preventDefault();
//		  e.stopImmediatePropagation();
//		 };
//		 var click = function(e) {
//		  if (!openingRelease) return;
//		  if (e.cancelable) e.preventDefault();
//		  e.stopImmediatePropagation();
//		 };
//		 popup._trackerContextPopupCleanup = cleanup;
//		 var capture = {capture:true, passive:false};
//		 document.addEventListener("pointerdown", press, capture);
//		 document.addEventListener("mousedown", press, capture);
//		 document.addEventListener("touchstart", press, capture);
//		 document.addEventListener("pointerup", release, capture);
//		 document.addEventListener("pointercancel", release, capture);
//		 document.addEventListener("mouseup", release, capture);
//		 document.addEventListener("touchend", release, capture);
//		 document.addEventListener("touchcancel", release, capture);
//		 document.addEventListener("click", click, capture);
//		 var node = popup.$ulTop && popup.$ulTop[0];
//		 if (node) {
//		  observer = new MutationObserver(function() {
//		   if (!node.isConnected || node.style.display === "none") cleanup();
//		  });
//		  observer.observe(node, {attributes:true, attributeFilter:["style"]});
//		 }
//		 */
//	}
//
//
//	
//	
//	/** 
//	 * Keeps the view list open through the release of its opening long press. 
//	 */
//	public static void keepPopupOpenUntilSelection(JButton button, JPopupMenu popup) {
//		return;
//		/** -j2sNative
//			var node = popup.秘getUI$().domNode;
//			if (popup._trackerPopupCleanup) popup._trackerPopupCleanup();
//			var widget = popup.$ulTop && popup.$ulTop.data("ui-j2smenu");
//			var collapse = widget && widget.collapseAll;
//			// This list closes on selection or an outside press, not a hover/blur timer.
//			if (widget) widget.collapseAll = function() {};
//			// Window activation can schedule a global menu close after this list opens.
//			// Dismiss this list only through its explicit selection/outside-press handlers.
//			var popupUI = swingjs.plaf.JSPopupMenuUI;
//			var closeMenusNow = popupUI.closeAllMenusNOW$;
//			var keepViewListOpen = function() {};
//			popupUI.closeAllMenusNOW$ = keepViewListOpen;
//			var openingRelease = true;
//			var observer;
//			var cleanup = function() {
//			 document.removeEventListener("pointerdown", press, true);
//			 document.removeEventListener("mousedown", press, true);
//			 document.removeEventListener("touchstart", press, true);
//			 document.removeEventListener("pointerup", release, true);
//			 document.removeEventListener("mouseup", release, true);
//			 document.removeEventListener("touchend", release, true);
//			 document.removeEventListener("click", click, true);
//			 document.removeEventListener("keydown", key, true);
//			 if (observer) observer.disconnect();
//			 for (var i = 0; i < items.length; i++)
//			  if (items[i].removeActionListener$java_awt_event_ActionListener)
//			   items[i].removeActionListener$java_awt_event_ActionListener(itemListener);
//			 if (widget) widget.collapseAll = collapse;
//			 if (popupUI.closeAllMenusNOW$ === keepViewListOpen)
//			  popupUI.closeAllMenusNOW$ = closeMenusNow;
//			 delete popup._trackerPopupCleanup;
//			};
//			var close = function() {
//			 cleanup();
//			 button.hidePopup = false;
//			 popup.setVisible$Z(false);
//			 // Swing's visibility flag can already be false after touch activation.
//			 // Force the native menu to hide as well.
//			 if (J2S.Swing && J2S.Swing.hideMenu) J2S.Swing.hideMenu(popup, true);
//			};
//			var press = function(e) {
//			 openingRelease = false;
//			 if (!node.contains(e.target)) close();
//			};
//			var release = function(e) {
//			 if (!openingRelease) return;
//			 // Consume only the release of the press that opened this list.
//			 // Disarm first so releasing the Swing button cannot fire its normal action.
//			 var model = button.getModel$();
//			 model.setArmed$Z(false);
//			 model.setPressed$Z(false);
//			 if (e.cancelable) e.preventDefault();
//			 e.stopImmediatePropagation();
//			};
//			var click = function(e) {
//			 if (openingRelease) {
//			  e.stopImmediatePropagation();
//			  return;
//			 }
//			 var item = e.target.closest(".ui-j2smenu-item");
//			 if (item && node.contains(item) && !item.closest(".ui-state-disabled")) {
//			  // Let the menu item perform its action before dismissing the list.
//			  setTimeout(close, 0);
//			 }
//			};
//			var key = function(e) { if (e.key === "Escape") close(); };
//			// Touch menu items can fire their Swing action without a DOM click.
//			var items = popup.getComponents$();
//			var itemListener = {
//			 actionPerformed$java_awt_event_ActionEvent: function() { close(); },
//			 equals$O: function(other) { return this === other; }
//			};
//			for (var i = 0; i < items.length; i++)
//			 if (items[i].addActionListener$java_awt_event_ActionListener)
//			  items[i].addActionListener$java_awt_event_ActionListener(itemListener);
//			popup._trackerPopupCleanup = cleanup;
//			document.addEventListener("pointerdown", press, true);
//			document.addEventListener("mousedown", press, true);
//			document.addEventListener("touchstart", press, true);
//			document.addEventListener("pointerup", release, true);
//			document.addEventListener("mouseup", release, true);
//			document.addEventListener("touchend", release, true);
//			document.addEventListener("click", click, true);
//			document.addEventListener("keydown", key, true);
//			observer = new MutationObserver(function() {
//			 if (!node.isConnected || node.style.display === "none") cleanup();
//			});
//			observer.observe(node, {attributes:true, attributeFilter:["style"]});
//		 */
//	}

//	private static final String RESIZE_HANDLER_KEY = "TWindowResizeHandler_Installed"; //$NON-NLS-1$

//	public static void installResizeHandler(JFrame window) {
//		window.addComponentListener(new ComponentAdapter() {
//		setupResizer(window);
//
//		// BH This is a hack
//
//		// In SwingJS, Resizer DOM node may be created asynchronously when window peer finishes initializing
//		if (OSPRuntime.isJS) {
//			OSPRuntime.trigger(100, (e) -> {
//				setupResizer(window);
//			});
//			OSPRuntime.trigger(500, (e) -> {
//				setupResizer(window);
//			});
//			OSPRuntime.trigger(1500, (e) -> {
//				setupResizer(window);
//			});
//		}
//	}

//	public static void closeAllMenus() {
//	// BH NOT ACCEPTABLE?
//	/**
//	 * -j2sNative
//	 * try {
//	 *   if (swingjs && swingjs.plaf && swingjs.plaf.JSPopupMenuUI) {
//	 *     swingjs.plaf.JSPopupMenuUI.closeAllMenus$();
//	 *   }
//	 *   if (swingjs && swingjs.plaf && swingjs.plaf.JSComponentUI) {
//	 *     swingjs.plaf.JSComponentUI.hideMenusAndToolTip$();
//	 *   }
//	 *   var applet = this.getFrameViewer$ ? (this.getFrameViewer$() && this.getFrameViewer$().applet) : null;
//	 *   if (!applet && window.J2S && J2S._applets) {
//	 *     for (var a in J2S._applets) {
//	 *       applet = J2S._applets[a];
//	 *       if (applet && applet._menus) break;
//	 *     }
//	 *   }
//	 *   if (applet && applet._menus && window.J2S && J2S.Swing && J2S.Swing.hideMenu) {
//	 *     for (var i in applet._menus) {
//	 *       J2S.Swing.hideMenu(applet._menus[i], true);
//	 *     }
//	 *   }
//	 *   if (window.$) {
//	 *     $(".ui-j2smenu:visible, .swingjsPopupMenu:visible").hide().attr("aria-hidden", "true").attr("aria-expanded", "false");
//	 *     $(".ui-j2smenu-node").removeClass("ui-state-active").removeClass("ui-state-focus");
//	 *   }
//	 * } catch (e) {}
//	 */
//}
//
//
//public static boolean haveAnyVisibleMenusInAnyApplication() {
//	// BH NOT ACCEPTABLE?
//	/**
//	 * -j2sNative
//	 * return (window.$ && $(".ui-j2smenu:visible, .swingjsPopupMenu:visible").length > 0);
//	 */
//	return false;
//}
//
//
//public static void hackUIDOMNodeStyle(JComponent jc, String... styles) {
//	DOMNode node = /** -j2sNative jc.ui.domNode || */null;
//	DOMNode.setStyles(node, styles);
//}

//	public static void hackWelcomePane(JEditorPane ep, String rawHTML) {
//		// BH NOT ACCEPTABLE?
//		// In SwingJS on iPad / Safari: directly inject and style the DOM elements
//		JEditorPane thePane = ep;
//		@SuppressWarnings("unused")
//		TextUI ui = thePane.getUI();
//		@SuppressWarnings("unused")
//		DOMNode domNode = (/** -j2sNative ui.domNode || */
//		null);
//		Runnable injectDOM = () -> {
//			System.err.println("!!!AIP inject");
//			// make explicitly final in JavaScript as well
//			// we do not want to use Java's this.$finals$ here.
//			DOMNode dnode = (/** -j2sNative domNode || */ null);
//			DOMNode rHTML = (/** -j2sNative rawHTML || */ null);
//			/**
//			 * -j2sNative ui.mytext = null; ui.rawHTML = null; ui.currentHTML = null;
//			 *            ui.setText$S(rHTML);
//			 */
//			DOMNode target = (/** -j2sNative ui.bodyNode ||*/ dnode);
//			String html = (String) DOMNode.getAttr(target, "innerHTML");
//			if (html.indexOf("Open Source Physics") < 0) {
//				DOMNode.setAttr(target, "innerHTML", rHTML);
//			}
//			DOMNode.setStyles(dnode, //
//					"width", "100%", //
//					"height", "100%", //
//					"minHeight", "350px", //
//					"display", "block", //
//					"backgroundColor", "#ffffff", //
//					"webkitOverflowScrolling", "touch", //
//					"boxSizing", "border-box");
//			DOMNode.setStyles(target, //
//					"width", "100%", //
//					"display", "block", //
//					"color", "#000000", //
//					"backgroundColor", "#ffffff", //
//					"padding", "20px", //
//					"fontFamily", "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif", //
//					"boxSizing", "border-box");
//			DOMNode parentElement = (DOMNode) DOMNode.getAttr(dnode, "parentElement");
//			if (parentElement != null) {
//				DOMNode.setStyles(parentElement, //
//						"width", "100%", //
//						"overflow", "auto", //
//						"backgroundColor", "#ffffff");
//				parentElement = (DOMNode) DOMNode.getAttr(parentElement, "parentElement");
//				if (parentElement != null) {
//					DOMNode.setStyles(parentElement, //
//							"width", "100%", //
//							"height", "100%", //
//							"backgroundColor", "#ffffff");
//				}
//			}
//
//		};
//		injectDOM.run();
//		/**
//		 * -j2sNative
//		 * 
//		 * 			if (window.requestAnimationFrame) {
//		 *            window.requestAnimationFrame(injectDOM.run$); }
//		*/
//		setTimeout(injectDOM, 30);
//		setTimeout(injectDOM, 100);
//		setTimeout(injectDOM, 250);
//		setTimeout(injectDOM, 500);
//		setTimeout(injectDOM, 1000);
//	}
//
//
//	private static void setTimeout(Runnable r, int ms) {
//		/**
//		 * -j2sNative
//		 * 
//		 * setTimeout(r.run$, ms); 
//		 */
//	}
//

}