package ca.landonjw.mixin.battle.log;

import ca.landonjw.ResizeableTextQueue;
import com.cobblemon.mod.common.client.battle.ClientBattleMessageQueue;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

@Mixin(ClientBattleMessageQueue.class)
public class ClientBattleMessageQueueMixin implements ResizeableTextQueue {

    @Unique private final List<Consumer<Component>> battleMessageListeners = new ArrayList<>();
    @Unique private final List<Component> battleMessages = new ArrayList<>();

    @Override
    public void cobblemon_ui_tweaks$subscribe(Consumer<Component> listener) {
        this.battleMessageListeners.add(listener);
        this.battleMessages.forEach(listener);
    }

    @Override
    public void cobblemon_ui_tweaks$add(Collection<Component> messages) {
        this.battleMessages.addAll(messages);
        this.battleMessageListeners.forEach(messages::forEach);
    }

}
