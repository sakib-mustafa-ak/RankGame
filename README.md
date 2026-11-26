# RankMatch

Java 17 implementation of the COM410 Assignment 2 card game. It includes a
graphical interface and a console mode. The game covers Levels 1-8 and adds a
Level 9 strategy hint and closest-prediction award.

## Run

From this directory, in Windows PowerShell:

```powershell
.\run.ps1
```

The launcher finds a Java 17+ JDK through `JAVA_HOME`, `PATH`, or common Windows
installations (including IntelliJ's bundled JDK), compiles into `obj/classes`,
and opens the graphical game window. In VS Code, use **Terminal > Run Task >
RankMatch: Run GUI**. If PowerShell blocks scripts on your machine, use
`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\run.ps1`.
With the Java extension installed, press **F5** and select **RankMatch: GUI**
to debug the windowed app.

To use the original terminal interface, run `.\run.ps1 -Console` or choose
**RankMatch: Run Console** in VS Code.

The game creates `highscores.txt` in the directory from which it is launched.
Keep that file to retain scores across runs. Enter `Computer` as a player name
for an automatic turn; the graphical interface plays it immediately. After a
round, you can view the top five scores in each match category and replay
every player's turn.

## Test

```powershell
.\run.ps1 -Test
```

Use `.\run.ps1 -BuildOnly` to compile without launching the game. VS Code also
offers **RankMatch: Build** and **RankMatch: Test** tasks.

The test runner needs no third-party libraries. Its `main` method throws on
any failed check and reports the number of successful checks.

## Windows executable

Run `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\build-exe.ps1`
with a Java 17+ JDK that includes `jpackage`. If the JDK is elsewhere, pass
`-JdkBin 'C:\path\to\jdk\bin'`. The script creates
`deliverables\RankMatch\RankMatch.exe` and `deliverables\RankMatch-Windows.zip`.
The ZIP contains the executable and its bundled Java runtime. Extract the
whole ZIP before running the executable; keep its `app` and `runtime` folders
beside it. The app saves `highscores.txt` in its working directory.

## Score interpretation

The specification's worked example uses `(guess - cards dealt) / matches`.
This implementation follows that signed formula; a low guess can give a
negative score. Scores are rounded to one decimal place before comparing
players, so equal displayed scores tie. The highest signed score wins, as
specified. The separate closest-prediction award uses the absolute difference
and does not change the official winner or high scores.

## Files

- `src/rankmatch/`: application source, including the graphical interface
- `tests/rankmatch/`: automated test source
- `run.ps1`: build, run, and test launcher
- `.vscode/tasks.json`: VS Code build, graphical run, console run, and test tasks
- `.vscode/launch.json`: VS Code Java debugger configurations
- `highscores.txt`: generated score data

The design and testing documents explain the decisions, tests, security steps,
and AI assistance. The demonstration script provides a five-minute recording
plan; the required individual voiceover should be recorded by the student.
