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

package org.apache.doris.nereids.jobs;

import org.apache.doris.catalog.Column;
import org.apache.doris.catalog.Table;
import org.apache.doris.catalog.TableIf.TableType;
import org.apache.doris.catalog.Type;
import org.apache.doris.nereids.PlannerContext;
import org.apache.doris.nereids.analyzer.UnboundRelation;
import org.apache.doris.nereids.memo.Group;
import org.apache.doris.nereids.memo.GroupExpression;
import org.apache.doris.nereids.memo.Memo;
import org.apache.doris.nereids.properties.LogicalProperties;
import org.apache.doris.nereids.rules.Rule;
import org.apache.doris.nereids.rules.RuleType;
import org.apache.doris.nereids.rules.rewrite.OneRewriteRuleFactory;
import org.apache.doris.nereids.trees.expressions.Slot;
import org.apache.doris.nereids.trees.expressions.SlotReference;
import org.apache.doris.nereids.trees.plans.Plan;
import org.apache.doris.nereids.trees.plans.PlanType;
import org.apache.doris.nereids.trees.plans.logical.LogicalProject;
import org.apache.doris.nereids.trees.plans.logical.LogicalRelation;
import org.apache.doris.nereids.types.IntegerType;
import org.apache.doris.nereids.types.StringType;
import org.apache.doris.qe.ConnectContext;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

public class RewriteTopDownJobTest {
    public static class FakeRule extends OneRewriteRuleFactory {
        @Override
        public Rule build() {
            return unboundRelation().then(unboundRelation -> {
                        Table olapTable = new Table(0, "test", TableType.OLAP, ImmutableList.of(
                                new Column("id", Type.INT),
                                new Column("name", Type.STRING)
                        ));
                        return new LogicalBoundRelation(olapTable, Lists.newArrayList("test"));
                    }
                ).toRule(RuleType.BINDING_RELATION);
        }
    }

    @Test
    public void testSimplestScene() {
        Plan leaf = new UnboundRelation(Lists.newArrayList("test"));
        LogicalProject project = new LogicalProject(ImmutableList.of(
                new SlotReference("name", StringType.INSTANCE, true, ImmutableList.of("test"))),
                leaf
        );
        PlannerContext plannerContext = new Memo(project)
                .newPlannerContext(new ConnectContext())
                .setDefaultJobContext();

        List<Rule> fakeRules = Lists.newArrayList(new FakeRule().build());
        plannerContext.topDownRewrite(fakeRules);

        Group rootGroup = plannerContext.getMemo().getRoot();
        Assertions.assertEquals(1, rootGroup.getLogicalExpressions().size());
        GroupExpression rootGroupExpression = rootGroup.getLogicalExpression();
        List<Slot> output = rootGroup.getLogicalProperties().getOutput();
        Assertions.assertEquals(output.size(), 1);
        Assertions.assertEquals(output.get(0).getName(), "name");
        Assertions.assertEquals(output.get(0).getDataType(), StringType.INSTANCE);
        Assertions.assertEquals(1, rootGroupExpression.children().size());
        Assertions.assertEquals(PlanType.LOGICAL_PROJECT, rootGroupExpression.getPlan().getType());

        Group leafGroup = rootGroupExpression.child(0);
        output = leafGroup.getLogicalProperties().getOutput();
        Assertions.assertEquals(output.size(), 2);
        Assertions.assertEquals(output.get(0).getName(), "id");
        Assertions.assertEquals(output.get(0).getDataType(), IntegerType.INSTANCE);
        Assertions.assertEquals(output.get(1).getName(), "name");
        Assertions.assertEquals(output.get(1).getDataType(), StringType.INSTANCE);
        Assertions.assertEquals(1, leafGroup.getLogicalExpressions().size());
        GroupExpression leafGroupExpression = leafGroup.getLogicalExpression();
        Assertions.assertEquals(PlanType.LOGICAL_BOUND_RELATION, leafGroupExpression.getPlan().getType());
    }

    private static class LogicalBoundRelation extends LogicalRelation {

        public LogicalBoundRelation(Table table, List<String> qualifier) {
            super(PlanType.LOGICAL_BOUND_RELATION, table, qualifier);
        }

        public LogicalBoundRelation(Table table, List<String> qualifier, Optional<GroupExpression> groupExpression,
                Optional<LogicalProperties> logicalProperties) {
            super(PlanType.LOGICAL_BOUND_RELATION, table, qualifier, groupExpression, logicalProperties);
        }

        @Override
        public Plan withGroupExpression(Optional<GroupExpression> groupExpression) {
            return new LogicalBoundRelation(table, qualifier, groupExpression, Optional.of(logicalProperties));
        }

        @Override
        public Plan withLogicalProperties(Optional<LogicalProperties> logicalProperties) {
            return new LogicalBoundRelation(table, qualifier, Optional.empty(), logicalProperties);
        }
    }
}
