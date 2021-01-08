package com.getindata.solved;

import com.getindata.JsonDeserializationSchema;
import com.getindata.JsonSerializationSchema;
import com.getindata.tutorial.base.model.SongEvent;
import com.getindata.tutorial.base.model.SongEventType;
import com.getindata.tutorial.base.model.UserStatistics;
import org.apache.flink.api.common.eventtime.TimestampAssigner;
import org.apache.flink.api.common.eventtime.TimestampAssignerSupplier;
import org.apache.flink.api.common.eventtime.Watermark;
import org.apache.flink.api.common.eventtime.WatermarkGenerator;
import org.apache.flink.api.common.eventtime.WatermarkGeneratorSupplier;
import org.apache.flink.api.common.eventtime.WatermarkOutput;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.util.Collector;

import java.util.Properties;

public class KafkaWindowAggregations {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment sEnv = StreamExecutionEnvironment.getExecutionEnvironment();
        sEnv.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        final Properties kafkaProperties = new Properties();
        kafkaProperties.setProperty("bootstrap.servers", "flink-slave-01.c.getindata-training.internal:9092,flink-slave-02.c.getindata-training.internal:9092,flink-slave-03.c.getindata-training.internal:9092,flink-slave-04.c.getindata-training.internal:9092,flink-slave-05.c.getindata-training.internal:9092");
        // TODO: Replace with the line below if you use docker.
        // kafkaProperties.setProperty("bootstrap.servers", "kafka:9092");

        final String inputTopic = "songs_alpaca"; // FIXME put your user name here
        final String outputTopic = "statistics_alpaca"; // FIXME put your user name here

        // create a stream of events from source
        final DataStream<SongEvent> events = sEnv.addSource(
                new FlinkKafkaConsumer<>(
                        inputTopic,
                        new JsonDeserializationSchema<>(SongEvent.class),
                        kafkaProperties
                )
        );

        final DataStream<UserStatistics> statistics = pipeline(events);

        statistics.addSink(
                new FlinkKafkaProducer<>(
                        outputTopic,
                        new JsonSerializationSchema<>(outputTopic),
                        kafkaProperties,
                        FlinkKafkaProducer.Semantic.EXACTLY_ONCE
                )
        );

        // execute streams
        sEnv.execute();
    }


    static DataStream<UserStatistics> pipeline(DataStream<SongEvent> source) {
        final DataStream<SongEvent> eventsInEventTime = source.assignTimestampsAndWatermarks(new SongWatermarkStrategy());

        // song plays in user sessions
        final WindowedStream<SongEvent, Integer, TimeWindow> windowedStream = eventsInEventTime
                .filter(new SongFilterFunction())
                .keyBy(new SongKeySelector())
                .window(EventTimeSessionWindows.withGap(Time.minutes(20)));

        return windowedStream.aggregate(
                new SongAggregationFunction(),
                new SongWindowFunction()
        );
    }

    static class SongWatermarkStrategy implements WatermarkStrategy<SongEvent> {

        private static final long FIVE_MINUTES = 5 * 1000 * 60L;

        @Override
        public WatermarkGenerator<SongEvent> createWatermarkGenerator(WatermarkGeneratorSupplier.Context context) {
            return new WatermarkGenerator<SongEvent>() {
                @Override
                public void onEvent(SongEvent songEvent, long eventTimestamp, WatermarkOutput output) {
                    Watermark watermark = songEvent.getUserId() % 2 == 1
                            ? new Watermark(songEvent.getTimestamp())
                            : new Watermark(songEvent.getTimestamp() - FIVE_MINUTES);
                    output.emitWatermark(watermark);
                }

                @Override
                public void onPeriodicEmit(WatermarkOutput output) {
                    // don't need to do anything because we emit in reaction to events above
                }
            };
        }

        @Override
        public TimestampAssigner<SongEvent> createTimestampAssigner(TimestampAssignerSupplier.Context context) {
            return (element, recordTimestamp) -> element.getTimestamp();
        }
    }

    static class SongFilterFunction implements FilterFunction<SongEvent> {
        @Override
        public boolean filter(final SongEvent songEvent) {
            return songEvent.getType() == SongEventType.PLAY;
        }
    }

    static class SongKeySelector implements KeySelector<SongEvent, Integer> {
        @Override
        public Integer getKey(SongEvent songEvent) {
            return songEvent.getUserId();
        }
    }

    static class SongAggregationFunction implements AggregateFunction<SongEvent, Long, Long> {
        @Override
        public Long createAccumulator() {
            return 0L;
        }

        @Override
        public Long add(SongEvent songEvent, Long count) {
            return count + 1;
        }

        @Override
        public Long getResult(Long count) {
            return count;

        }

        @Override
        public Long merge(Long count1, Long count2) {
            return count1 + count2;
        }

    }

    static class SongWindowFunction implements WindowFunction<Long, UserStatistics, Integer, TimeWindow> {
        @Override
        public void apply(Integer userId, TimeWindow window, Iterable<Long> input, Collector<UserStatistics> out) {
            long sum = 0;
            for (Long l : input) {
                sum += l;
            }

            out.collect(
                    UserStatistics.builder()
                            .userId(userId)
                            .count(sum)
                            .start(window.getStart())
                            .end(window.getEnd())
                            .build()
            );
        }
    }
}
