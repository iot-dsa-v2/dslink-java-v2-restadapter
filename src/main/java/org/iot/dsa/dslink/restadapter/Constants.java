package org.iot.dsa.dslink.restadapter;

public class Constants {
    
    // Parameters
    public static final String NAME = "Name";
    
    public static final String USERNAME = "Username";
    public static final String PASSWORD = "Password";
    public static final String CLIENT_ID = "Client ID";
    public static final String CLIENT_SECRET = "Client Secret";
    public static final String TOKEN_URL = "Token URL";
    public static final String CONNTYPE = "ConnType";
    
    public static final String SUB_PATH = "Subscribe Path";
    public static final String REST_URL = "REST URL";
    public static final String REST_METHOD = "Method";
    public static final String URL_PARAMETERS = "URL Parameters";
    public static final String REQUEST_BODY = "Body";
    public static final String RULE_TABLE = "Table";
    public static final String MIN_REFRESH_RATE = "Minimum Refresh Rate";
    public static final String MAX_REFRESH_RATE = "Maximum Refresh Rate";
    public static final String USE_BUFFER = "Buffer Enabled";
    public static final String MAX_BATCH_SIZE = "Maximum Batch Size";
    public static final String BUFFER_PURGE_ENABLED = "Enable Buffer Auto-Purge";
    public static final String BUFFER_MAX_SIZE = "Maximum Buffer Size";
    
    //Actions
    public static final String ACT_ADD_BASIC_CONN = "Basic Connection";
    public static final String ACT_ADD_OAUTH_CLIENT_CONN = "OAuth2 Client Flow";
    public static final String ACT_ADD_OAUTH_PASSWORD_CONN = "OAuth2 Password Flow";
    public static final String ACT_ADD_RULE = "Add Rule";
    public static final String ACT_ADD_RULE_TABLE = "Add Rule Table";
    public static final String ACT_REMOVE = "Remove";
    public static final String ACT_EDIT = "Edit";
    
    //Values
    public static final String PARAMS = "parameters";
    
    public static final String LAST_RESPONSE_CODE = "Last Response Code";
    public static final String LAST_RESPONSE_DATA = "Last Response Data";
    public static final String LAST_RESPONSE_TS = "Last Response Timestamp";
    public static final String LAST_RESPONSES_TABLE = "Last Responses";
    
    //FORMAT PLACEHOLDERS
    public static final String PLACEHOLDER_VALUE = "%VALUE%";
    public static final String PLACEHOLDER_TS = "%TIMESTAMP%";
    public static final String PLACEHOLDER_STATUS = "%STATUS%";
    public static final String PLACEHOLDER_BLOCK_START = "%STARTBLOCK%";
    public static final String PLACEHOLDER_BLOCK_END = "%ENDBLOCK%";
    
    //MISC
    public static final String BUFFER_PATH = "buffer";
}
