package jp.ac.ynu.tommylab.ecolog.drivingloggerml.getdrivinglog;

/**
 * Created by kawanuma on 2016/05/13.
 */
public class NmeaLogData {

    private String timestamp;
    private String jsonRaw;

    public NmeaLogData(String timestamp, String jsonRaw){

        this.timestamp = timestamp;
        this.jsonRaw = jsonRaw;
    }

    @Override
    public String toString() {

        StringBuilder s = new StringBuilder(100);

        s.append(this.timestamp);
        s.append(",");
        s.append(this.jsonRaw);

        s.append("\r\n");

        return s.toString();
    }
}
