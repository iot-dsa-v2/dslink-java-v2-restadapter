package org.iot.dsa.dslink.restadapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.etsdb.ByteArrayBuilder;
import org.etsdb.Serializer;
import org.iot.dsa.io.json.JsonReader;
import org.iot.dsa.io.json.JsonWriter;
import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSString;

public class SubUpdateSerializer extends Serializer<SubUpdate> {

    @Override
    public void toByteArray(final ByteArrayBuilder builder, SubUpdate obj, long ts) {
        JsonWriter jw = new JsonWriter(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                builder.put((byte) b); 
            }
        });
        jw.beginList();
        jw.value(obj.dateTime);
        jw.value(obj.value);
        jw.value(obj.status);
        jw.endList();
        jw.close();
    }

    @Override
    public SubUpdate fromByteArray(ByteArrayBuilder builder, long ts) {
        JsonReader jr = new JsonReader(new InputStream() {
            @Override
            public int read() throws IOException {
                return builder.getAvailable() > 0 ? builder.getByte() : -1;
            }
        }, DSString.UTF8.name());
        jr.next();
        DSList l = jr.getList();
        jr.close();
        if (l.size() < 3) {
            throw new RuntimeException("Failed to deserialize subscription update");
        }
        return new SubUpdate(l.getString(0), l.getString(1), l.getString(2), ts);
    }

}