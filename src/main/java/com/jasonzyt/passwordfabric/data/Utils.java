package com.jasonzyt.passwordfabric.data;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    public static String doSha256(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] array = md.digest(str.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte item : array) {
                sb.append(Integer.toHexString((item & 0xFF) | 0x100), 1, 3);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public record IPInfo(String status, String country, String countryCode, String region, String regionName,
                         String city, String zip, Double lat, Double lon, String timezone, String isp, String org,
                         String as, String query) {
        public static IPInfo fromJson(String json) {
            return new Gson().fromJson(json, IPInfo.class);
        }

        public String getCountry() {
            return country;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public String getRegion() {
            return region;
        }

        public String getRegionName() {
            return regionName;
        }

        public String getCity() {
            return city;
        }

        public String getZip() {
            return zip;
        }

        public Double getLat() {
            return lat;
        }

        public Double getLon() {
            return lon;
        }

        public String getTimezone() {
            return timezone;
        }

        public String getIsp() {
            return isp;
        }

        public String getOrg() {
            return org;
        }

        public String getAs() {
            return as;
        }

        public String getQuery() {
            return query;
        }
    }

    public static IPInfo getIPInfo(String ip) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("https://ip-api.com/json/" + ip + "?lang=zh-CN").build();
        try (
            Response response = client.newCall(request).execute()
        ) {
            if (response.body() != null) {
                return IPInfo.fromJson(response.body().string());
            }
            return null;
        }
    }

}