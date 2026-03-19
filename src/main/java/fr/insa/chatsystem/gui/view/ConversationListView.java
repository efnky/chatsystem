package fr.insa.chatsystem.gui.view;

import fr.insa.chatsystem.gui.api.ConversationSummaryUI;
import fr.insa.chatsystem.gui.view.theme.Theme;
import fr.insa.chatsystem.gui.view.theme.ThemeManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ConversationListView extends JPanel {

    private Consumer<String> onSelect = id -> {};
    private String filter = "";
    private Theme th = ThemeManager.get();

    private final JTextField search = new JTextField();

    // Keep last data so search can re-render the list
    private List<ConversationSummaryUI> lastConversations = List.of();
    private String lastSelectedIdOrNull = null;

    // prevents placeholder changes from triggering filtering logic
    private boolean suppressDocEvents = false;

    private Function<String, Boolean> lastOnlineFn = id -> false;

    public ConversationListView() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(Color.WHITE);

        // ---- iMessage-like search bar ----
        SearchBarView searchBar = new SearchBarView(search, "Recherche");

        // Re-render list when search changes
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onSearchChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { onSearchChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onSearchChanged(); }
        });

        // ESC: exit search (lose focus), KEEP current filter (do NOT recompute)
        installEscapeToExitSearch(search);

        // Track focus to apply filter only when search is selected
        search.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                // When user enters search, recompute with current text (if any)
                onSearchChanged();
            }

            @Override public void focusLost(java.awt.event.FocusEvent e) {
                // When leaving search, disable filtering (show all)
                // BUT: do not touch "filter" here if you want to keep the last active filter while unfocused.
                // You asked: filter applies only when selected => so we clear it on focus lost.
                filter = "";
                render(lastConversations, lastSelectedIdOrNull, lastOnlineFn);

                // If you also want placeholder back visually, do it without triggering doc events
                if (search.getText().trim().isEmpty()) {
                    suppressDocEvents = true;
                    SearchBarView.forcePlaceholder(search, "Recherche");
                    suppressDocEvents = false;
                }
            }
        });

        add(searchBar, BorderLayout.NORTH);
    }

    public void setOnSelect(Consumer<String> onSelect) {
        this.onSelect = onSelect;
    }

    public void render(List<ConversationSummaryUI> conversations, String selectedIdOrNull,
                       Function<String, Boolean> isOnlineByConversationId) {
        this.lastOnlineFn = (isOnlineByConversationId == null) ? (id -> false) : isOnlineByConversationId;
        this.lastConversations = (conversations == null) ? List.of() : conversations;
        this.lastSelectedIdOrNull = selectedIdOrNull;

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(true);
        listPanel.setBackground(Color.WHITE);

        for (var c : lastConversations) {
            if (!matchesFilter(c)) continue;

            boolean selected = lastSelectedIdOrNull != null && lastSelectedIdOrNull.equals(c.conversationId());
            boolean online = isOnlineByConversationId != null && Boolean.TRUE.equals(isOnlineByConversationId.apply(c.conversationId()));

            ConversationRowView row = new ConversationRowView(c, selected, online);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            row.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    onSelect.accept(c.conversationId());
                }
            });

            listPanel.add(row);
            JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
            sep.setMinimumSize(new Dimension(1, 1));
            sep.setPreferredSize(new Dimension(1, 1));
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            listPanel.add(sep);
        }

        listPanel.add(Box.createVerticalGlue());

        if (getComponentCount() > 1) remove(1);
        add(listPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }


    private void onSearchChanged() {
        if (suppressDocEvents) return;

        // Filter applies ONLY when the field is selected (has focus)
        if (!search.isFocusOwner()) return;

        boolean isPlaceholder = SearchBarView.isPlaceholder(search);

        String raw = search.getText();
        if (raw == null) raw = "";
        raw = raw.trim();

        // Filter applies ONLY if not empty and not placeholder
        if (isPlaceholder || raw.isEmpty()) {
            filter = "";
        } else {
            filter = raw.toLowerCase(Locale.ROOT);
        }

        render(lastConversations, lastSelectedIdOrNull, lastOnlineFn);
    }

    private boolean matchesFilter(ConversationSummaryUI c) {
        if (filter.isEmpty()) return true;

        String t = safeLower(c.title());
        String p = safeLower(c.lastPreview());

        return t.contains(filter) || p.contains(filter);
    }

    private static String safeLower(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT);
    }

    private static void installEscapeToExitSearch(JTextField field) {
        InputMap im = field.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = field.getActionMap();

        im.put(KeyStroke.getKeyStroke("ESCAPE"), "exitSearch");
        am.put("exitSearch", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                field.getCaret().setVisible(false);
                field.getCaret().setSelectionVisible(false);
                field.transferFocus();
            }
        });
    }

    /**
     * Rounded search bar container with icon + placeholder.
     */
    private static final class SearchBarView extends JPanel {

        private final JTextField field;
        private final String placeholder;

        private Theme th = ThemeManager.get();

        private static final Color PLACEHOLDER_COLOR = new Color(0x8E8E93);
        private static final Color TEXT_COLOR = new Color(0x3A3A3C);

        SearchBarView(JTextField field, String placeholder) {
            this.field = field;
            this.placeholder = placeholder;

            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            RoundedBubblePanel pill = new RoundedBubblePanel(th.inputTextBg(), 22);
            pill.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            pill.setLayout(new BorderLayout(8, 0));

            JLabel icon = new JLabel("\uD83D\uDD0D");
            icon.setForeground(PLACEHOLDER_COLOR);
            icon.setFont(icon.getFont().deriveFont(14f));

            field.setBorder(BorderFactory.createEmptyBorder());
            field.setOpaque(false);
            field.setBackground(new Color(0, 0, 0, 0));
            field.setFont(field.getFont().deriveFont(14f));

            forcePlaceholder(field, placeholder);

            field.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent e) {
                    if (isPlaceholder(field)) {
                        field.setText("");
                        field.setForeground(TEXT_COLOR);
                    }
                    field.getCaret().setVisible(true);
                }

                @Override public void focusLost(java.awt.event.FocusEvent e) {
                    if (field.getText().trim().isEmpty()) {
                        forcePlaceholder(field, placeholder);
                    }
                }
            });

            pill.add(icon, BorderLayout.WEST);
            pill.add(field, BorderLayout.CENTER);

            add(pill, BorderLayout.CENTER);
        }

        static void forcePlaceholder(JTextField field, String placeholder) {
            field.setText(placeholder);
            field.setForeground(PLACEHOLDER_COLOR);
        }

        static boolean isPlaceholder(JTextField field) {
            return PLACEHOLDER_COLOR.equals(field.getForeground());
        }
    }
}