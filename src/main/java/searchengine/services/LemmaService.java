package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LemmaService {

    // Используем библиотеку Lucene для морфологического анализа русского языка
    private final LuceneMorphology luceneMorphology;

    /**
     * Конструктор инициализирует морфологический анализатор для русского языка
     */
    public LemmaService() throws IOException {
        this.luceneMorphology = new RussianLuceneMorphology();
    }

    /**
     * Основной метод: извлекает леммы из текста и считает их частоту
     *
     * @param text входной текст (HTML или обычный)
     * @return Map<лемма, количество вхождений>
     */
    public Map<String, Integer> getLemmasWithCount(String text) {
        Map<String, Integer> lemmaCount = new HashMap<>();
        if (text == null || text.isBlank()) return lemmaCount;

        // Очистка текста от HTML
        String cleanText = cleanHtml(text);

        // Разбиваем текст на слова
        List<String> words = splitTextIntoWords(cleanText);

        for (String word : words) {
            if (word.isEmpty() || isServiceWord(word)) continue; // пропускаем стоп-слова

            // Получаем нормальную форму слова (лемму)
            List<String> baseForms = luceneMorphology.getNormalForms(word);
            String lemma = baseForms.get(0);

            // Считаем частоту леммы
            lemmaCount.put(lemma, lemmaCount.getOrDefault(lemma, 0) + 1);
        }
        return lemmaCount;
    }

    /**
     * Разделение текста на слова
     * Убираем все символы кроме русских букв и пробелов
     */
    private List<String> splitTextIntoWords(String text) {
        String cleanText = text.toLowerCase()
                .replaceAll("[^а-яё\\s]", " ");
        String[] words = cleanText.split("\\s+");
        return Arrays.stream(words)
                .filter(word -> !word.isBlank())
                .toList();
    }

    /**
     * Проверка, является ли слово "служебным"
     * (союзы, междометия, предлоги, частицы)
     */
    private boolean isServiceWord(String word) {
        List<String> morphInfo = luceneMorphology.getMorphInfo(word);

        for (String info : morphInfo) {
            if (info.contains("СОЮЗ")   // союз
                    || info.contains("МЕЖД")  // междометие
                    || info.contains("ПРЕДЛ") // предлог
                    || info.contains("ЧАСТ")) // частица
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Убирает HTML-теги и скрипты/стили, возвращает чистый текст
     */
    public static String cleanHtml(String html) {
        if (html == null || html.isEmpty()) return "";

        Document doc = Jsoup.parse(html);
        doc.select("script, style, noscript").remove(); // удаляем теги, которые не несут смысл
        String text = doc.text();
        return text.replaceAll("\\s+", " ").trim(); // нормализуем пробелы
    }
}

