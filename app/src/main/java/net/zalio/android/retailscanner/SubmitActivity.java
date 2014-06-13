package net.zalio.android.retailscanner;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.iubiquity.spreadsheets.client.SpreadsheetClient;

import net.zalio.android.utils.MyLog;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SubmitActivity extends Activity {

    private static final Level LOGGING_LEVEL = Level.OFF;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String PREF_AUTH_TOKEN = "authToken";
    private static final String AUTH_TOKEN_TYPE = "wise";

    static final String TAG = "TasksSample";

    static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;

    static final int REQUEST_AUTHORIZATION = 1;

    static final int REQUEST_ACCOUNT_PICKER = 2;

    final HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();

    //final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    GoogleAccountCredential credential;

    List<String> tasksList;

    ArrayAdapter<String> adapter;

    //com.google.api.services.tasks.Tasks service;

    int numAsyncTasks;

    private ListView listView;
    private SpreadsheetService service;
    private String authToken;
    private AndroidSpreadsheetClient client;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // enable logging
        Logger.getLogger("com.google.api.client").setLevel(LOGGING_LEVEL);
        // view and menu
        setContentView(R.layout.activity_submit);
        // Google Accounts
        credential =
                GoogleAccountCredential.usingOAuth2(this, Collections.singleton("https://spreadsheets.google.com/feeds"));
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        credential.setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));
        // Tasks client
        //service =
        //        new com.google.api.services.tasks.Tasks.Builder(httpTransport, jsonFactory, credential)
        //                .setApplicationName("Google-TasksAndroidSample/1.0").build();

        service = new SpreadsheetService("RetailScanner");

    }

    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        runOnUiThread(new Runnable() {
            public void run() {
                Dialog dialog =
                        GooglePlayServicesUtil.getErrorDialog(connectionStatusCode, SubmitActivity.this,
                                REQUEST_GOOGLE_PLAY_SERVICES);
                dialog.show();
            }
        });
    }

    void refreshView() {
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, tasksList);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkGooglePlayServicesAvailable()) {
            haveGooglePlayServices();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == Activity.RESULT_OK) {
                    haveGooglePlayServices();
                } else {
                    checkGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK) {
                    AsyncLoadTasks.run(this);
                } else {
                    chooseAccount();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        credential.setSelectedAccountName(accountName);
                        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.commit();
                        AsyncLoadTasks.run(this);
                    }
                }
                break;
        }
    }

    //@Override
    //public boolean onCreateOptionsMenu(Menu menu) {
    //    MenuInflater inflater = getMenuInflater();
    //    inflater.inflate(R.menu.main_menu, menu);
    //    return super.onCreateOptionsMenu(menu);
    //}
    //
    //@Override
    //public boolean onOptionsItemSelected(MenuItem item) {
    //    switch (item.getItemId()) {
    //        case R.id.menu_refresh:
    //            AsyncLoadTasks.run(this);
    //            break;
    //        case R.id.menu_accounts:
    //            chooseAccount();
    //            return true;
    //    }
    //    return super.onOptionsItemSelected(item);
    //}

    /** Check that Google Play services APK is installed and up to date. */
    private boolean checkGooglePlayServicesAvailable() {
        final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        }
        return true;
    }

    private void haveGooglePlayServices() {
        // check if there is already an account selected
        if (credential.getSelectedAccountName() == null) {
            // ask user to choose account
            chooseAccount();
        } else {
            // load calendars
            AsyncLoadTasks.run(this);
        }
    }

    private void chooseAccount() {
        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    static class AndroidSpreadsheetClient extends SpreadsheetClient {
        public AndroidSpreadsheetClient(HttpRequestFactory requestFactory) {
            super.requestFactory = requestFactory;
        }
    }
    private void createClient(final Context context,
                              final String token) {

        final HttpTransport transport = AndroidHttp.newCompatibleTransport();

        client = new AndroidSpreadsheetClient(
                transport.createRequestFactory(new HttpRequestInitializer() {

                    public void initialize(final HttpRequest request) {
                        //final GoogleHeaders headers = new GoogleHeaders();
                        HttpHeaders headers = new HttpHeaders();
                        headers.setUserAgent("RetailScanner");
                        //headers.setApplicationName("Spreadsheet API Android Example");
                        headers.set("GData-Version", 2);
                        //headers.gdataVersion = "2";
                        request.setHeaders(headers);
                        //request.headers = headers;
                        client.initializeParser(request);
                        request.setInterceptor(new HttpExecuteInterceptor() {

                            public void intercept(final HttpRequest request)
                                    throws IOException {
                                //final GoogleHeaders headers = (GoogleHeaders) request.headers;
                                final HttpHeaders headers = (HttpHeaders) request.getHeaders();
                                Log.d(TAG, "setting authToken in Header: "
                                        + token);
                                headers.setAuthorization("GoogleLogin auth=" + token);
                                //headers.setGoogleLogin(authToken);
                                //request.getInterceptor().intercept(request);
                                //new MethodOverride().intercept(request);
                            }
                        });

                        request.setUnsuccessfulResponseHandler(new HttpUnsuccessfulResponseHandler() {
                            public boolean handleResponse(
                                    final HttpRequest request,
                                    final HttpResponse response,
                                    final boolean retrySupported) {

                                if (response.getStatusCode() == 401) {
                                    Log.d(TAG, "invalidating token "
                                            + token);
                                    //gaccountManager
                                    //        .invalidateAuthToken(authToken);
                                    SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                                    settings.edit()
                                            .remove(PREF_AUTH_TOKEN)
                                            .commit();
                                    //final Intent intent = new Intent(context,
                                    //        TestActivity.class);
                                    //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    //TestActivity.this.startActivity(intent);
                                    //init();
                                    //getAccount();
                                }
                                return false;
                            }
                        });
                    }
                }));
    }

    static class AsyncLoadTasks extends AsyncTask {

        private final SubmitActivity parent;

        public static void run(SubmitActivity parent) {
            new AsyncLoadTasks(parent).execute();
        }

        AsyncLoadTasks (SubmitActivity parent) {
            this.parent = parent;
        }
        @Override
        protected Object doInBackground(Object[] params) {

            URL SPREADSHEET_FEED_URL = null;
            try {
                SPREADSHEET_FEED_URL = new URL(
                        "https://spreadsheets.google.com/feeds/spreadsheets/private/full");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            try {
                parent.authToken = parent.credential.getToken();
                parent.credential.getGoogleAccountManager().getAccountManager().getAuthTokenByFeatures(
                        GoogleAccountManager.ACCOUNT_TYPE,
                        parent.AUTH_TOKEN_TYPE,
                        null,
                        parent,
                        null,
                        null,
                        new AccountManagerCallback<Bundle>() {
                            @Override
                            public void run(AccountManagerFuture<Bundle> future) {
                                Bundle bundle;
                                try {
                                    bundle = future.getResult();
                                    //parent.setAccountName(bundle
                                    //        .getString(AccountManager.KEY_ACCOUNT_NAME));
                                    parent.authToken= bundle
                                            .getString(AccountManager.KEY_AUTHTOKEN);
                                } catch (final OperationCanceledException e) {
                                    // user canceled
                                    Log.d(TAG, "Line 87 " + e.getMessage());
                                } catch (final AuthenticatorException e) {
                                    Log.d(TAG, "Line 89 " + e.getMessage());
                                    //TestActivity.this.handleException(e);
                                } catch (final IOException e) {
                                    Log.d(TAG, "Line 92 " + e.getMessage());
                                    //TestActivity.this.handleException(e);
                                }
                            }
                        },
                        null
                );
                MyLog.i(TAG, "Token: " + parent.authToken);

                parent.createClient(parent, parent.authToken);

                com.iubiquity.spreadsheets.model.SpreadsheetFeed sheets = parent.client.getSpreadsheetMetafeed();
                for (com.iubiquity.spreadsheets.model.SpreadsheetEntry e:sheets.getEntries()) {
                    MyLog.i(TAG, e.title);
                }
            } catch (com.google.android.gms.auth.UserRecoverableAuthException e) {
                parent.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (GoogleAuthException e) {
                e.printStackTrace();
            }
            // Define the URL to request.  This should never change.

            // Make a request to the API and get all spreadsheets.
            //SpreadsheetFeed feed = null;
            //try {
            //    feed = parent.service.getFeed(SPREADSHEET_FEED_URL, SpreadsheetFeed.class);
            //} catch (IOException e) {
            //    e.printStackTrace();
            //} catch (ServiceException e) {
            //    e.printStackTrace();
            //}
            //if (feed != null) {
            //    List<SpreadsheetEntry> spreadsheets = feed.getEntries();
            //
            //    // Iterate through all of the spreadsheets returned
            //    for (SpreadsheetEntry spreadsheet : spreadsheets) {
            //        // Print the title of this spreadsheet to the screen
            //        MyLog.i(TAG, spreadsheet.getTitle().getPlainText());
            //    }
            //}
            return null;
        }
    }
}
