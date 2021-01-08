package com.getindata.tutorial.base.input.utils;

import com.getindata.tutorial.base.model.Song;
import org.joda.time.Duration;

import java.util.List;

import static org.apache.flink.shaded.guava18.com.google.common.collect.Lists.newArrayList;

public class Songs {

    public static final List<Song> SONGS = newArrayList(
            new Song(1, toMillis(2, 40), "Yellow Submarine", "The Beatles"),
            new Song(2, toMillis(2, 59), "Get Off Of My Cloud", "The Rolling Stones"),
            new Song(3, toMillis(5, 28), "Let It Bleed", "The Rolling Stones"),
            new Song(4, toMillis(3, 51), "Dancing Queen", "Abba"),
            new Song(5, toMillis(3, 53), "Rolling in the Deep", "Adele"),
            new Song(6, toMillis(3, 11), "Killer Queen", "Queen"),
            new Song(7, toMillis(3, 54), "California Gurls", "Katy Perry"),
            new Song(8, toMillis(4, 57), "Silent All These Years", "Tori Amos"),
            new Song(9, toMillis(6, 6), "Bohemian Rhapsody", "Queen"),
            new Song(10, toMillis(4, 32), "I want to break free", "Queen")
    );

    private static int toMillis(int minutes, int seconds) {
        return (int) Duration.standardMinutes(minutes).plus(Duration.standardSeconds(seconds)).getMillis();
    }

    private Songs() {
    }
}
