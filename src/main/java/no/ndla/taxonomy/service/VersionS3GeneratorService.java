/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import no.ndla.taxonomy.domain.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

@Service
@Profile("!junit")
public class VersionS3GeneratorService implements VersionGeneratorService {
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AmazonS3 s3Attachment;

    @Value(value = "${taxonomy.versions.bucket:local.taxonomy-versions.ndla}")
    private String bucketName;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${server.port:5000}")
    private int serverPort;

    @Override
    @Async
    public void generateJson(Version version) {
        generateJsonForApiEndpoints(version, restTemplate, serverPort);
    }

    @Override
    public void writeFile(String directory, String filename, byte[] filecontent, String contentType) {
        if (filecontent == null || filecontent.length == 0) {
            throw new RuntimeException("Filecontent is empty");
        }
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(filecontent.length);
        metadata.setContentType(contentType);

        String path = directory + filename;

        try {
            // Generating PutObjectRequest
            PutObjectRequest putAttachment = new PutObjectRequest(bucketName, path,
                    new ByteArrayInputStream(filecontent), metadata);
            putAttachment.withCannedAcl(CannedAccessControlList.PublicRead);

            PutObjectResult putResult = s3Attachment.putObject(putAttachment);

        } catch (AmazonServiceException ase) {
            logger.warn("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            logger.warn("Error Message:    " + ase.getMessage());
            logger.warn("HTTP Status Code: " + ase.getStatusCode());
            logger.warn("AWS Error Code:   " + ase.getErrorCode());
            logger.warn("Error Type:       " + ase.getErrorType());
            logger.warn("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            logger.warn("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            logger.warn("Error Message: " + ace.getMessage());
        } catch (Exception e) {
            logger.warn("Caught an exception");
            logger.warn("Error message:" + e.getMessage());
            logger.warn("Stack trace:" + Arrays.toString(e.getStackTrace()));
        }
    }
}
