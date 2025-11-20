package server;

import common.GameMsg;
import server.ClientHandler;
import server.RoomManager;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class GameServer extends JFrame {

    private JTextArea logArea;
    private JButton startBtn, stopBtn;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private boolean running = false;

    // 클라이언트 목록
    private Vector<ClientHandler> clients = new Vector<>();

    // 방 관리 매니저 추가
    public RoomManager roomManager = new RoomManager(this);

    public GameServer() {
        super("아브라카왓 게임 서버");

        setLayout(new BorderLayout());

        // ===== 로그 출력 =====
        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // ===== 컨트롤 버튼 =====
        JPanel bottom = new JPanel(new GridLayout(1, 2));
        startBtn = new JButton("서버 시작");
        stopBtn = new JButton("서버 종료");
        stopBtn.setEnabled(false);

        startBtn.addActionListener(e -> startServer(5555));
        stopBtn.addActionListener(e -> stopServer());

        bottom.add(startBtn);
        bottom.add(stopBtn);
        add(bottom, BorderLayout.SOUTH);

        // UI 설정
        setSize(500, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    // ============================================================
    // 서버 시작
    // ============================================================
    private void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            running = true;

            print("서버 시작됨. 포트: " + port);

            acceptThread = new Thread(() -> {
                while (running) {
                    try {
                        Socket socket = serverSocket.accept();
                        print("클라이언트 접속: " + socket.getInetAddress());

                        ClientHandler handler = new ClientHandler(socket, this);
                        clients.add(handler);
                        handler.start();

                    } catch (IOException e) {
                        if (running)
                            print("accept 오류: " + e.getMessage());
                    }
                }
            });

            acceptThread.start();

            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);

        } catch (IOException e) {
            print("서버 시작 실패: " + e.getMessage());
        }
    }

    // ============================================================
    // 서버 종료
    // ============================================================
    private void stopServer() {
        try {
            running = false;

            print("서버 종료 중...");

            if (serverSocket != null) serverSocket.close();

            for (ClientHandler ch : clients) {
                ch.close();
            }
            clients.clear();

            print("서버 종료 완료.");

            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);

        } catch (IOException e) {
            print("서버 종료 오류: " + e.getMessage());
        }
    }

    // ============================================================
    // 클라이언트 제거
    // ============================================================
    public synchronized void removeClient(ClientHandler handler) {
        clients.remove(handler);
        print("Client 제거됨. 현재 인원: " + clients.size());
    }

    // ============================================================
    // 로그
    // ============================================================
    public synchronized void print(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
        System.out.println(msg);
    }

    // ============================================================
    // 서버 전체 방송
    // ============================================================
    public synchronized void broadcastToAll(GameMsg msg) {
        for (ClientHandler ch : clients) {
            ch.send(msg);
        }
    }
    public void sendRoomListToAll() {
        String list = roomManager.getRoomListText();
        GameMsg msg = new GameMsg(GameMsg.ROOM_LIST, "SERVER", list);

        for (ClientHandler ch : clients) {
            ch.send(msg);
        }
    }


    public static void main(String[] args) {
        new GameServer();
    }
}