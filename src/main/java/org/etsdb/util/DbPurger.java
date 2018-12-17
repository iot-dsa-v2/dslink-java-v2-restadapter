package org.etsdb.util;

import org.apache.commons.io.FileUtils;
import org.etsdb.Database;
import org.etsdb.TimeRange;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;


public class DbPurger {
    private static DbPurger singleton = null;
    
    private Map<Database<?>, PurgeSettings> databases = new HashMap<Database<?>, PurgeSettings>();
    private ScheduledFuture<?> fut;
    private boolean running;
    
    private DbPurger() {
    }
    
    public static DbPurger getInstance() {
        if (singleton == null) {
            singleton = new DbPurger();
        }
        return singleton;
    }

    public synchronized void addDb(Database<?> db, PurgeSettings purgeSettings) {
        if (!databases.containsKey(db)) {
            databases.put(db, purgeSettings);
        }
    }

    public synchronized void removeDb(Database<?> db) {
        databases.remove(db);
    }

    public void stop() {
        running = false;
        synchronized (this) {
            if (fut != null) {
                fut.cancel(true);
            }
        }
    }

    public Runnable setupPurger() {
        running = true;
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                for (Entry<Database<?>, PurgeSettings> entry : databases.entrySet()) {
                    Database<?> db = entry.getKey();
                    PurgeSettings settings = entry.getValue();
                    if (!(settings.isPurgeEnabled() && running)) {
                        continue;
                    }

                    File path = db.getBaseDir();
                    long currSize = FileUtils.sizeOf(path);
                    long maxSize = settings.getMaxSizeInBytes();
                    long delCount = 0;
//                    LOGGER.info("Deciding whether to purge");
//                    LOGGER.info("curr = " + curr + " , request = " + request);
                    if (maxSize - currSize <= 0) {
                        if (!running) {
                            break;
                        }
//                        LOGGER.info("Going to purge");

                        List<String> series = db.getSeriesIds();
                        if (File.separatorChar != '/') {
                        	List<String> corrected = new ArrayList<String>();
	                        for (String s: series) {
	                        	corrected.add(s.replace(File.separatorChar, '/'));
	                        }
	                        series = corrected;
                        }
//                        LOGGER.info("Purge Step 1");
                        while (maxSize - currSize <= 0) {
//                        	LOGGER.info("Purge Step 2");
	                        TimeRange range = db.getTimeRange(series);
	                        if (range == null || range.isUndefined()) {
	                            break;
	                        }
//	                        LOGGER.info("Purge Step 3");
	                        long from = range.getFrom();
	                        for (String s : series) {
//	                        	LOGGER.info("Purge Step 4");
	                            delCount += db.delete(s, from, from + 3600000);
	                        }
//	                        LOGGER.info("Purge Step 5");
	                        if (delCount <= 0) {
	                            break;
	                        }
//	                        LOGGER.info("Purge Step 6");
	                        currSize = FileUtils.sizeOf(path);
//	                        LOGGER.info("Purge Step 7: curr = " + curr + " , request = " + request);
                        }
                    }
                    if (delCount > 0) {
//                        String p = path.getPath();
//                        LOGGER.info("Deleted {} records from {}", delCount, p);
                    }
                }
            }
        };
        return runner;
    }
}
