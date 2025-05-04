package com.fvp.service;

import com.fvp.entity.Link;
import com.fvp.entity.Model;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ModelProcessingService {

  private static final Logger logger = LoggerFactory.getLogger(ModelProcessingService.class);

  private final ModelService modelService;
  private final LinkModelShardingService linkModelShardingService;

  public ModelProcessingService(
      ModelService modelService,
      LinkModelShardingService linkModelShardingService) {
    this.modelService = modelService;
    this.linkModelShardingService = linkModelShardingService;
  }

  public void processModels(Link link, String models) {
    Set<String> modelSetToBeAdded = new HashSet<>();

    List<String> values = tokenize(models);
    if (values == null || values.isEmpty()) {
      return;
    }
    // For each token from the input
    for (String token : values) {
      String model = token.trim();
      if (!model.isEmpty()) {

        try {
          Model modelObject = modelService.getAllModels().get(token);
          if (modelObject == null) {
            modelSetToBeAdded.add(model);
          }
        } catch (Exception e) {
          logger.debug("No exact match found for model: {}", model);
        }
      }
    }
    // Save all matched models
    int savedCount = 0;
    for (String modelName : modelSetToBeAdded) {
      try {
        // Create base entity
        Model model = new Model();
        model.setName(modelName);
        model.setDataPresent(0);
        model.setThumbnail("");
        model.setThumbpath("");
        model.setTenantId(link.getTenantId());
        modelService.saveModel(model);
        savedCount++;
      } catch (Exception e) {
        logger.error("Error saving model '{}' for link ID {}: {}",
            modelName, link.getId(), e.getMessage(), e);
      }
    }
    logger.info("Saved {} entries to sharded link_model tables for link ID {} and model {} ",
        savedCount, link.getId(), link.getStar());
  }

  private List<String> tokenize(String input) {
    Gson gson = new Gson();
    Type listType = new TypeToken<List<String>>() {
    }.getType();
    return gson.fromJson(input, listType);
  }

}
