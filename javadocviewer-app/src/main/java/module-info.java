/**
 * This module contains an application to start the Javadoc viewer.
 */
module qupath.ui.javadocviewer.app {
    requires javafx.graphics;
    requires qupath.ui.javadocviewer.main;
    requires org.slf4j;

    opens qupath.ui.javadocviewer.app to javafx.graphics;
}