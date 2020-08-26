package tech.mihoyo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.ConsoleCommandSender
import net.mamoe.mirai.console.plugins.PluginManager
import net.mamoe.mirai.console.pure.MiraiConsolePureLoader
import tech.mihoyo.mirai.PluginBase
import java.io.File

object CQHTTPKtCli : CliktCommand(name = "cqhttp-kotlin") {
    enum class BackendType {
        Mirai,
        Telegram
    }

    private val backend: BackendType by option(
        help = """
        Backend client of CQHTTP. 
        "Mirai" to use mirai, a Kotlin implementation of QQ protocol;
        ------------------------------------------
        后端. "Mirai" 为使用mirai, 一个Kotlin实现的QQ协议客户端;
    """.trimIndent(),
        envvar = "cqhttp.backend"
    ).enum<BackendType>().default(BackendType.Mirai)

    internal val account: String? by option(
        help = """
            Account to auto login.
                QQ when using mirai backend
            ------------------------------------
            需要自动登录的帐号
                使用mirai后段时为QQ号
        """.trimIndent(),
        envvar = "cqhttp.account"
    )

    internal val password: String? by option(
        help = """
            Account password to auto login.
            ------------------------------------
            需要自动登录的帐号密码
        """.trimIndent(),
        envvar = "cqhttp.password"
    )

    override fun run() {
        when (backend) {
            BackendType.Mirai -> runMirai()
            else -> runMirai()
        }
    }
}

fun main(args: Array<String>) {
    CQHTTPKtCli.main(args)
}


fun runMirai() {
    MiraiConsolePureLoader.load("1.2.2", "0.5.2") // 启动 console

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

    if (CQHTTPKtCli.account != null && CQHTTPKtCli.password != null) {
        CommandManager.runCommand(ConsoleCommandSender, "login ${CQHTTPKtCli.account} ${CQHTTPKtCli.password}")
    }

    val miraiOKConfigFile = File(System.getProperty("user.dir"), "config.txt")
    if (miraiOKConfigFile.exists()) {
        var stateCommand = false
        miraiOKConfigFile.forEachLine {
            if (it == "----------") {
                stateCommand = true
                return@forEachLine
            }
            if (stateCommand) {
                val cmd = it.trim()
                if (cmd != "" && !cmd.startsWith("#")) {
                    CQHTTPKtCli.account?.let { account ->
                        if (cmd.startsWith("login") && cmd.contains(account)) return@forEachLine
                    }
                    CommandManager.runCommand(ConsoleCommandSender, cmd)
                }
            }
        }
    }
    runBlocking { CommandManager.join() }
}