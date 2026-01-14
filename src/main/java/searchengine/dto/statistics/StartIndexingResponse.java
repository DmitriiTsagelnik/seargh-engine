package searchengine.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO для ответа REST API при запуске индексации сайта.
 *
 * Используется контроллером индексации
 * для передачи клиенту информации о статусе запроса.
 *
 * Содержит:
 * - флаг успешности выполнения запроса
 * - сообщение об ошибке (если индексация не удалась)
 */
@Data
@AllArgsConstructor
public class StartIndexingResponse {

    /**
     * Флаг успешности запуска индексации.
     *
     * true  — индексация успешно запущена
     * false — произошла ошибка
     */
    private boolean result;

    /**
     * Сообщение об ошибке.
     *
     * Используется, если `result` = false,
     * чтобы клиент понял причину сбоя.
     */
    private String error;
}
