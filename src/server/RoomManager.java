package server;

import common.GameMsg;
import java.util.Vector;

public class RoomManager {

    private Vector<Room> rooms;
    private GameServer server;   // 로그 출력 등을 위해

    private int nextRoomId = 1;

    public RoomManager(GameServer server) {
        this.server = server;
        rooms = new Vector<>();
    }
    // 방 생성
    public Room createRoom(String roomName) {
        int id = nextRoomId++;
        Room room = new Room(id, roomName);
        rooms.add(room);

        server.print("[방 생성] " + roomName + " (ID=" + id + ")");
        return room;
    }
    // 방 제거
    public void removeRoom(Room room) {
        rooms.remove(room);
        server.print("[방 삭제] " + room.roomName + " (ID=" + room.roomId + ")");
    }
    // 방 찾기 (이름으로)
    public Room getRoom(String roomName) {
        for (Room r : rooms) {
            if (r.roomName.equals(roomName)) return r;
        }
        return null;
    }
    // 방 찾기 (ID로)
    public Room getRoomById(int id) {
        for (Room r : rooms) {
            if (r.roomId == id) return r;
        }
        return null;
    }
    // 로비용: 전체 방 이름 목록 만들기 → "방1|방2|방3"
    public String getRoomListText() {
        if (rooms.isEmpty()) return ""; // 빈 문자열 보내면 로비에서 리스트 없음 표시 가능

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rooms.size(); i++) {
            sb.append(rooms.get(i).roomName);
            if (i < rooms.size() - 1) sb.append("|");
        }
        return sb.toString();
    }
    // 로비 전체에 방 목록 갱신 메시지 보내기
    public void broadcastRoomList() {
        String list = getRoomListText();
        GameMsg msg = new GameMsg(GameMsg.ROOM_LIST, "SERVER", list);
        server.broadcastToAll(msg);
    }
}