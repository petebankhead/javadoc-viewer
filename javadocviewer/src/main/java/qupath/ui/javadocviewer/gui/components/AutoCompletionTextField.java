package qupath.ui.javadocviewer.gui.components;

import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * A {@link TextField} that provides suggestions on a context menu.
 * Suggestions are grouped by category and must implement {@link AutoCompleteTextFieldEntry}.
 *
 * @param <T>  the type of suggestions
 */
public class AutoCompletionTextField<T extends AutoCompleteTextFieldEntry> extends TextField {

    private static final int MAX_ENTRIES = 10;
    private final ContextMenu entriesPopup = new ContextMenu();
    private final List<T> suggestions = new ArrayList<>();

    /**
     * Create the auto-completion text field
     */
    public AutoCompletionTextField() {
        setUpListeners();
    }

    /**
     * @return the list of suggestions of this text field. This
     * is an {@link ArrayList}, so elements can be added to it.
     */
    public List<T> getSuggestions() {
        return suggestions;
    }

    private void setUpListeners() {
        textProperty().addListener((p, o, n) -> {
            String enteredText = getText();

            if (enteredText == null || enteredText.isEmpty()) {
                entriesPopup.hide();
            } else {
                String loweredCaseEnteredText = enteredText.toLowerCase();

                populatePopup(
                        suggestions.stream()
                                .filter(entry -> entry.getName().toLowerCase().contains(loweredCaseEnteredText))
                                .limit(MAX_ENTRIES)
                                .toList(),
                        enteredText
                );
            }
        });

        focusedProperty().addListener((p, o, n) -> entriesPopup.hide());
    }

    private void populatePopup(List<T> entries, String filter) {
        if (entries.isEmpty()) {
            entriesPopup.hide();
        } else {
            entriesPopup.getItems().setAll(entries.stream()
                    .map(AutoCompleteTextFieldEntry::getCategory)
                    .distinct()
                    .flatMap(category -> Stream.concat(
                            Stream.of(new CustomMenuItem(createCategoryItemText(category), false)),
                            entries.stream()
                                    .filter(entry -> entry.getCategory().equals(category))
                                    .map(entry -> {
                                        MenuItem menuItem = new CustomMenuItem(createEntryItemText(entry.getName(), filter), true);

                                        menuItem.setOnAction(actionEvent -> {
                                            setText(entry.getName());
                                            positionCaret(entry.getName().length());
                                            entriesPopup.hide();
                                            entry.onSelected();
                                        });
                                        return menuItem;
                                    })
                    ))
                    .toList()
            );

            entriesPopup.show(this, Side.BOTTOM, 0, 0);
        }
    }

    private static Node createCategoryItemText(String category) {
        Text text = new Text(category);
        text.getStyleClass().add("category-text");
        return text;
    }

    private static Node createEntryItemText(String text, String filter) {
        int filterIndex = text.toLowerCase().indexOf(filter.toLowerCase());

        Text textBefore = new Text(text.substring(0, filterIndex));
        Text textFiltered = new Text(text.substring(filterIndex,  filterIndex + filter.length()));
        Text textAfter = new Text(text.substring(filterIndex + filter.length()));

        textBefore.getStyleClass().add("regular-text");
        textFiltered.getStyleClass().add("highlighted-text");
        textAfter.getStyleClass().add("regular-text");

        return new TextFlow(textBefore, textFiltered, textAfter);
    }
}
