package com.klabis.common.sync;

import org.springframework.core.convert.converter.Converter;

/**
 * Configuration of synchronization between two sources
 * @param localSource
 * @param externalSource
 * @param converter
 * @param reverseConverter
 * @param <L>
 * @param <E>
 */
public record SyncLine<L extends SyncData, E extends SyncData>(SyncSource<L> localSource, SyncSource<E> externalSource,
                                                               Converter<L, E> converter,
                                                               Converter<E, L> reverseConverter) {

    public static <T extends SyncData> SyncLine<T, T> withoutMapping(SyncSource<T> localSource, SyncSource<T> externalSource) {
        return new SyncLine<>(localSource, externalSource, t -> t, t -> t);
    }

    public SyncLine {
        if (!externalSource.isOppositeOf(localSource)) {
            throw new IllegalArgumentException("External source must be opposite of local source");
        }
    }

    public boolean canProcess(SyncId syncId) {
        return localSource.matches(syncId) || externalSource.matches(syncId);
    }

    public SyncId pull(SyncId externalId) {
        if (!externalId.isExternalId()) {
            throw new IllegalArgumentException("to pull, external ID must be provided");
        }
        return externalSource.fetch(externalId)
                .map(externalData -> {
                    L localData = reverseConverter.convert(externalData);
                    return localSource.save(localData);
                })
                .orElseThrow(() -> new IllegalStateException("External item with ID %s not found".formatted(externalId)));
    }

    public SyncId push(SyncId localId) {
        if (!localId.isLocalId()) {
            throw new IllegalArgumentException("to push, local ID must be provided");
        }

        return localSource.fetch(localId).map(localData -> {
            E externalData = converter.convert(localData);
            return externalSource.save(externalData);
        }).orElseThrow(() -> new IllegalStateException("Local item with ID %s not found".formatted(localId)));
    }
}
