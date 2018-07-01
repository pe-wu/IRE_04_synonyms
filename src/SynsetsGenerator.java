import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.US_ASCII;

class SynsetsGenerator {

    private static ListMultimap<String, String> synsets;
    private int RADIX_HEX = 16;

    public SynsetsGenerator(Path wordnetDir) {
        System.out.println("building synsets...");
        String[] dataFiles = new String[]{"data.adj", "data.adv", "data.noun", "data.verb"};
        String[] exceptionFiles = new String[]{"adj.exc", "adv.exc", "noun.exc", "verb.exc"};

        synsets = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());


        for (String dataFile : dataFiles) {
            saveRegularSynsets(wordnetDir + "\\" + dataFile);
        }

        for (String exceptionFile : exceptionFiles) {
            addExceptionSynsets(wordnetDir + "\\" + exceptionFile);
        }
    }

    Multimap<String, String> getSynsets(Path wordnetDir) {


        return synsets;
    }

    private void saveRegularSynsets(String dataFile) {
        System.out.println("saving regular synsets... " + dataFile);
        Path filepath = Paths.get(dataFile);

        try (Stream<String> lines = Files.lines(filepath, US_ASCII)) {
            lines.forEach(processLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Consumer<String> processLine = (line) -> {
        if (!line.startsWith(" ")) {
            saveSynset(line);
        }
    };

    private void saveSynset(String line) {
        System.out.println("saving regular synsets... " + line);
        System.out.println(line.substring(14, 16));

        ArrayList<String> synonyms = getSynonymsInLine(line);
        addSynonyms(synonyms);
    }

    private void addSynonyms(ArrayList<String> synonyms) {
        for (String word : synonyms) {
            for (String synonym : synonyms) {
                if (!word.equals(synonym)) {
                    System.out.println("pair: " + word + "," + synonym);
                    synsets.put(word, synonym);
                }
            }
        }
    }

    private ArrayList<String> getSynonymsInLine(String line) {
        ArrayList<String> synonyms = new ArrayList<>();

        int synonymsCount = Integer.parseInt(line.substring(14, 16), RADIX_HEX);
        int wordStartIndex = 17;
        int wordEndIndex = 18;
        int wordsFound = 0;

        while (wordsFound < synonymsCount) {
            if (line.charAt(wordEndIndex) != ' ') {
                wordEndIndex++;
            } else {
                String word = line.substring(wordStartIndex, wordEndIndex);
                addWord(synonyms, word);
                wordStartIndex = wordEndIndex + 3;
                wordEndIndex = wordStartIndex + 1;
                wordsFound++;
//                System.out.println("found word: " + word);
            }
        }
        return synonyms;
    }

    private void addWord(ArrayList<String> synonyms, String word) {
        if (!word.contains("_")) {
            if (!word.contains("(")) {
                synonyms.add(word);
            } else {
                int toCutIndex = word.indexOf("(");
                synonyms.add(word.substring(0, toCutIndex));
            }
        }
    }

    private void addExceptionSynsets(String exceptionFile) {
        System.out.println("adding exception synsets... " + exceptionFile);

    }
}
