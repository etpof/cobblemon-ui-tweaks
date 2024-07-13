package ca.landonjw.mixin.battle.log;

import ca.landonjw.BattleLogRenderer;
import ca.landonjw.GUIHandler;
import ca.landonjw.ResizeableTextQueue;
import com.cobblemon.mod.common.client.CobblemonResources;
import com.cobblemon.mod.common.client.battle.ClientBattleMessageQueue;
import com.cobblemon.mod.common.client.gui.battle.widgets.BattleMessagePane;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(BattleMessagePane.class)
public abstract class BattleMessagePaneMixin extends AbstractSelectionList {

    @Shadow(remap = false) @Final public static int TEXT_BOX_WIDTH;
    @Shadow(remap = false) @Final public static int TEXT_BOX_HEIGHT;
    @Shadow(remap = false) protected abstract void correctSize();
    @Shadow(remap = false) private float opacity;
    @Shadow(remap = false) public abstract double getScrollAmount();
    @Shadow(remap = false) protected abstract int addEntry(@NotNull BattleMessagePane.BattleMessageLine entry);
    @Shadow(remap = false) protected abstract void updateScrollingState(double mouseX, double mouseY);
    @Shadow(remap = false) private boolean scrolling;

    @Unique private final List<Component> battleMessages = new ArrayList<>();

    public BattleMessagePaneMixin(Minecraft minecraft, int i, int j, int k, int l, int m) {
        super(minecraft, i, j, k, l, m);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/cobblemon/mod/common/client/battle/ClientBattleMessageQueue;subscribe(Lkotlin/jvm/functions/Function1;)V"), remap = false)
    private void cobblemon_ui_tweaks$init(ClientBattleMessageQueue instance, Function1<? super FormattedCharSequence, Unit> $i$f$forEach) {
        // When UI is first opened or becomes opaque, we set the scroll to the bottom of the message log.
        setScrollAmount(getMaxScroll());
        var queueWithBattleMessages = (ResizeableTextQueue)(Object)instance;
        // Any time a new battle message comes, we add it to the message list and determine how many lines should render (based on width).
        queueWithBattleMessages.cobblemon_ui_tweaks$subscribe(text -> {
            battleMessages.add(text);
            cobblemon_ui_tweaks$correctBattleText();
        });
    }

    /**
     * Updates the width based on our potentially stretched width and height.
     * Cobblemon would otherwise still try to set the GUI back to it's expected dimensions.
     */
    @Inject(method = "correctSize", at = @At("HEAD"), remap = false, cancellable = true)
    private void cobblemon_ui_tweaks$correctSize(CallbackInfo ci) {
        updateSize(getWidthOverride(), getHeightOverride(), (int)Math.round(getY() + 6), (int)Math.round(getY() + 6 + getHeightOverride()));
        setLeftPos((int)Math.round(getX()));
        ci.cancel();
    }

    /**
     * This will scale the battle text according to the current width of the message log.
     * This will additionally alter the scroll state to either auto-scroll if scrollbar is
     * at bottom of list, or stay where it was prior in the list.
     *
     * TODO: This is currently invoking every render to resolve scrolling issues. Can this be redesigned to prevent that?
     */
    @Unique
    private void cobblemon_ui_tweaks$correctBattleText() {
        var isFullyScrolled = getMaxScroll() - getScrollAmount() < 10;
        this.clearEntries();
        var textRenderer = Minecraft.getInstance().font;
        for (var message : battleMessages) {
            var line = message.copy().setStyle(message.getStyle().withBold(true).withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()));
            var wrappedLines = textRenderer.getSplitter().splitLines(line, getWidthOverride() - 11, line.getStyle());
            var lines = Language.getInstance().getVisualOrder(wrappedLines);
            for (var finalLine : lines) {
                this.addEntry(new BattleMessagePane.BattleMessageLine((BattleMessagePane)(Object)this, finalLine));
            }
        }
        if (isFullyScrolled) {
            setScrollAmount(getMaxScroll());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Alters the scroll amount if user is clicking on the scroll bar.
        updateScrollingState(mouseX, mouseY);
        if (scrolling) {
            setFocused(getEntryAtPosition(mouseX, mouseY));
            setDragging(true);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Unique
    private int getWidthOverride() {
        return GUIHandler.INSTANCE.getBattleLogWidth();
    }

    @Unique
    private void setWidthOverride(int width) {
        GUIHandler.INSTANCE.setBattleLogWidth(width);
    }

    @Unique
    private int getHeightOverride() {
        return GUIHandler.INSTANCE.getBattleLogHeight();
    }

    @Unique
    private void setHeightOverride(int height) {
        GUIHandler.INSTANCE.setBattleLogHeight(height);
    }

    @Unique
    private int getFrameWidth() {
        return getWidthOverride() + 16;
    }

    @Unique
    private int getFrameHeight() {
        return getHeightOverride() + 9;
    }

    @Unique
    private double getX() {
        return GUIHandler.INSTANCE.getBattleLogX();
    }

    @Unique
    private void setX(double value) {
        GUIHandler.INSTANCE.setBattleLogX(value);
    }

    @Unique
    private double getY() {
        return GUIHandler.INSTANCE.getBattleLogY();
    }

    @Unique
    private void setY(double value) {
        GUIHandler.INSTANCE.setBattleLogY(value);
    }

    /**
     * This will either move the message log or rescale it, if the player is within the bounds of those actions:
     * Moving: Top bar of the message log.
     * Rescaling: Bottom right corner of the message log.
     */
    @Inject(method = "mouseDragged", at = @At("TAIL"))
    private void cobblemon_ui_tweaks$mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (tryMove(mouseX, mouseY, deltaX, deltaY)) return;
        tryAdjustWidth(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Unique
    private boolean tryMove(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseY - deltaY < y0 - 20 || mouseY - deltaY > y0 + 20) return false;
        if (mouseX - deltaX < this.x0 - 10 || mouseX - deltaX > this.x1 + 10) return false;
        setX(getX() + deltaX);
        setY(getY() + deltaY);
        return true;
    }

    @Unique
    private boolean tryAdjustWidth(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 1) return false;
        var expandButtonX1 = getFrameWidth() - 9;
        var expandButtonX2 = getFrameWidth() - 4;
        var expandButtonY1 = getFrameHeight() - 9;
        var expandButtonY2 = getFrameHeight() - 4;
        if (mouseX - deltaX < this.x0 + expandButtonX1 - 10 || mouseX - deltaX > this.x0 + expandButtonX2 + 10) return false;
        if (mouseY - deltaY < this.y0 + expandButtonY1 - 10 || mouseY - deltaY > this.y0 + expandButtonY2 + 10) return false;
        var newHeight = Math.max(mouseY - this.y0, TEXT_BOX_HEIGHT);
        var newWidth = Math.max(mouseX - this.x0, TEXT_BOX_WIDTH);
        setHeightOverride((int)newHeight);
        setWidthOverride((int)newWidth - 12);
        correctSize();
        cobblemon_ui_tweaks$correctBattleText();
        return true;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cobblemon_ui_tweaks$render(GuiGraphics context, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        correctSize();

        var isFullyScrolled = opacity != 1 || getMaxScroll() - getScrollAmount() < 2;
        if (isFullyScrolled) {
            setScrollAmount(getMaxScroll());
        }

        BattleLogRenderer.INSTANCE.render(context, (int)Math.round(getX()), (int)Math.round(getY()), getFrameHeight(), getFrameWidth(), opacity);

        context.enableScissor(
                x0 + 5,
                (int)Math.round(getY() + 6),
                x0 + 5 + getWidthOverride(),
                (int)Math.round(getY() + 6 + getHeightOverride())
        );
        super.render(context, mouseX, mouseY, partialTicks);
        context.disableScissor();
        ci.cancel();
    }

    @Override
    public int getRowWidth() {
        return getWidthOverride();
    }

    @Override
    public int getRowLeft() {
        return x0 + 40;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.x0 + getWidthOverride();
    }

}
