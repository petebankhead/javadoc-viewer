package qupath.ui.javadocviewer.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A Javadoc specified by a {@link URI} and containing {@link JavadocElement JavadocElements}.
 * Elements are populated by looking at the {@link #INDEX_ALL_PAGE} page of the Javadoc.
 */
public class Javadoc {

    private static final Logger logger = LoggerFactory.getLogger(Javadoc.class);
    private static final Pattern ENTRY_PATTERN = Pattern.compile("<dt>(.*?)</dt>");
    private static final Pattern URI_PATTERN = Pattern.compile("href=\"(.+?)\"");
    private static final Pattern NAME_PATTERN = Pattern.compile("<a .*?>(?:<span .*?>)?(.*?)(?:</span>)?</a>");
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("</a> - (.+?) ");
    private static final String INDEX_PAGE = "index.html";
    private static final String INDEX_ALL_PAGE = "index-all.html";
    private final URI uri;
    private final List<JavadocElement> elements;

    private Javadoc(URI uri, List<JavadocElement> elements) {
        this.uri = uri;
        this.elements = Collections.unmodifiableList(elements);
    }

    /**
     * Asynchronously attempt to create a Javadoc from the specified URI.
     *
     * @param uri  the URI of the Javadoc
     * @return a CompletableFuture with the created Javadoc, or an empty Optional if the creation failed
     */
    public static CompletableFuture<Optional<Javadoc>> create(URI uri) {
        return getIndexPage(uri).thenApply(indexPage -> indexPage.map(page -> new Javadoc(
                uri,
                parseJavadocIndexPage(
                        uri.toString().substring(0, uri.toString().lastIndexOf('/') + 1),
                        page
                )
        )));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Javadoc javadoc = (Javadoc) o;
        return Objects.equals(uri, javadoc.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uri);
    }

    @Override
    public String toString() {
        return "Javadoc{" +
                "uri=" + uri +
                ", elements=" + elements +
                '}';
    }

    /**
     * @return the URI of this Javadoc
     */
    public URI getUri() {
        return uri;
    }

    /**
     * @return an unmodifiable view of the elements of this Javadoc
     */
    public List<JavadocElement> getElements() {
        return elements;
    }

    private static List<JavadocElement> parseJavadocIndexPage(String javadocURI, String indexHTMLPage) {
        List<JavadocElement> elements = new ArrayList<>();
        Matcher entryMatcher = ENTRY_PATTERN.matcher(indexHTMLPage);

        while (entryMatcher.find()) {
            if (entryMatcher.groupCount() > 0) {
                Matcher uriMatcher = URI_PATTERN.matcher(entryMatcher.group(1));
                Matcher nameMatcher = NAME_PATTERN.matcher(entryMatcher.group(1));
                Matcher categoryMatcher = CATEGORY_PATTERN.matcher(entryMatcher.group(1));

                if (uriMatcher.find() && uriMatcher.groupCount() > 0
                        && nameMatcher.find() && nameMatcher.groupCount() > 0
                        && categoryMatcher.find() && categoryMatcher.groupCount() > 0
                ) {
                    String name = nameMatcher.group(1).replace("&lt;", "<").replace("&gt;", ">");
                    if (nameMatcher.find() && nameMatcher.groupCount() > 0) {
                        name = nameMatcher.group(1) + "." + name;
                    }
                    String link = javadocURI + uriMatcher.group(1);

                    try {
                        URI uri = new URI(link);
                        elements.add(new JavadocElement(
                                uri,
                                name,
                                categoryMatcher.group(1)
                        ));
                    } catch (URISyntaxException e) {
                        logger.debug(String.format("Cannot create URI %s of Javadoc element", link), e);
                    }
                }
            }
        }

        return elements;
    }

    private static CompletableFuture<Optional<String>> getIndexPage(URI javadocIndexURI) {
        String link = javadocIndexURI.toString().replace(INDEX_PAGE, INDEX_ALL_PAGE);
        URI indexAllURI;
        try {
            indexAllURI = new URI(link);
        } catch (URISyntaxException e) {
            logger.debug(String.format("Cannot create URI %s of index page", link), e);
            return CompletableFuture.completedFuture(Optional.empty());
        }

        if (indexAllURI.getScheme().contains("http")) {
            return getIndexPageFromHttp(indexAllURI);
        } else {
            return CompletableFuture.supplyAsync(() -> {
                if (indexAllURI.getScheme().contains("jar")) {
                    return getIndexPageFromJar(indexAllURI);
                } else {
                    return getIndexPageFromDirectory(indexAllURI);
                }
            });
        }
    }

    private static CompletableFuture<Optional<String>> getIndexPageFromHttp(URI uri) {
        return HttpClient.newHttpClient().sendAsync(
                HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(Duration.of(10, ChronoUnit.SECONDS))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        ).handle((response, error) -> {
            if (response == null || error != null) {
                if (error != null) {
                    logger.debug("Error when retrieving Javadoc index page", error);
                }
                return Optional.empty();
            } else {
                return Optional.ofNullable(response.body());
            }
        });
    }

    private static Optional<String> getIndexPageFromJar(URI uri) {
        String jarURI = uri.toString().substring(
                uri.toString().indexOf('/'),
                uri.toString().lastIndexOf('!')
        );

        try (ZipFile zipFile = new ZipFile(jarURI)) {
            ZipEntry entry = zipFile.getEntry(INDEX_ALL_PAGE);

            if (entry == null) {
                logger.debug(String.format("%s not found in %s", INDEX_ALL_PAGE, jarURI));
                return Optional.empty();
            } else {
                try (
                        InputStream inputStream = zipFile.getInputStream(zipFile.getEntry(INDEX_ALL_PAGE));
                        Scanner scanner = new Scanner(inputStream)
                ) {
                    StringBuilder lines = new StringBuilder();
                    while (scanner.hasNextLine()) {
                        lines.append(scanner.nextLine());
                    }
                    return Optional.of(lines.toString());
                }
            }
        } catch (IOException e) {
            logger.debug(String.format("Error while reading %s", jarURI), e);
            return Optional.empty();
        }
    }

    private static Optional<String> getIndexPageFromDirectory(URI uri) {
        try (Stream<String> lines = Files.lines(Paths.get(uri))) {
            return Optional.of(lines.collect(Collectors.joining("\n")));
        } catch (Exception e) {
            logger.debug(String.format("Error while reading %s", uri), e);
            return Optional.empty();
        }
    }
}
