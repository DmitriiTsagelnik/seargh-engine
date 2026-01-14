package searchengine.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO для одного результата поиска.
 *
 * Используется как элемент списка `data` в SearchResponse.
 *
 * Содержит информацию о странице, необходимую для отображения пользователю:
 *  - URL сайта
 *  - имя сайта
 *  - путь страницы
 *  - заголовок страницы
 *  - сниппет текста с выделенными леммами
 *  - оценку релевантности страницы
 */
@Data
@AllArgsConstructor
public class SearchResult {

    /**
     * URL сайта.
     */
    private String site;

    /**
     * Человекочитаемое имя сайта.
     */
    private String siteName;

    /**
     * URI страницы относительно сайта.
     *
     * Используется для формирования полной ссылки:
     * site + uri
     */
    private String uri;

    /**
     * Заголовок страницы.
     */
    private String title;

    /**
     * Сниппет текста, содержащий
     * найденные леммы запроса.
     */
    private String snippet;

    /**
     * Оценка релевантности страницы.
     *
     * Используется для сортировки результатов поиска.
     */
    private float relevance;
}
