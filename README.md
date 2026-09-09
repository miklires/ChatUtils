<div align="center">

# Chat Utils

Client-side chat tools for Minecraft 26.2.

![Minecraft](https://img.shields.io/badge/Minecraft-26.2-62B47A)
![Fabric](https://img.shields.io/badge/Loader-Fabric-DBD0B4)
![Java](https://img.shields.io/badge/Java-25-E76F00)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![GitHub release](https://img.shields.io/github/v/release/miklires/ChatUtils?display_name=tag)](https://github.com/miklires/ChatUtils/releases)

</div>

Chat Utils combines mention alerts, filtering, searchable history, typing tools, appearance controls,
stream chat, translation, and optional privacy features in one settings screen. It runs entirely on
the client; a server installation is not required.

## Highlights

- Mention alerts with keyword rules, exclusions, highlighting, sounds, and cooldowns.
- Word filtering, player blacklist, friend highlighting, link allowlists, spam compaction, and
  anti-clear handling.
- Searchable history of up to 10,000 messages with session dividers and text export.
- Chat heads, timestamps, animations, chat bubbles, HUD placement, and vanilla chat controls.
- Macros, command keybinds, calculator expressions, Unicode symbols, and message fonts.
- Read-only Twitch and YouTube live chat integrations with bounded queues and response sizes.
- On-demand translation through an HTTPS LibreTranslate-compatible endpoint. Plain HTTP is accepted
  only for a local endpoint on `localhost`, `127.0.0.0/8`, or `::1`.
- Optional unsigned chat and AES-GCM encrypted messages for players sharing the same passphrase.

English and Russian translations are included. Minecraft selects the language from the active game
language and falls back to English when a translation is unavailable.

## Install

1. Install Minecraft 26.2 with Fabric Loader 0.19.3 or newer.
2. Add [Fabric API](https://modrinth.com/mod/fabric-api) to the `mods` directory.
3. Add the Chat Utils JAR. YACL is bundled in the release JAR.
4. Optionally install [Mod Menu](https://modrinth.com/mod/modmenu) for direct access to settings.

Open settings with `/chatutils`, or through Mod Menu.

## Commands and default keys

| Command | Action |
| --- | --- |
| `/chatutils` | Open settings |
| `/chatutils help` | Show command help |
| `/chatutils history` | Open searchable history |
| `/chatutils search <text>` | Search history |
| `/chatutils clear` | Clear stored history |
| `/chatutils translate [text]` | Translate text or the latest message |
| `/chatutils font [name]` | Show or select the outgoing message font |
| `/chatutils hidechat` | Toggle hidden chat |
| `/chatutils bubbles` | Toggle chat bubbles |
| `/chatutils heads` | Toggle chat heads |
| `/chatutils filter` | Toggle the word filter |
| `/chatutils mentions` | Toggle mention alerts |
| `/chatutils autoreply` | Toggle automatic replies |
| `/chatutils encrypt` | Toggle encrypted messages |
| `/chatutils streams` | Toggle stream chat |

`Z` temporarily reveals hidden chat while held. `H` opens chat history. All bindings can be changed
in Minecraft's Controls screen.

## Privacy and server compatibility

- Translation text is sent to the configured translation service.
- YouTube integration stores its API key in `config/chatutils.json` as plain text. Do not share that
  file or upload it with bug reports.
- Encrypted chat is visible to the server as ciphertext, but metadata and message timing remain
  visible. Every participant needs Chat Utils and the same passphrase.
- Unsigned or encrypted messages may be rejected by a server or prohibited by its rules. The mod
  does not bypass server enforcement.

## Build and test

```powershell
.\gradlew.bat clean build
```

The test suite covers calculator parsing, domain matching, configuration normalization, translation
endpoint policy, Twitch channel validation, and encrypted-message integrity and limits.

## License

[MIT](LICENSE)
