package com.patricerose.smartstorage.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.patricerose.smartstorage.ModNetwork;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

/** The terminal screen: Minecraft's big-chest screen plus a search box in the top-right corner. */
public class TerminalScreen extends ContainerScreen {
    private EditBox searchBox;
    private String searchText = "";

    public TerminalScreen(ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        searchBox = new EditBox(this.font, this.leftPos + 80, this.topPos + 4, 89, 11,
                Component.translatable("gui.smartstorage.search"));
        searchBox.setMaxLength(50);
        searchBox.setValue(searchText);
        searchBox.setResponder(text -> {
            searchText = text;
            ModNetwork.sendSearch(text);
        });
        addRenderableWidget(searchBox);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (searchBox != null && searchBox.isFocused()) {
            if (event.key() == InputConstants.KEY_ESCAPE) {
                searchBox.setFocused(false); // first Escape leaves the search box, second one closes
                return true;
            }
            searchBox.keyPressed(event);
            return true; // typing in the box shouldn't close the screen or move items
        }
        return super.keyPressed(event);
    }
}
