package no.ndla.taxonomy.service;

import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = "courses")
public class CourseResource {

    private TitanGraph graph;

    public CourseResource(TitanGraph graph) {
        this.graph = graph;
    }

    @GetMapping
    public List<Course> index() {
        List<Course> result = new ArrayList<>();

        GraphTraversal<Vertex, Vertex> courses = graph.traversal().V().hasLabel("course");
        while (courses.hasNext()) {
            Vertex v = courses.next();
            result.add(toCourse(v));
        }

        return result;
    }

    @PostMapping
    public ResponseEntity<Void> post(@RequestBody Course course) {
        TitanTransaction transaction = graph.buildTransaction().start();
        Vertex vertex = graph.addVertex(T.label, "course", "name", course.getName());
        transaction.commit();

        return ResponseEntity.created(URI.create("/courses/" + vertex.id())).build();
    }

    private Course toCourse(Vertex v) {
        Course course = new Course();
        course.id(v.id());
        v.values("name").forEachRemaining(n -> course.name((String) n));
        return course;
    }
}
