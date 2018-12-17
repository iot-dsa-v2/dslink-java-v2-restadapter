# Rest Adapter DSLink Usage Guide

The Rest Adapter DSLink can be used to set up subscriptions to DSA values that automatically push updates to a REST API such as BuildingOS.

## Add a Connection

Use one of the actions of the `main` node to add a connection, depending on the authentication scheme used by your REST API. BuildingOS uses OAuth2 Client Flow.

## Add a Rule

Once you have a connection, you can set up a rule using the `Add Rule` action. The Rule's node will then have children displaying the response code, data, and timestamp of the latest response received to a request sent according to the rule.
### Parameters:
- Subscribe Path: A DSA path. The DSLink will subscribe to it and push updates to the REST API.
- REST URL: The URL to push the updates to.
- Method: The REST method to use.
- URL Parameters: A map of URL parameters to use when sending the REST request. If you want to use the value, timestamp, or status of an update in the params, use the placeholders `%VALUE%`, `%TIMESTAMP%` and `%STATUS%`.
- Body: The body of the REST request, if applicable. Once again, use `%VALUE%`, `%TIMESTAMP%` and `%STATUS%` as placeholders.
  - Note: If the REST API supports recieving multiple updates in one message, you can use `%STARTBLOCK%` and `%ENDBLOCK%` to denote the boundaries of a repeatable block. When needing to send many updates at once (in the case of having to catch up after a disconnect), the DSLink will repeat the block for each update and use commas to separate the blocks. Note that this will only work if all value, timestamp, and status placeholders are in the body and inside the repeatable block.
- Minimum Refresh Rate: Optional, ensures that at least this many seconds elapse between updates. This means that the DSLink will suppress updates that are too close together. (Leave this parameter as 0 to not use this feature.)
- Maximum Refresh Rate: Optional, ensures that an update gets sent every this many seconds. This means that if the DSA value updates too infrequently, the DSLink will send duplicate updates. (Leave this parameter as 0 to not use this feature.)
## Add a Rule Table
If you want to set up multiple rules in bulk, create a table of Rules in Atrius Solution Builder/DGLux, and drag it into the `Add Rule Table` action. The table's columns should be as follows:

	row | Subscribe Path | REST URL | Method | URL Parameters | Body | Minimum Refresh Rate | Maximum Refresh Rate
    ==============================================================================================================
    
    
   The Rule Table's node will then have a child that stores each rule's last response code, data, and timestamp (similarly to the single rule node).
   
## Example: BuildingOS

- The REST URL for BuildingOS should be of the form `https://api.buildingos.com/gateways/<gateway_number>/data/`, e.g. `https://api.buildingos.com/gateways/19573/data/`.
- The Method should be `POST`.
- The URL Parameters should just be an empty map: `{}`
- The Body should be of the form:

```
{
  "meta": {
    "naive_timestamp_utc": <true or false>
  },
  "data": {
    "<BuildingOS meter id>": [
      %STARTBLOCK%
      [
        "%TIMESTAMP%",
        %VALUE%
      ]
      %ENDBLOCK%
    ]
  }
}
``` 
e.g:
```
{
  "meta": {
    "naive_timestamp_utc": true
  },
  "data": {
    "b9c7c0a03de611e895505254009e602c": [
      %STARTBLOCK%
      [
        "%TIMESTAMP%",
        %VALUE%
      ]
      %ENDBLOCK%
    ]
  }
}
```

### Rule Table Example
![](https://github.com/iot-dsa-v2/dslink-java-v2-restadapter/blob/develop/docs/rest_adapter_rule_table.png)
