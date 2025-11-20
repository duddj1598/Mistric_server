package server;

import common.GameMsg;
import java.util.Vector;

public class Room {

    public int roomId;
    public String roomName;

    public Vector<ClientHandler> players;

    public Room(int id, String name) {
        roomId = id;
        roomName = name;
        players = new Vector<>();
    }

    public void addPlayer(ClientHandler ch) {
        players.add(ch);
    }

    public void removePlayer(ClientHandler ch) {
        players.remove(ch);
    }

    public boolean isEmpty() { return players.isEmpty(); }

    public String getPlayerNames() {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<players.size(); i++) {
            sb.append(players.get(i).getNick());
            if (i < players.size() - 1) sb.append("|");
        }
        return sb.toString();
    }

    public void broadcast(GameMsg msg) {
        for (ClientHandler ch : players) {
            ch.send(msg);
        }
    }

    public void sendPlayerList() {
        String list = getPlayerNames();
        GameMsg msg = new GameMsg(GameMsg.ROOM_UPDATE, "SERVER", list);
        broadcast(msg);
    }
}