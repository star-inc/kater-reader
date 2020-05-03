package xyz.starinc.exa.kr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import im.delight.android.webview.AdvancedWebView;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements AdvancedWebView.Listener, EasyPermissions.PermissionCallbacks{

    private NetworkStatDetector networkStatDetector;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private webChromeClient mWebChromeClient;
    private IntentFilter intentFilter;
    public AdvancedWebView webView;
    private FrameLayout customViewContainer;
    private View mCustomView;
    private ViewFlipper viewFlipper;
    private ProgressBar mPbar;
    private WebSettings webSettings;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SharedPreferences sharedPreferences;
    boolean isPermissionRequested = false;
    boolean isFirstRun = false;
    private static final String  url = "https://kater.me/";
    private String webURL;
    private String webTitle;
    private int viewFlipperStats = 0;
    public static final int RC_PERMISSIONS = 123;

    static final String[] PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        viewFlipperStats = 0;

        customViewContainer = findViewById(R.id.customViewContainer);

        networkStatDetector = new NetworkStatDetector();
        intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        sharedPreferences = getSharedPreferences("GlobalPreferences", 0);
        isPermissionRequested = sharedPreferences.getBoolean("IsPermissionRequested", false);
        isFirstRun = sharedPreferences.getBoolean("IsFirstRun", false);

        mPbar = findViewById(R.id.loader);
        swipeRefreshLayout = findViewById(R.id.swipeContainer);
        viewFlipper = findViewById(R.id.viewFlipper);
        FloatingActionButton floatingActionButton = findViewById(R.id.fab);
        Button button = findViewById(R.id.button);

        webView = findViewById(R.id.newWeb);
        webView.setListener(this, this);

        webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webView.setThirdPartyCookiesEnabled(true);
        webView.setCookiesEnabled(true);
        webSettings.setAllowFileAccess(true);
        if(isNetworkAvailable(this)){
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        }else{
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        mWebChromeClient = new webChromeClient();
        webView.setWebChromeClient(mWebChromeClient);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            webSettings.setAllowUniversalAccessFromFileURLs(true);
        }else{
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mPbar.setVisibility(View.VISIBLE);
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                mPbar.setVisibility(View.GONE);
                if(url.startsWith("https://kater.me/auth/facebook?code=")){
                    view.loadUrl("https://kater.me");
                }
                super.onPageFinished(view, url);
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                String host = Uri.parse(url).getHost();
                if(url.contains("auth") && url.contains("facebook")){
                    view.loadUrl(url);
                }else if((url.startsWith("http://") || url.startsWith("https://")) && url.contains(".") && host != null && !host.equals("kater.me")){
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    if(Build.VERSION.SDK_INT >= 21){
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    }else{
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    }
                    startActivity(intent);
                }else{
                    view.loadUrl(url);
                }
                return true;
            }
        });
        webView.setOnKeyListener( new View.OnKeyListener() {
            @Override
            public boolean onKey( View v, int keyCode, KeyEvent event ) {
                if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
                return false;
            }
        });
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(isNetworkAvailable(MainActivity.this)){
                    webView.reload();
                }else{
                    Toast.makeText(MainActivity.this, R.string.network_not_available, Toast.LENGTH_SHORT).show();
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        swipeRefreshLayout.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if(webView.getScrollY() == 0){
                    swipeRefreshLayout.setEnabled(true);
                }else{
                    swipeRefreshLayout.setEnabled(false);
                }
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.reload();
                viewFlipperStats = 0;
                viewFlipper.showNext();
            }
        });
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    String s = webView.getUrl();
                    intent.putExtra(Intent.EXTRA_TEXT, s);
                    startActivity(Intent.createChooser(intent, getString(R.string.choose_an_app)));
                } catch(Exception e) {
                    Log.e("error", "Failed To Share");
                }
            }
        });
        floatingActionButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("URL", webView.getUrl());
                if(clipboard != null){
                    clipboard.setPrimaryClip(clip);
                }
                Toast.makeText(MainActivity.this, R.string.link_copied, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        if (savedInstanceState == null) {
            webView.loadUrl(url);
        }
        registerForContextMenu(webView);

        if(!isPermissionRequested){
            requestPermissions();
        }
        if(!isFirstRun){
            showFirstDialog();
        }
    }

    public boolean inCustomView() {
        return (mCustomView != null);
    }
    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo){
        super.onCreateContextMenu(contextMenu, view, contextMenuInfo);

        final WebView.HitTestResult webViewHitTestResult = webView.getHitTestResult();

        if (webViewHitTestResult.getType() == WebView.HitTestResult.IMAGE_TYPE || webViewHitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            contextMenu.setHeaderTitle(R.string.download_image);
            contextMenu.setHeaderIcon(R.drawable.ic_download);

            contextMenu.add(0, 1, 0, R.string.click_to_download).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {

                    String DownloadImageURL = webViewHitTestResult.getExtra();

                    if(URLUtil.isValidUrl(DownloadImageURL)){
                        DownloadManager.Request mRequest = new DownloadManager.Request(Uri.parse(DownloadImageURL));
                        mRequest.allowScanningByMediaScanner();
                        mRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        DownloadManager mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                        if(mDownloadManager != null){
                            mDownloadManager.enqueue(mRequest);
                        }
                        Toast.makeText(MainActivity.this,R.string.download_success,Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(MainActivity.this,R.string.download_failed,Toast.LENGTH_SHORT).show();
                    }
                    return false;
                }
            });
        }else if(webViewHitTestResult.getType() == WebView.HitTestResult.PHONE_TYPE){
            contextMenu.add(0, 1, 0, R.string.click_to_download).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    String phoneNumber = webViewHitTestResult.getExtra();
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNumber, null));
                    startActivity(intent);
                    return true;
                }
            });
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.share_btn) {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            String shareBody = webTitle+":\n"+webURL;
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Kater");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, "Share using"));
        } else if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        this.registerReceiver(networkStatDetector, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
        this.unregisterReceiver(networkStatDetector);
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (inCustomView()) {
            mWebChromeClient.onHideCustomView();
        }
    }
    @Override
    protected void onDestroy() {
        webView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        webView.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public void onBackPressed() {
        if (!webView.canGoBack()) {
            showExitDialog();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState ) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (inCustomView()) {
                mWebChromeClient.onHideCustomView();
                return true;
            }

            if ((mCustomView == null) && webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public void onPageStarted(String url, Bitmap favicon) {
    }

    @Override
    public void onPageFinished(String url) {
		webURL = webView.getUrl();
        webTitle = webView.getTitle();
    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
        webView.loadUrl("about:blank");
        if(viewFlipperStats == 0){
            viewFlipperStats = 1;
            viewFlipper.showNext();
        }
    }

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) {

    }

    @Override
    public void onExternalPageRequest(String url) {

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> list) {
        // Some permissions have been granted
        // ...
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        // Some permissions have been denied
        // ...
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }
    @AfterPermissionGranted(RC_PERMISSIONS)
    private void requestPermissions() {
        if(!EasyPermissions.hasPermissions(this, PERMISSIONS)) {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.perm_needed),
                    RC_PERMISSIONS, PERMISSIONS);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("IsPermissionRequested", true).apply();
            isPermissionRequested = sharedPreferences.getBoolean("IsPermissionRequested", false);
        }
    }
    public static boolean isNetworkAvailable(Context context) {
        if(context == null){
            return false;
        }
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        return true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return true;
                    }  else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){
                        return true;
                    }
                }
            }else{
                try{
                    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    if(activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                        Log.i("update_status", "Network is available : true");
                        return true;
                    }
                }catch(Exception e) {
                    Log.i("update_status", "" + e.getMessage());
                }
            }
        }
        Log.i("update_status","Network is available : FALSE ");
        return false;
    }
    private class webChromeClient extends WebChromeClient{

        private View mVideoProgressView;

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            AdvancedWebView newWebView = new AdvancedWebView(MainActivity.this);
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(newWebView);
            resultMsg.sendToTarget();
            return true;
        }
        @Override
        public void onCloseWindow(WebView window) {
            Log.d("onCloseWindow", "called");
        }

        @Override
        public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {

            // if a view already exists then immediately terminate the new one
            if (mCustomView != null) {
                callback.onCustomViewHidden();
                return;
            }
            mCustomView = view;
            webView.setVisibility(View.GONE);
            customViewContainer.setVisibility(View.VISIBLE);
            customViewContainer.addView(view);
            customViewCallback = callback;
        }

        @Override
        public View getVideoLoadingProgressView() {

            if (mVideoProgressView == null) {
                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                mVideoProgressView = inflater.inflate(R.layout.video_progress, null);
            }
            return mVideoProgressView;
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();    //To change body of overridden methods use File | Settings | File Templates.
            if (mCustomView == null)
                return;

            webView.setVisibility(View.VISIBLE);
            customViewContainer.setVisibility(View.GONE);

            // Hide the custom view.
            mCustomView.setVisibility(View.GONE);

            // Remove the custom view from its container.
            customViewContainer.removeView(mCustomView);
            customViewCallback.onCustomViewHidden();

            mCustomView = null;
        }
    }
    private class NetworkStatDetector extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent){
            if(isNetworkAvailable(context)){
                webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
            }else{
                webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            }
        }
    }
    public void showExitDialog(){
        final ViewGroup nullParent = null;
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View view = layoutInflater.inflate(R.layout.exit_notify, nullParent);

        alertDialog.setView(view);
        alertDialog.setCancelable(false);
        alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.finishAffinity(MainActivity.this);
                dialog.dismiss();
            }
        });
        alertDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }
    protected void showFirstDialog(){

        final ViewGroup nullParent = null;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View view = layoutInflater.inflate(R.layout.first_notify, nullParent);
        CheckBox checkBox = view.findViewById(R.id.checkBox);
        builder.setView(view);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which){
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("IsFirstRun", true);
                editor.apply();
                isFirstRun = sharedPreferences.getBoolean("IsFirstRun", false);
                dialog.dismiss();
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }else{
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });
    }
}
