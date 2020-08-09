package tech.mihoyo

import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.plugins.PluginManager
import net.mamoe.mirai.console.pure.MiraiConsolePureLoader
import tech.mihoyo.mirai.PluginBase


fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        when (args[0]) {
            "-help" -> println(
                """
                Usage:
                    -mirai : Run mirai console
            """.trimIndent()
            )
            "-mirai" -> runMirai()
        }
    } else {
        println("Usage: -mirai Run mirai console")
    }
}

fun runMirai() {
    MiraiConsolePureLoader.load("1.2.0", "0.5.2") // 启动 console

    val selfMiraiPluginClass = PluginBase.javaClass.asSubclass(net.mamoe.mirai.console.plugins.PluginBase::class.java)

    val plugin =
        selfMiraiPluginClass.kotlin.objectInstance ?: selfMiraiPluginClass.getDeclaredConstructor().apply {
            kotlin.runCatching {
                this.isAccessible = true
            }
        }.newInstance()
    plugin.dataFolder

    val pluginNameFiled = plugin.javaClass.superclass.getDeclaredField("pluginName")
    pluginNameFiled.isAccessible = true
    pluginNameFiled.set(plugin, "CQHTTPMirai")

    val pluginsSequence = PluginManager.javaClass.getDeclaredField("pluginsSequence")
    pluginsSequence.isAccessible = true

    val lockFreeLinkedListClass = Class.forName("net.mamoe.mirai.utils.LockFreeLinkedList")
    val addLast = lockFreeLinkedListClass.getDeclaredMethod("addLast", Object::class.java)
    addLast.isAccessible = true
    addLast.invoke(pluginsSequence.get(PluginManager), plugin)

    val load = plugin.javaClass.superclass.getDeclaredMethod("load\$mirai_console")
    load.isAccessible = true
    load.invoke(plugin)

    val enable = plugin.javaClass.superclass.getDeclaredMethod("enable\$mirai_console")
    enable.isAccessible = true
    enable.invoke(plugin)

    runBlocking { CommandManager.join() }
}