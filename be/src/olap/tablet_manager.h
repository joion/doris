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

#pragma once

#include <atomic>
#include <list>
#include <map>
#include <mutex>
#include <set>
#include <shared_mutex>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#include "common/status.h"
#include "gen_cpp/AgentService_types.h"
#include "gen_cpp/BackendService_types.h"
#include "gen_cpp/MasterService_types.h"
#include "olap/olap_define.h"
#include "olap/olap_meta.h"
#include "olap/options.h"
#include "olap/tablet.h"

namespace doris {

class Tablet;
class DataDir;

// TabletManager provides get, add, delete tablet method for storage engine
// NOTE: If you want to add a method that needs to hold meta-lock before you can call it,
// please uniformly name the method in "xxx_unlocked()" mode
class TabletManager {
public:
    TabletManager(int32_t tablet_map_lock_shard_size);
    ~TabletManager();

    bool check_tablet_id_exist(TTabletId tablet_id);

    // The param stores holds all candidate data_dirs for this tablet.
    // NOTE: If the request is from a schema-changing tablet, The directory selected by the
    // new tablet should be the same as the directory of origin tablet. Because the
    // linked-schema-change type requires Linux hard-link, which does not support cross disk.
    // TODO(lingbin): Other schema-change type do not need to be on the same disk. Because
    // there may be insufficient space on the current disk, which will lead the schema-change
    // task to be fail, even if there is enough space on other disks
    Status create_tablet(const TCreateTabletReq& request, std::vector<DataDir*> stores);

    // Drop a tablet by description
    // If set keep_files == true, files will NOT be deleted when deconstruction.
    // Return OLAP_SUCCESS, if run ok
    //        OLAP_ERR_TABLE_DELETE_NOEXIST_ERROR, if tablet not exist
    //        Status::OLAPInternalError(OLAP_ERR_NOT_INITED), if not inited
    Status drop_tablet(TTabletId tablet_id, TReplicaId replica_id, bool keep_files = false);

    Status drop_tablets_on_error_root_path(const std::vector<TabletInfo>& tablet_info_vec);

    TabletSharedPtr find_best_tablet_to_compaction(
            CompactionType compaction_type, DataDir* data_dir,
            const std::unordered_set<TTabletId>& tablet_submitted_compaction, uint32_t* score,
            std::shared_ptr<CumulativeCompactionPolicy> cumulative_compaction_policy);

    TabletSharedPtr get_tablet(TTabletId tablet_id, bool include_deleted = false,
                               std::string* err = nullptr);

    TabletSharedPtr get_tablet(TTabletId tablet_id, TabletUid tablet_uid,
                               bool include_deleted = false, std::string* err = nullptr);

    // Extract tablet_id and schema_hash from given path.
    //
    // The normal path pattern is like "/data/{shard_id}/{tablet_id}/{schema_hash}/xxx.data".
    // Besides that, this also support empty tablet path, which path looks like
    // "/data/{shard_id}/{tablet_id}"
    //
    // Return true when the path matches the path pattern, and tablet_id and schema_hash is
    // saved in input params. When input path is an empty tablet directory, schema_hash will
    // be set to 0. Return false if the path don't match valid pattern.
    static bool get_tablet_id_and_schema_hash_from_path(const std::string& path,
                                                        TTabletId* tablet_id,
                                                        TSchemaHash* schema_hash);

    static bool get_rowset_id_from_path(const std::string& path, RowsetId* rowset_id);

    void get_tablet_stat(TTabletStatResult* result);

    // parse tablet header msg to generate tablet object
    // - restore: whether the request is from restore tablet action,
    //   where we should change tablet status from shutdown back to running
    Status load_tablet_from_meta(DataDir* data_dir, TTabletId tablet_id, TSchemaHash schema_hash,
                                 const std::string& header, bool update_meta, bool force = false,
                                 bool restore = false, bool check_path = true);

    Status load_tablet_from_dir(DataDir* data_dir, TTabletId tablet_id, SchemaHash schema_hash,
                                const std::string& schema_hash_path, bool force = false,
                                bool restore = false);

    // 获取所有tables的名字
    //
    // Return OLAP_SUCCESS, if run ok
    //        Status::OLAPInternalError(OLAP_ERR_INPUT_PARAMETER_ERROR), if tables is null
    Status report_tablet_info(TTabletInfo* tablet_info);

    Status build_all_report_tablets_info(std::map<TTabletId, TTablet>* tablets_info);

    Status start_trash_sweep();

    void try_delete_unused_tablet_path(DataDir* data_dir, TTabletId tablet_id,
                                       SchemaHash schema_hash, const std::string& schema_hash_path);

    void update_root_path_info(std::map<std::string, DataDirInfo>* path_map,
                               size_t* tablet_counter);

    void get_partition_related_tablets(int64_t partition_id, std::set<TabletInfo>* tablet_infos);

    void do_tablet_meta_checkpoint(DataDir* data_dir);

    void obtain_specific_quantity_tablets(std::vector<TabletInfo>& tablets_info, int64_t num);

    void register_clone_tablet(int64_t tablet_id);
    void unregister_clone_tablet(int64_t tablet_id);

    void get_tablets_distribution_on_different_disks(
            std::map<int64_t, std::map<DataDir*, int64_t>>& tablets_num_on_disk,
            std::map<int64_t, std::map<DataDir*, std::vector<TabletSize>>>& tablets_info_on_disk);
    void get_cooldown_tablets(std::vector<TabletSharedPtr>* tables);

    void get_all_tablets_storage_format(TCheckStorageFormatResult* result);

private:
    // Add a tablet pointer to StorageEngine
    // If force, drop the existing tablet add this new one
    //
    // Return OLAP_SUCCESS, if run ok
    //        OLAP_ERR_TABLE_INSERT_DUPLICATION_ERROR, if find duplication
    //        Status::OLAPInternalError(OLAP_ERR_NOT_INITED), if not inited
    Status _add_tablet_unlocked(TTabletId tablet_id, const TabletSharedPtr& tablet,
                                bool update_meta, bool force);

    Status _add_tablet_to_map_unlocked(TTabletId tablet_id, const TabletSharedPtr& tablet,
                                       bool update_meta, bool keep_files, bool drop_old);

    bool _check_tablet_id_exist_unlocked(TTabletId tablet_id);

    Status _drop_tablet_unlocked(TTabletId tablet_id, TReplicaId replica_id, bool keep_files);

    TabletSharedPtr _get_tablet_unlocked(TTabletId tablet_id);
    TabletSharedPtr _get_tablet_unlocked(TTabletId tablet_id, bool include_deleted,
                                         std::string* err);

    TabletSharedPtr _internal_create_tablet_unlocked(const TCreateTabletReq& request,
                                                     const bool is_schema_change,
                                                     const Tablet* base_tablet,
                                                     const std::vector<DataDir*>& data_dirs);
    TabletSharedPtr _create_tablet_meta_and_dir_unlocked(const TCreateTabletReq& request,
                                                         const bool is_schema_change,
                                                         const Tablet* base_tablet,
                                                         const std::vector<DataDir*>& data_dirs);
    Status _create_tablet_meta_unlocked(const TCreateTabletReq& request, DataDir* store,
                                        const bool is_schema_change_tablet,
                                        const Tablet* base_tablet,
                                        TabletMetaSharedPtr* tablet_meta);

    void _add_tablet_to_partition(const TabletSharedPtr& tablet);

    void _remove_tablet_from_partition(const TabletSharedPtr& tablet);

    std::shared_mutex& _get_tablets_shard_lock(TTabletId tabletId);

private:
    DISALLOW_COPY_AND_ASSIGN(TabletManager);

    using tablet_map_t = std::unordered_map<int64_t, TabletSharedPtr>;

    struct tablets_shard {
        tablets_shard() = default;
        tablets_shard(tablets_shard&& shard) {
            tablet_map = std::move(shard.tablet_map);
            tablets_under_clone = std::move(shard.tablets_under_clone);
        }
        // protect tablet_map, tablets_under_clone and tablets_under_restore
        mutable std::shared_mutex lock;
        tablet_map_t tablet_map;
        std::set<int64_t> tablets_under_clone;
    };

    // trace the memory use by meta of tablet
    std::unique_ptr<MemTracker> _mem_tracker;

    const int32_t _tablets_shards_size;
    const int32_t _tablets_shards_mask;
    std::vector<tablets_shard> _tablets_shards;

    // Protect _partition_tablet_map, should not be obtained before _tablet_map_lock to avoid dead lock
    std::shared_mutex _partition_tablet_map_lock;
    // Protect _shutdown_tablets, should not be obtained before _tablet_map_lock to avoid dead lock
    std::shared_mutex _shutdown_tablets_lock;
    // partition_id => tablet_info
    std::map<int64_t, std::set<TabletInfo>> _partition_tablet_map;
    std::vector<TabletSharedPtr> _shutdown_tablets;

    std::mutex _tablet_stat_cache_mutex;
    std::shared_ptr<std::vector<TTabletStat>> _tablet_stat_list_cache =
            std::make_shared<std::vector<TTabletStat>>();

    tablet_map_t& _get_tablet_map(TTabletId tablet_id);

    tablets_shard& _get_tablets_shard(TTabletId tabletId);
};

} // namespace doris
