package com.fvp.util;

import java.util.zip.CRC32;

public class ShardHashingUtil {
    private static final int TOTAL_SHARDS = 50;
    private static final int MIN_SHARD = 11; // Starting from shard 11

    /**
     * Calculates the shard number for a given link ID
     * @param linkId The link ID to hash
     * @return The shard number (between 11 and 50)
     */
    public static int calculateShard(int linkId) {
        CRC32 crc32 = new CRC32();
        crc32.update(linkId);
        long hash = crc32.getValue();
        
        // Map the hash to a shard number between 11 and 50
        int shard = (int) (Math.abs(hash) % TOTAL_SHARDS) + MIN_SHARD;
        return shard;
    }

    /**
     * Gets the repository class name for a given shard number
     * @param shardNumber The shard number
     * @param isCategory If true, returns category repository, otherwise model repository
     * @return The fully qualified repository class name
     */
    public static String getRepositoryClassName(int shardNumber, boolean isCategory) {
        String prefix = isCategory ? "LinkCategoryShard" : "LinkModelShard";
        return "com.fvp.repository." + prefix + shardNumber + "Repository";
    }

    /**
     * Gets the entity class name for a given shard number
     * @param shardNumber The shard number
     * @param isCategory If true, returns category entity, otherwise model entity
     * @return The fully qualified entity class name
     */
    public static String getEntityClassName(int shardNumber, boolean isCategory) {
        String prefix = isCategory ? "LinkCategoryShard" : "LinkModelShard";
        return "com.fvp.entity." + prefix + shardNumber;
    }
} 