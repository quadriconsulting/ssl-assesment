package lw.ssl.analyze.utils;

import java.text.MessageFormat;

/**
 * Created by zmushko_m on 21.04.2016.
 */
public class ResourceContainer {
    private static final String SSL_LABS_URL = "https://dev.ssllabs.com/api/v3/";
    private static final String SSL_LABS_ANALYSIS_PREFIX = "analyze?all=on";
    private static final String SSL_LABS_INFO_PREFIX = "info";

    public static String getSSLLabsInfoUrl() {
        return new StringBuffer().append(SSL_LABS_URL).append(SSL_LABS_INFO_PREFIX).toString();
    }

    public static String getSSLLabsAnalysisUrl(String host, String port) {
        StringBuffer sslLabsUrl = new StringBuffer().append(SSL_LABS_URL).append(SSL_LABS_ANALYSIS_PREFIX);

        if (host != null && host != "") {
            sslLabsUrl.append(MessageFormat.format("&host={0}", host));
        }
        if (port != null && port != "") {
            sslLabsUrl.append(MessageFormat.format("&port={0}", port));
        }
        return sslLabsUrl.toString();
    }
}
