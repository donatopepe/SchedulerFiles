# SchedulerFiles

Java Swing and command-line utility for safe file copy/move operations.

## Features

- Copy or move files between directories
- Preserve source hierarchy or organize by year/month/extension
- Drag-and-drop and directory browser in Swing UI
- Compare by name, bytes, or SHA-256
- Verify SHA-256 after transfer (enabled by default)
- Skip symbolic links, junction-like links, and directory cycles
- Write `_files_scheduler.log` in destination
- Cancel running GUI operations
- Optional two-destination copy (software RAID-1 style replication in CLI)

## Requirements

- Runtime: JRE 8+
- Integrity: SHA-256 is enabled by default; no faster algorithm with comparable collision resistance is available in the standard Java 8 runtime.
- Build: JDK 8+; project targets Java 8 bytecode
- Release JAR: compiled with `javac --release 8` and embeds `AbsoluteLayout.jar`
- Bundled dependency: `dist/lib/AbsoluteLayout.jar`

## Run

### Swing UI

```bash
java -jar dist/SchedulerFiles.jar

The release JAR is self-contained and requires only Java 8+; no external
classpath or `dist/lib` folder is needed.
```

### CLI

```bash
java -cp "dist/SchedulerFiles.jar;dist/lib/AbsoluteLayout.jar" Cli --help
java -cp "dist/SchedulerFiles.jar;dist/lib/AbsoluteLayout.jar" Cli \
  --source /path/to/source --dest /path/to/destination --copy --original-tree

# Replicate every file to two destinations (copy mode only)
java -cp "dist/SchedulerFiles.jar;dist/lib/AbsoluteLayout.jar" Cli \
  --source /path/to/source --dest /path/to/destination-a --dest2 /path/to/destination-b --copy
```

On Linux/macOS, replace classpath separator `;` with `:`.

Main CLI options:

```text
--source <dir>       Source directory
--dest <dir>         Primary destination directory
--dest2 <dir>        Optional second destination (RAID-1 copy mode)
--copy               Copy files (default)
--move               Move files
--original-tree      Preserve hierarchy (default)
--scheduled-tree     Organize by year/month/extension
--compare-name       Skip matching names
--compare-content    Skip matching content
--verify-hash        Verify SHA-256
--version            Print version
--help               Print help
```

## Build and test

### Windows Command Prompt

```bat
rmdir /s /q build\classes 2>nul
mkdir build\classes
javac --release 8 -encoding UTF-8 -cp "dist\lib\AbsoluteLayout.jar" -d build\classes src\*.java tests\*.java
copy src\about.html build\classes\about.html
java -Djava.awt.headless=true -cp "build\classes;dist\lib\AbsoluteLayout.jar" TestRunner
```

### PowerShell

```powershell
Remove-Item build/classes -Recurse -Force -ErrorAction SilentlyContinue
New-Item build/classes -ItemType Directory -Force | Out-Null
javac --release 8 -encoding UTF-8 -cp 'dist/lib/AbsoluteLayout.jar' -d build/classes src/*.java tests/*.java
Copy-Item src/about.html build/classes/about.html
java -Djava.awt.headless=true -cp 'build/classes;dist/lib/AbsoluteLayout.jar' TestRunner
```

### Linux/macOS

```bash
rm -rf build/classes
mkdir -p build/classes
javac --release 8 -encoding UTF-8 \
  -cp 'dist/lib/AbsoluteLayout.jar' \
  -d build/classes src/*.java tests/*.java
cp src/about.html build/classes/about.html
java -Djava.awt.headless=true \
  -cp 'build/classes:dist/lib/AbsoluteLayout.jar' TestRunner
```

NetBeans users can open project and run **Build Project**. CI workflow in `.github/workflows/build.yml` compiles, tests, packages artifacts, and creates releases for `v*` tags.

## Safety

**Software provided "as is", without warranty.** Back up important data before move operations. Author assumes no responsibility for data loss, corruption, or damage.

## License

Free software. Do what you want with it.

Copyright (C) 2020 Donato Pepe
