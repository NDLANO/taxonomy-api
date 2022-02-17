/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.GrepCode;
import no.ndla.taxonomy.repositories.GrepCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;

@Service
public class GrepCodeServiceImpl implements GrepCodeService {
    private final GrepCodeRepository grepCodeRepository;

    public GrepCodeServiceImpl(GrepCodeRepository grepCodeRepository) {
        this.grepCodeRepository = grepCodeRepository;
    }

    @Override
    @Transactional(propagation = MANDATORY)
    public GrepCode getOrCreateGrepCode(String code) {
        return grepCodeRepository.findFirstByCode(code).orElseGet(() -> {
            final var competenceAim = new GrepCode();
            competenceAim.setCode(code);

            return grepCodeRepository.saveAndFlush(competenceAim);
        });
    }
}
