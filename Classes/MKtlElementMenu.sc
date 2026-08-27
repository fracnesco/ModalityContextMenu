/*
The context menu shown on MKtlGUI element views, opened by right-click or
alt(option)-click.

All class-side: MKtlGUI:addContextMenus wires it up, and this class builds a
fresh Menu on each menu click so that what it shows is always current.

Part of the ModalityContextMenu extension.
*/

MKtlElementMenu {

	// QMenu::popup is non-modal, so hold the Menu here to keep it from being
	// collected while it is on screen.
	classvar <currentMenu;

	// MKtlElementView:view is one of four shapes:
	//   MButtonView / MSliderView / MKnobView / NumberBox  - already Views
	//   MPadView / MHexPad / MRoundPad                     - SCViewHolder, one hop
	//   MPadUpViewRedirect / MPadMoveViewRedirect          - two hops
	// Follow .view until we reach the real View underneath.
	*qtViewOf { |obj|
		var node = obj, guard = 8;
		while { node.notNil and: { node.isKindOf(View).not } and: { guard > 0 } } {
			guard = guard - 1;
			node = if (node.respondsTo(\view)) { node.view } { nil };
		};
		^if (node.isKindOf(View)) { node } { nil }
	}

	// Wrap rather than replace the existing handlers, so MPadView's own
	// mouse actions and MHexPad:upDoesAction_ keep working.
	//
	// The menu opens on right-click or alt(option)-click. Returning true
	// from the action consumes the event in C++ before the widget's own Qt
	// handling runs (the same mechanism QView:mouseDownEvent uses to block
	// a view while ctrl-dragging) - without it, Button, Knob and Slider
	// still change value on a menu click even though the wrapped sclang
	// actions are skipped. The matching mouseUp is consumed via the
	// menuClick flag rather than by re-testing btn/mod, so a pad release
	// still fires valueAction = 0 even if alt changed state mid-gesture.
	*attachTo { |qtView, elementViews|
		var prevDown = qtView.mouseDownAction;
		var prevUp = qtView.mouseUpAction;
		var menuClick = false;

		qtView.mouseDownAction = { |vw, x, y, mod, btn, clicks|
			menuClick = (btn == 1) or: { mod.isAlt };
			if (menuClick) {
				MKtlElementMenu.showMenuFor(elementViews);
				true
			} {
				prevDown.value(vw, x, y, mod, btn, clicks);
			};
		};

		qtView.mouseUpAction = { |vw, x, y, mod, btn|
			if (menuClick) {
				menuClick = false;
				true
			} {
				prevUp.value(vw, x, y, mod, btn);
			};
		};
	}

	*showMenuFor { |elementViews|
		var elements = elementViews.collect(_.element).select(_.notNil);
		if (elements.isEmpty) { ^this };
		currentMenu = this.menuFor(elements);
		currentMenu.front;
		^this
	}

	// groupType groups share one underlying view across several
	// MKtlElementViews, so show a submenu per element rather than
	// letting all but one become unreachable.
	*menuFor { |elements|
		if (elements.size == 1) {
			^Menu(*this.actionsFor(elements.first))
			.title_(elements.first.name.asString)
		};
		^Menu(*elements.collect { |el|
			Menu(*this.actionsFor(el)).title_(el.name.asString)
		}).title_("% elements".format(elements.size))
	}

	*actionsFor { |element|
		^[
			MenuAction(element.elemPathString).enabled_(false),
			MenuAction.separator,

			this.copyMenuFor(element),

			MenuAction.separator,

			MenuAction("Show actions in new document", {
				Document(
					"actions_" ++ (element.name ? \element).asString,
					element.actionsAsString
				);
			}),
			MenuAction("Post element info", { element.postElemInfo })
		]
	}

	*copyMenuFor { |element|
		var items, groups;

		items = [
			MenuAction("action_ template", {
				this.prCopy(element.actionTemplateString(\action_))
			}),
			MenuAction("addAction template", {
				this.prCopy(element.actionTemplateString(\addAction))
			}),
			MenuAction("element path", {
				this.prCopy(element.elemPathString)
			})
		];

		groups = this.ancestorGroupsOf(element);
		if (groups.notEmpty) {
			items = items.add(
				Menu(*groups.collect { |group|
					MenuAction("elAt(%)".format(group.elemPathArgString), {
						this.prCopy(group.actionTemplateString(\action_))
					})
				}).title_("group action")
			);
		};

		items = items.add(
			MenuAction(".kr snippet", { this.prCopy(element.krCodeString) })
		);

		^Menu(*items).title_("Copy")
	}

	// every ancestor group that has a usable elAt path
	// (the root elementGroup has none, so it drops out)
	*ancestorGroupsOf { |element|
		var groups = [], node = element;
		while { (node = node.parent).notNil } {
			if (node.elemPath.notEmpty) { groups = groups.add(node) };
		};
		^groups
	}

	*prCopy { |string|
		string.copyToClipboard;
		"\n// copied to clipboard:\n%\n".postf(string);
	}
}
