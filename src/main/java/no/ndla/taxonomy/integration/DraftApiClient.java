/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.integration;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.Translation;
import no.ndla.taxonomy.integration.dtos.DraftNotesDTO;
import no.ndla.taxonomy.integration.dtos.UpdateNotesDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DraftApiClient {
    private static final Logger logger = LoggerFactory.getLogger(DraftApiClient.class);
    private final RestClient restClient;

    public DraftApiClient(@Value("${DRAFT_API_HOST:draft-api}") String draftApiHost) {
        var baseUrl = String.format("http://%s/draft-api", draftApiHost);
        restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestInitializer(new AuthorizationRequestInitializer())
                .build();
    }

    public void updateNotesWithNewConnection(NodeConnection newConnection) {
        var maybeParent = newConnection.getParent();
        var maybeChild = newConnection.getChild();
        var maybeParentId = maybeParent.flatMap(p -> getId(p.getContentUri()));
        var maybeChildId = maybeChild.flatMap(c -> getId(c.getContentUri()));

        if (maybeParent.isEmpty() || maybeChild.isEmpty() || maybeParentId.isEmpty() || maybeChildId.isEmpty()) {
            logger.error(
                    "Attempted to update draft with new connection, but parent or child was missing. This is a bug somewhere.");
            return;
        }

        var parent = maybeParent.get();
        var child = maybeChild.get();
        var parentId = maybeParentId.get();
        var childId = maybeChildId.get();

        var parentNotes = createNewParentNotes(parentId, childId, newConnection, child);
        var childNotes = createNewChildNotes(parentId, childId, newConnection, parent);

        var requestBodies =
                Stream.concat(parentNotes.stream(), childNotes.stream()).toList();
        if (!requestBodies.isEmpty()) {
            updateNotes(new UpdateNotesDTO(requestBodies));
        }
    }

    private List<DraftNotesDTO> createNewParentNotes(
            IdAndType parentId, IdAndType childId, NodeConnection newConnection, Node child) {
        var notesList = new ArrayList<DraftNotesDTO>();
        if (Objects.equals(parentId.type, "article")) {
            var relevanceNotePart = newConnection
                    .getRelevance()
                    .flatMap(r -> r.getTranslations().stream().findFirst().map(Translation::getName))
                    .orElse("barn");

            var noteString = String.format(
                    "Taksonomi: %s med id '%s' lagt til som %s", child.getNodeType(), childId.id, relevanceNotePart);
            var note = DraftNotesDTO.fromNote(parentId.id, noteString);
            notesList.add(note);

            if (newConnection.isPrimary().orElse(false)) {
                var primaryNoteString = String.format(
                        "Taksonomi: %s med id '%s' lagt til som primærkobling", child.getNodeType(), childId.id);
                var primaryNote = DraftNotesDTO.fromNote(parentId.id, primaryNoteString);
                notesList.add(primaryNote);
            }
        }
        return notesList;
    }

    private List<DraftNotesDTO> createNewChildNotes(
            IdAndType parentId, IdAndType childId, NodeConnection newConnection, Node parent) {
        var notesList = new ArrayList<DraftNotesDTO>();
        if (Objects.equals(childId.type, "article")) {
            var relevanceNotePart = newConnection
                    .getRelevance()
                    .flatMap(r -> r.getTranslations().stream().findFirst().map(Translation::getName))
                    .map(s -> " som " + s)
                    .orElse("");

            var noteString = String.format(
                    "Taksonomi: lagt til i %s med id '%s'%s", parent.getNodeType(), parentId.id, relevanceNotePart);
            var note = DraftNotesDTO.fromNote(parentId.id, noteString);
            notesList.add(note);
        }

        return notesList;
    }

    public void updateNotesWithDeletedConnection(NodeConnection deletedConnection) {
        var maybeParent = deletedConnection.getParent();
        var maybeChild = deletedConnection.getChild();
        var maybeParentId = maybeParent.flatMap(p -> getId(p.getContentUri()));
        var maybeChildId = maybeChild.flatMap(c -> getId(c.getContentUri()));

        if (maybeParent.isEmpty() || maybeChild.isEmpty() || maybeParentId.isEmpty() || maybeChildId.isEmpty()) {
            logger.error(
                    "Attempted to update draft with deleted connection, but parent or child was missing. This is a bug somewhere.");
            return;
        }

        var parent = maybeParent.get();
        var child = maybeChild.get();
        var parentId = maybeParentId.get();
        var childId = maybeChildId.get();

        var parentNotes = removedFromParentNotes(parentId, childId, parent);
        var childNotes = childRemovedNotes(parentId, childId, child);

        var requestBodies =
                Stream.concat(parentNotes.stream(), childNotes.stream()).toList();
        if (!requestBodies.isEmpty()) {
            updateNotes(new UpdateNotesDTO(requestBodies));
        }
    }

    private List<DraftNotesDTO> removedFromParentNotes(IdAndType parentId, IdAndType childId, Node parent) {
        var notesList = new ArrayList<DraftNotesDTO>();
        if (Objects.equals(childId.type, "article")) {
            var noteString = String.format("Taksonomi: fjernet fra %s med id '%s'", parent.getNodeType(), parentId.id);
            var note = DraftNotesDTO.fromNote(childId.id, noteString);
            notesList.add(note);
        }
        return notesList;
    }

    private List<DraftNotesDTO> childRemovedNotes(IdAndType parentId, IdAndType childId, Node child) {
        var notesList = new ArrayList<DraftNotesDTO>();
        if (Objects.equals(parentId.type, "article")) {
            var noteString = String.format("Taksonomi: %s med id '%s' fjernet", child.getNodeType(), childId.id);
            var note = DraftNotesDTO.fromNote(parentId.id, noteString);
            notesList.add(note);
        }
        return notesList;
    }

    public void updateNotesWithUpdatedConnection(
            NodeConnection nodeConnection, Relevance newRelevance, Optional<Boolean> newIsPrimary) {
        var maybeParent = nodeConnection.getParent();
        var maybeChild = nodeConnection.getChild();
        var maybeParentId = maybeParent.flatMap(p -> getId(p.getContentUri()));
        var maybeChildId = maybeChild.flatMap(c -> getId(c.getContentUri()));

        if (maybeParent.isEmpty() || maybeChild.isEmpty() || maybeParentId.isEmpty() || maybeChildId.isEmpty()) {
            logger.error(
                    "Attempted to update draft with updated connection, but parent or child was missing. This is a bug somewhere.");
            return;
        }

        var parent = maybeParent.get();
        var child = maybeChild.get();
        var parentId = maybeParentId.get();
        var childId = maybeChildId.get();

        var notes = new ArrayList<DraftNotesDTO>();

        var relevanceChanged =
                nodeConnection.getRelevance().map(r -> !r.equals(newRelevance)).orElse(true);
        if (relevanceChanged) {
            if (childId.type.equals("article")) {
                notes.add(DraftNotesDTO.fromNote(
                        childId.id,
                        String.format(
                                "Taksonomi: satt som %s av %s med id '%s'",
                                newRelevance, parent.getNodeType(), parentId.id)));
            }
            if (parentId.type.equals("article")) {
                var note = String.format(
                        "Taksonomi: %s med id '%s' satt som %s", child.getNodeType(), childId.id, newRelevance);
                notes.add(DraftNotesDTO.fromNote(parentId.id, note));
            }
        }

        var oldPrimary = nodeConnection.isPrimary().orElse(false);
        var newPrimary = newIsPrimary.orElse(false);
        if (oldPrimary != newPrimary && parentId.type.equals("article")) {
            var note =
                    String.format("Taksonomi: %s med id '%s' satt som primærkobling", child.getNodeType(), childId.id);
            notes.add(DraftNotesDTO.fromNote(parentId.id, note));
        }

        if (!notes.isEmpty()) {
            updateNotes(new UpdateNotesDTO(notes));
        }
    }

    record IdAndType(long id, String type) {}

    private Optional<IdAndType> getId(URI contentUri) {
        if (contentUri == null) {
            return Optional.empty();
        }
        var str = contentUri.toString();
        var splits = str.split(":");
        if (splits.length != 3) {
            return Optional.empty();
        }

        var type = splits[1];
        var id = splits[2];

        try {
            var longId = Long.parseLong(id);
            return Optional.of(new IdAndType(longId, type));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void updateNotes(UpdateNotesDTO updateNotes) {
        var response = restClient
                .post()
                .uri("/v1/drafts/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateNotes)
                .retrieve()
                .toBodilessEntity();
        if (!response.getStatusCode().is2xxSuccessful()) {
            logger.error(
                    "Got status code '{}' with when updating notes",
                    response.getStatusCode().value());
        }
    }
}
