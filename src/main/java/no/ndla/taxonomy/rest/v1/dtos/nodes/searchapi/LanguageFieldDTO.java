package no.ndla.taxonomy.rest.v1.dtos.nodes.searchapi;

import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.LanguageField;
import no.ndla.taxonomy.domain.Node;
import org.springframework.data.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Schema(name = "LanguageField")
public class LanguageFieldDTO<V> extends HashMap<String, V> {

    public LanguageFieldDTO() {

    }

    public LanguageFieldDTO(List<Pair<String, V>> translations) {
        translations.forEach(t -> {
            this.put(t.getFirst(), t.getSecond());
        });
    }

    public static LanguageFieldDTO<String> fromLanguageField(LanguageField<String> languageField) {
        var dto = new LanguageFieldDTO<String>();
        Set<String> keySet = languageField.keySet();
        for (String key : keySet) {
            dto.put(key, languageField.get(key));
        }
        return dto;
    }

    public static LanguageFieldDTO<List<String>> fromLanguageFieldList(LanguageField<List<String>> languageField) {
        var dto = new LanguageFieldDTO<List<String>>();
        Set<String> keySet = languageField.keySet();
        for (String key : keySet) {
            dto.put(key, languageField.get(key));
        }
        return dto;
    }

    public static LanguageFieldDTO<String> nameFromNode(Node node) {
        var languageField = new LanguageFieldDTO<String>();
        languageField.put("nb", node.getName());

        node.getTranslations().forEach(nt -> {
            languageField.put(nt.getLanguageCode(), nt.getName());
        });

        return languageField;
    }

}
