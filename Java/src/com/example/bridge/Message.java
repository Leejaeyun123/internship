/* 1. 정의 : UDP와 TCP 사이를 오가는 한 건의 메시지를 담는 불변 데이터 꾸러미
 * 2. 존재 이유 : 네트워크 I/O Thread와 Bridge Thread가 Queue로 안전하게 건네주기 위해서. 로직을 담지 않고 운반만 담당
 * 3. 경계("여기까지가 한 단위/한 책임/ 한 흐름"을 정하는 선) : "한 줄(Line) = 한 메시지" 규칙을 따름. payload는 개행(줄 바꿈)으로 끝나는 한 줄 텍스트(송/수신 시 개행은 제거되어 전달)
 */

package com.example.bridge;

import java.net.InetSocketAddress;

public class Message { /* Message : 메시지 한 개를 나타내는 타입 / 네트워크 계층과 애플리케이션 계층 사이의 데이터 단위를 표현 */
    public enum Origin { /* Origin : 메시지가 어디서 왔는지 구분하는 값. UDP 또는 TCP만 허용 */
        UDP, TCP
    }

    private final Origin origin; /* 출처 표시. ex) TCP에서 읽었으면 Origin.TCP */
    private final String payload; /* 메시지 본문 */
    private final InetSocketAddress udpSender; /* UDP 발신자 주소(IP+포트). TCP에서는 의미가 없음 */
}