package no.ndla.taxonomy.rest.v1;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = {"/v1/requestqueue"})
public class RequestQueue {

    public void list(){}

    public void clear(){}

    public void applyChanges(){}

}
