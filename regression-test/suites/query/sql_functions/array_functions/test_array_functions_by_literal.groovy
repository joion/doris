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

suite("test_array_functions_by_literal", "all") {
    sql "set enable_vectorized_engine = true"
    sql "set enable_array_type = true"

    // array_contains function
    qt_sql "select array_contains([1,2,3], 1)"
    qt_sql "select array_contains([1,2,3], 4)"
    qt_sql "select array_contains([1,2,3,NULL], 1)"
    qt_sql "select array_contains([1,2,3,NULL], NULL)"
    qt_sql "select array_contains([], 1)"
    qt_sql "select array_contains([], NULL)"
    qt_sql "select array_contains(NULL, 1)"
    qt_sql "select array_contains(NULL, NULL)"

    // array_position function
    qt_sql "select array_position([1,2,3], 1)"
    qt_sql "select array_position([1,2,3], 3)"
    qt_sql "select array_position([1,2,3], 4)"
    qt_sql "select array_position([NULL,2,3], 2)"
    qt_sql "select array_position([NULL,2,3], NULL)"
    qt_sql "select array_position([], 1)"
    qt_sql "select array_position([], NULL)"
    qt_sql "select array_position(NULL, 1)"
    qt_sql "select array_position(NULL, NULL)"

    // element_at function
    qt_sql "select element_at([1,2,3], 1)"
    qt_sql "select element_at([1,2,3], 3)"
    qt_sql "select element_at([1,2,3], 4)"
    qt_sql "select element_at([1,2,3], -1)"
    qt_sql "select element_at([1,2,3], NULL)"
    qt_sql "select element_at([1,2,NULL], 3)"
    qt_sql "select element_at([1,2,NULL], 2)"
    qt_sql "select element_at([], -1)"

    // array subscript function
    qt_sql "select [1,2,3][1]"
    qt_sql "select [1,2,3][3]"
    qt_sql "select [1,2,3][4]"
    qt_sql "select [1,2,3][-1]"
    qt_sql "select [1,2,3][NULL]"
    qt_sql "select [1,2,NULL][3]"
    qt_sql "select [1,2,NULL][2]"
    qt_sql "select [][-1]"

    // array_aggregation function
    qt_sql "select array_avg([1,2,3])"
    qt_sql "select array_sum([1,2,3])"
    qt_sql "select array_min([1,2,3])"
    qt_sql "select array_max([1,2,3])"
}
