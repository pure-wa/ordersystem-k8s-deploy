package com.beyond.ordersystem.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

@Component
public class SseEmitterRegistry {
    // SseEmitter 연결된 사용자 정보(ip, macaddress 정보 등)를 의미
    // ConcurrentHashMap은 Thread-Safe한 map(동시성 이수 발생x)
    private Map<String, SseEmitter> emitterMap = new HashMap<>();


    //        사용자가 로그아웃(새로고침)후에 다시 화면에 들어왔을때 알림메시지가 남앙있으려면 DB에 추가적으로 저장됨

    public void addSseEmitter(String email, SseEmitter sseEmitter) {
    emitterMap.put(email, sseEmitter);
    }

    public void removeEmitter(String email) {
    emitterMap.remove(email);

    }
    public SseEmitter getEmitter(String email) {
        return this.emitterMap.get(email);
    }


}