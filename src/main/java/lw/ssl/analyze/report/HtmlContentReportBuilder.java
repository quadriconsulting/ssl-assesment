package lw.ssl.analyze.report;

import api.lw.ssl.analyze.responce.WebResourceStatus;
import lw.ssl.analyze.utils.SSLResponseAnalys;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

/**
 * Created by zmushko_m on 25.04.2016.
 */
public class HtmlContentReportBuilder {
    public static String HTML_TEMPLATE_PATH = "template/reportTemplate.html";
    public static String URLS_TO_GET_FULL_REPORT_TEMPLATE = "https://www.ssllabs.com/ssltest/analyze.html?ignoreMismatch=on&latest&d={0}";

    public static String getHtmlContentReport(List<WebResourceStatus> webResourceStatusList, ServletContext servletContext) {
        if (webResourceStatusList != null && webResourceStatusList.size() > 0) {
            try {
                String templateContent = IOUtils.toString(servletContext.getResourceAsStream(HTML_TEMPLATE_PATH));

                StringBuffer reportTableRows = new StringBuffer();
                for (WebResourceStatus webResourceStatus : webResourceStatusList) {
                    reportTableRows.append("<tr>");
                        reportTableRows.append("<td>");
                            reportTableRows.append(webResourceStatus.getHost());
                        reportTableRows.append("</td>");
                        reportTableRows.append("<td>");
                            reportTableRows.append("<a href='" + MessageFormat.format(URLS_TO_GET_FULL_REPORT_TEMPLATE, addPrefixIsNeeded(webResourceStatus.getHost(), "www.") + "' target='_blank'>"));
                                reportTableRows.append("<span " + (SSLResponseAnalys.GRADE_SUCCESS_SYMBOL.contains(webResourceStatus.getStatus())?"":"style='color: green;'") + ">");
                                    reportTableRows.append(webResourceStatus.getStatus());
                                reportTableRows.append("</span>");
                            reportTableRows.append("</a>");
                        reportTableRows.append("</td>");
                        reportTableRows.append("<td>");
                            if (webResourceStatus.getExpireDaysAmount() == null) {
                                reportTableRows.append("<span>");
                            } else {
                                reportTableRows.append("<span " + (webResourceStatus.getExpireDaysAmount() > 32?"style='color: green;'":"style='color: red;'") + ">");
                            }
                                reportTableRows.append(webResourceStatus.getExpireDaysText() != null && !"".equals(webResourceStatus.getExpireDaysText())?webResourceStatus.getExpireDaysText():"");
                            reportTableRows.append("</span>");
                        reportTableRows.append("</td>");
                    reportTableRows.append("</tr>");
                }

                return MessageFormat.format(templateContent, reportTableRows);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            return "";
        }

        return "";
    }

    private static String addPrefixIsNeeded(String currentString, String prefix) {
        StringBuffer resultString = new StringBuffer();

        if (!currentString.startsWith(prefix)) {
            resultString.append(prefix);
        }
        resultString.append(currentString);
        return resultString.toString();
    }
}
