/*
sclang has no clipboard API - nothing in SCClassLibrary, nothing in ScIDE -
so this shells out to the platform's clipboard tool.

The string is routed through a temp file rather than echoed into the command,
which avoids escaping braces, quotes and newlines in generated code entirely.

Part of the ModalityContextMenu extension.

"MKtl(\\grid).elAt(\\knobModule, 3, 0)".copyToClipboard;
*/

+ String {

	copyToClipboard {
		var path = Platform.defaultTempDir +/+ "mktlClipboard.txt";
		var file = File(path, "w");
		var cmd;

		if (file.isOpen.not) {
			warn("String:copyToClipboard - could not open temp file at %."
				.format(path.quote));
			^this
		};
		file.write(this);
		file.close;

		cmd = Platform.case(
			\osx,		{ "cat % | pbcopy" },
			\linux,		{ "cat % | (xclip -selection clipboard 2>/dev/null || xsel --clipboard --input)" },
			\windows,	{ "clip < %" }
		);

		if (cmd.isNil) {
			warn("String:copyToClipboard - no clipboard command known for platform '%'."
				.format(thisProcess.platform.name));
			^this
		};

		cmd.format(path.quote).unixCmd(postOutput: false);
		^this
	}
}
