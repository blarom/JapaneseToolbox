package com.japanesetoolboxapp.loaders;

import android.content.Context;
import android.os.Looper;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.data.Word;
import com.japanesetoolboxapp.resources.Utilities;

import java.util.ArrayList;
import java.util.List;

public class JishoResultsAsyncTaskLoader extends AsyncTaskLoader<Object> {

    String mQuery;
    private boolean internetIsAvailable;
    private boolean mAllowLoaderStart;

    public JishoResultsAsyncTaskLoader(Context context, String query) {
        super(context);
        this.mQuery = query;
    }

    @Override
    protected void onStartLoading() {
        if (mAllowLoaderStart) forceLoad();
    }

    @Override
    public List<Word> loadInBackground() {

        internetIsAvailable = Utilities.internetIsAvailableCheck(getContext());

        List<Word> matchingWordsFromJisho = new ArrayList<>();

        if (internetIsAvailable && !TextUtils.isEmpty(mQuery)) {
            matchingWordsFromJisho = Utilities.getWordsFromJishoOnWeb(mQuery, getContext());
        } else {
            Log.i("Diagnosis Time", "Failed to access online resources.");
            if (Looper.myLooper()==null) Looper.prepare();
            Toast.makeText(getContext(), R.string.failed_to_connect_to_internet, Toast.LENGTH_SHORT).show();
            cancelLoadInBackground();
        }
        return matchingWordsFromJisho;
    }

    public void setLoaderState(boolean state) {
        mAllowLoaderStart = state;
    }
}
