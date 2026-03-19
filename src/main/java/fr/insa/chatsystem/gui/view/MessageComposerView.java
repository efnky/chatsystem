package fr.insa.chatsystem.gui.view;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

public final class MessageComposerView extends JPanel {

    @FunctionalInterface public interface SendTextHandler { void onSend(String text); }
    @FunctionalInterface public interface SendImageHandler { void onPickImage(); }

    private SendTextHandler onSend = t -> {};
    private SendImageHandler onPickImage = () -> {};

    private final JTextArea input = new JTextArea(2, 20);
    private final JScrollPane inputScroll = new JScrollPane(input);

    private final JButton plus = new JButton("+");
    private final JButton send = new JButton("↑"); // iMessage-ish

    public MessageComposerView() {
        setOpaque(false);
        setLayout(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));

        // input
        input.setLineWrap(true);
        input.setWrapStyleWord(true);

        inputScroll.setBorder(BorderFactory.createEmptyBorder());
        inputScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        inputScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // buttons
        plus.setFocusable(false);
        send.setFocusable(false);

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(plus, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(send, BorderLayout.CENTER);

        add(left, BorderLayout.WEST);
        add(inputScroll, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        // enable/disable send button
        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshSendEnabled(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshSendEnabled(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshSendEnabled(); }
        });
        refreshSendEnabled();

        // click actions
        plus.addActionListener(e -> onPickImage.onPickImage());
        send.addActionListener(e -> sendNow());

        // keyboard shortcuts:
        // Enter => send
        // Shift+Enter => newline
        installKeyBindings();
    }

    public void setOnSend(SendTextHandler h) { this.onSend = Objects.requireNonNull(h); }
    public void setOnPickImage(SendImageHandler h) { this.onPickImage = Objects.requireNonNull(h); }

    public void requestInputFocus() { input.requestFocusInWindow(); }

    private void refreshSendEnabled() {
        String t = input.getText();
        send.setEnabled(t != null && !t.trim().isEmpty());
    }

    private void sendNow() {
        String t = input.getText();
        if (t == null) t = "";
        t = t.trim();
        if (t.isEmpty()) return;

        onSend.onSend(t);

        input.setText("");
        refreshSendEnabled();
        requestInputFocus();
    }

    private void installKeyBindings() {
        InputMap im = input.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = input.getActionMap();

        // Enter => send
        im.put(KeyStroke.getKeyStroke("ENTER"), "sendMessage");
        am.put("sendMessage", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { sendNow(); }
        });

        // Shift+Enter => newline (default insert-break)
        im.put(KeyStroke.getKeyStroke("shift ENTER"), "insert-break");
    }
}