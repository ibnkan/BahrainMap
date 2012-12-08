package com.ibnkan.bahmap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.gson.Gson;

@SuppressWarnings("deprecation")
public class MainActivity extends MapActivity implements OnItemSelectedListener {

	private MapView mapView;
	private MapController mapController;
	private List<Overlay> mapOverlays;
	private MarkersOverlay mapMarkers;
	private GeoPoint bahrainCoordinates;
	private Drawable markerImage;
	private Drawable markerSelected;
	private Drawable markerOld;
	private ArrayList<LocationsList> locationsList;
	private ArrayList<Results> tweets;
	private boolean tweetsProcessed;
	private boolean translateTweets;
	private String keyword;
	private String hashtag;
	private int flipflop;
	private int markerIndex;
	private int markerRecent;
	private int minLat = Integer.MAX_VALUE;
	private int maxLat = Integer.MIN_VALUE;
	private int minLng = Integer.MAX_VALUE;
	private int maxLng = Integer.MIN_VALUE;
	private SlidingDrawer slidingDrawer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initialiseMap();
		initialiseSpinner();
		initialiseDrawer();
		initialiseLocationsList();

		initialiseTweetVars();
		new DownloadTweetsTask().execute(searchURL(keyword, hashtag));
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			return true;
		case R.id.menu_latest_news:
			Intent newsIntent = new Intent(this, NewsActivity.class);
			startActivity(newsIntent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}

	public void initialiseMap() {
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(false);

		mapController = mapView.getController();
		bahrainCoordinates = new GeoPoint(26066700, 50557700);
		mapController.setCenter(bahrainCoordinates);
		mapController.zoomToSpan(26326528 - 25579840, 50822864 - 50378151);

		mapOverlays = mapView.getOverlays();

		markerImage = getApplicationContext().getResources().getDrawable(
				R.drawable.map_marker);
		mapMarkers = new MarkersOverlay(markerImage, getApplicationContext());

		markerSelected = getResources().getDrawable(
				R.drawable.map_marker_selected);
		markerSelected.setBounds(-markerSelected.getIntrinsicWidth() / 2,
				-markerSelected.getIntrinsicHeight(),
				markerSelected.getIntrinsicWidth() / 2, 0);

		markerOld = getResources().getDrawable(R.drawable.map_marker_old);
		markerOld.setBounds(-markerOld.getIntrinsicWidth() / 2,
				-markerOld.getIntrinsicHeight(),
				markerOld.getIntrinsicWidth() / 2, 0);

		markerIndex = 0;
		markerRecent = -1;

	}

	public void initialiseDrawer() {
		final ImageButton handle = (ImageButton) findViewById(R.id.handle);
		slidingDrawer = (SlidingDrawer) findViewById(R.id.slidingDrawer);
		slidingDrawer.setOnDrawerCloseListener(new OnDrawerCloseListener() {

			public void onDrawerClosed() {
				handle.setImageDrawable(getResources().getDrawable(
						R.drawable.ic_action_arrowup));
			}

		});
		slidingDrawer.setOnDrawerOpenListener(new OnDrawerOpenListener() {

			public void onDrawerOpened() {
				handle.setImageDrawable(getResources().getDrawable(
						R.drawable.ic_action_arrowdown));
			}

		});

		ImageButton nextMarkerLong = (ImageButton) findViewById(R.id.nextMarker);
		ImageButton previousMarkerLong = (ImageButton) findViewById(R.id.previousMarker);

		nextMarkerLong.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				markerRecent = markerIndex;
				markerIndex = mapMarkers.size() - 1;
				showTweetMarker(markerIndex, true);
				return true;
			}
		});

		previousMarkerLong.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				markerRecent = markerIndex;
				markerIndex = 0;
				showTweetMarker(markerIndex, true);
				return true;
			}
		});

		final ImageButton translateButton = (ImageButton) findViewById(R.id.translate);
		translateButton.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				if (translateTweets && tweetsProcessed == true) {
					translateTweets = false;
					translateButton.setImageDrawable(getResources()
							.getDrawable(R.drawable.ic_action_translate));
					translateButton.setTag("OFF");
					showTweetMarker(markerIndex, false);
					toastMsg("Translation toggled OFF");

				} else if (tweetsProcessed == true) {
					translateTweets = true;
					translateButton.setImageDrawable(getResources()
							.getDrawable(R.drawable.ic_action_translate_not));
					translateButton.setTag("ON");
					TextView tweetText = (TextView) findViewById(R.id.tweetText);
					new DownloadTranslationTask().execute(tweetText.getText()
							.toString());
					toastMsg("Translation toggled ON");
				}
				return true;
			}
		});

	}

	public void initialiseSpinner() {
		Spinner spinner = (Spinner) findViewById(R.id.spinner);
		spinner.setOnItemSelectedListener(this);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter
				.createFromResource(this, R.array.spinnerList,
						android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
	}

	public void initialiseLocationsList() {

		locationsList = new ArrayList<LocationsList>();
		try {
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(getResources().openRawResource(
							R.raw.locations)));
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				String[] parts = line.split(",");

				LocationsList location = new LocationsList(parts[0].replaceAll(
						"^ال", "(ال)*").replaceAll("\\s", "(\\\\s)*"),
						Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));

				locationsList.add(location);

			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void initialiseTweetVars() {
		tweets = new ArrayList<Results>();
		tweetsProcessed = false;
		translateTweets = false;
		keyword = "(مسيرة OR مسيرات OR مظاهرة OR مظاهرات OR اعتصام)";
		hashtag = "(#Bahrain OR #البحرين) lang:ar";
		flipflop = 1;
		ImageButton translateButton = (ImageButton) findViewById(R.id.translate);
		translateButton.setTag("OFF");
	}

	public void toastMsg(CharSequence msg) {
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}

	public void showTweetMarker(int index, boolean animateTo) {

		TweetMarker tm = mapMarkers.getItem(index);

		TextView userName = (TextView) findViewById(R.id.userName);
		TextView tweetText = (TextView) findViewById(R.id.tweetText);
		TextView durationSince = (TextView) findViewById(R.id.durationSince);
		TextView timeStamp = (TextView) findViewById(R.id.timeStamp);
		ImageButton userImage = (ImageButton) findViewById(R.id.userImage);
		TextView markerCounter = (TextView) findViewById(R.id.markerCounter);
		TextView locationName = (TextView) findViewById(R.id.location_name);

		markerCounter.setText(String.valueOf(markerIndex + 1) + "/"
				+ mapMarkers.size() + " | ");
		locationName.setText(mapMarkers.getItem(markerIndex).getTitle());

		userName.setText("@" + tm.from_user);
		String cleanedText = tm.text.replaceAll("(\n|(@|#)\\w+:*|[\"“”])", "")
				.replaceAll("^[\\s\\W\\d]+", "");

		tweetText.setText(cleanedText);
		tweetText.setTextSize((cleanedText.length() > 110) ? 15 : 17);

		timeStamp.setText(new SimpleDateFormat("E, d MMM @ h:mm aa", Locale.US)
				.format(tm.createdAt));
		durationSince.setText(estimateDurationSince(tm.createdAt));

		new DownloadImageTask(userImage).execute(tm.profile_image_url);

		if (markerRecent >= 0) {
			mapMarkers
					.getItem(markerRecent)
					.setMarker(
							isOldTweet(mapMarkers.getItem(markerRecent).createdAt) ? markerOld
									: markerImage);
		}
		tm.setMarker(markerSelected);

		if (animateTo == true) {
			mapController.animateTo(tm.getPoint());
		}

		mapMarkers.setFocus(tm);
		mapView.postInvalidate();

		if (translateTweets) {
			TextView text = (TextView) findViewById(R.id.tweetText);
			new DownloadTranslationTask().execute(text.getText().toString());
			ImageButton translateButton = (ImageButton) findViewById(R.id.translate);
			translateButton.setImageDrawable(getResources().getDrawable(
					R.drawable.ic_action_translate_not));
			translateButton.setTag("ON");
		} else {
			ImageButton translateButton = (ImageButton) findViewById(R.id.translate);
			translateButton.setImageDrawable(getResources().getDrawable(
					R.drawable.ic_action_translate));
			translateButton.setTag("OFF");
		}

	}

	public String searchURL(String key, String hash) {
		try {
			return "http://search.twitter.com/search.json?q=exclude%3Aretweets%20"
					+ URLEncoder.encode(hash + " " + key, "UTF-8")
					+ "&result_type=recent&rpp=100";
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean isOldTweet(Date createdAt) {

		double second = 1000.0;
		double minute = 60.0 * second;
		double hour = 60.0 * minute;

		double age = new Date().getTime() - createdAt.getTime();

		return (age > 5 * hour) ? true : false;
	}

	public String estimateDurationSince(Date createdAt) {

		double second = 1000.0;
		double minute = 60.0 * second;
		double hour = 60.0 * minute;
		double day = 24.0 * hour;

		double age = new Date().getTime() - createdAt.getTime();

		if (age < 30 * second) {
			return getResources().getString(R.string.just_now);

		}

		if (age < 1.5 * minute) {
			return getResources().getString(R.string.one_minute_ago);
		}

		if (age < 2.5 * minute) {
			return getResources().getString(R.string.two_minutes_ago);
		}

		if (age < 10.5 * minute) {
			return getResources().getString(R.string.before) + " "
					+ (int) Math.round((age / minute)) + " "
					+ getResources().getString(R.string.minutes);
		}

		if (age < 55 * minute) {
			return getResources().getString(R.string.before) + " "
					+ (int) Math.round((age / minute)) + " "
					+ getResources().getString(R.string.minute);
		}

		if (age < 1.3 * hour) {
			return getResources().getString(R.string.an_hour_ago);
		}

		if (age < 1.7 * hour) {
			return getResources().getString(R.string.an_hour_half_ago);
		}

		if (age < 2.5 * hour) {
			return getResources().getString(R.string.two_hours_ago);
		}

		if (age < 11 * hour) {
			return getResources().getString(R.string.before) + " "
					+ (int) Math.round((age / hour)) + " "
					+ getResources().getString(R.string.hours);
		}

		if (age < 24 * hour) {
			return getResources().getString(R.string.before) + " "
					+ (int) Math.round((age / hour)) + " "
					+ getResources().getString(R.string.hour);
		}

		if (age < 2 * day) {
			return getResources().getString(R.string.yesterday);
		}

		return getResources().getString(R.string.old_tweet);

	}

	public int randomNudge() {
		flipflop *= -1;
		int randomnudge = (int) (800 * Math.random() * flipflop);
		return randomnudge;
	}

	public boolean smallMarkersArea() {
		if (mapMarkers.getLatSpanE6() < 20000
				&& mapMarkers.getLonSpanE6() < 20000) {
			return true;
		} else {
			return false;
		}

	}

	public void refreshTweetMarkers() {
		if (slidingDrawer.isOpened()) {
			slidingDrawer.animateClose();
		}
		mapOverlays.clear();
		mapMarkers.clear();
		mapController.setCenter(bahrainCoordinates);
		mapController.zoomToSpan(26326528 - 25579840, 50822864 - 50378151);
		mapView.invalidate();
		markerIndex = 0;
		markerRecent = -1;
		tweets.clear();
		tweetsProcessed = false;
		TextView markerCounter = (TextView) findViewById(R.id.markerCounter);
		TextView locationName = (TextView) findViewById(R.id.location_name);
		markerCounter.setText("---/--- |");
		locationName.setText("---");
		new DownloadTweetsTask().execute(searchURL(keyword, hashtag));
	}

	public void refreshInProgress(boolean isRefreshing) {
		ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar_refresh);
		ImageButton refreshButton = (ImageButton) findViewById(R.id.refreshButton);
		if (isRefreshing) {
			progressBar.setVisibility(View.VISIBLE);
			refreshButton.setVisibility(View.INVISIBLE);
		} else {
			refreshButton.setVisibility(View.VISIBLE);
			progressBar.setVisibility(View.GONE);
		}
	}

	public void onRefreshClick(View v) {
		refreshTweetMarkers();

	}

	public void onSearchClick(View v) {
		LinearLayout searchBar = (LinearLayout) findViewById(R.id.searchBar);
		final EditText searchInput = (EditText) findViewById(R.id.searchInput);
		final InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

		if (searchBar.getVisibility() == View.VISIBLE
				&& tweetsProcessed == true) {
			Editable searchKey = searchInput.getText();
			keyword = searchKey.toString();
			if (keyword.contains("#")) {
				hashtag = "";
			} else {
				hashtag = "(#Bahrain OR #البحرين)";

			}

			keyboard.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
			refreshTweetMarkers();

		} else {
			LinearLayout spinnerBar = (LinearLayout) findViewById(R.id.spinnerBar);
			spinnerBar.setVisibility(View.GONE);
			searchBar.setVisibility(View.VISIBLE);
			keyboard.toggleSoftInput(0, 0);

			searchInput.setOnEditorActionListener(new OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId,
						KeyEvent event) {

					if (actionId == EditorInfo.IME_ACTION_SEARCH
							&& tweetsProcessed == true) {
						Editable searchKey = searchInput.getText();
						keyword = searchKey.toString();
						if (keyword.contains("#")) {
							hashtag = "";
						} else {
							hashtag = "(#Bahrain OR #البحرين)";

						}
						keyboard.hideSoftInputFromWindow(
								searchInput.getWindowToken(), 0);
						refreshTweetMarkers();
						return true;
					}
					return false;
				}
			});
		}

	}

	public void onExitSearchClick(View v) {
		LinearLayout searchBar = (LinearLayout) findViewById(R.id.searchBar);
		if (searchBar.getVisibility() == View.VISIBLE) {
			LinearLayout spinnerBar = (LinearLayout) findViewById(R.id.spinnerBar);
			spinnerBar.setVisibility(View.VISIBLE);
			searchBar.setVisibility(View.GONE);
		}
	}

	public void onUserImageClick(View v) {
		Uri uri = Uri.parse("https://twitter.com/"
				+ mapMarkers.getItem(markerIndex).from_user + "/status/"
				+ mapMarkers.getItem(markerIndex).id_str);
		startActivity(new Intent(Intent.ACTION_VIEW, uri));
	}

	public void onPreviousMarkerClick(View v) {
		if (markerIndex > 0) {
			markerRecent = markerIndex;
			markerIndex -= 1;
		}
		showTweetMarker(markerIndex, true);

	}

	public void onNextMarkerClick(View v) {
		if (markerIndex < mapMarkers.size() - 1) {
			markerRecent = markerIndex;
			markerIndex += 1;
		}
		showTweetMarker(markerIndex, true);
	}

	public void onZoomOutClick(View v) {
		mapController.zoomOut();
	}

	public void onZoomInClick(View v) {
		mapController.zoomIn();
	}

	public void onTranslateClick(View v) {
		ImageButton translateButton = (ImageButton) findViewById(R.id.translate);
		if (translateButton.getTag() == "ON" && tweetsProcessed == true) {
			translateButton.setImageDrawable(getResources().getDrawable(
					R.drawable.ic_action_translate));
			translateButton.setTag("OFF");

			showTweetMarker(markerIndex, false);

		} else if (translateButton.getTag() == "OFF" && tweetsProcessed == true) {
			translateButton.setImageDrawable(getResources().getDrawable(
					R.drawable.ic_action_translate_not));
			translateButton.setTag("ON");
			TextView tweetText = (TextView) findViewById(R.id.tweetText);
			new DownloadTranslationTask().execute(tweetText.getText()
					.toString());
		}

	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);

		switch (pos) {
		case 0:
			keyword = "(مسيرة OR مسيرات OR مظاهرة OR مظاهرات OR اعتصام)";
			hashtag = "(#Bahrain OR #البحرين) lang:ar";
			break;
		case 1:
			keyword = "\"نقطة تفتيش\"";
			hashtag = "(#Bahrain OR #البحرين OR #chpo) lang:ar";
			break;
		case 2:
			keyword = "(اعتقال OR عتقل OR افراج)";
			hashtag = "(#Bahrain OR #البحرين) lang:ar";
			break;
		case 3:
			keyword = "(شرطة OR شغب OR مرتزق OR قوات)";
			hashtag = "(#Bahrain OR #البحرين) lang:ar";
			break;
		case 4:
			keyword = "(غلق OR اغلاق OR شارع OR شوارع)";
			hashtag = "(#Bahrain OR #البحرين) lang:ar";
			break;
		case 5:
			keyword = "(\"انتقل الى رحمة\" OR توفى OR وفاة)";
			hashtag = "(#Bahrain OR #البحرين) lang:ar";
			break;
		case 6:
			keyword = "";
			hashtag = "(#Bahrain OR #البحرين) lang:ar";
			break;
		default:
			keyword = "(مسيرة OR مسيرات OR مظاهرة OR مظاهرات OR اعتصام)";
			hashtag = "(#Bahrain OR #البحرين OR #chpo) lang:ar";
			break;

		}

		if (tweetsProcessed == true) {
			refreshTweetMarkers();
		}

	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {

	}

	public class DownloadTweetsTask extends AsyncTask<String, Void, Boolean> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			refreshInProgress(true);
		}

		@Override
		protected Boolean doInBackground(String... params) {

			try {
				InputStream twitterResponse = new URL(params[0]).openStream();
				BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(twitterResponse));

				TwitterSearch twitterSearch = new Gson().fromJson(
						bufferedReader.readLine(), TwitterSearch.class);
				if (twitterSearch.results.size() > 0) {
					tweets.addAll(twitterSearch.results);
				} else {
					return false;
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}

			return true;
		}

		protected void onPostExecute(Boolean downloaded) {
			super.onPostExecute(downloaded);

			if (downloaded) {
				for (Results tweet : tweets) {
					new AddTweetMarkerTask().execute(tweet);
				}
			}

			else {
				toastMsg("لا توجد أية نتائج");
				tweetsProcessed = true;
				refreshInProgress(false);

			}

		}
	}

	public class AddTweetMarkerTask extends
			AsyncTask<Results, Integer, Boolean> {

		MarkersOverlay tempOverlay = new MarkersOverlay(markerImage,
				getApplicationContext(), true);

		@Override
		protected Boolean doInBackground(Results... params) {
			Results tweet = params[0];

			for (LocationsList location : locationsList) {

				Pattern namePattern = Pattern.compile(location.name,
						Pattern.MULTILINE);
				Matcher locationMatch = namePattern.matcher(tweet.text);

				if (locationMatch.find()) {

					GeoPoint point = new GeoPoint(location.lat + randomNudge(),
							location.lng + randomNudge());

					TweetMarker localpoint = new TweetMarker(point,
							location.name.replaceAll("\\(ال\\)\\*", "ال")
									.replaceAll("\\(\\\\s\\)\\*", " "), "No",
							tweet.from_user, tweet.text, tweet.created_at,
							tweet.profile_image_url, tweet.id_str);

					localpoint
							.setMarker(isOldTweet(localpoint.createdAt) ? markerOld
									: markerImage);

					tempOverlay.addOverlay(localpoint);
					mapMarkers.addOverlay(localpoint);

					maxLat = Math.max(location.lat, maxLat);
					minLat = Math.min(location.lat, minLat);
					maxLng = Math.max(location.lng, maxLng);
					minLng = Math.min(location.lng, minLng);

					return true;
				}
			}

			tweets.remove(tweet);

			return false;
		}

		protected void onPostExecute(Boolean isTweetAdded) {
			super.onPostExecute(isTweetAdded);
			if (mapMarkers.size() == tweets.size()) {
				tweetsProcessed = true;
			}

			if (tweetsProcessed == true) {
				if (mapMarkers.size() == 0) {
					toastMsg("لا توجد أية نتائج");
					refreshInProgress(false);
				} else {
					mapOverlays.clear();
					mapOverlays.add(mapMarkers);

					final Timer timer = new Timer();
					timer.schedule(new TimerTask() {
						@Override
						public void run() {
							if (mapView.getLatitudeSpan() > 2 * mapMarkers
									.getLatSpanE6()
									&& mapView.getLongitudeSpan() > 2 * mapMarkers
											.getLonSpanE6()
									&& mapView.getZoomLevel() < 16) {
								mapController.zoomIn();
							} else {
								timer.cancel();
							}
						}
					}, 150, 75);

					mapController.animateTo(new GeoPoint((maxLat + minLat) / 2,
							(maxLng + minLng) / 2));
					refreshInProgress(false);
					markerIndex = 0;
					showTweetMarker(markerIndex, smallMarkersArea() ? true
							: false);

					for (int i = mapMarkers.size() - 1; i >= 0; i--) {
						mapMarkers.setFocus(mapMarkers.getItem(i));
					}

				}

			} else if (isTweetAdded == true) {
				mapOverlays.add(tempOverlay);
				mapView.invalidate();

				TextView markerCounter = (TextView) findViewById(R.id.markerCounter);
				markerCounter.setText("---/" + mapOverlays.size() + " | ");

			}
		}

	}

	public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

		private final WeakReference<ImageButton> imageButtonRef;
		ImageButton ib;

		public DownloadImageTask(ImageButton imageButton) {
			imageButtonRef = new WeakReference<ImageButton>(imageButton);
		}

		@Override
		protected void onPreExecute() {
			ib = imageButtonRef.get();
			ib.setImageDrawable(getResources().getDrawable(
					R.drawable.ic_action_userimage));

		}

		@Override
		protected Bitmap doInBackground(String... params) {
			try {
				InputStream inputStream = new URL(params[0]).openStream();
				Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
				inputStream.close();
				return bitmap;
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return null;

		}

		protected void onPostExecute(Bitmap bitmap) {
			super.onPostExecute(bitmap);

			ib.setImageBitmap(bitmap);

		}
	}

	public class DownloadTranslationTask extends
			AsyncTask<String, Void, String> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			refreshInProgress(true);
		}

		@Override
		protected String doInBackground(String... params) {
			String translation = null;
			String url = null;
			try {
				url = "http://mymemory.translated.net/api/get?langpair=ar|en&q="
						+ URLEncoder.encode(params[0], "UTF-8");
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}

			try {
				InputStream translationResponse = new URL(url).openStream();
				BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(translationResponse));

				TranslationSearch translationSearch = new Gson().fromJson(
						bufferedReader.readLine(), TranslationSearch.class);
				return translationSearch.responseData.translatedText;
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return translation;
			} catch (IOException e) {
				e.printStackTrace();
				return translation;
			}

		}

		protected void onPostExecute(String translation) {
			super.onPostExecute(translation);
			TextView tweetText = (TextView) findViewById(R.id.tweetText);
			tweetText.setTextSize((translation.length() > 110) ? 15 : 17);
			tweetText.setText(translation);
			refreshInProgress(false);
		}
	}

	public class LocationsList {

		public LocationsList(String n, int lt, int lg) {
			this.name = n;
			this.lat = lt;
			this.lng = lg;
		}

		public String name;
		public int lat;
		public int lng;
	}

	public class MarkersOverlay extends ItemizedOverlay<TweetMarker> {
		private ArrayList<TweetMarker> mOverlays = new ArrayList<TweetMarker>();
		Context mContext;
		boolean isTempOverlay;

		public MarkersOverlay(Drawable defaultMarker, Context context) {
			super(boundCenterBottom(defaultMarker));
			mContext = context;
			isTempOverlay = false;
			populate();
		}

		public MarkersOverlay(Drawable defaultMarker, Context context,
				boolean istemp) {
			super(boundCenterBottom(defaultMarker));
			mContext = context;
			isTempOverlay = istemp;
			populate();
		}

		public void addOverlay(TweetMarker overlay) {
			mOverlays.add(overlay);
			setLastFocusedIndex(-1);
			populate();

		}

		public void clear() {
			mOverlays.clear();
			setLastFocusedIndex(-1);
			populate();
		}

		@Override
		protected TweetMarker createItem(int i) {
			return mOverlays.get(i);
		}

		@Override
		public int size() {
			return mOverlays.size();
		}

		@Override
		protected boolean onTap(int index) {
			if (isTempOverlay == true) {
				return false;
			}

			markerRecent = markerIndex;
			markerIndex = index;
			showTweetMarker(markerIndex, false);
			if (slidingDrawer.isOpened() == false) {
				slidingDrawer.animateOpen();
			}
			mapView.invalidate();
			return true;
		}

	}

	public class TweetMarker extends OverlayItem {

		public String from_user;
		public String text;
		public String created_at;
		public String profile_image_url;
		public String id_str;
		public Date createdAt;

		public TweetMarker(GeoPoint point, String location, String trustedsource) {
			super(point, location, trustedsource);
		}

		public TweetMarker(GeoPoint point, String location,
				String trustedsource, String fromuser, String tweettext,
				String createdat, String image_url, String id) {
			super(point, location, trustedsource);
			from_user = fromuser;
			text = tweettext;
			created_at = createdat;
			profile_image_url = image_url;
			id_str = id;

			try {
				createdAt = new SimpleDateFormat(
						"EEE, dd MMM yyyy HH:mm:ss ZZZZZ", Locale.US)
						.parse(created_at);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

	}

	public class TwitterSearch {

		public ArrayList<Results> results;

	}

	public class Results {

		public String from_user;
		public String text;
		public String created_at;
		public String profile_image_url;
		public String id_str;
		public String iso_language_code;

	}

	public class TranslationSearch {
		TranslationData responseData;
	}

	public class TranslationData {
		String translatedText;
	}

}
