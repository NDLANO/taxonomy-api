/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

public class RelevanceTranslationsTest extends RestTest {
    // TODO: Do we even need this anymore?
    //    @Test
    //    public void can_get_all_relevances() throws Exception {
    //        MockHttpServletResponse response = testUtils.getResource("/v1/relevances?language=nb");
    //        RelevanceDTO[] relevances = testUtils.getObject(RelevanceDTO[].class, response);
    //
    //        assertEquals(2, relevances.length);
    //        assertAnyTrue(relevances, s -> s.name.equals("Kjernestoff"));
    //        assertAnyTrue(relevances, s -> s.name.equals("Tilleggstoff"));
    //    }
    //
    //    @Test
    //    public void can_get_single_relevance() throws Exception {
    //        URI id = builder.relevance(r -> r.name("Core").translation("Kjernestoff", "nb"))
    //                .getPublicId();
    //
    //        RelevanceDTO relevanceDTO = getRelevanceDTO(id, "nb");
    //        assertEquals("Kjernestoff", relevanceDTO.name);
    //    }
    //
    //    @Test
    //    public void fallback_to_default_language() throws Exception {
    //        URI id = builder.relevance(t -> t.name("Random")).getPublicId();
    //        RelevanceDTO relevance = getRelevanceDTO(id, "XX");
    //        assertEquals("Random", relevance.name);
    //    }
    //
    //    @Test
    //    public void can_get_default_language() throws Exception {
    //        URI id = builder.relevance(r -> r.name("Relevance").translation("Noe annet", "nb"))
    //                .getPublicId();
    //
    //        RelevanceDTO relevanceDTO = getRelevanceDTO(id, null);
    //        assertEquals("Relevance", relevanceDTO.name);
    //    }
    //
    //    @Test
    //    public void can_add_translation() throws Exception {
    //        Relevance something = builder.relevance(r -> r.name("Something"));
    //        URI id = something.getPublicId();
    //
    //        testUtils.updateResource("/v1/relevances/" + id + "/translations/nb", new TranslationPUT() {
    //            {
    //                name = "Something else";
    //            }
    //        });
    //
    //        assertEquals("Something else", something.getTranslation("nb").get().getName());
    //    }
    //
    //    @Test
    //    public void can_delete_translation() throws Exception {
    //        Relevance relevance = builder.relevance(r -> r.name("Relevance").translation("Relevans", "nb"));
    //        URI id = relevance.getPublicId();
    //
    //        testUtils.deleteResource("/v1/relevances/" + id + "/translations/nb");
    //
    //        assertNull(relevance.getTranslation("nb").orElse(null));
    //    }
    //
    //    @Test
    //    public void can_get_all_translations() throws Exception {
    //        Relevance relevance = builder.relevance(t -> t.name("Core")
    //                .translation("Kjernestoff", "nb")
    //                .translation("Core", "en")
    //                .translation("Kernestoff", "dk"));
    //        URI id = relevance.getPublicId();
    //
    //        TranslationDTO[] translations = testUtils.getObject(
    //                TranslationDTO[].class, testUtils.getResource("/v1/relevances/" + id + "/translations"));
    //
    //        assertEquals(3, translations.length);
    //        assertAnyTrue(translations, t -> t.name.equals("Kjernestoff") && t.language.equals("nb"));
    //        assertAnyTrue(translations, t -> t.name.equals("Core") && t.language.equals("en"));
    //        assertAnyTrue(translations, t -> t.name.equals("Kernestoff") && t.language.equals("dk"));
    //    }
    //
    //    @Test
    //    public void can_get_single_translation() throws Exception {
    //        Relevance relevance = builder.relevance(r -> r.name("Core").translation("Kjernestoff", "nb"));
    //        URI id = relevance.getPublicId();
    //
    //        TranslationDTO translation = testUtils.getObject(
    //                TranslationDTO.class, testUtils.getResource("/v1/relevances/" + id + "/translations/nb"));
    //        assertEquals("Kjernestoff", translation.name);
    //        assertEquals("nb", translation.language);
    //    }
    //
    //    private RelevanceDTO getRelevanceDTO(URI id, String language) throws Exception {
    //        String path = "/v1/relevances/" + id;
    //        if (isNotEmpty(language)) path = path + "?language=" + language;
    //        return testUtils.getObject(RelevanceDTO.class, testUtils.getResource(path));
    //    }
}
