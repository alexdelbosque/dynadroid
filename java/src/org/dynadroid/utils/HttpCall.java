package org.dynadroid.utils;

import android.os.AsyncTask;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.dynadroid.Application;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: brianknorr
 * Date: Aug 27, 2010
 * Time: 7:02:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class HttpCall {
    private static HttpClient httpClient;
    public static long DEFAULT_URL_CACHE_DURATION = 1000 * 60 * 60; //one hour

    private List<Header> headers = new ArrayList<Header>();
    public String result, url, data, cacheTag;
    public boolean lookInCache = true;
    public boolean offline = false;
    public long urlCacheDuration = DEFAULT_URL_CACHE_DURATION;
    public HttpResponse response;

    public HttpCall() {

    }

    public HttpCall(String url) {
        this.url = url;
    }

    public HttpCall url(String url) {
        this.url = url;
        return this;
    }

    public HttpCall data(String data) {
        this.data = data;
        return this;
    }

    public HttpCall addHeader(String name, String value) {
        headers.add(new BasicHeader(name,value));
        return this;
    }

    public HttpCall urlCacheDuration(long durationInMillis) {
        this.urlCacheDuration = durationInMillis;
        return this;
    }

    public HttpCall lookInCache(boolean lookInCache) {
        this.lookInCache = lookInCache;
        return this;
    }

    public HttpCall cacheTag(String tag) {
        this.cacheTag = tag;
        return this;
    }

    public HttpCall get(final HttpDelegate httpDelegate) {
        if (!retrieveFromCache(httpDelegate)) {
            HttpGet get = new HttpGet();
            httpDelegate.addCompleteTask(new AddToCacheDelegateTask());
            performActionInBackground(get, httpDelegate);
        }
        return this;
    }

    public HttpCall post(final HttpDelegate httpDelegate) {
        HttpPost post = new HttpPost();
        try {
            post.setEntity(new StringEntity(this.data));
            performActionInBackground(post, httpDelegate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public HttpCall post(String data, final HttpDelegate httpDelegate) {
        return data(data).post(httpDelegate);
    }
    
    public HttpCall put(final HttpDelegate httpDelegate) {
        HttpPut put = new HttpPut();
        try {
            put.setEntity(new StringEntity(this.data));
            performActionInBackground(put, httpDelegate);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }
    
    public HttpCall put(String data, final HttpDelegate httpDelegate) {
        return data(data).post(httpDelegate);
    }
    
    public HttpCall delete(final HttpDelegate httpDelegate) {
        HttpDelete delete = new HttpDelete();
        performActionInBackground(delete, httpDelegate);
        return this;
    }
    
    public HttpCall delete(String data, final HttpDelegate httpDelegate) {
        return data(data).delete(httpDelegate);
    }

    private String getCacheKey() {
        return (this.cacheTag != null) ? this.url + "_(" +this.cacheTag+")" : this.url;
    }

    private boolean retrieveFromCache(HttpDelegate httpDelegate) {
        HttpResponse response = this.lookInCache ? UrlCache.get(this.getCacheKey()) : null;
        if (response != null) {
            try {
                this.updateWithResponse(response);
            } catch (Exception e) {
                e.printStackTrace();
            }
            httpDelegate.complete(this);
            return true;
        }
        return false;
    }

    private void performActionInBackground(final HttpRequestBase action, final HttpDelegate httpDelegate) {
        final HttpCall me = this;
        new AsyncTask() {
            protected Object doInBackground(Object... objects) {
                performAction(me, action);
                return httpDelegate;
            }

            protected void onPostExecute(Object o) {
                ((HttpDelegate) o).complete(me);
            }
        }.execute();
    }

    private void updateWithResponse(HttpResponse response) throws IllegalStateException, IOException {
        this.response = response;
        HttpEntity entity = response.getEntity();
        BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent()));
        String line, result = "";
        while ((line = br.readLine()) != null) {
            result += line;
        }
        this.result = result;
    }

    protected static synchronized HttpClient httpClient() {
        if (httpClient == null) {
            try {
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);

                SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
                sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

                HttpParams params = new BasicHttpParams();
                HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
                HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

                SchemeRegistry registry = new SchemeRegistry();
                registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
                registry.register(new Scheme("https", sf, 443));

                ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

                httpClient = new DefaultHttpClient(ccm, params);
            } catch (Exception e) {
                httpClient = new DefaultHttpClient();
            }
        }
        return httpClient;
    }
    
    protected static synchronized void performAction(HttpCall httpCall, HttpRequestBase action) {
        try {
            action.setURI(new URI(httpCall.url));

            if (!Application.isOnline()) {
                httpCall.offline = true;
            } else {
                for (Header header : httpCall.headers) {
                    action.addHeader(header);
                }
                HttpResponse response = httpClient().execute(action);
                httpCall.updateWithResponse(response);
                httpCall.offline = false;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class AddToCacheDelegateTask implements HttpDelegateTask {
        public void doTask() {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status <= 299) {
                UrlCache.set(getCacheKey(), response, urlCacheDuration);
            }
        }
    }

    public static class MySSLSocketFactory extends SSLSocketFactory {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
            super(truststore);

            TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            sslContext.init(null, new TrustManager[]{tm}, null);
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        }

        @Override
        public Socket createSocket() throws IOException {
            return sslContext.getSocketFactory().createSocket();
        }
    }

}
