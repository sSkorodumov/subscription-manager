package com.example.subscription.manager;

import com.example.subscription.manager.dto.ObligationCreateRequest;
import com.example.subscription.manager.models.Category;
import com.example.subscription.manager.models.Obligation;
import com.example.subscription.manager.models.Recurrence;
import com.example.subscription.manager.models.Status;
import com.example.subscription.manager.repositories.ObligationRepository;
import com.example.subscription.manager.repositories.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ObligationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ObligationRepository obligationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    public void cleanDatabase() {
        paymentRepository.deleteAll();
        obligationRepository.deleteAll();
    }

    @Test
    public void shouldAutomaticallyReturnWarningWhenDuplicateTitleExists() throws Exception {
        // 1. Честно создаем и сохраняем первое обязательство в базу данных
        var existingObligation = com.example.subscription.manager.models.Obligation.builder()
                .title("Netflix") // Крупный регистр
                .amount(new BigDecimal("14.89"))
                .currency("USD")
                .category(Category.SUBSCRIPTION)
                .recurrence(Recurrence.MONTHLY)
                .nextPaymentDate(LocalDate.now().plusDays(10))
                .status(com.example.subscription.manager.models.Status.ACTIVE)
                .build();
        obligationRepository.save(existingObligation);

        // 2. Готовим запрос на создание ДУБЛИКАТА маленькими буквами
        ObligationCreateRequest duplicateRequest = new ObligationCreateRequest(
                "netflix", // Маленький регистр
                new BigDecimal("9.99"),
                "USD",
                Category.SUBSCRIPTION,
                Recurrence.MONTHLY,
                LocalDate.now().plusDays(15)
        );

        // 3. Отправляем реальный HTTP-запрос. Сервис сам пойдет в базу H2, найдет дубликат и создаст warning!
        mockMvc.perform(post("/obligations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isCreated()) // База запишет объект
                .andExpect(jsonPath("$.warning").value("Активное обязательство с таким названием уже существует")); // Проверяем честный warning!
    }

    @Test
    public void shouldApplyLazyExpiryOnlyToSingleObligations() throws Exception {
        LocalDate pastDate = LocalDate.now().minusDays(5); // Дата в прошлом

        // 1. Создаем просроченную РЕКУРРЕНТНУЮ подписку (Должна остаться ACTIVE)
        var pastSubscription = Obligation.builder()
                .title("Past Subscription")
                .amount(new BigDecimal("10.00"))
                .currency("USD")
                .category(Category.SUBSCRIPTION)
                .recurrence(Recurrence.MONTHLY)
                .nextPaymentDate(pastDate)
                .status(Status.ACTIVE)
                .build();

        // 2. Создаем просроченный РАЗОВЫЙ платеж (Должен уйти в EXPIRED)
        var pastSingleBill = Obligation.builder()
                .title("Past Single Bill")
                .amount(new BigDecimal("50.00"))
                .currency("RUB")
                .category(Category.BILL)
                .recurrence(null) // Разовый
                .nextPaymentDate(pastDate)
                .status(Status.ACTIVE)
                .build();

        obligationRepository.save(pastSubscription);
        obligationRepository.save(pastSingleBill);

        // 3. Вызываем эндпоинт получения списка всех обязательств
        mockMvc.perform(get("/obligations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Проверяем, что в полученном JSON подписка осталась ACTIVE
                .andExpect(jsonPath("$[?(@.title == 'Past Subscription')].status").value("ACTIVE"))
                // Проверяем, что разовый платеж перевелся лениво в EXPIRED
                .andExpect(jsonPath("$[?(@.title == 'Past Single Bill')].status").value("EXPIRED"));
    }

    @Test
    public void shouldCorrectlyShiftDatesForEachRecurrenceValue() throws Exception {
        LocalDate baseDate = LocalDate.now();

        // 1. Создаем и сохраняем 4 разных обязательства в тестовую БД
        var monthlySub = com.example.subscription.manager.models.Obligation.builder()
                .title("Monthly Sub").amount(new BigDecimal("10")).currency("USD")
                .category(Category.SUBSCRIPTION).recurrence(Recurrence.MONTHLY)
                .nextPaymentDate(baseDate).status(com.example.subscription.manager.models.Status.ACTIVE)
                .originalSubscriptionStartDay(baseDate.getDayOfMonth()).build();

        var quarterlySub = com.example.subscription.manager.models.Obligation.builder()
                .title("Quarterly Sub").amount(new BigDecimal("20")).currency("USD")
                .category(Category.SUBSCRIPTION).recurrence(Recurrence.QUARTERLY)
                .nextPaymentDate(baseDate).status(com.example.subscription.manager.models.Status.ACTIVE)
                .originalSubscriptionStartDay(baseDate.getDayOfMonth()).build();

        var yearlySub = com.example.subscription.manager.models.Obligation.builder()
                .title("Yearly Sub").amount(new BigDecimal("30")).currency("USD")
                .category(Category.SUBSCRIPTION).recurrence(Recurrence.YEARLY)
                .nextPaymentDate(baseDate).status(com.example.subscription.manager.models.Status.ACTIVE)
                .originalSubscriptionStartDay(baseDate.getDayOfMonth()).build();

        var singleBill = com.example.subscription.manager.models.Obligation.builder()
                .title("Single Bill").amount(new BigDecimal("40")).currency("RUB")
                .category(Category.BILL).recurrence(null) // Разовое
                .nextPaymentDate(baseDate).status(com.example.subscription.manager.models.Status.ACTIVE).build();

        monthlySub = obligationRepository.save(monthlySub);
        quarterlySub = obligationRepository.save(quarterlySub);
        yearlySub = obligationRepository.save(yearlySub);
        singleBill = obligationRepository.save(singleBill);

        // 2. Тестируем MONTHLY (+1 месяц)
        mockMvc.perform(post("/obligations/" + monthlySub.getId() + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obligation.nextPaymentDate").value(baseDate.plusMonths(1).toString()))
                .andExpect(jsonPath("$.payment.amount").value(10));

        // 3. Тестируем QUARTERLY (+3 месяца)
        mockMvc.perform(post("/obligations/" + quarterlySub.getId() + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obligation.nextPaymentDate").value(baseDate.plusMonths(3).toString()));

        // 4. Тестируем YEARLY (+1 год)
        mockMvc.perform(post("/obligations/" + yearlySub.getId() + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obligation.nextPaymentDate").value(baseDate.plusYears(1).toString()));

        // 5. Тестируем NULL (Перевод в CANCELLED)
        mockMvc.perform(post("/obligations/" + singleBill.getId() + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obligation.status").value("CANCELLED"));
    }

    @Test
    public void shouldCorrectlyHandle31stDayLeapMonthShift() throws Exception {
        // --- Сценарий 1: Обычный 2026 год (31 января -> 28 февраля -> 31 марта) ---
        LocalDate january31Of2026 = LocalDate.of(2026, 1, 31);
        var sub2026 = com.example.subscription.manager.models.Obligation.builder()
                .title("Sub 2026").amount(new BigDecimal("15")).currency("USD")
                .category(Category.SUBSCRIPTION).recurrence(Recurrence.MONTHLY)
                .nextPaymentDate(january31Of2026).status(com.example.subscription.manager.models.Status.ACTIVE)
                .originalSubscriptionStartDay(31).build();

        sub2026 = obligationRepository.save(sub2026);

        mockMvc.perform(post("/obligations/" + sub2026.getId() + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obligation.nextPaymentDate").value("2026-02-28"));

        mockMvc.perform(post("/obligations/" + sub2026.getId() + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obligation.nextPaymentDate").value("2026-03-31"));


        // --- Сценарий 2: Високосный 2028 год (31 января -> 29 февраля -> 31 марта) ---
        LocalDate january31Of2028 = LocalDate.of(2028, 1, 31); // 2028 — високосный год!
        var sub2028 = com.example.subscription.manager.models.Obligation.builder()
                .title("Sub 2028").amount(new BigDecimal("25")).currency("USD")
                .category(Category.SUBSCRIPTION).recurrence(Recurrence.MONTHLY)
                .nextPaymentDate(january31Of2028).status(com.example.subscription.manager.models.Status.ACTIVE)
                .originalSubscriptionStartDay(31).build();

        sub2028 = obligationRepository.save(sub2028);

        // Первая оплата: Сдвиг на високосный февраль (должно быть строго 29 число!)
        mockMvc.perform(post("/obligations/" + sub2028.getId() + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obligation.nextPaymentDate").value("2028-02-29"));

        // Вторая оплата: Возврат из високосного февраля на оригинальное 31 марта
        mockMvc.perform(post("/obligations/" + sub2028.getId() + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obligation.nextPaymentDate").value("2028-03-31"));
    }

    @Test
    public void shouldReturn422WhenPayingOrCancelingNonActiveObligation() throws Exception {
        // 1. Создаем два неактивных обязательства в базе данных
        var cancelledObligation = com.example.subscription.manager.models.Obligation.builder()
                .title("Already Cancelled").amount(new BigDecimal("10")).currency("USD")
                .category(Category.SUBSCRIPTION).recurrence(Recurrence.MONTHLY)
                .nextPaymentDate(LocalDate.now()).status(com.example.subscription.manager.models.Status.CANCELLED)
                .build();

        var expiredObligation = com.example.subscription.manager.models.Obligation.builder()
                .title("Already Expired").amount(new BigDecimal("20")).currency("USD")
                .category(Category.SUBSCRIPTION).recurrence(Recurrence.MONTHLY)
                .nextPaymentDate(LocalDate.now()).status(com.example.subscription.manager.models.Status.EXPIRED)
                .build();

        cancelledObligation = obligationRepository.save(cancelledObligation);
        expiredObligation = obligationRepository.save(expiredObligation);

        // 2. Тест: Попытка ОПЛАТИТЬ отмененную подписку -> Ожидаем 422
        mockMvc.perform(post("/obligations/" + cancelledObligation.getId() + "/pay"))
                .andExpect(status().isUnprocessableEntity()); // Статус 422

        // 3. Тест: Попытка ОТМЕНИТЬ уже истекшую подписку -> Ожидаем 422
        mockMvc.perform(patch("/obligations/" + expiredObligation.getId() + "/cancel"))
                .andExpect(status().isUnprocessableEntity()); // Статус 422
    }




}
