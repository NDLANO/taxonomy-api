package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.DomainObject;
import no.ndla.taxonomy.domain.Subject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface PathComponentGeneratingService {
    final int LENGTH_LIM = 42;

    default public String filterCharacters(String str) {
        char[] characters = str.toCharArray();
        {
            char[] outCharacters = new char[characters.length];
            var o = 0;
            for (var i = 0; i < characters.length; i++) {
                char ch = characters[i];
                if ((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) {
                    outCharacters[o++] = ch;
                }
            }
            characters = new char[o];
            for (var i = 0; i < o; i++) {
                characters[i] = outCharacters[i];
            }
        }
        return new String(characters);
    }

    default public <T extends DomainObject> String generatePathComponent(T domainObject, Function<T,String> nameExtractor) {
        if (nameExtractor == null) {
            nameExtractor = DomainObject::getName;
        }
        var words = Arrays.stream(nameExtractor.apply(domainObject).toLowerCase().split("\\s"))
                .map(this::filterCharacters)
                .filter(w -> !w.isBlank())
                .collect(Collectors.toList());
        var byLength = new ArrayList<>(words);
        Collections.sort(byLength, Comparator.comparingInt(String::length));
        var totalLength = words.stream().map(String::length).reduce((a, b) -> a + b + 1).orElse(0);
        var remove = byLength.iterator();
        if (remove.hasNext()) {
            while (totalLength > LENGTH_LIM) {
                var r = remove.next();
                totalLength -= r.length() + 1;
                if (!remove.hasNext()) {
                    break;
                }
                words.remove(r);
            }
        }
        return words.stream().reduce((a, b) -> a + "-" + b).orElse("null");
    }

    default public String generatePathComponent(DomainObject domainObject) {
        return generatePathComponent(domainObject, null);
    }

    default public String generatePathComponent(Subject subject) {
        // TODO - Can we have the UDIR ID available?
        return generatePathComponent(subject, Subject::getName);
    }

}
