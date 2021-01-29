## `0.3.5`

- 适配`mirai` 2.1.0
- `get_stranger_info`现支持获取陌生人名片
- 修复错误的array消息上报中错误的数据类型 #127
- 修复临时会话消息错误上报为群消息 #133
- 支持接收群组匿名消息
- 支持禁言匿名群成员 `set_group_anonymous_ban`
- 支持获取消息 `get_msg`
- 支持`mirai`2.1.0 音乐分享卡片消息
- 支持接收群成员荣誉变更事件 (`mriai`2.1.0暂仅支持龙王事件)

## `0.3.4` *2021/01/13*

- 适配`mirai` 2.0-RC
- `get_friend_list`现支持返回好友备注
- `get_stranger_info`极有限支持, 仅返回用户昵称, 且仅支持获取好友或存在于`Bot`所在某群中的成员 [相关Issue](https://github.com/mamoe/mirai/issues/234)
- OneBot Kotlin: 系统变量更名 #117
  - `onebot.backend`->`ONEBOT_BACKEND`
  - `onebot.account`->`ONEBOT_ACCOUNT`
  - `onebot.password`->`ONEBOT_PASSWORD`

## `0.3.3` *2020/11/28*

- **修复反向Websocket客户端概率出现未清除会话导致无法重连的问题** #81
- 再次修复`xml`以及`json`消息的字段不正确 #112
- 支持接收及发送闪照 #114
- 支持通过`json`发送程序分享富文本消息, 类似 ```{\"app\":\"com.tencent.weather\"&#44; ....```
- 支持接收群组及好友消息撤回事件 *(获取消息接口尚未支持)*
- HTTP上报服务支持超时, `http`配置项中增加`timeout`配置 #113
- 富文本消息段类型跟随OneBot标准使用`json`, `xml`, 弃用`rich`

## `0.3.2` *2020/11/23*
### **修复因合并`embedded`分支而在`0.3.1`中引入的`Array`格式消息上报序列化格式错误**

此错误导致`Array`格式上报的使用者无法正常解析收取到的消息, **`String`格式上报的使用者不受影响**

修复`xml`以及`json`消息的字段不正确

## `0.3.1` *2020/11/22*
- 优化事件处理机制 #109
- 更新依赖`mirai-console`至`1.0.0`, [更新日志](https://github.com/mamoe/mirai-console/releases/tag/1.0.0), **现在聊天中`/help`
  命令不会与`console`内建命令冲突了** #110
- 新版`console`内置了简单修改日志打印等级的配置, 因此弃用自定义`Logger`
  - `OneBot`配置项中`debug`项作废, 修改此项不会产生任何作用
  - 开启Debug打印的配置请修改`console`本身的配置, 位于`config/Console/Logger.yml`
    - 可将`defaultPriority: INFO`修改为`defaultPriority: DEBUG`或以上开启所有**mirai及所有插件**的Debug日志输出
    - **或在`loggers`项下新增`OneBot: DEBUG`或以上单独开启本插件的Debug日志输出**

## `0.3.0` *2020/11/16*
- 项目更名:
  - 插件版更名为`OneBot Mirai`, `mirai-console`中名为`OneBot`
  - Embedded版更名为`OneBot Kotlin`
- **适配`mirai-console 1.0`** #93 #99 #106
- 新增: [事件过滤器](https://github.com/howmanybots/onebot/blob/master/legacy/EventFilter.md) 支持, 与原版行为不一致的地方:
  - 未增加`event_filter`配置项, 将`filter.json`放置在`config/OneBot/filter.json`既视为启用事件过滤
  - 若文件不存在, 或过滤规则语法错误, 则不会对事件进行过滤
- 修复: 撤回他人消息出错 #55 #98
- 修复: `send_msg` API中群聊与私聊逻辑判断问题 #105
- 优化: 初次启动时自动生成样本配置文件
- 更新依赖`mirai-core`至`1.3.3`, 插件版添加获取群荣誉API `get_group_honor_info`支持

### OneBot Kotlin - [分支](https://github.com/yyuueexxiinngg/cqhttp-mirai/tree/embedded)

- 包含上述所有更新
- **配置文件位置同步变更至`config/OneBot/settings.yml`**
- 更新依赖`mirai-console`至`1.0-RC-1`
- 替换前端为`mirai-console-terminal`
- **同步`miraiOK`删除对`config.txt`的支持, 自动登录请修改`config/Console/AutoLogin.yml`使用`mirai-console`内建支持**
- **用以自动登录的环境变量更名**: 
  - `cqhttp.account` -> `onebot.account`
  - `cqhttp.password` -> `onebot.password`
- 使用`--args -- --xx`传入参数至`mirai-console`, 如`--args -- --help`将`--help`传入获取`mirai-console`提供的帮助信息

#### 注意事项:
`mirai-console 1.0`后配置文件路径有所变化, 现在配置文件位于`config/OneBot/settings.yml`

插件数据文件夹位置 *(image, record等)* 同样有所变化, 现在位于`data/OneBot`文件夹下

并且配置项中将原来的各账号移动至`bots`配置项下, 现在格式如下
```yaml
debug: true
bots:
  1234567890:
    ws_reverse:
  0987654321:
    ws_reverse:
```

## `0.2.3`  *2020/08/27*

- **修复: 反向WS客户端非`Universal`模式下`event`路由不保持长连接的问题**, 此BUG**导致所有非`Universal`模式接入的框架无法使用**(
  如[cqhttp.Cyan](https://github.com/frank-bots/cqhttp.Cyan)) #69
- **修复: 反向WS客户端添加`TLS`支持**, 需在配置文件`ws_reverse`中新增项`useTLS: true`以使用`TLS`建立连接, 配置文件详见README.md #42
- 修复: HTTP上报服务启动时发送的`meta_event`未签名, 此BUG导致一些框架(如[PicqBotX](https://github.com/HyDevelop/PicqBotX))无法正常使用 #65
- 修复: 心跳服务发送数据类型错误, 此BUG导致一些框架无法正常接收心跳数据包, (如[PicqBotX](https://github.com/HyDevelop/PicqBotX)
  , [cqhttp.Cyan](https://github.com/frank-bots/cqhttp.Cyan)) #70
- **修复: 从链接发送语音时语音不完整的问题** #59
- 修复: `get_version_info `
  API返回值现在符合[OneBot标准](https://github.com/howmanybots/onebot/blob/master/v11/specs/api/public.md#get_version_info-%E8%8E%B7%E5%8F%96%E7%89%88%E6%9C%AC%E4%BF%A1%E6%81%AF)了
  #67
  - 其中`app_version`为当前版本, `app_build_version`为当前`Commit`版本
- 修复: `set_group_name`
  API参数现在符合[OneBot标准](https://github.com/howmanybots/onebot/blob/master/v11/specs/api/public.md#set_group_name-%E8%AE%BE%E7%BD%AE%E7%BE%A4%E5%90%8D)了
- 新增: 通过链接下载媒体时支持`timeout`配置 #61 举例: [CQ:image,cache=0,**timeout=5**,url=xxxxxx]
- 新增: **通过链接下载媒体时支持`proxy`配置来通过代理下载**, 举例: [CQ:image,cache=0,**proxy=1**,url=xxxxxx], 需在配置文件中新增`proxy`项, 支持`HTTP`及`Sock`
  代理, 配置文件详见README.md
- 新增: 现在支持发送网易云音乐卡片了, 感谢 @F0ur 对[go-cqhttp](https://github.com/Mrs4s/go-cqhttp/)做出的贡献
- 新增: `get_group_member_info` API支持设置`no_cache`, 此前`mirai`已会实时更新群员权限, 即不需要设置为`true`, `no_cache`选项仅适用于实时获取群员特殊头衔
- 新增: 支持`get_image`和`get_record`API #60 , **需在配置中开启对应缓存**, 返回值中`file`指向媒体文件绝对路径, `file_type`为媒体实际类型, 未知类型返回`unknown`
  - `get_image`会**根据缓存下载图片**
  - `get_record`会返回已缓存语音

### Embedded版本 - [分支](https://github.com/yyuueexxiinngg/cqhttp-mirai/tree/embedded)

- 包含上述所有更新
- 优化: 现在读取config.txt自动登录时不会与传参和环境变量重复导致登录两次同一个`Bot`了 #64
  ~碎碎念: `mirai-console-1.0.0`已基本可用, 现在应该是基于`mirai-console-0.5.2`的最后一个大版本了 , 接下来重心是对`mirai-console-1.0.0`进行适配~

## `0.2.2`  *2020/08/20*

#### 0.2.2.5

- HTTP API服务器及正向Websocket服务器鉴权支持`Authorization`头 #58
- `0.2.2.4`中引入的读取[go-cqhttp](https://github.com/Mrs4s/go-cqhttp/)的`.image`文件现在支持`JRE 1.8`而非`JRE 1.9`以上了
- 现在调用`delete_msg`接口成功时不会错误返回报错了
- 现在`Bot`被邀请进群及加群申请被通过后会正常触发`MemberJoinEvent`事件了, `user_id`与`Bot`相同
- 现在支持接收及处理`Bot`被邀请加群事件了
- 现在发送已缓存媒体时可带上后缀了, 如以下格式都支持: `image, file=XXXX`, `image, file=XXXX.cqimg`

##### 已知BUG
- 使用`Embedded`版本并加载其他`mirai`插件后**无法正确读取`CQHTTPMirai`配置文件**导致无法正常使用, 此BUG**与`Embedded`版本初衷相违背**, 但由于`mirai-console 1.0.0`发布后配置文件读取逻辑需要重写, 故暂停此问题的修复

#### 0.2.2.4
- 优化Websocket反向客户端及服务端API处理逻辑, 现在调用耗时API(如下载大图再发送)时不会阻塞了, 具体例子为在`nonebot`中`您有命令正在执行，请稍后再试`不会在报错`WebSocket API call timeout`后才能发出 #15 
- 支持读取发送由[go-cqhttp](https://github.com/Mrs4s/go-cqhttp/)生成的图片`.image`缓存文件

**因小版本不一定全为BUG修复, 今后小版本不再使用`-Fix*`方式进行命名**

#### 0.2.2-Fix3

- 普通 修复`BotEvent`的系列化问题, 此BUG在`0.2.2`尝试升级`kotlin serialization`时引入, 会导致插件使用者收不到各类Bot时间, 如`好友请求`, `群成员加群请求/退群事件`, `禁言事件`等
  ~~那么Fix3它来了~~

#### 0.2.2-Fix2

- 普通 修复`get_group_info`, `get_group_member_list`API的参数解析错误, 举例: 此BUG会导致yobot无法获取群组和成员信息
  ~~希望没有Fix3~~

#### 0.2.2-Fix1

- **严重**  **修复尝试修复`.handle_quick_operation`API时对其引入的新BUG, 此BUG会导致只有在群里回复并AT发送人时才能正常解析消息**
  影响范围广泛, HTTP上报服务#48, 反向WS客户端与Nonebot #49

#### 0.2.2

- **基于`mirai-core 1.2.1`, 与1.1.3不兼容** #45 
- **插件版现在也支持发送语音了**
  - 发送`amr`和`silk`格式的语音全平台可收听, 发送`mp3`, `m4a`, `aac`等格式语音只有手机端可收听
- **修复`.handle_quick_operation`API中的消息解析错误**, 此错误导致无法使用`array`格式进行快速回复 #38 
- POST请求支持接收form-urlencoded #44
- HTTP上报服务`Content-Type`中加入编码值, 此前一些较严框架无法收到上报消息 #37 
- 支持发送心跳包 #41 
  - 心跳包默认不启用, 如需启用请在`Bot`设置中新增以下项

```yaml
'123456789':
  heartbeat:
    enable: true
    interval: 15000 # 心跳发送间隔, 单位毫秒, 如不填写默认15000
```

### Embedded版本 - [分支](https://github.com/yyuueexxiinngg/cqhttp-mirai/tree/embedded)

- 包含上述所有更新
- **增加获取群荣誉的API**, 如`龙王`, `群聊之火`, `快乐源泉`等, [详细API描述](https://github.com/howmanybots/onebot/blob/master/v11/specs/api/public.md#get_group_honor_info-%E8%8E%B7%E5%8F%96%E7%BE%A4%E8%8D%A3%E8%AA%89%E4%BF%A1%E6%81%AF)

~碎碎念: 这版本来昨天就要发, 但是`mirai`突然复活发版`mirai-core 1.2.0`, 适配后想跳过这版直接基于`mirai-console 1.0`上一波`cqhttp-mirai 0.3.0`, 但是今天测试了`console 1.0.0`后发现破坏体验的BUG有点多, 只好选择基于`console 0.5.2`再发一版, 那么下一版不出意外将基于`console 1.0.0`, 配置文件将会不兼容, 同时需要其他插件也适配`console 1.0.0`, 目前已确认`mirai-native`, `mirai-api-http`, `mirai-kts`等下版本将适配`console 1.0.0`~

## `0.2.1`  *2020/08/13*
- **修复正向WS路径`/`的事件处理逻辑错误** #33
- **修复好友/群成员添加请求事件的上报格式错误** #34 
- 修复当未开启反向WS时处理好友/群成员添加请求时的空指针异常
- 为图片下载添加UA, **减少因反爬虫机制导致的图片获取出错** #32 
- 对增强CQ码中的`url`值进行转义
- 接收图片时`file=md5`而非`mirai`的`imageId`
- **图片缓存文件夹由`images`改为`image`**, 位于`plugins/CQHTTPMirai/image`
- 通过`url`发送图片时, **默认对`url`进行hash并保存图片缓存(仅保存图片元数据, <0.2KB)**, 支持`cache=0`来不使用缓存
- 配置文件Bot设置中, **添加`cacheImage`字段**, 当设置为`true`时会对接收到的所有图片进行缓存, 默认不开启(仅保存图片元数据, <0.2KB)
- 支持发送接收到的图片(发送接收到图片的`file=`字段值), 需开启上述接收图片的缓存
- 支持发送CKYU生成的`cqimg`文件, 需将文件复制到`image`文件夹下, **发送时文件名不带`cqimg`后缀**
- 对CQ码内key进行trim, 现在CQ码中带空格不会报错了

### Embedded版本 - [分支](https://github.com/yyuueexxiinngg/cqhttp-mirai/tree/embedded)

- 包含上述所有更新
- 启动时可以传参`--account 1234567890 --password xxxxxx`来进行自动登录
- 会读取环境变量`cqhttp.account`和`cqhttp.password`, 作用同上, 优先级低, 会被参数覆盖
- **支持读取miraiOK生成的config.txt配置文件中的命令**
- 支持接收语音时获取下载链接
- 通过`url`发送语音时, 默认对`url`进行hash并保存语音缓存(保存完整语音数据), 支持`cache=0`来不使用缓存
- 配置文件Bot设置中, 添加`cacheRecord`字段, 当设置为`true`时会对接收到的所有语音进行下载缓存, 默认不开启(保存完整语音数据)
- 支持发送接收到的语音(发送接收到语音的`file=`字段值), 需开启上述接收语音的缓存

## `0.2.0`  *2020/08/09*
#### 0.2.0-Fix1

- 修复高CPU占用的问题 ~(谁还没写过个死循环呢, 我错了, 是我太菜了)~

#### 0.2.0

- 修复检测反向WS客户端连接状态导致的内存泄露 #22 
- 修复HTTP服务端接搜JSON格式POST请求时的编码错误 #25
- 修复潜在的内容转义问题 #26 
- 将好友请求、原消息保存条数从`4096`条下调至`512`条, 缩减内存占用
- 增加拓展API`set_group_name`支持. 来自[go-cqhttp的设置群名
](https://github.com/Mrs4s/go-cqhttp/blob/master/docs/cqhttp.md#%E8%AE%BE%E7%BD%AE%E7%BE%A4%E5%90%8D)
- 增加Embedded版本, 内置Core和Console, 支持语音, 目前只支持`.amr`格式语音

### Embedded版本 - [分支](https://github.com/yyuueexxiinngg/cqhttp-mirai/tree/embedded)

- 此版本内置Core和Console, 支持语音,  目前只支持`.amr`格式语音
- 请将此版本Jar包放至与`mirai-console`, `miraiOK`同级目录
- 此版本启动方式`java -jar cqhttp-mirai-**-embedded.jar`
- 此版本配置文件及`image`文件夹路径有所变更, 在`plugins`文件夹下, 而非`plugins/CQHTTPMirai`
- 请不要将此版本与主分支单插件版同时使用, 即不要在`plugins`文件夹下放置`cqhttp-mirai`的Jar包

## `0.1.9`  *2020/08/06*
- 修复未对服务进行配置时的报错 #20 
- 获取群成员列表时包含Bot本身 #23 
- 上报服务X-Signature格式符合CQHTTP标准 #21 
- 修复设置特殊头衔时的错误返回值
- 支持发送自定义Json消息　代码来自[mirai-native](https://github.com/iTXTech/mirai-native)
- 支持发送自定义Xml消息　代码来自[mirai-native](https://github.com/iTXTech/mirai-native)
- 修复CQCode转义逻辑, 现在[CQ-picfinder-robot](https://github.com/Tsuk1ko/CQ-picfinder-robot)发送的SauceNao图片可正常显示了

## `0.1.8`  *2020/08/05*
- 添加HTTP API服务端支持

配置中`http`项新增四项配置, 请参考[README.md](https://github.com/yyuueexxiinngg/cqhttp-mirai/blob/master/README.md)
```yaml
  http:
    enable: true   #新增
    host: 0.0.0.0   #新增
    port: 5700   #新增
    accessToken: ""   #新增
    postUrl: ""
    postMessageFormat: string
    secret: ""
```

## `0.1.7`  *2020/08/04*
#### Fix 1

- 修复ws反向客户端断线后重连问题

#### 0.1.7

- 添加Websoket正向服务端支持 #7 
- 反向Websoket客户端添加Api, Event路由支持 

配置新增`ws`项用以配置Websoket正向服务端
```yaml
  ws:
    # 可选，是否启用正向Websocket服务器，默认不启用
    enable: true
    # 可选，上报消息格式，string 为字符串格式，array 为数组格式, 默认为string
    postMessageFormat: string
    # 可选，访问口令, 默认为空, 即不设置Token
    accessToken: ""
    # 监听主机
    wsHost: "0.0.0.0"
    # 监听端口
    wsPort: 8080
```

反向ws配置新增三项配置以支持Api和Event路由, 现有配置默认使用Universal路由, 无需改动
```yaml
  ws_reverse:
    - enable: true
      postMessageFormat: string
      reverseHost: 
      reversePort: 
      reversePath: /ws
      reverseApiPath: /api  # 新增
      reverseEventPath: /event  # 新增
      useUniversal: true  # 新增
      reconnectInterval: 3000
```

## `0.1.6`  *2020/08/02*
- 修复当登录多个Bot时消息重复接收 #4 
- Websoket反向客户端添加多后端连接支持 #8 
- 使用CQHTTP原User-Agent #12 

### 本更新不兼容旧版配置文件
建议参考首页说明重新配置

## `0.1.4`  *2020/06/05*
- 修复```get_group_member_list```接口的返回值序列化错误
- 现在获取群成员信息时, ```nickname```和```name card```字段正确对应了
- 修复群成员加入及新好友请求的回应操作

## `0.1.3`  *2020/05/31*
- 修复数个api序列化返回值时发生错误导致无法返回数据
  - GetFriendList
  - GetGroupList
  - GetGroupMemberList
  - CanSendImage
  - CanSendRecord
- 支持_async调用

## `0.1.2`  *2020/05/25*
- 现在Websocket反向客户端默认为不启用
- 添加支持: CQHTTP .handle_quick_operation 隐藏API

## `0.1.1`  *2020/05/23*
- 修复当向插件提交的发送信息为单一JsonObject时的报错 ~(CQHTTP你好坑)~
- 现在关闭Console时不会打印"Websocket连接错误"的错误信息了

## `0.1.0`  *2020/05/23*
### 初始Release