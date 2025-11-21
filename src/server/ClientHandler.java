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
            in  = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());

            server.print("클라이언트 접속: " + socket.getInetAddress());

            while (connected) {
                Object obj = in.readObject();

                if (obj instanceof GameMsg msg) {
                    handleMessage(msg);
                }
            }

        } catch (ClassNotFoundException e) {
            server.print("알 수 없는 객체 수신 오류> " + e.getMessage());
        } catch (EOFException e) {
            server.print("클라이언트 정상 종료> " + nick);
        } catch (IOException e) {// 네트워크 끊김, 소켓 오류
            server.print("포트가 이미 사용중> " + e.getMessage());
        } finally {
            close();
        }
    }
    // 메시지 처리
    private void handleMessage(GameMsg msg) {

        switch (msg.mode) {
            // 로그인
            case GameMsg.LOGIN -> {
                nick = msg.user;
                server.print("[LOGIN] " + nick);

                GameMsg ok = new GameMsg(GameMsg.LOGIN_OK, "SERVER");
                send(ok);

                sendRoomList();  // 로비 방 목록 전달
            }
            // 방 생성
            case GameMsg.ROOM_CREATE -> {
                Room room = server.roomManager.createRoom(msg.text);
                enterRoom(room);
            }
            // 방 입장
            case GameMsg.ROOM_ENTER -> {
                Room room = server.roomManager.getRoom(msg.text);
                if (room != null) enterRoom(room);
            }
            // 방 나가기
            case GameMsg.ROOM_LEAVE -> leaveRoom();
            // 채팅
            case GameMsg.CHAT -> {
                if (currentRoom != null) {
                    currentRoom.broadcast(new GameMsg(GameMsg.CHAT, nick, msg.text));
                }
            }
            // 게임 시작
            case GameMsg.GAME_START -> {
                if (currentRoom != null) {
                    currentRoom.broadcast(new GameMsg(GameMsg.GAME_START, nick, null));
                }
            }

            default -> server.print("[UNKNOWN MODE] = " + msg.mode);
        }
    }
    // 방 입장
    private void enterRoom(Room room) {

        // 기존 방에서 제거
        leaveRoom();

        currentRoom = room;
        room.addPlayer(this);

        server.print("[입장] " + nick + " → " + room.roomName);

        room.sendPlayerList();          // 방 내 플레이어 갱신
        server.sendRoomListToAll();     // 로비 갱신
    }
    // 방 나가기
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
    // 로비에 방 목록 전달
    private void sendRoomList() {
        String list = server.roomManager.getRoomListText();
        GameMsg msg = new GameMsg(GameMsg.ROOM_LIST, "SERVER", list);
        send(msg);
    }
    // 서버 → 클라이언트 메시지 보내기
    public synchronized void send(GameMsg msg) {
        try {
            if (!connected) return;
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            server.print("서버 객체 전송 오류 (" + nick + ")> " + e.getMessage());
            close();
        }
    }
    // 연결 종료
    public void close() {
        if (!connected) return;
        connected = false;

        leaveRoom();  // 방 정리
        server.removeClient(this);  // 서버 목록에서 제거

        // 스트림/소켓 정리
        try {
            if (in != null) in.close();
        } catch (IOException e) {
            server.print("입력 스트림 종료 오류 (" + nick + ")> " + e.getMessage());
        }

        try {
            if (out != null) out.close();
        } catch (IOException e) {
            server.print("출력 스트림 종료 오류 (" + nick + ")> " + e.getMessage());
        }

        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            server.print("소켓 종료 오류 (" + nick + ")> " + e.getMessage());
        }

        server.print("Client 종료: " + nick);
    }

    public String getNick() { return nick; }
}
