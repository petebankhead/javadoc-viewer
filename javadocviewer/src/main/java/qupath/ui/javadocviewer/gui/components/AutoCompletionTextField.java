package qupath.ui.javadocviewer.gui.components;

import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * A {@link TextField} that provides suggestions on a context menu.
 * <p>
 * Suggestions are grouped by category and must implement {@link AutoCompleteTextFieldEntry}.
 * <p>
 * Suggestions are sorted according to their order.
 * <p>
 * No more than {@link #MAX_ENTRIES} suggestions are displayed at a time.
 *
 * @param <T> the type of suggestions
 */
public class AutoCompletionTextField<T extends AutoCompleteTextFieldEntry> extends TextField {

    private static final int MAX_ENTRIES = 50;
    private static final int MAX_POPUP_HEIGHT = 300;
    private final ContextMenu entriesPopup = new ContextMenu();
    private final List<T> suggestions = new ArrayList<>();

    /**
     * Create the auto-completion text field
     */
    public AutoCompletionTextField() {
        setUpUI();
        setUpListeners();
    }

    /**
     * @return the list of suggestions of this text field. This
     * is an {@link ArrayList}, so elements can be added to it.
     */
    public List<T> getSuggestions() {
        return suggestions;
    }

    private void setUpUI() {
        entriesPopup.setMaxHeight(MAX_POPUP_HEIGHT);

        // Make context menu respect max height
        // See https://stackoverflow.com/a/58542568
        entriesPopup.addEventHandler(Menu.ON_SHOWING, e -> {
            Node content = entriesPopup.getSkin().getNode();
            if (content instanceof Region region) {
                region.setMaxHeight(entriesPopup.getMaxHeight());
            }
        });
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
                                .filter(entry -> entry.getSearchableText().toLowerCase().contains(loweredCaseEnteredText))
                                .sorted()
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
            List<MenuItem> items = entries.stream()
                    .map(AutoCompleteTextFieldEntry::getCategory)
                    .distinct()
                    .flatMap(category -> Stream.concat(
                            Stream.of(new CustomMenuItem(createCategoryItemText(category), false)),
                            entries.stream()
                                    .filter(entry -> entry.getCategory().equals(category))
                                    .map(entry -> {
                                        MenuItem menuItem = new CustomMenuItem(createEntryItemText(entry, filter), true);

                                        menuItem.setOnAction(actionEvent -> {
                                            entriesPopup.hide();
                                            entry.onSelected();
                                        });
                                        return menuItem;
                                    })
                    ))
                    .toList();

            entriesPopup.getItems().clear();

            // Add first item, show popup, and then add other items
            // This is used to avoid the popup to ignore the anchor position
            // See https://stackoverflow.com/a/58542568
            entriesPopup.getItems().add(items.getFirst());
            entriesPopup.show(this, Side.BOTTOM, 0, 0);
            entriesPopup.getItems().addAll(items.stream().skip(1).toList());
        }
    }

    private static Node createCategoryItemText(String category) {
        Text text = new Text(category);
        text.getStyleClass().add("category-text");
        return text;
    }

    private Node createEntryItemText(T entry, String filter) {
        String searchableText = entry.getSearchableText();
        String text = entry.getName();

        int searchableTextIndex = text.indexOf(searchableText);
        int filterIndex = text.toLowerCase().indexOf(filter.toLowerCase(), searchableTextIndex);

        Text textBefore = new Text(text.substring(0, filterIndex));
        Text textFiltered = new Text(text.substring(filterIndex,  filterIndex + filter.length()));
        Text textAfter = new Text(text.substring(filterIndex + filter.length()));

        textBefore.getStyleClass().add("regular-text");
        textFiltered.getStyleClass().add("highlighted-text");
        textAfter.getStyleClass().add("regular-text");

        return new TextFlow(textBefore, textFiltered, textAfter);
    }
}
