package qupath.ui.javadocviewer.gui.viewer;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;

/**
 * A command to start a {@link JavadocViewer} in a standalone window.
 * Only one instance of the viewer will be created.
 */
public class JavadocViewerCommand implements Runnable {

    private final Stage owner;
    private final URI[] urisToSearch;
    private Stage stage;

    /**
     * Create the command. This will not create the viewer yet.
     *
     * @param owner  the stage that should own the viewer window. Can be null
     * @param urisToSearch  URIs to search for Javadocs. See {@link JavadocViewer#JavadocViewer(ReadOnlyStringProperty, URI...)}
     */
    public JavadocViewerCommand(Stage owner, URI... urisToSearch) {
        this.owner = owner;
        this.urisToSearch = urisToSearch;
    }

    @Override
    public void run() {
        if (stage == null) {
            try {
                stage = new Stage();
                if (owner != null) {
                    stage.initOwner(owner);
                }

                JavadocViewer javadocViewer = new JavadocViewer(null, urisToSearch);

                Scene scene = new Scene(javadocViewer);
                stage.setScene(scene);
                stage.show();

                stage.setMinWidth(javadocViewer.getWidth());
                stage.setMinHeight(javadocViewer.getHeight());

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        stage.show();
        stage.requestFocus();
    }
}
