package com.example.subscription.manager.services;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter createConnection() {
        // Создаем эмиттер (например, с таймаутом 30 минут = 1 800 000 миллисекунд)
        SseEmitter emitter = new SseEmitter(1_800_000L);

        // Добавляем клиента в наш список рассылки
        this.emitters.add(emitter);

        // ВАЖНО: Если клиент закрыл вкладку или соединение оборвалось,
        // мы должны удалить его эмиттер из списка, чтобы не отправлять сообщения «в пустоту»
        emitter.onCompletion(() -> this.emitters.remove(emitter));
        emitter.onTimeout(() -> this.emitters.remove(emitter));
        emitter.onError((e) -> this.emitters.remove(emitter));

        return emitter;
    }

    public void broadcastDeletion(UUID id) {
        // Создаем объект события строго по ТЗ
        var eventData = new DeletionEvent("obligation_deleted", id);

        // Проходим по всем подключенным клиентам
        for (SseEmitter emitter : emitters) {
            try {
                // Отправляем JSON-данные клиенту
                emitter.send(SseEmitter.event()
                        .name("obligation_deleted") // Имя события
                        .data(eventData));          // Тело события (Jackson сам превратит его в JSON)
            } catch (IOException e) {
                // Если отправить не удалось (клиент отключился, но мы еще не знали), удаляем его
                this.emitters.remove(emitter);
            }
        }
    }

    // Внутренний рекорд для структуры JSON
    public record DeletionEvent(String type, UUID id) {}
}
