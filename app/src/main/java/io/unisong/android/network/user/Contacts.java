package io.unisong.android.network.user;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;

/**
 * The list of contacts. Contains methods for returning a contact based on phone number
 * Created by Ethan on 8/9/2015.
 */
public class Contacts {

    public static Contacts getInstance(){
        return sInstance;
    }

    private static Contacts sInstance;

    private Context mContext;
    private List<Contact> mContacts;

    public Contacts(Context context) {
        mContext = context;
        mContacts = new ArrayList<>();

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
                        String phonetype = pCur
                                .getString(pCur
                                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                        String MainNumber = pCur
                                .getString(pCur
                                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                        phonenumbers.add(MainNumber);
                    }
                    pCur.close();

                }

                if(phonenumbers.size() > 0) {
                    Contact contact = new Contact(name, phonenumbers);
                    mContacts.add(contact);
                }
            }
        }

        sInstance = this;
    }

    private boolean checkEmail(String email){
        if (email == null)
            return false;

        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * A method that returns the selected contact if there is a match by phone number
     * it returns null otherwise
     * @param phonenumber the phone number to compare to contacts.
     * @return
     */
    public Contact getContactByPhone(String phonenumber){
        for(Contact contact : mContacts){
            if(contact.matchesPhoneNumber(phonenumber)){
                return contact;
            }
        }
        return null;
    }
}