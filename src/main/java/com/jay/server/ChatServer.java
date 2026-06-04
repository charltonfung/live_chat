package com.jay.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {

    // 日期分隔線格式 (例如 06/04),同一天只在第一則訊息前顯示一次
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd");

    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    // 上次廣播的日期,用來判斷是否要插入新的日期分隔線
    private LocalDate lastBroadcastDate = null;

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Chat server started on port " + port);

        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                clients.add(handler);
                Thread thread = new Thread(handler);
                thread.setDaemon(true);
                thread.start();
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 廣播聊天訊息 (type=1)
    public void broadcastMessage(String message) {
        // 跨日檢查:當天第一則訊息(包含登入/登出/聊天)前先送出日期分隔線
        // synchronized 避免多個 ClientHandler 併發呼叫時重複插入分隔線
        synchronized (this) {
            LocalDate today = LocalDate.now();
            if (!today.equals(lastBroadcastDate)) {
                lastBroadcastDate = today;
                String dateHeader = "──── " + today.format(DATE_FORMATTER) + " ────";
                for (ClientHandler client : clients) {
                    client.sendMessage(1, dateHeader);
                }
            }
        }
        for (ClientHandler client : clients) {
            client.sendMessage(1, message);
        }
    }

    // 廣播線上用戶列表 (type=2)
    public void broadcastUserList() {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler client : clients) {
            if (client.getName() != null) {
                if (sb.length() > 0) sb.append(",");
                sb.append(client.getName());
            }
        }
        String userList = sb.toString();
        for (ClientHandler client : clients) {
            client.sendMessage(2, userList);
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    /**
     * 嘗試把名字註冊給指定的 ClientHandler。
     *
     * synchronized 鎖住整個 server,讓「檢查是否重名」與「設定名字」變成原子動作,
     * 避免兩個用戶同時登入同名時兩邊都判定通過的競爭狀況。
     *
     * @return true  成功(名字未被使用,已寫入該 handler)
     *         false 失敗(名字已被線上其他用戶佔用)
     */
    public synchronized boolean reserveName(String requestedName, ClientHandler requester) {
        for (ClientHandler c : clients) {
            // 跳過自己;比對 name 是否相同(大小寫敏感)
            if (c != requester && requestedName.equals(c.getName())) {
                return false;
            }
        }
        requester.assignName(requestedName);
        return true;
    }
}
