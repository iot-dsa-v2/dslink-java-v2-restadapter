package org.iot.dsa.dslink.restadapter;

import org.iot.dsa.io.json.JsonReader;
import org.iot.dsa.node.DSElement;
import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSMap;

public class Util {

    enum AUTH_SCHEME {
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
}
