/*
Hooks the context menu into MKtlGUI without editing Modality-toolkit.

This file OVERWRITES two existing Modality methods. sclang supports this
explicitly - it counts overwritten methods at startup and lists them via
MethodOverride.printAll - but they are the coupling points to re-check after
a Modality update:

  MKtlGUI:*new                 (Modality/Classes/GUI/MKtlGUI.sc)
  MKtlElementView:snapback_    (Modality/Classes/GUI/MKtlGUI.sc)

Part of the ModalityContextMenu extension.
*/

+ MKtlGUI {

	// OVERWRITES MKtlGUI:*new - one line reproduced from upstream, plus the hook.
	//
	// Hooking *new rather than MKtl:gui is deliberate. MKtl:gui is itself an
	// extension method (in MKtlElementGUI.sc), and extension-overwrites-extension
	// resolves by compile order, whereas extension-overwrites-class-file is the
	// well-defined case. *new also covers both entry points: k.gui and the
	// direct MKtlGUI(mktl: k) form shown in MKtlGUI's helpfile.
	//
	// init contains no ^, so it returns this; addContextMenus does too.
	*new { |parent, bounds, mktl|
		^super.newCopyArgs( mktl, parent ).init( bounds ).addContextMenus;
	}

	addContextMenus {
		// groupType groups share one underlying view across several
		// MKtlElementViews, so group by view first - otherwise the last
		// attach would silently win and the rest become unreachable.
		var byQtView = IdentityDictionary.new;

		views.do { |elView|
			MKtlElementMenu.qtViewOf(elView.view) !? { |qtView|
				byQtView.put(qtView, (byQtView[qtView] ? []).add(elView));
			};
		};

		byQtView.keysValuesDo { |qtView, elViews|
			// snapback_ no longer installs this (see below), so do it here -
			// and skip button 1, so a right-click no longer fires the action.
			if (qtView.isKindOf(Button)) {
				qtView.mouseDownAction = { |vw, x, y, mod, btn|
					var elView = elViews.first;
					if (btn != 1 and: { elView.snapback }) {
						vw.valueAction = 1 - elView.snapbackValue;
					};
				};
			};
			MKtlElementMenu.attachTo(qtView, elViews);
		};

		^this
	}
}

+ MKtlElementView {

	// OVERWRITES MKtlElementView:snapback_ to be flag-only.
	//
	// Upstream this method installs and removes view.mouseDownAction directly.
	// That has to go: the view's keyDownAction lets the user press t / m over a
	// widget at any time, which would re-assign mouseDownAction and blow away
	// the context menu wrapper. There is only one snapback_ after compile, so
	// this version is also what runs during makeView - which means no click
	// handler is installed there either, and MKtlGUI:addContextMenus becomes
	// the single place that wires up mouse behaviour.
	snapback_ { |flag = true, post = true|
		snapback = flag;
		if (post) {
			if (flag) {
				"% gui: switching to momentary action/snapback.\n".postf(element);
			} {
				"% gui: switching to toggle action.\n".postf(element);
			};
		};
	}

	qtView { ^MKtlElementMenu.qtViewOf(view) }
}
