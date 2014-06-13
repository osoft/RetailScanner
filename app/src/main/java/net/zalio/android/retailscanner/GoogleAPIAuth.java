package net.zalio.android.retailscanner;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.util.ServiceException;

import net.zalio.android.utils.MyLog;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * Created by Henry on 6/11/14.
 */
public class GoogleAPIAuth extends AsyncTask{
    private static final String TAG = "GDataOAuthAsyncTask";
    private WeakReference<Activity> parentRef;
    private static GoogleAccountCredential credential;

    static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;

    static final int REQUEST_AUTHORIZATION = 1;

    static final int REQUEST_ACCOUNT_PICKER = 2;

    GoogleAPIAuth(Activity parentActivity) {
        parentRef = new WeakReference<Activity>(parentActivity);
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    protected Object doInBackground(Object[] params) {
        Activity parent = parentRef.get();
        if (parent == null) {
            return null;
        }

        AccountManager accountManager = AccountManager.get(parent);
        if (credential == null) {
            credential = GoogleAccountCredential.usingOAuth2(parent, Collections.singleton("https://spreadsheets.google.com/feeds"));
        }

        if (checkGooglePlayServicesAvailable(parent)) {
            haveGooglePlayServices(parent);
        }
        return null;
    }

    /** Check that Google Play services APK is installed and up to date. */
    private boolean checkGooglePlayServicesAvailable(Context context) {
        final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            //showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            MyLog.w(TAG, "Google Play Service not available!");
            return false;
        }
        return true;
    }

    private void haveGooglePlayServices(Activity parent) {
        // check if there is already an account selected
        if (credential.getSelectedAccountName() == null) {
            // ask user to choose account
            chooseAccount(parent);
        } else {
            // load calendars
            //AsyncLoadTasks.run(this);
        }
    }

    private void chooseAccount(Activity parent) {
        parent.startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    private void requestAuthorization(Activity parent) {
        //parent.startActivityForResult(credential.);
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        MyLog.i(TAG, "onResult: " + requestCode);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == Activity.RESULT_OK) {
                    haveGooglePlayServices(parentRef.get());
                } else {
                    checkGooglePlayServicesAvailable(parentRef.get());
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK) {
                    //AsyncLoadTasks.run(this);
                } else {
                    chooseAccount(parentRef.get());
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        credential.setSelectedAccountName(accountName);
                        MyLog.i(TAG, "Credential set!");
                        //SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                        //SharedPreferences.Editor editor = settings.edit();
                        //editor.putString(PREF_ACCOUNT_NAME, accountName);
                        //editor.commit();
                        //AsyncLoadTasks.run(this);
                        new Thread() {
                            @Override
                            public void run() {
                                SpreadsheetService service = new SpreadsheetService("RetailScanner");

                                URL SPREADSHEET_FEED_URL = null;
                                try {
                                    SPREADSHEET_FEED_URL = new URL(
                                            "https://spreadsheets.google.com/feeds/spreadsheets/private/full");
                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                }

                                try {
                                    service.setUserToken(credential.getToken());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (GoogleAuthException e) {
                                    e.printStackTrace();
                                }
                                // Define the URL to request.  This should never change.

                                // Make a request to the API and get all spreadsheets.
                                SpreadsheetFeed feed = null;
                                try {
                                    feed = service.getFeed(SPREADSHEET_FEED_URL, SpreadsheetFeed.class);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (ServiceException e) {
                                    e.printStackTrace();
                                }
                                if (feed != null) {
                                    List<SpreadsheetEntry> spreadsheets = feed.getEntries();

                                    // Iterate through all of the spreadsheets returned
                                    for (SpreadsheetEntry spreadsheet : spreadsheets) {
                                        // Print the title of this spreadsheet to the screen
                                        MyLog.i(TAG, spreadsheet.getTitle().getPlainText());
                                    }
                                }
                            }
                        }.start();
                    }
                }
                break;
        }
    }
}
