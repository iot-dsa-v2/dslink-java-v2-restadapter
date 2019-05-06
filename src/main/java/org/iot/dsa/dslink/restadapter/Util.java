package org.iot.dsa.dslink.restadapter;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import org.etsdb.Database;
import org.etsdb.DatabaseFactory;
import org.etsdb.QueryCallback;
import org.etsdb.util.DbPurger;
import org.etsdb.util.PurgeSettings;
import org.iot.dsa.DSRuntime;
import org.iot.dsa.io.json.JsonReader;
import org.iot.dsa.node.DSElement;
import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSMap;

public class Util {
    
    private static Database<SubUpdate> buffer = null;
    private static PurgeSettings bufferPurgesSettings = MainNode.instance;

    public enum AUTH_SCHEME {
        NO_AUTH,
        BASIC_USR_PASS,
        OAUTH2_CLIENT,
        OAUTH2_USR_PASS
    }
    
    public static Object dsElementToObject(DSElement element) {
        if (element.isBoolean()) {
            return element.toBoolean();
        } else if (element.isNumber()) {
            return element.toInt();
        } else if (element.isList()) {
            DSList dsl = element.toList();
            String[] arr = new String[dsl.size()];
            int i = 0;
            for (DSElement e: dsl) {
                arr[i] = e.toString();
                i++;
            }
            return arr;
        } else {
            return element.toString();
        }
    }
    
    public static DSMap dsElementToMap(DSElement elem) {
        if (elem instanceof DSMap) {
            return (DSMap) elem;
        } else {
            try (JsonReader reader = new JsonReader(elem.toString())) {
                return reader.getMap();
            } catch (Exception e) {
                return new DSMap();
            }
        }
    }
    
    public static double getDouble(DSList row, int index, double def) {
        try {
            return row.getDouble(index);
        } catch (Exception e) {
            try {
                return Double.parseDouble(row.getString(index));
            } catch (Exception e1) {
                return def;
            }
        }
    }
    
    public static double getDouble(DSMap map, String key, double def) {
        try {
            return map.getDouble(key);
        } catch (Exception e) {
            try {
                return Double.parseDouble(map.getString(key));
            } catch (Exception e1) {
                return def;
            }
        }
    }
    
    public static void setBufferPurgeSettings(PurgeSettings purgeSettings) {
        bufferPurgesSettings = purgeSettings;
    }
    
    private static void initBuffer() {
        File f = new File(Constants.BUFFER_PATH);
        buffer = DatabaseFactory.createDatabase(f, new SubUpdateSerializer());
        DbPurger purger = DbPurger.getInstance();
        purger.addDb(buffer, bufferPurgesSettings);
        Runnable purgeRunner = purger.setupPurger();
        DSRuntime.runAfterDelay(purgeRunner, 30000, 30000);
    }
    
    public static void storeInBuffer(String subId, SubUpdate update) {
        if (buffer == null) {
            initBuffer();
        }
        buffer.write(subId, update.ts, update);
    }
    
//    public static boolean isBufferEmpty(String subPath) {
//        if (buffer == null) {
//            return true;
//        }
//        buffer.get
//        return true;
//    }
    
    public static boolean processBuffer(String subId, UpdateSender subRule) {
        if (buffer == null) {
            return true;
        }
//        TimeRange range = buffer.getTimeRange(Collections.singletonList(subPath));
//        if (range == null || range.isUndefined()) {
//            return;
//        }
        final int maxBatchSize = subRule.getMaxBatchSize();
        final LinkedList<SubUpdate> updates = new LinkedList<SubUpdate>();
        final AtomicLong firstTs = new AtomicLong();
        final AtomicLong lastTs = new AtomicLong();
        int count;
        do {
            updates.clear();
            buffer.query(subId, lastTs.get(), Long.MAX_VALUE, maxBatchSize, new QueryCallback<SubUpdate>() {
                
                @Override
                public void sample(String seriesId, long ts, SubUpdate value) {
                    updates.add(value);
                    if (ts > lastTs.get()) {
                        lastTs.set(ts);
                    }
                }
            });
            count = updates.size();
            Queue<SubUpdate> failedUpdates = subRule.sendBatchUpdate(updates);
            if (failedUpdates == null || failedUpdates.size() < count) {
                buffer.delete(subId, firstTs.get(), lastTs.get());
                firstTs.set(lastTs.get());
                if (failedUpdates != null) {
                    for (SubUpdate failedUpdate: failedUpdates) {
                        storeInBuffer(subId, failedUpdate);
                    }
                }
            } else {
                return false;
            }
        } while (count >= maxBatchSize);
        
        return true;
    }
}
