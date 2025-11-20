package server;

import common.GameMsg;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {

    private Socket socket;
    private GameServer server;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    private boolean connected = true;

    private String nick = "UNKNOWN";
    private Room currentRoom = null;

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            // 반드시 서버는 OIS → OOS 순서!!
            in  = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            server.print("클라이언트 접속: " + socket.getInetAddress());

            while (connected) {
                Object obj = in.readObject();

                if (obj instanceof GameMsg msg) {
                    handleMessage(msg);
                }
            }

        } catch (EOFException e) {
            server.print("클라이언트 정상 종료: " + nick);
        } catch (Exception e) {
            server.print("오류 발생 (" + nick + "): " + e.getMessage());
        } finally {
            close();
        }
    }

    // ============================================================
    // 메시지 처리
    // ============================================================
    private void handleMessage(GameMsg msg) {

        switch (msg.mode) {

            // ---------------------------
            // 로그인
            // ---------------------------
            case GameMsg.LOGIN -> {
                nick = msg.user;
                server.print("[LOGIN] " + nick);

                GameMsg ok = new GameMsg(GameMsg.LOGIN_OK, "SERVER");
                send(ok);

                sendRoomList();  // 로비 방 목록 전달
            }

            // ---------------------------
            // 방 생성
            // ---------------------------
            case GameMsg.ROOM_CREATE -> {
                Room room = server.roomManager.createRoom(msg.text);
                enterRoom(room);
            }

            // ---------------------------
            // 방 입장
            // ---------------------------
            case GameMsg.ROOM_ENTER -> {
                Room room = server.roomManager.getRoom(msg.text);
                if (room != null) enterRoom(room);
            }

            // ---------------------------
            // 방 나가기
            // ---------------------------
            case GameMsg.ROOM_LEAVE -> leaveRoom();

            // ---------------------------
            // 채팅
            // ---------------------------
            case GameMsg.CHAT -> {
                if (currentRoom != null) {
                    currentRoom.broadcast(new GameMsg(GameMsg.CHAT, nick, msg.text));
                }
            }
            case GameMsg.GAME_START -> {
                if (currentRoom != null) {
                    currentRoom.broadcast(new GameMsg(GameMsg.GAME_START, nick, null));
                }
            }

            default -> server.print("[UNKNOWN MODE] = " + msg.mode);
        }
    }

    // ============================================================
    // 방 입장
    // ============================================================
    private void enterRoom(Room room) {

        // 기존 방에서 제거
        leaveRoom();

        currentRoom = room;
        room.addPlayer(this);

        server.print("[입장] " + nick + " → " + room.roomName);

        room.sendPlayerList();          // 방 내 플레이어 갱신
        server.sendRoomListToAll();     // 로비 갱신
    }

    // ============================================================
    // 방 나가기
    // ============================================================
    private void leaveRoom() {
        if (currentRoom == null) return;

        server.print("[퇴장] " + nick + " ← " + currentRoom.roomName);

        currentRoom.removePlayer(this);

        // 방이 비었으면 삭제
        if (currentRoom.players.isEmpty()) {
            server.roomManager.removeRoom(currentRoom);
        } else {
            currentRoom.sendPlayerList();
        }

        currentRoom = null;

        // 로비 갱신
        server.sendRoomListToAll();
    }

    // ============================================================
    // 로비에 방 목록 전달
    // ============================================================
    private void sendRoomList() {
        String list = server.roomManager.getRoomListText();
        GameMsg msg = new GameMsg(GameMsg.ROOM_LIST, "SERVER", list);
        send(msg);
    }

    // ============================================================
    // 서버 → 이 클라이언트 메시지 보내기
    // ============================================================
    public synchronized void send(GameMsg msg) {
        try {
            if (!connected) return;
            out.writeObject(msg);
            out.flush();
        } catch (Exception e) {
            server.print("전송 실패: " + nick);
            close();
        }
    }

    // ============================================================
    // 연결 종료
    // ============================================================
    public void close() {
        if (!connected) return;
        connected = false;

        leaveRoom();                // 방 정리
        server.removeClient(this);  // 서버 목록에서 제거

        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}

        server.print("Client 종료: " + nick);
    }

    public String getNick() { return nick; }
}
