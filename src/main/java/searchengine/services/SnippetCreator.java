package searchengine.services;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SnippetCreator {

    // Максимальная длина сниппета в символах
    private static final int MAX_SNIPPET_CHARS = 200;

    /**
     * Создаёт сниппет (короткий фрагмент текста) для поискового результата.
     *
     * @param htmlContent HTML-код страницы
     * @param lemmas      набор лемм, которые нужно выделить
     * @return текст сниппета с выделенными леммами
     */
    public String createSnippet(String htmlContent, Set<String> lemmas) {
        // Проверка на пустой контент или пустой набор лемм
        if (htmlContent == null || htmlContent.isBlank() || lemmas.isEmpty()) return "";

        // Извлекаем чистый текст из HTML
        String text = Jsoup.parse(htmlContent).text();

        // Переводим текст в нижний регистр для поиска
        String lowerText = text.toLowerCase();

        // Находим первую встречающуюся лемму в тексте
        int firstIndex = -1;
        String foundLemma = null;
        for (String lemma : lemmas) {
            int idx = lowerText.indexOf(lemma.toLowerCase());
            if (idx >= 0 && (firstIndex == -1 || idx < firstIndex)) {
                firstIndex = idx;  // сохраняем индекс первой найденной леммы
                foundLemma = lemma;
            }
        }

        // Определяем границы сниппета
        int start = 0;
        int end = Math.min(text.length(), MAX_SNIPPET_CHARS);

        if (firstIndex >= 0) {
            // Центрируем сниппет вокруг найденной леммы
            start = Math.max(0, firstIndex - MAX_SNIPPET_CHARS / 2);
            end = Math.min(text.length(), start + MAX_SNIPPET_CHARS);
        }

        // Получаем подстроку для сниппета
        String snippet = text.substring(start, end);

        // Выделяем все леммы жирным (<b>)
        for (String lemma : lemmas) {
            snippet = snippet.replaceAll(
                    "(?iu)(" + Pattern.quote(lemma) + ")", // игнорируем регистр
                    "<b>$1</b>"                             // оборачиваем в <b>
            );
        }

        // Добавляем многоточие, если сниппет не с начала или конца текста
        if (start > 0) snippet = "..." + snippet;
        if (end < text.length()) snippet = snippet + "...";

        return snippet;
    }
}

