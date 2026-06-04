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

    // 時間戳格式 (時:分:秒),在訊息廣播時加在前面
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Socket socket;
    private final ChatServer server;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String name;
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
                        name = dis.readUTF();
                        // 加入聊天室時,由伺服器產生時間戳,確保所有人看到的時間一致
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
}
