package com.example.rp;

import static android.content.ContentValues.TAG;
import static com.example.rp.RP_RegisterRequest.getPinnedCertSslSocketFactory;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

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

public class RP_BuyRequest extends StringRequest {
    final static private String URL = "https://192.168.0.5:443/RP_SERVER/FIDOBuyRequest.php";

    private Map<String ,String> map;

    public RP_BuyRequest(String userID, String p_id, Response.Listener<String> listener, Context context) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        super(Method.POST, URL, listener, null);

        SSLSocketFactory sslSocketFactory = getPinnedCertSslSocketFactory(context, R.raw.server);
        HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);

        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }
        });

        map = new HashMap<>();
        map.put("userID",userID);
        Log.d(TAG, "userID : "+ userID);
        map.put("p_id", p_id);
        Log.d(TAG, "p_id : "+ p_id);
    }
    @Nullable
    @Override
    protected Map<String, String> getParams() throws AuthFailureError {

        return map;
    }
}
