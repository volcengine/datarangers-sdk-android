// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

/**
 * uri config for all services in {@link com.bytedance.applog.server.Api}
 */
public class UriConfig {

    public static final String PATH_REGISTER = "/service/2/device_register/";

    public static final String PATH_DEVICE_UPDATE = "/service/2/device_update";

    public static final String PATH_SEND = "/service/2/app_log/";

    public static final String PATH_CONFIG = "/service/2/log_settings/";

    public static final String PATH_AB = "/service/2/abtest_config/";

    public static final String PATH_PROFILE = "/service/2/profile/";

    public static final String PATH_ALINK_QUERY = "/service/2/alink_data";

    public static final String PATH_ALINK_ATTRIBUTION = "/service/2/attribution_data";

    /**
     * uri for register service mandatory
     */
    private String mRegisterUri;

    private String mReportOaidUri;

    /**
     * uri for send service mandatory
     */
    private String[] mSendUris;

    /**
     * uri for setting service optional: if empty, setting service is disabled
     */
    private String mSettingUri;

    /**
     * uri for ab service optional: if empty, ab service is disabled
     */
    private String mAbUri;

    /**
     * uri for profile service optional: if empty, profile service is disabled
     */
    private String mProfileUri;

    /**
     * uri for business service optional: if empty, profile service is disabled
     */
    private String mBusinessUri;

    private String mAlinkQueryUri;

    private String mAlinkAttributionUri;

    private UriConfig(Builder builder) {
        this.mRegisterUri = builder.mRegisterUri;
        this.mReportOaidUri = builder.mReportOaidUri;
        this.mSendUris = builder.mSendUris;
        this.mSettingUri = builder.mSettingUri;
        this.mAbUri = builder.mAbUri;
        this.mProfileUri = builder.mProfileUri;
        this.mBusinessUri = builder.mBusinessUri;
        this.mAlinkQueryUri = builder.mAlinkQueryUri;
        this.mAlinkAttributionUri = builder.mAlinkAttributionUri;
    }

    public static class Builder {
        private String mRegisterUri;
        private String mReportOaidUri;
        private String mActiveUri;
        private String[] mSendUris;
        private String mSettingUri;
        private String mAbUri;
        private String mProfileUri;
        private String mBusinessUri;
        private String mAlinkQueryUri;
        private String mAlinkAttributionUri;

        public Builder setRegisterUri(final String uri) {
            this.mRegisterUri = uri;
            return this;
        }

        public Builder setReportOaidUri(final String uri) {
            this.mReportOaidUri = uri;
            return this;
        }

        public Builder setActiveUri(final String uri) {
            this.mActiveUri = uri;
            return this;
        }

        public Builder setSendUris(final String[] uris) {
            this.mSendUris = uris;
            return this;
        }

        public Builder setSettingUri(final String uri) {
            this.mSettingUri = uri;
            return this;
        }

        public Builder setAbUri(final String uri) {
            this.mAbUri = uri;
            return this;
        }

        public Builder setProfileUri(final String uri) {
            this.mProfileUri = uri;
            return this;
        }

        public Builder setBusinessUri(final String uri) {
            this.mBusinessUri = uri;
            return this;
        }

        public Builder setALinkQueryUri(final String uri) {
            this.mAlinkQueryUri = uri;
            return this;
        }

        public Builder setALinkAttributionUri(final String uri) {
            this.mAlinkAttributionUri = uri;
            return this;
        }

        public UriConfig build() {
            return new UriConfig(this);
        }
    }

    public String getRegisterUri() {
        return mRegisterUri;
    }

    public String getReportOaidUri() {
        return mReportOaidUri;
    }

    public String[] getSendUris() {
        return mSendUris;
    }

    public String getSettingUri() {
        return mSettingUri;
    }

    public String getAbUri() {
        return mAbUri;
    }

    public String getProfileUri() {
        return mProfileUri;
    }

    public String getBusinessUri() {
        return mBusinessUri;
    }

    public String getAlinkAttributionUri() {
        return mAlinkAttributionUri;
    }

    public String getAlinkQueryUri() {
        return mAlinkQueryUri;
    }

    public void setRegisterUri(final String uri) {
        mRegisterUri = uri;
    }

    public void setReportOaidUri(final String uri) {
        mReportOaidUri = uri;
    }

    public void setSendUris(final String[] uris) {
        mSendUris = uris;
    }

    public void setSettingUri(final String uri) {
        mSettingUri = uri;
    }

    public void setAbUri(final String uri) {
        mAbUri = uri;
    }

    public void setProfileUri(final String uri) {
        mProfileUri = uri;
    }

    public void setBusinessUri(final String uri) {
        mBusinessUri = uri;
    }

    public void setALinkQueryUri(final String uri) {
        mAlinkQueryUri = uri;
    }

    public void setALinkAttributionUri(final String uri) {
        mAlinkAttributionUri = uri;
    }

    /**
     * Create a UriConfig with the given domain.
     *
     * @param domain          domain for register/active/send/config/ab, e.g: "https://log.snssdk.com"
     * @param extraSendDomain extra domain for send.
     * @return a UriConfig without profileUri and monitorUri.
     */
    public static UriConfig createByDomain(String domain, String[] extraSendDomain) {
        Builder builder = new Builder();
        builder.setRegisterUri(domain + PATH_REGISTER)
                .setReportOaidUri(domain + PATH_DEVICE_UPDATE)
                .setALinkAttributionUri(domain + PATH_ALINK_ATTRIBUTION)
                .setALinkQueryUri(domain + PATH_ALINK_QUERY);
        if (extraSendDomain == null || extraSendDomain.length == 0) {
            builder.setSendUris(new String[]{domain + PATH_SEND});
        } else {
            String[] sendUris = new String[extraSendDomain.length + 1];
            sendUris[0] = domain + PATH_SEND;
            for (int i = 1; i < sendUris.length; ++i) {
                sendUris[i] = extraSendDomain[i - 1] + PATH_SEND;
            }
            builder.setSendUris(sendUris);
        }
        builder.setSettingUri(domain + PATH_CONFIG)
                .setAbUri(domain + PATH_AB)
                .setProfileUri(domain + PATH_PROFILE);
        return builder.build();
    }
}
