package com.jay.ui;

/**
 * 獨立啟動 Chat Client 的進入點。
 *
 * 用途:模擬多人登入時,先確認 ServerMain 已在執行,
 *       再多次執行此 main,每個 JVM 代表一位使用者(輸入不同名字即可)。
 *
 * IntelliJ 設定:Run → Edit Configurations → Modify options
 *               → 勾選「Allow multiple instances」,即可重複執行同一個 Run Config。
 */
public class ClientMain {

    /**
     * Client 進入點:只啟動聊天入口 UI,不啟動 Server。
     */
    public static void main(String[] args) {
        // 確保在有圖形環境的情況下執行(避免 headless 例外)
        System.setProperty("java.awt.headless", "false");

        // 顯示輸入名字的入口畫面;成功登入後會切到 ClientChatUI
        ChatEntryUI chatEntryUI = new ChatEntryUI();
        chatEntryUI.showEntryUI();
    }
}
