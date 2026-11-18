package uws.ac.uk.bookmark;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class GoogleAuthHelper {

    // start google authentication from login or registration
    public static void signIn(Activity activity) {

        CredentialManager credentialManager =
                CredentialManager.create(activity);

        GetGoogleIdOption googleOption =
                new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(
                                activity.getString(R.string.default_web_client_id))
                        .setAutoSelectEnabled(false)
                        .build();

        GetCredentialRequest request =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(googleOption)
                        .build();

        // request a google account credential
        androidx.credentials.CredentialManagerCallback<
                GetCredentialResponse,
                androidx.credentials.exceptions.GetCredentialException> callback =
                new androidx.credentials.CredentialManagerCallback<
                        GetCredentialResponse,
                        androidx.credentials.exceptions.GetCredentialException>() {

                    @Override
                    public void onResult(GetCredentialResponse response) {
                        Credential credential = response.getCredential();

                        if (credential instanceof CustomCredential
                                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                .equals(credential.getType())) {

                            try {
                                GoogleIdTokenCredential googleCredential =
                                        GoogleIdTokenCredential.createFrom(
                                                ((CustomCredential) credential).getData());

                                authenticateWithFirebase(
                                        activity,
                                        googleCredential.getIdToken());

                            } catch (Exception e) {
                                showError(activity,
                                        "Could not read Google account: " + e.getMessage());
                            }

                        } else {
                            showError(activity, "Unsupported Google credential");
                        }
                    }

                    @Override
                    public void onError(
                            androidx.credentials.exceptions.GetCredentialException e) {

                        // closing the account selector is not an authentication failure
                        if (!(e instanceof
                                androidx.credentials.exceptions.GetCredentialCancellationException)) {
                            showError(activity,
                                    "Google sign-in failed: " + e.getMessage());
                        }
                    }
                };

        credentialManager.getCredentialAsync(
                activity,
                request,
                null,
                androidx.core.content.ContextCompat.getMainExecutor(activity),
                callback
        );
    }

    // exchange the google token for a firebase session
    private static void authenticateWithFirebase(
            Activity activity,
            String idToken) {

        FirebaseAuth auth = FirebaseAuth.getInstance();

        AuthCredential credential =
                GoogleAuthProvider.getCredential(idToken, null);

        auth.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {

                    if (!task.isSuccessful()) {
                        showError(activity,
                                task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Firebase authentication failed");
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();

                    if (user == null) {
                        showError(activity, "Could not retrieve Google account");
                        return;
                    }

                    saveGoogleProfile(activity, user);
                });
    }

    // create a firestore profile only when one does not exist
    private static void saveGoogleProfile(
            Activity activity,
            FirebaseUser user) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {
                        openHome(activity);
                        return;
                    }

                    String username = user.getDisplayName();

                    if (username == null || username.trim().isEmpty()) {
                        username = user.getEmail() != null
                                ? user.getEmail().split("@")[0]
                                : "reader";
                    }

                    Map<String, Object> profile = new HashMap<>();
                    profile.put("username", username);
                    profile.put("email", user.getEmail());
                    profile.put("bio", null);
                    profile.put("photoUrl", user.getPhotoUrl() != null
                            ? user.getPhotoUrl().toString()
                            : null);
                    profile.put("createdAt", FieldValue.serverTimestamp());
                    profile.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection("users")
                            .document(user.getUid())
                            .set(profile)
                            .addOnSuccessListener(unused -> openHome(activity))
                            .addOnFailureListener(e ->
                                    showError(activity,
                                            "Could not save profile: " + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        showError(activity,
                                "Could not check profile: " + e.getMessage()));
    }

    // open the homepage after successful authentication
    private static void openHome(Activity activity) {

        Toast.makeText(activity,
                "Welcome to Bookmark!",
                Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(activity, MainActivity.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        activity.startActivity(intent);
        activity.finish();
    }

    private static void showError(Activity activity, String message) {
        Toast.makeText(activity,
                message,
                Toast.LENGTH_LONG).show();
    }

    // clear saved credential selection after logout
    public static void clearCredentialState(Activity activity) {

        CredentialManager manager = CredentialManager.create(activity);

        manager.clearCredentialStateAsync(
                new ClearCredentialStateRequest(),
                null,
                androidx.core.content.ContextCompat.getMainExecutor(activity),
                new androidx.credentials.CredentialManagerCallback<
                        Void,
                        androidx.credentials.exceptions.ClearCredentialException>() {

                    @Override
                    public void onResult(Void result) {
                        // credential state cleared
                    }

                    @Override
                    public void onError(
                            androidx.credentials.exceptions.ClearCredentialException e) {
                        android.util.Log.w(
                                "BOOKMARK_AUTH",
                                "Could not clear credential state", e);
                    }
                }
        );
    }
}
