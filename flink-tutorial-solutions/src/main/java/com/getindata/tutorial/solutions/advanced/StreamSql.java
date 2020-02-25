/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.getindata.tutorial.solutions.advanced;

import com.getindata.tutorial.base.input.SongEventTableSource;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

public class StreamSql {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment sEnv = StreamExecutionEnvironment.getExecutionEnvironment();
        final StreamTableEnvironment tEnv = StreamTableEnvironment.create(sEnv);
        sEnv.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        // FIXME: replace with https://ci.apache.org/projects/flink/flink-docs-release-1.10/dev/table/connect.html#kafka-connector
        tEnv.registerTableSource("songs", new SongEventTableSource());

        final Table table = tEnv.sqlQuery(
                "SELECT " +
                        "TUMBLE_START(t, INTERVAL '3' SECOND) as wStart, " +
                        "TUMBLE_END(t, INTERVAL '3' SECOND) as wEnd, " +
                        "COUNT(1) as cnt, " +
                        "song_name as songName, " +
                        "userId " +
                        "FROM songs " +
                        "WHERE type = 'PLAY' " +
                        "GROUP BY song_name, userId, TUMBLE(t, INTERVAL '3' SECOND)");

        tEnv.toAppendStream(table, TypeInformation.of(Row.class)).print();

        sEnv.execute();
    }

}