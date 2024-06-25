package ca.landonjw.mixin;

import ca.landonjw.GUIHandler;
import com.cobblemon.mod.common.client.gui.pc.BoxStorageSlot;
import com.cobblemon.mod.common.client.gui.pc.PartyStorageSlot;
import com.cobblemon.mod.common.client.gui.pc.StorageSlot;
import com.cobblemon.mod.common.client.gui.pc.StorageWidget;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(StorageWidget.class)
public class StorageWidgetMixin {

    @Final @Shadow(remap = false) private ArrayList<BoxStorageSlot> boxSlots;
    @Final @Shadow(remap = false) private ArrayList<PartyStorageSlot> partySlots;

    @Inject(method = "renderWidget", at = @At("TAIL"))
    public void cobblemon_ui_tweaks$renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        GUIHandler.INSTANCE.setHoveredPokemon(null);
        this.boxSlots.forEach(slot -> tryGetHoveredPokemon(slot, mouseX, mouseY, "pc"));
        this.partySlots.forEach(slot -> tryGetHoveredPokemon(slot, mouseX, mouseY, "party"));
    }

    @Unique
    private void tryGetHoveredPokemon(StorageSlot slot, int mouseX, int mouseY, String slotType) {
        if (slot.isHovered(mouseX, mouseY) && slot.getPokemon() != null) {
            GUIHandler.INSTANCE.setHoveredPokemon(slot.getPokemon());
            GUIHandler.INSTANCE.setHoveredPokemonType(slotType);
        }
    }

}
