package com.ipaloma.jxbpaydemo;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private final static String TAG = "MainActivity";
    private Button mCheckoutBtn;
    private EditText mSandbox;

    private Activity mActivity = null;

    private int payment_requestcode = 666666;
    private Intent mLoadActivityIntent;
    private String mResultUrl;
    private RadioGroup mChannel;
    private EditText mBillNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Sandbox
        mSandbox = (EditText) findViewById(R.id.sandbox_value);
        // 订单编号
        mBillNumber = (EditText) findViewById(R.id.billnumber_value);
        //支付方式选择
        mChannel = (RadioGroup) findViewById(R.id.channels);

        // 支付按钮
        mCheckoutBtn = (Button) findViewById(R.id.checkout);

        mCheckoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sandbox = mSandbox.getText().toString().trim();
                if(sandbox.trim() == "") {
                    Toast.makeText(mActivity, R.string.no_sandbox_specified, Toast.LENGTH_LONG).show();
                    return;
                }
                String billnumber = mSandbox.getText().toString().trim();
                if(sandbox.trim() == "") {
                    Toast.makeText(mActivity, R.string.no_billnumber_specified, Toast.LENGTH_LONG).show();
                    return;
                }
                int checkedId = mChannel.getCheckedRadioButtonId();
                mLoadActivityIntent = getPackageManager().getLaunchIntentForPackage("com.ipaloma.jxbpay");
                if(mLoadActivityIntent != null) {
                    mLoadActivityIntent.putExtra("amount", 0.01);				// 支付金额
                    mLoadActivityIntent.putExtra("sandbox", sandbox);	// 注册商户的二级域名
                    mLoadActivityIntent.putExtra("title", "经销宝收银台");	// 定义收银台界面的title
                    mLoadActivityIntent.putExtra("billnumber", billnumber);	// 订单编号
                    mLoadActivityIntent.putExtra("notifyurl", "http://xxx?orderid="+billnumber);	// 支付完成后，将会调用此url（http post）通知结果(json格式)
                    mLoadActivityIntent.putExtra("env",  checkedId == R.id.dev ? "dev" : checkedId == R.id.demo ? "demo" : "");

                    startActivityForResult(mLoadActivityIntent, payment_requestcode);
                }
                // TODO the application not installed, try to install it first
            }
        });
        mActivity = this;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "---onActivityResult--- \n" + requestCode + " " + resultCode + " " + (data == null ? "data = null" : data.toString()));

        if (requestCode == payment_requestcode && data == null)
            return;
        if (requestCode == payment_requestcode && data != null) {
            String message = data.getExtras().getString("message");//得到消息
            String transctionid = data.getExtras().getString("transctionid");//得到支付交易流水号
            String resulturl = data.getExtras().getString("resulturl");//得到结果查询url
            // resultCode : RESULT_OK, RESULT_CANCELED(检查message内容)
            Toast.makeText(this, resultCode == RESULT_OK ? "支付成功" : "支付失败", Toast.LENGTH_LONG).show();
            return;
        }
    }


    public static byte[] httpPost(String url, String param) {
        if (url != null && url.length() != 0) {
            HttpClient httpClient = createHttpClient();
            HttpPost httpPost = new HttpPost(url);

            try {
                httpPost.setEntity(new StringEntity(param, "utf-8"));
                httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
                HttpResponse response;
                if ((response = httpClient.execute(httpPost)).getStatusLine().getStatusCode() != 200) {
                    Log.e("SDK_Sample.Util", "httpGet fail, status code = " + response.getStatusLine().getStatusCode());
                    return null;
                } else {
                    return EntityUtils.toByteArray(response.getEntity());
                }
            } catch (Exception var3) {
                Log.e("SDK_Sample.Util", "httpPost exception, e = " + var3.getMessage());
                var3.printStackTrace();
                return null;
            }
        } else {
            Log.e("SDK_Sample.Util", "httpPost, url is null");
            return null;
        }
    }

    private static HttpClient createHttpClient() {
        try {
            KeyStore keyStore;
            (keyStore = KeyStore.getInstance(KeyStore.getDefaultType())).load((InputStream) null, (char[]) null);
            //SocketFactory socketFactory4;
            //(socketFactory4 = new SocketFactory(keyStore)).setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            BasicHttpParams httpParams;
            HttpProtocolParams.setVersion(httpParams = new BasicHttpParams(), HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(httpParams, "UTF-8");
            SchemeRegistry registry;
            (registry = new SchemeRegistry()).register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            //registry.register(new Scheme("https", socketFactory4, 443));
            ThreadSafeClientConnManager var5 = new ThreadSafeClientConnManager(httpParams, registry);
            return new DefaultHttpClient(var5, httpParams);
        } catch (Exception var3) {
            return new DefaultHttpClient();
        }
    }
}
