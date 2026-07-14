# SchedulerFiles

Java Swing and command-line utility for safe file copy/move operations.

## Features

- Copy or move files between directories
- Preserve source hierarchy or organize by year/month/extension
- Drag-and-drop and directory browser in Swing UI
- Compare by name, bytes, or SHA-256
- Verify SHA-256 after transfer
- Skip symbolic links, junction-like links, and directory cycles
- Write `_files_scheduler.log` in destination
- Cancel running GUI operations

## Requirements

- Runtime: JRE 8+
- Build: JDK 8+ (`--release 8` requires JDK 9+)
- Bundled dependency: `dist/lib/AbsoluteLayout.jar`

## Run

### Swing UI

```bash
java -jar dist/SchedulerFiles.jar
```

### CLI

```bash
java -cp "dist/SchedulerFiles.jar;dist/lib/AbsoluteLayout.jar" Cli --help
java -cp "dist/SchedulerFiles.jar;dist/lib/AbsoluteLayout.jar" Cli \
  --source /path/to/source --dest /path/to/destination --copy --original-tree
```

On Linux/macOS, replace classpath separator `;` with `:`.

Main CLI options:

```text
--source <dir>       Source directory
--dest <dir>         Destination directory
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
