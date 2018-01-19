package com.japanesetoolboxapp.utiities;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class SharedMethods {

    public static void trimCache(Context context) {
        // http://stackoverflow.com/questions/10977288/clear-application-cache-on-exit-in-android
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    public static List<String[]> readCSVFile(String filename, Context context) {

        List<String[]> mySheet = new ArrayList<>();

        // OpenCSV implementation
        //                String next[] = null;
        //                CSVReader reader = null;
        //                try {
        //                    reader = new CSVReader(new InputStreamReader(GlobalTranslatorActivity.getAssets().open(filename)));
        //                } catch (IOException e) {
        //                    e.printStackTrace();
        //                }
        //                if (reader != null) {
        //                    for (; ; ) {
        //                        try {
        //                            next = reader.readNext();
        //                        } catch (IOException e) {
        //                            e.printStackTrace();
        //                        }
        //                        if (next != null) {
        //                            mySheet.add(next);
        //                        } else {
        //                            break;
        //                        }
        //                    }
        //                }
        //                try {
        //                    reader.close();
        //                } catch (IOException e) {
        //                    e.printStackTrace();
        //                }

        // "|" Parser implementation

        BufferedReader fileReader = null;

        int line_number = 0;
        try {
            String line;
            fileReader = new BufferedReader(new InputStreamReader(context.getAssets().open(filename)));

            while ((line = fileReader.readLine()) != null) {
                String[] tokens = line.split("\\|",-1);
                if (tokens.length > 0) {
                    mySheet.add(tokens);
                    line_number++;
                }
            }
        } catch (Exception e) {
            System.out.println("Error in CsvFileReader !!!");
            e.printStackTrace();
            Log.i("Diagnosis Time","Error in CsvFileReader opening Loaded DecompositionDatabase_PART4. Line number:"+line_number);
        } finally {
            try {
                if (fileReader != null) {fileReader.close();}
            } catch (IOException e) {
                System.out.println("Error while closing fileReader !!!");
                e.printStackTrace();
                Log.i("Diagnosis Time","Error in CsvFileReader closing Loaded DecompositionDatabase_PART4.");
            }
        }

        return mySheet;
    }

    public static List<String[]> readCSVFileFirstRow(String filename, Context context) {

        List<String[]> mySheetFirstRow = new ArrayList<>();

        //OpenCSV implementation
        //				  String firstrow[] = null;
        //                String next[] = null;
        //                CSVReader reader = null;
        //
        //                try {
        //                    reader = new CSVReader(new InputStreamReader(GlobalTranslatorActivity.getAssets().open(filename)));
        //                } catch (IOException e) {
        //                    e.printStackTrace();
        //                }
        //
        //                if (reader != null) {
        //                    try {
        //                        firstrow = reader.readNext();
        //                    } catch (IOException e) {
        //                        e.printStackTrace();
        //                    }
        //                    if (firstrow != null) {
        //                        mySheetFirstRow.add(firstrow);
        //                    }
        //                }
        //
        //                try {
        //                    reader.close();
        //                } catch (IOException e) {
        //                    e.printStackTrace();
        //                }

        // "|" Parser implementation

        BufferedReader fileReader = null;

        try {
            String line;
            fileReader = new BufferedReader(new InputStreamReader(context.getAssets().open(filename)));

            line = fileReader.readLine();
            String[] tokens = line.split("\\|",-1);
            if (tokens.length > 0) {
                mySheetFirstRow.add(tokens);
            }
        } catch (Exception e) {
            System.out.println("Error in CsvFileReader !!!");
            e.printStackTrace();
        } finally {
            try {
                if (fileReader != null) {fileReader.close();}
            } catch (IOException e) {
                System.out.println("Error while closing fileReader !!!");
                e.printStackTrace();
            }
        }

        return mySheetFirstRow;
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(source);
        }
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =(InputMethodManager) activity.getBaseContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    public void makeDelay(int milliseconds){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {}
        }, milliseconds);
    }

    public static long getAvailableMemory() {
        final Runtime runtime = Runtime.getRuntime();
        final long usedMemInMB=(runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
        final long maxHeapSizeInMB=runtime.maxMemory() / 1048576L;
        final long availHeapSizeInMB = maxHeapSizeInMB - usedMemInMB;
        Log.i("Diagnosis Time","Available heap size: " + availHeapSizeInMB);
        return availHeapSizeInMB;
    }

    //Internet Connectivity functions
    public void TellUserIfThereIsNoInternetConnection(final Activity activity) {
        //adapted from https://stackoverflow.com/questions/43315393/android-internet-connection-timeout

        if(!internetIsAvailableCheck(activity.getBaseContext())) {
            try {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(activity.getBaseContext(), "Failed to connect to the Internet.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public boolean internetIsAvailableCheck(Context context) {
        //adapted from https://stackoverflow.com/questions/43315393/android-internet-connection-timeout
        final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();

        if (activeNetworkInfo != null) { // connected to the internet
            //Toast.makeText(context, activeNetworkInfo.getTypeName(), Toast.LENGTH_SHORT).show();

            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
                return isWifiInternetAvailable();
            } else if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                // connected to the mobile provider's data plan
                return true;
            }
        }
        return false;
    }
    public boolean isWifiInternetAvailable() {
        //adapted from https://stackoverflow.com/questions/43315393/android-internet-connection-timeout
        try {
            InetAddress ipAddr = InetAddress.getByName("google.com"); //You can replace it with your name
            return !ipAddr.toString().equals("");
        } catch (Exception e) {
            return false;
        }
    }
}
