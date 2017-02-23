package cn.edu.uestc.Adhoc.entity.route;

import java.util.HashMap;
import java.util.Map;

public enum StateFlags {
    // The entry is believed Viable.
    VALID,
    // The entry is known to be flawed but has not been removed yet.
    INVALID,
    // The entry is broken but a repair operation is possible.
    REPAIRABLE,
    // The entry is broken but the repair procedure is underway.
    REPAIRING,

    EXPIRED,

    RREQSENT;
    static Map<StateFlags,String>  map = new HashMap<StateFlags, String> ();
    static {
        map.put(VALID,"valid");
        map.put(INVALID,"invalid");
        map.put(REPAIRABLE,"可恢复");
        map.put(EXPIRED,"过期");
    }
    public String getShow(){
        return map.get(this);
    }
}
