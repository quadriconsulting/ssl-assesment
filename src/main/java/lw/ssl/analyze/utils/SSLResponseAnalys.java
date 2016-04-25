package lw.ssl.analyze.utils;

import api.lw.ssl.analyze.responce.WebResourceStatus;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by zmushko_m on 21.04.2016.
 */
public class SSLResponseAnalys {
    public static String GRADE_SUCCESS_SYMBOL = "AA+A-";
    public static String NO_GRADE_STATUS_SYMBOL = "T";
    public static long DAYS_TO_EXPIRE = 32;

    public HostAnalysysResponse analysysResult(JSONObject analysisResponseJSON) {
        HostAnalysysResponse hostAnalysysResponse = new SSLResponseAnalys.HostAnalysysResponse();
        hostAnalysysResponse.setSuccessfull(true);
        hostAnalysysResponse.setWebResourceStatus(new WebResourceStatus());

        hostAnalysysResponse.getWebResourceStatus().setHost(analysisResponseJSON.getString("host"));

        if (analysisResponseJSON != null) {
            try {
                JSONArray endpoints = analysisResponseJSON.getJSONArray("endpoints");

                if (endpoints != null) {
                    for (int i = 0; i < endpoints.length(); i++) {
                        JSONObject endpoint = endpoints.getJSONObject(i);

                        if (endpoint != null) {
                            String grade = endpoint.getString("grade");

                            if (grade == null) {
                                hostAnalysysResponse.setSuccessfull(false);
                                hostAnalysysResponse.getWebResourceStatus().setStatus(NO_GRADE_STATUS_SYMBOL);
                            } else if (!GRADE_SUCCESS_SYMBOL.contains(grade)){
                                hostAnalysysResponse.setSuccessfull(false);
                            }
                            hostAnalysysResponse.getWebResourceStatus().setStatus(grade);
                        }
                    }
                }
            } catch (JSONException e) {
                hostAnalysysResponse.setSuccessfull(false);
                hostAnalysysResponse.getWebResourceStatus().setStatus(NO_GRADE_STATUS_SYMBOL);
            }

            try {
                JSONArray cerfs = analysisResponseJSON.getJSONArray("certs");

                if (cerfs != null) {
                    Integer max = null;
                    for (int i = 0; i < cerfs.length(); i++) {
                        JSONObject cerf = cerfs.getJSONObject(i);

                        if (cerf != null) {
                            long notAfter = cerf.getLong("notAfter");
                            int daysCount = Days.daysBetween(new DateTime(),new DateTime(notAfter)).getDays();
                            max = Math.max(max==null?Integer.MIN_VALUE:max,daysCount);
                        }
                    }
                    if(max == null){
                        hostAnalysysResponse.getWebResourceStatus().setExpireDaysText("no certificate");
                        hostAnalysysResponse.setSuccessfull(false);
                    } else {
                        if (max < DAYS_TO_EXPIRE) {
                            hostAnalysysResponse.setSuccessfull(false);
                        }
                        if (max < 0) {
                            hostAnalysysResponse.getWebResourceStatus().setExpireDaysText(Long.toString(-max) + " ago");
                        } else {
                            hostAnalysysResponse.getWebResourceStatus().setExpireDaysText(Long.toString(max));
                        }
                        hostAnalysysResponse.getWebResourceStatus().setExpireDaysAmount(max);
                    }
                }
            } catch (JSONException e) {
                hostAnalysysResponse.setSuccessfull(false);
            }
        }

        return hostAnalysysResponse;
    }

    public class HostAnalysysResponse {
        private boolean isSuccessfull;
        private WebResourceStatus webResourceStatus;

        public boolean isSuccessfull() {
            return isSuccessfull;
        }

        public void setSuccessfull(boolean successfull) {
            isSuccessfull = successfull;
        }

        public WebResourceStatus getWebResourceStatus() {
            return webResourceStatus;
        }

        public void setWebResourceStatus(WebResourceStatus webResourceStatus) {
            this.webResourceStatus = webResourceStatus;
        }
    }
}
