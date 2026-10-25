package com.riseofnations.client;

import com.riseofnations.RiseOfNationsMod;
import com.riseofnations.core.civ.CivColor;
import com.riseofnations.core.civ.CivError;
import com.riseofnations.core.civ.CivView;
import com.riseofnations.core.civ.CivWire;
import com.riseofnations.core.civ.DevelopmentStage;
import com.riseofnations.core.civ.EconomicStatus;
import com.riseofnations.core.civ.Government;
import com.riseofnations.net.RenameCivilizationPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Shows the player's civilization (all numbers come from the server) or, if they have none,
 * a button to found one. Also hosts the rename form. Every button here does something real.
 */
final class CivilizationScreen extends Screen {

    private CivView view;
    private CivError error;
    private String message = "";
    private boolean renaming = false;
    private boolean pending = false;
    private String renameDraft = "";

    CivilizationScreen(CivWire.StateMessage state) {
        super(Component.literal("Civilization"));
        this.view = state.view();
        this.error = state.error();
        if (error != CivError.NONE) message = error.message();
    }

    /** Called when the server sends fresh data while this screen is open. */
    void applyState(CivWire.StateMessage state) {
        this.view = state.view();
        this.error = state.error();
        this.pending = false;
        if (state.error() == CivError.NONE) {
            this.message = "";
            this.renaming = false;
            this.renameDraft = "";
        } else {
            this.message = state.error().message();
        }
        this.rebuildWidgets();
    }

    private boolean blocked() {
        return error == CivError.DATA_PROTECTED || error == CivError.SERVER_NOT_READY;
    }

    private int top() {
        return Math.max(8, this.height / 2 - 92);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = top();

        if (view == null) {
            if (!blocked()) {
                Button found = Button.builder(Component.literal("Found a Civilization"),
                                button -> this.minecraft.gui.setScreen(new CreateCivilizationScreen()))
                        .bounds(cx - 100, top + 70, 200, 20).build();
                found.setTooltip(Tooltip.create(Component.literal("Choose a name, leader, color and government.")));
                this.addRenderableWidget(found);
            }
            this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.onClose())
                    .bounds(cx - 100, top + 96, 200, 20).build());
            return;
        }

        if (renaming) {
            EditBox box = new EditBox(this.font, cx - 100, top + 58, 200, 20, Component.literal("New name"));
            box.setMaxLength(64);
            box.setHint(Component.literal("New civilization name"));
            box.setValue(renameDraft);
            box.setResponder(text -> renameDraft = text);
            this.addRenderableWidget(box);
            this.setInitialFocus(box);

            Button apply = Button.builder(Component.literal("Apply"), button -> sendRename())
                    .bounds(cx - 100, top + 86, 96, 20).build();
            apply.active = !pending;
            this.addRenderableWidget(apply);
            this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> {
                renaming = false;
                message = "";
                this.rebuildWidgets();
            }).bounds(cx + 4, top + 86, 96, 20).build());
            return;
        }

        Button rename = Button.builder(Component.literal("Rename"), button -> {
            renaming = true;
            message = "";
            renameDraft = view.name();
            this.rebuildWidgets();
        }).bounds(cx - 100, top + 164, 96, 20).build();
        rename.setTooltip(Tooltip.create(Component.literal("Change your civilization's name. Names must be unique.")));
        this.addRenderableWidget(rename);
        this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.onClose())
                .bounds(cx + 4, top + 164, 96, 20).build());
    }

    private void sendRename() {
        pending = true;
        message = "";
        try {
            ClientPlayNetworking.send(new RenameCivilizationPayload(CivWire.encodeRename(renameDraft)));
        } catch (RuntimeException e) {
            RiseOfNationsMod.LOGGER.warn("Could not send the rename request: {}", e.getMessage());
            pending = false;
            message = "Could not reach the server. Is the mod installed on it?";
        }
        this.rebuildWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int top = top();
        int left = cx - 100;

        if (view == null) {
            ScreenText.centered(graphics, this.font, "Civilization", cx, top, ScreenText.WHITE);
            ScreenText.centered(graphics, this.font, "You have not founded a civilization yet.", cx, top + 30, ScreenText.WHITE);
            if (!blocked()) {
                ScreenText.centered(graphics, this.font, "Found one to start your story.", cx, top + 44, ScreenText.GRAY);
            }
            drawMessage(graphics, cx, top + 126);
            return;
        }

        CivColor color = CivColor.fromId(view.colorId());
        int nameColor = 0xFF000000 | (color == null ? 0xFFFFFF : color.rgb());
        ScreenText.centered(graphics, this.font, view.name(), cx, top, nameColor);
        ScreenText.centered(graphics, this.font, "Led by " + view.leaderName(), cx, top + 13, ScreenText.WHITE);

        if (renaming) {
            ScreenText.centered(graphics, this.font, "Rename your civilization", cx, top + 42, ScreenText.GOLD);
            drawMessage(graphics, cx, top + 114);
            return;
        }

        Government government = Government.fromId(view.governmentId());
        DevelopmentStage stage = DevelopmentStage.fromIdOrDefault(view.stageId());
        EconomicStatus economy = EconomicStatus.fromIdOrDefault(view.economyId());
        int y = top + 36;
        ScreenText.left(graphics, this.font, "Government: " + (government == null ? view.governmentId() : government.displayName()), left, y, ScreenText.WHITE);
        ScreenText.left(graphics, this.font, "Treasury: " + number(view.treasury()) + " coins", left, y + 13, ScreenText.WHITE);
        ScreenText.left(graphics, this.font, "Population: " + number(view.population()) + " (simulated)", left, y + 26, ScreenText.WHITE);
        ScreenText.left(graphics, this.font, "Settlements: " + view.settlements(), left, y + 39, ScreenText.WHITE);
        ScreenText.left(graphics, this.font, "Development: " + stage.displayName(), left, y + 52, ScreenText.WHITE);
        ScreenText.left(graphics, this.font, "Economy: " + economy.displayName(), left, y + 65, ScreenText.WHITE);

        ScreenText.centered(graphics, this.font, "Population and settlements are simulated numbers,", cx, y + 86, ScreenText.GRAY);
        ScreenText.centered(graphics, this.font, "not entities placed in the world.", cx, y + 98, ScreenText.GRAY);
        ScreenText.centered(graphics, this.font, "Coming later: trade, diplomacy, armies.", cx, y + 114, ScreenText.GRAY);
        drawMessage(graphics, cx, top + 150);
    }

    private void drawMessage(GuiGraphicsExtractor graphics, int cx, int y) {
        if (pending) {
            ScreenText.centered(graphics, this.font, "Waiting for the server...", cx, y, ScreenText.GRAY);
        } else if (!message.isEmpty()) {
            ScreenText.centered(graphics, this.font, message, cx, y, ScreenText.RED);
        }
    }

    private static String number(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(null);
    }
}
