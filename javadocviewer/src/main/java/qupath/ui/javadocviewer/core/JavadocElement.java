package qupath.ui.javadocviewer.core;

import java.net.URI;

/**
 * An element (function, class, enum...) of a Javadoc.
 *
 * @param uri the URI of the Javadoc owning this element
 * @param name the name of the element (e.g. the function name)
 * @param category the category of the element (e.g. "function" or "class")
 */
public record JavadocElement(URI uri, String name, String category) {}
