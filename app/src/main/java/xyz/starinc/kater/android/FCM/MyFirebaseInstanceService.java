package xyz.starinc.kater.android.FCM;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
<<<<<<< HEAD:app/src/main/java/xyz/starinc/kater/android/FCM/MyFirebaseInstanceIDService.java

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;

public class MyFirebaseInstanceIDService extends FirebaseMessagingService {
=======
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseInstanceService extends FirebaseMessagingService {
>>>>>>> ef58e07e4f44a40f8d260a7c6e011ab15af83973:app/src/main/java/xyz/starinc/kater/android/FCM/MyFirebaseInstanceService.java

    SharedPreferences preferences;
    private static final String TAG = "MyFirebaseIIDService";

    @Override
<<<<<<< HEAD:app/src/main/java/xyz/starinc/kater/android/FCM/MyFirebaseInstanceIDService.java
    public void onNewToken(@NonNull String refreshedToken) {
=======
    public void onNewToken(String s) {
        super.onNewToken(s);

        //Getting registration token
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
>>>>>>> ef58e07e4f44a40f8d260a7c6e011ab15af83973:app/src/main/java/xyz/starinc/kater/android/FCM/MyFirebaseInstanceService.java

        //Displaying token on logcat
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        storeRegistrationId(this, refreshedToken);
    }
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
    }
    /**
     * Sends the registration ID to your server over HTTP, so it can use FCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     */
    private void storeRegistrationId(Context mContext, String token) {
        preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("fcm_token", token);
        editor.apply();
<<<<<<< HEAD:app/src/main/java/xyz/starinc/kater/android/FCM/MyFirebaseInstanceIDService.java
=======

>>>>>>> ef58e07e4f44a40f8d260a7c6e011ab15af83973:app/src/main/java/xyz/starinc/kater/android/FCM/MyFirebaseInstanceService.java
    }

}
