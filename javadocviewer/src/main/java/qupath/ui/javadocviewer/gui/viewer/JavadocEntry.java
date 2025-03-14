package qupath.ui.javadocviewer.gui.viewer;

import qupath.ui.javadocviewer.gui.components.AutoCompleteTextFieldEntry;
import qupath.ui.javadocviewer.core.JavadocElement;

import java.util.Map;

/**
 * An {@link AutoCompleteTextFieldEntry} that represents a {@link JavadocElement}.
 */
class JavadocEntry implements AutoCompleteTextFieldEntry {

    private static final Map<String, Integer> CATEGORY_ORDER = Map.of(
            "Class", 1,
            "Interface", 2,
            "Enum", 3,
            "Constructor", 4,
            "Static", 5,
            "Method", 6
    );
    private final JavadocElement javadocElement;
    private final Runnable onSelected;
    private String searchableText;

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
        if (searchableText == null) {
            searchableText = switch (javadocElement.category()) {
                // expect "some.package.Class". Retain "Class"
                case "Class", "Interface" -> javadocElement.name().substring(javadocElement.name().lastIndexOf(".") + 1);
                // expects "some.package.Class.Enum" or "Class.Enum.variable". Retain "Class.Enum" or "Enum.variable"
                case "Enum" -> {
                    int lastPointIndex = javadocElement.name().lastIndexOf(".");
                    if (lastPointIndex > -1) {
                        int secondLastPointIndex = javadocElement.name().lastIndexOf(".", lastPointIndex-1);
                        if (secondLastPointIndex > -1) {
                            yield javadocElement.name().substring(secondLastPointIndex+1);
                        }
                    }
                    yield javadocElement.name();
                }
                // expect "Class.Class(Parameter)" for constructors or "Class.function(Parameter) for functions. Retain "Class" or "function"
                case "Constructor", "Static", "Method" -> {
                    int pointIndex = javadocElement.name().indexOf(".");
                    int parenthesisIndex = javadocElement.name().indexOf("(");

                    if (parenthesisIndex > -1) {
                        yield javadocElement.name().substring(pointIndex+1, parenthesisIndex);
                    } else {
                        yield javadocElement.name().substring(pointIndex+1);
                    }
                }
                default -> javadocElement.name();
            };
        }
        return searchableText;
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
        int categoryComparison = CATEGORY_ORDER.getOrDefault(getCategory(), 0) - CATEGORY_ORDER.getOrDefault(otherEntry.getCategory(), 0);
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
