/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.shardingproxy.backend.schema.impl;

import lombok.Getter;
import org.apache.shardingsphere.api.config.sharding.ShardingRuleConfiguration;
import org.apache.shardingsphere.underlying.common.metadata.table.TableMetaDataInitializerEntry;
import org.apache.shardingsphere.core.rule.ShardingRule;
import org.apache.shardingsphere.sharding.execute.metadata.loader.ShardingTableMetaDataLoader;
import org.apache.shardingsphere.shardingproxy.backend.executor.BackendExecutorContext;
import org.apache.shardingsphere.shardingproxy.backend.schema.LogicSchema;
import org.apache.shardingsphere.shardingproxy.backend.schema.LogicSchemas;
import org.apache.shardingsphere.shardingproxy.backend.schema.ProxyConnectionManager;
import org.apache.shardingsphere.shardingproxy.config.yaml.YamlDataSourceParameter;
import org.apache.shardingsphere.shardingproxy.context.ShardingProxyContext;
import org.apache.shardingsphere.underlying.common.constant.properties.PropertiesConstant;
import org.apache.shardingsphere.underlying.common.constant.properties.ShardingSphereProperties;
import org.apache.shardingsphere.underlying.common.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.underlying.common.metadata.datasource.DataSourceMetas;
import org.apache.shardingsphere.sql.parser.binder.metadata.table.TableMetas;
import org.apache.shardingsphere.underlying.common.metadata.table.TableMetaDataInitializer;
import org.apache.shardingsphere.underlying.common.rule.BaseRule;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Transparent schema.
 */
@Getter
public final class TransparentSchema extends LogicSchema {
    
    private final ShardingSphereMetaData metaData;
    
    private final ShardingRule shardingRule;
    
    public TransparentSchema(final String name, final Map<String, YamlDataSourceParameter> dataSources) throws SQLException {
        super(name, dataSources);
        // TODO we should remove it after none-sharding parsingEngine completed.
        shardingRule = new ShardingRule(new ShardingRuleConfiguration(), getDataSources().keySet());
        metaData = createMetaData();
    }
    
    private ShardingSphereMetaData createMetaData() throws SQLException {
        DataSourceMetas dataSourceMetas = new DataSourceMetas(LogicSchemas.getInstance().getDatabaseType(), getDatabaseAccessConfigurationMap());
        TableMetas tableMetas = createTableMetaDataInitializerEntry(dataSourceMetas).initAll();
        return new ShardingSphereMetaData(dataSourceMetas, tableMetas);
    }
    
    private TableMetaDataInitializerEntry createTableMetaDataInitializerEntry(final DataSourceMetas dataSourceMetas) {
        ShardingSphereProperties properties = ShardingProxyContext.getInstance().getProperties();
        Map<BaseRule, TableMetaDataInitializer> tableMetaDataInitializes = new HashMap<>(1, 1);
        tableMetaDataInitializes.put(shardingRule,
                new ShardingTableMetaDataLoader(dataSourceMetas, BackendExecutorContext.getInstance().getExecutorEngine(), new ProxyConnectionManager(getBackendDataSource()),
                        properties.<Integer>getValue(PropertiesConstant.MAX_CONNECTIONS_SIZE_PER_QUERY), properties.<Boolean>getValue(PropertiesConstant.CHECK_TABLE_METADATA_ENABLED)));
        return new TableMetaDataInitializerEntry(tableMetaDataInitializes);
    }
}
