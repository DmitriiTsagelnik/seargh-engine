package searchengine.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO для результата загрузки (fetch) веб-страницы.
 *
 * Используется внутри сервиса индексации,
 * чтобы передать информацию о странице после её загрузки.
 *
 * Содержит:
 *  - HTTP статус-код ответа
 *  - HTML-код страницы
 *  - путь страницы относительно сайта
 */
@Data
@AllArgsConstructor
public class FetchResult {

    /**
     * HTTP статус-код, полученный при запросе страницы.
     *
     * Например:
     * 200 — успешно
     * 404 — страница не найдена
     * 500 — ошибка сервера
     */
    private int statusCode;

    /**
     * HTML-содержимое страницы.
     *
     * Используется для дальнейшего извлечения текста,
     * заголовка и лемм.
     */
    private String html;

    /**
     * Путь страницы относительно сайта.
     *
     * Например:
     * "/" или "/about"
     */
    private String path;
}
