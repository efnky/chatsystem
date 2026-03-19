package fr.insa.chatsystem.gui.view.theme;

import java.awt.*;

public interface Theme {

    Color appBackground();

    Color bubbleMineBg();
    Color bubbleOtherBg();
    Color bubbleMineText();
    Color bubbleOtherText();

    Color danger();           // "!"
    Color popupBg();
    Color popupBorder();

    Color overlayPillBg();
    Color overlayPillBorder();

    Color whisperTextFont();
    Color highlightColor();
    Color inputTextBg();

    int bubbleRadius();
    int popupRadius();

    float messageFontSize();
    float reactionEmojiFontSize();
    float reactionCountFontSize();
}