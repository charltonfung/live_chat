package com.jay.ui;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientChatUI {

    // 伺服器傳給客戶端的訊息類型
    private static final int TYPE_CHAT_MESSAGE = 1;
    private static final int TYPE_USER_LIST    = 2;

    // 客戶端傳給伺服器的訊息類型
    private static final int TYPE_SEND_MESSAGE = 2;
    private static final int TYPE_LOGOUT       = 3;

    private final Socket socket;
    private final String name;
    private DataOutputStream dos;

    // 改用 JTextPane 才能對同一行內不同片段套不同顏色/字型
    private JTextPane messageArea;
    private DefaultListModel<String> userListModel;

    // 各種片段對應的樣式,在 showChatUI 內初始化後給 appendStyled 使用
    private Style styleTime;       // [HH:mm:ss]
    private Style styleSelfName;   // 自己的名字
    private Style styleOtherName;  // 別人的名字
    private Style styleContent;    // 聊天內容
    private Style styleSystem;     // 系統訊息(加入/離開聊天室)
    private Style styleDate;       // 日期分隔線

    public ClientChatUI(Socket socket, String name) {
        this.socket = socket;
        this.name = name;
        try {
            this.dos = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showChatUI() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Group Chat - " + name);
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.setSize(600, 400);
            frame.setLocationRelativeTo(null);

            // 關閉視窗時送出登出訊息
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    logout();
                    frame.dispose();
                    System.exit(0);
                }
            });

            // 主面板
            JPanel mainPanel = new JPanel(new BorderLayout());

            // 訊息顯示區
            JPanel messagePanel = new JPanel(new BorderLayout());
            messagePanel.setBorder(BorderFactory.createTitledBorder("Messages"));

            messageArea = new JTextPane();
            messageArea.setEditable(false);
            messageArea.setFont(new Font("楷體", Font.PLAIN, 14));
            // JTextPane 預設就會自動換行,不需要 setLineWrap

            // 初始化各片段樣式
            initStyles();

            JScrollPane messageScrollPane = new JScrollPane(messageArea);
            messagePanel.add(messageScrollPane, BorderLayout.CENTER);

            // 線上用戶列表
            JPanel usersPanel = new JPanel(new BorderLayout());
            usersPanel.setBorder(BorderFactory.createTitledBorder("Online Users"));
            usersPanel.setPreferredSize(new Dimension(150, 0));

            userListModel = new DefaultListModel<>();
            JList<String> userList = new JList<>(userListModel);
            userList.setFont(new Font("楷體", Font.PLAIN, 14));
            usersPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

            // 訊息輸入區
            JPanel inputPanel = new JPanel(new BorderLayout());
            inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JTextField messageInputField = new JTextField();
            messageInputField.setFont(new Font("楷體", Font.PLAIN, 14));
            inputPanel.add(messageInputField, BorderLayout.CENTER);

            JButton sendButton = new JButton("Send");
            sendButton.setFont(new Font("楷體", Font.BOLD, 14));
            sendButton.setBackground(new Color(70, 130, 180));
            sendButton.setForeground(Color.WHITE);
            sendButton.setFocusPainted(false);
            inputPanel.add(sendButton, BorderLayout.EAST);

            // 發送訊息邏輯
            Runnable sendAction = () -> {
                String message = messageInputField.getText().trim();
                if (!message.isEmpty()) {
                    sendMessage(message);
                    messageInputField.setText("");
                }
            };

            sendButton.addActionListener(e -> sendAction.run());
            // 按 Enter 也可以發送
            messageInputField.addActionListener(e -> sendAction.run());

            mainPanel.add(messagePanel, BorderLayout.CENTER);
            mainPanel.add(usersPanel, BorderLayout.EAST);
            mainPanel.add(inputPanel, BorderLayout.SOUTH);

            frame.add(mainPanel);
            frame.setVisible(true);

            // 啟動背景執行緒接收伺服器訊息
            startReceiving();
        });
    }

    /**
     * 註冊各種文字片段的樣式。所有 Style 都掛在 messageArea 的 StyledDocument 上,
     * 之後 appendStyled() 直接引用。字型統一沿用 messageArea 預設的「楷體」。
     */
    private void initStyles() {
        StyledDocument doc = messageArea.getStyledDocument();

        // 時間:灰色,稍微低調
        styleTime = doc.addStyle("time", null);
        StyleConstants.setForeground(styleTime, new Color(108, 117, 125));

        // 自己的名字:醒目的橘紅 + 粗體,容易跟其他人區分
        styleSelfName = doc.addStyle("selfName", null);
        StyleConstants.setForeground(styleSelfName, new Color(220, 53, 69));
        StyleConstants.setBold(styleSelfName, true);

        // 別人的名字:藍色 + 粗體
        styleOtherName = doc.addStyle("otherName", null);
        StyleConstants.setForeground(styleOtherName, new Color(13, 110, 253));
        StyleConstants.setBold(styleOtherName, true);

        // 聊天內容:預設黑色
        styleContent = doc.addStyle("content", null);
        StyleConstants.setForeground(styleContent, Color.BLACK);

        // 系統訊息(加入/離開):灰色斜體
        styleSystem = doc.addStyle("system", null);
        StyleConstants.setForeground(styleSystem, new Color(108, 117, 125));
        StyleConstants.setItalic(styleSystem, true);

        // 日期分隔線:深灰粗體,當作小標題
        styleDate = doc.addStyle("date", null);
        StyleConstants.setForeground(styleDate, new Color(73, 80, 87));
        StyleConstants.setBold(styleDate, true);
    }

    private void startReceiving() {
        Thread receiver = new Thread(() -> {
            try {
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                while (!socket.isClosed()) {
                    int type = dis.readInt();
                    String content = dis.readUTF();

                    if (type == TYPE_CHAT_MESSAGE) {
                        SwingUtilities.invokeLater(() -> {
                            appendStyled(content);
                            // 自動捲動到最新訊息
                            messageArea.setCaretPosition(messageArea.getDocument().getLength());
                        });
                    } else if (type == TYPE_USER_LIST) {
                        SwingUtilities.invokeLater(() -> {
                            userListModel.clear();
                            if (!content.isEmpty()) {
                                for (String user : content.split(",")) {
                                    userListModel.addElement(user);
                                }
                            }
                        });
                    }
                }
            } catch (IOException e) {
                // 與伺服器斷線
                SwingUtilities.invokeLater(() ->
                    appendStyled("【與伺服器斷線】")
                );
            }
        });
        receiver.setDaemon(true);
        receiver.start();
    }

    /**
     * 解析伺服器送來的單一行訊息並依片段套上不同顏色。
     *
     * 支援三種格式:
     *   1. 日期分隔線:以 "────" 開頭                    → 整行 styleDate
     *   2. 系統訊息: "[HH:mm:ss] 【XXX 加入/離開了聊天室】" → 時間 styleTime + 後段 styleSystem
     *   3. 聊天訊息: "[HH:mm:ss] name: message"          → 時間 + 名字(自己/別人不同色) + 內容
     */
    private void appendStyled(String message) {
        StyledDocument doc = messageArea.getStyledDocument();
        try {
            // 1) 日期分隔線
            if (message.startsWith("────")) {
                doc.insertString(doc.getLength(), message + "\n", styleDate);
                return;
            }

            // 嘗試切出 "[HH:mm:ss]" 前綴(長度固定 10)
            if (message.length() >= 10 && message.charAt(0) == '[' && message.charAt(9) == ']') {
                String timePart = message.substring(0, 10);   // [HH:mm:ss]
                String rest     = message.substring(10);      // " 後面..."

                doc.insertString(doc.getLength(), timePart, styleTime);

                // 2) 系統訊息:後段含「【...加入了聊天室】」或「【...離開了聊天室】」
                if (rest.contains("【") && (rest.contains("加入了聊天室") || rest.contains("離開了聊天室"))) {
                    doc.insertString(doc.getLength(), rest + "\n", styleSystem);
                    return;
                }

                // 3) 聊天訊息:找第一個 ": " 來切名字與內容
                int colonIdx = rest.indexOf(": ");
                if (colonIdx > 0) {
                    String namePart    = rest.substring(0, colonIdx);   // " name"(前面含空格)
                    String contentPart = rest.substring(colonIdx);      // ": content"

                    // 判斷是不是自己發的,套不同顏色
                    String trimmedName = namePart.trim();
                    Style nameStyle = trimmedName.equals(this.name) ? styleSelfName : styleOtherName;

                    doc.insertString(doc.getLength(), namePart, nameStyle);
                    doc.insertString(doc.getLength(), contentPart + "\n", styleContent);
                    return;
                }
            }

            // 後備:格式不符就整行用預設樣式
            doc.insertString(doc.getLength(), message + "\n", styleContent);
        } catch (BadLocationException e) {
            // 理論上 insertString(getLength(), ...) 不會落到非法位置;真出事印出來方便除錯
            e.printStackTrace();
        }
    }

    private void sendMessage(String message) {
        try {
            dos.writeInt(TYPE_SEND_MESSAGE);
            dos.writeUTF(message);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logout() {
        try {
            dos.writeInt(TYPE_LOGOUT);
            dos.flush();
            socket.close();
        } catch (IOException ignored) {}
    }
}
