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

    private final LuceneMorphology luceneMorphology;

    public LemmaService() throws IOException {
        this.luceneMorphology = new RussianLuceneMorphology();
    }

    public Map<String, Integer> getLemmasWithCount(String text) {
        Map<String, Integer> lemmaCount = new HashMap<>();
        if (text == null || text.isBlank()) return lemmaCount;

        String cleanText = cleanHtml(text);
        List<String> words = splitTextIntoWords(cleanText);

        for (String word : words) {
            if (word.isEmpty() || isServiceWord(word)) continue;

            List<String> baseForms = luceneMorphology.getNormalForms(word);
            String lemma = baseForms.get(0);
            lemmaCount.put(lemma, lemmaCount.getOrDefault(lemma, 0) + 1);
        }
        return lemmaCount;
    }

    private List<String> splitTextIntoWords(String text) {
        String cleanText = text.toLowerCase()
                .replaceAll("[^а-яё\\s]", " ");
        String[] words = cleanText.split("\\s+");
        return Arrays.stream(words)
                .filter(word -> !word.isBlank())
                .toList();
    }

    private boolean isServiceWord(String word) {
        List<String> morphInfo = luceneMorphology.getMorphInfo(word);

        for (String info : morphInfo) {
            if (info.contains("СОЮЗ")
                    || info.contains("МЕЖД")
                    || info.contains("ПРЕДЛ")
                    || info.contains("ЧАСТ")) {
                return true;
            }
        }
        return false;
    }



    public static String cleanHtml(String html) {
        if (html == null || html.isEmpty()) return "";

        Document doc = Jsoup.parse(html);
        doc.select("script, style, noscript").remove();
        String text = doc.text();
        return text.replaceAll("\\s+", " ").trim();
    }
}
