/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.NoRepositoryBean;

import java.net.URI;

@NoRepositoryBean
public interface TaxonomyRepository<T> extends JpaRepository<T, Integer>, JpaSpecificationExecutor<T> {

    String RESOURCE_METADATA = " LEFT JOIN FETCH r.metadata rm LEFT JOIN FETCH rm.grepCodes"
            + " LEFT JOIN FETCH rm.customFieldValues rcfv LEFT JOIN FETCH rcfv.customField ";
    String NODE_METADATA = " LEFT JOIN FETCH n.metadata nm LEFT JOIN FETCH nm.grepCodes"
            + " LEFT JOIN FETCH nm.customFieldValues ncfv LEFT JOIN FETCH ncfv.customField ";
    String CHILD_METADATA = " LEFT JOIN FETCH c.metadata cm LEFT JOIN FETCH cm.grepCodes"
            + " LEFT JOIN FETCH cm.customFieldValues ccfv LEFT JOIN FETCH ccfv.customField ";
    String NODE_CONNECTION_METADATA = " LEFT JOIN FETCH nc.metadata ncm LEFT JOIN FETCH ncm.grepCodes"
            + " LEFT JOIN FETCH ncm.customFieldValues nccfv LEFT JOIN FETCH nccfv.customField ";
    String NODE_RESOURCE_METADATA = " LEFT JOIN FETCH nr.metadata nrm LEFT JOIN FETCH nrm.grepCodes"
            + " LEFT JOIN FETCH nrm.customFieldValues nrcfv LEFT JOIN FETCH nrcfv.customField ";

    T findByPublicId(URI id);

    default T getByPublicId(URI id) {
        T entity = findByPublicId(id);
        if (null == entity)
            throw new NotFoundException("entity", id);
        return entity;
    }

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    void deleteByPublicId(URI id);

    default void deleteAllAndFlush() {
        deleteAll();
        flush();
    }
}
