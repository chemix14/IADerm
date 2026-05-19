package com.example.iaderm;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

/**
 * OnboardingActivity — 3-step onboarding + consent flow.
 *
 * Steps:
 * 1. "Analiza tu piel con IA" — Introduce the app's purpose
 * 2. "Seguimiento inteligente" — Explain tracking features
 * 3. "Tu privacidad es primero" — Privacy explanation + consent checkboxes
 *
 * The "Comenzar" button only enables when all 3 consent checkboxes are checked.
 * After completion, sets a SharedPreference flag to skip onboarding on next launch.
 */
public class OnboardingActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "iaderm_prefs";
    private static final String KEY_ONBOARDING_COMPLETE = "onboarding_complete";

    private ViewPager2 viewPager;
    private MaterialButton btnNext;
    private TextView btnSkip;
    private LinearLayout dotsContainer;
    private LinearLayout consentContainer;

    private CheckBox checkDisclaimer;
    private CheckBox checkPrivacy;
    private CheckBox checkLocal;

    private static final int TOTAL_PAGES = 3;

    // Onboarding content
    private final int[] titles = {
            R.string.onboarding_title_1,
            R.string.onboarding_title_2,
            R.string.onboarding_title_3
    };

    private final int[] descriptions = {
            R.string.onboarding_desc_1,
            R.string.onboarding_desc_2,
            R.string.onboarding_desc_3
    };

    // Using Android built-in icons as placeholders (replace with Lottie/custom illustrations)
    private final int[] illustrations = {
            android.R.drawable.ic_menu_camera,
            android.R.drawable.ic_menu_recent_history,
            android.R.drawable.ic_lock_lock
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if onboarding already completed
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)) {
            navigateToHome();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        // Bind views
        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);
        dotsContainer = findViewById(R.id.dotsContainer);
        consentContainer = findViewById(R.id.consentContainer);

        checkDisclaimer = findViewById(R.id.checkDisclaimer);
        checkPrivacy = findViewById(R.id.checkPrivacy);
        checkLocal = findViewById(R.id.checkLocal);

        // Set up ViewPager
        viewPager.setAdapter(new OnboardingAdapter());
        setupDots(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                setupDots(position);
                updateButtonState(position);
            }
        });

        // Skip button
        btnSkip.setOnClickListener(v -> viewPager.setCurrentItem(TOTAL_PAGES - 1, true));

        // Next / Start button
        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < TOTAL_PAGES - 1) {
                viewPager.setCurrentItem(current + 1, true);
            } else {
                // Final page: complete onboarding
                completeOnboarding();
            }
        });

        // Consent checkbox listeners (enable button only when all checked)
        View.OnClickListener checkListener = v -> updateConsentButtonState();
        checkDisclaimer.setOnClickListener(checkListener);
        checkPrivacy.setOnClickListener(checkListener);
        checkLocal.setOnClickListener(checkListener);
    }

    private void setupDots(int currentPosition) {
        dotsContainer.removeAllViews();
        for (int i = 0; i < TOTAL_PAGES; i++) {
            TextView dot = new TextView(this);
            dot.setText("●");
            dot.setTextSize(12);
            dot.setPadding(8, 0, 8, 0);

            if (i == currentPosition) {
                dot.setTextColor(getColor(R.color.primary));
            } else {
                dot.setTextColor(getColor(R.color.outline));
            }
            dotsContainer.addView(dot);
        }
    }

    private void updateButtonState(int position) {
        if (position == TOTAL_PAGES - 1) {
            // Last page: show consent, change button text
            consentContainer.setVisibility(View.VISIBLE);
            btnNext.setText(R.string.onboarding_start);
            btnSkip.setVisibility(View.GONE);
            updateConsentButtonState();
        } else {
            consentContainer.setVisibility(View.GONE);
            btnNext.setText(R.string.onboarding_next);
            btnNext.setEnabled(true);
            btnSkip.setVisibility(View.VISIBLE);
        }
    }

    private void updateConsentButtonState() {
        boolean allChecked = checkDisclaimer.isChecked()
                && checkPrivacy.isChecked()
                && checkLocal.isChecked();
        btnNext.setEnabled(allChecked);
        btnNext.setAlpha(allChecked ? 1.0f : 0.5f);
    }

    private void completeOnboarding() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply();
        navigateToHome();
    }

    private void navigateToHome() {
        AppNavigator.openHome(this);
        finish();
    }

    // ═══════════════════════════════════════════════════════
    // ViewPager2 Adapter
    // ═══════════════════════════════════════════════════════

    private class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.PageViewHolder> {

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_onboarding_page, parent, false);
            return new PageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            holder.tvTitle.setText(titles[position]);
            holder.tvDescription.setText(descriptions[position]);
            holder.imgIllustration.setImageResource(illustrations[position]);
            // Tint placeholder icons with primary color
            holder.imgIllustration.setColorFilter(getColor(R.color.primary));
        }

        @Override
        public int getItemCount() {
            return TOTAL_PAGES;
        }

        class PageViewHolder extends RecyclerView.ViewHolder {
            ImageView imgIllustration;
            TextView tvTitle;
            TextView tvDescription;

            PageViewHolder(@NonNull View itemView) {
                super(itemView);
                imgIllustration = itemView.findViewById(R.id.imgIllustration);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvDescription = itemView.findViewById(R.id.tvDescription);
            }
        }
    }
}
