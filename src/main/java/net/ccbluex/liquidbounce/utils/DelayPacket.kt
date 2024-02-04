package net.ccbluex.liquidbounce.utils

import net.minecraft.network.Packet

class DelayPacket(var pck: Packet<*>, var time: Long)