package de.mide.bildvonwebapi;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;


/**
 * Demo-App für das Laden einer Grafik-Datei von einer Web-API.
 * Die Grafik-Datei wird dann in einem ImageView-Element dargestellt.
 * <br><br>
 * Siehe hier für eine Liste alle verfügbaren Bilder:
 * <a href="https://randomuser.me/photos">https://randomuser.me/photos</a>.
 * <br><br>
 *
 * This file is licensed under the terms of the BSD 3-Clause License.
 */
public class MainActivity extends Activity {
	
	public static String TAG4LOGGING = "BildVonWebApiHolen";
	
	/** UI-Elemente zur Eingabe der Bild-Nummer von 0 bis 99. */
	protected EditText _bildNummerEditText = null;

	/** UI-Element um das geladene Bild anzuzeigen. */
	protected ImageView _imageView = null;
	
	/** UI-Element zum Starten des Ladevorgangs. */
	protected Button _startButton = null;
	
	/** UI-element zum Starten des Ladevorgangs mit einer zufällig ausgewählten Bild-Nummer. */
	protected Button _startButtonZufall = null;
	
	/** Nummer für das zu holende Bild, von 0 bis 99, wird vom Nutzer eingegeben. */
	protected int _bildNummer = -1;
	
	/** UI-Element zur Fortschritts-Anzeige während des Lade-Vorgangs. */
	protected ProgressBar _progressBar = null;
	
	/** Zufalls-Generator für Bild-Nummer. */
	protected Random _random = null; 
	
	
	/**
	 * Lifecycle-Methode: Laden der Layout-Datei, holen der Referenzen auf die UI-Elemente.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate( savedInstanceState     );
		setContentView( R.layout.activity_main );
		
		_bildNummerEditText = findViewById( R.id.editTextBildNummer           );
		_imageView          = findViewById( R.id.imageViewElement             );
		_startButton        = findViewById( R.id.starteWebRequestButton       );
		_startButtonZufall  = findViewById( R.id.starteWebRequestButtonZufall );
		_progressBar        = findViewById( R.id.ladeProgressBar              );
		
		//_progressBar.setVisibility(View.INVISIBLE); // Unsichtbar, wird aber für Layout berücksichtigt
		buttonsEinschalten(true);
		
		setzeZufallsBildNr();
	}
		
	
	/**
	 * Event-Handler füür Zufalls-Button.
	 * Ruf zuerst {@link MainActivity#setzeZufallsBildNr()} auf und
	 * dann die Methode {@link MainActivity#onStartButtonBetaetigt(View)}. 
	 */
	public void onStartButtonZufall(View view) {
		
		setzeZufallsBildNr();
		
		onStartButtonBetaetigt(view);
	}
	
	
	/**
	 * Schreibt Zufalls-Zahl in EditText-Element.
	 */
	protected void setzeZufallsBildNr() {
		
		if (_random == null) {

			_random = new Random();
		}
		
		int zufallsZahl = _random.nextInt(100); // Zahl 0..99
		
		_bildNummerEditText.setText(zufallsZahl + "");		
	}	
	
	
	/**
	 * Event-Handler für Button, wird in Layout-Datei dem Button
     * mit Attribut <i>android:onClick</i> zugewiesen.<br><br>
	 *
     * Es wird zunächst die vom Nutzer eingegebene Bild-Nummer
     * ausgelesen und überprüft. Wenn alles damit in Ordnung ist,
     * dann wird ein der Start-Button deaktiviert und der
     * Hintergrund-Thread für den HTTP-Request zum Laden des
     * Bildes gestartet. 
	 */
	public void onStartButtonBetaetigt(View view) {
				
		String bildNummerAlsStr = _bildNummerEditText.getText().toString();
		
		// Prüfen, ob überhaupt eine Bild-Nummer eingegeben wurde
		if (bildNummerAlsStr.length() == 0) {

			Toast.makeText(this,
                           "Bitte eine Bild-Nummer von 0 bis 99 eingeben!",
                           Toast.LENGTH_LONG).show();
			return;
		}
		
		
		// Bild-Nummer in int-Zahl umwandeln
		try {

			_bildNummer = Integer.parseInt(bildNummerAlsStr);
		}
		catch (Exception ex) {

			Toast.makeText(this,
                           "Fehler beim Parsen der Bild-Nummer: " + ex,
                           Toast.LENGTH_LONG).show();
			return;						
		}
				
				
		// Vorbereitungen auf der UI für den Ladevorgang
		buttonsEinschalten(false);
		_imageView.setImageResource(android.R.color.transparent); // http://stackoverflow.com/a/8243184/1364368
		
		
	    // Hintergrund-Thread mit HTTP-Request starten
		MeinHintergrundThread mht = new MeinHintergrundThread();
		mht.start();			
	}
	
	
	/**
	 * Laden der Grafik-Datei via HTTP. 
	 * Diese Methode muss in einem Hintergrund-Thread ausgeführt werden!<br><br>
	 * 
	 * Beispiel-URL: 
	 * <a href="https://api.randomuser.me/portraits/men/75.jpg">https://api.randomuser.me/portraits/men/75.jpg</a>
	 * Es handelt sich um eine Teil-Funktion der Web-API <a href="http://randomuser.me">randomuser.me</a>,
	 * die auch zufällige Datensätze mit Vor-/Nachnamen etc. zurückliefert.
     *
     * @throws Exception Wirft Exception bei Fehler der Internet-Verbindung.
	 */
	protected InputStream holeDatenVonWebAPI() throws Exception {
		
		// Schritt 0: Sicherheits-Überprüfung
		if (_bildNummer < 0 || _bildNummer > 99) {
						
			_startButton.post( new Runnable() {

				@Override
				public void run() {

					Toast.makeText(MainActivity.this,
							       "Interner Fehler: Unzulässige Bild-Nummer: " + _bildNummer,
							        Toast.LENGTH_LONG).show();
					buttonsEinschalten(true);
				}
			});
			
			return null;
		}
		

		// URL erzeugen		
		URL url = new URL("https://api.randomuser.me/portraits/men/" + _bildNummer + ".jpg");
		Log.i(TAG4LOGGING, "URL: " + url);
		
		
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		
		Log.i(TAG4LOGGING, "Response Code von HTTP-Request: " + urlConnection.getResponseCode() + " - " + urlConnection.getResponseMessage() );
		
		return new BufferedInputStream( urlConnection.getInputStream() );
	}
	
	
	/**
	 * Dekodieren der JPEG-Datei und Darstellen im ImageView-Element.
	 * Der Button wird auch wieder aktiviert.
	 * 
	 * @param inputStream InputStream mit Ergebnis des HTTP-Requests.
	 */
	protected void bildDarstellen(InputStream inputStream) throws IOException {

		final Bitmap bitmap = BitmapFactory.decodeStream( inputStream );

		inputStream.close();

		if (bitmap == null) {

			Log.e(TAG4LOGGING, "Leeres Bitmap-Objekt als Ergebnis der Dekodierung erhalten.");

			_startButton.post( new Runnable() {

				@Override
				public void run() {

					Toast.makeText(MainActivity.this,
							       "Fehler: Leeres Bitmap-Objekt als Ergebnis der Dekodierung erhalten.",
							        Toast.LENGTH_LONG).show();
					buttonsEinschalten(true);
				}
			});

		} else {

			Log.i(TAG4LOGGING, "Bitmap dekodiert, Höhe=" + bitmap.getHeight() + ", Breite=" + bitmap.getWidth() );
			// Die Bilder sollten immer die Größe 512x512 haben

			_imageView.post( new Runnable() {

				@Override
				public void run() {

					_imageView.setImageBitmap(bitmap);
					buttonsEinschalten(true);
				}
			});
		}
	}
	
	
	/**
	 * Mit dieser Methode können unmittelbar vor dem Lade-Vorgang
	 * die Buttons deaktiviert werden. In dieser Methode
	 * wird auch die Fortschritts-Anzeige eingeschaltet, während
	 * die Buttons ausgeschaltet sind.
	 * Damit diese Methode auch aus einem Hintergrund-Thread augerufen
	 * werden kann, werden die UI-Zugriff mit der <tt>post()</tt>-Methode
	 * an den Main-Thread durchgereicht. 
	 * 
	 * @param buttonsAktiv Ist <tt>true</tt>, wenn die Buttons aktiv geschaltet
	 *                     werden sollen (also Ladevorgang beendet), <tt>false</tt>
	 *                     zum Abschalten der Buttons.
	 */
	protected void buttonsEinschalten(boolean buttonsAktiv) {
		
		final boolean buttonsAktivFinal = buttonsAktiv; 
		
		_startButton.post(new Runnable() {

			@Override
			public void run() {

				_startButton.setEnabled      (buttonsAktivFinal);
				_startButtonZufall.setEnabled(buttonsAktivFinal);
				
				if (buttonsAktivFinal) {

					_progressBar.setVisibility(View.INVISIBLE); // unsichtbar, aber Platz wird für Layout berücksichtigt

				} else {

					_progressBar.setVisibility(View.VISIBLE);
				}
			}
		});
	}


	/* *************************** */
	/* *** Start innere Klasse *** */
	/* *************************** */	
	
	/**
	 * Zugriff auf Web-API (Internet-Zugriff) wird in
	 * eigenen Thread ausgelagert, damit der Main-Thread
	 * nicht blockiert wird.
	 */
	protected class MeinHintergrundThread extends Thread {

		/**
		 * Der Inhalt in der überschriebenen <i>run()</i>-Methode
		 * wird in einem Hintergrund-Thread ausgeführt.
		 */
		@Override
		public void run() {
			
			try {
				
				InputStream is = holeDatenVonWebAPI();
				
				if (is == null) {
					throw new Exception("Leeren InputStream als Ergebnis von HTTP-Request erhalten.");
				}

				bildDarstellen(is);
				
			}
			catch (Exception ex) {

			    Log.e(TAG4LOGGING, "Exception: " + ex);
			    
				final Exception exception = ex;
				
				_startButton.post( new Runnable() {

					@Override
					public void run() {

						Toast.makeText(MainActivity.this,
                                       "Exception aufgetreten: " + exception,
                                       Toast.LENGTH_LONG).show();
						buttonsEinschalten(true);
					}
				});				
				
			}			
		}
				
	};
	
	/* *************************** */
	/* *** Ende innere Klasse  *** */
	/* *************************** */		

};
