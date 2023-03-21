package no.ndla.taxonomy.rest.v1.dtos.nodes.searchapi;

import no.ndla.taxonomy.domain.Node;
import org.springframework.data.util.Pair;

import java.util.HashMap;
import java.util.List;

public class LanguageField<V> extends HashMap<String, V> {

    public LanguageField() {

    }

    public LanguageField(List<Pair<String, V>> translations) {
        translations.forEach(t -> {
            this.put(t.getFirst(), t.getSecond());
        });
    }

    public static LanguageField<String> nameFromNode(Node node) {
        var languageField = new LanguageField<String>();
        languageField.put("nb", node.getName());

        node.getTranslations().forEach(nt -> {
            languageField.put(nt.getLanguageCode(), nt.getName());
        });

        return languageField;
    }
}
