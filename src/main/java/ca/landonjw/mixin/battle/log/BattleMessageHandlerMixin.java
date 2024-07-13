package ca.landonjw.mixin.battle.log;

import ca.landonjw.ResizeableTextQueue;
import com.cobblemon.mod.common.api.net.ClientNetworkPacketHandler;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.net.battle.BattleMessageHandler;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMessagePacket;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BattleMessageHandler.class)
public abstract class BattleMessageHandlerMixin implements ClientNetworkPacketHandler<BattleMessagePacket> {

    /**
     * Cobblemon currently does its battle text width here, instead of letting the battle message GUI
     * handle it. We instead override this function to simply pass along the raw battle messages
     * and then let the battle message GUI determine how the text should render based on its width.
     */
    @Override
    public void handle(@NotNull BattleMessagePacket battleMessagePacket, @NotNull Minecraft minecraft) {
        var battle = CobblemonClient.INSTANCE.getBattle();
        if (battle == null) return;

        var messageQueue = (ResizeableTextQueue)(Object)battle.getMessages();
        messageQueue.cobblemon_ui_tweaks$add(battleMessagePacket.getMessages());
    }

}
