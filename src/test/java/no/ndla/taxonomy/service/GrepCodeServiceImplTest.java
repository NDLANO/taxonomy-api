/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.repositories.GrepCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class GrepCodeServiceImplTest {
    private GrepCodeRepository grepCodeRepository;
    private GrepCodeServiceImpl grepCodeService;

    @BeforeEach
    void setUp(@Autowired GrepCodeRepository grepCodeRepository) {
        this.grepCodeRepository = grepCodeRepository;
        grepCodeService = new GrepCodeServiceImpl(grepCodeRepository);
    }

    @Test
    @Transactional
    void getOrCreateGrepCode() {
        assertFalse(grepCodeRepository.findFirstByCode("TEST12").isPresent());
        final var aim1 = grepCodeService.getOrCreateGrepCode("TEST12");

        assertNotNull(aim1);
        assertNotNull(aim1.getId());

        assertSame(aim1, grepCodeRepository.findFirstByCode("TEST12").orElseThrow());
        assertSame(aim1, grepCodeService.getOrCreateGrepCode("TEST12"));
    }
}
