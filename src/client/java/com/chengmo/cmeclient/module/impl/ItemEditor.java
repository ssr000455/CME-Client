package com.chengmo.cmeclient.module.impl;

import com.chengmo.cmeclient.gui.ItemEditorScreen;
import com.chengmo.cmeclient.module.Module;

public class ItemEditor extends Module {
    public ItemEditor() {
        super("ItemEditor", Category.SINGLEPLAYER);
    }

    @Override
    public void onDisable() {
        if (ItemEditorScreen.isOpen) ItemEditorScreen.close();
    }
}
