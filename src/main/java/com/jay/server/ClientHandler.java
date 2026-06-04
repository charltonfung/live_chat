package com.jay.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientHandler implements Runnable {

    // 客戶端傳給伺服器的訊息類型
    private static final int TYPE_LOGIN   = 1;
    private static final int TYPE_MESSAGE = 2;
    private static final int TYPE_LOGOUT  = 3;

    // 伺服器回覆給客戶端的「登入結果」訊息類型
    // (1=聊天訊息, 2=用戶列表 已用,從 3 開始延伸)
    static final int TYPE_LOGIN_OK     = 3;
    static final int TYPE_LOGIN_REJECT = 4;

    // 時間戳格式 (時:分:秒),在訊息廣播時加在前面
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Socket socket;
    private final ChatServer server;
    private DataInputStream dis;
    private DataOutputStream dos;
    // volatile:讓 reserveName 中設定的 name 對其他 thread 立即可見
    private volatile String name;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ClientHandler(Socket socket, ChatServer server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        try {
            while (!socket.isClosed()) {
                int type = dis.readInt();

                switch (type) {
                    case TYPE_LOGIN:
                        String requestedName = dis.readUTF();
                        // 嘗試註冊名字;若已被其他線上用戶佔用,送出 REJECT 並關閉連線
                        if (!server.reserveName(requestedName, this)) {
                            sendMessage(TYPE_LOGIN_REJECT,
                                    "名稱「" + requestedName + "」已被使用,請改用其他名稱");
                            // 不要走 close() 廣播流程(因為 name 還是 null 就不會廣播離開)
                            // 直接關 socket;run() 的 while 會自然結束
                            try { socket.close(); } catch (IOException ignored) {}
                            return;
                        }
                        // reserveName 已把名字寫到 this.name,可以安全廣播加入事件
                        sendMessage(TYPE_LOGIN_OK, "");
                        String loginTime = LocalTime.now().format(TIME_FORMATTER);
                        System.out.println("[Server] [" + loginTime + "] " + name + " 已登入");
                        server.broadcastMessage("[" + loginTime + "] 【" + name + " 加入了聊天室】");
                        server.broadcastUserList();
                        break;

                    case TYPE_MESSAGE:
                        String message = dis.readUTF();
                        // 由伺服器產生時間戳,避免每個客戶端時鐘不一致
                        String timestamp = LocalTime.now().format(TIME_FORMATTER);
                        System.out.println("[Server] [" + timestamp + "] " + name + ": " + message);
                        // 廣播格式: [HH:mm:ss] name: message
                        server.broadcastMessage("[" + timestamp + "] " + name + ": " + message);
                        break;

                    case TYPE_LOGOUT:
                        return;

                    default:
                        System.out.println("[Server] 收到未知類型: " + type);
                }
            }
        } catch (IOException e) {
            // 客戶端斷線，正常結束
        } finally {
            close();
        }
    }

    // 伺服器送給此客戶端的訊息
    // type=1: 聊天訊息, type=2: 用戶列表
    public synchronized void sendMessage(int type, String content) {
        if (closed.get()) return;
        try {
            dos.writeInt(type);
            dos.writeUTF(content);
            dos.flush();
        } catch (IOException e) {
            close();
        }
    }

    public void close() {
        if (closed.compareAndSet(false, true)) {
            server.removeClient(this);
            if (name != null) {
                // 離開聊天室時,由伺服器產生時間戳
                String logoutTime = LocalTime.now().format(TIME_FORMATTER);
                System.out.println("[Server] [" + logoutTime + "] " + name + " 已離線");
                server.broadcastMessage("[" + logoutTime + "] 【" + name + " 離開了聊天室】");
                server.broadcastUserList();
            }
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    public String getName() {
        return name;
    }

    /**
     * 提供給 ChatServer.reserveName 在 synchronized 區塊內呼叫,
     * 確保名字寫入與「重名檢查」是同一個鎖保護下完成。
     * package-private:只允許同一 package 的 ChatServer 使用。
     */
    void assignName(String name) {
        this.name = name;
    }
}
