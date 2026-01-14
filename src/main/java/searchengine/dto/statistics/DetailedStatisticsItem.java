package searchengine.dto.statistics;

import lombok.Data;

/**
 * DTO для хранения детализированной статистики по одному сайту.
 *
 * Используется как часть StatisticsData
 * и передаётся клиенту в ответе REST API.
 *
 * Позволяет отображать подробные данные
 * о каждом сайте:
 *  - URL и имя
 *  - статус индексации
 *  - время последнего обновления
 *  - ошибки при индексации
 *  - количество страниц и лемм
 */
@Data
public class DetailedStatisticsItem {

    /**
     * URL сайта.
     */
    private String url;

    /**
     * Человекочитаемое имя сайта.
     */
    private String name;

    /**
     * Текущий статус индексации.
     * Например: "INDEXING", "INDEXED", "FAILED"
     */
    private String status;

    /**
     * Время последнего обновления статуса
     * в формате Unix timestamp (миллисекунды).
     */
    private long statusTime;

    /**
     * Сообщение об ошибке индексации,
     * если она произошла.
     */
    private String error;

    /**
     * Количество страниц на сайте.
     */
    private int pages;

    /**
     * Количество лемм на сайте.
     */
    private int lemmas;
}
