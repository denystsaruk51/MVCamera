package com.denysapps.mvcamera.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;

import com.denysapps.mvcamera.R;

public class MyProgressDialog extends Dialog {
    public MyProgressDialog(Context context) {
        super(context);
        init(context);
    }

    /*
    Init Progress dialog
     */
    private void init(Context context) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.dialog_progress);

        getWindow().setBackgroundDrawable(new ColorDrawable(0));

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.dimAmount = 0f;
        getWindow().setAttributes(lp);

        setCancelable(false);
    }

    @Override
    public void show() {
        // we are using try - catch in order to prevent crashing issue
        // when the activity is finished but the AsyncTask is still processing
        try {
            super.show();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }
}

