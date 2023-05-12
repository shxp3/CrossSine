package net.ccbluex.liquidbounce.ui.sound

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.utils.FileUtils
import java.io.File

class TipSoundManager {
    //Classic
    var enableSound: TipSoundPlayer
    var disableSound: TipSoundPlayer
    //Herta
    var HertaEnableSound: TipSoundPlayer
    var HertaDisableSound: TipSoundPlayer

    init {
        val enableSoundFile = File(CrossSine.fileManager.soundsDir, "enable.wav")
        val disableSoundFile = File(CrossSine.fileManager.soundsDir, "disable.wav")

        if (!enableSoundFile.exists()) {
            FileUtils.unpackFile(enableSoundFile, "assets/minecraft/crosssine/sound/enable.wav")
        }
        if (!disableSoundFile.exists()) {
            FileUtils.unpackFile(disableSoundFile, "assets/minecraft/crosssine/sound/disable.wav")
        }
        val HertaEnableSoundFile = File(CrossSine.fileManager.hertaSoundDir, "enable.wav")
        val HertaDisableSoundFile = File(CrossSine.fileManager.hertaSoundDir, "disable.wav")

        if (!HertaEnableSoundFile.exists()) {
            FileUtils.unpackFile(HertaEnableSoundFile, "assets/minecraft/crosssine/sound/herta/enable.wav")
        }
        if (!HertaDisableSoundFile.exists()) {
            FileUtils.unpackFile(HertaDisableSoundFile, "assets/minecraft/crosssine/sound/herta/disable.wav")
        }
        //Classic
        enableSound = TipSoundPlayer(enableSoundFile)
        disableSound = TipSoundPlayer(disableSoundFile)
        //Herta
        HertaEnableSound = TipSoundPlayer(HertaEnableSoundFile)
        HertaDisableSound = TipSoundPlayer(HertaDisableSoundFile)
    }
}