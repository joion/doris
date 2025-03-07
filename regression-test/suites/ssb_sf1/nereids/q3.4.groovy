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

suite("ssb_sf1_q3_4_nereids") {
    String realDb = context.config.getDbByLastGroup(context.group)
    // get parent directory's group
    realDb = realDb.substring(0, realDb.lastIndexOf("_"))

    sql "use ${realDb}"

    sql 'set enable_nereids_planner=true'
    // nereids need vectorized
    sql 'set enable_vectorized_engine=true'

    sql 'set exec_mem_limit=2147483648*2'

    test {
        sql(new File(context.file.parentFile, "../sql/q3.4.sql").text)

        resultFile(file = "../sql/q3.4.out", tag = "q3.4")
    }
}
