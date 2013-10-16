package com.me.LibGDXLoaderDemo2;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;

public class DemoGame extends Game
{
    Screen currentScreen;

	@Override
	public void create()
    {
        Gdx.app.log("DemoGame", "Setting new screen! - DemoScreen()");

        if(currentScreen == null)
            currentScreen = new DemoScreen();

        setScreen(currentScreen);
	}

    public void changeScreen(final Screen screen)
    {
        //currentScreen.pause();
        //currentScreen.dispose();

        //set to new screen
        Gdx.app.log("DemoGame", "Setting new screen!"+screen.getClass());
        currentScreen = screen;
        setScreen(currentScreen);
    }
}
