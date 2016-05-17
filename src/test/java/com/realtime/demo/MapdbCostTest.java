package com.realtime.demo;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

public class MapdbCostTest extends AbstractCostTest {
    DB db = DBMaker.fileDB("traffic.mapdb").fileMmapEnable().make();//closeOnJvmShutdown()

    HTreeMap<Integer, String> map =
        (HTreeMap<Integer, String>) db.hashMap("hash_rate").createOrOpen(); //.treeMap("collectionName").createOrOpen();

    public MapdbCostTest() {
    }

    @Override
    public void execute(int index) {
        map.put(index, "data-" + index);
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
        db.commit();
        db.close();
    }

    @Override
    public void setUp() throws Exception {

    }

}
