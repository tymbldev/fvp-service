package com.fvp.entity.generated;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Utility class to generate entity classes for LinkCategoryShard and LinkModelShard
 * This is a build-time utility and should not be included in the final application
 */
public class GenerateEntities {

  private static final String ENTITY_PACKAGE = "com.fvp.entity";
  private static final String CATEGORY_TEMPLATE = 
      "package " + ENTITY_PACKAGE + ";\n\n" +
      "import javax.persistence.Entity;\n" +
      "import javax.persistence.Table;\n" +
      "import lombok.Data;\n" +
      "import lombok.NoArgsConstructor;\n\n" +
      "@Data\n" +
      "@Entity\n" +
      "@Table(name = \"link_category_shard_%d\")\n" +
      "@NoArgsConstructor\n" +
      "public class LinkCategoryShard%d extends BaseLinkCategory {\n\n" +
      "}";
      
  private static final String MODEL_TEMPLATE =
      "package " + ENTITY_PACKAGE + ";\n\n" +
      "import javax.persistence.Entity;\n" +
      "import javax.persistence.Table;\n\n" +
      "@Entity\n" +
      "@Table(name = \"link_model_shard_%d\")\n" +
      "public class LinkModelShard%d extends BaseLinkModel {\n\n" +
      "}";
      
  public static void main(String[] args) {
    // Generate from shard 13 to 50 (we already manually created 11-12)
    for (int i = 14; i <= 50; i++) {
      generateCategoryEntity(i);
      generateModelEntity(i);
    }
    System.out.println("Entity generation completed.");
  }
  
  private static void generateCategoryEntity(int shardNumber) {
    String className = "LinkCategoryShard" + shardNumber;
    String content = String.format(CATEGORY_TEMPLATE, shardNumber, shardNumber);
    String filePath = "src/main/java/com/fvp/entity/" + className + ".java";
    writeToFile(filePath, content);
  }
  
  private static void generateModelEntity(int shardNumber) {
    String className = "LinkModelShard" + shardNumber;
    String content = String.format(MODEL_TEMPLATE, shardNumber, shardNumber);
    String filePath = "src/main/java/com/fvp/entity/" + className + ".java";
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