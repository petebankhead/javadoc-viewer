package qupath.ui.javadocviewer.gui.viewer;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import qupath.ui.javadocviewer.gui.components.AutoCompletionTextField;
import qupath.ui.javadocviewer.core.Javadoc;
import qupath.ui.javadocviewer.core.JavadocsFinder;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A window to browse several Javadocs found by the {@link JavadocsFinder}.
 * An {@link AutoCompletionTextField} allows to search for Javadoc elements.
 */
public class JavadocViewer extends BorderPane {

    private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ui.javadocviewer.strings");
    private static final Pattern REDIRECTION_PATTERN = Pattern.compile("window\\.location\\.replace\\(['\"](.*?)['\"]\\)");
    private static final List<String> CATEGORIES_TO_SKIP = List.of("package", "module", "Variable", "Exception", "Annotation", "Element");
    private final WebView webView = new WebView();
    @FXML
    private Button back;
    @FXML
    private Button forward;
    @FXML
    private ComboBox<URI> uris;
    @FXML
    private HBox searchContainer;
    @FXML
    private AutoCompletionTextField<JavadocEntry> autoCompletionTextField;

    /**
     * Create the javadoc viewer.
     *
     * @param stylesheet a property containing a link to a stylesheet which should
     *                   be applied to this viewer. Can be null
     * @param urisToSearch URIs to search for Javadocs. See {@link JavadocsFinder#findJavadocs(URI...)}
     * @throws IOException if the window creation fails
     */
    public JavadocViewer(ReadOnlyStringProperty stylesheet, URI... urisToSearch) throws IOException {
        initUI(stylesheet, Arrays.stream(urisToSearch).toList());
        setUpListeners();
    }

    /**
     * Set the search text field to an input query.
     *
     * @param input the search query string.
     */
    public void setSearchInput(String input) {
        autoCompletionTextField.setText(input);
    }

    @FXML
    private void onBackClicked(ActionEvent ignoredEvent) {
        offset(-1);
    }

    @FXML
    private void onForwardClicked(ActionEvent ignoredEvent) {
        offset(1);
    }

    private void initUI(ReadOnlyStringProperty stylesheet, List<URI> urisToSearch) throws IOException {
        FXMLLoader loader = new FXMLLoader(JavadocViewer.class.getResource("javadoc_viewer.fxml"), resources);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();

        setCenter(webView);

        this.uris.setCellFactory(col -> new ListCell<>() {
            @Override
            protected void updateItem(URI item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setGraphic(null);
                } else {
                    setText(getName(item));
                }
            }
        });
        this.uris.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(URI item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setGraphic(null);
                } else {
                    setText(getName(item));
                }
            }
        });

        if (stylesheet != null) {
            webView.getEngine().userStyleSheetLocationProperty().bind(stylesheet);
        }

        webView.getEngine().loadContent(resources.getString("JavadocViewer.findingJavadocs"));
        JavadocsFinder.findJavadocs(urisToSearch.toArray(new URI[0])).thenAccept(javadocs -> Platform.runLater(() -> {
            this.uris.getItems().setAll(javadocs.stream()
                    .map(Javadoc::uri)
                    .sorted(Comparator.comparing(JavadocViewer::getName))
                    .toList()
            );

            if (this.uris.getItems().isEmpty()) {
                webView.getEngine().loadContent(resources.getString("JavadocViewer.noJavadocs"));
            } else {
                this.uris.getSelectionModel().select(this.uris.getItems().stream()
                        .filter(u -> getName(u).toLowerCase().contains("qupath"))
                        .findFirst()
                        .orElse(this.uris.getItems().getFirst())
                );
            }

            autoCompletionTextField.getSuggestions().addAll(javadocs.stream()
                    .map(Javadoc::elements)
                    .flatMap(List::stream)
                    .map(javadocElement -> new JavadocEntry(
                            javadocElement,
                            () -> webView.getEngine().load(javadocElement.uri().toString())
                    ))
                    .filter(javadocEntry -> !CATEGORIES_TO_SKIP.contains(javadocEntry.getCategory()))
                    .toList());
        }));
    }

    private void setUpListeners() {
        back.disableProperty().bind(webView.getEngine().getHistory().currentIndexProperty().isEqualTo(0));
        forward.disableProperty().bind(webView.getEngine().getHistory().currentIndexProperty().greaterThanOrEqualTo(
                Bindings.size(webView.getEngine().getHistory().getEntries()).subtract(1)
        ));

        uris.getSelectionModel().selectedItemProperty().addListener((p, o, n) -> {
            if (n != null) {
                webView.getEngine().load(n.toString());
            }
        });
        if (uris.getSelectionModel().getSelectedItem() != null) {
            webView.getEngine().load(uris.getSelectionModel().getSelectedItem().toString());
        }

        // Sometimes, redirection is not automatically performed
        // (see https://github.com/qupath/qupath/pull/1513#issuecomment-2095553840)
        // This code enforces redirection
        webView.getEngine().documentProperty().addListener((p, o, n) -> {
            if (n != null) {
                Matcher redirectionMatcher = REDIRECTION_PATTERN.matcher(n.getDocumentElement().getTextContent());

                if (redirectionMatcher.find() && redirectionMatcher.groupCount() > 0) {
                    changeLocation(webView.getEngine().getLocation(), redirectionMatcher.group(1)).ifPresent(newLocation ->
                            webView.getEngine().load(newLocation)
                    );
                }
            }
        });
    }

    private void offset(int offset) {
        WebHistory history = webView.getEngine().getHistory();
        int index = history.getCurrentIndex() + offset;

        if (index >= 0 && index < history.getEntries().size()) {
            history.go(offset);
        }
    }

    private static String getName(URI uri) {
        if ("jar".equals(uri.getScheme()))
            uri = URI.create(uri.getRawSchemeSpecificPart());
        var path = Paths.get(uri);

        String name = path.getFileName().toString().toLowerCase();
        // If we have index.html, we want to take the name of the parent
        if (name.endsWith(".html")) {
            var fileName = path.getParent().getFileName().toString();
            if (fileName.endsWith(".jar!"))
                fileName = fileName.substring(0, fileName.length()-1);
            return fileName;
        }
        return name;
    }

    private static Optional<String> changeLocation(String currentLocation, String newLocation) {
        int index = currentLocation.lastIndexOf("/");

        if (index == -1) {
            return Optional.empty();
        } else {
            return Optional.of(currentLocation.substring(0, currentLocation.lastIndexOf("/")) + "/" + newLocation);
        }
    }
}
