/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class VersionSchemaHandlerService {
    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    public void bindSession() {
        if (!TransactionSynchronizationManager.hasResource(entityManagerFactory)) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            TransactionSynchronizationManager.bindResource(
                    entityManagerFactory, new EntityManagerHolder(entityManager));
        }
    }

    public void unbindSession() {
        EntityManagerHolder emHolder =
                (EntityManagerHolder) TransactionSynchronizationManager.unbindResource(entityManagerFactory);
        EntityManagerFactoryUtils.closeEntityManager(emHolder.getEntityManager());
    }
}
