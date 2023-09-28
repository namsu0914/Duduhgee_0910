package com.example.rp;

import static android.content.ContentValues.TAG;
import static com.example.rp.RP_RegisterRequest.getPinnedCertSslSocketFactory;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.example.duduhgee.R;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

public class RP_SavePaymentRequest extends StringRequest{
    final static private String URL = "https://192.168.0.5:443/RP_SERVER/RP_SavePayment.php";
    private Map<String ,String > map;

    public RP_SavePaymentRequest(String userID, String product, String unitPrice, Response.Listener<String> listener, Context context) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        super(Method.POST, URL, listener, null);

        SSLSocketFactory sslSocketFactory = getPinnedCertSslSocketFactory(context, R.raw.server);
        HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);

        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }
        });

        map= new HashMap<>();
        map.put("userID", userID);
        map.put("product",product);
        Log.d(TAG, "RP_SavePaymentRequest: " + product);
        map.put("unitPrice",unitPrice);

    }
    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return map;
    }
}