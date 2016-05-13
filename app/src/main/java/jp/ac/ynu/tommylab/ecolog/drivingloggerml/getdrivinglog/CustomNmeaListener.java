package jp.ac.ynu.tommylab.ecolog.drivingloggerml.getdrivinglog;

import android.location.GpsStatus;

import java.util.Calendar;

import jp.ac.ynu.tommylab.ecolog.drivingloggerml.StringUtil;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.TimeStamp;

/**
 * Created by kawanuma on 2016/04/19.
 */
public class CustomNmeaListener implements GpsStatus.NmeaListener {

    private GetDrivingLog gdl;

    public CustomNmeaListener(GetDrivingLog gdl) {
        this.gdl = gdl;
    }

    @Override
    public void onNmeaReceived(long timestamp, String nmeaSentence) {

        if (StringUtil.isNullOrEmpty(nmeaSentence)) {
            return;
        }
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(timestamp);
        this.gdl.addNmeaLog(TimeStamp.getSplitedTimeStringFromYearTilllMillis(time), nmeaSentence);
    }
}
