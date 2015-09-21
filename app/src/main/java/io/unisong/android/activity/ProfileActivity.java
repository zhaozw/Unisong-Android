package io.unisong.android.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import io.unisong.android.R;
import io.unisong.android.network.NetworkUtilities;
import io.unisong.android.network.http.HttpClient;
import io.unisong.android.network.user.CurrentUser;
import io.unisong.android.network.user.User;

/**
 * Created by ezturner on 9/17/2015.
 */
public class ProfileActivity extends ActionBarActivity {


    private final static String LOG_TAG = ProfileActivity.class.getSimpleName();

    private User mUser;
    private ImageView mProfilePictureView;
    private HttpClient mClient;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mProfilePictureView = (ImageView) findViewById(R.id.profile_picture_view);

        mUser = CurrentUser.getInstance().getUser();
        mProfilePictureView.setImageBitmap(mUser.getProfilePicture());

        if(mUser.isFacebookUser()){

        }

        mClient = HttpClient.getInstance();

    }

    /**
     * This method is called when a user presses the profile upload button.
     * @param v the view that was pressed to call this method
     */
    public void uploadPicture(View v){
        Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, 1);
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
        {
            Uri chosenImageUri = data.getData();

            Log.d(LOG_TAG , chosenImageUri.toString());

            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), chosenImageUri);
                Matrix matrix = new Matrix();

                matrix.postRotate(-90);

                Bitmap temp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                        bitmap.getHeight(), matrix, true);
                mProfilePictureView.setImageBitmap(temp);
            } catch (FileNotFoundException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            }


            getUploadThread(bitmap).start();
        }
    }

    private Thread getUploadThread(final Bitmap bitmap){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                mUser.uploadProfilePicture(bitmap);
            }
        });
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mClient = null;
        mUser = null;
        mProfilePictureView = null;

    }



}
