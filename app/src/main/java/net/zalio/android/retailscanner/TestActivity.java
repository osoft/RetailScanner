package net.zalio.android.retailscanner;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.common.collect.Lists;
import com.iubiquity.spreadsheets.client.SpreadsheetClient;
import com.iubiquity.spreadsheets.model.SpreadsheetEntry;
import com.iubiquity.spreadsheets.model.SpreadsheetFeed;
import com.iubiquity.spreadsheets.model.WorksheetData;
import com.iubiquity.spreadsheets.model.WorksheetEntry;
import com.iubiquity.spreadsheets.model.WorksheetFeed;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//import com.google.api.client.extensions.android2.AndroidHttp;
//import com.google.api.client.googleapis.GoogleHeaders;
//import com.google.api.client.googleapis.MethodOverride;
//import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;

/**
 *
 * Example of how to use the spreadsheet-api. The example simply lists all the
 * available spreadsheets the are available in the private user feed.
 *
 * <p>
 * To enable logging of HTTP requests/responses, change {@link #LOGGING_LEVEL}
 * to {@link Level#CONFIG} or {@link Level#ALL} and run this command:
 * </p>
 *
 * <pre>
 * adb shell setprop log.tag.HttpTransport DEBUG
 * </pre>
 *
 */
public class TestActivity extends ListActivity {

    public static final String TAG = TestActivity.class.getSimpleName();

    /** Logging level for HTTP requests/responses. */
    private static final Level LOGGING_LEVEL = Level.ALL;

    private static final String AUTH_TOKEN_TYPE = "wise";
    private static final int REQUEST_AUTHENTICATE = 0;

    private static GoogleAccountManager accountManager;

    static class AndroidSpreadsheetClient extends SpreadsheetClient {
        public AndroidSpreadsheetClient(HttpRequestFactory requestFactory) {
            super.requestFactory = requestFactory;
        }
    }

    public static SpreadsheetClient client;

    private String authToken;
    private String accountName;

    private final Context context = this;

    private int k;

    private SharedPreferences settings;

    public static final String PREF_ACCOUNT_NAME = "accountName";
    public static final String PREF_AUTH_TOKEN = "authToken";
    private String[] spreadsheetNames = { "No spreadsheets found!" };
    List<String> list = Lists.newArrayList();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.getLogger("com.google.api.client").setLevel(
                TestActivity.LOGGING_LEVEL);

        init();
    }
    private void init() {
        TestActivity.accountManager = new GoogleAccountManager(this);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        this.authToken = settings.getString(TestActivity.PREF_AUTH_TOKEN, null);
        this.accountName = settings.getString(TestActivity.PREF_ACCOUNT_NAME,
                null);
        this.createClient(this, TestActivity.accountManager);
        getListView().setTextFilterEnabled(true);
        populateList();
    }
    private void populateList() {
        new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] params) {
                try {
                    SpreadsheetFeed feed = client.getSpreadsheetMetafeed();
                    for (SpreadsheetEntry entry : feed.getEntries()) {
                        list.add(entry.title);
                        WorksheetFeed wf = client.executeGetWorksheetFeed(entry.getWorksheetFeedLink());
                        WorksheetEntry we = wf.getEntries().get(0);
                        WorksheetData wd = client.getWorksheetData(we.getCellFeedLink());
                        int size = wd.rows.size();
                        client.addCell(wf.getEntries().get(0), "TEST_Retailer", size + 1, 1);
                        client.addCell(wf.getEntries().get(0), "TEST_Brand", size + 1, 2);
                        client.addCell(wf.getEntries().get(0), "TEST_Model#", size + 1, 3);
                        client.addCell(wf.getEntries().get(0), "" + (Math.round(Math.random()*1000)+800), size + 1, 5);
                    }

                } catch (IOException e) {
                }

                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                TestActivity.this.spreadsheetNames = (String[]) list.toArray(new String[list.size()]);
                setListAdapter(new ArrayAdapter<String>(TestActivity.this,
                        android.R.layout.simple_list_item_1, spreadsheetNames));
            }
        }.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.getAccount();
    }

    private void chooseAccount() {
        TestActivity.accountManager.getAccountManager().getAuthTokenByFeatures(
                GoogleAccountManager.ACCOUNT_TYPE,
                TestActivity.AUTH_TOKEN_TYPE, null, TestActivity.this, null,
                null, new AccountManagerCallback<Bundle>() {

                    public void run(final AccountManagerFuture<Bundle> future) {
                        Bundle bundle;
                        try {
                            bundle = future.getResult();
                            TestActivity.this.setAccountName(bundle
                                    .getString(AccountManager.KEY_ACCOUNT_NAME));
                            TestActivity.this.setAuthToken(bundle
                                    .getString(AccountManager.KEY_AUTHTOKEN));
                        } catch (final OperationCanceledException e) {
                            // user canceled
                            Log.d(TAG, "Line 87 " + e.getMessage());
                        } catch (final AuthenticatorException e) {
                            Log.d(TAG, "Line 89 " + e.getMessage());
                            TestActivity.this.handleException(e);
                        } catch (final IOException e) {
                            Log.d(TAG, "Line 92 " + e.getMessage());
                            TestActivity.this.handleException(e);
                        }
                    }
                }, null
        );
    }

    private void createClient(final Context context,
                              final GoogleAccountManager gaccountManager) {

        final HttpTransport transport = AndroidHttp.newCompatibleTransport();

        TestActivity.client = new AndroidSpreadsheetClient(
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
                        TestActivity.client.initializeParser(request);
                        request.setInterceptor(new HttpExecuteInterceptor() {

                            public void intercept(final HttpRequest request)
                                    throws IOException {
                                //final GoogleHeaders headers = (GoogleHeaders) request.headers;
                                final HttpHeaders headers = (HttpHeaders) request.getHeaders();
                                Log.d(TAG, "setting authToken in Header: "
                                        + authToken);
                                headers.setAuthorization("GoogleLogin auth=" + authToken);
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
                                            + authToken);
                                    gaccountManager
                                            .invalidateAuthToken(authToken);
                                    settings.edit()
                                            .remove(TestActivity.PREF_AUTH_TOKEN)
                                            .commit();
                                    //final Intent intent = new Intent(context,
                                    //        TestActivity.class);
                                    //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    //TestActivity.this.startActivity(intent);
                                    init();
                                    getAccount();
                                }
                                return false;
                            }
                        });
                    }
                }));
    }

    private void getAccount() {
        final Account account = TestActivity.accountManager
                .getAccountByName(this.accountName);
        Log.d(TAG, "getAccount " + this.accountName + " try no :" + k);
        if (account != null) {
            // handle invalid token
            if (this.authToken == null) {
                TestActivity.accountManager.getAccountManager().getAuthToken(account,
                        TestActivity.AUTH_TOKEN_TYPE, true,
                        new AccountManagerCallback<Bundle>() {

                            public void run(
                                    final AccountManagerFuture<Bundle> future) {
                                try {
                                    final Bundle bundle = future.getResult();
                                    if (bundle
                                            .containsKey(AccountManager.KEY_INTENT)) {
                                        final Intent intent = bundle
                                                .getParcelable(AccountManager.KEY_INTENT);
                                        int flags = intent.getFlags();
                                        flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
                                        intent.setFlags(flags);
                                        TestActivity.this
                                                .startActivityForResult(
                                                        intent,
                                                        TestActivity.REQUEST_AUTHENTICATE);
                                    } else if (bundle
                                            .containsKey(AccountManager.KEY_AUTHTOKEN)) {
                                        TestActivity.this.setAuthToken(bundle
                                                .getString(AccountManager.KEY_AUTHTOKEN));

                                    }
                                    k = 0;
                                } catch (final Exception e) {
                                    Toast.makeText(context, e.toString(),
                                            Toast.LENGTH_LONG);
                                    TestActivity.this.handleException(e);
                                }
                            }
                        }, null);
            }
            return;
        }
        this.chooseAccount();
    }

    private void handleException(final Exception e) {

        if (e instanceof HttpResponseException) {
            //final HttpResponse response = ((HttpResponseException) e).response;
            final HttpResponseException responseException = ((HttpResponseException)e);
            final int statusCode = responseException.getStatusCode();
            //try {
            //    //responseException.
            //    //response.ignore();
            //} catch (final IOException e1) {
            //    Toast.makeText(context, e1.toString(), Toast.LENGTH_SHORT);
            //}
            if (statusCode == 401) {
                k++;
                if (k <= 1) {
                    this.getAccount();
                }
                return;
            }
        } else {
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT);
        }
        Log.e(TestActivity.TAG, e.getMessage(), e);
    }

    @Override
    protected void onActivityResult(final int requestCode,
                                    final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TestActivity.TAG, "onActivityResult");
        if (requestCode == TestActivity.REQUEST_AUTHENTICATE) {
            if (resultCode == Activity.RESULT_OK) {
                this.getAccount();
            } else {
                this.chooseAccount();
            }
        }
    }

    private void setAccountName(final String accountName) {
        final SharedPreferences.Editor editor = settings.edit();
        editor.putString(TestActivity.PREF_ACCOUNT_NAME, accountName);
        editor.commit();
        this.accountName = accountName;
    }

    private void setAuthToken(final String authToken) {
        Log.d(TAG, "saving authToken " + authToken);
        final SharedPreferences.Editor editor = settings.edit();
        editor.putString(TestActivity.PREF_AUTH_TOKEN, authToken);
        editor.commit();
        this.authToken = authToken;
    }

}