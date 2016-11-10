package cn.edu.uestc.Adhoc.entity.serial;

import java.util.EventObject;

/**
 * Created by walter on 15-12-18.
 */
public class SerialPortEvent extends EventObject {
    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    private Object obj;

    public SerialPortEvent(Serial source) {
        super(source);
        this.obj = source;
    }

    public Object getSource() {
        return obj;
    }
}
