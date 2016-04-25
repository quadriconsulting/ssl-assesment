package lw.ssl.analyze.utils;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;

/**
 * Created by zmushko_m on 25.04.2016.
 */
public class SSLTest {
    private static final int CONNECT_TIMEOUT_MS = 30 * 1000;
    private static final int CONTENT_READ_TIMEOUT_MS = 30 * 1000;

    public static JSONObject getStatistic(final String host, final String port, boolean isNewAssessment) {
        InputStream inputAnalysisStream = null;

        try {
            final URL getStatisticUrl = new URL(ResourceContainer.getSSLLabsAnalysisUrl(host, port, isNewAssessment));
            final URLConnection connection = getStatisticUrl.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(CONTENT_READ_TIMEOUT_MS);
            inputAnalysisStream = connection.getInputStream();

            String analysisResponseString = IOUtils.toString(inputAnalysisStream);
            JSONObject analysisResponseJSON = new JSONObject(analysisResponseString);

            if ("READY".equals(analysisResponseJSON.getString("status")) || "ERROR".equals(analysisResponseJSON.getString("status"))) {
                return analysisResponseJSON;
            } else {
                int timerInterval;

                if (!"IN_PROGRESS".equals(analysisResponseJSON.getString("status"))) {
                    timerInterval = 5000;
                } else {
                    timerInterval = 10000;
                }

                try {
                    Thread.sleep(timerInterval);
                    return getStatistic(host, port, false);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputAnalysisStream != null) {
                try {
                    inputAnalysisStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }
}
