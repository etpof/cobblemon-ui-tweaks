package ca.landonjw.mixin;

import ca.landonjw.BattleLogRenderer;
import ca.landonjw.CobblemonUITweaks;
import ca.landonjw.GUIHandler;
import ca.landonjw.ResizeableTextQueue;
import com.cobblemon.mod.common.api.gui.GuiUtilsKt;
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
import net.minecraft.resources.ResourceLocation;
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
    @Shadow(remap = false) private static boolean expanded;
    @Shadow(remap = false) @Final public static int TEXT_BOX_HEIGHT;

    @Shadow(remap = false) protected abstract void correctSize();

    @Shadow(remap = false) private float opacity;

    @Shadow(remap = false) public abstract double getScrollAmount();

    @Shadow(remap = false) protected abstract int addEntry(@NotNull BattleMessagePane.BattleMessageLine entry);

    @Shadow(remap = false) protected abstract void updateScrollingState(double mouseX, double mouseY);

    @Shadow(remap = false) private boolean scrolling;
    @Shadow(remap = false) @Final public static int EXPAND_TOGGLE_SIZE;
    @Unique private final List<Component> battleMessages = new ArrayList<>();

    public BattleMessagePaneMixin(Minecraft minecraft, int i, int j, int k, int l, int m) {
        super(minecraft, i, j, k, l, m);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/cobblemon/mod/common/client/battle/ClientBattleMessageQueue;subscribe(Lkotlin/jvm/functions/Function1;)V"), remap = false)
    private void cobblemon_ui_tweaks$init(ClientBattleMessageQueue instance, Function1<? super FormattedCharSequence, Unit> $i$f$forEach) {
        var queueWithBattleMessages = (ResizeableTextQueue)(Object)instance;
        queueWithBattleMessages.cobblemon_ui_tweaks$subscribe(text -> {
            battleMessages.add(text);
            var isFullyScrolled = getMaxScroll() - getScrollAmount() < 10;
            if (isFullyScrolled) {
                setScrollAmount(getMaxScroll());
            }
            cobblemon_ui_tweaks$correctBattleText();
        });
    }

    @Inject(method = "correctSize", at = @At("HEAD"), remap = false, cancellable = true)
    private void cobblemon_ui_tweaks$correctSize(CallbackInfo ci) {
        System.out.println(getHeightOverride() + ", " + (int)Math.round(getY() + 6) + ", " + (int)Math.round(getY() + 6 + getHeightOverride()));
        updateSize(getWidthOverride(), getHeightOverride(), (int)Math.round(getY() + 6), (int)Math.round(getY() + 6 + getHeightOverride()));
        setLeftPos((int)Math.round(getX()));
        ci.cancel();
    }

    @Unique
    private void cobblemon_ui_tweaks$correctBattleText() {
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
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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

    private double getX() {
        return GUIHandler.INSTANCE.getBattleLogX();
    }

    private void setX(double value) {
        GUIHandler.INSTANCE.setBattleLogX(value);
    }

    private double getY() {
        return GUIHandler.INSTANCE.getBattleLogY();
    }

    private void setY(double value) {
        GUIHandler.INSTANCE.setBattleLogY(value);
    }

    @Inject(method = "mouseDragged", at = @At("TAIL"))
    private void cobblemon_ui_tweaks$mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (tryMove(mouseX, mouseY, deltaX, deltaY)) return;
        tryAdjustWidth(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Unique
    private boolean tryMove(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseY - deltaY < y0 - 20 || mouseY - deltaY > y0 + 20) return false;
        if (mouseX - deltaX < this.x0 - 10 || mouseX - deltaX > this.x1 + 10) return false;
//        var xDiff = mouseX - this.x0;
//        var yDiff = mouseY - this.y0;
//        if (lastMouseX != 0) {
//            x = x - (int)(mouseX - lastMouseX);
//        }
//        else {
//            x = x + (int)Math.round(deltaX);
//        }
//        if (lastMouseY != 0) {
//            y = y - (int)(mouseY - lastMouseY);
//        }
//        else {
//            y = y + (int)Math.round(deltaY);
//        }
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
        cobblemon_ui_tweaks$correctBattleText();
        correctSize();
        return true;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cobblemon_ui_tweaks$render(GuiGraphics context, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        correctSize();

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