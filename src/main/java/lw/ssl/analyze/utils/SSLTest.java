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

    public static class SSLInfo {
        private int maxAssessments;
        private int currentAssessments;
        private int newAssessmentCoolOff;

        public SSLInfo() {
        }

        public SSLInfo(int maxAssessments, int currentAssessments, int newAssessmentCoolOff) {
            this.maxAssessments = maxAssessments;
            this.currentAssessments = currentAssessments;
            this.newAssessmentCoolOff = newAssessmentCoolOff;
        }

        public int getPossibleAssessmentsAmount() {
            return maxAssessments - currentAssessments;
        }

        public int getMaxAssessments() {
            return maxAssessments;
        }

        public void setMaxAssessments(int maxAssessments) {
            this.maxAssessments = maxAssessments;
        }

        public int getCurrentAssessments() {
            return currentAssessments;
        }

        public void setCurrentAssessments(int currentAssessments) {
            this.currentAssessments = currentAssessments;
        }

        public int getNewAssessmentCoolOff() {
            return newAssessmentCoolOff;
        }

        public void setNewAssessmentCoolOff(int newAssessmentCoolOff) {
            this.newAssessmentCoolOff = newAssessmentCoolOff;
        }
    }

    public static SSLInfo getCountOfPossibleAssessments() {
        InputStream inputInfoStream = null;

        try {
            final URL getStatisticUrl = new URL(ResourceContainer.getSSLLabsInfoUrl());
            final URLConnection connection = getStatisticUrl.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(CONTENT_READ_TIMEOUT_MS);
            inputInfoStream = connection.getInputStream();

            String analysisResponseString = IOUtils.toString(inputInfoStream);
            JSONObject infoJSON = new JSONObject(analysisResponseString);

            return new SSLInfo(infoJSON.getInt("maxAssessments"), infoJSON.getInt("currentAssessments"), infoJSON.getInt("newAssessmentCoolOff"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ;

        return new SSLInfo();
    }

    public static JSONObject getStatistic(final String host, final String port, boolean isNewAssessment) throws IOException {
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
