package interdroid.swan.contextactions;

import android.app.IntentService;
import android.content.Intent;
import android.media.MediaPlayer;

public class SoundService extends IntentService {

	public SoundService() {
		super("Sound Service");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		MediaPlayer.create(this, R.raw.beep).start();
	}

}
