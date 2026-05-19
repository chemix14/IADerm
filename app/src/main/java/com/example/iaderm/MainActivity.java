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
    private final Fragment aiChatFragment = new AIChatFragment();
    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment activeFragment = homeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);
        fabAnalyze = findViewById(R.id.fabAnalyze);

        // Setup fragments (all added, only home shown)
        fm.beginTransaction().add(R.id.nav_host_fragment, aiChatFragment, "4").hide(aiChatFragment).commit();
        fm.beginTransaction().add(R.id.nav_host_fragment, triggersFragment, "3").hide(triggersFragment).commit();
        fm.beginTransaction().add(R.id.nav_host_fragment, historyFragment, "2").hide(historyFragment).commit();
        fm.beginTransaction().add(R.id.nav_host_fragment, homeFragment, "1").commit();

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                fm.beginTransaction().hide(activeFragment).show(homeFragment).commit();
                activeFragment = homeFragment;
                fabAnalyze.show();
                return true;
            } else if (itemId == R.id.nav_history) {
                fm.beginTransaction().hide(activeFragment).show(historyFragment).commit();
                activeFragment = historyFragment;
                fabAnalyze.show();
                return true;
            } else if (itemId == R.id.nav_triggers) {
                fm.beginTransaction().hide(activeFragment).show(triggersFragment).commit();
                activeFragment = triggersFragment;
                fabAnalyze.show();
                return true;
            } else if (itemId == R.id.nav_ai_chat) {
                fm.beginTransaction().hide(activeFragment).show(aiChatFragment).commit();
                activeFragment = aiChatFragment;
                fabAnalyze.hide();
                return true;
            } else if (itemId == R.id.nav_capture) {
                AppNavigator.openCapture(this);
                return false;
            }
            return false;
        });

        fabAnalyze.setOnClickListener(v -> AppNavigator.openCapture(this));
        
        handleIntent(getIntent());
    }
    
    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }
    
    private void handleIntent(android.content.Intent intent) {
        if (intent != null && intent.hasExtra("OPEN_TAB")) {
            int tabId = intent.getIntExtra("OPEN_TAB", R.id.nav_home);
            bottomNav.setSelectedItemId(tabId);
        }
    }

    public void switchToTab(int navItemId) {
        bottomNav.setSelectedItemId(navItemId);
    }
}
