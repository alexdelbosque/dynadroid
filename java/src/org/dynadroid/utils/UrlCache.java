package org.dynadroid.utils;


import org.apache.http.HttpResponse;
import org.dynadroid.Application;
import org.dynadroid.utils.ObjectStore;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

public class UrlCache {
    private static HashMap<String,UrlData> urlMap = null;
    private static String CACHE_FILE = "url_cache_" + Application.activity.versionCode();

    static {
        load();
    }

    public synchronized static void load() {
        urlMap = (HashMap)ObjectStore.load(CACHE_FILE);
        if (urlMap == null) {
            urlMap = new HashMap();
        } else {
            removeAllExpired();
        }
    }

    private synchronized static void removeAllExpired() {
        HashMap<String,UrlData> notExpiredMap = new HashMap();
        for (String url : urlMap.keySet()) {
            UrlData data = urlMap.get(url);
            if (!data.expired()) {
                notExpiredMap.put(url,data);
            }
        }
        urlMap = notExpiredMap;
    }

    private synchronized static boolean removeIfExpired(String url) {
        UrlData urlData = urlMap.get(url);
        if (urlData==null) return true;
        if (urlData.expired()) {
            urlMap.remove(url);
            return true;
        }
        return false;
    }

    public synchronized static HttpResponse get(String url) {
        if (!removeIfExpired(url)) {
            return urlMap.get(url).response;
        }
        return null;
    }

    public synchronized static void set(String url, HttpResponse response, long duration) {
        urlMap.put(url,new UrlData(response, duration));
    }

    public synchronized static void expireAll() {
        urlMap.clear();
    }

    public synchronized static void expire(String url) {
        urlMap.remove(url);
    }

    public synchronized static void save() {
        ObjectStore.save(urlMap, CACHE_FILE);
    }

    private static class UrlData implements Serializable {
        private HttpResponse response;
        private long duration;
        private long timestamp;

        private UrlData(HttpResponse response, long duration) {
            this.response = response;
            this.duration = duration;
            this.timestamp = new Date().getTime();
        }

        boolean expired() {
            //System.out.println("~~~~~~~~~~~~~~ duration= " + duration + " timestamp= " +timestamp + " new Date().getTime()="+new Date().getTime() + " minus=" + (new Date().getTime() - timestamp));
            return (new Date().getTime() - timestamp) > duration;
        }

    }

}
