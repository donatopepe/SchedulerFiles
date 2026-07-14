# SchedulerFiles

A Java Swing application for scheduling file copy and move operations.

## Features

- Copy or move files from a source directory to a destination directory
- Preserve source folder hierarchy or organize by year/month/file extension
- Drag-and-drop support for selecting source/destination paths
- Scheduling capabilities for automated file transfers
- File comparison options (by content or by name)

## Requirements

- Java Runtime Environment (JRE) 7 or later

## Build

This is a NetBeans project. Open it in NetBeans IDE and build, or run:

```
javac -d build/classes src/*.java src/icon/*.java
```

## Usage

Run the compiled JAR:

```
java -jar dist/SchedulerFiles.jar
```

Or from NetBeans: right-click project → Run.

## License

GNU General Public License v3.0 or later.

Copyright (C) 2020 Donato Pepe
