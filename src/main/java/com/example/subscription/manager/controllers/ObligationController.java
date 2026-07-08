package com.example.subscription.manager.controllers;

import com.example.subscription.manager.dto.*;
import com.example.subscription.manager.models.Category;
import com.example.subscription.manager.models.Obligation;
import com.example.subscription.manager.models.Status;
import com.example.subscription.manager.services.ObligationService;
import com.example.subscription.manager.services.SseService;
import com.example.subscription.manager.utils.ObligationParser;
import com.example.subscription.manager.utils.PaymentParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/obligations")
@AllArgsConstructor
@Tag(name = "Обязательства", description = "Операции по управлению подписками и платежами")
public class ObligationController {
    private final ObligationService obligationService;
    private final ObligationParser obligationParser;
    private final PaymentParser paymentParser;
    private final SseService sseService;

    @PostMapping
    @Operation(
            summary = "Создать новое обязательство",
            description = "Принимает данные подписки или разового платежа. Если дата платежа в прошлом, автоматически выставит статус EXPIRED. Если в базе уже есть активное обязательство с таким же названием, создаст запись, но вернет warning."
    )
    @ApiResponse(responseCode = "201", description = "Обязательство успешно создано")
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации входящих данных",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))
    )
    public ResponseEntity<ObligationResponse> createObligation(@Valid @RequestBody ObligationCreateRequest obligationCreateRequest) {
        CreateObligationReturnValue result = obligationService.createObligation(obligationCreateRequest);

        ObligationResponse response = obligationParser.parseObligation(result.obligation(), result.warning());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Получить список обязательств",
            description = "Возвращает список всех обязательств с сортировкой по дате следующего платежа (по возрастанию). Поддерживает необязательную фильтрацию по статусу и категории. ПЕРЕД выдачей ответа автоматически переводит просроченные активные разовые платежи в статус EXPIRED (логика Lazy Expiry)."
    )
    @ApiResponse(responseCode = "200", description = "Список обязательств успешно получен")
    @ApiResponse(
            responseCode = "400",
            description = "Некорректные параметры фильтрации",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = org.springframework.http.ProblemDetail.class)
            )
    )
    public ResponseEntity<List<ObligationResponse>> getObligations(@RequestParam(name = "status", required = false)Status status,
                                                                   @RequestParam(name = "category", required = false)Category category) {
        var obligations = obligationService.getObligations(category, status);
        List<ObligationResponse> responses = new ArrayList<>();
        for (Obligation obligation : obligations) {
            ObligationResponse response = obligationParser.parseObligation(obligation, null);
            responses.add(response);
        }

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/upcoming")
    @Operation(
            summary = "Получить прогноз предстоящих расходов",
            description = "Возвращает все обязательства, дата платежа которых попадает в окно [today, today + N days]. " +
                    "Автоматически группирует суммы расходов по валютам (без конвертации) и формирует отдельный " +
                    "список renewal_alerts, содержащий только рекуррентные подписки, для отправки уведомлений в Telegram-бота."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Прогноз расходов успешно сформирован"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Некорректное значение количества дней",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = org.springframework.http.ProblemDetail.class)
            )
    )
    public ResponseEntity<UpcomingObligationsResponse> getUpcomingObligations(@RequestParam(name = "days", defaultValue = "7") @Min(0) Integer days) {
        var upcomingObligations = obligationService.getUpcomingObligations(days);
        List<ObligationResponse> obligations = new ArrayList<>();
        for (Obligation obligation : upcomingObligations) {
            ObligationResponse response = obligationParser.parseObligation(obligation, null);
            obligations.add(response);
        }
        Map<String, BigDecimal> totals = obligationService.countTotals(upcomingObligations);
        var upcomingSubscriptions = obligationService.getUpcomingSubscriptions(upcomingObligations);
        List<RenewalAlertResponse> renewalAlerts = new ArrayList<>();
        for (Obligation obligation : upcomingSubscriptions) {
            RenewalAlertResponse alert = obligationParser.parseObligation(obligation);
            renewalAlerts.add(alert);
        }
        UpcomingObligationsResponse response = new UpcomingObligationsResponse(obligations, totals, renewalAlerts);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/pay")
    @Operation(
            summary = "Зафиксировать факт оплаты обязательства",
            description = "Регистрирует успешную транзакцию и обновляет параметры обязательства. " +
                    "Для разовых платежей выставляет статус CANCELLED. " +
                    "Для рекуррентных подписок сдвигает дату следующего платежа вперед на 1 месяц, 3 месяца или 1 год " +
                    "с использованием originalSubscriptionStartDay для предотвращения накопления смещения дат."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Оплата успешно зафиксирована, дата обязательства обновлена"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Обязательство с указанным ID не найдено в базе данных",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = org.springframework.http.ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "422",
            description = "Бизнес-ошибка: оплата возможна только для обязательств со статусом ACTIVE",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = org.springframework.http.ProblemDetail.class)
            )
    )
    public ResponseEntity<PaymentResultResponse> pay(@PathVariable UUID id) {
        ServicePaymentResult result = obligationService.pay(id);
        PaymentResultResponse response = new PaymentResultResponse(obligationParser.parseObligation(result.obligation(), null),
                                                                   paymentParser.parsePayment(result.payment()));
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(
            summary = "Отменить обязательство/подписку",
            description = "Переводит обязательство в статус CANCELLED. Запись не удаляется физически из базы данных. " +
                    "Отменить можно только те обязательства, которые сейчас находятся в статусе ACTIVE."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Обязательство успешно отменено, статус изменен на CANCELLED"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Обязательство с указанным ID не найдено в системе",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = org.springframework.http.ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "422",
            description = "Бизнес-ошибка: невозможно отменить обязательство со статусом EXPIRED или CANCELLED",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = org.springframework.http.ProblemDetail.class)
            )
    )
    public ResponseEntity<ObligationResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(obligationParser.parseObligation(obligationService.cancel(id), null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить обязательство", description = "Физически удаляет обязательство и все связанные платежи (каскадно на уровне БД).")
    @ApiResponse(responseCode = "204", description = "Обязательство успешно удалено, тело ответа пустое")
    @ApiResponse(responseCode = "404", description = "Обязательство не найдено")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        obligationService.delete(id);
        sseService.broadcastDeletion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Подключиться к потоку уведомлений (SSE)",
            description = "Открывает постоянное HTTP-соединение (Server-Sent Events) для получения реактивных уведомлений в реальном времени. " +
                    "Используется фронтендом и Telegram-ботом для мгновенного обновления интерфейса при изменениях на бэкенде."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Соединение успешно установлено, поток открыт. Сервер будет транслировать событие obligation_deleted при удалении записей."
    )
    public SseEmitter connectToNotifications() {
        return sseService.createConnection();
    }

}
