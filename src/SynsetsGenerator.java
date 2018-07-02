import com.google.common.collect.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

class SynsetsGenerator {

    private static Multimap<String, String> synsets;
    private final Path wordnetDir;

    SynsetsGenerator(Path wordnetDir) {
        System.out.println("Building synsets...");
        this.wordnetDir = wordnetDir;

        Multimap<String, String> adjSynsets = generateSynsets("data.adj", "adj.exc");
        Multimap<String, String> advSynsets = generateSynsets("data.adv", "adv.exc");
        Multimap<String, String> nounSynsets = generateSynsets("data.noun", "noun.exc");
        Multimap<String, String> verbSynsets = generateSynsets("data.verb", "verb.exc");

        synsets = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        synsets.putAll(adjSynsets);
        synsets.putAll(advSynsets);
        synsets.putAll(nounSynsets);
        synsets.putAll(verbSynsets);
    }

    private Multimap<String, String> generateSynsets(String data, String exceptions) {
        Path dataPath = Paths.get(wordnetDir + "\\" + data);
        Path exceptionsPath = Paths.get(wordnetDir + "\\" + exceptions);

        Multimap<String, String> map = getRegularSynsets(dataPath);
        mergeExceptions(map, exceptionsPath);

        return map;
    }

    private Multimap<String, String> getRegularSynsets(Path dataPath) {
        Multimap<String, String> map = Multimaps.synchronizedMultimap(HashMultimap.create());

        try (Stream<String> lines = Files.lines(dataPath, UTF_8)) {
            lines.forEach((line) -> {
                if (!line.startsWith(" ")) {
                    addSynonyms(map, getSynonyms(line));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }

    private void addSynonyms(Multimap<String, String> map, ArrayList<String> synonyms) {
        for (String word : synonyms) {
            for (String synonym : synonyms) {
                map.put(word, synonym);
            }
        }
    }

    private ArrayList<String> getSynonyms(String line) {
        ArrayList<String> synonyms = new ArrayList<>();

        int wordStartIndex = 17;
        int wordEndIndex = 18;
        int wordsFound = 0;
        int wordsCount = Integer.parseInt(line.substring(14, 16), 16);

        while (wordsFound < wordsCount) {
            if (line.charAt(wordEndIndex) != ' ') {
                wordEndIndex++;
            } else {
                String word = line.substring(wordStartIndex, wordEndIndex);
                addWord(synonyms, word.toLowerCase());
                wordStartIndex = wordEndIndex + 3;
                wordEndIndex = wordStartIndex + 1;
                wordsFound++;
            }
        }
        return synonyms;
    }

    private void addWord(ArrayList<String> synonyms, String word) {
        if (!word.contains("_")) {
            if (!word.contains("(")) {
                synonyms.add(word);
            } else {
                int toCutIndex = word.lastIndexOf("(");
                synonyms.add(word.substring(0, toCutIndex));
            }
        }
    }

    private void mergeExceptions(Multimap<String, String> map, Path exceptionsPath) {
        try (Stream<String> lines = Files.lines(exceptionsPath, UTF_8)) {
            lines.forEach((line) -> {

                StringTokenizer st = new StringTokenizer(line);
                String inflected = st.nextToken();

                while (st.hasMoreTokens()) {
                    String otherWord = st.nextToken();
                    Collection<String> otherWordSynset = map.get(otherWord);
                    map.putAll(inflected, otherWordSynset);
                }


            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    Multimap<String, String> getSynsets() {
        System.out.println(synsets.size());

        for (String key : synsets.keySet()) {
            for (String val : synsets.get(key)) {
                System.out.print(val + "\t");
            }
            System.out.println();
        }

        int keys = 0;
        int vals = 0;
        int goodSyns = 0;
        for (String key : synsets.keySet()) {
            keys++;
            if (synsets.get(key).size()>1) goodSyns++;
            for (String val : synsets.get(key)) {
                vals++;
            }
        }
        System.out.println("keys: " + keys);
        System.out.println("vals: " + (vals - keys));
        System.out.println("goodSyns: " + goodSyns);

        return synsets;
    }

    private String normalize(String text) {
        return text.replaceAll("[.,:!?]", " ");
    }
}
