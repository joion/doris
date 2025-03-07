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

#include "olap/primary_key_index.h"

#include <gtest/gtest.h>

#include "io/fs/file_writer.h"
#include "io/fs/local_file_system.h"
#include "olap/row_cursor.h"
#include "olap/tablet_schema_helper.h"
#include "util/debug_util.h"
#include "util/file_utils.h"

namespace doris {

class PrimaryKeyIndexTest : public testing::Test {
public:
    PrimaryKeyIndexTest() {}
    virtual ~PrimaryKeyIndexTest() {}

    void SetUp() override {
        if (FileUtils::check_exist(kTestDir)) {
            EXPECT_TRUE(FileUtils::remove_all(kTestDir).ok());
        }
        EXPECT_TRUE(FileUtils::create_dir(kTestDir).ok());
    }
    void TearDown() override {
        if (FileUtils::check_exist(kTestDir)) {
            EXPECT_TRUE(FileUtils::remove_all(kTestDir).ok());
        }
    }

private:
    const std::string kTestDir = "./ut_dir/primary_key_index_test";
};

TEST_F(PrimaryKeyIndexTest, builder) {
    std::string filename = kTestDir + "/builder";
    io::FileWriterPtr file_writer;
    auto fs = io::global_local_filesystem();
    EXPECT_TRUE(fs->create_file(filename, &file_writer).ok());

    PrimaryKeyIndexBuilder builder(file_writer.get());
    builder.init();
    size_t num_rows = 0;
    std::vector<std::string> keys;
    for (int i = 1000; i < 10000; i += 2) {
        keys.push_back(std::to_string(i));
        builder.add_item(std::to_string(i));
        num_rows++;
    }
    EXPECT_EQ("1000", builder.min_key().to_string());
    EXPECT_EQ("9998", builder.max_key().to_string());
    segment_v2::PrimaryKeyIndexMetaPB index_meta;
    EXPECT_TRUE(builder.finalize(&index_meta));
    EXPECT_TRUE(file_writer->close().ok());
    EXPECT_EQ(num_rows, builder.num_rows());

    PrimaryKeyIndexReader index_reader;
    io::FileReaderSPtr file_reader;
    EXPECT_TRUE(fs->open_file(filename, &file_reader).ok());
    EXPECT_TRUE(index_reader.parse(file_reader, index_meta).ok());
    EXPECT_EQ(num_rows, index_reader.num_rows());

    std::unique_ptr<segment_v2::IndexedColumnIterator> index_iterator;
    EXPECT_TRUE(index_reader.new_iterator(&index_iterator).ok());
    bool exact_match = false;
    uint32_t row_id;
    for (size_t i = 0; i < keys.size(); i++) {
        bool exists = index_reader.check_present(keys[i]);
        EXPECT_TRUE(exists);
        auto status = index_iterator->seek_at_or_after(&keys[i], &exact_match);
        EXPECT_TRUE(status.ok());
        EXPECT_TRUE(exact_match);
        row_id = index_iterator->get_current_ordinal();
        EXPECT_EQ(i, row_id);
    }
    // find a non-existing key "8701"
    {
        string key("8701");
        Slice slice(key);
        bool exists = index_reader.check_present(slice);
        EXPECT_FALSE(exists);
        auto status = index_iterator->seek_at_or_after(&slice, &exact_match);
        EXPECT_TRUE(status.ok());
        EXPECT_FALSE(exact_match);
        row_id = index_iterator->get_current_ordinal();
        EXPECT_EQ(3851, row_id);
    }

    // find prefix "87"
    {
        string key("87");
        Slice slice(key);
        bool exists = index_reader.check_present(slice);
        EXPECT_FALSE(exists);
        auto status = index_iterator->seek_at_or_after(&slice, &exact_match);
        EXPECT_TRUE(status.ok());
        EXPECT_FALSE(exact_match);
        row_id = index_iterator->get_current_ordinal();
        EXPECT_EQ(3850, row_id);
    }

    // find prefix "9999"
    {
        string key("9999");
        Slice slice(key);
        bool exists = index_reader.check_present(slice);
        EXPECT_FALSE(exists);
        auto status = index_iterator->seek_at_or_after(&slice, &exact_match);
        EXPECT_FALSE(exact_match);
        EXPECT_TRUE(status.is_not_found());
    }
}

} // namespace doris
