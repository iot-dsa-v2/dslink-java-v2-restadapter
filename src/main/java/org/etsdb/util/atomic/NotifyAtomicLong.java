package org.etsdb.util.atomic;

import java.util.concurrent.atomic.AtomicLong;
import org.etsdb.util.Handler;

/**
 * @author Samuel Grenier
 */
public class NotifyAtomicLong {

    private final AtomicLong aLong = new AtomicLong();
    private Handler<Long> handler;

    public void setHandler(Handler<Long> handler) {
        this.handler = handler;
    }

    public long get() {
        return aLong.get();
    }

    public void set(long val) {
        aLong.set(val);
        notifyHandler(val);
    }

    public long addAndGet(long l) {
        return notifyHandler(aLong.addAndGet(l));
    }

    public long incrementAndGet() {
        return notifyHandler(aLong.incrementAndGet());
    }

    private long notifyHandler(long l) {
        if (handler != null) {
            handler.handle(l);
        }
        return l;
    }
}
