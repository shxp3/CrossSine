package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.event.MovementInputEvent;
import net.ccbluex.liquidbounce.event.PostPlayerInputEvent;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Re-implements {@code MovementInputFromOptions.updatePlayerMoveState} to
 * match Raven's dual-event model:
 * <p>
 * 1. Raw WASD / jump / sneak are written from the keybinds.
 * 2. {@link MovementInputEvent} fires — modules may mutate the input and
 * override {@code sneakMultiplier} (defaults to vanilla 0.3).
 * 3. The (possibly overridden) sneak multiplier is applied to
 * forward / strafe when sneaking.
 * 4. {@link PostPlayerInputEvent} fires at return.
 */
@Mixin(MovementInputFromOptions.class)
public abstract class MixinMovementInputFromOptions extends MovementInput {

    @Shadow
    @Final
    private GameSettings gameSettings;

    /**
     * @author CrossSine
     * @reason Raven-style Pre/Post player-input event dispatch with a
     * mutable sneak multiplier. Replaces the @Inject that used to
     * live in this mixin.
     */
    @Overwrite
    public void updatePlayerMoveState() {
        this.moveStrafe = 0.0F;
        this.moveForward = 0.0F;

        if (this.gameSettings.keyBindForward.isKeyDown()) ++this.moveForward;
        if (this.gameSettings.keyBindBack.isKeyDown()) --this.moveForward;
        if (this.gameSettings.keyBindLeft.isKeyDown()) ++this.moveStrafe;
        if (this.gameSettings.keyBindRight.isKeyDown()) --this.moveStrafe;

        this.jump = this.gameSettings.keyBindJump.isKeyDown();
        this.sneak = this.gameSettings.keyBindSneak.isKeyDown();

        // Pre-event: modules can mutate input + override sneak slowdown.
        final MovementInputEvent pre = new MovementInputEvent((MovementInput) (Object) this);
        CrossSine.eventManager.callEvent(pre);

        // Write the (possibly modified) input back in case a listener swapped
        // the `original` reference — in practice they mutate fields in place,
        // but be defensive.
        this.moveForward = pre.getOriginal().moveForward;
        this.moveStrafe = pre.getOriginal().moveStrafe;
        this.jump = pre.getOriginal().jump;
        this.sneak = pre.getOriginal().sneak;

        if (this.sneak) {
            final double mul = pre.getSneakMultiplier();
            this.moveStrafe = (float) ((double) this.moveStrafe * mul);
            this.moveForward = (float) ((double) this.moveForward * mul);
        }

        // Post-event: fired once the final moveForward/moveStrafe are set.
        CrossSine.eventManager.callEvent(new PostPlayerInputEvent());
    }
}
