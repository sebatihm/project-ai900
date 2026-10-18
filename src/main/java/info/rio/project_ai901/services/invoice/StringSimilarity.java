package info.rio.project_ai901.services.invoice;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class StringSimilarity {

    private StringSimilarity() {
    }

    public static double similarity(String first, String second) {
        String a = normalize(first);
        String b = normalize(second);
        if (a.isBlank() || b.isBlank()) {
            return 0.0;
        }
        if (a.equals(b)) {
            return 1.0;
        }
        Set<String> firstTokens = tokens(a);
        Set<String> secondTokens = tokens(b);
        Set<String> intersection = new LinkedHashSet<>(firstTokens);
        intersection.retainAll(secondTokens);
        Set<String> union = new LinkedHashSet<>(firstTokens);
        union.addAll(secondTokens);
        double jaccard = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
        if (a.contains(b) || b.contains(a)) {
            return Math.max(jaccard, 0.9);
        }
        return jaccard;
    }

    private static Set<String> tokens(String value) {
        return Arrays.stream(value.split(" "))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
