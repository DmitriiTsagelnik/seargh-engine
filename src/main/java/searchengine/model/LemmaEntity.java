package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Сущность LemmaEntity описывает лемму — нормализованную форму слова.
 *
 * Леммы используются поисковым движком для:
 *  - анализа текстов страниц
 *  - подсчёта частот слов
 *  - построения поискового индекса
 *
 * Каждая лемма привязана к конкретному сайту,
 * так как одно и то же слово на разных сайтах
 * имеет независимую статистику.
 */
@Entity
/**
 * Уникальное ограничение гарантирует,
 * что в рамках одного сайта каждая лемма
 * хранится только один раз.
 *
 * Это критически важно для корректного
 * подсчёта частоты и поиска.
 */
@Table(name = "lemma", uniqueConstraints = {@UniqueConstraint(columnNames = {"lemma", "site_id"})})
@Getter
@Setter
@NoArgsConstructor
public class LemmaEntity {

    /**
     * Уникальный идентификатор леммы в базе данных.
     * Генерируется автоматически.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Сайт, к которому относится данная лемма.
     *
     * Связь Many-to-One:
     *  - у одного сайта может быть множество лемм
     *  - лемма не существует вне контекста сайта
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    /**
     * Текст леммы — нормализованная форма слова.
     *
     * Примеры:
     * "бегал", "бегу" → "бег"
     * "страницы" → "страница"
     */
    @Column(name = "lemma", length = 255)
    private String lemma;

    /**
     * Частота леммы на сайте.
     *
     * Показывает, на скольких страницах сайта
     * встречается данная лемма.
     *
     * Используется:
     * - для фильтрации слишком частых слов
     * - при расчёте релевантности страниц
     */
    @Column(nullable = false)
    private int frequency;
}
