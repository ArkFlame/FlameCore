package com.arkflame.core.colorapi;

import net.md_5.bungee.api.chat.ClickEvent;

public enum ClickAction {
    RUN_COMMAND(ClickEvent.Action.RUN_COMMAND),
    SUGGEST_COMMAND(ClickEvent.Action.SUGGEST_COMMAND),
    OPEN_URL(ClickEvent.Action.OPEN_URL),
    COPY_TO_CLIPBOARD(ClickEvent.Action.COPY_TO_CLIPBOARD),
    CHANGE_PAGE(ClickEvent.Action.CHANGE_PAGE);

    private final ClickEvent.Action bungeeAction;

    ClickAction(ClickEvent.Action bungeeAction) {
        this.bungeeAction = bungeeAction;
    }

    public ClickEvent.Action toBungee() {
        return this.bungeeAction;
    }
}