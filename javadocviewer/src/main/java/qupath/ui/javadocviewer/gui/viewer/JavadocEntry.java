package qupath.ui.javadocviewer.gui.viewer;

import qupath.ui.javadocviewer.gui.components.AutoCompleteTextFieldEntry;
import qupath.ui.javadocviewer.core.JavadocElement;

/**
 * An {@link AutoCompleteTextFieldEntry} that represents a {@link JavadocElement}.
 */
class JavadocEntry implements AutoCompleteTextFieldEntry {

    private final JavadocElement javadocElement;
    private final Runnable onSelected;

    /**
     * Create a Javadoc entry from a Javadoc element.
     *
     * @param javadocElement the javadoc element to represent
     * @param onSelected a function to call when this element is selected
     */
    public JavadocEntry(JavadocElement javadocElement, Runnable onSelected) {
        this.javadocElement = javadocElement;
        this.onSelected = onSelected;
    }

    @Override
    public String getName() {
        return javadocElement.name();
    }

    @Override
    public String getSearchableText() {
        int parenthesisIndex = javadocElement.name().indexOf("(");

        if (parenthesisIndex > -1) {
            return javadocElement.name().substring(0, parenthesisIndex);
        } else {
            return javadocElement.name();
        }
    }

    @Override
    public String getCategory() {
        return javadocElement.category();
    }

    @Override
    public void onSelected() {
        onSelected.run();
    }
}
