package com.animerec.app.ui;

/**
 * Main activity that hosts all fragments and manages navigation.
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0012\u0010\r\u001a\u00020\u000e2\b\u0010\u000f\u001a\u0004\u0018\u00010\u0010H\u0014J\b\u0010\u0011\u001a\u00020\u0012H\u0016J\u0010\u0010\u0013\u001a\u00020\u000e2\u0006\u0010\u0014\u001a\u00020\u0015H\u0014J\u0012\u0010\u0016\u001a\u00020\u000e2\b\u0010\u0014\u001a\u0004\u0018\u00010\u0015H\u0002J\u0010\u0010\u0017\u001a\u00020\u000e2\u0006\u0010\u0018\u001a\u00020\u0019H\u0016J\b\u0010\u001a\u001a\u00020\u000eH\u0016R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\n\u001a\u00020\u000bX\u0082.\u00a2\u0006\u0004\n\u0002\u0010\f\u00a8\u0006\u001b"}, d2 = {"Lcom/animerec/app/ui/MainActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "<init>", "()V", "navController", "Landroidx/navigation/NavController;", "appBarConfiguration", "Landroidx/navigation/ui/AppBarConfiguration;", "bottomNav", "Lcom/google/android/material/bottomnavigation/BottomNavigationView;", "authViewModel", "error/NonExistentClass", "Lerror/NonExistentClass;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onSupportNavigateUp", "", "onNewIntent", "intent", "Landroid/content/Intent;", "handleIntent", "onTrimMemory", "level", "", "onLowMemory", "app_debug"})
public final class MainActivity extends androidx.appcompat.app.AppCompatActivity {
    private androidx.navigation.NavController navController;
    private androidx.navigation.ui.AppBarConfiguration appBarConfiguration;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNav;
    private error.NonExistentClass authViewModel;
    
    public MainActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    public boolean onSupportNavigateUp() {
        return false;
    }
    
    /**
     * Handle the case where the app is opened with a deep link
     * (for OAuth redirect handling in LoginFragment)
     */
    @java.lang.Override()
    protected void onNewIntent(@org.jetbrains.annotations.NotNull()
    android.content.Intent intent) {
    }
    
    private final void handleIntent(android.content.Intent intent) {
    }
    
    @java.lang.Override()
    public void onTrimMemory(int level) {
    }
    
    @java.lang.Override()
    public void onLowMemory() {
    }
}