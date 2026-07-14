# SchedulerFiles

A Java Swing application for scheduling file copy and move operations.

## Features

- Copy or move files from a source directory to a destination directory
- Preserve source folder hierarchy or organize by year/month/file extension
- Drag-and-drop support for selecting source/destination paths
- Scheduling capabilities for automated file transfers
- File comparison options: by name, by content (byte-by-byte or SHA-256 hash)
- SHA-256 hash verification for integrity check after copy/move
- Transaction log (`_files_scheduler.log`) written to destination directory

## Download

Pre-built JAR available from [GitHub Releases](../../releases).

## Requirements

- Java Runtime Environment (JRE) 8 or later

## Build from source

```bash
# Compile
javac -d build/classes -cp "dist/lib/AbsoluteLayout.jar" src/*.java

# Run tests
javac -d build/classes -cp "build/classes;dist/lib/AbsoluteLayout.jar" tests/*.java
java -cp "build/classes;dist/lib/AbsoluteLayout.jar" TestRunner

# Package JAR
mkdir -p build/jar/lib
cp dist/lib/AbsoluteLayout.jar build/jar/lib/
cp build/classes/*.class build/jar/
cd build/jar
jar cfm ../../dist/SchedulerFiles.jar ../../build/Manifest.txt *.class lib/
```

Or simply open in NetBeans IDE → Build.

## Usage

```
java -jar dist/SchedulerFiles.jar
```

Or double-click the JAR file.

## ⚠️ Disclaimer

**THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.**

The author assumes **no responsibility** for any data loss, corruption,
or damage that may occur from using this software. Always back up your
data before performing file operations. Use at your own risk.

## License

Free software. Do what you want with it.

Copyright (C) 2020 Donato Pepe
