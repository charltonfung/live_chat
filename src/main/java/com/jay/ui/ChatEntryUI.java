package com.jay.ui;

import javax.swing.*;
import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ChatEntryUI {
    public void showEntryUI() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Live Chat Entry");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);
            frame.setLocationRelativeTo(null); // Center the frame

            // Set the main panel
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BorderLayout());
            mainPanel.setBackground(new Color(240, 240, 240));

            // Title
            JLabel titleLabel = new JLabel("Welcome to Live Chat", SwingConstants.CENTER);
            titleLabel.setFont(new Font("楷體", Font.BOLD, 18));
            titleLabel.setForeground(new Color(70, 130, 180));
            titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
            mainPanel.add(titleLabel, BorderLayout.NORTH);

            // Input panel
            JPanel inputPanel = new JPanel();
            inputPanel.setLayout(new GridLayout(2, 1, 10, 10));
            inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

            // Name input
            JTextField nameField = new JTextField();
            nameField.setFont(new Font("楷體", Font.PLAIN, 14));
            nameField.setBorder(BorderFactory.createTitledBorder("Your Name"));
            inputPanel.add(nameField);

            mainPanel.add(inputPanel, BorderLayout.CENTER);

            // Button panel
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

            JButton enterButton = new JButton("Enter");
            enterButton.setFont(new Font("楷體", Font.BOLD, 14));
            enterButton.setBackground(new Color(70, 130, 180));
            enterButton.setForeground(Color.WHITE);
            enterButton.setFocusPainted(false);

            JButton exitButton = new JButton("Exit");
            exitButton.setFont(new Font("楷體", Font.BOLD, 14));
            exitButton.setBackground(new Color(220, 20, 60));
            exitButton.setForeground(Color.WHITE);
            exitButton.setFocusPainted(false);

            buttonPanel.add(enterButton);
            buttonPanel.add(exitButton);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

            // Add action listeners
            enterButton.addActionListener(e -> {
                String name = nameField.getText().trim();
                nameField.setText("");
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please enter your name.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    Socket socket = login(name);
                    frame.dispose();
                    ClientChatUI chatUI = new ClientChatUI(socket, name);
                    chatUI.showChatUI();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "無法連線到伺服器：" + ex.getMessage(), "連線失敗", JOptionPane.ERROR_MESSAGE);
                }
            });

            // 在名字輸入框按 Enter 等同點 Enter 按鈕,免去切去滑鼠的麻煩
            nameField.addActionListener(e -> enterButton.doClick());

            exitButton.addActionListener(e -> {
                int response = JOptionPane.showConfirmDialog(frame, "Are you sure you want to exit?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.YES_OPTION) {
                    frame.dispose();
                    System.exit(0);
                }
            });

            frame.add(mainPanel);
            frame.setVisible(true);
        });
    }

    private Socket login(String name) throws IOException {
        Socket socket = new Socket(Constant.SERVER_IP, Constant.SERVER_PORT);
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        dos.writeInt(1);
        dos.writeUTF(name);
        dos.flush();
        return socket;
    }
}