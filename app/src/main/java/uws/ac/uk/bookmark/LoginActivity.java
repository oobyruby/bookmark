package uws.ac.uk.bookmark;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.graphics.drawable.Drawable;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput;
    private EditText passwordInput;
    private TextView loginButton;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);

        TextView googleButton = findViewById(R.id.googleButton);
        TextView registerLink = findViewById(R.id.registerLink);

        setupPasswordToggle(passwordInput);

        loginButton.setOnClickListener(v -> loginUser());

        googleButton.setOnClickListener(v ->
                Toast.makeText(this,
                        "Google login coming soon",
                        Toast.LENGTH_SHORT).show());

        registerLink.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    // log in an existing user
    private void loginUser() {

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,
                    "Please complete all fields",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email address");
            emailInput.requestFocus();
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("logging in...");

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(this,
                                "Welcome back!",
                                Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(
                                LoginActivity.this,
                                MainActivity.class
                        );

                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                        finish();

                    } else {

                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";

                        loginButton.setEnabled(true);
                        loginButton.setText("log in");

                        Toast.makeText(this,
                                error,
                                Toast.LENGTH_LONG).show();
                    }
                });
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

                        // switch between hidden and visible text
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