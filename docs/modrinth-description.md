# Chat Utils

**A practical client-side toolbox for Minecraft chat.**

Vanilla chat gives you a scroll wheel and a colour toggle. Chat Utils gives you the rest: what
arrives, what it looks like, what gets hidden, what you can find again three hours later, and what
goes out when you press a key. One settings screen, 111 options, every one of them optional.

Client-side only. Nothing to install on the server, works on any server you can already join.

---

## Nothing important gets past you

* **Mention alerts** — your own name plus any keywords you add. Highlight, bold, colour, sound.
* **Per-keyword colour and sound** — `boss | #FF5555 | entity.wither.spawn`, so you can tell *what*
  was said without reading it.
* **Exclusions** — keywords that cancel an alert, for the bot that says your name every 30 seconds.
* **Regex mode**, whole-word matching, case sensitivity, and a cooldown so one busy minute doesn't
  become forty pings.
* **Hotbar text** — the alert appears above your hotbar, where you're already looking.
* **A sound for every message**, if you want one — optionally only for real players.

## Chat you can actually read

* **Chat heads** — the sender's face next to their message, aligned across wrapped lines.
* **Timestamps** — off, on hover, or always; your own format, your own colour.
* **Friend colours** — a list of names that always stand out.
* **Chat bubbles** — messages float over the player who said them, in the world. Off by default.
* **Smooth animation** when messages slide in, and when the chat opens.
* **HUD offset** — move the chat box off whatever your other mods put there.
* **Full vanilla chat control** — scale, width, focused and unfocused height, opacity, background
  opacity, line spacing, chat delay, colours, links — all of it lives here now. Chat Utils replaces
  the game's own Chat Settings button, so there is exactly one place to look.

## Filtering

* **Word filter** with three modes: censor it, hide the line, or hide it and tell you it happened.
  Custom censor character.
* **Reveal on click** — a censored word can be opened when you actually want to know.
* **Font-aware matching** — the filter folds the mod's 16 Unicode fonts before matching, including
  text such as 𝔤𝔬𝔱𝔥𝔦𝔠.
* **Blacklist** — players whose messages never reach you.
* **Hide your own name** — for screenshots and streams.
* **Spam collapsing** — repeated lines become one line with a counter.
* **Anti-clear** — servers that flush your chat with 100 blank lines no longer can.
* **Hidden chat** with a peek key — chat is gone until you hold `Z`. Mentions can still get through.
* **Link control** — clickable links with a domain allowlist and an optional confirmation prompt.

## History that survives

* **2000 messages**, kept across world exits and disconnects — `F3 + D` doesn't take them with it.
* **A history screen** with search, on `H`.
* **Inline search** right in the chat window, no screen change.
* **Session dividers** so you can see where one login ended and the next began.
* **Export to `.txt`**, and a **Clear** button — the one vanilla never gave you.

## Typing

* **~3900 symbols** — arrows, maths, boxes, music, cards, kaomoji, the lot, in a categorised picker.
  Works in chat, on signs, in books, and at the anvil.
* **`:name` suggestions** — type `:arrow` and pick from the list.
* **16 Unicode fonts** — 𝐛𝐨𝐥𝐝, 𝑖𝑡𝑎𝑙𝑖𝑐, 𝒮𝒸𝓇𝒾𝓅𝓉, 𝔉𝔯𝔞𝔨𝔱𝔲𝔯, 𝔻𝕠𝕦𝕓𝕝𝕖, 𝚖𝚘𝚗𝚘, ｆｕｌｌｗｉｄｔｈ, Ⓒⓘⓡⓒⓛⓔⓓ,
  ѕмαℓℓ ᴄᴀᴘs and more, chosen from a button next to the chat line.
* **Macros** — `gg | Good game everyone`, expanded as you send.
* **Chat calculator** — type `=12*64` and get the answer without leaving the game.
* **Command hotkeys** — bound in the mod itself as `KEY | /home` or `KEY | some message`. No second
  screen, no numbered slots, no restart. Fonts, macros and encryption still apply.
* **Repeat last command** on a key.
* **Auto-reply** — `gg | ggwp`. Off by default, and fenced in on four sides: it never answers you,
  never answers its own words coming back, honours a per-rule cooldown, and stops dead at 4 replies a
  minute no matter what you set.

## Twitch and YouTube, in game

* **Twitch chat** — type a channel name. That's the whole setup: no token, no account, no OAuth.
* **YouTube live chat** — a video ID and a Data API key.
* Colour-coded tags, a rate limit so a big stream doesn't drown your chat, and stream messages can
  trigger your mention alerts too.

## Translation

Point it at an HTTPS LibreTranslate-compatible endpoint, pick source and target languages, and
translate the last message with a key or `/chatutils translate`. Plain HTTP is allowed only for a
loopback endpoint used for local self-hosting.

## Privacy

* **No chat reports** — messages go out unsigned. Warns you when a server actually requires signing.
* **End-to-end encryption** — AES-GCM with a PBKDF2-derived key. Share a passphrase with your
  friends and the server sees ciphertext. If a message can't be encrypted it is not sent — it never
  silently falls back to plaintext.

---

## Commands

| Command | What it does |
| --- | --- |
| `/chatutils` | Opens the settings |
| `/chatutils help` | Lists everything below, in game |
| `/chatutils hidechat` | Hide/show the chat |
| `/chatutils bubbles` | Chat bubbles on/off |
| `/chatutils heads` | Chat heads on/off |
| `/chatutils filter` | Word filter on/off |
| `/chatutils mentions` | Mention alerts on/off |
| `/chatutils autoreply` | Auto-reply on/off |
| `/chatutils encrypt` | Encryption on/off |
| `/chatutils streams` | Twitch/YouTube chat on/off |
| `/chatutils history` | Opens the history screen |
| `/chatutils search <text>` | Opens the history already searched |
| `/chatutils clear` | Empties the history |
| `/chatutils font [name]` | Shows or sets the message font |
| `/chatutils translate [text]` | Translates the text, or the last message |
| `/chatutils reveal <id>` | Shows what a censored word really said |
| `/hidechat` | Short form |
| `/chatsearch [text]` | Short form |

These commands are handled on the client and are not sent as server commands. Server rules still
apply to messages produced by client-side features.

## Keys

| Key | Action |
| --- | --- |
| `Z` (hold) | Peek at hidden chat |
| `H` | Open chat history |
| unbound | Toggle hidden chat |
| unbound | Translate last message |
| unbound | Repeat last command |
| your own | Any number of `KEY \| /command` bindings, set inside the mod |

---

## Install

* Minecraft **26.2**, **Fabric**
* [Fabric API](https://modrinth.com/mod/fabric-api) — required
* [YetAnotherConfigLib](https://modrinth.com/mod/yacl) — required
* [Mod Menu](https://modrinth.com/mod/modmenu) — optional, adds the settings button to the mod list

Client-side only. There is no server component, but servers may reject unsigned or encrypted chat
and may restrict client-side modifications in their rules.

Available in **English** and **Russian**.
