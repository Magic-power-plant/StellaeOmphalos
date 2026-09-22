package com.mpp.stellaeomphalos.client.codex;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Immutable layout products preserve component styles across rendering frames. */
public record CodexTextLayout(List<FormattedCharSequence> lines) {
    public CodexTextLayout {
        lines = List.copyOf(lines);
    }

    public static CodexTextLayout layout(Font font, Component text, int width) {
        return new CodexTextLayout(font.split(text, Math.max(1, width)));
    }
}
