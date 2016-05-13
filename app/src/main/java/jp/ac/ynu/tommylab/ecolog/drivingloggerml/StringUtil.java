package jp.ac.ynu.tommylab.ecolog.drivingloggerml;

/**
 * Created by kawanuma on 2016/05/13.
 */
public class StringUtil {

    public static boolean isNullOrEmpty(String text) {

        if (text == null || text.length() <= 0) {
            return true;
        } else {
            return false;
        }
    }

}
