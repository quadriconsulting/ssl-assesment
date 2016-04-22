package api.lw.ssl.analyze.responce;

/**
 * Created by zmushko_m on 22.04.2016.
 */
public class WebResourceStatus {

    public WebResourceStatus() {
    }

    public WebResourceStatus(String host, String status, Integer expireDaysAmount) {
        this.host = host;
        this.status = status;
        this.expireDaysAmount = expireDaysAmount;
    }

    private String host;
    private String status;
    private Integer expireDaysAmount;
    private String expireDaysText;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getExpireDaysAmount() {
        return expireDaysAmount;
    }

    public void setExpireDaysAmount(Integer expireDaysAmount) {
        this.expireDaysAmount = expireDaysAmount;
    }

    public String getExpireDaysText() {
        return expireDaysText;
    }

    public void setExpireDaysText(String expireDaysText) {
        this.expireDaysText = expireDaysText;
    }
}
