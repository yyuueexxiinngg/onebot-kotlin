# CQHTTP Mirai

![Gradle CI](https://github.com/mnixry/cqhttp-mirai/workflows/Gradle%20CI/badge.svg)

__CQHTTP runs on Mirai__

## 开始使用
0. 请首先运行[Mirai-console](https://github.com/mamoe/mirai-console)相关客户端生成plugins文件夹
1. 将`cqhttp-mirai`生成的`jar包文件`放入`plugins`文件夹中
2. 编辑`plugins/CQHTTPMirai/setting.yml`配置文件, 将以下给出配置复制并修改
3. 再次启动[Mirai-console](https://github.com/mamoe/mirai-console)相关客户端

## 配置相关

```yaml
# Debug日志输出选项
debug: false
# 要进行配置的QQ号 (Mirai支持多帐号登录, 故需要对每个帐号进行单独设置)
'1234567890':
  # HTTP 相关配置
  http:
    # 可选，事件及数据上报URL, 默认为空, 即不上报
    postUrl: ""
    # 可选，上报消息格式，string 为字符串格式，array 为数组格式, 默认为string
    postMessageFormat: string
    # 可选，上报数据签名密钥, 默认为空
    secret: ""
  # 可选，反向客户端服务
  ws_reverse:
    # 可选，是否启用反向客户端，默认不启用
    - enable: true
      # 上报消息格式，string 为字符串格式，array 为数组格式
      postMessageFormat: string
      # 反向Websocket主机
      reverseHost: 127.0.0.1
      # 反向Websocket端口
      reversePort: 8080
      # 反向Websocket路径
      reversePath: /ws
      # 反向Websocket Api路径 尚未实现
      #  reverseApiPath: /ws/
      # 反向Websocket Event路径 尚未实现
      #  reverseEventPath: /ws/
      # 反向 WebSocket 客户端断线重连间隔，单位毫秒
      reconnectInterval: 3000
    - enable: true # 这里是第二个连接, 相当于CQHTTP分身版
      postMessageFormat: string
      reverseHost: 127.0.0.1
      reversePort: 9222
      reversePath: /ws
      reconnectInterval: 3000
'0987654321': # 这里是第二个QQ Bot的配置
  ws_reverse:
    - enable: true
      postMessageFormat: string
      reverseHost: 
      reversePort: 
      reversePath: /ws
      reconnectInterval: 3000
```

## 计划

- [x] 反向Websocket客户端
- [x] HTTP上报服务
- [ ] HTTP API
- [ ] Websocket服务端


## 已经支持的CQHTTP API

#### 特别注意, 很多信息Mirai不支持获取, 如群成员的年龄、性别等, 为保证兼容性, 这些项已用`Unknown`, `0`之类的信息填充占位

- SendMessage (不包含讨论组消息)
- SendGroupMessage
- SendPrivateMessage
- DeleteMessage
- SetGroupKick
- SetGroupBan
- SetWholeGroupBan
- SetGroupCard
- SetGroupLeave
- SetFriendAddRequest
- GetLoginInfo
- GetFriendList
- GetGroupList
- GetGroupInfo (不支持获取群容量, 返回`0`)
- GetGroupMemberInfo
- GetGroupMemberList
- CanSendImage (恒为true)
- GetStatus (不完全支持, 仅返回`online`和`good`两项)
- GetVersionInfo

## 尚未支持的CQHTTP API

- GetImage
- SetRestartPlugin
- CleanDataDir
- CleanPluginLog
- GetCookies (Mirai不会支持)
- GetCSRFToken (Mirai不会支持)
- GetRecord
- GetCredentials
- GetStrangerInfo
- SendDiscussMessage
- SetGroupAnonymous
- SetGroupAdmin
- SetDiscussLeave
- SendLike (Mirai不会支持)
- SetAnonymousBan

## 开源协议

[AGPL-3.0](LICENSE) © yyuueexxiinngg

## 直接或间接引用到的其他开源项目

- [mirai-api-http](https://github.com/mamoe/mirai-api-http) -  [LICENSE](https://github.com/mamoe/mirai-api-http/blob/master/LICENSE)
- [Mirai Native](https://github.com/iTXTech/mirai-native)  -  [LICENSE](https://github.com/iTXTech/mirai-native/blob/master/LICENSE)
- [CQHTTP](https://github.com/richardchien/coolq-http-api) -  [LICENSE](https://github.com/richardchien/coolq-http-api/blob/master/LICENSE)
