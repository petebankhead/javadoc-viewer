package qupath.ui.javadocviewer.core;

import java.net.URI;
import java.util.List;

/**
 * A collection of utility functions.
 */
class Utils {

    private static final List<String> WEBSITE_SCHEMES = List.of("http", "https");

    private Utils() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * Indicate whether the provided URI links to a website.
     *
     * @param uri the URI to check
     * @return whether the provided URI links to a website
     */
    public static boolean doesUrilinkToWebsite(URI uri) {
        return uri.getScheme() != null && WEBSITE_SCHEMES.contains(uri.getScheme());
    }
}
