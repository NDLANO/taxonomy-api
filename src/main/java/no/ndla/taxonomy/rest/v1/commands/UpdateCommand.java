package no.ndla.taxonomy.rest.v1.commands;


public abstract class UpdateCommand<T> {
    public abstract void apply(T entity);
}
