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

package org.apache.doris.nereids.trees.plans.physical;

import org.apache.doris.nereids.memo.GroupExpression;
import org.apache.doris.nereids.properties.LogicalProperties;
import org.apache.doris.nereids.trees.expressions.Expression;
import org.apache.doris.nereids.trees.plans.Plan;
import org.apache.doris.nereids.trees.plans.PlanType;
import org.apache.doris.nereids.trees.plans.visitor.PlanVisitor;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Physical filter plan.
 */
public class PhysicalFilter<CHILD_TYPE extends Plan> extends PhysicalUnary<CHILD_TYPE> {

    private final Expression predicates;

    public PhysicalFilter(Expression predicates, LogicalProperties logicalProperties, CHILD_TYPE child) {
        this(predicates, Optional.empty(), logicalProperties, child);
    }

    public PhysicalFilter(Expression predicates, Optional<GroupExpression> groupExpression,
                          LogicalProperties logicalProperties, CHILD_TYPE child) {
        super(PlanType.PHYSICAL_FILTER, groupExpression, logicalProperties, child);
        this.predicates = Objects.requireNonNull(predicates, "predicates can not be null");
    }

    public Expression getPredicates() {
        return predicates;
    }

    @Override
    public String toString() {
        return "PhysicalFilter (" + predicates + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PhysicalFilter that = (PhysicalFilter) o;
        return predicates.equals(that.predicates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicates);
    }

    @Override
    public List<Expression> getExpressions() {
        return ImmutableList.of(predicates);
    }

    @Override
    public <R, C> R accept(PlanVisitor<R, C> visitor, C context) {
        return visitor.visitPhysicalFilter((PhysicalFilter<Plan>) this, context);
    }

    @Override
    public PhysicalUnary<Plan> withChildren(List<Plan> children) {
        Preconditions.checkArgument(children.size() == 1);
        return new PhysicalFilter<>(predicates, logicalProperties, children.get(0));
    }

    @Override
    public Plan withGroupExpression(Optional<GroupExpression> groupExpression) {
        return new PhysicalFilter<>(predicates, groupExpression, logicalProperties, child());
    }

    @Override
    public Plan withLogicalProperties(Optional<LogicalProperties> logicalProperties) {
        return new PhysicalFilter<>(predicates, Optional.empty(), logicalProperties.get(), child());
    }
}
