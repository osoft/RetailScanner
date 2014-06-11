package net.zalio.android.retailscanner;

import android.app.Activity;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import net.zalio.android.utils.MyLog;


public class FieldSelectionActivity extends Activity {

    private static final String TAG = FieldSelectionActivity.class.getSimpleName();
    private ImageView mIvPhoto;

    private boolean locatingLT = true;
    private boolean locatingRB = false;

    private FrameLayout mFlHighlight;
    private Rect mRectHighlight;
    private FrameLayout mFlBg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_field_selection);

        mIvPhoto = (ImageView) findViewById(R.id.ivPhoto);
        mFlBg = (FrameLayout) findViewById(R.id.flBackground);

        MyLog.i(TAG, "file://" + getIntent().getStringExtra("path"));
        mIvPhoto.setImageURI(Uri.parse("file://" + getIntent().getStringExtra("path")));
        mIvPhoto.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                return false;
            }
        });
        toggleHideyBar();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.field_selection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        MyLog.i(TAG, event.toString());
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (locatingLT) {
                locatingLT = false;
                locatingRB = true;
                mRectHighlight = new Rect();
                mRectHighlight.left = (int)event.getX();
                mRectHighlight.top = (int)event.getY();

                MyLog.i(TAG, "Set Left/Top to: " + mRectHighlight.left + " / " + mRectHighlight.top);
                return true;
            } else if (locatingRB) {
                locatingRB = false;
                mRectHighlight.right = (int)event.getX();
                mRectHighlight.bottom = (int)event.getY();
                mFlHighlight = new FrameLayout(this);
                mFlHighlight.setBackgroundColor(0x33333333);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(mRectHighlight.width(), mRectHighlight.height());
                lp.setMargins(mRectHighlight.left, mRectHighlight.top, 0, 0);
                mFlBg.addView(mFlHighlight, lp);

                MyLog.i(TAG, "Set Right/Bottom to: " + mRectHighlight.right + " / " + mRectHighlight.bottom);
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * Detects and toggles immersive mode (also known as "hidey bar" mode).
     */
    public void toggleHideyBar() {

        // The UI options currently enabled are represented by a bitfield.
        // getSystemUiVisibility() gives us that bitfield.
        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        boolean isImmersiveModeEnabled =
                ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
        if (isImmersiveModeEnabled) {
            MyLog.i(TAG, "Turning immersive mode mode off. ");
        } else {
            MyLog.i(TAG, "Turning immersive mode mode on.");
        }

        // Navigation bar hiding:  Backwards compatible to ICS.
        if (Build.VERSION.SDK_INT >= 14) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

        // Status bar hiding: Backwards compatible to Jellybean
        if (Build.VERSION.SDK_INT >= 16) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        // Immersive mode: Backward compatible to KitKat.
        // Note that this flag doesn't do anything by itself, it only augments the behavior
        // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
        // all three flags are being toggled together.
        // Note that there are two immersive mode UI flags, one of which is referred to as "sticky".
        // Sticky immersive mode differs in that it makes the navigation and status bars
        // semi-transparent, and the UI flag does not get cleared when the user interacts with
        // the screen.
        if (Build.VERSION.SDK_INT >= 18) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }
}
