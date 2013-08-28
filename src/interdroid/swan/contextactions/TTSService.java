package interdroid.swan.contextactions;

import android.app.IntentService;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

public class TTSService extends IntentService {

	public TTSService() {
		super("TTS Service");
	}

	private TextToSpeech tts = null;

	@Override
	protected void onHandleIntent(final Intent intent) {
		tts = new TextToSpeech(this, new OnInitListener() {

			@Override
			public void onInit(int status) {
				synchronized (tts) {
					tts.notify();
				}
			}
		});
		synchronized (tts) {
			try {
				tts.wait();
				tts.speak("stap", TextToSpeech.QUEUE_FLUSH, null);
				tts.shutdown();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
