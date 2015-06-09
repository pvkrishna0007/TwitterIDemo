package com.example.krishna.twitteridemo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;

import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;


public class TwitterActivity extends ActionBarActivity {

    static String TWITTER_CONSUMER_KEY = "PBppEM3wPcoK8yXf9b97R1yZp";
    static String TWITTER_CONSUMER_SECRET = "y6dVHldx03bX4CENXn2G8scaebecp9I5bdGUaZruKuW2HZl3bc";

    // Preference Constants
    static String PREFERENCE_NAME = "twitter_oauth";
    static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";

    static final String PREF_KEY_TWITTER_U_NAME = "username";

    static final String TWITTER_CALLBACK_URL = "oauth://t4jsample";

    // Twitter oauth urls
    static final String URL_TWITTER_AUTH = "auth_url";
    static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
    static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";

    // Login button
    Button btnLoginTwitter;
    // Update status button
    Button btnUpdateStatus;
    // Logout button
    Button btnLogoutTwitter;
    // EditText for update
    EditText txtUpdate;
    // lbl update
    TextView lblUpdate;
    TextView lblUserName;

    // Progress dialog
    ProgressDialog pDialog;

    // Twitter
    private static Twitter twitter;
    private static RequestToken requestToken;

    // Shared Preferences
    private static SharedPreferences mSharedPreferences;

    // Internet Connection detector
    private ConnectionDetector cd;

    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twitter);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        cd = new ConnectionDetector(getApplicationContext());

        // Check if Internet present
        if (!cd.isConnectingToInternet()) {
            // Internet Connection is not present
            alert.showAlertDialog(TwitterActivity.this, "Internet Connection Error",
                    "Please connect to working Internet connection", false);
            // stop executing code by return
            return;
        }

        // Check if twitter keys are set
        if (TWITTER_CONSUMER_KEY.trim().length() == 0 || TWITTER_CONSUMER_SECRET.trim().length() == 0) {
            // Internet Connection is not present
            alert.showAlertDialog(TwitterActivity.this, "Twitter oAuth tokens", "Please set your twitter oauth tokens first!", false);
            // stop executing code by return
            return;
        }

        // All UI elements
        btnLoginTwitter = (Button) findViewById(R.id.btnLoginTwitter);
        btnUpdateStatus = (Button) findViewById(R.id.btnUpdateStatus);
        btnLogoutTwitter = (Button) findViewById(R.id.btnLogoutTwitter);
        txtUpdate = (EditText) findViewById(R.id.txtUpdateStatus);
        lblUpdate = (TextView) findViewById(R.id.lblUpdate);
        lblUserName = (TextView) findViewById(R.id.lblUserName);

        // Shared Preferences
        mSharedPreferences = getApplicationContext().getSharedPreferences(
                "MyPref", 0);

        /**
         * Twitter login button click event will call loginToTwitter() function
         * */
        btnLoginTwitter.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // Call login twitter function
                loginToTwitter();
            }
        });

        /**
         * Button click event to Update Status, will call updateTwitterStatus()
         * function
         * */
        btnUpdateStatus.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Call update status function
                // Get the status from EditText
                String status = txtUpdate.getText().toString();

                // Check for blank text
                if (status.trim().length() > 0) {
                    // update status
                    new UpdateTwitterStatus().execute(status);// NetworkOnMainThreadException raises when directly calls.
                } else {
                    // EditText is empty
                    Toast.makeText(getApplicationContext(),
                            "Please enter status message", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });

        btnLogoutTwitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Twitter logged out.", Toast.LENGTH_SHORT).show();
                mSharedPreferences.edit().putBoolean(PREF_KEY_TWITTER_LOGIN, false).commit();
            }
        });

        changeToAlreadyLoggedIn();
    }

    public void changeToAlreadyLoggedIn(){
        /** This if conditions is tested once is
         * redirected from twitter page. Parse the uri to get oAuth
         * Verifier
         * */
        if (!isTwitterLoggedInAlready()) {
            Toast.makeText(getApplicationContext(),
                    " Twitter Login is required...", Toast.LENGTH_LONG).show();
            Uri uri = getIntent().getData();

            if (uri != null && uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
                new TwitLoginTask().execute(uri);// NetworkOnMainThreadException raises when directly calls.
            }
        } else {
            Toast.makeText(getApplicationContext(),
                    " Twitter is already logged in.", Toast.LENGTH_LONG).show();

            // Displaying in xml ui
            String username = mSharedPreferences.getString(PREF_KEY_TWITTER_U_NAME, "EMPTY");

            // Hide login button
            btnLoginTwitter.setVisibility(View.GONE);

            // Show Update Twitter
            lblUpdate.setVisibility(View.VISIBLE);
            txtUpdate.setVisibility(View.VISIBLE);
            btnUpdateStatus.setVisibility(View.VISIBLE);
            btnLogoutTwitter.setVisibility(View.VISIBLE);

            // Displaying in xml ui
            lblUserName.setText(Html.fromHtml("<b>Welcome " + username + "</b>"));
        }
    }

    /**
     * Function to login twitter
     */
    private void loginToTwitter() {
        // Check if already logged in
        if (!isTwitterLoggedInAlready()) {
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
            builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
            Configuration configuration = builder.build();

            TwitterFactory factory = new TwitterFactory(configuration);
            twitter = factory.getInstance();

            new TwitTask().execute();// NetworkOnMainThreadException raises when directly calls.
        } else {
            // user already logged into twitter
            Toast.makeText(getApplicationContext(),
                    "Already Logged into twitter", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Check user already logged in your application using twitter Login flag is
     * fetched from Shared Preferences
     */
    private boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
    }

    class TwitTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                requestToken = twitter
                        .getOAuthRequestToken(TWITTER_CALLBACK_URL);

            } catch (TwitterException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            TwitterActivity.this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                    .parse(requestToken.getAuthenticationURL())));
            finish();
            Toast.makeText(getApplicationContext(),
                    "Logged into twitter", Toast.LENGTH_LONG).show();
        }
    }

    class TwitLoginTask extends AsyncTask<Uri, Void, Void> {

        @Override
        protected Void doInBackground(Uri... params) {
            getLoginDetails(params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

        }
    }

    public void getLoginDetails(Uri uri) {
        // oAuth verifier
        String verifier = uri
                .getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);

        try {
            // Get the access token
            AccessToken accessToken = twitter.getOAuthAccessToken(
                    requestToken, verifier);

            Log.e("Twitter OAuth Token", "> " + accessToken.getToken());

            long userID = accessToken.getUserId();
            User user = twitter.showUser(userID);
            final String username = user.getName();

            // Shared Preferences
            SharedPreferences.Editor e = mSharedPreferences.edit();

            // After getting access token, access token secret
            // store them in application preferences
            e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
            e.putString(PREF_KEY_OAUTH_SECRET, accessToken.getTokenSecret());
            // Store login status - true
            e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
            e.putString(PREF_KEY_TWITTER_U_NAME, username);
            e.commit(); // save changes

            Log.e("Twitter OAUth Token", "UName:" + username);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    // Hide login button
                    btnLoginTwitter.setVisibility(View.GONE);

                    // Show Update Twitter
                    lblUpdate.setVisibility(View.VISIBLE);
                    txtUpdate.setVisibility(View.VISIBLE);
                    btnUpdateStatus.setVisibility(View.VISIBLE);
                    btnLogoutTwitter.setVisibility(View.VISIBLE);

                    // Displaying in xml ui
                    lblUserName.setText(Html.fromHtml("<b>Welcome " + username + "</b>"));

                    Log.d("TwitterActivity", "run (Line:318) :");
                    Log.d("Twitter", " Logged in...");
                    Toast.makeText(getApplicationContext(),
                            " Twitter Logged in", Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            // Check log for login errors
            Log.d("TwitterActivity", "getLoginDetails (Line:326) :"+e);
            Log.e("Twitter Login Error", "> " + e);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), " Twitter Login error", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    /**
     * Function to update status
     */
    class UpdateTwitterStatus extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Dialog
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(TwitterActivity.this);
            pDialog.setMessage("Updating to twitter...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        /**
         * getting Places JSON
         */
        protected String doInBackground(String... args) {
            Log.d("Tweet Text", "> " + args[0]);
            String status = args[0];
            try {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
                builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);

                // Access Token
                String access_token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, "");
                // Access Token Secret
                String access_token_secret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, "");

                AccessToken accessToken = new AccessToken(access_token, access_token_secret);
                Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);

                // Update status
                twitter4j.Status response = twitter.updateStatus(status);

                getTweets(twitter);

                Log.d("Status", "> " + response.getText());
            } catch (TwitterException e) {
                // Error in updating status
                Log.d("Twitter Update Error", e.getMessage());
            }
            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog and show
         * the data in UI Always use runOnUiThread(new Runnable()) to update UI
         * from background thread, otherwise you will get error
         * *
         */
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after getting all products
            pDialog.dismiss();
            // updating UI from Background Thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Status tweeted successfully", Toast.LENGTH_SHORT).show();
                    // Clearing EditText field
                    txtUpdate.setText("");
                }
            });
        }
    }

    private ArrayList getTweets(Twitter twitter) {
        ArrayList tweetsList = new ArrayList();
        try {
            ResponseList<Status> response = twitter.getUserTimeline();
            for (int i = 0; i < response.size(); i++) {
                TwitterInfo tweetInfo = new TwitterInfo();
                Status status = response.get(i);
                tweetInfo.setText(status.getText());
                tweetInfo.setCreatedAt(status.getCreatedAt());
                tweetInfo.setCount(status.getRetweetCount());
                tweetInfo.setName(status.getUser().getName());
                tweetInfo.setPicurl(status.getUser().getProfileImageURL());
                tweetsList.add(tweetInfo);
            }

            Log.e("Twitter List", "List-->"+tweetsList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tweetsList;
    }

    private class TwitterInfo {
        private String picurl;
        private String name;
        private int count;
        private Date createdAt;
        private String text;

        public void setPicurl(String picurl) {
            this.picurl = picurl;
        }

        public String getPicurl() {
            return picurl;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return "TwitterInfo{" +
                    "picurl='" + picurl + '\'' +
                    ", name='" + name + '\'' +
                    ", count=" + count +
                    ", createdAt=" + createdAt +
                    ", text='" + text + '\'' +
                    '}';
        }
    }
}
