package fr.insa.chatsystem.gui.view;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class MessengerView extends JPanel {

    private final ConversationListView conversationList = new ConversationListView();
    private final MessageListView messageList = new MessageListView();
    private final ComposerView composer = new ComposerView();

    private final JButton menuBtn = new JButton("⋯");
    private Runnable onDisconnect = () -> {};

    private Consumer<String> onChangePseudo = p -> {};

    private JDialog changePseudoDialog;
    private JTextField pseudoField;
    private JLabel pseudoError;

    public MessengerView() {
        setLayout(new BorderLayout());

        var left = new JScrollPane(conversationList);
        left.setPreferredSize(new Dimension(260, 0));
        left.setBorder(new MatteBorder(0, 0, 0, 1, new Color(0xD0D0D0)));
        left.setViewportBorder(null);

        var messagesScroll = new JScrollPane(messageList);
        messageList.setOwningScrollPane(messagesScroll);

        messagesScroll.setBorder(BorderFactory.createEmptyBorder());
        messagesScroll.setViewportBorder(BorderFactory.createEmptyBorder());
        messagesScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        messagesScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        messagesScroll.setOpaque(false);
        messagesScroll.getViewport().setOpaque(true);

        JScrollBar zero = new JScrollBar() {
            @Override public Dimension getPreferredSize() { return new Dimension(0, 0); }
        };
        zero.setOpaque(false);
        messagesScroll.setVerticalScrollBar(zero);
        messagesScroll.setHorizontalScrollBar(new JScrollBar() {
            @Override public Dimension getPreferredSize() { return new Dimension(0, 0); }
        });

        JPanel topBar = buildTopBar();

        var right = new JPanel(new BorderLayout());
        right.add(topBar, BorderLayout.NORTH);
        right.add(messagesScroll, BorderLayout.CENTER);
        right.add(composer, BorderLayout.SOUTH);
        right.setBackground(Color.WHITE);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);
    }

    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(true);
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(0xEEEEEE)));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        right.setOpaque(false);

        styleTopButton(menuBtn);
        right.add(menuBtn);

        topBar.add(right, BorderLayout.EAST);

        // Popup menu
        JPopupMenu menu = new JPopupMenu();

        JMenuItem changePseudo = new JMenuItem("Change pseudo…");
        changePseudo.addActionListener(e -> openChangePseudoDialog());
        menu.add(changePseudo);

        menu.addSeparator();

        JMenuItem disconnect = new JMenuItem("Disconnect");
        disconnect.addActionListener(e -> onDisconnect.run());
        menu.add(disconnect);

        menuBtn.addActionListener(e -> menu.show(menuBtn, 0, menuBtn.getHeight()));
        return topBar;
    }

    private void openChangePseudoDialog() {
        if (changePseudoDialog == null) {
            buildChangePseudoDialog();
        }

        pseudoError.setVisible(false);
        pseudoError.setText(" ");
        pseudoField.setText("");
        pseudoField.requestFocusInWindow();

        changePseudoDialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        changePseudoDialog.setVisible(true);
    }

    private void buildChangePseudoDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        changePseudoDialog = new JDialog(owner, "Change pseudo", Dialog.ModalityType.MODELESS);
        changePseudoDialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        root.setBackground(Color.WHITE);

        JLabel title = new JLabel("Choose a new pseudo");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        title.setForeground(new Color(0x1C1C1E));

        pseudoField = new JTextField(18);
        pseudoField.setFont(pseudoField.getFont().deriveFont(14f));

        // error label (iOS-ish)
        pseudoError = new JLabel(" ");
        pseudoError.setForeground(new Color(0xFF3B30)); // iOS red
        pseudoError.setFont(pseudoError.getFont().deriveFont(12.5f));
        pseudoError.setVisible(false);

        // buttons
        JButton cancel = new JButton("Cancel");
        JButton ok = new JButton("OK");

        cancel.addActionListener(e -> changePseudoDialog.setVisible(false));
        ok.addActionListener(e -> submitChangePseudo());

        // Enter = OK
        pseudoField.addActionListener(e -> submitChangePseudo());

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(title);
        center.add(Box.createVerticalStrut(10));
        center.add(pseudoField);
        center.add(Box.createVerticalStrut(6));
        center.add(pseudoError);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(ok);

        root.add(center, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);

        changePseudoDialog.setContentPane(root);
        changePseudoDialog.pack();
        changePseudoDialog.setResizable(false);
    }

    private void submitChangePseudo() {
        String p = pseudoField.getText();
        if (p == null) p = "";
        p = p.trim();

        if (p.isEmpty()) {
            showChangePseudoError("Pseudo required");
            return;
        }

        // ask core
        onChangePseudo.accept(p);
    }

    // called by presenter on GuiUpdate.PseudoChangeFailed
    public void showChangePseudoError(String message) {
        if (pseudoError == null) return;
        pseudoError.setText(message == null ? "Invalid pseudo" : message);
        pseudoError.setVisible(true);
        if (changePseudoDialog != null) changePseudoDialog.pack();
    }

    // called by presenter on GuiUpdate.PseudoChanged
    public void closeChangePseudoDialog() {
        if (changePseudoDialog != null) changePseudoDialog.setVisible(false);
    }

    private static void styleTopButton(JButton b) {
        b.setFocusable(false);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setMargin(new Insets(0, 8, 0, 8));
        b.setFont(b.getFont().deriveFont(Font.PLAIN, 18f));
        b.setForeground(new Color(0x3A3A3C));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(44, 28));
    }

    public void setOnDisconnect(Runnable r) {
        this.onDisconnect = Objects.requireNonNull(r, "onDisconnect");
    }

    public void setOnChangePseudo(Consumer<String> r) {
        this.onChangePseudo = Objects.requireNonNull(r, "onChangePseudo");
    }

    public ConversationListView conversationList() { return conversationList; }
    public MessageListView messageList() { return messageList; }
    public ComposerView composer() { return composer; }
}