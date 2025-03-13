package qupath.ui.javadocviewer.gui.viewer;

import qupath.ui.javadocviewer.gui.components.AutoCompleteTextFieldEntry;
import qupath.ui.javadocviewer.core.JavadocElement;

import java.util.Map;

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

    @Override
    public int compareTo(AutoCompleteTextFieldEntry otherEntry) {
        Map<String, Integer> order = Map.of(
                "Class", 1,
                "Interface", 2,
                "Enum", 3,
                "Constructor", 4,
                "Static", 5,
                "Method", 6,
                "Variable", 7,
                "Exception", 8,
                "Annotation", 9,
                "Element", 10     // display categories in that order
        );

        int categoryComparison = order.getOrDefault(getCategory(), 0) - order.getOrDefault(otherEntry.getCategory(), 0);
        if (categoryComparison != 0) {
            return categoryComparison;
        }

        return getName().compareTo(otherEntry.getName());
    }

    @Override
    public String toString() {
        return String.format("Javadoc entry of %s", javadocElement);
    }
}
