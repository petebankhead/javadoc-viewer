/**
 * This module provides a Javadoc viewer.
 * It supports multiple Javadoc sources and provides a
 * search feature across all Javadocs.
 */
module qupath.ui.javadocviewer.main {
    exports qupath.ui.javadocviewer.main.gui.viewer;
    requires org.slf4j;
    requires java.net.http;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
}