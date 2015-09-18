package io.unisong.android.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
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


    private User mUser;
    private ImageView mProfilePictureView;
    private HttpClient mClient;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mProfilePictureView = (ImageView) findViewById(R.id.profile_picture_view);

        mUser = CurrentUser.getInstance().getUser();

        if(mUser.isFacebookUser()){

        }

        mClient = HttpClient.getInstance();

    }

    public void onChooseProfilePicture(View v){
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

            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), chosenImageUri);
            } catch (FileNotFoundException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            }
            JSONObject object = new JSONObject();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            } catch (NullPointerException e){
                e.printStackTrace();
                return;
            }
            byte[] byteArray = byteArrayOutputStream .toByteArray();
            String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);

            try {
                object.put("data", encoded);
            } catch (JSONException e){
                e.printStackTrace();
                return;
            }

            try {
                mClient.put(NetworkUtilities.HTTP_URL + "", object);
            } catch (IOException e){
                e.printStackTrace();
            }

        }
    }
}
