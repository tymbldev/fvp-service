-- Link Model Shard Indexes Creation Script
-- This script creates necessary indexes for all 50 shards of the link_model table
-- Created based on query patterns in LinkModelDynamicQueryBuilder

-- Shard 1
ALTER TABLE link_model_shard_1 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_1 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_1 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_1 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_1 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_1 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_1 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 2
ALTER TABLE link_model_shard_2 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_2 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_2 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_2 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_2 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_2 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_2 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 3
ALTER TABLE link_model_shard_3 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_3 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_3 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_3 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_3 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_3 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_3 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 4
ALTER TABLE link_model_shard_4 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_4 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_4 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_4 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_4 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_4 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_4 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 5
ALTER TABLE link_model_shard_5 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_5 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_5 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_5 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_5 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_5 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_5 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 6
ALTER TABLE link_model_shard_6 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_6 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_6 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_6 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_6 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_6 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_6 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 7
ALTER TABLE link_model_shard_7 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_7 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_7 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_7 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_7 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_7 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_7 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 8
ALTER TABLE link_model_shard_8 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_8 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_8 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_8 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_8 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_8 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_8 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 9
ALTER TABLE link_model_shard_9 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_9 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_9 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_9 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_9 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_9 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_9 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 10
ALTER TABLE link_model_shard_10 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_10 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_10 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_10 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_10 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_10 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_10 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 11
ALTER TABLE link_model_shard_11 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_11 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_11 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_11 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_11 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_11 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_11 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 12
ALTER TABLE link_model_shard_12 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_12 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_12 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_12 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_12 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_12 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_12 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 13
ALTER TABLE link_model_shard_13 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_13 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_13 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_13 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_13 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_13 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_13 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 14
ALTER TABLE link_model_shard_14 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_14 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_14 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_14 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_14 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_14 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_14 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 15
ALTER TABLE link_model_shard_15 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_15 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_15 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_15 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_15 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_15 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_15 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 16
ALTER TABLE link_model_shard_16 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_16 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_16 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_16 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_16 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_16 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_16 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 17
ALTER TABLE link_model_shard_17 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_17 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_17 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_17 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_17 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_17 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_17 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 18
ALTER TABLE link_model_shard_18 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_18 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_18 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_18 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_18 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_18 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_18 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 19
ALTER TABLE link_model_shard_19 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_19 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_19 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_19 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_19 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_19 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_19 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 20
ALTER TABLE link_model_shard_20 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_20 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_20 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_20 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_20 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_20 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_20 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 21
ALTER TABLE link_model_shard_21 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_21 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_21 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_21 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_21 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_21 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_21 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 22
ALTER TABLE link_model_shard_22 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_22 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_22 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_22 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_22 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_22 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_22 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 23
ALTER TABLE link_model_shard_23 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_23 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_23 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_23 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_23 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_23 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_23 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 24
ALTER TABLE link_model_shard_24 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_24 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_24 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_24 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_24 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_24 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_24 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 25
ALTER TABLE link_model_shard_25 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_25 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_25 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_25 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_25 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_25 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_25 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 26
ALTER TABLE link_model_shard_26 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_26 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_26 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_26 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_26 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_26 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_26 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 27
ALTER TABLE link_model_shard_27 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_27 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_27 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_27 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_27 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_27 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_27 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 28
ALTER TABLE link_model_shard_28 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_28 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_28 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_28 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_28 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_28 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_28 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 29
ALTER TABLE link_model_shard_29 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_29 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_29 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_29 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_29 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_29 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_29 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 30
ALTER TABLE link_model_shard_30 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_30 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_30 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_30 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_30 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_30 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_30 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 31
ALTER TABLE link_model_shard_31 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_31 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_31 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_31 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_31 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_31 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_31 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 32
ALTER TABLE link_model_shard_32 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_32 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_32 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_32 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_32 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_32 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_32 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 33
ALTER TABLE link_model_shard_33 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_33 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_33 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_33 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_33 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_33 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_33 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 34
ALTER TABLE link_model_shard_34 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_34 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_34 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_34 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_34 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_34 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_34 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 35
ALTER TABLE link_model_shard_35 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_35 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_35 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_35 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_35 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_35 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_35 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 36
ALTER TABLE link_model_shard_36 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_36 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_36 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_36 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_36 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_36 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_36 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 37
ALTER TABLE link_model_shard_37 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_37 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_37 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_37 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_37 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_37 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_37 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 38
ALTER TABLE link_model_shard_38 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_38 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_38 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_38 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_38 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_38 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_38 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 39
ALTER TABLE link_model_shard_39 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_39 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_39 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_39 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_39 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_39 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_39 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 40
ALTER TABLE link_model_shard_40 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_40 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_40 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_40 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_40 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_40 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_40 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 41
ALTER TABLE link_model_shard_41 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_41 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_41 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_41 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_41 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_41 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_41 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 42
ALTER TABLE link_model_shard_42 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_42 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_42 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_42 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_42 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_42 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_42 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 43
ALTER TABLE link_model_shard_43 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_43 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_43 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_43 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_43 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_43 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_43 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 44
ALTER TABLE link_model_shard_44 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_44 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_44 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_44 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_44 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_44 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_44 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 45
ALTER TABLE link_model_shard_45 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_45 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_45 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_45 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_45 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_45 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_45 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 46
ALTER TABLE link_model_shard_46 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_46 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_46 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_46 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_46 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_46 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_46 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 47
ALTER TABLE link_model_shard_47 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_47 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_47 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_47 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_47 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_47 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_47 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 48
ALTER TABLE link_model_shard_48 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_48 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_48 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_48 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_48 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_48 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_48 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 49
ALTER TABLE link_model_shard_49 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_49 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_49 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_49 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_49 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_49 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_49 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order);

-- Shard 50
ALTER TABLE link_model_shard_50 ADD INDEX idx_id (id);
ALTER TABLE link_model_shard_50 ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE link_model_shard_50 ADD INDEX idx_model (model);
ALTER TABLE link_model_shard_50 ADD INDEX idx_tenant_model (tenant_id, model);
ALTER TABLE link_model_shard_50 ADD INDEX idx_link_id (link_id);
ALTER TABLE link_model_shard_50 ADD INDEX idx_random_order (random_order);
ALTER TABLE link_model_shard_50 ADD INDEX idx_tenant_model_random (tenant_id, model, random_order); 