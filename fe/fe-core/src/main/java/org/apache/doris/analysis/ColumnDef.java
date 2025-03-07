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
// This file is copied from
// https://github.com/apache/impala/blob/branch-2.9.0/fe/src/main/java/org/apache/impala/ColumnDef.java
// and modified by Doris

package org.apache.doris.analysis;

import org.apache.doris.catalog.AggregateType;
import org.apache.doris.catalog.Column;
import org.apache.doris.catalog.PrimitiveType;
import org.apache.doris.catalog.ScalarType;
import org.apache.doris.catalog.Type;
import org.apache.doris.common.AnalysisException;
import org.apache.doris.common.Config;
import org.apache.doris.common.FeNameFormat;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Column definition which is generated by SQL syntax parser
// Syntax:
//      name type [key] [agg_type] [NULL | NOT NULL] [DEFAULT default_value] [comment]
// Example:
//      id bigint key NOT NULL DEFAULT "-1" "user id"
//      pv bigint sum NULL DEFAULT "-1" "page visit"
public class ColumnDef {
    private static final Logger LOG = LogManager.getLogger(ColumnDef.class);

    /*
     * User can set default value for a column
     * eg:
     *     k1 INT NOT NULL DEFAULT "10"
     *     k1 INT NULL
     *     k1 INT NULL DEFAULT NULL
     *
     * ColumnnDef will be transformed to Column in Analysis phase, and in Column, default value is a String.
     * No matter does the user set the default value as NULL explicitly, or not set default value, the default value
     * in Column will be "null", so that Doris can not distinguish between "not set" and "set as null".
     *
     * But this is OK because Column has another attribute "isAllowNull".
     * If the column is not allowed to be null, and user does not set the default value,
     * even if default value saved in Column is null, the "null" value can not be loaded into this column,
     * so data correctness can be guaranteed.
     */
    public static class DefaultValue {
        public boolean isSet;
        public String value;
        // used for column which defaultValue is an expression.
        public DefaultValueExprDef defaultValueExprDef;

        public DefaultValue(boolean isSet, String value) {
            this.isSet = isSet;
            this.value = value;
            this.defaultValueExprDef = null;
        }

        /**
         * used for column which defaultValue is an expression.
         * @param isSet is Set DefaultValue
         * @param value default value
         * @param exprName default value expression
         */
        public DefaultValue(boolean isSet, String value, String exprName) {
            this.isSet = isSet;
            this.value = value;
            this.defaultValueExprDef = new DefaultValueExprDef(exprName);
        }

        // default "CURRENT_TIMESTAMP", only for DATETIME type
        public static String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";
        public static String NOW = "now";
        public static DefaultValue CURRENT_TIMESTAMP_DEFAULT_VALUE = new DefaultValue(true, CURRENT_TIMESTAMP, NOW);
        // no default value
        public static DefaultValue NOT_SET = new DefaultValue(false, null);
        // default null
        public static DefaultValue NULL_DEFAULT_VALUE = new DefaultValue(true, null);
        public static String ZERO = new String(new byte[] {0});
        // default "value", "0" means empty hll
        public static DefaultValue HLL_EMPTY_DEFAULT_VALUE = new DefaultValue(true, ZERO);
        // default "value", "0" means empty bitmap
        public static DefaultValue BITMAP_EMPTY_DEFAULT_VALUE = new DefaultValue(true, ZERO);
    }

    // parameter initialized in constructor
    private String name;
    private TypeDef typeDef;
    private AggregateType aggregateType;
    private boolean isKey;
    private boolean isAllowNull;
    private DefaultValue defaultValue;
    private String comment;
    private boolean visible;

    public ColumnDef(String name, TypeDef typeDef) {
        this.name = name;
        this.typeDef = typeDef;
        this.comment = "";
        this.defaultValue = DefaultValue.NOT_SET;
    }

    public ColumnDef(String name, TypeDef typeDef, boolean isKey, AggregateType aggregateType,
                     boolean isAllowNull, DefaultValue defaultValue, String comment) {
        this(name, typeDef, isKey, aggregateType, isAllowNull, defaultValue, comment, true);
    }

    public ColumnDef(String name, TypeDef typeDef, boolean isKey, AggregateType aggregateType,
            boolean isAllowNull, DefaultValue defaultValue, String comment, boolean visible) {
        this.name = name;
        this.typeDef = typeDef;
        this.isKey = isKey;
        this.aggregateType = aggregateType;
        this.isAllowNull = isAllowNull;
        this.defaultValue = defaultValue;
        this.comment = comment;
        this.visible = visible;
    }

    public static ColumnDef newDeleteSignColumnDef() {
        return new ColumnDef(Column.DELETE_SIGN, TypeDef.create(PrimitiveType.TINYINT), false, null, false,
                new ColumnDef.DefaultValue(true, "0"), "doris delete flag hidden column", false);
    }

    public static ColumnDef newDeleteSignColumnDef(AggregateType aggregateType) {
        return new ColumnDef(Column.DELETE_SIGN, TypeDef.create(PrimitiveType.TINYINT), false, aggregateType, false,
                new ColumnDef.DefaultValue(true, "0"), "doris delete flag hidden column", false);
    }

    public static ColumnDef newSequenceColumnDef(Type type) {
        return new ColumnDef(Column.SEQUENCE_COL, new TypeDef(type), false, null, true, DefaultValue.NULL_DEFAULT_VALUE,
                "sequence column hidden column", false);
    }

    public static ColumnDef newSequenceColumnDef(Type type, AggregateType aggregateType) {
        return new ColumnDef(Column.SEQUENCE_COL, new TypeDef(type), false,
                aggregateType, true, DefaultValue.NULL_DEFAULT_VALUE,
                "sequence column hidden column", false);
    }

    public boolean isAllowNull() {
        return isAllowNull;
    }

    public String getDefaultValue() {
        return defaultValue.value;
    }

    public String getName() {
        return name;
    }

    public AggregateType getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(AggregateType aggregateType) {
        this.aggregateType = aggregateType;
    }

    public boolean isKey() {
        return isKey;
    }

    public void setIsKey(boolean isKey) {
        this.isKey = isKey;
    }

    public TypeDef getTypeDef() {
        return typeDef;
    }

    public Type getType() {
        return typeDef.getType();
    }

    public String getComment() {
        return comment;
    }

    public boolean isVisible() {
        return visible;
    }

    public void analyze(boolean isOlap) throws AnalysisException {
        if (name == null || typeDef == null) {
            throw new AnalysisException("No column name or column type in column definition.");
        }
        FeNameFormat.checkColumnName(name);

        // When string type length is not assigned, it need to be assigned to 1.
        if (typeDef.getType().isScalarType()) {
            final ScalarType targetType = (ScalarType) typeDef.getType();
            if (targetType.getPrimitiveType().isStringType()
                    && !targetType.isAssignedStrLenInColDefinition()) {
                targetType.setLength(1);
            }
        }

        typeDef.analyze(null);

        Type type = typeDef.getType();

        if (!Config.enable_quantile_state_type && type.isQuantileStateType()) {
            throw new AnalysisException("quantile_state is disabled"
                    + "Set config 'enable_quantile_state_type' = 'true' to enable this column type.");
        }

        // disable Bitmap Hll type in keys, values without aggregate function.
        if (type.isBitmapType() || type.isHllType()) {
            if (isKey) {
                throw new AnalysisException("Key column can not set bitmap or hll type:" + name);
            }
            if (aggregateType == null) {
                throw new AnalysisException("Bitmap and hll type have to use aggregate function" + name);
            }
        }

        // A column is a key column if and only if isKey is true.
        // aggregateType == null does not mean that this is a key column,
        // because when creating a UNIQUE KEY table, aggregateType is implicit.
        if (aggregateType != null) {
            if (isKey) {
                throw new AnalysisException("Key column can not set aggregation type: " + name);
            }

            // check if aggregate type is valid
            if (!aggregateType.checkCompatibility(type.getPrimitiveType())) {
                throw new AnalysisException(String.format("Aggregate type %s is not compatible with primitive type %s",
                        toString(), type.toSql()));
            }
        }

        if (type.getPrimitiveType() == PrimitiveType.FLOAT || type.getPrimitiveType() == PrimitiveType.DOUBLE) {
            if (isOlap && isKey) {
                throw new AnalysisException("Float or double can not used as a key, use decimal instead.");
            }
        }

        if (type.getPrimitiveType() == PrimitiveType.HLL) {
            if (defaultValue.isSet) {
                throw new AnalysisException("Hll type column can not set default value");
            }
            defaultValue = DefaultValue.HLL_EMPTY_DEFAULT_VALUE;
        }

        if (type.getPrimitiveType() == PrimitiveType.BITMAP) {
            if (defaultValue.isSet && defaultValue != DefaultValue.NULL_DEFAULT_VALUE) {
                throw new AnalysisException("Bitmap type column can not set default value");
            }
            defaultValue = DefaultValue.BITMAP_EMPTY_DEFAULT_VALUE;
        }

        if (type.getPrimitiveType() == PrimitiveType.ARRAY) {
            if (defaultValue.isSet && defaultValue != DefaultValue.NULL_DEFAULT_VALUE) {
                throw new AnalysisException("Array type column default value only support null");
            }
        }
        if (isKey() && type.getPrimitiveType() == PrimitiveType.STRING) {
            throw new AnalysisException("String Type should not be used in key column[" + getName()
                    + "].");
        }
        if (type.getPrimitiveType() == PrimitiveType.MAP) {
            if (defaultValue.isSet && defaultValue != DefaultValue.NULL_DEFAULT_VALUE) {
                throw new AnalysisException("Map type column default value just support null");
            }
        }

        if (type.getPrimitiveType() == PrimitiveType.STRUCT) {
            if (defaultValue.isSet && defaultValue != DefaultValue.NULL_DEFAULT_VALUE) {
                throw new AnalysisException("Struct type column default value just support null");
            }
        }

        // If aggregate type is REPLACE_IF_NOT_NULL, we set it nullable.
        // If default value is not set, we set it NULL
        if (aggregateType == AggregateType.REPLACE_IF_NOT_NULL) {
            isAllowNull = true;
            if (!defaultValue.isSet) {
                defaultValue = DefaultValue.NULL_DEFAULT_VALUE;
            }
        }

        if (!isAllowNull && defaultValue == DefaultValue.NULL_DEFAULT_VALUE) {
            throw new AnalysisException("Can not set null default value to non nullable column: " + name);
        }

        if (defaultValue.isSet && defaultValue.value != null) {
            validateDefaultValue(type, defaultValue.value, defaultValue.defaultValueExprDef);
        }
    }

    @SuppressWarnings("checkstyle:Indentation")
    public static void validateDefaultValue(Type type, String defaultValue, DefaultValueExprDef defaultValueExprDef)
            throws AnalysisException {
        Preconditions.checkNotNull(defaultValue);
        Preconditions.checkArgument(type.isScalarType());
        ScalarType scalarType = (ScalarType) type;

        // check if default value is valid.
        // if not, some literal constructor will throw AnalysisException
        PrimitiveType primitiveType = scalarType.getPrimitiveType();
        switch (primitiveType) {
            case TINYINT:
            case SMALLINT:
            case INT:
            case BIGINT:
                new IntLiteral(defaultValue, type);
                break;
            case LARGEINT:
                new LargeIntLiteral(defaultValue);
                break;
            case FLOAT:
                FloatLiteral floatLiteral = new FloatLiteral(defaultValue);
                if (floatLiteral.getType().equals(Type.DOUBLE)) {
                    throw new AnalysisException("Default value will loose precision: " + defaultValue);
                }
                break;
            case DOUBLE:
                new FloatLiteral(defaultValue);
                break;
            case DECIMALV2:
            case DECIMAL32:
            case DECIMAL64:
            case DECIMAL128:
                DecimalLiteral decimalLiteral = new DecimalLiteral(defaultValue);
                decimalLiteral.checkPrecisionAndScale(scalarType.getScalarPrecision(), scalarType.getScalarScale());
                break;
            case DATE:
            case DATEV2:
                new DateLiteral(defaultValue, DateLiteral.getDefaultDateType(type));
                break;
            case DATETIME:
            case DATETIMEV2:
                if (defaultValueExprDef == null) {
                    new DateLiteral(defaultValue, DateLiteral.getDefaultDateType(type));
                } else {
                    if (defaultValueExprDef.getExprName().equals(DefaultValue.NOW)) {
                        break;
                    } else {
                        throw new AnalysisException("date literal [" + defaultValue + "] is invalid");
                    }
                }
                break;
            case CHAR:
            case VARCHAR:
            case HLL:
            case STRING:
                if (defaultValue.length() > scalarType.getLength()) {
                    throw new AnalysisException("Default value is too long: " + defaultValue);
                }
                break;
            case BITMAP:
            case ARRAY:
            case MAP:
            case STRUCT:
                break;
            case BOOLEAN:
                new BoolLiteral(defaultValue);
                break;
            default:
                throw new AnalysisException("Unsupported type: " + type);
        }
    }

    public String toSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("`").append(name).append("` ");
        sb.append(typeDef.toSql()).append(" ");

        if (aggregateType != null) {
            sb.append(aggregateType.name()).append(" ");
        }

        if (!isAllowNull) {
            sb.append("NOT NULL ");
        } else {
            // should append NULL to make result can be executed right.
            sb.append("NULL ");
        }

        if (defaultValue.isSet) {
            sb.append("DEFAULT \"").append(defaultValue.value).append("\" ");
        }
        sb.append("COMMENT \"").append(comment).append("\"");

        return sb.toString();
    }

    public Column toColumn() {
        return new Column(name, typeDef.getType(), isKey, aggregateType, isAllowNull, defaultValue.value, comment,
                visible, defaultValue.defaultValueExprDef, Column.COLUMN_UNIQUE_ID_INIT_VALUE);
    }

    @Override
    public String toString() {
        return toSql();
    }
}
