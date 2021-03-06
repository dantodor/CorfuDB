package org.corfudb.protocols.wireprotocol;

import com.esotericsoftware.kryo.NotNull;
import com.google.common.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by mwei on 9/18/15.
 */
public interface IMetadata {

    Map<Byte, LogUnitMetadataType> metadataTypeMap =
            Arrays.<LogUnitMetadataType>stream(LogUnitMetadataType.values())
                    .collect(Collectors.toMap(LogUnitMetadataType::asByte, Function.identity()));

    EnumMap<IMetadata.LogUnitMetadataType, Object> getMetadataMap();

    /**
     * Get the streams that belong to this append.
     *
     * @return A set of streams that belong to this append.
     */
    @SuppressWarnings("unchecked")
    default Set<UUID> getStreams() {
        return (Set<UUID>) getMetadataMap().getOrDefault(
                LogUnitMetadataType.STREAM,
                // Handle a special condition in the Replex case,
                // where streams are stored in a stream address map instead.
                getStreamAddressMap() == null ? Collections.EMPTY_SET :
                getStreamAddressMap().keySet());
    }

    /**
     * Get whether or not this entry contains a given stream.
     * @param stream    The stream to check.
     * @return          True, if the entry contains the given stream.
     */
    default boolean containsStream(UUID stream) {
        return  getBackpointerMap().keySet().contains(stream) ||
                getStreams().contains(stream);
    }

    /**
     * Set the streams that belong to this append.
     *
     * @param streams The set of streams that will belong to this append.
     */
    default void setStreams(Set<UUID> streams) {
        getMetadataMap().put(IMetadata.LogUnitMetadataType.STREAM, streams);
    }

    /**
     * Get the rank of this append.
     *
     * @return The rank of this append.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    default DataRank getRank() {
        return (DataRank) getMetadataMap().getOrDefault(LogUnitMetadataType.RANK,
                null);
    }

    /**
     * Set the rank of this append.
     *
     * @param rank The rank of this append.
     */
    default void setRank(@Nullable DataRank rank) {
        EnumMap<LogUnitMetadataType, Object> map = getMetadataMap();
        if (rank != null) {
            map.put(LogUnitMetadataType.RANK, rank);
        } else {
            if (map.containsKey(LogUnitMetadataType.RANK)) {
                map.remove(LogUnitMetadataType.RANK);
            }
        }
    }

    /**
     * Get the logical stream addresses that belong to this append.
     *
     * @return A map of UUID to logical stream addresses that belong to this append.
     */
    @SuppressWarnings("unchecked")
    default Map<UUID, Long> getLogicalAddresses() {
        return (Map<UUID, Long>) getMetadataMap().getOrDefault(IMetadata.LogUnitMetadataType.STREAM_ADDRESSES,
                Collections.EMPTY_MAP);
    }

    /**
     * Set the logical stream addresses that belong to this append.
     *
     * @param streams The map from UUID to logical stream addresses that will belong to this append.
     */
    default void setLogicalAddresses(Map<UUID, Long> streams) {
        getMetadataMap().put(IMetadata.LogUnitMetadataType.STREAM_ADDRESSES, streams);
    }

    @SuppressWarnings("unchecked")
    default Map<UUID, Long> getBackpointerMap() {
        return (Map<UUID, Long>) getMetadataMap().getOrDefault(LogUnitMetadataType.BACKPOINTER_MAP,
                Collections.EMPTY_MAP);
    }

    default void setBackpointerMap(Map<UUID, Long> backpointerMap) {
        getMetadataMap().put(LogUnitMetadataType.BACKPOINTER_MAP, backpointerMap);
    }

    default void setGlobalAddress(Long address) {
        getMetadataMap().put(LogUnitMetadataType.GLOBAL_ADDRESS, address);
    }

    @SuppressWarnings("unchecked")
    default Long getGlobalAddress() {
        if (getMetadataMap() == null || getMetadataMap().get(LogUnitMetadataType.GLOBAL_ADDRESS) == null) {
            return -1L;
        }
        return Optional.ofNullable((Long) getMetadataMap().get(LogUnitMetadataType.GLOBAL_ADDRESS)).orElse((long) -1);
    }

    @SuppressWarnings("unchecked")
    default Map<UUID,Long> getStreamAddressMap() {
        return ((Map<UUID,Long>) getMetadataMap()
                .get(LogUnitMetadataType.STREAM_ADDRESSES));
    }

    @SuppressWarnings("unchecked")
    default Long getStreamAddress(UUID stream) {
        return ((Map<UUID,Long>) getMetadataMap().get(LogUnitMetadataType.STREAM_ADDRESSES)) == null ? null :
                ((Map<UUID,Long>) getMetadataMap().get(LogUnitMetadataType.STREAM_ADDRESSES)).get(stream);
    }


    default void clearCommit() {
        getMetadataMap().put(LogUnitMetadataType.COMMIT, false);
    }

    default void setCommit() {
        getMetadataMap().put(LogUnitMetadataType.COMMIT, true);
    }

    @RequiredArgsConstructor
    public enum LogUnitMetadataType implements ITypedEnum {
        STREAM(0, new TypeToken<Set<UUID>>() {}),
        RANK(1, TypeToken.of(DataRank.class)),
        STREAM_ADDRESSES(2, new TypeToken<Map<UUID, Long>>() {}),
        BACKPOINTER_MAP(3, new TypeToken<Map<UUID, Long>>() {}),
        GLOBAL_ADDRESS(4, TypeToken.of(Long.class)),
        COMMIT(5, TypeToken.of(Boolean.class)),
        ;
        final int type;
        @Getter
        final TypeToken<?> componentType;

        public byte asByte() {
            return (byte) type;
        }

        public static Map<Byte, LogUnitMetadataType> typeMap =
                Arrays.<LogUnitMetadataType>stream(LogUnitMetadataType.values())
                        .collect(Collectors.toMap(LogUnitMetadataType::asByte, Function.identity()));
    }

    @Value
    @AllArgsConstructor
    class DataRank implements Comparable<DataRank> {
        public long rank;
        @NotNull
        public UUID uuid;

        public DataRank(long rank) {
            this(rank, UUID.randomUUID());
        }

        @Override
        public int compareTo(DataRank o) {
            int rankCompared = Long.compare(this.rank, o.rank);
            if (rankCompared==0) {
                return uuid.compareTo(o.getUuid());
            } else {
                return rankCompared;
            }
        }
    }

    
}
