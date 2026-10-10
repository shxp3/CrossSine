package net.ccbluex.liquidbounce.features.special


class NotificationManager {
    val list = ArrayList<NotificationUtil>()

}
enum class TYPE {
    SUCCESS,
    INFO,
    ERROR,
    WARNING
}
class NotificationUtil(var title: String, var content: String, var type: TYPE, var system: Long, var timer: Int)