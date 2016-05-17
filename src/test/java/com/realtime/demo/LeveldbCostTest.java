package com.realtime.demo;

import static org.iq80.leveldb.impl.Iq80DBFactory.bytes;
import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

import java.io.File;

import org.iq80.leveldb.Options;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

public class LeveldbCostTest extends AbstractCostTest {
    Options options = new Options();

    org.iq80.leveldb.DB leveldb;

    public LeveldbCostTest() {
        options.createIfMissing(true);
        options.cacheSize(100 * 1048576); // 100MB cache

    }

    @Override
    public void execute(int index) {
        leveldb.put(bytes("data-" + index), bytes("data-" + index));
    }

    @Override
    public void close() {

    }

    @Override
    public void setUp() throws Exception {
        leveldb = factory.open(new File("example"), options);
    }

}
