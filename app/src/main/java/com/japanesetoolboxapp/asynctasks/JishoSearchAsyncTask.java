package com.japanesetoolboxapp.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.data.Word;
import com.japanesetoolboxapp.resources.Utilities;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class JishoSearchAsyncTask extends AsyncTask<Void, Void, List<Word>> {

    private WeakReference<Context> contextRef;
    private final String mQuery;
    public JishoSearchAsyncResponseHandler listener;

    public JishoSearchAsyncTask(Context context, String query, JishoSearchAsyncResponseHandler listener) {
        contextRef = new WeakReference<>(context);
        this.mQuery = query;
        this.listener = listener;
    }

    protected void onPreExecute() {
        super.onPreExecute();
    }
    @Override
    protected List<Word> doInBackground(Void... voids) {

        List<Word> matchingWordsFromJisho = new ArrayList<>();

        if (Utilities.internetIsAvailableCheck(contextRef.get()) && !TextUtils.isEmpty(mQuery)) {
            matchingWordsFromJisho = Utilities.getWordsFromJishoOnWeb(mQuery, contextRef.get());
        } else {
            Log.i("Diagnosis Time", "Failed to access online resources.");
            if (Looper.myLooper()==null) Looper.prepare();
            Toast.makeText(contextRef.get(), R.string.failed_to_connect_to_internet, Toast.LENGTH_SHORT).show();
        }

        return matchingWordsFromJisho;
    }

    @Override
    protected void onPostExecute(List<Word> words) {
        super.onPostExecute(words);
        listener.onJishoSearchAsyncTaskResultFound(words);
    }

    public interface JishoSearchAsyncResponseHandler {
        void onJishoSearchAsyncTaskResultFound(List<Word> text);
    }
}
