package com.unibook.publisher.common.exception.notfound;

import java.util.UUID;

public class RevisionNotFoundException extends ResourceNotFoundException {
    public RevisionNotFoundException(UUID revisionId) {
        super("Ревізію", revisionId);
    }

    public RevisionNotFoundException(UUID chapterId, Integer versionNumber) {
        super(String.format("Ревізію для розділу %s з номером %d не знайдено", chapterId, versionNumber));
    }
}
