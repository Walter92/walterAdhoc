package cn.edu.uestc.Adhoc.entity.route;

import java.util.HashMap;
import java.util.Map;

public enum StateFlags {
    // The entry is believed Viable.
    VALID(0),
    // The entry is known to be flawed but has not been removed yet.
    INVALID(1),
    // The entry is broken but a repair operation is possible.
    REPAIRABLE(2),
    // The entry is broken but the repair procedure is underway.
    REPAIRING(5),

    EXPIRED(3),

    RREQSENT(4);
    static Map<StateFlags,String>  map = new HashMap<StateFlags, String> ();
    static {
        map.put(VALID,"有效");
        map.put(INVALID,"失效");
        map.put(REPAIRABLE,"可恢复");
        map.put(EXPIRED,"过期");
    }
    private int value;
    private StateFlags(int value){
        this.value=value;
    }

    public boolean equals(StateFlags stateFlags){
        return this.value==stateFlags.value;
    }
    public String getShow(){
        return map.get(this);
    }
}
