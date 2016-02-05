package io.unisong.android.activity.friends.contacts;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.ArrayList;
import java.util.List;

/**
 * The list of contacts. Contains methods for returning a contact based on phone number
 * Created by Ethan on 8/9/2015.
 */
public class ContactsLoader {

    private static final String LOG_TAG = ContactsLoader.class.getSimpleName();
    private static ContactsLoader instance;

    public static ContactsLoader getInstance(){
        return instance;
    }

    private Context context;
    private List<Contact> contacts;

    public ContactsLoader(Context context) {
        this.context = context;
        contacts = new ArrayList<>();


        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
                null, null, null);

                int size = cursor.getCount();
        if (cursor.getCount() > 0) {

            while (cursor.moveToNext()) {

                String name = "";

                List<String> phonenumbers = new ArrayList<>();

                String id = cursor.getString(cursor
                        .getColumnIndex(ContactsContract.Contacts._ID));
                name = cursor
                        .getString(cursor
                                .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                String phone = "";

                if (name != null && !name.equals("")) {
                    if (checkEmail(name)) {
                        continue;
                    }
                }
                if (Integer
                        .parseInt(cursor.getString(cursor
                                .getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0)

                {
                    System.out.println("name : " + name);
                    Cursor pCur = cr
                            .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                                            + " = ?", new String[]{id},
                                    null);

                    while (pCur.moveToNext()) {
                        int phonetype = pCur
                                .getInt(pCur
                                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                        if(phonetype == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {

                            String MainNumber = pCur
                                    .getString(pCur
                                            .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            if(MainNumber != null && !MainNumber.equals(""))
                                phone = MainNumber;
                        }
                    }
                    pCur.close();

                }

                if(!phone.equals("")) {

                    try{
                        Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(phone, "US");
                        phone = phoneUtil.format(phoneNumber , PhoneNumberUtil.PhoneNumberFormat.E164);
                    } catch (NumberParseException e){
                        // currently only support US numbers?
                        e.printStackTrace();
                        Log.d(LOG_TAG, "Parsing Phone number failed!");
                        break;
                    }

                    Contact contact = new Contact(name, phone);
                    contacts.add(contact);
                }
            }
        }

        cursor.close();

        instance = this;
    }

    private boolean checkEmail(String email){
        if (email == null)
            return false;

        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public List<Contact> getContacts(){
        return contacts;
    }
}