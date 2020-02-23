package com.getindata;


import com.getindata.AdvancedTimeHandling.SongCountingProcessFunction;
import com.getindata.AdvancedTimeHandling.UserKeySelector;
import com.getindata.tutorial.base.model.SongCount;
import com.getindata.tutorial.base.model.SongEvent;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.operators.KeyedProcessOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static com.getindata.tutorial.base.model.TestDataBuilders.aRollingStonesSongEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;

// FIXME: remove code
@Disabled
class AdvancedTimeHandlingTest {

    @Test
    void shouldEmitNotificationIfUserListensToTheBandAtLeastThreeTimes() throws Exception {
        SongCountingProcessFunction function = new SongCountingProcessFunction();
        KeyedOneInputStreamOperatorTestHarness<Integer, SongEvent, SongCount> harness = getHarness(function);

        harness.open();

        harness.processElement(
                aRollingStonesSongEvent().setUserId(10).setTimestamp(Instant.parse("2020-02-10T12:00:00.0Z").toEpochMilli()).build(),
                Instant.parse("2020-02-10T12:00:00.0Z").toEpochMilli()
        );
        harness.processElement(
                aRollingStonesSongEvent().setUserId(10).setTimestamp(Instant.parse("2020-02-10T12:05:00.0Z").toEpochMilli()).build(),
                Instant.parse("2020-02-10T12:05:00.0Z").toEpochMilli()
        );
        harness.processElement(
                aRollingStonesSongEvent().setUserId(10).setTimestamp(Instant.parse("2020-02-10T12:10:00.0Z").toEpochMilli()).build(),
                Instant.parse("2020-02-10T12:10:00.0Z").toEpochMilli()
        );
        harness.processWatermark(Instant.parse("2020-02-10T12:25:00.0Z").toEpochMilli());

        List<SongCount> output = getResults(harness);

        assertEquals(output.size(), 1);
        assertEquals(output.get(0), new SongCount(10, 3));
    }

    @Test
    void shouldNotEmitNotificationIfGapBetweenSongsIsTooLong() throws Exception {
        SongCountingProcessFunction function = new SongCountingProcessFunction();
        KeyedOneInputStreamOperatorTestHarness<Integer, SongEvent, SongCount> harness = getHarness(function);

        harness.open();

        harness.processElement(
                aRollingStonesSongEvent().setUserId(10).setTimestamp(Instant.parse("2020-02-10T12:00:00.0Z").toEpochMilli()).build(),
                Instant.parse("2020-02-10T12:00:00.0Z").toEpochMilli()
        );
        harness.processElement(
                aRollingStonesSongEvent().setUserId(10).setTimestamp(Instant.parse("2020-02-10T12:05:00.0Z").toEpochMilli()).build(),
                Instant.parse("2020-02-10T12:05:00.0Z").toEpochMilli()
        );
        // gap is longer than 15 minutes
        harness.processWatermark(Instant.parse("2020-02-10T12:25:00.0Z").toEpochMilli());
        harness.processElement(
                aRollingStonesSongEvent().setUserId(10).setTimestamp(Instant.parse("2020-02-10T12:30:00.0Z").toEpochMilli()).build(),
                Instant.parse("2020-02-10T12:30:00.0Z").toEpochMilli()
        );
        harness.processWatermark(Instant.parse("2020-02-10T12:35:00.0Z").toEpochMilli());

        List<SongCount> output = getResults(harness);

        assertEquals(output.size(), 0);
    }


    private KeyedOneInputStreamOperatorTestHarness<Integer, SongEvent, SongCount> getHarness(SongCountingProcessFunction function) throws Exception {
        KeyedProcessOperator<Integer, SongEvent, SongCount> keyedProcessOperator = new KeyedProcessOperator<>(function);
        return new KeyedOneInputStreamOperatorTestHarness<>(
                keyedProcessOperator,
                new UserKeySelector(),
                TypeInformation.of(Integer.class)
        );
    }

    private List<SongCount> getResults(KeyedOneInputStreamOperatorTestHarness<Integer, SongEvent, SongCount> harness) {
        return harness.extractOutputStreamRecords()
                .stream()
                .map(StreamRecord::getValue)
                .collect(Collectors.toList());
    }

}