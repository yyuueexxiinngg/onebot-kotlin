package mirai

import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.enable
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.load
import net.mamoe.mirai.console.pure.MiraiConsolePureLoader

import tech.mihoyo.mirai.PluginBase

object RunMirai {

    // 执行 gradle task: runMiraiConsole 来自动编译, shadow, 复制, 并启动 pure console.
    @JvmStatic
    suspend fun main(args: Array<String>) {
        // 默认在 /test 目录下运行
        MiraiConsolePureLoader.startAsDaemon()
        PluginBase.load()
        PluginBase.enable()

        val bot = MiraiConsole.addBot(123456, "").alsoLogin()

        MiraiConsole.job.join()
    }
}