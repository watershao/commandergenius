// This string is autogenerated by ChangeAppSettings.sh, do not change spaces amount
package org.enigmagame.enigma;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.util.Log;
import java.io.*;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.StatFs;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.lang.String;

class Settings
{
	static String SettingsFileName = "libsdl-settings.cfg";

	static AlertDialog changeConfigAlert = null;
	static Thread changeConfigAlertThread = null;
	static boolean settingsLoaded = false;

	static void Save(final MainActivity p)
	{
		try {
			ObjectOutputStream out = new ObjectOutputStream(p.openFileOutput( SettingsFileName, p.MODE_WORLD_READABLE ));
			out.writeBoolean(Globals.DownloadToSdcard);
			out.writeBoolean(Globals.PhoneHasArrowKeys);
			out.writeBoolean(Globals.PhoneHasTrackball);
			out.writeBoolean(Globals.UseAccelerometerAsArrowKeys);
			out.writeBoolean(Globals.UseTouchscreenKeyboard);
			out.writeInt(Globals.TouchscreenKeyboardSize);
			out.writeInt(Globals.AccelerometerSensitivity);
			out.writeInt(Globals.TrackballDampening);
			out.writeInt(Globals.AudioBufferConfig);
			out.writeInt(Globals.OptionalDataDownload.length);
			for(int i = 0; i < Globals.OptionalDataDownload.length; i++)
				out.writeBoolean(Globals.OptionalDataDownload[i]);
			out.writeInt(Globals.TouchscreenKeyboardTheme);
			out.close();
			settingsLoaded = true;
			
		} catch( FileNotFoundException e ) {
		} catch( SecurityException e ) {
		} catch ( IOException e ) {};
	}

	static void Load( final MainActivity p )
	{
		if(settingsLoaded) // Prevent starting twice
		{
			startDownloader(p);
			return;
		}
		try {
			ObjectInputStream settingsFile = new ObjectInputStream(new FileInputStream( p.getFilesDir().getAbsolutePath() + "/" + SettingsFileName ));
			Globals.DownloadToSdcard = settingsFile.readBoolean();
			Globals.PhoneHasArrowKeys = settingsFile.readBoolean();
			Globals.PhoneHasTrackball = settingsFile.readBoolean();
			Globals.UseAccelerometerAsArrowKeys = settingsFile.readBoolean();
			Globals.UseTouchscreenKeyboard = settingsFile.readBoolean();
			Globals.TouchscreenKeyboardSize = settingsFile.readInt();
			Globals.AccelerometerSensitivity = settingsFile.readInt();
			Globals.TrackballDampening = settingsFile.readInt();
			Globals.AudioBufferConfig = settingsFile.readInt();
			Globals.OptionalDataDownload = new boolean[settingsFile.readInt()];
			for(int i = 0; i < Globals.OptionalDataDownload.length; i++)
				Globals.OptionalDataDownload[i] = settingsFile.readBoolean();
			Globals.TouchscreenKeyboardTheme = settingsFile.readInt();
			
			settingsLoaded = true;
			
			AlertDialog.Builder builder = new AlertDialog.Builder(p);
			builder.setTitle(p.getResources().getString(R.string.device_config));
			builder.setPositiveButton(p.getResources().getString(R.string.device_change_cfg),
					new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item) 
				{
						changeConfigAlert = null;
						dialog.dismiss();
						showDownloadConfig(p);
				}
			});
			/*
			builder.setNegativeButton("Start", new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int item) 
				{
						changeConfigAlert = null;
						dialog.dismiss();
						startDownloader(p);
				}
			});
			*/
			AlertDialog alert = builder.create();
			alert.setOwnerActivity(p);
			changeConfigAlert = alert;

			class Callback implements Runnable
			{
				MainActivity p;
				Callback( MainActivity _p ) { p = _p; }
				public void run()
				{
					try {
						Thread.sleep(2000);
					} catch( InterruptedException e ) {};
					if( changeConfigAlert == null )
						return;
					changeConfigAlert.dismiss();
					startDownloader(p);
				}
			};
			changeConfigAlertThread = new Thread(new Callback(p));
			changeConfigAlertThread.start();

			alert.show();

			return;
			
		} catch( FileNotFoundException e ) {
		} catch( SecurityException e ) {
		} catch ( IOException e ) {};
		
		// This code fails for both of my phones!
		/*
		Configuration c = new Configuration();
		c.setToDefaults();
		
		if( c.navigation == Configuration.NAVIGATION_TRACKBALL || 
			c.navigation == Configuration.NAVIGATION_DPAD ||
			c.navigation == Configuration.NAVIGATION_WHEEL )
		{
			Globals.AppNeedsArrowKeys = false;
		}
		
		System.out.println( "libSDL: Phone keypad type: " + 
				(
				c.navigation == Configuration.NAVIGATION_TRACKBALL ? "Trackball" :
				c.navigation == Configuration.NAVIGATION_DPAD ? "Dpad" :
				c.navigation == Configuration.NAVIGATION_WHEEL ? "Wheel" :
				c.navigation == Configuration.NAVIGATION_NONAV ? "None" :
				"Unknown" ) );
		*/

		showDownloadConfig(p);
	}

	static void showDownloadConfig(final MainActivity p) {

		long freeSdcard = 0;
		long freePhone = 0;
		try {
			StatFs sdcard = new StatFs(Environment.getExternalStorageDirectory().getPath());
			StatFs phone = new StatFs(Environment.getDataDirectory().getPath());
			freeSdcard = (long)sdcard.getAvailableBlocks() * sdcard.getBlockSize() / 1024 / 1024;
			freePhone = (long)phone.getAvailableBlocks() * phone.getBlockSize() / 1024 / 1024;
		}catch(Exception e) {}

		final CharSequence[] items = { p.getResources().getString(R.string.storage_phone, freePhone),
										p.getResources().getString(R.string.storage_sd, freeSdcard) };
		AlertDialog.Builder builder = new AlertDialog.Builder(p);
		String [] downloadFiles = Globals.DataDownloadUrl.split("\\^");
		builder.setTitle(downloadFiles[0].split("[|]")[0]);
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int item) 
			{
				Globals.DownloadToSdcard = (item == 1);

				dialog.dismiss();
				showOptionalDownloadConfig(p);
			}
		});
		AlertDialog alert = builder.create();
		alert.setOwnerActivity(p);
		alert.show();
	};

	static void showOptionalDownloadConfig(final MainActivity p) {

		String [] downloadFiles = Globals.DataDownloadUrl.split("\\^");
		if(downloadFiles.length <= 1)
		{
			Globals.OptionalDataDownload = new boolean[1];
			Globals.OptionalDataDownload[0] = true;
			showKeyboardConfig(p);
			return;
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(p);
		builder.setTitle(p.getResources().getString(R.string.optional_downloads));

		CharSequence[] items = new CharSequence[ downloadFiles.length - 1 ];
		for(int i = 1; i < downloadFiles.length; i++ )
			items[i-1] = new String(downloadFiles[i].split("[|]")[0]);

		if( Globals.OptionalDataDownload == null || Globals.OptionalDataDownload.length != items.length + 1 )
			Globals.OptionalDataDownload = new boolean[downloadFiles.length];
		Globals.OptionalDataDownload[0] = true;

		builder.setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() 
		{
			public void onClick(DialogInterface dialog, int item, boolean isChecked) 
			{
				Globals.OptionalDataDownload[item+1] = isChecked;
			}
		});
		builder.setPositiveButton(p.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int item) 
			{
				dialog.dismiss();
				showKeyboardConfig(p);
			}
		});

		AlertDialog alert = builder.create();
		alert.setOwnerActivity(p);
		alert.show();
	};

	static void showKeyboardConfig(final MainActivity p)
	{
		if( ! Globals.AppNeedsArrowKeys )
		{
			showTrackballConfig(p);
			return;
		}
		
		final CharSequence[] items = { p.getResources().getString(R.string.controls_arrows),
										p.getResources().getString(R.string.controls_trackball),
										p.getResources().getString(R.string.controls_touch) };

		AlertDialog.Builder builder = new AlertDialog.Builder(p);
		builder.setTitle(p.getResources().getString(R.string.controls_question));
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int item) 
			{
				Globals.PhoneHasArrowKeys = (item == 0);
				Globals.PhoneHasTrackball = (item == 1);

				dialog.dismiss();
				showTrackballConfig(p);
			}
		});
		AlertDialog alert = builder.create();
		alert.setOwnerActivity(p);
		alert.show();
	}

	static void showTrackballConfig(final MainActivity p)
	{
		Globals.TrackballDampening = 0;
		if( ! Globals.PhoneHasTrackball )
		{
			showAdditionalInputConfig(p);
			return;
		}
		
		final CharSequence[] items = { p.getResources().getString(R.string.trackball_no_dampening),
										p.getResources().getString(R.string.trackball_fast),
										p.getResources().getString(R.string.trackball_medium),
										p.getResources().getString(R.string.trackball_slow) };

		AlertDialog.Builder builder = new AlertDialog.Builder(p);
		builder.setTitle(p.getResources().getString(R.string.trackball_question));
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int item) 
			{
				Globals.TrackballDampening = item;

				dialog.dismiss();
				showAdditionalInputConfig(p);
			}
		});
		AlertDialog alert = builder.create();
		alert.setOwnerActivity(p);
		alert.show();
	}
	
	
	static void showAdditionalInputConfig(final MainActivity p)
	{
		if( ! Globals.AppNeedsArrowKeys )
		{
			showAccelerometerConfig(p);
			return;
		}
		final CharSequence[] items = {
			p.getResources().getString(R.string.controls_screenkb),
			p.getResources().getString(R.string.controls_accelnav),
		};

		Globals.UseTouchscreenKeyboard = false;
		Globals.UseAccelerometerAsArrowKeys = false;

		AlertDialog.Builder builder = new AlertDialog.Builder(p);
		builder.setTitle(p.getResources().getString(R.string.controls_additional));
		builder.setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() 
		{
			public void onClick(DialogInterface dialog, int item, boolean isChecked) 
			{
				if( item == 0 )
					Globals.UseTouchscreenKeyboard = isChecked;
				if( item == 1 )
				Globals.UseAccelerometerAsArrowKeys = isChecked;
			}
		});
		builder.setPositiveButton(p.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int item) 
			{
				dialog.dismiss();
				showAccelerometerConfig(p);
			}
		});

		AlertDialog alert = builder.create();
		alert.setOwnerActivity(p);
		alert.show();
	}

	static void showAccelerometerConfig(final MainActivity p)
	{
		Globals.AccelerometerSensitivity = 0;
		if( ! Globals.UseAccelerometerAsArrowKeys )
		{
			showScreenKeyboardConfig(p);
			return;
		}
		
		final CharSequence[] items = { p.getResources().getString(R.string.accel_fast),
										p.getResources().getString(R.string.accel_medium),
										p.getResources().getString(R.string.accel_slow) };

		AlertDialog.Builder builder = new AlertDialog.Builder(p);
		builder.setTitle(R.string.accel_question);
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int item) 
			{
				Globals.AccelerometerSensitivity = item;

				dialog.dismiss();
				showScreenKeyboardConfig(p);
			}
		});
		AlertDialog alert = builder.create();
		alert.setOwnerActivity(p);
		alert.show();
	}

	static void showScreenKeyboardConfig(final MainActivity p)
	{
		Globals.TouchscreenKeyboardSize = 0;
		if( ! Globals.UseTouchscreenKeyboard )
		{
			showScreenKeyboardThemeConfig(p);
			return;
		}
		
		final CharSequence[] items = {	p.getResources().getString(R.string.controls_screenkb_large),
										p.getResources().getString(R.string.controls_screenkb_medium),
										p.getResources().getString(R.string.controls_screenkb_small),
										p.getResources().getString(R.string.controls_screenkb_tiny) };

		AlertDialog.Builder builder = new AlertDialog.Builder(p);
		builder.setTitle(p.getResources().getString(R.string.controls_screenkb_size));
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int item) 
			{
				Globals.TouchscreenKeyboardSize = item;

				dialog.dismiss();
				showScreenKeyboardThemeConfig(p);
			}
		});
		AlertDialog alert = builder.create();
		alert.setOwnerActivity(p);
		alert.show();
	}

	static void showScreenKeyboardThemeConfig(final MainActivity p)
	{
		Globals.TouchscreenKeyboardTheme = 0;
		if( ! Globals.UseTouchscreenKeyboard )
		{
			showAudioConfig(p);
			return;
		}
		
		final CharSequence[] items = {
			p.getResources().getString(R.string.controls_screenkb_by, "Ultimate Droid", "Sean Stieber"),
			p.getResources().getString(R.string.controls_screenkb_by, "Ugly Arrows", "pelya")
			};

		AlertDialog.Builder builder = new AlertDialog.Builder(p);
		builder.setTitle(p.getResources().getString(R.string.controls_screenkb_theme));
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int item) 
			{
				if( item == 0 )
					Globals.TouchscreenKeyboardTheme = 1;
				if( item == 1 )
					Globals.TouchscreenKeyboardTheme = 0;

				dialog.dismiss();
				showAudioConfig(p);
			}
		});
		AlertDialog alert = builder.create();
		alert.setOwnerActivity(p);
		alert.show();
	}
	
	static void showAudioConfig(final MainActivity p)
	{
		final CharSequence[] items = {	p.getResources().getString(R.string.audiobuf_verysmall),
										p.getResources().getString(R.string.audiobuf_small),
										p.getResources().getString(R.string.audiobuf_medium),
										p.getResources().getString(R.string.audiobuf_large) };

		AlertDialog.Builder builder = new AlertDialog.Builder(p);
		builder.setTitle(R.string.audiobuf_question);
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int item) 
			{
				Globals.AudioBufferConfig = item;
				dialog.dismiss();
				Save(p);
				startDownloader(p);
			}
		});
		AlertDialog alert = builder.create();
		alert.setOwnerActivity(p);
		alert.show();
	}

	static void Apply(Activity p)
	{
		nativeIsSdcardUsed( Globals.DownloadToSdcard ? 1 : 0 );
		
		if( Globals.PhoneHasTrackball )
			nativeSetTrackballUsed();
		if( Globals.AppUsesMouse )
			nativeSetMouseUsed();
		if( Globals.AppUsesJoystick && !Globals.UseAccelerometerAsArrowKeys )
			nativeSetJoystickUsed();
		if( Globals.AppUsesMultitouch )
			nativeSetMultitouchUsed();
		nativeSetAccelerometerSensitivity(Globals.AccelerometerSensitivity);
		nativeSetTrackballDampening(Globals.TrackballDampening);
		if( Globals.UseTouchscreenKeyboard )
		{
			nativeSetTouchscreenKeyboardUsed();
			nativeSetupScreenKeyboard(	Globals.TouchscreenKeyboardSize, 
										Globals.TouchscreenKeyboardTheme,
										Globals.AppTouchscreenKeyboardKeysAmount, 
										Globals.AppTouchscreenKeyboardKeysAmountAutoFire);
		}
		SetupTouchscreenKeyboardGraphics(p);
		String lang = new String(Locale.getDefault().getLanguage());
		if( Locale.getDefault().getCountry().length() > 0 )
			lang = lang + "_" + Locale.getDefault().getCountry();
		System.out.println( "libSDL: setting envvar LANG to '" + lang + "'");
		nativeSetEnv( "LANG", lang );
		// TODO: get current user name and set envvar USER, the API is not availalbe on Android 1.6 so I don't bother with this
	}

	static byte [] loadRaw(Activity p,int res)
	{
		byte [] buf = new byte[65536 * 2];
		byte [] a = new byte[0];
		try{
			InputStream is = new GZIPInputStream(p.getResources().openRawResource(res));
			int readed = 0;
			while( (readed = is.read(buf)) >= 0 )
			{
				byte [] b = new byte [a.length + readed];
				System.arraycopy(a, 0, b, 0, a.length);
				System.arraycopy(buf, 0, b, a.length, readed);
				a = b;
			}
		} catch(Exception e) {};
		return a;
	}
	
	static void SetupTouchscreenKeyboardGraphics(Activity p)
	{
		if( Globals.UseTouchscreenKeyboard )
		{
			if( Globals.TouchscreenKeyboardTheme == 1 )
			{
				nativeSetupScreenKeyboardButtons(loadRaw(p, R.raw.ultimatedroid));
			}
		}
	}
	
	static void startDownloader(MainActivity p)
	{
		class Callback implements Runnable
		{
			public MainActivity Parent;
			public void run()
			{
				Parent.startDownloader();
			}
		}
		Callback cb = new Callback();
		cb.Parent = p;
		p.runOnUiThread(cb);
	};
	

	private static native void nativeIsSdcardUsed(int flag);
	private static native void nativeSetTrackballUsed();
	private static native void nativeSetTrackballDampening(int value);
	private static native void nativeSetAccelerometerSensitivity(int value);
	private static native void nativeSetMouseUsed();
	private static native void nativeSetJoystickUsed();
	private static native void nativeSetMultitouchUsed();
	private static native void nativeSetTouchscreenKeyboardUsed();
	private static native void nativeSetupScreenKeyboard(int size, int theme, int nbuttons, int nbuttonsAutoFire);
	private static native void nativeSetupScreenKeyboardButtons(byte[] img);
	public static native void nativeSetEnv(final String name, final String value);
}

