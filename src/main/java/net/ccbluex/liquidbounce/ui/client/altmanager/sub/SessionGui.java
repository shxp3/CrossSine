package net.ccbluex.liquidbounce.ui.client.altmanager.sub;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager;
import net.ccbluex.liquidbounce.ui.client.gui.ThemedBackground;
import net.ccbluex.liquidbounce.ui.client.gui.ThemedUI;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.MouseUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.util.UUID;

public class SessionGui extends GuiScreen {
    private final GuiScreen previousScreen;

    private String status = "§7Idle";
    private GuiTextField sessionField;

    private float btn0 = 0f, btn1 = 0f, btn2 = 0f, fieldFocus = 0f;
    private float enterAnim = 0f, exitAnim = 0f;
    private Runnable exitAction = null;

    public SessionGui(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    private float panW() { return 360f; }
    private float panH() { return 220f; }
    private float panX() { return width / 2f - panW() / 2f; }
    private float panY() { return height / 2f - panH() / 2f; }
    private float innerX() { return panX() + 18f; }
    private float innerW() { return panW() - 36f; }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        sessionField = new GuiTextField(1, mc.fontRendererObj, 0, 0, 0, 0);
        sessionField.setMaxStringLength(32767);
        sessionField.setFocused(true);
        sessionField.setEnableBackgroundDrawing(false);
        enterAnim = 0f; exitAnim = 0f; exitAction = null;
        super.initGui();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        enterAnim += (1f - enterAnim) * 0.1f;

        // ── Exit animation ─────────────────────────────────────────────────
        if (exitAction != null) {
            exitAnim += (1f - exitAnim) * 0.13f;
            if (exitAnim > 0.97f) {
                Runnable action = exitAction; exitAction = null; action.run(); return;
            }
        }

        float scale = ThemedUI.INSTANCE.lerp(0.85f, 1f, enterAnim) * ThemedUI.INSTANCE.lerp(1f, 0.85f, exitAnim);
        float alpha = Math.max(0f, Math.min(1f, enterAnim * (1f - exitAnim)));

        Color accent = ThemedUI.INSTANCE.accent();
        ThemedBackground.INSTANCE.draw(width, height);

        float cx = width / 2f, cy = height / 2f;
        GL11.glPushMatrix();
        GL11.glTranslatef(cx, cy, 0f);
        GL11.glScalef(scale, scale, 1f);
        GL11.glTranslatef(-cx, -cy, 0f);
        GL11.glColor4f(1f, 1f, 1f, alpha);

        float panX = panX(), panY = panY(), panW = panW(), panH = panH();
        float innerX = innerX(), innerW = innerW();

        ThemedUI.INSTANCE.drawPanel(panX, panY, panW, panH, accent, true);
        float divY = ThemedUI.INSTANCE.drawSplitTitle(innerX, panY + 16f, "Session", " Login", accent, innerW);

        // status
        Fonts.SFBold40.drawStringWithShadow(status, innerX, divY + 8f, new Color(195, 200, 215, 220).getRGB());

        // text field
        float fY = divY + 8f + Fonts.SFBold40.FONT_HEIGHT + 10f;
        float fH = 22f;
        sessionField.xPosition = (int) (innerX + 6f);
        sessionField.yPosition = (int) (fY + fH / 2f - 4f);
        fieldFocus = ThemedUI.INSTANCE.drawTextField(sessionField, innerX, fY, innerW, fH,
                "name:uuid:token", accent, fieldFocus);

        // buttons
        float bY = fY + fH + 16f;
        float bH = 22f;
        float gap = 6f;
        float bW = (innerW - gap * 2) / 3f;
        btn0 = ThemedUI.INSTANCE.drawButton(innerX,                bY, bW, bH, "Login",
                mouseX, mouseY, btn0, accent, false, true);
        btn1 = ThemedUI.INSTANCE.drawButton(innerX + bW + gap,     bY, bW, bH, "Restore",
                mouseX, mouseY, btn1, accent, false, false);
        btn2 = ThemedUI.INSTANCE.drawButton(innerX + (bW + gap)*2, bY, bW, bH, "กลับ",
                mouseX, mouseY, btn2, accent, false, false);

        // hint
        String hint = "Format: username:uuid:token";
        Fonts.SFBold35.drawStringWithShadow(hint,
                innerX, panY + panH - Fonts.SFBold35.FONT_HEIGHT - 12f,
                new Color(120, 125, 140, 160).getRGB());

        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glPopMatrix();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && exitAction == null) {
            float divY = panY() + 16f + Fonts.font40Bold.FONT_HEIGHT + 7f;
            float fY = divY + 8f + Fonts.SFBold40.FONT_HEIGHT + 10f;
            float fH = 22f;
            float bY = fY + fH + 16f;
            float bH = 22f;
            float gap = 6f;
            float bW = (innerW() - gap * 2) / 3f;
            float ix = innerX();

            if (MouseUtils.mouseWithinBounds(mouseX, mouseY, ix, bY, ix + bW, bY + bH)) { doLogin(); return; }
            if (MouseUtils.mouseWithinBounds(mouseX, mouseY, ix + bW + gap, bY, ix + bW * 2 + gap, bY + bH)) { doRestore(); return; }
            if (MouseUtils.mouseWithinBounds(mouseX, mouseY, ix + (bW + gap) * 2, bY, ix + innerW(), bY + bH)) {
                exitAction = () -> mc.displayGuiScreen(previousScreen); exitAnim = 0f; return;
            }
        }
        sessionField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void doLogin() {
        try {
            String[] args = sessionField.getText().split(":");
            String name = args[0];
            UUID uuid = UUID.fromString(args[1].replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
            String token = args[2];
            try {
                mc.getSessionService().joinServer(new GameProfile(uuid, name), token, uuid.toString());
            } catch (AuthenticationUnavailableException e) { status = "§cError: Authentication unavailable"; return; }
            catch (InvalidCredentialsException e)        { status = "§cError: Invalid credentials"; return; }
            catch (AuthenticationException e)            { status = "§cError: Authentication failed"; return; }
            mc.session = new Session(args[0], args[1], args[2], "mojang");
            exitAction = () -> mc.displayGuiScreen(previousScreen); exitAnim = 0f;
        } catch (Exception e) {
            status = "§cError: " + e.getMessage();
            e.printStackTrace();
        }
    }

    private void doRestore() {
        try {
            mc.session = GuiAltManager.Companion.getOriginalSession();
            exitAction = () -> mc.displayGuiScreen(previousScreen); exitAnim = 0f;
        } catch (Exception e) {
            status = "§cError: Couldn't restore session";
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) { exitAction = () -> mc.displayGuiScreen(previousScreen); exitAnim = 0f; return; }
        if (keyCode == Keyboard.KEY_RETURN) { doLogin(); return; }
        sessionField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        sessionField.updateCursorCounter();
        super.updateScreen();
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}