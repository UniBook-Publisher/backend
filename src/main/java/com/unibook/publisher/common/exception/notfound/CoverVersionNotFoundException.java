package com.unibook.publisher.common.exception.notfound;

import java.util.UUID;

public class CoverVersionNotFoundException extends ResourceNotFoundException {
    public CoverVersionNotFoundException(int versionNumber, UUID manuscriptId) {
        super(String.format("Версію обкладинки з номером %d для рукопису %s не знайдено", versionNumber, manuscriptId));
    }
}
