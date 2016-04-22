package lw.ssl.analyze.utils.notificators;

import lw.ssl.analyze.utils.PropertyFilesHelper;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Created by zmushko_m on 22.04.2016.
 */
public class GitHubNotificator {
    private static final String EMAIL_PROPERTIES_SERVLET_CONTENT_PATH = "/WEB-INF/properties/github.properties";
    public static final String RESULTS = "results";
    public static final String PATTERN = "yyyy.MM.dd_HH.mm.ss";

    public static void notificate(String messageBody, String subject, ServletContext servletContext) {
        Properties props = PropertyFilesHelper.getPropertyByPath(EMAIL_PROPERTIES_SERVLET_CONTENT_PATH, servletContext);

        if (!props.isEmpty()) {
            try {
                GitHub github = GitHub.connectUsingPassword(props.getProperty("github.login"), props.getProperty("github.password"));
                GHRepository repository = github.getRepository(props.getProperty("github.repository"));
                repository.createContent(messageBody, subject, props.getProperty("github.path")+ RESULTS +new SimpleDateFormat(PATTERN).format(new Date())+".js");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
