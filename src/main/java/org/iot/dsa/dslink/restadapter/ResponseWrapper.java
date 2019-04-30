package org.iot.dsa.dslink.restadapter;

import org.iot.dsa.time.DSDateTime;

public interface ResponseWrapper {
    
    public int getCode();
    
    public String getData();
    
    public DSDateTime getTS();

}
