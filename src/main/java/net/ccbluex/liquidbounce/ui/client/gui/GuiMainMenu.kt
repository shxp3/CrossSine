package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.font.FontLoaders
import net.ccbluex.liquidbounce.ui.client.GuiBackground
import net.ccbluex.liquidbounce.utils.Btn
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.i18n.LanguageManager
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.minecraft.client.gui.*
import net.minecraft.client.resources.I18n
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.client.GuiModList
import java.awt.Color

class GuiMainMenu : GuiScreen(), GuiYesNoCallback {
    var drawed = false
    var clicked = false
    fun drawBtns() {
        this.buttonList.add(
            Btn(
                100,
                (this.width / 2) - (130 / 2),
                this.height / 2 - 20,
                130,
                23,
                I18n.format("Singleplayer"),
                null,
                2,
                if (LiquidBounce.Darkmode.equals(true)) { Color(20, 20, 20, 170) } else { Color(255, 255, 255, 170) }
            )
        )
        this.buttonList.add(
            Btn(
                101,
                (this.width / 2) - (130 / 2),
                this.height / 2 + 10,
                130,
                23,
                I18n.format("Multiplayer"),
                null,
                2,
                if (LiquidBounce.Darkmode.equals(true)) { Color(20, 20, 20, 180) } else { Color(255, 255, 255, 170) }
            )
        )

        this.buttonList.add(
            Btn(
                200,
                (this.width / 2) - (130 / 2),
                this.height / 2 + 40,
                130,
                23,
                LanguageManager.get("Alts"),
                null,
                2,
                if (LiquidBounce.Darkmode.equals(true)) { Color(20, 20, 20, 180) } else { Color(255, 255, 255, 170) }
            )
        )
        this.buttonList.add(
            Btn(
                111,
                (this.width / 2) - (130 / 2),
                this.height / 2 + 70,
                130,
                23,
                LanguageManager.get("More"),
                null,
                2,
                if (LiquidBounce.Darkmode.equals(true)) { Color(20, 20, 20, 180) } else { Color(255, 255, 255, 170) }
            )
        )


        this.buttonList.add(
            Btn(
                104,
                this.width - 35,
                10,
                25,
                25,
                I18n.format("Exit"),
                if (LiquidBounce.Darkmode.equals(true)) { ResourceLocation("crosssine/imgs/icon/quit.png") } else { ResourceLocation("crosssine/imgs/icon/quit.png") },
                2,
                if (LiquidBounce.Darkmode.equals(true)) { Color(20, 20, 20, 180) } else { Color(255, 255, 255, 170) }
            )
        )

        this.buttonList.add(
            Btn(
                103,
                this.width - 65,
                10,
                25,
                25,
                I18n.format("Option").replace(".", ""),
                if (LiquidBounce.Darkmode.equals(true)) { ResourceLocation("crosssine/imgs/icon/setting.png") } else { ResourceLocation("crosssine/imgs/icon/setting.png") },
                2,
                if (LiquidBounce.Darkmode.equals(true)) { Color(20, 20, 20, 180) } else { Color(255, 255, 255, 170) }
                )
        )

        this.buttonList.add(
            Btn(
                201,
                this.width - 95,
                10,
                25,
                25,
                I18n.format("Background"),
                if (LiquidBounce.Darkmode.equals(true)) { ResourceLocation("crosssine/imgs/icon/wallpaper.png") } else { ResourceLocation("crosssine/imgs/icon/wallpaper.png") },
                2,
                if (LiquidBounce.Darkmode.equals(true)) { Color(20, 20, 20, 180) } else { Color(255, 255, 255, 170) }
            )
        )

              this.buttonList.add(
            Btn(
                204,
                this.width - 125,
                10,
                25,
                25,
                "Website",
                if (LiquidBounce.Darkmode.equals(true)) { ResourceLocation("crosssine/imgs/icon/website.png") } else { ResourceLocation("crosssine/imgs/icon/website.png") },
                2,
                if (LiquidBounce.Darkmode.equals(true)) { Color(20, 20, 20, 180) } else { Color(255, 255, 255, 170) }
            )
        )


        this.buttonList.add(
            Btn(
                203, this.width - 155, 10, 25, 25, "Discord", if (LiquidBounce.Darkmode.equals(true)) { ResourceLocation("crosssine/imgs/icon/discord.png") } else { ResourceLocation("crosssine/imgs/icon/discord.png") }, 2,
                if (LiquidBounce.Darkmode.equals(true)) { Color(20, 20, 20, 180) } else { Color(255, 255, 255, 170) }
            )
        )
        this.buttonList.add(
            Btn(
                66, this.width - 185, 10, 25, 25, "Youtube", if (LiquidBounce.Darkmode.equals(true)) { ResourceLocation("crosssine/imgs/icon/youtube.png") } else { ResourceLocation("crosssine/imgs/icon/youtube.png") }, 2,
                if (LiquidBounce.Darkmode.equals(true)) { Color(20, 20, 20, 180) } else { Color(255, 255, 255, 170) }
            )
        )
        this.buttonList.add(
            Btn(
                84444, this.width - 215, 10, 25, 25, "Reload Client", if (LiquidBounce.Darkmode.equals(true)) { ResourceLocation("crosssine/imgs/icon/reload.png") } else { ResourceLocation("crosssine/imgs/icon/reload.png") }, 2,
                if (LiquidBounce.Darkmode.equals(true)) { Color(20, 20, 20, 180) } else { Color(255, 255, 255, 170) }
            )
        )

        drawed = true
    }

    override fun initGui() {
        val defaultHeight = (this.height / 3.5).toInt()
        drawBtns()
        super.initGui()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(1)
        val defaultHeight = (this.height).toFloat()
        val defaultWidth = (this.width).toFloat()
        if (LiquidBounce.Darkmode.equals(true)) { RenderUtils.drawRect(0F, 0F, defaultWidth, defaultHeight, Color(0, 0, 0, 0)) } else { RenderUtils.drawRect(0F, 0F, defaultWidth, defaultHeight, Color(0, 0, 0, 100)) }
        val i = 0
        val defaultHeight1 = (this.height).toDouble()
        val defaultWidth1 = (this.width).toDouble()
        FontLoaders.F40.drawCenteredString(" ${LiquidBounce.CLIENT_NAME} ${LiquidBounce.CLIENT_VERSION}", this.width.toDouble() / 2, this.height.toDouble() / 2 - 60, Color(255, 255, 255, 255).rgb)
        FontLoaders.F30.drawCenteredString( "Hi ${mc.getSession().username}", this.width.toDouble() / 2, this.height.toDouble() / 2 - 80, Color(255, 255, 255, 200).rgb)


        var versionMsg =
            "Version: " + LiquidBounce.CLIENT_VERSION
        FontLoaders.F16.drawString(
            versionMsg,
            this.width - FontLoaders.F16.getStringWidth(versionMsg) - 10F,
            this.height - 15f,
            Color(1, 1, 1, 170).rgb
        )
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun mouseClicked(p_mouseClicked_1_: Int, i2: Int, i3: Int) {
        clicked = true
        super.mouseClicked(p_mouseClicked_1_, i2, i3)
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            66 -> MiscUtils.showURL("https://www.youtube.com/@shxp3")
            100 -> mc.displayGuiScreen(GuiSelectWorld(this))
            101 -> mc.displayGuiScreen(GuiMultiplayer(this))
            102 -> mc.displayGuiScreen(GuiModList(this))
            103 -> mc.displayGuiScreen(GuiOptions(this, mc.gameSettings))
            104 -> mc.shutdown()
            200 -> mc.displayGuiScreen(GuiAltManager(this))
            201 -> mc.displayGuiScreen(GuiBackground(this))
            203 -> MiscUtils.showURL("https://discord.gg/68qm3qMznG")
            204 -> MiscUtils.showURL("https://${LiquidBounce.CLIENT_WEBSITE}")
            111 -> mc.displayGuiScreen(GuiMore(this))
            84444 -> ClientUtils.reloadClient()
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {}
}
