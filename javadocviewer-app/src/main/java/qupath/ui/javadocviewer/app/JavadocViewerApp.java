package qupath.ui.javadocviewer.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ui.javadocviewer.main.gui.viewer.JavadocViewer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * An application that starts a {@link JavadocViewer}.
 */
public class JavadocViewerApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(JavadocViewerApp.class);

    /**
     * Start the Javadoc viewer.
     *
     * @param args  directories to search for Javadocs
     */
    public static void main(String[] args) {
        Application.launch(JavadocViewerApp.class, args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        JavadocViewer javadocViewer = new JavadocViewer(
                null,
                getParameters().getRaw().stream().
                        map(param -> {
                            try {
                                return new URI(param);
                            } catch (URISyntaxException e) {
                                logger.warn(String.format("Couldn't convert URI %s", param), e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .toArray(URI[]::new)
        );

        Scene scene = new Scene(javadocViewer);
        stage.setScene(scene);
        stage.show();

        stage.setMinWidth(javadocViewer.getWidth());
        stage.setMinHeight(javadocViewer.getHeight());
    }
}