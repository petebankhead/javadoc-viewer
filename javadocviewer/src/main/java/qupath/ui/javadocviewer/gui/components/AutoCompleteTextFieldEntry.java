package qupath.ui.javadocviewer.gui.components;

/**
 * An entry to a {@link AutoCompletionTextField}.
 */
public interface AutoCompleteTextFieldEntry extends Comparable<AutoCompleteTextFieldEntry> {

    /**
     * @return the text that should be displayed by this entry
     */
    String getName();

    /**
     * @return the text to use when filtering this entry
     */
    String getSearchableText();

    /**
     * @return the category this entry belongs to
     */
    String getCategory();

    /**
     * This function is called whenever this entry is selected by the user.
     */
    void onSelected();
}
