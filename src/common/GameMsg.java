package common;

import javax.swing.*;
import java.io.Serializable;

/**
 * 클라이언트 ↔ 서버 간 전송되는 공통 메시지 객체
 * mode 값으로 메시지의 종류를 구분한다.
 */
public class GameMsg implements Serializable {

    // =============== 메시지 타입 (mode) 정의 ===============
    public static final int LOGIN = 0x01;          // 로그인 요청
    public static final int LOGIN_OK = 0x02;       // 로그인 성공

    public static final int ROOM_LIST = 0x10;      // 로비: 방 목록 전달
    public static final int ROOM_CREATE = 0x11;    // 방 생성 요청
    public static final int ROOM_ENTER = 0x12;     // 방 입장 요청
    public static final int ROOM_UPDATE = 0x13;    // 방 내 플레이어 목록 갱신
    public static final int ROOM_LEAVE = 0x14;

    public static final int CHAT = 0x20;           // 채팅
    public static final int CHAT_SYSTEM = 0x21;    // 시스템 메시지

    public static final int GAME_START = 0x30;     // 게임 시작
    public static final int GAME_STATE = 0x31;     // 게임 전체 상태 동기화
    public static final int CAST_SPELL = 0x32;     // 주문 외치기
    public static final int SPELL_RESULT = 0x33;   // 주문 결과

    public static final int DISCONNECT = 0xFF;     // 종료/연결 끊김


    // =============== 공통 필드 ===============
    public int mode;             // 메시지 타입
    public String user;          // 보낸 사람 닉네임
    public String text;          // 문자열 메시지

    // 게임 데이터
    public int[] hp;             // 생명력 4명
    public int[][] stones;       // 마법의 돌 정보 (각 플레이어 5개)
    public int remainStones;     // 남은 돌

    // 이미지 메시지를 위해 (필요하면)
    public ImageIcon image;

    // =============== 생성자 ===============
    public GameMsg(int mode, String user) {
        this.mode = mode;
        this.user = user;
    }

    public GameMsg(int mode, String user, String text) {
        this.mode = mode;
        this.user = user;
        this.text = text;
    }

    public GameMsg(int mode, String user, String text, ImageIcon img) {
        this.mode = mode;
        this.user = user;
        this.text = text;
        this.image = img;
    }
}
