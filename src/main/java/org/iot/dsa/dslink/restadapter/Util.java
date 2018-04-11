package org.iot.dsa.dslink.restadapter;

import org.iot.dsa.io.json.JsonReader;
import org.iot.dsa.node.DSElement;
import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSMap;

public class Util {

    enum AUTH_SCHEME {
        NO_AUTH("No_Auth"),
        BASIC_USR_PASS("Baisc_Usr_Pass"),
        OAUTH2_CLIENT("Auth2_Cli"),
        OAUTH2_USR_PASS("Auth2_Usr_Pass");

        String val;

        AUTH_SCHEME(String val) {
            this.val = val;
        }

        @Override
        public String toString() {
            return val;
        }
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

}
