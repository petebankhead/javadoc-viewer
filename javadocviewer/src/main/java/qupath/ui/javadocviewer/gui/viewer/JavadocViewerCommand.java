package qupath.ui.javadocviewer.gui.viewer;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.util.ResourceBundle;

/**
 * A command to start a {@link JavadocViewer} in a standalone window.
 * Only one instance of the viewer will be created.
 */
public class JavadocViewerCommand implements Runnable {

    private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ui.javadocviewer.strings");
    private final Stage owner;
    private final ReadOnlyStringProperty stylesheet;
    private final URI[] urisToSearch;
    private Stage stage;
    private JavadocViewer javadocViewer;

    /**
     * Create the command. This will not create the viewer until either the command is run or {@link #getJavadocViewer()} is called.
     *
     * @param owner the stage that should own the viewer window. Can be null
     * @param stylesheet a property containing a link to a stylesheet which should
     *                   be applied to the viewer. Can be null
     * @param urisToSearch URIs to search for Javadocs. See {@link JavadocViewer#JavadocViewer(ReadOnlyStringProperty, URI...)}
     */
    public JavadocViewerCommand(Stage owner, ReadOnlyStringProperty stylesheet, URI... urisToSearch) {
        this.owner = owner;
        this.stylesheet = stylesheet;
        this.urisToSearch = urisToSearch;
    }

    /**
     * Get a reference to the singleton {@link JavadocViewer}, creating it if required.
     *
     * @return the singleton {@link JavadocViewer}
     * @throws RuntimeException if the JavadocViewer cannot be initialized
     */
    public JavadocViewer getJavadocViewer() {
        if (javadocViewer == null) {
            try {
                javadocViewer = new JavadocViewer(stylesheet, urisToSearch);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return javadocViewer;
    }

    @Override
    public void run() {
        if (stage == null) {
            stage = new Stage();
            if (owner != null) {
                stage.initOwner(owner);
            }
            stage.setTitle(resources.getString("JavadocViewer.title"));

            javadocViewer = getJavadocViewer();

            Scene scene = new Scene(javadocViewer);
            stage.setScene(scene);
            stage.show();

            stage.setMinWidth(javadocViewer.getWidth());
            stage.setMinHeight(javadocViewer.getHeight());
        }

        stage.show();
        stage.requestFocus();
    }
}
