package com.jay.live_chat;

import com.jay.server.ChatServer;
import com.jay.ui.ChatEntryUI;
import com.jay.ui.Constant;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LiveChatApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(LiveChatApplication.class, args);
	}

	@Override
	public void run(String... args) {
		System.setProperty("java.awt.headless", "false");

		// 在背景執行緒啟動 Socket 伺服器
		ChatServer chatServer = new ChatServer();
		Thread serverThread = new Thread(() -> {
			try {
				chatServer.start(Constant.SERVER_PORT);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		serverThread.setDaemon(true);
		serverThread.start();

		// 啟動聊天入口界面
		ChatEntryUI chatEntryUI = new ChatEntryUI();
		chatEntryUI.showEntryUI();
	}
}
