package com.fvp.repository.generated;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Utility class to generate repository interfaces for LinkCategoryShard and LinkModelShard This is
 * a build-time utility and should not be included in the final application
 */
public class GenerateRepositories {

  private static final String REPOSITORY_PACKAGE = "com.fvp.repository";
  private static final String ENTITY_PACKAGE = "com.fvp.entity";

  private static final String CATEGORY_REPO_TEMPLATE =
      "package " + REPOSITORY_PACKAGE + ";\n\n" +
          "import " + ENTITY_PACKAGE + ".LinkCategoryShard%1$d;\n" +
          "import java.util.List;\n" +
          "import java.util.Optional;\n" +
          "import org.springframework.data.jpa.repository.Query;\n" +
          "import org.springframework.data.repository.query.Param;\n" +
          "import org.springframework.stereotype.Repository;\n\n" +
          "@Repository\n" +
          "public interface LinkCategoryShard%1$dRepository extends\n" +
          "    ShardedLinkCategoryRepository<LinkCategoryShard%1$d, Integer> {\n\n" +
          "  @Query(value = \"SELECT lc.* FROM link_category_shard_%1$d lc JOIN link l ON lc.link_id = l.id \" +\n"
          +
          "      \"WHERE lc.tenant_id = :tenantId AND lc.category = :category \" +\n" +
          "      \"AND lc.created_on >= DATE_SUB(NOW(), INTERVAL 3 DAY) \" +\n" +
          "      \"AND l.thumb_path_processed = 1 \" +\n" +
          "      \"ORDER BY RAND() LIMIT 1\",\n" +
          "      nativeQuery = true)\n" +
          "  Optional<LinkCategoryShard%1$d> findRandomRecentLinkByCategory(\n" +
          "      @Param(\"tenantId\") Integer tenantId,\n" +
          "      @Param(\"category\") String category\n" +
          "  );\n\n" +
          "  @Query(value = \"SELECT lc.* FROM link_category_shard_%1$d lc JOIN link l ON lc.link_id = l.id \" +\n"
          +
          "      \"WHERE lc.tenant_id = :tenantId AND lc.category = :category \" +\n" +
          "      \"AND l.thumb_path_processed = 1 \" +\n" +
          "      \"ORDER BY RAND() LIMIT 1\",\n" +
          "      nativeQuery = true)\n" +
          "  Optional<LinkCategoryShard%1$d> findRandomLinkByCategory(\n" +
          "      @Param(\"tenantId\") Integer tenantId,\n" +
          "      @Param(\"category\") String category\n" +
          "  );\n\n" +
          "  @Query(value =\n" +
          "      \"SELECT COUNT(lc.id) AS count FROM link_category_shard_%1$d lc JOIN link l ON lc.link_id = l.id \"\n"
          +
          "          +\n" +
          "          \"WHERE lc.tenant_id = :tenantId AND lc.category = :category \" +\n" +
          "          \"AND l.thumb_path_processed = 1\",\n" +
          "      nativeQuery = true)\n" +
          "  Long countByTenantIdAndCategory(\n" +
          "      @Param(\"tenantId\") Integer tenantId,\n" +
          "      @Param(\"category\") String category\n" +
          "  );\n\n" +
          "  @Query(value = \"SELECT lc.* FROM link_category_shard_%1$d lc JOIN link l ON lc.link_id = l.id \" +\n"
          +
          "      \"WHERE lc.tenant_id = :tenantId AND lc.link_id = :linkId \" +\n" +
          "      \"AND l.thumb_path_processed = 1\",\n" +
          "      nativeQuery = true)\n" +
          "  List<LinkCategoryShard%1$d> findByTenantIdAndLinkId(\n" +
          "      @Param(\"tenantId\") Integer tenantId,\n" +
          "      @Param(\"linkId\") Integer linkId\n" +
          "  );\n\n" +
          "  @Query(value = \"SELECT DISTINCT lc.category FROM link_category_shard_%1$d lc \" +\n" +
          "      \"WHERE lc.tenant_id = :tenantId\",\n" +
          "      nativeQuery = true)\n" +
          "  List<String> findAllDistinctCategories(\n" +
          "      @Param(\"tenantId\") Integer tenantId\n" +
          "  );\n\n" +
          "  @Query(value = \"SELECT lc.* FROM link_category_shard_%1$d lc JOIN link l ON lc.link_id = l.id \" +\n"
          +
          "      \"WHERE lc.tenant_id = :tenantId AND lc.category = :category \" +\n" +
          "      \"AND l.thumb_path_processed = 1\",\n" +
          "      nativeQuery = true)\n" +
          "  List<LinkCategoryShard%1$d> findByTenantIdAndCategory(\n" +
          "      @Param(\"tenantId\") Integer tenantId,\n" +
          "      @Param(\"category\") String category\n" +
          "  );\n\n" +
          "  @Query(value = \"SELECT lc.* FROM link_category_shard_%1$d lc JOIN link l ON lc.link_id = l.id \" +\n"
          +
          "      \"WHERE lc.tenant_id = :tenantId AND lc.category = :category \" +\n" +
          "      \"AND l.thumb_path_processed = 1 \" +\n" +
          "      \"ORDER BY lc.random_order\",\n" +
          "      nativeQuery = true)\n" +
          "  List<LinkCategoryShard%1$d> findByTenantIdAndCategoryOrderByRandomOrder(\n" +
          "      @Param(\"tenantId\") Integer tenantId,\n" +
          "      @Param(\"category\") String category\n" +
          "  );\n\n" +
          "  @Query(value = \"SELECT lc.* FROM link_category_shard_%1$d lc JOIN link l ON lc.link_id = l.id \" +\n"
          +
          "      \"WHERE lc.tenant_id = :tenantId AND lc.category = :category \" +\n" +
          "      \"AND (:minDuration IS NULL OR l.duration >= :minDuration) \" +\n" +
          "      \"AND (:maxDuration IS NULL OR l.duration <= :maxDuration) \" +\n" +
          "      \"AND (:quality IS NULL OR :quality = '' OR l.quality = :quality) \" +\n" +
          "      \"AND l.thumb_path_processed = 1 \" +\n" +
          "      \"ORDER BY lc.random_order LIMIT :limit OFFSET :offset\",\n" +
          "      nativeQuery = true)\n" +
          "  List<LinkCategoryShard%1$d> findByCategoryWithFiltersPageable(\n" +
          "      @Param(\"tenantId\") Integer tenantId,\n" +
          "      @Param(\"category\") String category,\n" +
          "      @Param(\"minDuration\") Integer minDuration,\n" +
          "      @Param(\"maxDuration\") Integer maxDuration,\n" +
          "      @Param(\"quality\") String quality,\n" +
          "      @Param(\"offset\") int offset,\n" +
          "      @Param(\"limit\") int limit\n" +
          "  );\n\n" +
          "  @Query(value =\n" +
          "      \"SELECT COUNT(lc.id) AS count FROM link_category_shard_%1$d lc JOIN link l ON lc.link_id = l.id \"\n"
          +
          "          +\n" +
          "          \"WHERE lc.tenant_id = :tenantId AND lc.category = :category \" +\n" +
          "          \"AND (:minDuration IS NULL OR l.duration >= :minDuration) \" +\n" +
          "          \"AND (:maxDuration IS NULL OR l.duration <= :maxDuration) \" +\n" +
          "          \"AND (:quality IS NULL OR :quality = '' OR l.quality = :quality) \" +\n" +
          "          \"AND l.thumb_path_processed = 1\",\n" +
          "      nativeQuery = true)\n" +
          "  Long countByCategoryWithFilters(\n" +
          "      @Param(\"tenantId\") Integer tenantId,\n" +
          "      @Param(\"category\") String category,\n" +
          "      @Param(\"minDuration\") Integer minDuration,\n" +
          "      @Param(\"maxDuration\") Integer maxDuration,\n" +
          "      @Param(\"quality\") String quality\n" +
          "  );\n\n" +
          "  @Query(value = \"SELECT lc.* FROM link_category_shard_%1$d lc JOIN link l ON lc.link_id = l.id \" +\n"
          +
          "      \"WHERE lc.tenant_id = :tenantId AND lc.category = :category \" +\n" +
          "      \"AND (:minDuration IS NULL OR l.duration >= :minDuration) \" +\n" +
          "      \"AND (:maxDuration IS NULL OR l.duration <= :maxDuration) \" +\n" +
          "      \"AND (:quality IS NULL OR :quality = '' OR l.quality = :quality) \" +\n" +
          "      \"AND l.id != :excludeId \" +\n" +
          "      \"AND l.thumb_path_processed = 1 \" +\n" +
          "      \"ORDER BY lc.random_order LIMIT :limit OFFSET :offset\",\n" +
          "      nativeQuery = true)\n" +
          "  List<LinkCategoryShard%1$d> findByCategoryWithFiltersExcludingLinkPageable(\n" +
          "      @Param(\"tenantId\") Integer tenantId,\n" +
          "      @Param(\"category\") String category,\n" +
          "      @Param(\"minDuration\") Integer minDuration,\n" +
          "      @Param(\"maxDuration\") Integer maxDuration,\n" +
          "      @Param(\"quality\") String quality,\n" +
          "      @Param(\"excludeId\") Integer excludeId,\n" +
          "      @Param(\"offset\") int offset,\n" +
          "      @Param(\"limit\") int limit\n" +
          "  );\n\n" +
          "  @Query(value = \"SELECT lc.* FROM link_category_shard_%1$d lc JOIN link l ON lc.link_id = l.id \" +\n"
          +
          "      \"WHERE lc.tenant_id = :tenantId \" +\n" +
          "      \"AND lc.category IN :categoryNames \" +\n" +
          "      \"AND l.thumb_path_processed = 1 \" +\n" +
          "      \"GROUP BY lc.category \" +\n" +
          "      \"ORDER BY RAND()\",\n" +
          "      nativeQuery = true)\n" +
          "  List<LinkCategoryShard%1$d> findRandomLinksByCategoryNames(\n" +
          "      @Param(\"tenantId\") Integer tenantId,\n" +
          "      @Param(\"categoryNames\") List<String> categoryNames\n" +
          "  );\n\n" +
          "  @Query(value =\n" +
          "      \"SELECT lc.category, COUNT(lc.id) as count FROM link_category_shard_%1$d lc JOIN link l ON lc.link_id = l.id \"\n"
          +
          "          +\n" +
          "          \"WHERE lc.tenant_id = :tenantId AND lc.category IN :categoryNames \" +\n" +
          "          \"AND l.thumb_path_processed = 1 \" +\n" +
          "          \"GROUP BY lc.category\",\n" +
          "      nativeQuery = true)\n" +
          "  List<Object[]> countByTenantIdAndCategories(\n" +
          "      @Param(\"tenantId\") Integer tenantId,\n" +
          "      @Param(\"categoryNames\") List<String> categoryNames\n" +
          "  );\n" +
          "}";

  private static final String MODEL_REPO_TEMPLATE =
      "package " + REPOSITORY_PACKAGE + ";\n\n" +
          "import " + ENTITY_PACKAGE + ".LinkModelShard%1$d;\n" +
          "import java.util.List;\n" +
          "import java.util.Optional;\n" +
          "import org.springframework.data.jpa.repository.Modifying;\n" +
          "import org.springframework.data.jpa.repository.Query;\n" +
          "import org.springframework.data.repository.query.Param;\n" +
          "import org.springframework.stereotype.Repository;\n" +
          "import org.springframework.transaction.annotation.Transactional;\n\n" +
          "@Repository\n" +
          "public interface LinkModelShard%1$dRepository extends ShardedLinkModelRepository<LinkModelShard%1$d> {\n\n"
          +
          "  @Override\n" +
          "  @Query(value = \"SELECT lm.* FROM link_model_shard_%1$d lm WHERE lm.tenant_id = :tenantId\",\n"
          +
          "      nativeQuery = true)\n" +
          "  List<LinkModelShard%1$d> findByTenantId(@Param(\"tenantId\") Integer tenantId);\n\n" +
          "  @Override\n" +
          "  @Query(value = \"SELECT lm.* FROM link_model_shard_%1$d lm WHERE lm.link_id = :linkId\",\n"
          +
          "      nativeQuery = true)\n" +
          "  List<LinkModelShard%1$d> findByLinkId(@Param(\"linkId\") Integer linkId);\n\n" +
          "  @Override\n" +
          "  @Query(value = \"SELECT lm.* FROM link_model_shard_%1$d lm WHERE lm.model = :model AND lm.tenant_id = :tenantId\",\n"
          +
          "      nativeQuery = true)\n" +
          "  List<LinkModelShard%1$d> findByModelAndTenantId(@Param(\"model\") String model,\n" +
          "      @Param(\"tenantId\") Integer tenantId);\n\n" +
          "  @Override\n" +
          "  @Query(value = \"SELECT lm.* FROM link_model_shard_%1$d lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1\",\n"
          +
          "      nativeQuery = true)\n" +
          "  List<LinkModelShard%1$d> findByTenantIdAndModel(@Param(\"tenantId\") Integer tenantId,\n"
          +
          "      @Param(\"model\") String model);\n\n" +
          "  @Override\n" +
          "  @Query(value = \"SELECT lm.* FROM link_model_shard_%1$d lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1 ORDER BY RAND() LIMIT 1\",\n"
          +
          "      nativeQuery = true)\n" +
          "  Optional<LinkModelShard%1$d> findRandomLinkByModel(@Param(\"tenantId\") Integer tenantId,\n"
          +
          "      @Param(\"model\") String model);\n\n" +
          "  @Override\n" +
          "  @Query(value = \"SELECT lm.* FROM link_model_shard_%1$d lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1 ORDER BY lm.random_order\",\n"
          +
          "      nativeQuery = true)\n" +
          "  List<LinkModelShard%1$d> findByTenantIdAndModelOrderByRandomOrder(\n" +
          "      @Param(\"tenantId\") Integer tenantId, @Param(\"model\") String model);\n\n" +
          "  @Override\n" +
          "  @Query(value = \"SELECT COUNT(lm.id) AS count FROM link_model_shard_%1$d lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1\",\n"
          +
          "      nativeQuery = true)\n" +
          "  Long countByTenantIdAndModel(@Param(\"tenantId\") Integer tenantId, @Param(\"model\") String model);\n\n"
          +
          "  @Override\n" +
          "  @Query(value = \"SELECT lm.* FROM link_model_shard_%1$d lm JOIN link l ON lm.link_id = l.id \" +\n"
          +
          "      \"WHERE lm.tenant_id = :tenantId AND lm.model IN :modelNames \" +\n" +
          "      \"AND l.thumb_path_processed = 1 \" +\n" +
          "      \"GROUP BY lm.model \" +\n" +
          "      \"ORDER BY RAND()\",\n" +
          "      nativeQuery = true)\n" +
          "  List<LinkModelShard%1$d> findRandomLinksByModelNames(\n" +
          "      @Param(\"tenantId\") Integer tenantId,\n" +
          "      @Param(\"modelNames\") List<String> modelNames\n" +
          "  );\n\n" +
          "  @Override\n" +
          "  @Query(value =\n" +
          "      \"SELECT lm.model, COUNT(lm.id) as count FROM link_model_shard_%1$d lm JOIN link l ON lm.link_id = l.id \"\n"
          +
          "          +\n" +
          "          \"WHERE lm.tenant_id = :tenantId AND lm.model IN :modelNames \" +\n" +
          "          \"AND l.thumb_path_processed = 1 \" +\n" +
          "          \"GROUP BY lm.model\",\n" +
          "      nativeQuery = true)\n" +
          "  List<Object[]> countByTenantIdAndModels(\n" +
          "      @Param(\"tenantId\") Integer tenantId,\n" +
          "      @Param(\"modelNames\") List<String> modelNames\n" +
          "  );\n\n" +
          "  @Override\n" +
          "  @Query(value = \"SELECT lm.* FROM link_model_shard_%1$d lm JOIN link l ON lm.link_id = l.id \" +\n"
          +
          "      \"WHERE lm.tenant_id = :tenantId AND lm.model = :model \" +\n" +
          "      \"AND (:maxDuration IS NULL OR l.duration <= :maxDuration) \" +\n" +
          "      \"AND (:quality IS NULL OR :quality = '' OR l.quality = :quality) \" +\n" +
          "      \"AND l.thumb_path_processed = 1 \" +\n" +
          "      \"ORDER BY lm.random_order LIMIT :limit OFFSET :offset\",\n" +
          "      nativeQuery = true)\n" +
          "  List<LinkModelShard%1$d> findByModelWithFiltersPageable(\n" +
          "      @Param(\"tenantId\") Integer tenantId,\n" +
          "      @Param(\"model\") String model,\n" +
          "      @Param(\"maxDuration\") Integer maxDuration,\n" +
          "      @Param(\"quality\") String quality,\n" +
          "      @Param(\"offset\") int offset,\n" +
          "      @Param(\"limit\") int limit\n" +
          "  );\n" +
          "  \n" +
          "  @Override\n" +
          "  @Query(value = \"SELECT DISTINCT lm.model FROM link_model_shard_%1$d lm \" +\n" +
          "      \"JOIN link l ON lm.link_id = l.id \" +\n" +
          "      \"WHERE lm.tenant_id = :tenantId \" +\n" +
          "      \"AND l.thumb_path_processed = 1\",\n" +
          "      nativeQuery = true)\n" +
          "  List<String> findAllDistinctModels(@Param(\"tenantId\") Integer tenantId);\n" +
          "  \n" +
          "  @Override\n" +
          "  @Query(value = \"SELECT lm.* FROM link_model_shard_%1$d lm JOIN link l ON lm.link_id = l.id \" +\n"
          +
          "      \"WHERE lm.tenant_id = :tenantId AND lm.model = :model \" +\n" +
          "      \"AND (:maxDuration IS NULL OR l.duration <= :maxDuration) \" +\n" +
          "      \"AND (:quality IS NULL OR :quality = '' OR l.quality = :quality) \" +\n" +
          "      \"AND l.thumb_path_processed = 1 \" +\n" +
          "      \"AND l.id != :excludeId \" +\n" +
          "      \"ORDER BY lm.random_order LIMIT :limit OFFSET :offset\",\n" +
          "      nativeQuery = true)\n" +
          "  List<LinkModelShard%1$d> findByModelWithFiltersExcludingLinkPageable(\n" +
          "      @Param(\"tenantId\") Integer tenantId,\n" +
          "      @Param(\"model\") String model,\n" +
          "      @Param(\"maxDuration\") Integer maxDuration,\n" +
          "      @Param(\"quality\") String quality,\n" +
          "      @Param(\"excludeId\") Integer excludeId,\n" +
          "      @Param(\"offset\") int offset,\n" +
          "      @Param(\"limit\") int limit\n" +
          "  );\n" +
          "  \n" +
          "  @Override\n" +
          "  @Query(value = \"SELECT COUNT(lm.id) FROM link_model_shard_%1$d lm JOIN link l ON lm.link_id = l.id \" +\n"
          +
          "      \"WHERE lm.tenant_id = :tenantId AND lm.model = :model \" +\n" +
          "      \"AND (:maxDuration IS NULL OR l.duration <= :maxDuration) \" +\n" +
          "      \"AND (:quality IS NULL OR :quality = '' OR l.quality = :quality) \" +\n" +
          "      \"AND l.thumb_path_processed = 1\",\n" +
          "      nativeQuery = true)\n" +
          "  Long countByModelWithFilters(\n" +
          "      @Param(\"tenantId\") Integer tenantId,\n" +
          "      @Param(\"model\") String model,\n" +
          "      @Param(\"maxDuration\") Integer maxDuration,\n" +
          "      @Param(\"quality\") String quality\n" +
          "  );\n" +
          "  \n" +
          "  @Override\n" +
          "  @Modifying\n" +
          "  @Transactional\n" +
          "  @Query(value = \"DELETE FROM link_model_shard_%1$d WHERE link_id = :linkId\", nativeQuery = true)\n"
          +
          "  void deleteByLinkId(@Param(\"linkId\") Integer linkId);\n" +
          "}";

  public static void main(String[] args) {
    // Generate from shard 14 to 50 (we already manually created 11-13)
    for (int i = 14; i <= 50; i++) {
      generateCategoryRepository(i);
      generateModelRepository(i);
    }
    System.out.println("Repository generation completed.");
  }

  private static void generateCategoryRepository(int shardNumber) {
    String className = "LinkCategoryShard" + shardNumber + "Repository";
    String content = String.format(CATEGORY_REPO_TEMPLATE, shardNumber);
    String filePath = "src/main/java/com/fvp/repository/" + className + ".java";
    writeToFile(filePath, content);
  }

  private static void generateModelRepository(int shardNumber) {
    String className = "LinkModelShard" + shardNumber + "Repository";
    String content = String.format(MODEL_REPO_TEMPLATE, shardNumber);
    String filePath = "src/main/java/com/fvp/repository/" + className + ".java";
    writeToFile(filePath, content);
  }

  private static void writeToFile(String filePath, String content) {
    try (FileWriter writer = new FileWriter(filePath)) {
      writer.write(content);
      System.out.println("Generated " + filePath);
    } catch (IOException e) {
      System.err.println("Error writing to file " + filePath + ": " + e.getMessage());
    }
  }
} 