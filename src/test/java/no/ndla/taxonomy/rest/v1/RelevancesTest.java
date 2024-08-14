/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

public class RelevancesTest extends RestTest {

    // TODO: Do we even need this anymore?
    //    @Test
    //    public void can_get_a_single_relevance() throws Exception {
    //        MockHttpServletResponse response = testUtils.getResource("/v1/relevances/" + "urn:relevance:core");
    //        RelevanceDTO relevance = testUtils.getObject(RelevanceDTO.class, response);
    //
    //        assertEquals("Core material", relevance.name);
    //    }
    //
    //    @Test
    //    public void can_get_all_relevances() throws Exception {
    //        builder.relevance(f -> f.publicId("urn:relevance:1").name("Core material"));
    //        builder.relevance(f -> f.publicId("urn:relevance:2").name("Supplementary material"));
    //
    //        MockHttpServletResponse response = testUtils.getResource("/v1/relevances");
    //        RelevanceDTO[] relevances = testUtils.getObject(RelevanceDTO[].class, response);
    //
    //        assertEquals(4, relevances.length);
    //        assertAnyTrue(relevances, f -> f.name.equals("Core material"));
    //        assertAnyTrue(relevances, f -> f.name.equals("Supplementary material"));
    //    }
    //
    //    @Test
    //    public void can_delete_relevance() throws Exception {
    //        builder.relevance(f -> f.publicId("urn:relevance:1").name("Core material"));
    //
    //        testUtils.deleteResource("/v1/relevances/" + "urn:relevance:1");
    //        assertNull(relevanceRepository.findByPublicId(URI.create("urn:relevance:1")));
    //    }
    //
    //    @Test
    //    public void duplicate_ids_not_allowed() throws Exception {
    //        RelevancePUT command = new RelevancePUT() {
    //            {
    //                id = URI.create("urn:relevance:1");
    //                name = "name";
    //            }
    //        };
    //
    //        testUtils.createResource("/v1/relevances", command, status().isCreated());
    //        testUtils.createResource("/v1/relevances", command, status().isConflict());
    //    }
    //
    //    @Test
    //    public void can_update_relevance() throws Exception {
    //        URI id = builder.relevance().getPublicId();
    //
    //        RelevancePUT command = new RelevancePUT() {
    //            {
    //                name = "Supplementary material";
    //            }
    //        };
    //
    //        testUtils.updateResource("/v1/relevances/" + id, command);
    //
    //        Relevance relevance = relevanceRepository.getByPublicId(id);
    //        assertEquals(command.name, relevance.getName());
    //    }
    //
    //    @Test
    //    public void get_unknown_relevance_fails_gracefully() throws Exception {
    //        testUtils.getResource("/v1/relevances/nonexistantid", status().isNotFound());
    //    }
}
