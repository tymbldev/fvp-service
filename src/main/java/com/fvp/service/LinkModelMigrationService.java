package com.fvp.service;

import com.fvp.entity.BaseLinkModel;
import com.fvp.entity.LinkModel;
import com.fvp.repository.LinkModelRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkModelMigrationService {

  private static final Logger logger = LoggerFactory.getLogger(LinkModelMigrationService.class);
  private static final int BATCH_SIZE = 1000;

  @Value("${migration.model.chunkSize:100}")
  private int modelChunkSize;

  @Autowired
  private LinkModelRepository linkModelRepository;

  /**
   * Get the current chunk size for model migration
   *
   * @return the chunk size
   */
  public int getModelChunkSize() {
    return modelChunkSize;
  }

  /**
   * Set the chunk size for model migration
   *
   * @param chunkSize the new chunk size
   */
  public void setModelChunkSize(int chunkSize) {
    if (chunkSize <= 0) {
      throw new IllegalArgumentException("Chunk size must be greater than zero");
    }
    this.modelChunkSize = chunkSize;
    logger.info("Model migration chunk size set to {}", chunkSize);
  }
} 