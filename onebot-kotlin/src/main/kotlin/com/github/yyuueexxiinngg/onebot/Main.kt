package com.github.yyuueexxiinngg.onebot

import kotlinx.coroutines.CancellationException
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.command.descriptor.ExperimentalCommandDescriptors
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.enable
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.load
import net.mamoe.mirai.console.terminal.ConsoleTerminalExperimentalApi
import net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader
import net.mamoe.mirai.console.util.ConsoleExperimentalApi

object OneBotKtCli : CliktCommand(name = "onebot-kotlin") {
    enum class BackendType {
        Mirai,
        Telegram
    }

    private val backend: BackendType by option(
        help = """```
        Backend client of OntBot.
        "Mirai" to use mirai, a Kotlin implementation of QQ protocol;
        ------------------------------------------
        后端. "Mirai" 为使用mirai, 一个Kotlin实现的QQ协议客户端;
    ```""".trimIndent(),
        envvar = "ONEBOT_BACKEND"
    ).enum<BackendType>().default(BackendType.Mirai)

    internal val account: String? by option(
        help = """```
            Account to auto login.
                QQ when using mirai backend
            ------------------------------------
            需要自动登录的帐号
                使用mirai后段时为QQ号
        ```""".trimIndent(),
        envvar = "ONEBOT_ACCOUNT"
    )

    internal val password: String? by option(
        help = """```
            Account password to auto login.
            ------------------------------------
            需要自动登录的帐号密码
        ```""".trimIndent(),
        envvar = "ONEBOT_PASSWORD"
    )

    private val args: Boolean? by option(
        help = """```
            Arguments pass through to backend.
                Usage: --args -- --help
            ------------------------------------
            要传递给后端的参数
                用法: --args -- --help
       ``` """.trimIndent()
    ).flag()

    private val argsToPass by argument().multiple()

    override fun run() {
        when (backend) {
            BackendType.Mirai -> runMirai(argsToPass.toTypedArray())
            else -> runMirai(argsToPass.toTypedArray())
        }
    }
}

fun main(args: Array<String>) {
    OneBotKtCli.main(args)
}

@OptIn(ConsoleExperimentalApi::class, ExperimentalCommandDescriptors::class, ConsoleTerminalExperimentalApi::class)
fun runMirai(args: Array<String>) {
    MiraiConsoleTerminalLoader.parse(args, exitProcess = true)
    MiraiConsoleTerminalLoader.startAsDaemon()
    PluginBase.load()
    PluginBase.enable()

    try {
        runBlocking {
            if (OneBotKtCli.account != null && OneBotKtCli.password != null) {
                MiraiConsole.addBot(OneBotKtCli.account!!.toLong(), OneBotKtCli.password!!) {
                    fileBasedDeviceInfo()
                }.alsoLogin()
            }

            MiraiConsole.job.join()
        }
    } catch (e: CancellationException) {
        // ignored
    }
}