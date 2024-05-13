package qupath.ui.javadocviewer.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/**
 * Utility class to search for Javadocs.
 */
public class JavadocsFinder {

    private static final Logger logger = LoggerFactory.getLogger(JavadocsFinder.class);
    private static final String JAVADOC_INDEX_FILE = "index.html";
    private static final List<String> ARCHIVE_EXTENSIONS = List.of(".jar", ".zip");

    private JavadocsFinder() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * Asynchronously search for Javadocs in the specified URIs and around the application executable.
     *
     * @param urisToSearch  URIs to search for Javadocs. It can be a directory, an HTTP link,
     *                      a link to a Javadoc...
     * @return a CompletableFuture with the list of Javadocs found
     */
    public static CompletableFuture<List<Javadoc>> findJavadocs(URI... urisToSearch) {
        return CompletableFuture.supplyAsync(() ->
                Stream.concat(
                        Arrays.stream(urisToSearch).map(JavadocsFinder::findJavadocUris).flatMap(Collection::stream),
                        findJavadocUrisAroundExecutable().stream()
                )
                .map(Javadoc::create)
                .map(CompletableFuture::join)
                .flatMap(Optional::stream)
                .distinct()
                .toList()
        );
    }

    private static List<URI> findJavadocUrisAroundExecutable() {
        URI codeUri;
        try {
            codeUri = JavadocsFinder.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (URISyntaxException e) {
            logger.debug("Could not convert URI", e);
            return List.of();
        }

        Path codePath;
        try {
            codePath = Paths.get(codeUri);
        } catch (Exception e) {
            logger.debug(String.format("Could not convert URI %s to path", codeUri), e);
            return List.of();
        }

        // If we have a jar file, we need to check the location...
        if (codePath.getFileName().toString().toLowerCase().endsWith(".jar")) {
            if (codePath.getParent().toString().endsWith("/build/libs")) {
                // We are probably using gradlew run
                // We can go up several directories to the root project, and then search inside for javadocs
                return findJavadocUris(codePath.getParent().resolve("../../../").normalize(), 4);
            } else {
                // We are probably within a pre-built package
                // javadoc jars should be either in the same directory or a subdirectory
                return findJavadocUris(codePath.getParent(), 2);
            }
        } else {
            // If we have a binary directory, we may well be launching from an IDE
            // We can go up several directories to the root project, and then search inside for javadocs
            return findJavadocUris(codePath.resolve("../../../").normalize(), 4);
        }
    }

    private static List<URI> findJavadocUris(URI uri) {
        if (uri.getScheme() != null && List.of("http", "https").contains(uri.getScheme())) {
            return List.of(uri);
        } else {
            try {
                return findJavadocUris(Paths.get(uri), 2);
            } catch (Exception e) {
                logger.debug(String.format("Could not convert URI %s to path", uri), e);
                return List.of();
            }
        }
    }

    private static List<URI> findJavadocUris(Path path, int searchDepth) {
        if (path == null) {
            return List.of();
        } else {
            logger.debug(String.format("Searching for javadocs in %s (depth=%d)", path, searchDepth));

            if (Files.isDirectory(path)) {
                return findJavadocUrisFromDirectory(path, searchDepth);
            } else {
                return findJavadocUrisFromFile(path).map(List::of).orElse(List.of());
            }
        }
    }

    private static List<URI> findJavadocUrisFromDirectory(Path directory, int searchDepth) {
        try (Stream<Path> walk = Files.walk(directory, searchDepth)) {
            return walk
                    .map(JavadocsFinder::findJavadocUrisFromFile)
                    .flatMap(Optional::stream)
                    .toList();
        } catch (IOException e) {
            logger.debug("Exception while requesting javadoc URIs", e);
            return List.of();
        }
    }

    private static Optional<URI> findJavadocUrisFromFile(Path path) {
        File file = path.toFile();

        if (
                JAVADOC_INDEX_FILE.equalsIgnoreCase(file.getName()) &&
                List.of("javadoc", "javadocs", "docs").contains(file.getParentFile().getName().toLowerCase())
        ) {
            try (Stream<String> lines = Files.lines(path)) {
                if (lines.anyMatch(l -> l.contains("javadoc"))) {
                    return Optional.of(path.toUri());
                }
            } catch (IOException e) {
                logger.debug(String.format("Error while reading %s", path), e);
            }
        }

        Optional<String> extension = getExtension(file.toString());
        if (extension.isPresent() &&
                ARCHIVE_EXTENSIONS.contains(extension.get()) &&
                file.getName().toLowerCase().endsWith("javadoc" + extension.get())
        ) {
            try (ZipFile zipFile = new ZipFile(file)) {
                if (zipFile.getEntry(JAVADOC_INDEX_FILE) != null) {
                    return Optional.of(new URI(String.format("jar:%s!/%s", file.toURI(), JAVADOC_INDEX_FILE)));
                }
            } catch (IOException | URISyntaxException e) {
                logger.warn(String.format("Error while reading %s", path), e);
            }
        }

        return Optional.empty();
    }

    private static Optional<String> getExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".")));
    }
}
