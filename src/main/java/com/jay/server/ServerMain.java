package com.jay.server;

import com.jay.ui.Constant;

/**
 * 獨立啟動 Chat Server 的進入點。
 *
 * 用途:模擬多人登入時,先執行此 main 啟動伺服器,
 *       再分別跑多次 ClientMain,每個 JVM 代表一位使用者。
 *
 * 注意:不要與 LiveChatApplication 同時執行,否則會因 port 8888 衝突而失敗。
 */
public class ServerMain {

    /**
     * Server 進入點:建立 ChatServer 並阻塞於 accept 迴圈。
     */
    public static void main(String[] args) {
        // 建立 Server 物件
        ChatServer chatServer = new ChatServer();

        try {
            // 綁定預設 port,start() 內部會持續 accept,屬於阻塞呼叫
            chatServer.start(Constant.SERVER_PORT);
        } catch (Exception e) {
            // 啟動失敗(例如 port 被占用)印出錯誤並結束 JVM
            e.printStackTrace();
            System.exit(1);
        }
    }
}
