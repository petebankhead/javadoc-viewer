# Javadoc Viewer

A simple JavaFX Javadoc viewer.

## Installing

The repository contains two subprojects:

* One subproject (`javadocviewer`) containing the UI implementation of the Javadoc viewer.
* One subproject (`javadocviewer-app`) to start the UI implementation as a standalone application. It is mainly used for development.

To use the javadoc viewer:

```groovy
// build.gradle

dependencies {
  def javadocViewerVersion = "0.1.0-SNAPSHOT"

  implementation "io.github.qupath:javadocviewer:${javadocViewerVersion}"
}
```

If you don't use Java modules in your application, you also have to import the `javafx.controls` and `javafx.fxml` modules:

```groovy
// build.gradle

javafx {
    version = ...
    modules = [ 'javafx.controls', 'javafx.fxml' ]
}
```

Then, take a look at the `JavadocViewerApp` class of `javadocviewer-app` to see
an example on how to use the javadoc viewer.

## Building

You can build every module of the javadoc viewer from source with:

```bash
./gradlew clean build
```

The outputs will be under each subproject's `build/libs`.