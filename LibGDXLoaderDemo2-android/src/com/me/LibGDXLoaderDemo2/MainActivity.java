package com.me.LibGDXLoaderDemo2;

import android.content.Context;
import android.os.Bundle;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.enplug.utilities.Utilities;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.Command;

public class MainActivity extends AndroidApplication
{
    private AndroidApplicationConfiguration cfg;
    private final Handler myHandler = new Handler();//Used for running initialize() on the main thread

    //Used with reflection to grab class objects from a child app
    private Class<?> loadedClass;
    private Context childAppCtx;

    //Used to initialize views and objects after reflection
    private View childView;
    private Game childGame;
    private FrameLayout libgdxFrame;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        cfg = new AndroidApplicationConfiguration();
        cfg.useGL20 = false;

        Log.d("MainActivity", "Initializing splash screen");
        libgdxFrame = (FrameLayout) findViewById(R.id.libgdxFrame);
        childView = initializeForView(new DemoGame(), cfg);
        libgdxFrame.addView(childView);

        Log.d("MainActivity", "Downloading Child apk");
        downloadUpdate("EnplugPlayer.apk", "http://enplug.com/packages/player/40/EnplugPlayer.apk");
        //downloadUpdate("star-assault-android.apk", "http://dl.dropboxusercontent.com/sh/xt7xpa15401ru11/BHawa9XIMC/star-assault-android.apk?token_hash=AAGgjL8F9eDn9LhCbMPBOBeztZeRo-4Un923YFoLrUcEyA&dl=1");
    }

    //we call this method only after the child app has been downloaded/installed
    public void startChildApp()
    {
        Log.d("MainActivity", "Dynamically loading child app");
        try
        {
            Log.d("MainActivity", ">>>>>>>>>>Initializing Class Loader");
            childAppCtx = getApplicationContext().createPackageContext("net.obviam.starassault", Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);

            Log.d("MainActivity", "Using class loader to find entrypoint of libgdx app.");

            loadedClass =  Class.forName("net.obviam.starassault.StarAssault", true, childAppCtx.getClassLoader());

            //type testing
            Log.d("MainActivity", "Loaded Class is: " + loadedClass.getConstructor().newInstance().getClass().toString());
            Log.d("MainActivity", "Loaded Super Class is: "+loadedClass.getConstructor().newInstance().getClass().getSuperclass().toString());

            //This doesn't work. Generates a class cast exception.
            childGame = (Game) loadedClass.getConstructor().newInstance();

            Log.d("MainActivity", "Initializing child view");

            childView = initializeForView(childGame, cfg);

            //Not entirely sure if using a handler is still needed
            myHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Log.d("MainActivity", "Attaching view to screen layout");

                        libgdxFrame.removeView(childView);
                        libgdxFrame.addView(childView);
                    }
                    catch (Exception e)
                    {
                        Log.e("MainActivity", e.toString());
                    }
                }
            });

            Log.d("MainActivity", "Startup sequence complete!<<<<<<<<<<<<<<<<");
        }
        catch(Exception ex)
        {
            Log.e("MainActivity", "Error in main activity onCreate()!");
            Log.e("MainActivity", ex.toString());
        }
    }

    //---------------------------------------------------------------------------------------------------
    //Utility Methods borrowed from EnpugUtilities.jar (thanks Justin!) to download and install a child apk from a remote location.
    private void installUpdate(final String filename)
    {
        Utilities.logI("MainActivity", "Status - Starting install for: " + filename + ".");
        Command command = new Command(0, "pm install -r /sdcard/Download/"+filename)
        {
            @Override
            public void output(int id, String line)
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
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void downloadUpdate(final String filename, final String url)
    {
        Utilities.logI("MainActivity", "Starting download of: " + filename + " from " + url);

        Command command = new Command(0, "wget -O /sdcard/Download/"+filename + " " + url)
        {
            @Override
            public void output(int id, String line)
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
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}