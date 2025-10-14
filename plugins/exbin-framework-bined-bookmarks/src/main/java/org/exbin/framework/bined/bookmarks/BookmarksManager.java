/*
 * Copyright (C) ExBin Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exbin.framework.bined.bookmarks;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.exbin.bined.CodeAreaSelection;
import org.exbin.bined.swing.section.SectCodeArea;
import org.exbin.framework.App;
import org.exbin.framework.action.api.ActionConsts;
import org.exbin.framework.menu.api.ActionMenuCreation;
import org.exbin.framework.action.api.ActionModuleApi;
import org.exbin.framework.action.api.DialogParentComponent;
import org.exbin.framework.menu.api.MenuManagement;
import org.exbin.framework.bined.BinEdFileHandler;
import org.exbin.framework.bined.BinEdFileManager;
import org.exbin.framework.bined.BinedModule;
import org.exbin.framework.bined.bookmarks.action.AddBookmarkAction;
import org.exbin.framework.bined.bookmarks.action.EditBookmarkAction;
import org.exbin.framework.bined.bookmarks.action.ManageBookmarksAction;
import org.exbin.framework.bined.bookmarks.gui.BookmarksManagerPanel;
import org.exbin.framework.bined.bookmarks.model.BookmarkRecord;
import org.exbin.framework.bined.bookmarks.options.BookmarkOptions;
import org.exbin.framework.contribution.api.GroupSequenceContributionRule;
import org.exbin.framework.contribution.api.SequenceContribution;
import org.exbin.framework.editor.api.EditorModuleApi;
import org.exbin.framework.editor.api.EditorProvider;
import org.exbin.framework.file.api.FileHandler;
import org.exbin.framework.options.api.OptionsModuleApi;
import org.exbin.framework.utils.ActionUtils;
import org.exbin.framework.language.api.LanguageModuleApi;
import org.exbin.framework.menu.api.MenuModuleApi;
import org.exbin.framework.options.api.OptionsStorage;
import org.exbin.framework.utils.UiUtils;

/**
 * Bookmarks manager.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class BookmarksManager {

    public static final String BOOKMARKS_POPUP_SUBMENU_ID = BinedBookmarksModule.MODULE_ID + ".bookmarksPopupSubMenu";

    private final ResourceBundle resourceBundle = App.getModule(LanguageModuleApi.class).getBundle(BookmarksManager.class);

    private final List<BookmarkRecord> bookmarkRecords = new ArrayList<>();
    private BookmarkOptions bookmarkOptions;
    private BookmarksPositionColorModifier bookmarksPositionColorModifier;

    private EditorProvider editorProvider;

    private final ManageBookmarksAction manageBookmarksAction = new ManageBookmarksAction();
    private final AddBookmarkAction addBookmarkAction = new AddBookmarkAction();
    private final EditBookmarkAction editBookmarkAction = new EditBookmarkAction();
    private JMenu bookmarksMenu;

    public BookmarksManager() {
    }

    public void setEditorProvider(EditorProvider editorProvider) {
        this.editorProvider = editorProvider;
        EditorModuleApi editorModule = App.getModule(EditorModuleApi.class);
        editorModule.addEditorProviderComponentListener((editorComponent) -> {
            registerBookmarksComponentActions(editorComponent);
        });
    }

    public void init() {
        addBookmarkAction.setup(resourceBundle);
        editBookmarkAction.setup(resourceBundle);
        manageBookmarksAction.setup(resourceBundle);

        BinedModule binedModule = App.getModule(BinedModule.class);

        OptionsModuleApi optionsModule = App.getModule(OptionsModuleApi.class);
        OptionsStorage optionsStorage = optionsModule.getAppOptions();
        bookmarkOptions = new BookmarkOptions(optionsStorage);
        loadBookmarkRecords();
        updateBookmarksMenu();
        bookmarksPositionColorModifier = new BookmarksPositionColorModifier(bookmarkRecords);
        BinEdFileManager fileManager = binedModule.getFileManager();
        fileManager.addPainterColorModifier(bookmarksPositionColorModifier);
    }

    private void loadBookmarkRecords() {
        int bookmarksCount = bookmarkOptions.getBookmarksCount();
        for (int i = 0; i < bookmarksCount; i++) {
            BookmarkRecord bookmarkRecord = bookmarkOptions.getBookmarkRecord(i);
            bookmarkRecords.add(bookmarkRecord);
        }
    }

    private void saveBookmarkRecords() {
        int bookmarksCount = bookmarkRecords.size();
        bookmarkOptions.setBookmarksCount(bookmarksCount);
        for (int i = 0; i < bookmarksCount; i++) {
            bookmarkOptions.setBookmarkRecord(i, bookmarkRecords.get(i));
        }
    }

    @Nonnull
    public List<BookmarkRecord> getBookmarkRecords() {
        return bookmarkRecords;
    }

    @Nonnull
    public AbstractAction getManageBookmarksAction() {
        return manageBookmarksAction;
    }

    public void setBookmarkRecords(List<BookmarkRecord> records) {
        bookmarkRecords.clear();
        bookmarkRecords.addAll(records);
        saveBookmarkRecords();
        bookmarksPositionColorModifier.notifyBookmarksChanged();
        updateBookmarksMenu();
    }

    @Nonnull
    public BookmarksManagerPanel createBookmarksManagerPanel() {
        final BookmarksManagerPanel bookmarksManagerPanel = new BookmarksManagerPanel();
        final DialogParentComponent dialogParentComponent = () -> bookmarksManagerPanel;
        bookmarksManagerPanel.setController(new BookmarksManagerPanel.Controller() {
            @Override
            public void addRecord() {
                Optional<FileHandler> activeFile = editorProvider.getActiveFile();
                if (activeFile.isPresent()) {
                    BinEdFileHandler fileHandler = (BinEdFileHandler) activeFile.get();
                    SectCodeArea codeArea = fileHandler.getCodeArea();
                    addBookmarkAction.setCurrentSelection(codeArea.getSelectionHandler());
                } else {
                    addBookmarkAction.setCurrentSelection(new CodeAreaSelection());
                }
                addBookmarkAction.setDialogParentComponent(dialogParentComponent);
                addBookmarkAction.actionPerformed(null);
                Optional<BookmarkRecord> bookmarkRecord = addBookmarkAction.getBookmarkRecord();
                if (bookmarkRecord.isPresent()) {
                    List<BookmarkRecord> records = bookmarksManagerPanel.getBookmarkRecords();
                    records.add(bookmarkRecord.get());
                    bookmarksManagerPanel.setBookmarkRecords(records);
                }
            }

            @Override
            public void editRecord() {
                Optional<FileHandler> activeFile = editorProvider.getActiveFile();
                if (activeFile.isPresent()) {
                    BinEdFileHandler fileHandler = (BinEdFileHandler) activeFile.get();
                    SectCodeArea codeArea = fileHandler.getCodeArea();
                    editBookmarkAction.setCurrentSelection(codeArea.getSelectionHandler());
                } else {
                    editBookmarkAction.setCurrentSelection(new CodeAreaSelection());
                }
                BookmarkRecord selectedRecord = bookmarksManagerPanel.getSelectedRecord();
                int selectedRow = bookmarksManagerPanel.getTable().getSelectedRow();
                editBookmarkAction.setBookmarkRecord(new BookmarkRecord(selectedRecord));
                editBookmarkAction.setDialogParentComponent(dialogParentComponent);
                editBookmarkAction.actionPerformed(null);
                Optional<BookmarkRecord> bookmarkRecord = editBookmarkAction.getBookmarkRecord();
                if (bookmarkRecord.isPresent()) {
                    bookmarksManagerPanel.updateRecord(bookmarkRecord.get(), selectedRow);
                }
            }

            @Override
            public void removeRecord() {
                int[] selectedRows = bookmarksManagerPanel.getTable().getSelectedRows();
                Arrays.sort(selectedRows);
                List<BookmarkRecord> records = bookmarksManagerPanel.getBookmarkRecords();
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    records.remove(selectedRows[i]);
                }
                bookmarksManagerPanel.setBookmarkRecords(records);
            }

            @Override
            public void selectAll() {
                bookmarksManagerPanel.getTable().selectAll();
            }

            @Override
            public void moveUp() {
                JTable table = bookmarksManagerPanel.getTable();
                int[] selectedRows = table.getSelectedRows();
                Arrays.sort(selectedRows);
                List<BookmarkRecord> records = bookmarksManagerPanel.getBookmarkRecords();
                ListSelectionModel selectionModel = table.getSelectionModel();
                for (int i = 0; i < selectedRows.length; i++) {
                    int index = selectedRows[i];
                    selectionModel.removeSelectionInterval(index, index);
                    BookmarkRecord movedRecord = records.remove(index - 1);
                    records.add(index, movedRecord);
                    table.addRowSelectionInterval(index - 1, index - 1);
                }
                bookmarksManagerPanel.updateBookmarkRecords(records);
            }

            @Override
            public void moveDown() {
                JTable table = bookmarksManagerPanel.getTable();
                int[] selectedRows = table.getSelectedRows();
                Arrays.sort(selectedRows);
                List<BookmarkRecord> records = bookmarksManagerPanel.getBookmarkRecords();
                ListSelectionModel selectionModel = table.getSelectionModel();
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    int index = selectedRows[i];
                    selectionModel.removeSelectionInterval(index, index);
                    BookmarkRecord movedRecord = records.remove(index);
                    records.add(index + 1, movedRecord);
                    table.addRowSelectionInterval(index + 1, index + 1);
                }
                bookmarksManagerPanel.updateBookmarkRecords(records);
            }
        });

        return bookmarksManagerPanel;
    }

    @Nonnull
    public JMenu getBookmarksMenu() {
        if (bookmarksMenu == null) {
            Action bookmarksMenuAction = new AbstractAction(resourceBundle.getString("bookmarksMenu.text")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                }
            };
            bookmarksMenuAction.putValue(Action.SHORT_DESCRIPTION, resourceBundle.getString("bookmarksMenu.shortDescription"));
            bookmarksMenu = UiUtils.createMenu();
            bookmarksMenu.setAction(bookmarksMenuAction);
            updateBookmarksMenu();
        }
        return bookmarksMenu;
    }

    public void registerBookmarksPopupMenuActions() {
        MenuModuleApi menuModule = App.getModule(MenuModuleApi.class);
        Action bookmarksPopupMenuAction = new AbstractAction(resourceBundle.getString("bookmarksMenu.text")) {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        bookmarksPopupMenuAction.putValue(ActionConsts.ACTION_MENU_CREATION, new ActionMenuCreation() {
            @Override
            public boolean shouldCreate(String menuId, String subMenuId) {
                BinedModule binedModule = App.getModule(BinedModule.class);
                BinedModule.PopupMenuVariant menuVariant = binedModule.getPopupMenuVariant();
                return menuVariant == BinedModule.PopupMenuVariant.EDITOR;
            }

            @Override
            public void onCreate(JMenuItem menuItem, String menuId, String subMenuId) {
            }
        });
        bookmarksPopupMenuAction.putValue(Action.SHORT_DESCRIPTION, resourceBundle.getString("bookmarksMenu.shortDescription"));
        MenuManagement mgmt = menuModule.getMenuManagement(BinedModule.CODE_AREA_POPUP_MENU_ID, BinedBookmarksModule.MODULE_ID);
        SequenceContribution contribution = mgmt.registerMenuItem(() -> {
            JMenu bookmarksPopupMenu = UiUtils.createMenu();
            bookmarksPopupMenu.setAction(bookmarksPopupMenuAction);
            bookmarksPopupMenu.addMenuListener(new MenuListener() {
                @Override
                public void menuSelected(MenuEvent e) {
                    updateBookmarksMenu(bookmarksPopupMenu);
                }

                @Override
                public void menuDeselected(MenuEvent e) {
                }

                @Override
                public void menuCanceled(MenuEvent e) {
                }
            });
            return bookmarksPopupMenu;
        });
        mgmt.registerMenuRule(contribution, new GroupSequenceContributionRule(BinedModule.CODE_AREA_POPUP_FIND_GROUP_ID));
    }

    public void registerBookmarksComponentActions(JComponent component) {
        ActionMap actionMap = component.getActionMap();
        InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        int metaMask = ActionUtils.getMetaMask();
        for (int i = 0; i < 10; i++) {
            final int bookmarkIndex = i;
            String goToActionKey = "go-to-bookmark-" + i;
            actionMap.put(goToActionKey, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Optional<FileHandler> activeFile = editorProvider.getActiveFile();
                    if (activeFile.isPresent()) {
                        BinEdFileHandler fileHandler = (BinEdFileHandler) activeFile.get();
                        SectCodeArea codeArea = fileHandler.getCodeArea();
                        goToBookmark(codeArea, bookmarkIndex);
                    }
                }
            });
            inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_0 + i, metaMask), goToActionKey);
            inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_NUMPAD0 + i, metaMask), goToActionKey);

            String addActionKey = "add-bookmark-" + i;
            actionMap.put(addActionKey, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Optional<FileHandler> activeFile = editorProvider.getActiveFile();
                    if (activeFile.isPresent()) {
                        BinEdFileHandler fileHandler = (BinEdFileHandler) activeFile.get();
                        SectCodeArea codeArea = fileHandler.getCodeArea();
                        addBookmark(codeArea, bookmarkIndex);
                    }
                }
            });
            inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_0 + i, metaMask | KeyEvent.SHIFT_DOWN_MASK), addActionKey);
            inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_NUMPAD0 + i, metaMask | KeyEvent.SHIFT_DOWN_MASK), addActionKey);

            String clearActionKey = "clear-bookmark-" + i;
            actionMap.put(clearActionKey, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearBookmark(bookmarkIndex);
                }
            });
            inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_0 + i, metaMask | KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK), clearActionKey);
            inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_NUMPAD0 + i, metaMask | KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK), clearActionKey);
        }
        component.setActionMap(actionMap);
        component.setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, inputMap);
    }

    public void goToBookmark(SectCodeArea codeArea, int bookmarkIndex) {
        if (bookmarkRecords.size() > bookmarkIndex) {
            BookmarkRecord record = bookmarkRecords.get(bookmarkIndex);
            if (record.isEmpty()) {
                return;
            }

            codeArea.setActiveCaretPosition(record.getStartPosition());
            codeArea.centerOnCursor();
        }
    }

    public void addBookmark(SectCodeArea codeArea, int bookmarkIndex) {
        long position = codeArea.getDataPosition();

        if (bookmarkRecords.size() <= bookmarkIndex) {
            int recordsToInsert = bookmarkIndex - bookmarkRecords.size() + 1;
            for (int i = 0; i < recordsToInsert; i++) {
                bookmarkRecords.add(new BookmarkRecord());
            }
        }

        BookmarkRecord record = bookmarkRecords.get(bookmarkIndex);
        record.setStartPosition(position);
        record.setLength(1);
        saveBookmarkRecords();
        bookmarksPositionColorModifier.notifyBookmarksChanged();
        updateBookmarksMenu();
    }

    public void clearBookmark(int bookmarkIndex) {
        if (bookmarkRecords.size() > bookmarkIndex) {
            if (bookmarkRecords.size() == bookmarkIndex + 1) {
                bookmarkRecords.remove(bookmarkIndex);
            } else {
                bookmarkRecords.get(bookmarkIndex).setEmpty();
            }
            saveBookmarkRecords();
            bookmarksPositionColorModifier.notifyBookmarksChanged();
            updateBookmarksMenu();
        }
    }

    public void updateBookmarksMenu() {
        if (bookmarksMenu != null) {
            updateBookmarksMenu(bookmarksMenu);
        }
    }

    public void updateBookmarksMenu(JMenu menu) {
        menu.removeAll();

        ActionModuleApi actionModule = App.getModule(ActionModuleApi.class);
        int recordsLimit = Math.min(bookmarkRecords.size(), 10);
        int metaMask = ActionUtils.getMetaMask();
        String bookmarkActionName = resourceBundle.getString("bookmarkAction.text");
        String bookmarkActionDescription = resourceBundle.getString("bookmarkAction.shortDescription");
        for (int i = 0; i < recordsLimit; i++) {
            BookmarkRecord bookmarkRecord = bookmarkRecords.get(i);

            Action bookmarkAction = new AbstractAction(bookmarkActionName + " " + (i + 1)) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    long startPosition = bookmarkRecord.getStartPosition();
                    Optional<FileHandler> activeFile = editorProvider.getActiveFile();
                    if (activeFile.isPresent()) {
                        BinEdFileHandler fileHandler = (BinEdFileHandler) activeFile.get();
                        SectCodeArea codeArea = fileHandler.getCodeArea();
                        codeArea.setActiveCaretPosition(startPosition);
                        codeArea.centerOnCursor();
                    }
                }
            };
            bookmarkAction.putValue(Action.ACCELERATOR_KEY, javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_0 + i, metaMask));
            final Color bookmarkColor = bookmarkRecord.getColor();
            bookmarkAction.putValue(Action.SMALL_ICON, new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    g.setColor(bookmarkColor);
                    g.fillRect(x + 2, y + 2, 12, 12);
                    g.setColor(Color.BLACK);
                    g.drawRect(x + 2, y + 2, 12, 12);
                }

                @Override
                public int getIconWidth() {
                    return 16;
                }

                @Override
                public int getIconHeight() {
                    return 16;
                }
            });
            bookmarkAction.putValue(Action.SHORT_DESCRIPTION, bookmarkActionDescription);

            menu.add(actionModule.actionToMenuItem(bookmarkAction));
        }

        if (!bookmarkRecords.isEmpty()) {
            menu.addSeparator();
        }
        menu.add(actionModule.actionToMenuItem(manageBookmarksAction));
    }
}
