package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.domain.NotFoundException;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.Assert.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@Component
public class TestUtils {

    private static HttpMessageConverter mappingJackson2HttpMessageConverter;
    private static GraphFactory factory;
    private static MockMvc mockMvc;

    @Autowired
    public TestUtils(HttpMessageConverter<?>[] converters, WebApplicationContext webApplicationContext, GraphFactory factory) {
        mappingJackson2HttpMessageConverter = Arrays.stream(converters)
                .filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter)
                .findAny()
                .orElse(null);
        this.factory = factory;

        assertNotNull("the JSON message converter must not be null", this.mappingJackson2HttpMessageConverter);

        mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    public static String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        mappingJackson2HttpMessageConverter.write(o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

    public static MockHttpServletResponse createResource(String path, Object command) throws Exception {
        return createResource(path, command, status().isCreated());
    }

    public static MockHttpServletResponse createResource(String path, Object command, ResultMatcher resultMatcher) throws Exception {
        return mockMvc.perform(
                post(path)
                        .contentType(APPLICATION_JSON_UTF8)
                        .content(json(command)))
                .andExpect(resultMatcher)
                .andReturn()
                .getResponse();
    }

    public static MockHttpServletResponse getResource(String path) throws Exception {
        return mockMvc.perform(
                get(path)
                        .accept(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andReturn()
                .getResponse();
    }

    public static MockHttpServletResponse deleteResource(String path) throws Exception {
        return mockMvc.perform(
                delete(path))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse();
    }

    public static MockHttpServletResponse updateResource(String path, Object command) throws Exception {
        return mockMvc.perform(
                put(path)
                        .contentType(APPLICATION_JSON_UTF8)
                        .content(json(command)))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse();
    }

    public static String getId(MockHttpServletResponse response) {
        String location = response.getHeader("Location");
        return location.substring(location.lastIndexOf("/") + 1);
    }

    public static <V> V getObject(Class<V> theClass, MockHttpServletResponse response) throws Exception {
        MockHttpInputMessage mockHttpInputMessage = new MockHttpInputMessage(response.getContentAsByteArray());
        return (V) mappingJackson2HttpMessageConverter.read(theClass, mockHttpInputMessage);
    }

    public static <V> void assertAnyTrue(V[] objects, Predicate<V> predicate) {
        assertTrue("Array was empty", objects.length > 0);
        String className = objects[0].getClass().getSimpleName();
        assertTrue("No " + className + " matching predicate found.", Arrays.stream(objects).anyMatch(predicate));
    }

    public static <V> void assertAnyTrue(Iterator<V> objects, Predicate<V> predicate) {
        assertTrue(objects.hasNext());

        boolean anyTrue = false;
        String className = null;
        while (objects.hasNext()) {
            V next = objects.next();
            className = next.getClass().getSimpleName();
            if (predicate.test(next)) {
                anyTrue = true;
                break;
            }
        }

        assertTrue("No " + className + " matching predicate found.", anyTrue);
    }

    public static <V> void assertAllTrue(V[] objects, Predicate<V> predicate) {
        assertTrue(Arrays.stream(objects).allMatch(predicate));
    }

    public static boolean isValidId(URI id) {
        return id != null && id.toString().contains("urn");
    }

    public static void clearGraph() throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            assertTrue("Are you mad?", factory.isTest());
            graph.vertices().forEachRemaining(v -> v.remove());
            transaction.commit();
        }
    }


    public static void assertNotFound(Consumer<Graph> consumer) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            consumer.accept(graph);
            transaction.rollback();
            fail("Expected NotFoundException");
        } catch (NotFoundException expectedException) {
            //ok
        }
    }

    public static int count(Iterator iterator) {
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }
}
