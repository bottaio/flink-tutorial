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

package com.getindata;

import com.getindata.tutorial.base.kafka.KafkaProperties;
import com.getindata.tutorial.base.model.SongCount;
import com.getindata.tutorial.base.model.SongEvent;
import com.getindata.tutorial.base.utils.shortcuts.Shortcuts;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AdvancedTimeHandling {

    public static void main(String[] args) throws Exception {
        final String userName = KafkaProperties.getUsername();
        final StreamExecutionEnvironment sEnv = StreamExecutionEnvironment.getExecutionEnvironment();
        sEnv.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        KeyedStream<SongEvent, Integer> keyedSongs = Shortcuts.getSongsWithTimestamps(sEnv, userName)
                .filter(new TheRollingStonesFilterFunction())
                .keyBy(new UserKeySelector());

        DataStream<SongCount> counts = keyedSongs.process(new SongCountingProcessFunction());

        counts.print();

        sEnv.execute();
    }

    static class TheRollingStonesFilterFunction implements FilterFunction<SongEvent> {
        @Override
        public boolean filter(SongEvent songEvent) {
            // TODO put your code here
            return true;
        }
    }

    static class UserKeySelector implements KeySelector<SongEvent, Integer> {
        @Override
        public Integer getKey(SongEvent songEvent) {
            // TODO put your code here
            return null;
        }
    }

    static class SongCountingProcessFunction extends KeyedProcessFunction<Integer, SongEvent, SongCount> {

        private static final Logger LOG = LoggerFactory.getLogger(SongCountingProcessFunction.class);

        private static final long FIFTEEN_MINUTES = 15 * 60 * 1000L;

        /**
         * The state that is maintained by this process function
         */
        private ValueState<Integer> counterState;
        private ValueState<Long> lastTimestampState;

        @Override
        public void open(Configuration parameters) {
            counterState = getRuntimeContext().getState(new ValueStateDescriptor<>(
                    "counter",
                    Integer.class
            ));
            lastTimestampState = getRuntimeContext().getState(new ValueStateDescriptor<>(
                    "lastTimestamp",
                    Long.class
            ));
        }

        @Override
        public void processElement(SongEvent songEvent, Context context, Collector<SongCount> collector) throws Exception {
            Integer currentCounter = counterState.value();
            Long lastTimestamp = lastTimestampState.value();

            if (currentCounter == null) {
                // Initialize state.
                // TODO put your code here
            } else {
                // Update state.
                // TODO put your code here
            }
        }

        @Override
        public void onTimer(long timestamp, OnTimerContext ctx, Collector<SongCount> out) throws Exception {
            Integer currentCounter = counterState.value();
            Long lastTimestamp = lastTimestampState.value();

            // TODO put your code here
        }
    }
}
