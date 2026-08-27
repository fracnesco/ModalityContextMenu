# ModalityContextMenu

A context menu for the element views of a
[Modality](https://github.com/ModalityTeam/Modality-toolkit) `MKtlGUI`,
opened by right-click or option-click (alt-click).

```supercollider
MKtl('grid', "intech-grid-nesso").gui;   // now right-click (or option-click) any knob, fader, button or pad
```

The menu tells you how to address that element in code, copies
ready-to-paste action templates to the clipboard, shows every action currently
registered on it, and posts its full description.

## Why

The GUI mirrors your hardware, but there is no path from a widget back to the
code you need to write for it. Working out that the knob you just wiggled is
`MKtl(\grid).elAt(\knobModule, 3, 0)` otherwise means reading the `.desc.scd`
file, or calling `postElements` and counting. And once actions *are* attached,
there is no way to see what is registered short of remembering what you
evaluated.

## Requirements

- SuperCollider **3.13+** (for `Menu` / `MenuAction`)
- The [Modality Toolkit](https://github.com/ModalityTeam/Modality-toolkit) quark

## Install

```supercollider
Quarks.install("https://github.com/fracnesco/ModalityContextMenu");
```

Then **recompile the class library** (`Language > Recompile Class Library`, or
<kbd>Cmd</kbd>+<kbd>Shift</kbd>+<kbd>L</kbd>) — a quark's classes are only picked
up at compile time.

To check or remove:

```supercollider
Quarks.installed.detect { |q| q.name == "ModalityContextMenu" };
Quarks.uninstall("ModalityContextMenu");
```

## The menu

| Entry | |
|---|---|
| *(header)* | the element's `elAt` path, e.g. `MKtl('grid').elAt(\knobModule, 3, 0)` |
| Copy ▸ `action_` template | a bare skeleton, ready to fill in |
| Copy ▸ `addAction` template | the same, but appending rather than replacing |
| Copy ▸ element path | just the path |
| Copy ▸ group action ▸ … | one entry per ancestor group, in the `{ \|el, grp\| }` form |
| Copy ▸ `.kr` snippet | for use in a SynthDef |
| Show actions in new document | every action that fires for this element, as re-evaluable code |
| Post element info | name, type, ioType, spec, value, tags, and the protocol detail from `elemDesc` |

A menu click - right-click or option-click - is swallowed before the widget's
built-in Qt handling runs, so it never also moves the widget or fires its
action. On macOS Qt delivers ctrl+click as a right-click, so trackpads work too.

Where a `groupType` group makes several elements share one underlying view — a
pad's `\on` / `\off` / `\touch`, say — the menu shows one submenu per element
rather than letting all but one become unreachable.

## Also usable without the GUI

The code-string helpers are added to `MAbstractElement`, so they work on any
element or group straight from the post window:

```supercollider
k = MKtl('grid', "intech-grid-nesso");

k.elAt(\knobModule, 3, 0).elemPath;           // -> [ knobModule, 3, 0 ]
k.elAt(\knobModule, 3, 0).elemPathString;     // -> "MKtl('grid').elAt(\knobModule, 3, 0)"
k.elAt(\knobModule, 3, 0).elemPathArgString;  // -> "\knobModule, 3, 0"
k.elAt(\knobModule, 3, 0).actionTemplateString;
k.elAt(\knobModule, 3, 0).actionTemplateString(\addAction);
k.elAt(\knobModule, 3, 0).krCodeString;
k.elAt(\knobModule, 3, 0).actionsAsString;
k.elAt(\knobModule, 3, 0).postElemInfo;

"anything".copyToClipboard;   // added to String
```

`elemPath` walks the `parent` chain and picks, at each level, the group key when
it is a real name and the integer index when `MKtlDesc:makeElemKeys` only filled
in a positional `(i+1).asSymbol`. That is what produces `elAt(\knobModule, 3, 0)`
rather than `elAt(\knobModule, '4', '1')`. It round-trips:

```supercollider
k.elementGroup.flat.every { |el| k.elAt(*el.elemPath) === el };   // -> true
```

That round-trip is verified against every element of these descs: `intech-grid-nesso`,
`intech-grid-nesso2`, `korg-nanokontrol`, `korg-nanokontrol2`, `novation-launchpad`,
`akai-lpd8`, `akai-mpkmini`, `akai-apcmini`, `ableton-push-2`, `arturia-minilab`,
`behringer-bcr2000` — around 1300 elements in total.

> `MAbstractElement:keys` looks like it does the same job, but it does not: it
> recurses into `MKtlElementGroup:keys`, which returns a group's *child* keys
> rather than its ancestor path. This quark routes around it and does not change it.

## How it hooks in, and what that costs

Nothing in Modality-toolkit is modified. Instead, two of its methods are
overwritten from this quark — sclang supports this explicitly, and
`MethodOverride.printAll` will list exactly these two:

- **`MKtlGUI:*new`** — one line reproduced verbatim from upstream, plus a call to
  `addContextMenus`. Hooking `*new` rather than `MKtl:gui` covers both entry
  points (`k.gui` and `MKtlGUI(mktl: k)`) and avoids
  extension-overwrites-extension, which resolves by compile order.
- **`MKtlElementView:snapback_`** — reimplemented as flag-only. Upstream it
  installs and removes `view.mouseDownAction` directly, which would blow away the
  menu wrapper every time `t` or `m` is pressed over a widget. The click
  behaviour moves into `MKtlGUI:addContextMenus` instead, where it also skips
  button 1, so right-clicking a snapback button no longer fires its action.

Because Modality is untouched, an upstream update can silently desync this. The
things to re-check after one are those two method bodies, plus these staying
public: `MKtlGUI:views`, `MKtlElementView:element/view/snapback/snapbackValue`,
`MKtlElementGroup:elemKeyOf/elemIndexOf/at`, and `addGroupsAsParent` being true
during `MKtl:makeElements`.

## Platform support

sclang has no clipboard API, so `copyToClipboard` shells out:

| | |
|---|---|
| macOS | `pbcopy` — tested |
| Linux | `xclip`, falling back to `xsel` — untested, needs one of them installed |
| Windows | `clip` — untested |

Everything else is platform-independent. Reports welcome.

## License

GPL-2.0, matching the Modality toolkit family. See [LICENSE](LICENSE).
