package org.xdty.lancamera;

import java.io.UnsupportedEncodingException;

import okio.ByteString;

public class Utils {

    public static String basic(String userInfo) {
        try {
            byte[] bytes = userInfo.getBytes("ISO-8859-1");
            String encoded = ByteString.of(bytes).base64();
            return "Basic " + encoded;
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError();
        }
    }

}
