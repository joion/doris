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

package org.apache.doris.nereids.trees.expressions.visitor;

import org.apache.doris.nereids.analyzer.UnboundAlias;
import org.apache.doris.nereids.analyzer.UnboundFunction;
import org.apache.doris.nereids.analyzer.UnboundSlot;
import org.apache.doris.nereids.analyzer.UnboundStar;
import org.apache.doris.nereids.trees.expressions.Add;
import org.apache.doris.nereids.trees.expressions.Alias;
import org.apache.doris.nereids.trees.expressions.And;
import org.apache.doris.nereids.trees.expressions.Arithmetic;
import org.apache.doris.nereids.trees.expressions.Between;
import org.apache.doris.nereids.trees.expressions.BooleanLiteral;
import org.apache.doris.nereids.trees.expressions.CaseWhen;
import org.apache.doris.nereids.trees.expressions.ComparisonPredicate;
import org.apache.doris.nereids.trees.expressions.CompoundPredicate;
import org.apache.doris.nereids.trees.expressions.Divide;
import org.apache.doris.nereids.trees.expressions.DoubleLiteral;
import org.apache.doris.nereids.trees.expressions.EqualTo;
import org.apache.doris.nereids.trees.expressions.Expression;
import org.apache.doris.nereids.trees.expressions.GreaterThan;
import org.apache.doris.nereids.trees.expressions.GreaterThanEqual;
import org.apache.doris.nereids.trees.expressions.IntegerLiteral;
import org.apache.doris.nereids.trees.expressions.LessThan;
import org.apache.doris.nereids.trees.expressions.LessThanEqual;
import org.apache.doris.nereids.trees.expressions.Like;
import org.apache.doris.nereids.trees.expressions.Literal;
import org.apache.doris.nereids.trees.expressions.Mod;
import org.apache.doris.nereids.trees.expressions.Multiply;
import org.apache.doris.nereids.trees.expressions.NamedExpression;
import org.apache.doris.nereids.trees.expressions.Not;
import org.apache.doris.nereids.trees.expressions.NullLiteral;
import org.apache.doris.nereids.trees.expressions.NullSafeEqual;
import org.apache.doris.nereids.trees.expressions.Or;
import org.apache.doris.nereids.trees.expressions.Regexp;
import org.apache.doris.nereids.trees.expressions.Slot;
import org.apache.doris.nereids.trees.expressions.SlotReference;
import org.apache.doris.nereids.trees.expressions.StringLiteral;
import org.apache.doris.nereids.trees.expressions.StringRegexPredicate;
import org.apache.doris.nereids.trees.expressions.Subtract;
import org.apache.doris.nereids.trees.expressions.WhenClause;
import org.apache.doris.nereids.trees.expressions.functions.AggregateFunction;
import org.apache.doris.nereids.trees.expressions.functions.BoundFunction;

/**
 * Use the visitor to visit expression and forward to unified method(visitExpression).
 */
public abstract class ExpressionVisitor<R, C> {

    public abstract R visit(Expression expr, C context);

    public R visitAlias(Alias alias, C context) {
        return visitNamedExpression(alias, context);
    }

    public R visitComparisonPredicate(ComparisonPredicate cp, C context) {
        return visit(cp, context);
    }

    public R visitEqualTo(EqualTo equalTo, C context) {
        return visitComparisonPredicate(equalTo, context);
    }

    public R visitGreaterThan(GreaterThan greaterThan, C context) {
        return visitComparisonPredicate(greaterThan, context);
    }

    public R visitGreaterThanEqual(GreaterThanEqual greaterThanEqual, C context) {
        return visitComparisonPredicate(greaterThanEqual, context);
    }

    public R visitLessThan(LessThan lessThan, C context) {
        return visitComparisonPredicate(lessThan, context);
    }

    public R visitLessThanEqual(LessThanEqual lessThanEqual, C context) {
        return visitComparisonPredicate(lessThanEqual, context);
    }

    public R visitNullSafeEqual(NullSafeEqual nullSafeEqual, C context) {
        return visitComparisonPredicate(nullSafeEqual, context);
    }

    public R visitNot(Not not, C context) {
        return visit(not, context);
    }

    public R visitSlot(Slot slot, C context) {
        return visitNamedExpression(slot, context);
    }

    public R visitNamedExpression(NamedExpression namedExpression, C context) {
        return visit(namedExpression, context);
    }

    public R visitSlotReference(SlotReference slotReference, C context) {
        return visitSlot(slotReference, context);
    }

    public R visitLiteral(Literal literal, C context) {
        return visit(literal, context);
    }

    public R visitBooleanLiteral(BooleanLiteral booleanLiteral, C context) {
        return visit(booleanLiteral, context);
    }

    public R visitStringLiteral(StringLiteral stringLiteral, C context) {
        return visit(stringLiteral, context);
    }

    public R visitIntegerLiteral(IntegerLiteral integerLiteral, C context) {
        return visit(integerLiteral, context);
    }

    public R visitNullLiteral(NullLiteral nullLiteral, C context) {
        return visit(nullLiteral, context);
    }

    public R visitDoubleLiteral(DoubleLiteral doubleLiteral, C context) {
        return visit(doubleLiteral, context);
    }

    public R visitBetween(Between between, C context) {
        return visit(between, context);
    }

    public R visitCompoundPredicate(CompoundPredicate compoundPredicate, C context) {
        return visit(compoundPredicate, context);
    }

    public R visitAnd(And and, C context) {
        return visitCompoundPredicate(and, context);
    }

    public R visitOr(Or or, C context) {
        return visitCompoundPredicate(or, context);
    }

    public R visitStringRegexPredicate(StringRegexPredicate stringRegexPredicate, C context) {
        return visit(stringRegexPredicate, context);
    }

    public R visitLike(Like like, C context) {
        return visitStringRegexPredicate(like, context);
    }

    public R visitRegexp(Regexp regexp, C context) {
        return visitStringRegexPredicate(regexp, context);
    }

    public R visitBoundFunction(BoundFunction boundFunction, C context) {
        return visit(boundFunction, context);
    }

    public R visitAggregateFunction(AggregateFunction aggregateFunction, C context) {
        return visitBoundFunction(aggregateFunction, context);
    }

    public R visitArithmetic(Arithmetic arithmetic, C context) {
        return visit(arithmetic, context);
    }

    public R visitAdd(Add add, C context) {
        return visitArithmetic(add, context);
    }

    public R visitSubtract(Subtract subtract, C context) {
        return visitArithmetic(subtract, context);
    }

    public R visitMultiply(Multiply multiply, C context) {
        return visitArithmetic(multiply, context);
    }

    public R visitDivide(Divide divide, C context) {
        return visitArithmetic(divide, context);
    }

    public R visitMod(Mod mod, C context) {
        return visitArithmetic(mod, context);
    }

    public R visitWhenClause(WhenClause whenClause, C context) {
        return visit(whenClause, context);
    }

    public R visitCaseWhen(CaseWhen caseWhen, C context) {
        return visit(caseWhen, context);
    }

    /* ********************************************************************************************
     * Unbound expressions
     * ********************************************************************************************/

    public R visitUnboundFunction(UnboundFunction unboundFunction, C context) {
        return visit(unboundFunction, context);
    }

    public R visitUnboundAlias(UnboundAlias unboundAlias, C context) {
        return visitNamedExpression(unboundAlias, context);
    }

    public R visitUnboundSlot(UnboundSlot unboundSlot, C context) {
        return visitSlot(unboundSlot, context);
    }

    public R visitUnboundStar(UnboundStar unboundStar, C context) {
        return visitNamedExpression(unboundStar, context);
    }
}
