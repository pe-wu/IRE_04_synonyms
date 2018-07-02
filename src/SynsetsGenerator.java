import com.google.common.collect.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.US_ASCII;

class SynsetsGenerator {

    private static Multimap<String, String> synsets;
    private final Path wordnetDir;

    SynsetsGenerator(Path wordnetDir) {
        System.out.println("Building synsets...");
        this.wordnetDir = wordnetDir;

        Multimap<String, String> adjSynsets = generateSynsets("data.adj");
        Multimap<String, String> advSynsets = generateSynsets("data.adv");
        Multimap<String, String> nounSynsets = generateSynsets("data.noun");
        Multimap<String, String> verbSynsets = generateSynsets("data.verb");

        synsets = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        synsets.putAll(adjSynsets);
        synsets.putAll(advSynsets);
        synsets.putAll(nounSynsets);
        synsets.putAll(verbSynsets);

        System.out.println("### before adding exceptions ###");
        printSynsetStats();

        addExceptions(adjSynsets, "adj.exc");
        addExceptions(advSynsets, "adv.exc");
        addExceptions(nounSynsets, "noun.exc");
        addExceptions(verbSynsets, "verb.exc");

        System.out.println("### after adding exceptions ###");
        printSynsetStats();
    }

    private Multimap<String, String> generateSynsets(String data) {
        Path dataPath = Paths.get(wordnetDir + "\\" + data);
        Multimap<String, String> map = Multimaps.synchronizedMultimap(HashMultimap.create());

        try (Stream<String> lines = Files.lines(dataPath, US_ASCII)) {
            lines.forEach((line) -> {
                if (!line.startsWith(" ")) {
                    addSynonyms(map, getSynonyms(line.toLowerCase(Locale.ENGLISH)));
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
//                check(word, line);
                addWord(synonyms, word);
                wordStartIndex = wordEndIndex + 3;
                wordEndIndex = wordStartIndex + 1;
                wordsFound++;
            }
        }
        return synonyms;
    }

    private void check(String word, String line) {
        if (word.contains(".")) {
            System.err.println("problem with word: " + word + " line: " + line);
        }
    }

    private void addWord(ArrayList<String> synonyms, String word) {
        if (!word.contains("_")) {
            if (!word.contains("(")) {
                synonyms.add(word);
            } else {
                int cutIndex = word.lastIndexOf("(");
                synonyms.add(word.substring(0, cutIndex));
            }
        }
    }

    private void addExceptions(Multimap<String, String> map, String exceptions) {
        Path exceptionsPath = Paths.get(wordnetDir + "\\" + exceptions);

        try (Stream<String> lines = Files.lines(exceptionsPath, US_ASCII)) {
            lines.forEach((line) -> {
                StringTokenizer st = new StringTokenizer(line);
                String inflectedWord = st.nextToken();

                while (st.hasMoreTokens()) {
                    String baseWord = st.nextToken();
                    Collection<String> otherWordSynset = map.get(baseWord);
                    synsets.putAll(inflectedWord, otherWordSynset);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Multimap<String, String> getSynsets() {
//        for (String key : synsets.keySet()) {
//            for (String val : synsets.get(key)) {
//                System.out.print(val + "\t");
//            }
//            System.out.println();
//        }
        return synsets;
    }

    private void printSynsetStats() {
        int keys = 0;
        int vals = 0;
        int goodSyns = 0;
        for (String key : synsets.keySet()) {
            keys++;
            if (synsets.get(key).size() > 1) {
                goodSyns++;
            }
            for (String val : synsets.get(key)) {
                vals++;
            }
        }
        System.out.println("keys:     " + keys);
        System.out.println("vals:     " + (vals - keys));
        System.out.println("goodSyns: " + goodSyns);
        System.out.println();
    }
}
