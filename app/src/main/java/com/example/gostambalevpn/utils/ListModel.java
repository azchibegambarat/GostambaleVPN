package com.example.gostambalevpn.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class ListModel {

    private  String CompanyName="";
    private String packageName;
    private boolean selected;
    private String image;

    public ListModel() {
    }

    public ListModel(String packageName, String companyName, boolean b) {
        this.CompanyName = companyName;
        this.packageName = packageName;
        this.selected = b;
    }
    private static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
    public ListModel(String packageName, String companyName, Context context, boolean b) {
        try {
            this.selected = b;
            this.CompanyName = companyName;
            this.packageName = packageName;
            Drawable icon = context.getPackageManager().getApplicationIcon(packageName);
            if(icon != null){
                Bitmap bitmapDrawable = drawableToBitmap(icon);
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                bitmapDrawable.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
                byte[] byteArray = byteStream.toByteArray();
                String baseString = Base64.encodeToString(byteArray,Base64.DEFAULT);
                this.image = "data:image/png;base64, " + baseString;
            }

        } catch (PackageManager.NameNotFoundException ignore) {
        }

    }


    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }



    /*********** Set Methods ******************/

    public void setCompanyName(String CompanyName)
    {
        this.CompanyName = CompanyName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /*********** Get Methods ****************/

    public String getCompanyName()
    {
        return this.CompanyName;
    }


}