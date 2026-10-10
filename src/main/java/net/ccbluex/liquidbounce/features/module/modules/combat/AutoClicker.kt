package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerRangeValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
import net.minecraft.item.ItemSword
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraftforge.fml.relauncher.ReflectionHelper
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.lang.reflect.InvocationTargetException
import kotlin.random.Random

@ModuleInfo("AutoClicker", ModuleCategory.COMBAT)
object AutoClicker : Module() {
    private val modeValue = ListValue("Left-Click-Mode", arrayOf("Normal", "Jitter", "Butterfly", "Dynamic"), "Normal")
    val invClicker = BoolValue("InvClicker", false)
    private val cpsValue = IntegerRangeValue("CPS-Amount", 7, 12, 1, 40)
    private val dynamicAmount = IntegerRangeValue("Dynamic-Amount", 1, 2, 1, 5).displayable { modeValue.equals("Dynamic") }
    private val jitterAmount = FloatValue("Jitter-Rotate-Amount", 0F, 0F, 10F).displayable { modeValue.equals("Jitter") }
    private val swordOnlyValue = BoolValue("Left-SwordOnly", false)
    private val CPS = TimerMS()
    var canLeftClick = false
    private var leftDelay = 0
    private var dynamicCps = cpsValue.get().first.toDouble()
    private var increasing = true
    private var lastUpdate = 0L

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (invClicker.get() && mc.currentScreen != null) {
            if (!Mouse.isButtonDown(0) || !Keyboard.isKeyDown(54) && !Keyboard.isKeyDown(42)) {
                return
            }
            inInvClick(mc.currentScreen)
        }
        leftClicker()
    }

    private fun getDelay(): Int {
        val now = System.currentTimeMillis()
        when (modeValue.get().lowercase()) {
            "normal" -> leftDelay = TimeUtils.randomClickDelay(cpsValue.get().first, cpsValue.get().last).toInt()

            "jitter" -> {
                leftDelay = if (Random.nextInt(1, 14) <= 3) {
                    if (Random.nextInt(1, 3) == 1) {
                        Random.nextInt(98, 102)
                    } else {
                        Random.nextInt(114, 117)
                    }
                } else {
                    if (Random.nextInt(1, 4) == 1) {
                        Random.nextInt(64, 69)
                    } else {
                        Random.nextInt(83, 85)
                    }

                }
            }

            "butterfly" -> {
                if (Random.nextInt(1, 10) == 1) {
                    leftDelay = Random.nextInt(225, 250)
                } else {
                    leftDelay = if (Random.nextInt(1, 6) == 1) {
                        Random.nextInt(89, 94)
                    } else if (Random.nextInt(1, 3) == 1) {
                        Random.nextInt(95, 103)
                    } else if (Random.nextInt(1, 3) == 1) {
                        Random.nextInt(115, 123)
                    } else {
                        if (Random.nextBoolean()) {
                            Random.nextInt(131, 136)
                        } else {
                            Random.nextInt(165, 174)
                        }
                    }
                }
            }
            "dynamic" -> {
                // อัปเดตทุก 200–500ms
                if (now - lastUpdate >= Random.nextLong(200, 500)) {
                    lastUpdate = now
                    increasing = !increasing
                }

                // ขยับค่า cps ทีละนิด
                val min = cpsValue.get().first.toDouble()
                val max = cpsValue.get().last.toDouble()

                val step = RandomUtils.nextInt(dynamicAmount.get().first, dynamicAmount.get().last) // จะเอาเร็วแค่ไหนก็ปรับเอา

                dynamicCps = if (increasing) {
                    (dynamicCps + step).coerceAtMost(max)
                } else {
                    (dynamicCps - step).coerceAtLeast(min)
                }

                // แปลงเป็น delay
                leftDelay = (1000.0 / dynamicCps).toInt()
            }
        }
        return leftDelay
    }

    private fun leftClicker() {
        if (swordOnlyValue.get() && mc.thePlayer.heldItem?.item !is ItemSword || mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK) {
            MouseUtils.leftClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindAttack)
            canLeftClick = false
            return
        }
        if (HitSelect.state && HitSelect.getCancelClick() && HitSelect.mode.equals("Pause")) {
            MouseUtils.leftClicked = false
            return
        }
        if (SilentAura.state && SilentAura.target != null) {
            canLeftClick = false
            return
        }
        if (mc.gameSettings.keyBindAttack.isKeyDown) {
            if (CPS.hasTimePassed(getDelay().toLong())) {
                KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode)
                if (modeValue.equals("Jitter")) {
                    mc.thePlayer.rotationYaw += RandomUtils.nextFloat(-jitterAmount.get(), jitterAmount.get())
                    mc.thePlayer.rotationPitch += RandomUtils.nextFloat(-jitterAmount.get(), jitterAmount.get())
                }
                MouseUtils.leftClicked = true
                canLeftClick = true
                CPS.reset()
            } else {
                MouseUtils.leftClicked = false
            }
        } else MouseUtils.leftClicked = false
    }

    private fun inInvClick(guiScreen: GuiScreen) {
        val mouseInGUIPosX = Mouse.getX() * guiScreen.width / mc.displayWidth
        val mouseInGUIPosY = guiScreen.height - Mouse.getY() * guiScreen.height / mc.displayHeight - 1

        try {
            if (CPS.hasTimePassed(getDelay().toLong())) {
                ReflectionHelper.findMethod<GuiScreen?>(
                    GuiScreen::class.java,
                    null,
                    arrayOf(
                        "func_73864_a",
                        "mouseClicked"
                    ),
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE
                ).invoke(guiScreen, mouseInGUIPosX, mouseInGUIPosY, 0)
                CPS.reset()
            }
        } catch (ignored: IllegalAccessException) {
        } catch (ignored: InvocationTargetException) {
        }
    }
    override val tag: String?
        get() = "${cpsValue.get().first} - ${cpsValue.get().last} CPS"
}