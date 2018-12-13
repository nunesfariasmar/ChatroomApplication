package mcc.mcc18.Notifications;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class MyFirebaseService extends FirebaseInstanceIdService {
    private static final String TAG = "FirebaseIDService";
    private static FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private static DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        System.out.println("Refreshed token: " + refreshedToken);
        super.onTokenRefresh();
        sendRegistrationToServer(refreshedToken);
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    public static void sendRegistrationToServer(String token) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user == null) {
            System.out.println("No user created yet");
            return;
        }
        databaseReference.child("Notifications").child(user.getUid()).child("id").setValue(user.getUid());
        databaseReference.child("Notifications").child(user.getUid()).child("token").setValue(token);
    }
}