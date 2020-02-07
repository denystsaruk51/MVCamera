package com.denysapps.mvcamera.ui.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import com.denysapps.mvcamera.ui.dialog.MyProgressDialog;

public class BaseActivity extends AppCompatActivity {
    public MyProgressDialog dlg_progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();

        dlg_progress = new MyProgressDialog(this);
    }

    @Override
    public void onRestart(){
        super.onRestart();

        if (dlg_progress == null){
            dlg_progress = new MyProgressDialog(this);
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (dlg_progress != null)
            dlg_progress.dismiss();

    }

    /*
     show progress dialog
    */
    public void showProgressDialog(){
        dlg_progress.show();
    }
    /*
     hide progress dialog
    */
    public void hidProgressDialog(){
        dlg_progress.dismiss();
    }
}

