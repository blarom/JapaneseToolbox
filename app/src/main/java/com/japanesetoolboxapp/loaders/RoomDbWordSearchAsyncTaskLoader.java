package com.japanesetoolboxapp.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.japanesetoolboxapp.data.JapaneseToolboxCentralRoomDatabase;
import com.japanesetoolboxapp.data.Word;
import com.japanesetoolboxapp.resources.Utilities;

import java.util.ArrayList;
import java.util.List;

public class RoomDbWordSearchAsyncTaskLoader extends AsyncTaskLoader<Object> {

    String mSearchWord;

    public RoomDbWordSearchAsyncTaskLoader(Context context, String searchWord) {
        super(context);
        mSearchWord = searchWord;
    }

    @Override
    protected void onStartLoading() {
        if (!TextUtils.isEmpty(mSearchWord)) forceLoad();
    }

    @Override
    public List<Word> loadInBackground() {

        List<Word> localMatchingWordsList = new ArrayList<>();
        if (!TextUtils.isEmpty(mSearchWord)) {
            JapaneseToolboxCentralRoomDatabase japaneseToolboxCentralRoomDatabase = JapaneseToolboxCentralRoomDatabase.getInstance(getContext());
            List<Long> matchingWordIds = Utilities.getMatchingWordIdsAndDoBasicFiltering(mSearchWord, japaneseToolboxCentralRoomDatabase);
            localMatchingWordsList = japaneseToolboxCentralRoomDatabase.getWordListByWordIds(matchingWordIds);
        }

        return localMatchingWordsList;
    }
}
