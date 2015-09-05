package io.unisong.android.network.user;

import android.telephony.PhoneNumberUtils;

import java.util.List;

/**
 * Created by Ethan on 8/9/2015.
 */
public class Contact {

    private String mName;
    private List<String> mPhoneNumbers;

    public Contact(String name, List<String> phonenumbers){
        mName = name;
        mPhoneNumbers = phonenumbers;
    }

    public boolean matchesPhoneNumber(String phonenumber){
        for(String number : mPhoneNumbers){
            if(PhoneNumberUtils.compare(phonenumber , number)) return true;
        }
        return false;
    }

    public String getName(){
        return mName;
    }
}
