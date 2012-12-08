package com.ibnkan.bahmap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

public class NewsActivity extends Activity {

	private ListView listView;
	private TweetsAdapter tweetsAdapter;
	private ArrayList<com.ibnkan.bahmap.MainActivity.Results> tweets;
	final private String searchURL = "http://search.twitter.com/search.json?q=from%3Abahrainmirror+OR+%28from%3Aalwasatnews+OR+from%3AAJEnglish+AND+Bahrain%29&result_type=recent&rpp=100";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_news);
		tweets = new ArrayList<com.ibnkan.bahmap.MainActivity.Results>();
		tweetsAdapter = new TweetsAdapter(this, R.layout.listrow, tweets);
		listView = (ListView) findViewById(R.id.list);
		listView.setAdapter(tweetsAdapter);
		new DownloadTweetsTask().execute(searchURL);
	}

	public class DownloadTweetsTask extends AsyncTask<String, Integer, String> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			Toast.makeText(getApplicationContext(), "تنزيل الأخبار...",
					Toast.LENGTH_SHORT).show();
		}

		@Override
		protected String doInBackground(String... params) {
			String msg = "تم التنزيل";

			try {
				InputStream twitterResponse = new URL(params[0]).openStream();
				BufferedReader buffReader = new BufferedReader(
						new InputStreamReader(twitterResponse));

				com.ibnkan.bahmap.MainActivity.TwitterSearch twitterSearch = new Gson()
						.fromJson(
								buffReader.readLine(),
								com.ibnkan.bahmap.MainActivity.TwitterSearch.class);

				tweets.addAll(twitterSearch.results);

			} catch (MalformedURLException e) {
				e.printStackTrace();
				msg = "فشل التنزيل";
			} catch (IOException e) {
				e.printStackTrace();
				msg =  "فشل التنزيل";
			}
			return msg;
		}

		protected void onPostExecute(String msg) {
			super.onPostExecute(msg);
			Toast.makeText(getApplicationContext(), msg,
					Toast.LENGTH_LONG).show();
			tweetsAdapter.notifyDataSetChanged();
		}
	}

	public class TweetsAdapter extends
			ArrayAdapter<com.ibnkan.bahmap.MainActivity.Results> {

		private ArrayList<com.ibnkan.bahmap.MainActivity.Results> tweets;
		private Context c;

		public TweetsAdapter(Context context, int textViewResourceId,
				ArrayList<com.ibnkan.bahmap.MainActivity.Results> tweets) {
			super(context, textViewResourceId, tweets);
			this.tweets = tweets;
			this.c = context;

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;

			if (view == null) {
				LayoutInflater li = (LayoutInflater) c
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = li.inflate(R.layout.listrow, null);
			}

			com.ibnkan.bahmap.MainActivity.Results tweet = tweets.get(position);

			if (tweet != null) {
				TextView userID = (TextView) view.findViewById(R.id.list_userID);
				userID.setText("@" + tweet.from_user);

				TextView text = (TextView) view.findViewById(R.id.list_text);
				text.setText(tweet.text.replaceAll("(\n|(@|#)\\w+:*|[\"“”])", "")
						.replaceAll("^[\\s\\W\\d]+", ""));

				Date createdAt = null;
				TextView timeStamp = (TextView) view
						.findViewById(R.id.list_timeStamp);
				try {
					createdAt = new SimpleDateFormat(
							"EEE, dd MMM yyyy HH:mm:ss ZZZZZ", Locale.US)
							.parse(tweet.created_at);
					timeStamp
							.setText(new SimpleDateFormat("E, d MMM @ h:mm aa", Locale.US)
									.format(createdAt));
				} catch (ParseException e) {
					e.printStackTrace();
				}

				TextView since = (TextView) view.findViewById(R.id.list_since);
				since.setText(estimatePostAge(createdAt));

				ImageView imageView = (ImageView) view
						.findViewById(R.id.list_userImage);
				new DownloadImageTask(imageView).execute(tweet.profile_image_url);

			}
			return view;

		}

		public String estimatePostAge(Date createdAt) {

			double second = 1000.0;
			double minute = 60.0 * second;
			double hour = 60.0 * minute;
			double day = 24.0 * hour;

			Date now = new Date();
			double age = now.getTime() - createdAt.getTime();

			if (age < 30 * second) {
				return c.getString(R.string.just_now);

			}

			if (age < 1.5 * minute) {
				return c.getString(R.string.one_minute_ago);
			}

			if (age < 2.5 * minute) {
				return c.getString(R.string.two_minutes_ago);
			}

			if (age < 10.5 * minute) {
				return c.getString(R.string.before) + " "
						+ (int) Math.round((age / minute)) + " "
						+ c.getString(R.string.minutes);
			}

			if (age < 55 * minute) {
				return c.getString(R.string.before) + " "
						+ (int) Math.round((age / minute)) + " "
						+ c.getString(R.string.minute);
			}

			if (age < 1.3 * hour) {
				return c.getString(R.string.an_hour_ago);
			}

			if (age < 1.7 * hour) {
				return c.getString(R.string.an_hour_half_ago);
			}

			if (age < 2.5 * hour) {
				return c.getString(R.string.two_hours_ago);
			}

			if (age < 11 * hour) {
				return c.getString(R.string.before) + " "
						+ (int) Math.round((age / hour)) + " "
						+ c.getString(R.string.hours);
			}

			if (age < 24 * hour) {
				return c.getString(R.string.before) + " "
						+ (int) Math.round((age / hour)) + " "
						+ c.getString(R.string.hour);
			}

			if (age < 2 * day) {
				return c.getString(R.string.yesterday);
			}

			return c.getString(R.string.old_tweet);

		}

		public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

			private final WeakReference<ImageView> imageViewRef;

			public DownloadImageTask(ImageView imageView) {
				imageViewRef = new WeakReference<ImageView>(imageView);
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

				ImageView iv = imageViewRef.get();
				iv.setImageBitmap(bitmap);

			}
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
//		 Inflate the menu; this adds items to the action bar if it is present.
		 getMenuInflater().inflate(R.menu.activity_news, menu);
		return true;
	}

	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	// switch (item.getItemId()) {
	// case android.R.id.home:
	// // This ID represents the Home or Up button. In the case of this
	// // activity, the Up button is shown. Use NavUtils to allow users
	// // to navigate up one level in the application structure. For
	// // more details, see the Navigation pattern on Android Design:
	// //
	// //
	// http://developer.android.com/design/patterns/navigation.html#up-vs-back
	// //
	// NavUtils.navigateUpFromSameTask(this);
	// return true;
	// }
	// return super.onOptionsItemSelected(item);
	// }

}
