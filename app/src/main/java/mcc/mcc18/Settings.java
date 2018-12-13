package mcc.mcc18;

import android.content.Context;
import android.util.Pair;

import com.google.firebase.storage.FirebaseStorage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import mcc.mcc18.Utils.PicResolution;

/**
 *
 */
public class Settings {
    public static Pair<Integer, Integer> imageQuality = PicResolution.FULLRES;
    public static final String imagePath = "images/";
    public static final String imgRefPath = "https://firebasestorage.googleapis.com/v0/b/dazzling-tiger-222016.appspot.com/o/";
    public static final String imgConverterUrl = "https://us-central1-dazzling-tiger-222016.cloudfunctions.net/image-resize-1";
    public static final String testPath = "imagesTest/";
    public static final String configTextFile = "config.txt";
    public static FirebaseStorage storage = FirebaseStorage.getInstance();


    //old version of convertion url. Used to call a function which dynamically stretched the picture
    public static String getConvertedImgUrl(String filename){
        String url = imgConverterUrl + "?f=" + filename.replace("%2F","/").replace("%3A",":");
        if(imageQuality.first != 0){
            url += "&width=" + imageQuality.first + "&height=" + imageQuality.second;
        }
        return url;
    }

    public static void writeToConfigFile(String quality, Context context){
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(configTextFile, Context.MODE_PRIVATE));
            outputStreamWriter.write(quality);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            System.out.println("Exception File write failed: " + e.toString());
        }
    }

    public static void readConfigFile(Context context){
        String ret = "";

        try {
            InputStream inputStream = context.openFileInput("config.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
                updateImageQuality(ret.trim(), context);
            }
        }
        catch (FileNotFoundException e) {
            System.out.println("File not found! Let's create a new then!");
            new File(context.getFilesDir(), configTextFile);
            updateImageQuality(PicResolution.FULL, context);

        } catch (IOException e) {
            System.out.println("Cannot read file exception: " + e.toString());
        }
    }

    public static int getCorrespondentQuality(){
        if(imageQuality.equals(PicResolution.LOWRES))
            return 2;
        else if(imageQuality.equals(PicResolution.HIGHRES)){
            return 1;
        }
        else if(imageQuality.equals(PicResolution.FULLRES)){
            return 0;
        }
        else return 0;
    }

    //Yes this could be a map but i'm lazy to implement it
    public static void updateImageQuality(String quality, Context context){
        if(quality.equals(PicResolution.LOW))
            imageQuality = PicResolution.LOWRES;
        else if(quality.equals(PicResolution.HIGH)){
            imageQuality = PicResolution.HIGHRES;
        }
        else if(quality.equals(PicResolution.FULL)){
            imageQuality = PicResolution.FULLRES;
        }
        else return;
        writeToConfigFile(quality, context);
    }
}
