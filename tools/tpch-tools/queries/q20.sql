-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

-- Modified

select /*+SET_VAR(exec_mem_limit=8589934592, parallel_fragment_exec_instance_num=8, enable_vectorized_engine=true, batch_size=4096, disable_join_reorder=true, enable_cost_based_join_reorder=true, enable_projection=true) */
s_name, s_address from
supplier left semi join
(
    select * from
    (
        select l_partkey,l_suppkey, 0.5 * sum(l_quantity) as l_q
        from lineitem
        where l_shipdate >= date '1994-01-01'
            and l_shipdate < date '1994-01-01' + interval '1' year
        group by l_partkey,l_suppkey
    ) t2 join
    (
        select ps_partkey, ps_suppkey, ps_availqty
        from partsupp left semi join part
        on ps_partkey = p_partkey and p_name like 'forest%'
    ) t1
    on t2.l_partkey = t1.ps_partkey and t2.l_suppkey = t1.ps_suppkey
    and t1.ps_availqty > t2.l_q
) t3
on s_suppkey = t3.ps_suppkey
join nation
where s_nationkey = n_nationkey
    and n_name = 'CANADA'
order by s_name;
