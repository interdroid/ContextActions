package interdroid.swan.contextactions;

import android.app.IntentService;
import android.content.Intent;
import android.media.AudioManager;

public class RingerMuteService extends IntentService {
	public RingerMuteService() {
		super("RingerService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
		am.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
	}

}
