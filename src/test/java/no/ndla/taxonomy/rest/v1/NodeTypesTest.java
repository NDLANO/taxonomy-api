package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.NodeType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class NodeTypesTest extends RestTest {

    @Test
    public void can_get_a_single_node_type() throws Exception {
        builder.nodeType(f -> f
                .publicId("urn:nodetype:programarea")
                .name("Program area")
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/nodetypes/" + "urn:nodetype:programarea");
        NodeTypes.NodeTypeIndexDocument nodeType = testUtils.getObject(NodeTypes.NodeTypeIndexDocument.class, response);

        assertEquals("Program area", nodeType.name);
    }

    @Test
    public void can_get_all_node_types() throws Exception {
        builder.nodeType(f -> f
                .publicId("urn:nodetype:programarea")
                .name("Program area")
        );

        builder.nodeType(f -> f
                .publicId("urn:nodetype:educationprogram")
                .name("Education program")
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/nodetypes");
        NodeTypes.NodeTypeIndexDocument[] nodeTypes = testUtils.getObject(NodeTypes.NodeTypeIndexDocument[].class, response);

        assertEquals(4, nodeTypes.length);
        // Our types:
        assertAnyTrue(nodeTypes, f -> f.name.equals("Program area"));
        assertAnyTrue(nodeTypes, f -> f.name.equals("Education program"));
        // Standard types:
        assertAnyTrue(nodeTypes, f -> f.id.toString().equals("urn:nodetype:topic"));
        assertAnyTrue(nodeTypes, f -> f.id.toString().equals("urn:nodetype:subject"));
    }

    @Test
    public void can_delete_node_type() throws Exception {
        builder.nodeType(f -> f
                .publicId("urn:nodetype:programarea")
                .name("Program area")
        );

        testUtils.deleteResource("/v1/nodetypes/" + "urn:nodetype:programarea");
        assertNull(nodeTypeRepository.findByPublicId(URI.create("urn:nodetype:programarea")));
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        NodeTypes.NodeTypeCommand command = new NodeTypes.NodeTypeCommand() {{
            id = URI.create("urn:nodetype:name");
            name = "name";
        }};

        testUtils.createResource("/v1/nodetypes", command, status().isCreated());
        testUtils.createResource("/v1/nodetypes", command, status().isConflict());
    }

    @Test
    public void can_update_node_type() throws Exception {
        URI id = builder.nodeType().getPublicId();

        NodeTypes.NodeTypeCommand command = new NodeTypes.NodeTypeCommand() {{
            name = "Education program";
        }};

        testUtils.updateResource("/v1/nodetypes/" + id, command);

        NodeType nodeType = nodeTypeRepository.getByPublicId(id);
        assertEquals(command.name, nodeType.getName());
    }

    @Test
    public void get_unknown_node_type_fails_gracefully() throws Exception {
        testUtils.getResource("/v1/nodetypes/nonexistantid", status().isNotFound());
    }
}
