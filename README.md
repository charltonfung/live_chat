# Live Chat

Java Swing 多人聊天室,基於 Socket。

## 環境

JDK 17

## 啟動

**1. 編譯**

```powershell
.\mvnw.cmd compile
```

**2. 開 Terminal 跑 Server**

```powershell
java -cp target\classes com.jay.server.ServerMain
```

**3. 每位用戶開一個 Terminal 跑 Client**

```powershell
java -cp target\classes com.jay.ui.ClientMain
```

跑幾次就有幾位用戶,輸入名字按 Enter 進入聊天室。

## Port

預設 `127.0.0.1:8888`,要改改 `Constant.java`。
