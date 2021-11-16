/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;

@Service
@Profile("junit")
public class VersionFileGeneratorService implements VersionGeneratorService {
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value(value = "${versions.files.folder:file:/tmp}")
    private String rootFolder;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${server.port:5000}")
    private int serverPort;

    @Override
    @Async
    public void generateJson(Version version) {
        // generateJsonForApiEndpoints(version, restTemplate, serverPort); // Only to test local generation of all files
        try {
            writeFile(rootFolder, "test.json", "{\"key\":\"value\"}".getBytes(), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeFile(String directory, String filename, byte[] filecontent, String contentType)
            throws IOException {
        File file = new File(rootFolder + "/" + directory + filename);
        file.getParentFile().mkdirs();

        InputStream stream = new ByteArrayInputStream(filecontent);
        String filepath = file.getPath();
        OutputStream bos = new FileOutputStream(filepath);
        int bytesRead;
        byte[] buffer = new byte[8192];

        while ((bytesRead = stream.read(buffer, 0, 8192)) != -1) {
            bos.write(buffer, 0, bytesRead);
        }
        bos.close();
        // close the stream
        stream.close();
    }
}
