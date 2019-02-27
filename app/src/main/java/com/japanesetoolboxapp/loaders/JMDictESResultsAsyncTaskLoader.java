package com.japanesetoolboxapp.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.japanesetoolboxapp.data.Word;
import com.japanesetoolboxapp.resources.Utilities;

import java.util.ArrayList;
import java.util.List;

public class JMDictESResultsAsyncTaskLoader extends AsyncTaskLoader<Object> {

    private final String mQuery;
    private boolean mAllowLoaderStart;

    public JMDictESResultsAsyncTaskLoader(Context context, String query) {
        super(context);
        this.mQuery = query;
    }

    @Override
    protected void onStartLoading() {
        if (mAllowLoaderStart) forceLoad();
    }

    @Override
    public List<Word> loadInBackground() {

        List<Word> matchingWordsFromJMDict = new ArrayList<>();

        if (Utilities.internetIsAvailableCheck(getContext()) && !TextUtils.isEmpty(mQuery)) {
            matchingWordsFromJMDict = Utilities.getWordsFromJMDictES(mQuery, getContext());
        } else {
            cancelLoadInBackground();
        }
        return matchingWordsFromJMDict;
    }

    public void setLoaderState(boolean state) {
        mAllowLoaderStart = state;
    }
}