# Jev for WhatsApp (Android, English)

A reply assistant for one-to-one WhatsApp chats in English. Tap Jev's bubble in a
chat: it reads the conversation on screen, works out what the other person wants
and how things stand between you, asks what you want to achieve, then drafts two
replies, checks them and scores them. You pick one, Jev writes it into WhatsApp's
message box, and you decide whether to send it.

**Jev never sends a message.** It never touches the send button and never presses
enter. Sending is always your tap.

This is a separate app from the Chinese Jev Chat Assistant in the repository root
(QQ, X and Feishu). It has its own package (`com.jev.overseas`), its own engine and
its own Gradle build in this directory, and it shares no code with the root app.

Status: **v0.1.0, early release.** Tested on one phone (Android 16) with WhatsApp
2.26.38.73. See [Limitations](#limitations).

## What it does

1. **Reads the chat when you ask.** Only when you tap the bubble, and only the
   WhatsApp chat on screen. It scrolls back to collect up to 24 recent messages
   (it can read further back on request). Group chats are recognised and refused.
2. **Asks who you are talking to, once per chat.** Five scenes: Work, Romance,
   Friends, Family and General, each with relationship types (for example Work ›
   Manager or senior, Romance › Talking stage, Family › Parent or elder). Jev
   remembers the choice for that chat under a salted hash, never the name.
3. **Analyses the situation.** The Jev model answers a fixed set of questions about
   the latest messages (what they are asking for, pressure, boundaries, friction,
   tone) and places the exchange in a scene matrix: who is ahead, what the next step
   should be. Your options (stances) come from that, for example "Give an update",
   "It will be late", "Ask for more time".
4. **Builds a goal.** The stance you pick, any detail you type (for example a new
   date) and fine-tune switches ("Don't apologise", "No new promises", "Don't
   explain why") become a goal
   with required items and things to avoid.
5. **Drafts and checks two replies.** A drafting model writes two candidates. Jev
   then checks each one: does it complete the goal, does it promise or decide
   something you did not ask for, does it admit fault, contradict something you
   said earlier, or miss a required item. Each reply gets a score out of 5 from goal
   completion and delivery. Only a reply that passes every check can be marked
   "Top pick"; anything doubtful asks you to look before using it.
6. **Fills the message box.** "Fill" writes the chosen reply into WhatsApp's input
   field, after checking that the same chat is still open. You edit and send it
   yourself.

## Privacy in short

- Jev reads only the WhatsApp chat on screen, only when you tap the bubble. It
  takes no screenshots and reads no other app.
- The messages it read and the scene you chose are sent to
  [OpenRouter](https://openrouter.ai) and from there to the model providers
  (TypeSafe for the Jev model, DeepSeek for drafting). This includes the other
  person's messages, so think about whether that is acceptable for a given chat.
- Nothing is sent to the authors of this project. There is no analytics and no
  server of ours.
- On the phone Jev keeps your OpenRouter key (encrypted with an Android Keystore
  key), your settings, the scene per chat (salted hash) and a diagnostic log with
  counts, scores and timings but no chat text. Backups are disabled.

Full details: [PRIVACY.md](PRIVACY.md).

## Requirements

- Android 11 or later (minSdk 30). Tested on Android 16.
- WhatsApp for Android. Tested on 2.26.38.73; other versions may read incorrectly
  (see Limitations).
- Your own [OpenRouter API key](https://openrouter.ai/keys) with a little credit.
  One round usually costs well under one US cent.

## Install

There is no prebuilt APK in this release yet. Build it from source:

```bash
cd overseas
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk   # platform 35 and build-tools installed
./gradlew :assistant:assembleDebug
adb install -r assistant/build/outputs/apk/debug/assistant-debug.apk
```

Then open the Jev app and follow its three setup steps:

1. **Add your OpenRouter key** and tap "Test connection".
2. **Turn on the accessibility service** "Jev reply assistant". On Android 13 and
   later, an app installed from a file must be allowed first: Settings › Apps ›
   Jev › ⋮ › Allow restricted settings.
3. **Open a one-to-one WhatsApp chat and tap the bubble.**

## Using it

- **Bubble**: shown only while WhatsApp is open. Tap it in a chat to start a round.
- **Panel**: shows what Jev read, its analysis and your options. Pick a stance,
  add a detail if asked, then "Draft replies".
- **Reply cards**: the score ring shows the score out of 5. "Why 4.x" opens the
  breakdown: the checks, goal completion, delivery and how the score is calculated.
- **Goal & analysis**: the goal, the fine-tune switches, the analysis and "Report a
  problem".
- **Minimise** keeps the round; **Close** clears it. A round expires after 15
  minutes.

## Limitations

- **One device and one WhatsApp version tested.** Reading relies on WhatsApp's
  internal view IDs, which can change in any update. A WhatsApp update may stop
  Jev from reading, or make it read wrongly, until the adapter is updated. The
  recorded screens in `fixtures/screendumps/` show what the adapter expects.
- English chats only. If the other person writes mostly in a non-Latin script, Jev
  says so and does not analyse.
- One-to-one chats only. A group whose visible screen shows only your own messages
  looks one-to-one until Jev reads an earlier screen with someone else's message.
- Photos, videos, stickers and documents are not opened. When their latest message
  is only a file, Jev answers from the messages around it ("Thanks, I've got it").
  A voice note or a deleted message as the latest message cannot be analysed; you
  can write your own goal instead.
- The analysis thresholds were tuned on a small authored pilot corpus. Agreement
  figures in [docs/TESTING.md](docs/TESTING.md) are against the corpus author's own
  labels, not an independent validation.
- Two contacts with the same display name share a remembered scene.

## Disclaimer

Jev is an independent open-source project. It is not affiliated with, endorsed by
or sponsored by WhatsApp LLC or Meta Platforms, Inc. "WhatsApp" is a trademark of
WhatsApp LLC and is used here only to say which app Jev works with.

Jev uses Android's accessibility service to read what is on your own screen. Use
it only on your own device and your own chats, and follow the laws and the terms
of service that apply to you. The suggestions come from language models and can be
wrong; you are responsible for what you send.

## Project layout

| Path | What it is |
|---|---|
| `core/` | Pure Kotlin (no Android): the engine (analysis, goals, drafting, checks, scoring), scene matrices, the session state machine, the WhatsApp screen adapter, the model gateway and diagnostics. All unit tests live here. |
| `a11y/` | Android library: walks the accessibility node tree and measures whether the message list has settled. Performs no actions. |
| `assistant/` | The app: accessibility service, bubble, panel, WhatsApp reader, Fill, settings and the encrypted key store. |
| `probe/` | A read-only development app that records WhatsApp screens as JSON "ScreenDumps" (no network permission). Used to update the adapter for new WhatsApp versions. |
| `fixtures/` | Recorded screens (text masked where it is not test-chat text) and the authored test corpus. |
| `tools/` | `probe/read_dump.py` prints the messages in a dump; `or_client.py` is a small OpenRouter client for experiments. |
| `docs/` | [Testing](docs/TESTING.md) and [diagnostics](docs/DIAGNOSTICS.md). |

How the pieces fit together: [ARCHITECTURE.md](ARCHITECTURE.md).

## Development

```bash
cd overseas
./gradlew test             # offline unit tests, no key and no network
./gradlew assembleDebug    # builds assistant and probe
```

Live evaluations against the real models cost a little money and need an
OpenRouter key in `OPENROUTER_API_KEY`; see [docs/TESTING.md](docs/TESTING.md).

Ground rules for contributions:

- Never add any code path that sends a message, presses the send button or
  triggers the keyboard's enter action.
- Keys never go into the repository, logs or diagnostics.
- Diagnostics never contain chat text, names, replies or the goal the user typed.
- `core/src/test/resources/approved/` holds the approved scene matrices and apology
  table, typed by hand. They are the specification: change them only for an agreed
  behaviour change, never to make a test pass.
- Report bugs with the "Bug report (WhatsApp assistant)" issue template and the
  diagnostic report from the app.

## License

MIT, as the rest of this repository. See [LICENSE](../LICENSE) and
[NOTICE](../NOTICE).
