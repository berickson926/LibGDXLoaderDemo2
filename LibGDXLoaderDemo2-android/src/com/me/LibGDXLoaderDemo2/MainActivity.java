package com.me.LibGDXLoaderDemo2;

import android.content.Context;
import android.os.Bundle;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.enplug.utilities.Utilities;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.Command;
import dalvik.system.DexClassLoader;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AndroidApplication
{
    //Used for running initialize() on the main thread
    private final Handler myHandler = new Handler();
    private static final long RUNTIME_PERIOD = 10;
    private static final long INITIAL_DELAY = 2;
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean appFlag = true; //debugging mechanism for knowing which app to start/stop. CAREFUL - demo is hardcoded and requires this flag to be true at startup otherwise it will crash
    private boolean splashActive = false;


    //Used to initialize views and objects after reflection
    private AndroidApplicationConfiguration cfg;
    private View childView1, childView2, splashView;
    private ApplicationListener childGame1, childGame2, splash;
    private Screen childScreen;
    private FrameLayout libgdxFrame;
    private Class<?> loadedClass;
    private Context childAppCtx;

    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        cfg = new AndroidApplicationConfiguration();
        cfg.useGL20 = true;

        //Use splash screen for basic setup requirements
        startSplashScreen();

        //Start the thread scheduler to swap between apps afterwards on a fixed time interval
        Log.d("MainActivity", "======================================================================");
        scheduler.scheduleAtFixedRate(
                new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.d("MainActivity", "Running application swap task!");

                        //we will assume that the Calc/calendar apps have been validated after the app launcher surveys all installed apps on the device
                        launchApplications();
                    }
                }, INITIAL_DELAY, RUNTIME_PERIOD, TimeUnit.SECONDS);

        //------------------------------------------------------------------

        //Log.d("MainActivity", "Downloading Child apk");
        //downloadUpdate("EnplugPlayer.apk", "http://enplug.com/packages/player/40/EnplugPlayer.apk");
        //downloadUpdate("star-assault-android.apk", "http://dl.dropboxusercontent.com/sh/xt7xpa15401ru11/BHawa9XIMC/star-assault-android.apk?token_hash=AAGgjL8F9eDn9LhCbMPBOBeztZeRo-4Un923YFoLrUcEyA&dl=1");
        //downloadUpdate("starAssault.apk", "http://dl-web.dropbox.com/get/personalProjects/starAssault.apk?w=AADGmZDev2JNaI2ZIj9-QqQD-erlT8QYlMha2Cw5q3lUig&dl=1");

//        Log.d("MainActivity", "Starting first Child app");
//        startChildApp();
//
//        Log.d("MainActivity", "Starting second child app");
//        startSecondChildApp();


    }

    private void launchApplications()
    {
        //Start the next app in the rotation
        if(appFlag)
        {
            Log.d("MainActivity", "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            Log.d("MainActivity", "Starting PhotoTile App!");

            appFlag = false;

            myHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    startChildApp();
                }
            });
        }
        else
        {
            Log.d("MainActivity", "-----------------------------------------------------------------");
            Log.d("MainActivity", "Starting sample StarGuard app!");

            appFlag = true;

            try
            {
                myHandler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        startSecondChildApp();
                    }
                });

            }
            catch(Exception e)
            {
                Log.e("MainActivity", "Failed to stop webgl demo! "+e);
            }
        }
    }

    public void startSplashScreen()
    {
        Log.d("MainActivity", "Initializing splash screen");
        libgdxFrame = (FrameLayout) findViewById(R.id.libgdxFrame);
        splash = new DemoGame();
        splashView = initializeForView(splash, cfg);
        libgdxFrame.addView(splashView);

        splashActive = true;
    }

    //we call this method only after the child app has been downloaded/installed
    public void startChildApp()
    {
        Log.d("MainActivity", "Dynamically loading child app");
        try
        {
            Log.d("MainActivity", ">>>>>>>>>>Initializing Class Loader");
            String className = "com.enplug.photowall.PhotoWall";
            String packageFullPath = "mnt/sdcard/Download/PhotoWall.apk";
            Context currentContext = getApplicationContext();
            ClassLoader cl = currentContext.getClassLoader();

            Log.d("MainActivity", "Using Dex Class Loader");
            File dexOutputDir = currentContext.getDir("dex", 0);
            DexClassLoader dexLoader = new DexClassLoader(packageFullPath, dexOutputDir.getAbsolutePath(), null, cl);

            Log.d("MainActivity", "Using class loader to find entrypoint of libgdx app.");
            loadedClass = Class.forName(className, true, dexLoader);

            //type testing
            Log.d("MainActivity", "Loaded Class is: " + loadedClass.getConstructor().newInstance().getClass().toString());
            Log.d("MainActivity", "Loaded Super Class is: "+loadedClass.getConstructor().newInstance().getClass().getSuperclass().toString());

            Log.d("MainActivity", "Instantiating child Game from dynamically loaded class.");
            childGame1 = (ApplicationListener) loadedClass.getConstructor().newInstance();

            Log.d("MainActivity", "Starting new game");
            childView1 = initializeForView(childGame1, cfg);

            /*
                Careful! this is all very concrete and trying to remove/pause things that don't exist will result in exceptions.
             */
            Log.d("MainActivity", "Attaching view to screen layout");
            if(splashActive)
            {
                Log.d("MainActivity", "Disabling Splash screen instead of other app");
                //splash.pause();
                splash.dispose();
                libgdxFrame.removeView(splashView);

                splashActive = false;
            }
            else
            {
                //childGame2.pause();
                childGame2.dispose();
                libgdxFrame.removeView(childView2);
            }

            libgdxFrame.addView(childView1);

            Log.d("MainActivity", "Child App 1 Startup sequence complete!<<<<<<<<<<<<<<<<");
        }
        catch(Exception ex)
        {
            Log.e("MainActivity", "Error in main activity startChildApp()!",ex);
        }
    }

    public void startSecondChildApp()
    {

        Log.d("MainActivity", "Dynamically loading child app");
        try
        {
            Log.d("MainActivity", ">>>>>>>>>>Initializing Class Loader");
            final String className = "net.obviam.starassault.StarAssault";
            final String packageFullPath = "mnt/sdcard/Download/star-assault-android.apk";
            final Context currentContext = getApplicationContext();
            final ClassLoader cl = currentContext.getClassLoader();

            Log.d("MainActivity", "Using Dex Class Loader");
            final File dexOutputDir = currentContext.getDir("dex", 0);
            final DexClassLoader dexLoader = new DexClassLoader(packageFullPath, dexOutputDir.getAbsolutePath(), null, cl);

            Log.d("MainActivity", "Using class loader to find entrypoint of libgdx app.");
            loadedClass = Class.forName(className, true, dexLoader);

            //type testing
            Log.d("MainActivity", "Loaded Class is: " + loadedClass.getConstructor().newInstance().getClass().toString());
            Log.d("MainActivity", "Loaded Super Class is: "+loadedClass.getConstructor().newInstance().getClass().getSuperclass().toString());

            Log.d("MainActivity", "Instantiating child Game from dynamically loaded class.");

            childGame2 = (ApplicationListener) loadedClass.getConstructor().newInstance();

            Log.d("MainActivity", "Starting new game");
            childView2 = initializeForView(childGame2, cfg);

            /*
                Careful! this is all very concrete and trying to remove/pause things that don't exist will result in exceptions.
             */
//            Log.d("MainActivity", "Attaching view to screen layout");
//            splash.pause();
//            libgdxFrame.removeView(splashView);
//            libgdxFrame.addView(childView2);

            //If we are using the 1st child app we need to remove that instead of the splash screen
            //childGame1.pause();
            childGame1.dispose();
            libgdxFrame.removeView(childView1);
            libgdxFrame.addView(childView2);

            Log.d("MainActivity", "Child App 2 Startup sequence complete!<<<<<<<<<<<<<<<<");
        }
        catch(Exception ex)
        {
            Log.e("MainActivity", "Error in main activity startChildApp()!",ex);
        }
    }

    //---------------------------------------------------------------------------------------------------
    //Utility Methods borrowed from EnpugUtilities.jar (thanks Justin!) to download and install a child apk from a remote location.
    private void installUpdate(final String filename)
    {
        Utilities.logI("MainActivity", "Status - Starting install for: " + filename + '.');
        final Command command = new Command(0, "pm install -r /sdcard/Download/"+filename)
        {
            @Override
            public void output(final int id, final String line)
            {
                if(line.toLowerCase().contains("success"))
                {
                    startChildApp();
                }
            }
        };
        try
        {
            RootTools.getShell(true).add(command).waitForFinish();
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
    }

    private void downloadUpdate(final String filename, final String url)
    {
        Utilities.logI("MainActivity", "Starting download of: " + filename + " from " + url);

        final Command command = new Command(0, "wget -O /sdcard/Download/"+filename + ' ' + url)
        {
            @Override
            public void output(final int id, final String line)
            {
                if(line.contains("100% |*******************************|"))
                {
                    Log.d("MainActivity", "Download Complete!");
                    installUpdate(filename);
                }
            }
        };
        try
        {
            RootTools.getShell(true).add(command).waitForFinish();
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
    }
}