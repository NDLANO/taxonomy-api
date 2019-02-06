package no.ndla.taxonomy.rest.v1.commands;

import java.net.URI;

public abstract class CreateCommand<T> {
    public abstract URI getId();

    public abstract void apply(T entity);
}
