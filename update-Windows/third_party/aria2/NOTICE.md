# aria2 sidecar notices

All six release updaters (Windows amd64, Linux amd64 and macOS arm64,
client and server) embed their pinned `aria2c` executable as data and extract it
into a private directory before launching it as a separate process. No aria2
code is linked into the Go updater. The release does not distribute loose engine
executables; license texts and corresponding source remain in the source bundle.

- **aria2**, version 1.36.0, Copyright (C) 2006-2019 Tatsuhiro Tsujikawa and
  contributors. License: GNU General Public License, version 2. See
  [`COPYING`](COPYING).
- The Windows amd64, Linux amd64, and macOS arm64 embedded binaries are taken
  from Motrix commit `7012040fec926e16fe8f6c403cf038527f5c18b9` under its
  `extra/<platform>/<arch>/engine/` directories. Motrix is Copyright
  Dr_rOot and licensed under MIT; its notice is preserved in
  [`LICENSE-MOTRIX`](LICENSE-MOTRIX).
- Motrix identifies its aria2 source at <https://github.com/agalwood/aria2>.
  The archived source snapshot is commit
  `905073c6990ee9621a27cf8341ca54de7816bdc5`.
  Its Linux arm build history also references the Aria2-Pro-Core build
  scripts, licensed under GPL-3.0. The release source bundle includes the
  upstream aria2 1.36.0 source, the linked aria2 fork snapshot, and the pinned
  Motrix / Aria2-Pro-Core source snapshots so the binary provenance and build
  patches can be reviewed.

The third-party source bundle is published as
`aria2-third-party-source.tar.gz` on the same GitHub Release as the updater
assets. It contains the upstream source, linked fork, build scripts, patches,
and license texts referenced above.
The sidecar remains a separate executable process and communicates with the
updater through command-line arguments and files; no aria2 code is linked into
the Go updater.
