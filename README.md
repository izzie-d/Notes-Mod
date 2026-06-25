# Notes

notes is a small client side fabric mod for keeping minecraft plans in minecraft.

i wanted it to feel like a simple notepad you can open while playing allowing you to write down build ideas, pin a few tasks to the HUD, track materials, and close it without leaving the world or relying on a server or you being able to find the same .txt file again!

if any suggestions are wanted either make a github issue or dm me on discord at @iz.zyd

everything is saved locally in your current minecraft instance at:

```text
config/notemod/notes.json
```

that file is plain JSON, so you can save it, back it up move it to another directory or edit it manually!

## What It Does

- categories for grouping notes, like projects, smp, or resources
- standard notes for plain text
- task notes for checklists, priorities, reminders, and material tracking
- search across titles, note text, categories, priorities, and task text
- A pinned HUD view for notes you want visible while playing
- an edit ui Position button for moving the pinned HUD by dragging it or typing exact X/Y coords
- local reminders with in game messages, optional sound, and custom hour/minute/second timing
- no server, account, cloud sync, or internet connection required 


## Opening Notes

default keybinds:

```text
N - Open Notes
H - Show or hide pinned notes on the HUD
```

inside the Notes screen:

```text
Ctrl+N - New note
Ctrl+D - Duplicate note
Delete - Delete selected note
```

reminders use the little H / M / S boxes in the editor. type the delay you want, then press "set" for a one time reminder, repeat for a repeating reminder, or snooze to push the current reminder back by that amount

the edit UI position button lets you move the pinned HUD. you can drag the preview around or type exact X/Y coordinates

## Writing Tasks

the editor keeps your text editable. checklist and progress syntax stays as normal text while you are editing.

checklist examples:

```text
[ ] Gather obsidian
[ ] Find fortress
[x] Build portal // completed
```

manual progress examples:

```text
Stone: 1243 / 5000
Glass: 382 / 1000
Logs: 256 / 2000
```

tracked item examples:

```text
@item minecraft:gold_ingot 20
@item minecraft:oak_log 128
@item diamond 12
```

tracked item lines count the matching item in your player inventory. when the target is complete, the count turns gold in the HUD

while editing an item tracker, press the TAB to autocomplete item ids. for example:

```text
@item gold
```

can complete to a matching minecraft item id.

## Building

this project targets:

```text
minecraft 26.1.2
fabric Loader 0.19.3
java 25
```
though it will be updated to newer versions and maybe ported to older versions if people wish!

Build with:

```sh
./gradlew build
```

this is made using JDK 25 so try using the same :>

## Credits
- Krumbit (Lunar Dev) - encouraging me to learn java in the first place :>

- Ahmed / Batman (Lunar Admin) - giving me ideas that people would appreciate so I have a task to create :>

- Dragon (Lunar Staff) - also extra encouragement <3

- Cait (Lunar Staff) - also giving encouragement as they wish to use this :>>>

- Box - Answering any questions about Java or modding in general even late in the night :> and giving some code snippets to help 


## Honorable Mentions
- ChatGPT -  for helping me with questions but not giving code directly 

- everyone who has released public code on github which i could use as examples for certain features <3

and ofc everyone else who gave encouragement and support during the development process <3

Love you all <3

time taken - 6 hours total
