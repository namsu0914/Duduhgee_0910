package com.example.rp;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;
import com.example.asm.ASM_SignatureActivity;
import com.example.duduhgee.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

public class RP_Buy2Activity extends AppCompatActivity {

    private Button btn_buy;
    //private static final String KEY_NAME = userID;
    private KeyStore keyStore;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private BiometricPrompt.AuthenticationCallback authenticationCallback;
    private CancellationSignal cancellationSignal = null;
    private static final String TAG = RP_BuyActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy2);

        btn_buy = findViewById(R.id.btn_buy);

        // 구매하기 버튼
        btn_buy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) throws RuntimeException {
                Intent intent = getIntent();
                String userID = intent.getStringExtra("userID");

                Response.Listener<String> responseListener = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);

                            if (jsonObject.has("Challenge")) {
                                String header = jsonObject.getString("Header");
                                String username = jsonObject.getString("Username");
                                String challenge = jsonObject.getString("Challenge");
                                String policy = jsonObject.getString("Policy");
                                String transaction = jsonObject.getString("Transaction");

                                Log.d(TAG,"Header: "+header);
                                Log.d(TAG,"Username: "+username);
                                Log.d(TAG,"Challenge: "+challenge);
                                Log.d(TAG,"Policy: "+policy);
                                Log.d(TAG, "Transaction: " + transaction);

                                authenticationCallback = new BiometricPrompt.AuthenticationCallback() {
                                    @Override
                                    public void onAuthenticationFailed() {
                                        super.onAuthenticationFailed();
                                        notifyUser("Authentication Failed");
                                    }

                                    @Override
                                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                                        super.onAuthenticationError(errorCode, errString);
                                        notifyUser("Authentication Error: " + errString);
                                    }

                                    @Override
                                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                                        super.onAuthenticationSucceeded(result);
                                        notifyUser("인증에 성공하였습니다");

                                        ASM_SignatureActivity signatureActivity = new ASM_SignatureActivity();
                                        byte[] signedChallenge = signatureActivity.signChallenge(challenge, userID);

                                        if (signedChallenge != null) {
                                            // Method invocation was successful
                                            Log.d(TAG, "Signed Challenge: " + Base64.encodeToString(signedChallenge, Base64.NO_WRAP));

                                            Intent successIntent = new Intent(RP_Buy2Activity.this, RP_BuySuccessActivity.class);
                                            successIntent.putExtra("purchase_item", "toothbrush"); // 구매한 항목 정보 전달
                                            successIntent.putExtra("signed_challenge", signedChallenge); // 서명된 도전 정보 전달
                                            startActivity(successIntent);
                                            finish();

                                        } else {
                                            // Method invocation failed
                                            Log.e(TAG, "Failed to sign the challenge");
                                        }

                                        try {
                                            verifySignature(signedChallenge, challenge, userID); // userID에 실제 사용자의 ID를 전달해야 함
                                        } catch (KeyStoreException | CertificateException |
                                                 IOException | NoSuchAlgorithmException |
                                                 UnrecoverableEntryException |
                                                 KeyManagementException e) {
                                            throw new RuntimeException(e);
                                        }

                                    }
                                };
                                if (checkBiometricSupport()) {
                                    BiometricPrompt biometricPrompt = new BiometricPrompt.Builder(RP_Buy2Activity.this)
                                            .setTitle("지문 인증을 시작합니다")
                                            .setSubtitle("지문 인증 시작")
                                            .setDescription("지문")
                                            .setNegativeButton("Cancel", getMainExecutor(), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    notifyUser("Authentication Cancelled");
                                                }
                                            }).build();

                                    biometricPrompt.authenticate(getCancellationSignal(), getMainExecutor(), authenticationCallback);
                                }

                            } else {
                                Log.e(TAG, "Header not found in JSON response");
                            }
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(), "오류가 발생하였습니다. ", Toast.LENGTH_SHORT).show();
                            throw new RuntimeException(e);
                        }
                    }
                };
                RP_BuyRequest buyRequest = null;
                try {
                    buyRequest = new RP_BuyRequest(userID, responseListener, RP_Buy2Activity.this);
                } catch (CertificateException | NoSuchAlgorithmException | KeyManagementException |
                         IOException | KeyStoreException e) {
                    throw new RuntimeException(e);
                }
                RequestQueue queue = Volley.newRequestQueue(RP_Buy2Activity.this);
                queue.add(buyRequest);
            }
        });
    }

    private void verifySignature(byte[] signString, String chall, String userID) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableEntryException, KeyManagementException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(userID, null);
        publicKey = privateKeyEntry.getCertificate().getPublicKey();
        String stringpublicKey = Base64.encodeToString(publicKey.getEncoded(), Base64.NO_WRAP);

        Response.Listener<String> responseListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    boolean success = jsonObject.getBoolean("purchased");

                    if (success) {
                        // 검증 성공
                        Toast.makeText(getApplicationContext(), "서명이 확인되었습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        // 검증 실패
                        Toast.makeText(getApplicationContext(), "서명이 유효하지 않습니다. ", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "서명 검증 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        };

        RP_VerifyRequest verifyRequest = new RP_VerifyRequest(userID, chall, Base64.encodeToString(signString, Base64.NO_WRAP), stringpublicKey, responseListener, RP_Buy2Activity.this);
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(verifyRequest);
    }

    private void notifyUser(String message) {
        Toast.makeText(RP_Buy2Activity.this, message, Toast.LENGTH_SHORT).show();
    }

    private CancellationSignal getCancellationSignal() {
        cancellationSignal = new CancellationSignal();
        cancellationSignal.setOnCancelListener(() -> notifyUser("Authentication was Cancelled by the user"));
        return cancellationSignal;
    }

    @TargetApi(Build.VERSION_CODES.P)
    public Boolean checkBiometricSupport() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (!keyguardManager.isDeviceSecure()) {
            notifyUser("Fingerprint authentication has not been enabled in settings");
            return false;
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.USE_BIOMETRIC) != PackageManager.PERMISSION_GRANTED) {
            notifyUser("Fingerprint Authentication Permission is not enabled");
            return false;
        }
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            return true;
        } else {
            return false;
        }
    }
}