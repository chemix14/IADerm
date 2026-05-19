package com.example.iaderm;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private ExtendedFloatingActionButton fabAnalyze;
    
    private final Fragment homeFragment = new HomeFragment();
    private final Fragment historyFragment = new HistoryFragment();
    private final Fragment triggersFragment = new TriggersFragment();
    private final Fragment profileFragment = new ProfileFragment();
    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment activeFragment = homeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);
        fabAnalyze = findViewById(R.id.fabAnalyze);

        // Setup fragments (all added, only home shown)
        fm.beginTransaction().add(R.id.nav_host_fragment, profileFragment, "4").hide(profileFragment).commit();
        fm.beginTransaction().add(R.id.nav_host_fragment, triggersFragment, "3").hide(triggersFragment).commit();
        fm.beginTransaction().add(R.id.nav_host_fragment, historyFragment, "2").hide(historyFragment).commit();
        fm.beginTransaction().add(R.id.nav_host_fragment, homeFragment, "1").commit();

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                fm.beginTransaction().hide(activeFragment).show(homeFragment).commit();
                activeFragment = homeFragment;
                return true;
            } else if (itemId == R.id.nav_history) {
                fm.beginTransaction().hide(activeFragment).show(historyFragment).commit();
                activeFragment = historyFragment;
                return true;
            } else if (itemId == R.id.nav_triggers) {
                fm.beginTransaction().hide(activeFragment).show(triggersFragment).commit();
                activeFragment = triggersFragment;
                return true;
            } else if (itemId == R.id.nav_profile) {
                fm.beginTransaction().hide(activeFragment).show(profileFragment).commit();
                activeFragment = profileFragment;
                return true;
            } else if (itemId == R.id.nav_capture) {
                AppNavigator.openCapture(this);
                return false;
            }
            return false;
        });

        fabAnalyze.setOnClickListener(v -> AppNavigator.openCapture(this));
    }

    public void switchToTab(int navItemId) {
        bottomNav.setSelectedItemId(navItemId);
    }
}
