// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.nereids.rules.analysis;

import org.apache.doris.catalog.Catalog;
import org.apache.doris.catalog.Database;
import org.apache.doris.catalog.Table;
import org.apache.doris.nereids.rules.Rule;
import org.apache.doris.nereids.rules.RuleType;
import org.apache.doris.nereids.trees.plans.logical.LogicalOlapScan;
import org.apache.doris.qe.ConnectContext;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Rule to bind relations in query plan.
 */
public class BindRelation extends OneAnalysisRuleFactory {
    @Override
    public Rule build() {
        return unboundRelation().thenApply(ctx -> {
            ConnectContext connectContext = ctx.plannerContext.getConnectContext();
            List<String> nameParts = ctx.root.getNameParts();
            switch (nameParts.size()) {
                case 1: {
                    // Use current database name from catalog.
                    String dbName = connectContext.getDatabase();
                    Table table = getTable(dbName, nameParts.get(0), connectContext.getCatalog());
                    // TODO: should generate different Scan sub class according to table's type
                    return new LogicalOlapScan(table, ImmutableList.of(dbName));
                }
                case 2: {
                    // Use database name from table name parts.
                    String dbName = connectContext.getClusterName() + ":" + nameParts.get(0);
                    Table table = getTable(dbName, nameParts.get(1), connectContext.getCatalog());
                    return new LogicalOlapScan(table, ImmutableList.of(dbName));
                }
                default:
                    throw new IllegalStateException("Table name [" + ctx.root.getTableName() + "] is invalid.");
            }
        }).toRule(RuleType.BINDING_RELATION);
    }

    private Table getTable(String dbName, String tableName, Catalog catalog) {
        Database db = catalog.getInternalDataSource().getDb(dbName)
                .orElseThrow(() -> new RuntimeException("Database [" + dbName + "] does not exist."));
        db.readLock();
        try {
            return db.getTable(tableName).orElseThrow(() -> new RuntimeException(
                    "Table [" + tableName + "] does not exist in database [" + dbName + "]."));
        } finally {
            db.readUnlock();
        }
    }
}
