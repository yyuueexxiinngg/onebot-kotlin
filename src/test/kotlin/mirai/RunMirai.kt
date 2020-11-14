package mirai

import net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader

object RunMirai {

    // 执行 gradle task: runMiraiConsole 来自动编译, shadow, 复制, 并启动 pure console.
    @JvmStatic
    fun main(args: Array<String>) {
        // 默认在 /test 目录下运行
        MiraiConsoleTerminalLoader.main(emptyArray())
    }
}