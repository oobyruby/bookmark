package uws.ac.uk.bookmark;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.MotionEvent;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.graphics.drawable.Drawable;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;

    private TextView createAccountButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_register);

        // initialise firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // connect registration fields
        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);

        createAccountButton = findViewById(R.id.createAccountButton);

        TextView googleButton = findViewById(R.id.googleButton);
        TextView loginLink = findViewById(R.id.loginLink);

        // enable password visibility toggles
        setupPasswordToggle(passwordInput);
        setupPasswordToggle(confirmPasswordInput);

        // handle registration and navigation
        createAccountButton.setOnClickListener(v -> registerUser());

        googleButton.setOnClickListener(v ->
                Toast.makeText(this,
                        "Google registration coming soon",
                        Toast.LENGTH_SHORT).show());

        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });
    }

    // register a new user
    private void registerUser() {

        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        // check all fields have been completed
        if (username.isEmpty() || email.isEmpty()
                || password.isEmpty() || confirmPassword.isEmpty()) {

            Toast.makeText(this,
                    "Please complete all fields",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // validate email address
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

            emailInput.setError("Please enter a valid email address");
            emailInput.requestFocus();
            return;
        }

        // check password length
        if (password.length() < 6) {

            passwordInput.setError("Password must be at least 6 characters");
            passwordInput.requestFocus();
            return;
        }

        // check passwords match
        if (!password.equals(confirmPassword)) {

            confirmPasswordInput.setError("Passwords do not match");
            confirmPasswordInput.requestFocus();
            return;
        }

        createAccountButton.setEnabled(false);
        createAccountButton.setText("creating account...");

        // create account using firebase authentication
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser firebaseUser =
                                firebaseAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            saveUserToFirestore(firebaseUser, username, email);
                        } else {
                            showRegistrationError("Could not retrieve your account.");
                        }

                    } else {

                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed";

                        showRegistrationError(error);
                    }
                });
    }

    // save user profile to firestore
    private void saveUserToFirestore(
            FirebaseUser firebaseUser,
            String username,
            String email) {

        Map<String, Object> user = new HashMap<>();

        user.put("username", username);
        user.put("email", email);
        user.put("bio", null);
        user.put("photoUrl", null);
        user.put("createdAt", FieldValue.serverTimestamp());
        user.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(firebaseUser.getUid())
                .set(user)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(this,
                            "Welcome to Bookmark!",
                            Toast.LENGTH_SHORT).show();

                    // open homepage after successful registration
                    Intent intent = new Intent(
                            RegisterActivity.this,
                            MainActivity.class
                    );

                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(this,
                            "Account created, but profile could not be saved: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG).show();

                    createAccountButton.setEnabled(true);
                    createAccountButton.setText("create account");
                });
    }

    // display registration errors
    private void showRegistrationError(String message) {

        createAccountButton.setEnabled(true);
        createAccountButton.setText("create account");

        Toast.makeText(this,
                message,
                Toast.LENGTH_LONG).show();
    }

    // toggle password visibility
    private void setupPasswordToggle(EditText input) {

        input.setOnTouchListener((v, event) -> {

            if (event.getAction() == MotionEvent.ACTION_UP) {

                Drawable endIcon =
                        input.getCompoundDrawablesRelative()[2];

                if (endIcon != null) {

                    int iconStart = input.getWidth()
                            - input.getPaddingEnd()
                            - endIcon.getBounds().width();

                    if (event.getX() >= iconStart - 12) {

                        boolean isHidden = input.getTransformationMethod()
                                instanceof PasswordTransformationMethod;

                        int cursorPosition = input.getSelectionStart();

                        input.setTransformationMethod(
                                isHidden
                                        ? HideReturnsTransformationMethod.getInstance()
                                        : PasswordTransformationMethod.getInstance()
                        );

                        input.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                R.drawable.ic_register_lock,
                                0,
                                isHidden
                                        ? R.drawable.ic_register_eye
                                        : R.drawable.ic_register_eye_off,
                                0
                        );

                        if (cursorPosition >= 0) {
                            input.setSelection(cursorPosition);
                        }

                        return true;
                    }
                }
            }

            return false;
        });
    }
}
