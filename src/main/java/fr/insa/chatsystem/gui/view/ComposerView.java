package fr.insa.chatsystem.gui.view;

import fr.insa.chatsystem.gui.view.theme.Theme;
import fr.insa.chatsystem.gui.view.theme.ThemeManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.function.Consumer;

public final class ComposerView extends JPanel {

    private final JTextArea input = new JTextArea(1, 20);
    private final JScrollPane inputScroll = new JScrollPane(input);

    private final JButton attach = new JButton("+");
    private final JButton send = new JButton("↑");

    private Consumer<String> onSend = t -> {};
    private Consumer<String> onAttach = fileRef -> {};

    private Theme th = ThemeManager.get();

    public ComposerView() {
        setOpaque(false);
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));

        int pillH = 44;
        int sideW = 66;

        Color pillBg = th.inputTextBg();
        int radius = 24;

        // Left pill (+)
        RoundedBubblePanel leftPill = new RoundedBubblePanel(pillBg, radius);
        leftPill.setOpaque(false);
        leftPill.setLayout(new BorderLayout());
        leftPill.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        leftPill.setPreferredSize(new Dimension(sideW, pillH));
        stylePillButton(attach);
        leftPill.add(attach, BorderLayout.CENTER);

        // Middle pill
        RoundedBubblePanel middlePill = new RoundedBubblePanel(pillBg, radius);
        middlePill.setOpaque(false);
        middlePill.setLayout(new BorderLayout());
        // IMPORTANT: keep pill padding here (horizontal), not vertical
        middlePill.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        middlePill.setPreferredSize(new Dimension(10, pillH));

        // Input config
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setOpaque(false);
        input.setBorder(BorderFactory.createEmptyBorder());
        input.setMargin(new Insets(0, 0, 0, 0)); // avoid LAF margins
        input.setFont(input.getFont().deriveFont(Font.PLAIN, 16f));

        // Scroll config (invisible)
        inputScroll.setBorder(BorderFactory.createEmptyBorder());
        inputScroll.setViewportBorder(BorderFactory.createEmptyBorder());
        inputScroll.setOpaque(false);
        inputScroll.getViewport().setOpaque(false);
        inputScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        inputScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        inputScroll.setVerticalScrollBar(new JScrollBar() {
            @Override public Dimension getPreferredSize() { return new Dimension(0, 0); }
        });

        middlePill.add(inputScroll, BorderLayout.CENTER);

        // Right pill (send)
        RoundedBubblePanel rightPill = new RoundedBubblePanel(pillBg, radius);
        rightPill.setOpaque(false);
        rightPill.setLayout(new BorderLayout());
        rightPill.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        rightPill.setPreferredSize(new Dimension(sideW, pillH));
        stylePillButton(send);
        rightPill.add(send, BorderLayout.CENTER);

        // Layout 3 pills
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;

        gc.gridx = 0;
        gc.insets = new Insets(0, 0, 0, 10);
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        add(leftPill, gc);

        gc.gridx = 1;
        gc.insets = new Insets(0, 0, 0, 0);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        add(middlePill, gc);

        gc.gridx = 2;
        gc.insets = new Insets(0, 10, 0, 0);
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        add(rightPill, gc);

        // Auto vertical centering of first line (after layout + on resize)
        middlePill.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) { recenterFirstLine(); }
        });
        SwingUtilities.invokeLater(this::recenterFirstLine);

        // Enable/disable send button live
        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshSendEnabled(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshSendEnabled(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshSendEnabled(); }
        });
        refreshSendEnabled();

        // Actions
        send.addActionListener(e -> sendNow());

        attach.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            int res = chooser.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                if (f != null) onAttach.accept(f.getAbsolutePath());
            }
        });

        // Key bindings
        installKeyBindings();
    }

    /**
     * Centers the first line vertically inside the visible area.
     * Uses real font metrics (ascent/descent) + actual viewport height.
     */
    private void recenterFirstLine() {
        if (inputScroll.getViewport() == null) return;

        int h = inputScroll.getViewport().getHeight();
        if (h <= 0) return;

        FontMetrics fm = input.getFontMetrics(input.getFont());
        int lineH = fm.getAscent() + fm.getDescent();

        int pad = Math.max(0, (h - lineH) / 2);
        // a tiny tweak looks more iMessage-ish (cursor feels centered)
        int top = Math.max(0, pad - 1);
        int bottom = Math.max(0, pad);

        input.setBorder(BorderFactory.createEmptyBorder(top, 0, bottom, 0));
        input.revalidate();
        input.repaint();
    }

    private void refreshSendEnabled() {
        String t = input.getText();
        send.setEnabled(t != null && !t.trim().isEmpty());
    }

    private void sendNow() {
        String t = input.getText();
        if (t == null) t = "";
        t = t.trim();
        if (t.isEmpty()) return;

        onSend.accept(t);

        input.setText("");
        refreshSendEnabled();
        SwingUtilities.invokeLater(() -> input.requestFocusInWindow());
    }

    private void installKeyBindings() {
        InputMap im = input.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = input.getActionMap();

        im.put(KeyStroke.getKeyStroke("ENTER"), "sendMessage");
        am.put("sendMessage", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { sendNow(); }
        });

        im.put(KeyStroke.getKeyStroke("shift ENTER"), "insert-break");
    }

    private static void stylePillButton(JButton b) {
        b.setFocusable(false);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setHorizontalAlignment(SwingConstants.CENTER);
        b.setVerticalAlignment(SwingConstants.CENTER);
        b.setFont(b.getFont().deriveFont(Font.PLAIN, 18f));
    }

    public void setOnSend(Consumer<String> onSend) { this.onSend = onSend; }
    public void setOnAttach(Consumer<String> onAttach) { this.onAttach = onAttach; }

    public void setEnabledForConversation(boolean enabled) {
        input.setEnabled(enabled);
        attach.setEnabled(enabled);
        if (!enabled) input.setText("");
        refreshSendEnabled();
        SwingUtilities.invokeLater(this::recenterFirstLine);
    }
}