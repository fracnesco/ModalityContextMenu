# ModalityContextMenu

A right-click context menu for the element views in a
[Modality](https://github.com/ModalityTeam/Modality-toolkit) `MKtlGUI`.

```supercollider
MKtl('grid', "intech-grid-nesso").gui;   // right-click any knob, fader, button or pad
```

Right-clicking a widget tells you how to address that element in code, copies
ready-to-paste action templates to the clipboard, shows every action currently
registered on it, and posts its full description.

## Why

The GUI mirrors the hardware, but there is no path from a widget back to the
code you need to write for it. Working out that the knob you just wiggled is
`MKtl(\grid).elAt(\knobModule, 3, 0)` otherwise means reading the `.desc.scd`
file or calling `postElements` and counting. And once actions *are* attached,
there is no way to see what is registered short of remembering what you
evaluated.

## Menu

| Entry | |
|---|---|
| *(header)* | the element's `elAt` path, e.g. `MKtl('grid').elAt(\knobModule, 3, 0)` |
| Copy ▸ `action_` template | a bare skeleton, ready to fill in |
| Copy ▸ `addAction` template | same, but appends instead of replacing |
| Copy ▸ element path | just the path |
| Copy ▸ group action ▸ … | one entry per ancestor group, `{ \|el, grp\| }` form |
| Copy ▸ `.kr` snippet | for use in a SynthDef |
| Show actions in new document | every action that fires for this element, as re-evaluable code |
| Post element info | name, type, ioType, spec, value, tags, and the protocol detail from `elemDesc` |

Right-click is swallowed, so it never also moves the widget or fires its action.
On macOS Qt delivers ctrl+click as a right-click, so trackpads work too.

## Install

The repo lives in SuperCollider's `downloaded-quarks` folder and is registered
as a quark, so its path is in `sclang_conf.yaml`:

```supercollider
Quarks.install("/Users/you/Library/Application Support/SuperCollider/downloaded-quarks/ModalityContextMenu");
// then recompile the class library (Language > Recompile Class Library, or cmd-shift-L)
```

To check or undo:

```supercollider
Quarks.installed.detect { |q| q.name == "ModalityContextMenu" };
Quarks.uninstall("ModalityContextMenu");   // just removes it from sclang_conf.yaml
```

## Also usable without the GUI

The code-string helpers are added to `MAbstractElement`, so they work on any
element or group from the post window:

```supercollider
k = MKtl('grid', "intech-grid-nesso");

k.elAt(\knobModule, 3, 0).elemPath;          // -> [ knobModule, 3, 0 ]
k.elAt(\knobModule, 3, 0).elemPathString;    // -> "MKtl('grid').elAt(\knobModule, 3, 0)"
k.elAt(\knobModule, 3, 0).actionTemplateString;
k.elAt(\knobModule, 3, 0).actionsAsString;
k.elAt(\knobModule, 3, 0).postElemInfo;

"anything".copyToClipboard;
```

`elemPath` walks the `parent` chain and picks, at each level, the group key when
it is a real name and the integer index when `MKtlDesc:makeElemKeys` only filled
in a positional `(i+1).asSymbol`. That is what produces `elAt(\knobModule, 3, 0)`
rather than `elAt(\knobModule, '4', '1')`.

(`MAbstractElement:keys` looks like it does this, but it does not — it recurses
into `MKtlElementGroup:keys`, which returns a group's *child* keys rather than
its ancestor path. This quark does not touch it.)

## Coupling to Modality

Nothing in Modality-toolkit is modified, so an upstream update can silently
desync this. `MethodOverride.printAll` should list exactly two entries from this
quark:

- `MKtlGUI:*new` — one line reproduced verbatim from upstream.
- `MKtlElementView:snapback_` — reimplemented as flag-only, so that pressing
  `t` / `m` over a widget no longer re-assigns `mouseDownAction` and blows away
  the menu wrapper.

It also relies on these staying public: `MKtlGUI:views`,
`MKtlElementView:element/view/snapback/snapbackValue`,
`MKtlElementGroup:elemKeyOf/elemIndexOf/at`, and `addGroupsAsParent` being true
during `MKtl:makeElements`.

## Requires

SuperCollider 3.13+ (for `Menu` / `MenuAction`) and the Modality toolkit.
